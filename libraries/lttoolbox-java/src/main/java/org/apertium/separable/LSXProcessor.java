package org.apertium.separable;

import org.apertium.lttoolbox.Alphabet;
import org.apertium.lttoolbox.Compression;
import org.apertium.lttoolbox.process.Node;
import org.apertium.lttoolbox.process.State;
import org.apertium.lttoolbox.process.TransducerExe;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joan Jerez, ToBeIT
 * @serial GPL 3.0
 */

public class LSXProcessor {

    private static final int EXIT_FAILURE = 1;
    private static final int INT_MAX = Integer.MAX_VALUE;
    public static boolean DEBUG = false;

    // lsx_processor.h

    private Node root;
    private Map<String, TransducerExe> trans;
    private State initial_state;
    private final Set<Character> escaped_chars = new HashSet<>();
    private final Set<Character> alphabetic_chars = new HashSet<>();
    private final Map<Node, Double> all_finals = new HashMap<>();
    private Alphabet alphabet;
    private boolean dictionary_case = false;
    private boolean repeat_rules = false;
    private boolean postgen = false;
    private boolean at_end = false;
    private boolean at_null = false;

    private final List<String> blank_queue = new ArrayList<>();
    private final List<String> bound_blank_queue = new ArrayList<>();
    private final List<String> lu_queue = new ArrayList<>();

    public LSXProcessor() {
        this.setPostgenMode(false);
        this.setRepeatMode(false);
        this.setDictionaryCaseMode(false);
        this.setNullFlush(false);
        this.escaped_chars.add('[');
        this.escaped_chars.add(']');
        this.escaped_chars.add('{');
        this.escaped_chars.add('}');
        this.escaped_chars.add('^');
        this.escaped_chars.add('$');
        this.escaped_chars.add('/');
        this.escaped_chars.add('\\');
        this.escaped_chars.add('@');
        this.escaped_chars.add('<');
        this.escaped_chars.add('>');
    }

    private int word_boundary;
    private int word_boundary_s;
    private int word_boundary_ns;
    private int any_char;
    private int any_tag;
    private int reading_boundary;

    public void load(ByteBuffer input) throws IOException {
        readTransducerSet(input, alphabetic_chars, alphabet, trans);
        // symbols
        word_boundary = alphabet.cast("<$>");
        word_boundary_s = alphabet.cast("<$_>");
        word_boundary_ns = alphabet.cast("<$->");
        any_char = alphabet.cast("<ANY_CHAR>");
        any_tag = alphabet.cast("<ANY_TAG>");
        reading_boundary = alphabet.cast("</>");

        for (Map.Entry<String, TransducerExe> entry : trans.entrySet()) {
            TransducerExe transducer = entry.getValue();
            root.addTransition(0, 0, transducer.getInitial(), 0.0);
            all_finals.putAll(transducer.getFinals2());
        }
        initial_state.init(root);
    }

    private void readNextLU(File input) throws IOException {
        String blank, wblank = "", lu = "";
        blank = input.readBlank();
        if (input.peek() == '[') {
            input.read();
            input.read();
            wblank = input.finishWBlank();
            wblank = wblank.substring(2, wblank.length()-4);
        }
        if (input.peek() == '^') {
            input.read();
            lu = input.readBlock('^', '$');
            lu = lu.substring(1, lu.length()-2);
        }

        blank_queue.addLast(blank);
        bound_blank_queue.addLast(wblank);
        lu_queue.addLast(lu);

        if (input.peek() == '\0') {
            input.read();
            at_end = true;
            at_null = true;
        } else if (input.eof()) {
            at_end = true;
        }
    }

    private void processWord(Reader input, Writer output) throws IOException {
        if(lu_queue.isEmpty())
        {
            readNextLU((File) input);
        }
        if(at_end && lu_queue.size() == 1 && lu_queue.getLast().isEmpty())
        {
            // we're at the final blank, no more work to do
            output.write(blank_queue.getLast());
            blank_queue.removeFirst();
            bound_blank_queue.removeFirst();
            lu_queue.removeFirst();
            return;
        }
        int last_final = 0;
        String last_final_out = "";
        State word_initial_state = initial_state.copy();
        int idx = 0;
        boolean firstupper = false;
        boolean uppercase = false;
        while(word_initial_state.size() > 0)
        {
            if(idx == lu_queue.size())
            {
                if(at_end)
                {
                    break;
                }
                readNextLU((File) input);
            }
            String lu = lu_queue.get(idx);
            if(lu.isEmpty())
            {
                break;
            }
            if (idx == 0 && !dictionary_case) {
                firstupper = Character.isUpperCase(lu.charAt(0));
                uppercase = lu.length() > 1 && Character.isUpperCase(lu.charAt(1));
            }
            for(int i = 0; i < lu.length(); i++)
            {
                if(lu.charAt(i) == '<')
                {
                    int j = i+1;
                    for(; j < lu.length(); j++)
                    {
                        if(lu.charAt(j) == '\\')
                        {
                            j++;
                        }
                        else if(lu.charAt(j) == '>')
                        {
                            j++;
                            break;
                        }
                    }
                    String tag = lu.substring(i, j-i);
                    i = j-1;
                    if(!alphabet.isSymbolDefined(tag))
                    {
                        alphabet.includeSymbol(tag);
                    }
                    word_initial_state.step_override(alphabet.cast(tag), any_tag, alphabet.cast(tag));
                }
                else if (postgen && lu.charAt(i) == '/') {
                    word_initial_state.step(reading_boundary);
                    word_initial_state.merge(word_initial_state);
                }
                else
                {
                    if(lu.charAt(i) == '\\')
                    {
                        i++;
                    }
                    word_initial_state.step_override(lu.charAt(i), Character.toLowerCase(lu.charAt(i)), any_char, lu.charAt(i));
                }
            }
            word_initial_state.step(word_boundary, word_boundary_s, word_boundary_ns);
            if(word_initial_state.isFinal2(all_finals))
            {
                last_final = idx+1;
                Set<Integer> intSet = escaped_chars.stream()
                        .map(c -> (int) c) // El cast automático convierte 'A' en 65
                        .collect(Collectors.toSet());
                last_final_out = word_initial_state.filterFinals(all_finals, alphabet, intSet,
                        false, 1, INT_MAX, uppercase, firstupper).substring(1);
                if (postgen) {
                    // TODO: why don't slashes get escaped by filterFinals()?
                    last_final_out = last_final_out.replace("/", "\\/");
                    last_final_out = last_final_out.replace("<\\/>", "/");
                }
            }
            idx++;
        }
        if(last_final == 0)
        {
            output.write(blank_queue.getFirst());
            blank_queue.removeFirst();
            if(!bound_blank_queue.getFirst().isEmpty())
            {
                // C++ u_fprintf(output, "[[%S]]", bound_blank_queue.front().c_str());
                output.write( "[[" + bound_blank_queue.getFirst() + "]]");
            }
            bound_blank_queue.removeFirst();
            output.write("^" + lu_queue.getFirst() + "$");
            lu_queue.removeFirst();
            return;
        }

        int output_count = 0;
        int pos = 0;
        boolean pop_queue = true;
        boolean replace_empty = false;
        List<String> blanks = new ArrayList<>();
        List<String> lus = new ArrayList<>();
        List<Integer> wblanks_index = new ArrayList<>();
        Set<Integer> wblanks_to_merge =  new HashSet<>();
        Map<String, Integer> lu_index = new HashMap<>();
        for (int i = 0; i < last_final; i++) {
            // note: if a pattern matches 2 identical LUs, we'll lose the first wblank
            lu_index.put(lu_queue.get(i), i);
            wblanks_to_merge.add(i);
            if (postgen) {
                List<String> pieces = splitEscaped(lu_queue.get(i), '/');
                if (!pieces.isEmpty()) lu_index.put(pieces.getLast(),i);
            }
        }
        while(pos != -1 && pos != last_final_out.length())
        {
            if (pop_queue) {
                if (output_count < last_final) {
                    if (replace_empty && blank_queue.get(output_count).isEmpty()) {
                        blanks.addLast(" ");
                    } else {
                        blanks.addLast(blank_queue.get(output_count));
                    }
                    output_count++;
                } else {
                    blanks.addLast(" ");
                }
            } else {
                blanks.addLast("");
            }
            int start = pos;
            pos = last_final_out.indexOf("<$", start);
            if(pos == -1)
            {
                lus.addLast(last_final_out.substring(start));
                break;
            }
            else
            {
                lus.addLast(last_final_out.substring(start, pos-start));
                pos += 2;
                if (last_final_out.charAt(pos) == '-') {
                    pop_queue = false;
                    pos++;
                } else if (last_final_out.charAt(pos) == '_') {
                    pop_queue = true;
                    replace_empty = true;
                    pos++;
                } else {
                    pop_queue = true;
                    replace_empty = false;
                }
                pos++;
            }
        }
        for (String s : lus) {
            Integer loc = lu_index.get(s);
            if (lu_index.containsKey(s)) {
                wblanks_index.addLast(loc);
                wblanks_to_merge.remove(loc);
            } else {
                wblanks_index.addLast(-1);
            }
        }
        StringBuilder trail_blank = new StringBuilder();
        for(; output_count < last_final; output_count++)
        {
            if(!Objects.equals(blank_queue.get(output_count), " "))
            {
                trail_blank.append(blank_queue.get(output_count));
            }
        }
        StringBuilder merged_wblank = new StringBuilder();
        for (Integer i : wblanks_to_merge) {
            if (bound_blank_queue.get(i).isEmpty()) continue;
            if (merged_wblank.isEmpty()) {
                merged_wblank = new StringBuilder(bound_blank_queue.get(i));
            } else {
                merged_wblank.append("; ");
                merged_wblank.append(bound_blank_queue.get(i));
            }
        }
        List<String> wblanks = new ArrayList<>();
        int changed_idx = 0;
        for (int i = 0; i < wblanks_index.size(); i++) {
            int src = wblanks_index.get(i);
            wblanks.addLast((src == -1) ? merged_wblank.toString() : bound_blank_queue.get(src));
            if (postgen) {
                if (lus.get(i).indexOf('/') != -1) {
                    continue;
                }
                String tmp = "";
                if (src != -1) {
                    tmp = lu_queue.get(i);
                }
                else if (changed_idx < wblanks_to_merge.size()) {
                    // C++ auto pieces = StringUtils::split_escaped(lu_queue[i], '/');
                    List<String> pieces = splitEscaped(lu_queue.get(i), '/');
                    tmp += pieces.getFirst();
                    tmp += '/';
                    tmp += lus.get(i);
                    changed_idx++;
                }
                else {
                    tmp += '/';
                    tmp += lus.get(i);
                }
                lus.set(i, tmp);
            }
        }

        blank_queue.remove(blank_queue.get(last_final));
        bound_blank_queue.remove(bound_blank_queue.get(last_final));
        lu_queue.remove(lu_queue.get(last_final));

        if (repeat_rules) {
            blank_queue.addFirst(trail_blank + blank_queue.getFirst());
            blank_queue.addAll(blanks);
            bound_blank_queue.addAll(wblanks);
            lu_queue.addAll(lus);
        } else {
            for (int i = 0; i < lus.size(); i++) {
                if (wblanks.get(i).isEmpty()) {
                    output.write(blanks.get(i) + "^"+ lus.get(i) + "$");
                    // C++ u_fprintf(output, "%S^%S$", blanks[i].c_str(), lus[i].c_str());
                } else {
                    output.write(blanks.get(i)  + "[[" + wblanks.get(i) + "]]^" + lus.get(i) + "$");
                    // C++ u_fprintf(output, "%S[[%S]]^%S$", blanks[i].c_str(), wblanks[i].c_str(), lus[i].c_str());
                }
            }
            output.write(trail_blank.toString());
        }
    }

    void process(Reader input, Writer output) throws IOException {
        {
            while(true)
            {
                while(!at_end || !lu_queue.isEmpty())
                {
                    processWord(input, output);
                }
                if(at_null)
                {
                    output.write('\0');
                    output.flush();
                    at_end = false;
                    at_null = false;
                }
                else
                {
                    break;
                }
            }
        }
    }

    public static void readTransducerSet(ByteBuffer input, Set<Character> letters, Alphabet alphabet,
                                         Map<String, TransducerExe> transducers) throws IOException {

        readShared(input, letters, alphabet);
        int count = Compression.multibyte_read(input);
        for (int i = 0; i < count; i++) {
            String name = Compression.String_read(input);
            TransducerExe transducer = new TransducerExe();
            transducer.read(input, alphabet);
            transducers.put(name, transducer);
        }
    }

    private static void readShared(ByteBuffer input, Set<Character> letters, Alphabet alphabet) throws IOException {
        input.mark();
        byte[] header = new byte[4];
        input.get(header);
        if (Arrays.equals(header, Compression.HEADER_LTTOOLBOX)) {
            long features = input.getLong();
            if (DEBUG) System.err.println("LSXProcessor.readShared() got features " + features);
            if (features>=1) throw new IOException("LSX has features that are unknown to this version of lttoolbox - upgrade!");
        } else {
            // Old binary format
            input.reset();
            if (DEBUG) System.err.println("LSXProcessor.readShared() got Old binary format");
        }
        // read letters (<alphabet>ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz</alphabet>)
        for (int len = Compression.multibyte_read(input); len > 0; len--) {
            letters.add((char)(Compression.multibyte_read(input)));
        }
        // symbols
        Alphabet.read(input);
        if (DEBUG) {
            System.err.println("LSXProcessor load(" + input);
            System.err.println("alphabet = " + alphabet.toString().replace(',', '\n'));
        }
    }

    public static List<String> splitEscaped(String str, char delim) {
        List<String> result = new ArrayList<>();
        int start = 0;
        int length = str.length();

        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);

            if (c == '\\') {
                i++; // Saltamos el siguiente carácter porque está escapado
                continue;
            }

            if (c == delim) {
                if (i > start) {
                    result.add(str.substring(start, i));
                }
                start = i + 1;
            }
        }

        if (start < length) {
            result.add(str.substring(start));
        }

        return result;
    }

    void setNullFlush(boolean val) {}
    void setDictionaryCaseMode(boolean val)
    {
        dictionary_case = val;
    }
    void setRepeatMode(boolean val)
    {
        repeat_rules = val;
    }
    void setPostgenMode(boolean val)
    {
        postgen = val;
        // this could potentially cause problems at some point...
        if (val) {
            escaped_chars.remove('/');
        } else {
            escaped_chars.add('/');
        }
    }

    /*
    First, identify the C++ types used in your implementation and their Java equivalents.

    C++	Java equivalent	Notes
    UString	String	Java strings use UTF-16; verify Unicode behavior.
    std::vector<T>	ArrayList<T>	Dynamic array.
    std::map<K,V>	TreeMap<K,V> or HashMap<K,V>	Choose based on ordering requirements.
    std::set<T>	TreeSet<T> or HashSet<T>	Choose based on ordering requirements.
    std::deque<T>	ArrayDeque<T> or LinkedList<T>	ArrayDeque is usually preferable for queue operations.
    std::vector::push_back()	list.add()	Appends an element.
    std::vector::pop_front()	list.remove(0)	Removes the first element.
    std::map::find()	map.get() or map.containsKey()	Use containsKey() if null values are possible.
    std::map::insert()	map.put()	Inserts or replaces a mapping.
    std::string::substr()	String.substring()	Java's second argument is an exclusive end index.
    std::string::find()	String.indexOf()	Returns -1 if not found.
    std::string::npos	-1	Java's indexOf() uses -1 for no match.
    FILE*	InputStream or Reader	Depends on whether input is byte-oriented or character-oriented.
    UFILE*	Writer or OutputStream	Choose based on output encoding requirements.
    u_fprintf()	Writer.write() or Formatter	ICU-specific formatting needs special care.
    u_isupper()	Character.isUpperCase()	Check Unicode behavior.
    towlower()	Character.toLowerCase()	A Java char is not always sufficient for Unicode code points.
    INT_MAX	Integer.MAX_VALUE	Java integer limit.
    auto&	Explicit Java type	Java does not support C++ references.

    One important difference: C++ strings and Java strings have different Unicode representations. Since your code uses ICU's UString, u_isupper(), and towlower(), verify that Java's Unicode operations behave consistently with the original implementation.
     */

}
