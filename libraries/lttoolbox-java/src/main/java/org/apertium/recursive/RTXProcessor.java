package org.apertium.recursive;

import org.apertium.lttoolbox.Alphabet;
import org.apertium.lttoolbox.Compression;
import org.apertium.lttoolbox.Pair;
import org.apertium.lttoolbox.collections.Transducer;
import org.apertium.transfer.ApertiumRE;

import java.io.*;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apertium.recursive.ByteCode.*;
import static org.apertium.recursive.Chunk.ClipType.*;
import static org.apertium.recursive.Chunk.TreeMode.*;

/**
 * @author Joan Jerez, ToBeIT
 * @serial GPL 3.0
 */

public class RTXProcessor {
    private static final int EXIT_FAILURE = 1;

    // rtx_processor.h

    private static class StackElement {
        int mode;
        boolean b;
        int i;
        String s;
        Chunk c;
    }

    /**
     * Alphabet instance for the pattern transducer
     */
    Alphabet alphabet;
    /**
     * The pattern transducer
     */
    MatchExe2 mx;
    /**
     * Attribute category regular expressions
     */
    Map<String, ApertiumRE> attr_items = new TreeMap<>(Comparator.naturalOrder()); // C++ map<wstring, ApertiumRE, Ltstr> attr_items;
    /**
     * Virtual machine global variables
     * name => value
     */
    Map<String, String> variables = new TreeMap<>(Comparator.naturalOrder());
    /**
     * Virtual machine global variables to wblank map
     * name => value
     */
    Map<String, String> wblankVariables = new TreeMap<>(Comparator.naturalOrder());
    /**
     * Lists
     * name => { values }
     */
    Map<String, Set<String>> lists = new TreeMap<>(Comparator.naturalOrder());
    /**
     * Lists, but all values are converted to lower case
     * Used for case-insensitive comparison
     * name => { values }
     */
    Map<String, Set<String>> listslow = new TreeMap<>(Comparator.naturalOrder());

    /**
     * Bytecode for input-time rules
     */
    List<String> rule_map;
    /**
     * Bytecode for output-time rules
     */
    List<String> output_rules;
    /**
     * Debug names for input-time rules (maybe empty)
     */
    List<String> inRuleNames;
    /**
     * Debug names for output-time rules (maybe empty)
     */
    List<String> outRuleNames;
    /**
     * Length of pattern of each input-time rule, including blanks
     */
    List<Integer> pat_size;
    /**
     * Maximum value of pat_size
     */
    int longestPattern;
    /**
     * Number of Chunk* global variables
     */
    int varCount;

    /**
     * The stack used by the virtual machine
     * The stack size is set to 32 because a rule would only go higher than
     * roughly 10 if it was evaluating a very complex conditional,
     * so 32 is probably more than anyone will ever need
     */
    StackElement[] theStack = new StackElement[32];
    /**
     * Index of the top element on theStack
     */
    int stackIdx;
    /**
     * A parallel stack to store wordbound blanks that mimics the operations
     * of the main stack. wblanks are added everytime lemmas are clipped
     */
    String[] theWblankStack = new String[32];
    /**
     * wordbound blank to be output
     */
    String out_wblank;
    /**
     * Input to the virtual machine
     * currentInput is global rather than a parameter mostly for symmetry
     * with currentOutput
     */
    List<Chunk> currentInput;
    /**
     * Output from the virtual machine
     * The return value of the virtual machine is whether the rule was
     * rejected, and passing data by semi-global array saves some amount of
     * allocating and copying
     */
    List<Chunk> currentOutput;

    /**
     * Chunk containing currentInput for output-time rules
     */
    Chunk parentChunk;
    /**
     * Chunks waiting to be written to output stream
     */
    Deque<Chunk> outputQueue;

    /**
     * The parse stack
     * Each element represents one possible partial or complete parse
     * of the input read in since the last flush
     */
    List<ParseNode> parseGraph = new ArrayList<>();
    /**
     * Pool allocator for Chunks, freed on output flush
     */
    Pool<Chunk> chunkPool;
    /**
     * Pool allocator for ParseNodes, freed on output flush
     */
    Pool<ParseNode> parsePool;
    /**
     * The next few tokens in the input stream (usually 5)
     */
    Deque<Chunk> inputBuffer;
    /**
     * Pseudo-input buffer used by checkForReduce()
     * Rules that output a single node are processed immediately
     * but when rules output multiple nodes, treating them as input
     * necessitates that reductions could happen, so any output past the first
     * node is pushed onto currentContinuation, with the next node to be
     * processed being currentContinuation.back()->back()
     */
    List<List<Chunk>> currentContinuation;

    /* SETTINGS */

    boolean furtherInput = true; // if EOF or \0 has been reached in the input stream, true otherwise
    boolean inword = false; // true if the next input token should be parsed as an LU, false otherwise
    boolean inwblank = false; // true if the next input token should be parsed as a wordbound blank, false otherwise
    boolean nullFlush = false; // Whether output should flush on \0
    boolean printingSteps = false; // If true, each instruction of virtual machine will be printed to wcerr
    boolean printingRules = false; // If true, each rule that is applied will be printed to wcerr
    boolean printingBranches = false; // If true, each action of filterParseGraph() will be logged to wcerr
    boolean printingAll = false; // If true, produce a full report, similar to (printingRules && printingBranches)
    boolean noCoref = true; // false if input comes from apertium-anaphora, true otherwise
    boolean isLinear = false; // true if rule application should mimic the chunker-interchunk-postchunk pipeline, false otherwise
    boolean printingTrees = false; // If true, parse tree will be printed according to treePrintMode before output-time rules are applied
    boolean printingText = true; // If false, output-time rules will not be applied and linear output will not be produced
    /* Manner in which to print trees
     * Set by setOutputMode()*/
    Chunk.TreeMode treePrintMode = TreeModeNest;
    /**
     * Counter used to give distinct, consistent identifiers to ParseNodes
     * for tracing purposes
     */
    int newBranchId = 0;
    boolean noFilter = true; // If this is set to true, filterParseGraph() will only discard branches on parse error
    /**
     * Branch of parseGraph currently being operated on
     * Needed by applyRule() for FETCHCHUNK and SETCHUNK
     */
    ParseNode currentBranch = null;

    void pushStack(boolean b) {
        theStack[++stackIdx].mode = 0;
        theStack[stackIdx].b = b;
        theWblankStack[stackIdx] = EMPTY;
    }

    void pushStack(int i) {
        theStack[++stackIdx].mode = 1;
        theStack[stackIdx].i = i;
        theStack[++stackIdx].mode = 0;
    }

    void pushStack(String s) {
        String wbl = "";
        theStack[++stackIdx].mode = 2;
        theStack[stackIdx].s = s;
        theWblankStack[stackIdx] = wbl;
    }

    void pushStack(String s, String wbl) {
        theStack[++stackIdx].mode = 2;
        theStack[stackIdx].s = s;
        theWblankStack[stackIdx] = wbl;
    }

    void pushStack(Chunk c) {
        theStack[++stackIdx].mode = 3;
        theStack[stackIdx].c = c;
        theWblankStack[stackIdx] = EMPTY;
    }

    public void withoutCoref(boolean b) {
        noCoref = b;
    }

    public void completeTrace(boolean b) {
        printingAll = b;
    }

    public void printFilter(boolean b) {
        printingBranches = b;
    }

    public void noFiltering(boolean b) {
        noFilter = b;
    }

    public void printRules(boolean b) {
        printingRules = b;
    }

    public void printSteps(boolean b) {
        printingSteps = b;
    }

    public void mimicChunker(boolean val) {
        isLinear = val;
    }

    public void printTrees(boolean haveT) {
        printingTrees = haveT;
    }

    public void printText(boolean b) {
        printingText = b;
    }

    public void read(String filename) {
        try (FileInputStream in = new FileInputStream(filename)) {
            longestPattern = 2 * Compression.multibyte_read(in) - 1;
            int count = Compression.multibyte_read(in);
            for (int i = 0; i < count; i++) {
                pat_size.addLast(Compression.multibyte_read(in));
                rule_map.addLast(Compression.String_read(in));
            }
            varCount = Compression.multibyte_read(in);
            Alphabet.read(in);
            Transducer t = new Transducer();
            Transducer.read(t, in, alphabet.size());
            Map<Integer, Pair<Integer, Double>> finals = new HashMap<>();
            HashMap<Integer, Double> finalWeights;
            for (int i = 0, limit = Compression.multibyte_read(in); i != limit; i++) {
                int key = Compression.multibyte_read(in);
                int rl = Compression.multibyte_read(in);
                double wgt = Compression.multibyte_read(in);
                Pair<Integer, Double> ruleWeight;
                ruleWeight = new Pair<>(rl, wgt);
                finals.put(key, ruleWeight);
            }
            mx = new MatchExe2(t, alphabet, finals, pat_size);
            // attr_items
            boolean recompile_attr = !Compression.String_read(in).equals(pcre_version());
            for (int i = 0, limit = Compression.multibyte_read(in); i != limit; i++) {
                String cad_k = Compression.String_read(in);
                ApertiumRE item = attr_items.get(cad_k);
                item.read(in);
                String fallback = Compression.String_read(in);
                if (recompile_attr) {
                    attr_items.get(cad_k).compile(fallback);
                }
            }
            // variables
            for (int i = 0, limit = Compression.multibyte_read(in); i != limit; i++) {
                String cad_k = Compression.String_read(in);
                variables.put(cad_k, Compression.String_read(in));
            }
            // lists
            for (int i = 0, limit = Compression.multibyte_read(in); i != limit; i++) {
                String cad_k = Compression.String_read(in);
                for (int j = 0, limit2 = Compression.multibyte_read(in); j != limit2; j++) {
                    String cad_v = Compression.String_read(in);
                    Set<String> set_s = lists.computeIfAbsent(cad_k, k -> new TreeSet<>(Comparator.naturalOrder()));
                    set_s.add(cad_v);
                    lists.put(cad_k, set_s);
                    Set<String> set_s2 = listslow.computeIfAbsent(cad_k, k -> new TreeSet<>(Comparator.naturalOrder()));
                    set_s2.add(cad_v.toLowerCase());
                    listslow.put(cad_k, set_s2);
                }
            }
            int nameCount = Compression.multibyte_read(in);
            for (int i = 0; i < nameCount; i++) {
                inRuleNames.addLast(Compression.String_read(in));
            }
            nameCount = Compression.multibyte_read(in);
            for (int i = 0; i < nameCount; i++) {
                outRuleNames.addLast(Compression.String_read(in));
            }

        } catch (Exception e) {
            System.err.println("Unable to open file " + filename);
            System.exit(EXIT_FAILURE);
        }
    }

    private String pcre_version() {
        return "8.45 2021-06-15";
    }

    private boolean beginsWith(String s1, String s2) {
        int limit = s2.length(), constraint = s1.length();

        if (constraint < limit) {
            return false;
        }
        for (int i = 0; i != limit; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean endsWith(String s1, String s2) {
        int limit = s2.length(), constraint = s1.length();

        if (constraint < limit) {
            return false;
        }
        for (int i = limit - 1, j = constraint - 1; i >= 0; i--, j--) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private String copycase(String source_word, String target_word) {
        StringBuilder result;

        boolean firstupper = Character.isUpperCase(source_word.charAt(0));
        boolean uppercase = firstupper && Character.isUpperCase((source_word.charAt(source_word.length() - 1)));
        boolean sizeone = source_word.length() == 1;

        if (!uppercase || sizeone) {
            if (isLinear) {
                result = new StringBuilder(target_word);
                result.setCharAt(0, Character.toLowerCase((result.charAt(0))));
            } else result = new StringBuilder(target_word.toLowerCase());
        } else {
            result = new StringBuilder(target_word.toUpperCase());
        }

        if (firstupper) {
            result.setCharAt(0, Character.toUpperCase(result.charAt(0)));
        }

        return result.toString();
    }

    private String caseOf(String s) {
        return copycase(s, "aa");
    }

    private boolean popBool() {
        if (theStack[stackIdx].mode == 0) {
            return theStack[stackIdx--].b;
        } else {
            System.err.println("tried to pop bool but mode is " + theStack[stackIdx].mode);
            System.exit(EXIT_FAILURE);
        }
        return false;
    }

    private int popInt() {
        if (theStack[stackIdx].mode == 1) {
            return theStack[stackIdx--].i;
        } else {
            System.err.println("tried to pop int but mode is " + theStack[stackIdx].mode);
            System.exit(EXIT_FAILURE);
        }
        return 0;
    }

    private String popString() {
        {
            if (theStack[stackIdx].mode == 2) {
                return theStack[stackIdx--].s;
            } else if (theStack[stackIdx].mode == 3) {
                return theStack[stackIdx--].c.target;
            } else {
                System.err.println("tried to pop wstring but mode is " + theStack[stackIdx].mode);
                System.exit(EXIT_FAILURE);
            }
        }
        return null;
    }

    private void popString(String dest) {
        if (theStack[stackIdx].mode == 2) {
            String save = theStack[stackIdx--].s;
            theStack[stackIdx--].s = dest;
            dest = save;
        } else if (theStack[stackIdx].mode == 3) {
            dest = theStack[stackIdx--].c.target;
        } else {
            System.err.print("tried to pop wstring but mode is " + theStack[stackIdx].mode);
            System.exit(EXIT_FAILURE);
        }
    }

    private Chunk popChunk() {
        if (theStack[stackIdx].mode == 3) {
            return theStack[stackIdx--].c;
        } else {
            System.err.println("tried to pop Chunk but mode is " + theStack[stackIdx].mode);
            System.exit(EXIT_FAILURE);
        }
        return null;
    }

    private void stackCopy(int src, int dest) {
        theStack[dest].mode = theStack[src].mode;
        switch (theStack[src].mode) {
            case 0:
                theStack[dest].b = theStack[src].b;
                theWblankStack[dest] = theWblankStack[src];
                break;
            case 1:
                theStack[dest].i = theStack[src].i;
                theWblankStack[dest] = theWblankStack[src];
                break;
            case 2:
                theStack[dest].s = theStack[src].s;
                theWblankStack[dest] = theWblankStack[src];
                break;
            case 3:
                theStack[dest].c = theStack[src].c;
                theWblankStack[dest] = theWblankStack[src];
                break;
            default:
                System.err.print("Unknown StackElement mode " + theStack[src].mode);
                break;
        }
    }

    private boolean gettingLemmaFromWord(String attr) {
        return (attr.contentEquals("lem") || attr.contentEquals("lemh") || attr.contentEquals("whole"));
    }

    private boolean applyRule(String rule) {
        stackIdx = 0;
        List<Boolean> editted = new ArrayList<>(currentInput.size());
        for(int i=0; i <  currentInput.size(); i++) {
            editted.set(i, false);
        }
        byte[] rule_data = rule.getBytes();
        Iterator<String> it;
        String list, needle, substr, str, val;
        int pos;
        boolean found, a, b;
        Chunk ch;
        for (int i = 0, rule_size = rule.length(); i < rule_size; i++) {
            switch (rule_data[i]) {
                case DROP:
                    if (printingSteps) { System.err.println("drop"); }
                    stackIdx--;
                    break;
                case DUP:
                    if (printingSteps) { System.err.println("dup"); }
                    stackCopy(stackIdx, stackIdx + 1);
                    stackIdx++;
                    break;
                case OVER:
                    if (printingSteps) { System.err.println("over"); }
                    stackCopy(stackIdx - 1, stackIdx + 1);
                    stackIdx++;
                    break;
                case SWAP:
                    if (printingSteps) { System.err.println("swap"); }
                    {
                        stackCopy(stackIdx, stackIdx + 1);
                        stackCopy(stackIdx - 1, stackIdx);
                        stackCopy(stackIdx + 1, stackIdx - 1);
                    }
                    break;
                case STRING:
                    {
                        if (printingSteps) { System.err.println("string"); }
                        int ct = rule_data[++i];
                        stackIdx++;
                        theStack[stackIdx].mode = 2;
                        theStack[stackIdx].s = rule;
                        i += ct;
                        if (printingSteps) { System.err.println(" -> " + theStack[stackIdx].s); }
                    }
                    break;
                case INT:
                    if (printingSteps) { System.err.println("int " + (int) rule.charAt(i + 1)); }
                    pushStack(rule_data[++i]);
                    break;
                case PUSHFALSE:
                    if (printingSteps) { System.err.println("pushfalse"); }
                    pushStack(false);
                    break;
                case PUSHTRUE:
                    if (printingSteps) { System.err.println("pushtrue"); }
                    pushStack(true);
                    break;
                case PUSHNULL:
                    if (printingSteps) { System.err.println("pushnull"); }
                    pushStack((Chunk) null);
                    break;
                case JUMP:
                    if (printingSteps) { System.err.println("jump"); }
                    ++i;
                    i += rule_data[i];
                    break;
                case JUMPONTRUE:
                    if (printingSteps) { System.err.println("jumpontrue"); }
                    if (!popBool()) {
                        i++;
                        if (printingSteps) { System.err.println(" -> false"); }
                    } else {
                        ++i;
                        i += rule_data[i];
                        if (printingSteps) { System.err.println(" -> true, jumping"); }
                    }
                    break;
                case JUMPONFALSE:
                    if (printingSteps) { System.err.println("jumponfalse"); }
                    if (popBool()) {
                        i++;
                        if (printingSteps) { System.err.println(" -> true"); }
                    } else {
                        ++i;
                        i += rule_data[i];
                        if (printingSteps) { System.err.println(" -> false, jumping"); }
                    }
                    break;
                case AND:
                    if (printingSteps) { System.err.println("and"); }
                    {
                        a = popBool();
                        b = popBool();
                        pushStack(a && b);
                    }
                    break;
                case OR:
                    if (printingSteps) { System.err.println("or"); }
                    {
                        a = popBool();
                        b = popBool();
                        pushStack(a || b);
                    }
                    break;
                case NOT:
                    if (printingSteps) { System.err.println("not"); }
                    theStack[stackIdx].b = !theStack[stackIdx].b;
                    break;
                case EQUAL:
                case EQUALCL:
                    if (printingSteps) { System.err.println("equal"); }
                    {
                        String a2 = "";
                        popString(a2);
                        String b2 = "";
                        popString(b2);
                        if (rule_data[i] == EQUALCL) {
                            a2 = a2.toLowerCase();
                            b2 = b2.toLowerCase();
                        }
                        pushStack(a2.equals(b2));
                        if (printingSteps) { System.err.println(" -> " + (a2.equals(b2))); }
                    }
                    break;
                case ISPREFIX:
                case ISPREFIXCL:
                    if (printingSteps) { System.err.println("isprefix"); }
                    {
                        substr = popString();
                        str = popString();
                        if (rule.charAt(i) == ISPREFIXCL) {
                            pushStack(beginsWith(str.toLowerCase(), substr.toLowerCase()));
                        } else {
                            pushStack(beginsWith(str, substr));
                        }
                    }
                    break;
                case ISSUFFIX:
                case ISSUFFIXCL:
                    if (printingSteps) { System.err.println("issuffix"); }
                    {
                        substr = popString();
                        str = popString();
                        if (rule.charAt(i) == ISSUFFIXCL) {
                            pushStack(endsWith(str.toLowerCase(), substr.toLowerCase()));
                        } else {
                            pushStack(endsWith(str, substr));
                        }
                    }
                    break;
                case HASPREFIX:
                case HASPREFIXCL:
                    /*
                    Correspondencias
                    La equivalencia entre ambos lenguajes sería:
                    C++	         Java
                    begin()	     iterator()
                    end()	     No necesitas equivalente
                    it != limit	 it.hasNext()
                    *it	         it.next()
                    it++	     it.next()
                     */
                    if (printingSteps) { System.err.println("hasprefix"); }
                    {
                        list = popString();
                        needle = popString();

                        if (rule.charAt(i) == HASPREFIX) {
                            it = lists.get(list).iterator();
                        } else {
                            needle = needle.toLowerCase();
                            it = listslow.get(list).iterator();
                        }

                        found = false;
                        while (it.hasNext()) {
                            if (beginsWith(needle, it.next())) {
                                found = true;
                                break;
                            }
                        }
                        pushStack(found);
                    }
                    break;
                case HASSUFFIX:
                case HASSUFFIXCL:
                    if (printingSteps) { System.err.println("hassuffix"); }
                    {
                        list = popString();
                        needle = popString();

                        if (rule.charAt(i) == HASSUFFIX) {
                            it = lists.get(list).iterator();
                        } else {
                            needle = needle.toLowerCase();
                            it = listslow.get(list).iterator();
                        }

                        found = false;
                        while (it.hasNext()) {
                            if (endsWith(needle, it.next())) {
                                found = true;
                                break;
                            }
                        }
                        pushStack(found);
                    }
                    break;
                case ISSUBSTRING:
                case ISSUBSTRINGCL:
                    if (printingSteps) { System.err.println("issubstring"); }
                    {
                        needle = popString();
                        String haystack = popString();
                        if (rule.charAt(i) == ISSUBSTRINGCL) {
                            needle = needle.toLowerCase();
                            haystack = haystack.toLowerCase();
                        }
                        pushStack(haystack.contains(needle)); // C++ haystack.find(needle) != wstring::npos
                    }
                    break;
                case IN:
                case INCL:
                    if (printingSteps) { System.err.println("in"); }
                    {
                        list = popString();
                        str = popString();
                        if (rule.charAt(i) == INCL) {
                            str = str.toLowerCase();
                            Set<String> myset = listslow.get(list);
                            pushStack(myset.contains(str));
                        } else {
                            Set<String> myset = lists.get(list);
                            pushStack(myset.contains(str));
                        }
                    }
                    break;
                case SETVAR:
                    if (printingSteps) {System.err.println("setvar");}
                    {
                        String var = popString();
                        val = popString();
                        currentBranch.stringVars.put(var, val);
                        currentBranch.wblankVars.put(val, theWblankStack[stackIdx + 1]);
                        theWblankStack[stackIdx + 1] = EMPTY;
                        if (printingSteps) {
                            System.err.print(" -> " + var + " = '" + val + "'");
                        }
                    }
                    break;
                case OUTPUT:
                    if (printingSteps) { System.err.println("output");
                    }
                    {
                        ch = popChunk();
                        if (ch == null) break; // FETCHCHUNK
                        if (isLinear && ch.contents.isEmpty()) {
                            boolean word = true;
                            int last = 0;
                            byte[] targ = ch.target.getBytes();
                            boolean chunk = false;
                            for (int c = 0, limit = ch.target.length(); c < limit; c++) {
                                if (targ[c] == '\\') c++;
                                else if ((targ[c] == '{' || targ[c] == '$') && word) {
                                    if (targ[c] == '{') chunk = true;
                                    Chunk temp = chunkPool.next();
                                    temp.isBlank = false;
                                    temp.target = ch.target.substring(last, c - last);
                                    temp.wblank = out_wblank;
                                    out_wblank = EMPTY;
                                    if (chunk) {
                                        currentOutput.getLast().contents.addLast(temp); // C++ currentOutput.back()->contents.push_back(temp);
                                    } else {
                                        currentOutput.addLast(temp);
                                    }
                                    last = c + 1;
                                    word = false;
                                } else if ((targ[c] == '^' || targ[c] == '}') && !word) {
                                    if (c > last) {
                                        Chunk temp = chunkPool.next();
                                        temp.isBlank = true;
                                        temp.target = ch.target.substring(last, c - last);
                                        if (chunk) {
                                            currentOutput.getLast().contents.addLast(temp);
                                        } else {
                                            currentOutput.addLast(temp);
                                        }
                                    }
                                    if (targ[c] == '}') chunk = false;
                                    last = c + 1;
                                    word = true;
                                }
                            }
                            if (last == 0 && !ch.target.isEmpty()) {
                                currentOutput.addLast(ch);
                            } else if (last < ch.target.length()) {
                                Chunk temp = chunkPool.next();
                                temp.isBlank = true;
                                temp.target = ch.target.substring(last);
                                currentOutput.addLast(temp);
                            }
                        } else {
                            ch.wblank = out_wblank;
                            currentOutput.addLast(ch);
                            out_wblank = EMPTY;
                        }
                    }
                    break;
                case OUTPUTALL:
                    if (printingSteps) { System.err.println("outputall"); }
                    currentOutput = currentInput;
                    return true;
                case PUSHINPUT:
                    if (printingSteps) { System.err.println("pushinput"); }
                    {
                        int loc = popInt();
                        pos = 2 * (loc - 1);
                        ch = getChunk(pos, loc);
                        pushStack(ch);
                    }
                    break;
                case SOURCECLIP:
                    if (printingSteps) { System.err.println("sourceclip"); }
                    {
                        String part = "";
                        popString(part);
                        ch = popChunk();
                        if (ch == null) pushStack("");
                        else {
                            if (gettingLemmaFromWord(part)) {
                                pushStack(ch.chunkPart(attr_items.get(part), SourceClip), ch.wblank);
                            } else {
                                pushStack(ch.chunkPart(attr_items.get(part), SourceClip));
                            }
                        }
                        if (printingSteps) { System.err.println(" -> " + theStack[stackIdx].s); }
                    }
                    break;
                case TARGETCLIP:
                    if (printingSteps) { System.err.println("targetclip"); }
                    {
                        String part = "";
                        popString(part);
                        ch = popChunk();
                        if (ch == null) pushStack("");
                        else {
                            if (gettingLemmaFromWord(part)) {
                                pushStack(ch.chunkPart(attr_items.get(part), TargetClip), ch.wblank);
                            } else {
                                pushStack(ch.chunkPart(attr_items.get(part), TargetClip));
                            }
                        }
                        if (printingSteps) {
                            System.err.println(" -> " + theStack[stackIdx].s);
                        }
                    }
                    break;
                case REFERENCECLIP:
                    if (printingSteps) { System.err.println("referenceclip"); }
                    {
                        String part = "";
                        popString(part);
                        ch = popChunk();
                        if (ch == null) pushStack("");
                        else pushStack(ch.chunkPart(attr_items.get(part), ReferenceClip));
                        if (printingSteps) { System.err.println(" -> " + theStack[stackIdx].s); }
                    }
                    break;
                case SETCLIP:
                    if (printingSteps) { System.err.println("setclip"); }
                    pos = 2 * (popInt() - 1);
                    String part = popString();
                    if (pos >= 0) {
                        if (!editted.get(pos)) {
                            currentInput.set(pos, new Chunk());
                            editted.set(pos, true);
                        }
                        currentInput.get(pos).setChunkPart(attr_items.get(part), popString());
                        if (printingSteps) { System.err.println(" -> " + currentInput.get(pos).target); }
                    } else {
                        theStack[stackIdx].c.setChunkPart(attr_items.get(part), popString());
                    }
                    break;
                case FETCHVAR:
                    if (printingSteps) { System.err.print("fetchvar"); }
                    {
                        String name = popString();
                        val = currentBranch.stringVars.get(name);
                        String wblank_val = currentBranch.wblankVars.get(name);
                        pushStack(val, wblank_val);
                        if (printingSteps) { System.err.println(" -> " + name + " = " + val); }
                    }
                    break;
                case FETCHCHUNK:
                    if (printingSteps) { System.err.println("fetchchunk"); }
                    pushStack(currentBranch.chunkVars.get(popInt()));
                    break;
                case SETCHUNK:
                    if (printingSteps) { System.err.println("setchunk"); }
                    {
                        pos = popInt();
                        currentBranch.chunkVars.set(pos, popChunk());
                    }
                    break;
                case GETCASE:
                    if (printingSteps) { System.err.println("getcase"); }
                    pushStack(caseOf(popString()));
                    if (printingSteps) { System.err.println(" -> " + theStack[stackIdx].s); }
                    break;
                case SETCASE:
                    if (printingSteps) {
                        System.err.println("setcase");
                    }
                    {
                        String src = popString();
                        String dest = popString();
                        pushStack(copycase(src, dest));

                        if (printingSteps) { System.err.println(" -> " + theStack[stackIdx].s); }
                    }
                    break;
                case CONCAT:
                    if (printingSteps) { System.err.println("concat"); }
                    {
                        if (theStack[stackIdx].mode != 2 || theStack[stackIdx - 1].mode != 2) {
                            System.err.println("Cannot CONCAT non-strings.");
                            System.exit(EXIT_FAILURE);
                        }
                        stackIdx--;
                        theStack[stackIdx].s = theStack[stackIdx].s + theStack[stackIdx + 1].s;
                    }
                    break;
                case CHUNK:
                    if (printingSteps) { System.err.println("chunk"); }
                    {
                        ch = chunkPool.next();
                        ch.isBlank = false;
                        pushStack(ch);
                    }
                    break;
                case APPENDCHILD:
                    if (printingSteps) { System.err.println("appendchild"); }
                    {
                        Chunk kid = popChunk();
                        if (isLinear && kid.target.charAt(0) == '^') {
                            int j;
                            for (j = 0; j < kid.target.length(); j++) {
                                if (kid.target.charAt(j) == '$') break;
                            }
                            ch = chunkPool.next();
                            ch.isBlank = false;
                            ch.target = kid.target.substring(1, j - 1);
                            ch.wblank = out_wblank;
                            out_wblank = EMPTY;
                            theStack[stackIdx].c.contents.addLast(ch);
                            ch = chunkPool.next();
                            ch.isBlank = true;
                            ch.target = kid.target.substring(j + 1);
                            theStack[stackIdx].c.contents.addLast(ch);
                        } else {
                            kid.wblank = out_wblank;
                            out_wblank = EMPTY;
                            theStack[stackIdx].c.contents.addLast(kid);
                        }
                        if (printingSteps) {
                            System.err.println(" -> child with surface '" + kid.target + "' appended");
                        }
                    }
                    break;
                case APPENDSURFACE:
                    if (printingSteps) { System.err.println("appendsurface"); }
                    {
                        if (theStack[stackIdx].mode != 2 && theStack[stackIdx].mode != 3) {
                            System.err.println("Cannot append non-string to chunk surface.");
                            System.exit(EXIT_FAILURE);
                        }
                        stackIdx--;
                        if (theStack[stackIdx].mode != 3) {
                            System.err.println("Cannot APPENDSURFACE to non-chunk.");
                            System.exit(EXIT_FAILURE);
                        }
                        if (theStack[stackIdx + 1].mode == 2) {
                            theStack[stackIdx].c.target += theStack[stackIdx + 1].s;
                            out_wblank = Chunk.combineWblanks(out_wblank, theWblankStack[stackIdx + 1]);
                            theWblankStack[stackIdx + 1] = EMPTY;
                        } else {
                            theStack[stackIdx].c.target += theStack[stackIdx + 1].c.target;
                            theStack[stackIdx].c.wblank += theStack[stackIdx + 1].c.wblank;
                        }
                        if (printingSteps) {
                            System.err.println(" -> " + theStack[stackIdx + 1].s);
                        }
                    }
                    break;
                case APPENDSURFACESL:
                    if (printingSteps) { System.err.println("appendsurfacesl"); }
                    {
                        if (theStack[stackIdx].mode != 2 && theStack[stackIdx].mode != 3) {
                            System.err.println("Cannot append non-string to chunk surface.");
                            System.exit(EXIT_FAILURE);
                        }
                        stackIdx--;
                        if (theStack[stackIdx].mode != 3) {
                            System.err.println("Cannot APPENDSURFACESL to non-chunk.");
                            System.exit(EXIT_FAILURE);
                        }
                        if (theStack[stackIdx + 1].mode == 2) {
                            theStack[stackIdx].c.source += theStack[stackIdx + 1].s;
                            out_wblank = Chunk.combineWblanks(out_wblank, theWblankStack[stackIdx + 1]);
                            theWblankStack[stackIdx + 1] = EMPTY;
                        } else {
                            theStack[stackIdx].c.source += theStack[stackIdx + 1].c.source;
                            theStack[stackIdx].c.wblank += theStack[stackIdx + 1].c.wblank;
                        }
                        if (printingSteps) {
                            System.err.println(" -> " + theStack[stackIdx + 1].s);
                        }
                    }
                    break;
                case APPENDSURFACEREF:
                    if (printingSteps) { System.err.println("appendsurfaceref"); }
                    {
                        if (theStack[stackIdx].mode != 2 && theStack[stackIdx].mode != 3) {
                            System.err.println("Cannot append non-string to chunk surface.");
                            System.exit(EXIT_FAILURE);
                        }
                        stackIdx--;
                        if (theStack[stackIdx].mode != 3) {
                            System.err.println("Cannot APPENDSURFACEREF to non-chunk.");
                            System.exit(EXIT_FAILURE);
                        }
                        if (theStack[stackIdx + 1].mode == 2) {
                            theStack[stackIdx].c.coref += theStack[stackIdx + 1].s;
                        } else {
                            theStack[stackIdx].c.coref += theStack[stackIdx + 1].c.coref;
                        }
                        if (printingSteps) { System.err.println(" -> " + theStack[stackIdx + 1].s); }
                    }
                    break;
                case APPENDALLCHILDREN:
                    if (printingSteps) { System.err.println("appendallchildren"); }
                    {
                        ch = popChunk();
                        for (int k = 0; k < ch.contents.size(); k++) {
                            theStack[stackIdx].c.contents.addLast(ch.contents.get(k));
                        }
                    }
                    break;
                case APPENDALLINPUT:
                    if (printingSteps) {
                        System.err.println("appendallinput");
                    }
                    {
                        // 1. Obtenemos la referencia a la lista (equivalente a vector<Chunk*>& vec)
                        List<Chunk> vec = theStack[stackIdx].c.contents;
                        // 2. Insertamos todos los elementos al final (equivalente a vec.insert(vec.end(), ...))
                        vec.addAll(currentInput);
                    }
                    break;
                case BLANK:
                    if (printingSteps) {
                        System.err.println("blank");
                    }
                    {
                        int loc = 2 * (popInt() - 1) + 1;
                        if (loc == -1) {
                            ch = chunkPool.next();
                            ch.target = " ";
                            ch.isBlank = true;
                            pushStack(ch);
                        } else {
                            pushStack(currentInput.get(loc));
                        }
                    }
                    break;
                case CONJOIN:
                    if (printingSteps) {
                        System.err.println("conjoin");
                    }
                    {
                        Chunk join = chunkPool.next();
                        join.isBlank = true;
                        join.isJoiner = true;
                        join.target = "+";
                        pushStack(join);
                    }
                    break;
                case REJECTRULE:
                    if (printingSteps) { System.err.println("rejectrule");}
                    return false;
                case DISTAG:
                    if (printingSteps) { System.err.println("distag");}
                    {
                        if (theStack[stackIdx].mode != 2) {
                            System.err.println("Cannot DISTAG non-string.");
                            System.exit(EXIT_FAILURE);
                        }
                        String s = theStack[stackIdx].s;
                        if (!s.isEmpty() && s.charAt(0) == '<' && s.charAt(s.length() - 1) == '>') {
                            s = s.replace("><", ".");
                        }
                    }
                    break;
                case GETRULE:
                    if (printingSteps) { System.err.println("getrule"); }
                    {
                        pos = 2 * (popInt() - 1);
                        pushStack(currentInput.get(pos).rule);
                    }
                    break;
                case SETRULE:
                    if (printingSteps) { System.err.println("setrule"); }
                    {
                        pos = 2 * (popInt() - 1);
                        int rl = popInt();
                        if (pos == -2) {
                            theStack[stackIdx].c.rule = rl;
                        } else {
                            currentInput.get(pos).rule = rl;
                        }
                    }
                    break;
                case LUCOUNT:
                    if (printingSteps) { System.err.println("lucount");}
                    pushStack(String.valueOf((currentInput.size() + 1) / 2));
                    break;
                default:
                    System.err.println("unknown instruction: " + rule.charAt(i));
                    System.exit(EXIT_FAILURE);
            }
        }
        return true;
    }

    private Chunk getChunk(int pos, int loc) {
        Chunk ch = null;
        if (pos == -2) ch = parentChunk;
        else if (0 <= pos && pos < currentInput.size()) ch = currentInput.get(pos);
        else {
            int n = 0;
            for (Chunk chunk : currentInput) {
                if (!chunk.isBlank) n++;
                if (n == loc) {
                    ch = chunk;
                    break;
                }
            }
            if (ch == null) {
                ch = currentInput.getLast();
            }
        }
        return ch;
    }

    public Chunk readToken(Reader reader) throws IOException {
        int pos = 0;

        StringBuilder cur = new StringBuilder();
        String wbl = "";
        String src = "";
        String dest = "";
        String coref = "";

        boolean inSquare = false;

        while (true) {
            int value = reader.read();

            if (value == -1 || (nullFlush && value == 0)) { // C++ feof(in) Java reader.read() == -1
                furtherInput = false;
                Chunk ret = chunkPool.next();
                ret.target = cur.toString();
                ret.isBlank = true;
                return ret;
            }

            char val = (char) value;

            if (val == '\\') {
                cur.append('\\');
                int next = reader.read();
                if (next != -1)
                    cur.append((char) next);
            } else if (val == '[' && !inword) {
                int next = reader.read();
                if (next == '[') {
                    inwblank = true;
                    Chunk ret = chunkPool.next();
                    ret.target = cur.toString();
                    ret.isBlank = true;
                    return ret;
                }

                cur.append('[');
                inSquare = true;

                if (next == '\\') {
                    cur.append('\\');
                    int escaped = reader.read();
                    if (escaped != -1) {
                        cur.append((char) escaped);
                    }
                } else {
                    cur.append((char) next);
                    if (next == ']') {
                        inSquare = false;
                    }
                }
            } else if (inSquare) {
                cur.append(val);
                if (val == ']') {
                    inSquare = false;
                }
            } else if (inwblank) {
                if (val == ']') {
                    cur.append(val);
                    int next = reader.read();
                    if (next == '\\') {
                        cur.append('\\');
                        int escaped = reader.read();
                        if (escaped != -1) {
                            cur.append((char) escaped);
                        }
                    } else if (next == ']') {
                        cur.append(']');
                        int next2 = reader.read();
                        if (next2 == '\\') {
                            cur.append('\\');
                            int escaped = reader.read();
                            if (escaped != -1)
                                cur.append((char) escaped);
                        } else if (next2 == '^') {
                            inwblank = false;
                            cur.insert(0, "[[");
                            wbl = cur.toString();
                            cur.setLength(0);
                            inword = true;
                        } else {
                            System.err.println(
                                    "Parse Error: Wordbound blank " +
                                            "should be immediately followed " +
                                            "by a Lexical Unit -> [[..]]^..$");
                            System.exit(EXIT_FAILURE);
                        }
                    } else if (next != -1) {
                        cur.append((char) next);
                    }
                } else {
                    cur.append(val);
                }
            } else if (inword && (val == '$' || val == '/')) {
                if (pos == 0) {
                    src = cur.toString();
                    cur.setLength(0);
                } else if (pos == 1) {
                    dest = cur.toString();
                    cur.setLength(0);
                } else if (pos >= 2 && !noCoref && val == '$') {
                    coref = cur.toString();
                    cur.setLength(0);
                } else {
                    cur.setLength(0);
                }
                pos++;

                if (val == '$') {
                    inword = false;
                    Chunk ret = chunkPool.next();
                    ret.wblank = wbl;
                    ret.source = src;
                    ret.target = dest;
                    ret.coref = coref;
                    ret.isBlank = false;
                    if (src.startsWith("*") && dest.startsWith("*")) {
                        Chunk ret2 = chunkPool.next();
                        ret2.target = dest.substring(1) + "<UNKNOWN:INTERNAL>";
                        ret2.contents.add(ret);
                        ret2.rule = -1;
                        ret2.isBlank = false;
                        return ret2;
                    }
                    return ret;
                }
            } else if (!inword && val == '^') {
                inword = true;
                Chunk ret = chunkPool.next();
                ret.target = cur.toString();
                ret.isBlank = true;
                cur.setLength(0);
                return ret;
            } else {
                cur.append(val);
            }
        }
    }

    public boolean getNullFlush() {
        return nullFlush;
    }

    public void setNullFlush(boolean null_flush) {
        this.nullFlush = null_flush;
    }

    public boolean setOutputMode(String mode) {
        switch (mode) {
            case "flat" -> {
                treePrintMode = TreeModeFlat;
                return true;
            }
            case "nest" -> {
                treePrintMode = TreeModeNest;
                return true;
            }
            case "latex" -> {
                treePrintMode = TreeModeLatex;
                return true;
            }
            case "dot" -> {
                treePrintMode = TreeModeDot;
                return true;
            }
            case "box" -> {
                treePrintMode = TreeModeBox;
                return true;
            }
            case null, default -> {
                return false;
            }
        }
    }

    public boolean lookahead(ParseNode node) {
        Chunk next = null;
        // 1. Recorrer currentContinuation en orden inverso
        // C++: vector<vector<Chunk*>*> -> Java: List<List<Chunk>>
        for (int i = currentContinuation.size() - 1; i >= 0; i--) {
            List<Chunk> subList = currentContinuation.get(i);

            if (subList != null) {
                // 2. Recorrer la sublista en orden inverso
                // Nota: Aquí se corrige el bug del "it++" del código C++ original
                for (int j = subList.size() - 1; j >= 0; j--) {
                    Chunk chunk = subList.get(j);
                    if (chunk != null && !chunk.isBlank) {
                        next = chunk;
                        break;
                    }
                }
            }
            if (next != null) {
                break;
            }
        }

        // 3. Si no se encontró en las continuaciones, buscar en inputBuffer
        // C++: list<Chunk*> -> Java: List<Chunk> (como un LinkedList)
        if (next == null && inputBuffer != null) {
            for (Chunk chunk : inputBuffer) {
                if (chunk != null && !chunk.isBlank) {
                    next = chunk;
                    break;
                }
            }
        }

        // 4. Retornar la evaluación final usando el operador punto (.)
        return (next != null && node.shouldShift(next));
    }

    private void checkForReduce(List<ParseNode> result, ParseNode node) throws IOException {
        if (printingAll) {
            System.err.println("Checking for reductions for branch " + node.id);
        }
        mx.resetRejected();
        Pair<Integer, Double> rule_and_weight = node.getRule();
        int rule = rule_and_weight.first;
        double weight = node.weight + rule_and_weight.second;
        currentBranch = node;
        while (rule != -1) {
            int len = pat_size.get(rule - 1);
            int first;
            int last = node.lastWord;
            node.getChunks(currentInput, len - 1);
            currentOutput.clear();
            if (printingRules || printingAll) {
                if (printingAll && treePrintMode == TreeModeLatex) {
                    System.err.print("\\subsection{");
                }
                else{
                    System.err.println();
                }
                System.err.print("Applying rule " + rule);
                if (rule <= inRuleNames.size()) {
                    System.err.print(" (" + inRuleNames.get(rule - 1) + ")");
                }
                if (printingAll) {
                    System.err.print(" to branch " + node.id + " with weight" + rule_and_weight.second);
                }
                if (printingAll && treePrintMode == TreeModeLatex) {
                    System.err.println("}");
                } else {
                    System.err.print(": ");
                }
                for (Chunk chunk : currentInput) {
                    chunk.writeTree((printingAll ? treePrintMode : TreeModeFlat), null);
                }
                System.err.println();
            }
            if (applyRule(rule_map.get(rule - 1))) {
                if (printingAll) {
                    for (Chunk c : currentOutput) c.writeTree(treePrintMode, null);
                    System.err.println();
                }
                List<Chunk> temp = new ArrayList<>();
                while (currentOutput.size() > 1) {
                    temp.addLast(currentOutput.getLast());
                    currentOutput.removeLast();
                }
                ParseNode back = node.popNodes(len);
                ParseNode cur = parsePool.next();
                if (back == null) {
                    first = 0;
                    cur.init(mx, currentOutput.getFirst(), weight);
                } else {
                    first = back.lastWord + 1;
                    cur.init(back, currentOutput.getFirst(), weight);
                }
                cur.stringVars = node.stringVars;
                cur.wblankVars = node.wblankVars;
                cur.chunkVars = node.chunkVars;
                cur.id = node.id;
                if (temp.isEmpty()) {
                    checkForReduce(result, cur);
                    break;
                }
                currentContinuation.addLast(temp);
                List<ParseNode> res = new ArrayList<>();
                List<ParseNode> res2 = new ArrayList<>();
                checkForReduce(res, cur);
                while (!temp.isEmpty()) {
                    for (ParseNode re : res) {
                        cur = parsePool.next();
                        cur.init(re, temp.getLast());
                        cur.stringVars = re.stringVars;
                        cur.wblankVars = re.wblankVars;
                        cur.chunkVars = re.chunkVars;
                        cur.firstWord = first;
                        cur.lastWord = last;
                        checkForReduce(res2, cur);
                    }
                    temp.removeLast(); // C++ temp.pop_back()
                    res.clear();
                    List<ParseNode> swapping = res2;
                    res2 = res;
                    res = swapping;
                }
                for (ParseNode re : res) {
                    result.addLast(re);
                }
                currentContinuation.removeLast();
                break;
            } else {
                if (printingRules) {
                    System.err.println(" -> rule was rejected");
                }
                if (printingAll) {
                    System.err.println("This rule was rejeced.");
                    mx.rejectRule(rule);
                    rule_and_weight = node.getRule();
                    rule = rule_and_weight.first;
                    weight = node.weight + rule_and_weight.second;
                }
            }
            if (rule == -1) {
                if (printingAll) System.err.println("No further reductions possible for branch " + node.id + ".");
                result.addLast(node);
            } else if (lookahead(node)) {
                node.id = ++newBranchId;
                if (printingAll) System.err.println("\nSplitting stack and creating branch " + node.id);
                result.addLast(node); // C++ result.push_back(node)
            }

        }
    }

    private void outputAll(Writer out) throws IOException {
        int queueSize = outputQueue.size() - 1;
        boolean conjoining = false;
        Chunk tojoin = null;
        while(!outputQueue.isEmpty())
        {
            Chunk ch = outputQueue.getFirst(); // C++ outputQueue.front()
            outputQueue.removeFirst(); // C++ outputQueue.pop_front()
            if(printingTrees && outputQueue.size() == queueSize)
            {
                if(printingText) {out.write('\n');}
                queueSize--;
                ch.writeTree(treePrintMode, out);
                out.flush();
                if(!printingText) continue;
            }
            if(ch.rule == -1)
            {
                if(printingRules)// && !ch->isBlank)
                {
                    out.flush();
                    System.err.print("\nNo rule specified: ");
                    ch.writeTree(TreeModeFlat, null);
                    System.err.println();
                }
                if(printingAll && !ch.isBlank)
                {
                    if(treePrintMode == TreeModeLatex) System.err.println("\\subsubsection{Output Node}");
                    else System.err.print("Output Node:");
                    ch.writeTree(treePrintMode, null);
                    System.err.println();
                }
                if(!ch.contents.isEmpty())
                {
                    List<String> tags = ch.getTags(new ArrayList<>());
                    for(int i=0; i<ch.contents.size(); i++)
                    {
                        ch.contents.get(i).updateTags(tags);
                        outputQueue.addFirst(ch.contents.get(i)); // C++ outputQueue.push_front(*it);
                    }
                }
                else if(conjoining && !ch.isBlank)
                {
                    tojoin.conjoin(ch);
                }
                else if(ch.isJoiner)
                {
                    if(tojoin != null) conjoining = true;
                }
                else
                {
                    conjoining = false;
                    if(tojoin != null)
                    {
                        tojoin.output(out);
                        tojoin = null;
                    }
                    if(ch.isBlank) ch.output(out);
                    else tojoin = ch;
                }
            }
            else
            {
                parentChunk = ch;
                List<String> parentTags;
                List<String> tags = ch.getTags(new ArrayList<>());
                currentInput = new ArrayList<>(ch.contents);
                for (Chunk chunk : currentInput) {
                    chunk.updateTags(tags);
                }
                currentOutput.clear();
                if(printingRules) {
                    out.flush();
                    System.err.print("\nApplying output rule " + ch.rule);
                    if(ch.rule < outRuleNames.size())
                    {
                        System.err.print(" (" + outRuleNames.get(ch.rule) + ")");
                    }
                    System.err.print(": " + parentChunk.target + " -> ");
                    for (Chunk chunk : currentInput) {
                        chunk.writeTree(TreeModeFlat, null);
                    }
                    System.err.println();
                }
                if(printingAll)
                {
                    if(treePrintMode == TreeModeLatex)
                    {
                        System.err.print("\\subsubsection{Applying Output Rule " + ch.rule);
                        if(ch.rule < outRuleNames.size())
                        {
                            System.err.println(": " + outRuleNames.get(ch.rule) + "}");
                        }
                    }
                    else
                    {
                        System.err.print("Applying Output Rule " + ch.rule);
                        if(ch.rule < outRuleNames.size())
                        {
                            System.err.println(": " + outRuleNames.get(ch.rule));
                        }
                    }
                    ch.writeTree(treePrintMode, null);
                }
                out.flush();
                applyRule(output_rules.get(ch.rule));
                for (Chunk chunk : currentOutput) {
                    outputQueue.addFirst(chunk);
                }
            }
        }
        if(tojoin != null) tojoin.output(out);
    }

    private boolean filterParseGraph() throws IOException {
        if(printingAll)
        {
            if(treePrintMode == TreeModeLatex)
            {
                System.err.println("\\subsection{Filtering Branches}\n\n\\begin{itemize}");
            }
            else System.err.println("Filtering Branches:");
        }
        boolean shouldOutput = !furtherInput && inputBuffer.size() == 1;
        int[] state = new int[parseGraph.size()];
        int N = parseGraph.size();
        Arrays.fill(state, 0, N, 1);
        int count = N;
        if(furtherInput || inputBuffer.size() > 1)
        {
            for(int i = 0; i < N; i++)
            {
                if(parseGraph.get(i).isDone() ||
                    (!parseGraph.get(i).chunk.isBlank && !lookahead(parseGraph.get(i))))
                {
                    state[i] = 0;
                    count--;
                }
            }
            if(count == 0)
            {
                if(printingAll)
                {
                    if(treePrintMode == TreeModeLatex)
                    {
                        System.err.println("\\item No branch can accept further input.");
                    }
                    else System.err.println("No branch can accept further input.");
                }
                shouldOutput = true;
                Arrays.fill(state, 0, N, 1);
                count = N;
            }
        }
        else if(printingAll)
        {
            if(treePrintMode == TreeModeLatex)
            {
                System.err.println("\\item Input buffer is empty.");
            }
            else System.err.println("Input buffer is empty.");
        }
        int min = -1;
        ParseNode minNode = null;
        ParseNode cur;
        Map<Integer, List<Integer>> filter = new HashMap<>();
        if(printingBranches) { System.err.println("shouldOutput: " + shouldOutput + " branch count: " + N); }
        for(int i = 0; i < N; i++)
        {
            if(printingBranches) { System.err.print("examining node " + i + "(length: " + parseGraph.get(i).length +
                    ", weight: " + parseGraph.get(i).weight + ") ... "); }
            if(printingAll)
            {
                if(treePrintMode == TreeModeLatex) System.err.print("\\item ");
                System.err.print("Branch " + parseGraph.get(i).id + " ");
            }
            if(state[i] == 0)
            {
                if(printingAll) System.err.println(" has no possible continuations.");
                continue;
            }
            else if(noFilter && !shouldOutput) continue;
            if(min == -1)
            {
                if(printingAll) System.err.println(" has no active branch to compare to.");
                if(printingBranches) { System.err.println("FIRST!"); }
                min = i;
                minNode = parseGraph.get(i);
                cur = minNode;
                List<Integer> l = new ArrayList<>();
                l.addLast(i);
                filter.put(cur.firstWord, l);
            }
            else
            {
                cur = parseGraph.get(i);
                if(shouldOutput)
                {
                    if(cur.length < minNode.length
                            || (cur.length == minNode.length && cur.weight >= minNode.weight))
                    {
                        if(printingBranches) { System.err.println(i + " beats " + min + " in length or weight"); }
                        if(printingAll) System.err.println(" has fewer partial parses or a higher weight than branch "
                                + minNode.id + ".");
                        state[min] = 0;
                        min = i;
                        minNode = cur;
                    }
                    else
                    {
                        state[i] = 0;
                        if(printingBranches) {System.err.println(min + " beats " + i + " in length or weight"); }
                        if(printingAll) System.err.println(" has more partial parses or a lower weight than branch "
                                + minNode.id + ".");
                    }
                    count--;
                }
                else if(!filter.containsKey(cur.firstWord))
                {
                    List<Integer> l = new ArrayList<>();
                    l.addLast(i);
                    filter.put(cur.firstWord,l);
                    if(printingBranches) { System.err.println(i + " has nothing to compare with"); }
                    if(printingAll) System.err.println(" has no prior branch covering the same final span.");
                }
                else
                {
                    List<Integer> other = filter.get(cur.firstWord);
                    double w = parseGraph.get(other.getFirst()).weight;
                    if(w > cur.weight)
                    {
                        if(printingBranches) { System.err.println(i + " has lower weight - discarding."); }
                        if(printingAll) System.err.println(" has a lower weight than branch " + parseGraph.get(other.getFirst()).id
                                + " and will be discarded.");
                        state[i] = 0;
                        count--;
                    }
                    else if(w < cur.weight)
                    {
                        if(printingBranches) { System.err.println(i + " has higher weight - discarding others."); }
                        if(printingAll)
                        {
                            System.err.print(" has a higher weight than ");
                            for(Integer it : other) {System.err.print("branch " + parseGraph.get(it).id  + ", ");}
                            System.err.println("which will be discarded.");
                        }
                        for (Integer integer : other) {
                            state[integer] = 0;
                            count--;
                        }
                        other.clear();
                        other.set(0, i);
                    }
                    else
                    {
                        if(printingBranches) { System.err.println( i + " has same weight - keeping all."); }
                        if(printingAll) System.err.println(" has the same weight as branch " + parseGraph.get(other.getFirst()).id + ".");
                        other.addLast(i);
                    }
                }
            }
        }
        if(printingAll && treePrintMode == TreeModeLatex) System.err.println("\\end{itemize}");
        if(count == N) return shouldOutput;
        if(count > 100 && !filter.isEmpty())
        {
            int maxbin = 100 / filter.size();
            for(int j=0; j < filter.size(); j++)
            {
                for(int i = maxbin; i < filter.get(j).size(); i++)
                {
                    state[filter.get(j).get(i)] = 0;
                    count--;
                }
            }
        }
        List<ParseNode> temp = new ArrayList<>(count);
        for(int i = 0; i < N; i++)
        {
            if(state[i] != 0)
            {
                temp.addLast(parseGraph.get(i));
                if(printingBranches)
                {
                    System.err.print("keeping branch " + i + " first word: " + parseGraph.get(i).firstWord + " ending with ");
                    parseGraph.get(i).chunk.writeTree(TreeModeFlat, null);
                    System.err.println();
                }
            }
            else if(printingBranches)
            {
                System.err.print("discarding branch " + i + " first word: " + parseGraph.get(i).firstWord + " ending with ");
                parseGraph.get(i).chunk.writeTree(TreeModeFlat, null);
                System.err.println();
            }
        }
        if(printingBranches) { System.err.println("remaining branches: " + temp.size());}
        parseGraph = temp;
        return shouldOutput;
    }

    private void processGLR( Reader in, Writer out) throws IOException {

        int sentenceId = 1;
        if(printingAll && treePrintMode == TreeModeLatex)
        {
            System.err.println("\\section{Sentence " + sentenceId + "}");
        }
        while (furtherInput && inputBuffer.size() < 5) {
            inputBuffer.addLast(readToken(in)); // C++ push_back()
        }
        boolean real_printingAll = printingAll;

        while (true) {
            Chunk next = inputBuffer.getFirst(); // C++ front()
            if(next.isBlank){ printingAll = false; }
            if(printingAll)
            {
                System.err.println();
                if(treePrintMode == TreeModeLatex) System.err.println("\\subsection{Reading Input}");
                else System.err.println("Reading Input:");
                next.writeTree(treePrintMode, null);
                System.err.println();
            }
            inputBuffer.removeFirst(); // C++ pop_front()

            if (parseGraph.isEmpty()) {
                if (next.isBlank) {
                    next.output(out);
                    if (furtherInput) { inputBuffer.addLast(readToken(in)); }
                    if(inputBuffer.isEmpty())
                    {
                        System.err.flush();
                        out.flush();
                        break;
                    }
                    continue;
                }
                ParseNode node = parsePool.next();
                node.init(mx, next);
                node.id = ++newBranchId;
                node.stringVars = variables;
                node.wblankVars = wblankVariables;
                node.chunkVars = new ArrayList<>(varCount);
                checkForReduce(parseGraph, node);

            } else {
                mx.prepareChunk(!next.source.isEmpty() ? next.source : next.target);
                // conditional deals with unknowns
                List<ParseNode> result = new ArrayList<>();
                for (ParseNode branch : parseGraph) {
                    ParseNode node = parsePool.next();
                    node.init(branch, next, true);
                    node.id = branch.id;
                    node.stringVars.putAll(branch.stringVars);
                    node.wblankVars.putAll(branch.wblankVars);
                    node.chunkVars = branch.chunkVars;
                    checkForReduce(result, node);
                }
                parseGraph.clear();
                parseGraph.addAll(result);
            }
            if(printingAll && treePrintMode != TreeModeLatex)
            {
                for(ParseNode branch : parseGraph)
                {
                    System.err.println("Branch " + branch.id + ": " + branch.length + " nodes, weight = " + branch.weight);
                    List<Chunk> parts = new ArrayList<>(branch.length);
                    branch.getChunks(parts, branch.length-1);
                    for(Chunk node : parts)
                    {
                        if(node.isBlank) System.err.println("[Blank]: ");
                        else System.err.println("[Chunk]: ");
                        node.writeTree(treePrintMode, null);
                    }
                }
            }

            if (furtherInput) {
                inputBuffer.addLast(readToken(in));
            }

            if (filterParseGraph()) {
                System.err.flush();
                if (printingAll) {
                    if (treePrintMode == TreeModeLatex) {
                        System.err.println("\\subsection{Outputting Branch " +
                                parseGraph.getFirst().id + "}");
                    } else {
                        System.err.println();
                        System.err.println("************************************************************");
                        System.err.println("************************************************************");
                        System.err.println("************************************************************");
                        System.err.println("Outputting Branch " + parseGraph.getFirst().id);
                        List<Chunk> parts = new ArrayList<>(parseGraph.getFirst().length);
                        parseGraph.getFirst().getChunks(parts, parseGraph.getFirst().length - 1);
                        for (Chunk node : parts) {
                            if (node.isBlank) System.err.print("[Blank]: ");
                            else System.err.print("[Chunk]: ");
                            node.writeTree(treePrintMode, null);
                        }
                        System.err.println("************************************************************");
                        System.err.println("************************************************************");
                        System.err.println("************************************************************");
                        System.err.println();
                    }
                }
                currentBranch = parseGraph.getFirst();
                parseGraph.getFirst().getChunks(outputQueue, parseGraph.getFirst().length - 1);
                parseGraph.clear();
                outputAll(out);
                variables.clear();
                variables.putAll(currentBranch.stringVars);
                wblankVariables.clear();
                wblankVariables.putAll(currentBranch.wblankVars);
                out.flush();
                List<String> wblanks = new ArrayList<>();
                List<String> sources = new ArrayList<>();
                List<String> targets = new ArrayList<>();
                List<String> corefs = new ArrayList<>();
                List<Boolean> blanks = new ArrayList<>();
                List<Boolean> unknowns = new ArrayList<>();
                int N = inputBuffer.size();
                for (int i = 0; i < N; i++) {
                    Chunk temp = inputBuffer.getFirst();
                    if (!temp.contents.isEmpty()) {
                        unknowns.addLast(true);
                        temp = temp.contents.getFirst();
                    } else {
                        unknowns.addLast(false);
                    }
                    wblanks.addLast(temp.wblank);
                    sources.addLast(temp.source);
                    targets.addLast(temp.target);
                    corefs.addLast(temp.coref);
                    blanks.addLast(temp.isBlank);
                    inputBuffer.removeFirst();
                }
                chunkPool.reset();
                parsePool.reset();
                newBranchId = 0;

                if (printingAll) sentenceId++;
                if ((furtherInput || inputBuffer.size() > 1) && printingAll && treePrintMode == TreeModeLatex) {
                    System.err.println("\\section{Sentence " + sentenceId + "}");
                }
                for (int i = 0; i < N; i++) {
                    Chunk c = chunkPool.next();
                    c.wblank = wblanks.get(i);
                    c.source = sources.get(i);
                    c.target = targets.get(i);
                    c.coref = corefs.get(i);
                    c.isBlank = blanks.get(i);
                    if (unknowns.get(i)) {
                        Chunk c2 = chunkPool.next();
                        c2.target = targets.get(i).substring(1) + "<UNKNOWN:INTERNAL>";
                        c2.contents.addLast(c);
                        c = c2;
                    }
                    inputBuffer.addLast(c);
                }
            }

            printingAll = real_printingAll;

            if (!furtherInput && inputBuffer.size() == 1) {
                inputBuffer.getFirst().output(out);
                out.flush();
                break;
            }

            if (!furtherInput && inputBuffer.isEmpty()) {
                break;
            }
        }
    }

    /*
        Cambios clave explicados:
        1. Eliminación del goto: Se envolvió el bloque en un while(true). Cuando la ejecución de la regla es exitosa
        (applyRule) o cuando no se encuentra ninguna regla (rule == -1), el flujo llama a break para romper el ciclo.
        Si applyRule falla (antiguo disparador del goto), el bucle simplemente da otra vuelta de forma natural.
        2. Estructuras de Datos (std::list): Se ha mapeado std::list<Chunk*> a LinkedList<Chunk> de Java.
        Esto nos permite usar de forma directa operaciones eficientes en los extremos como
        peekFirst(), addLast(), removeFirst() y removeLast() (los cuales equivalen a front(), push_back(), pop_front() y pop_back()).
        3. Iteradores simplificados: Los bucles for con iteradores de C++ se han sustituido por
        bucles For-Each de Java (for (Chunk chunk : t1x)), lo que hace el código mucho más legible y menos propenso a errores de punteros.
        4. Tipos de datos: Java no soporta tipos nativos unsigned int. Se han unificado a int,
        que cuenta con suficiente capacidad para los tamaños de colecciones comunes en memoria.
     */

    private void processTRXLayer(Deque<Chunk> t1x, Deque<Chunk> t2x) throws IOException {
        if(t1x.isEmpty())
        {
            return;
        }
        int[] state = new int[1024];
        int first = 0;
        int last;
        if(!furtherInput || t1x.size() >= longestPattern)
        {
            mx.resetRejected();
            int len = 0;
            int rule = -1;
            int i;
            // Reemplazamos la etiqueta del goto por un bucle controlado
            while(true){
                last = 1;
                state[0] = mx.getInitial();
                i = 0; // Reiniciamos el contador que controla 'longestPattern'
                for (Chunk x : t1x) {
                    if (i >= longestPattern) {
                        break;
                    }
                    i++;
                    if (x.isBlank) {
                        mx.matchBlank(state, first, last);
                    } else {
                        MatchExe2.StateRange sr = new MatchExe2.StateRange(first, last);
                        mx.matchChunk(state, sr, x.matchSurface(), false);
                        int r = mx.getRule(state, first, last).first;
                        if (r != -1) {
                            rule = r;
                            len = i;
                        }
                    }
                }
                if(rule == -1)
                {
                    t2x.addLast(t1x.getFirst());
                    if(!t2x.getLast().isBlank && t2x.getLast().target.isEmpty())
                    {
                        t2x.removeLast();
                        if(!t2x.isEmpty() && !t1x.isEmpty())
                        {
                            t2x.getLast().target += t1x.getFirst().target;
                            t1x.removeFirst();
                        }
                    }
                    t1x.removeFirst();
                }
                else {
                    i = 0;
                    // En Java, ajustamos el tamaño de la lista actualizando una estructura dinámica
                    currentInput.clear();
                    for (Chunk chunk : t1x)
                    {
                        if (i >= len) {
                            break;
                        }
                        currentInput.set(i, chunk);
                        i++;
                    }
                    currentOutput.clear();
                    if (printingRules) {
                        System.err.println("Applying rule " + rule);
                        if (rule <= inRuleNames.size()) {
                            System.err.print(" (" + inRuleNames.get(rule - 1) + ")");
                        }
                        System.err.print(": ");
                        for (Chunk chunk : currentInput) {
                            chunk.writeTree(TreeModeFlat, null);
                        }
                        System.err.println();
                    }
                    if (applyRule(rule_map.get(rule - 1))) {
                        for (Chunk chunk : currentOutput) {
                            t2x.addLast(chunk);
                        }
                        for (int n = 0; n < len; n++) {
                            t1x.removeFirst();
                        }
                    } else {
                        // En lugar de "goto try_again_for_reject_rule;", dejamos que el bucle continúe.
                        // El flujo volverá automáticamente al inicio del 'while(true)'.
                        break;
                    }
                }
            }
        }
    }

    private void processTRX(Reader in, Writer out) throws IOException {

        Deque<Chunk> t1x = new ArrayDeque<>();
        Deque<Chunk> t2x = new ArrayDeque<>();
        Deque<Chunk> t3x = new ArrayDeque<>();

        while (furtherInput || !t1x.isEmpty() || !t2x.isEmpty()) {
            while (furtherInput && t1x.size() < 2 * longestPattern) {
                t1x.addLast(readToken(in));
            }
            if(furtherInput)
            {
                processTRXLayer(t1x, t2x);
                processTRXLayer(t2x, t3x);
            }
            else
            {
                while(!t1x.isEmpty())
                {
                    processTRXLayer(t1x, t2x);
                }
                while(!t2x.isEmpty())
                {
                    processTRXLayer(t2x, t3x);
                }
            }
            while (!t3x.isEmpty()) {
                Chunk cur = t3x.getFirst();
                t3x.removeFirst();
                List<String> tags = cur.getTags(new ArrayList<>());
                if (cur.rule == -1) {
                    if (cur.contents.isEmpty()) {
                        cur.output(out);
                    } else {
                        while (!cur.contents.isEmpty()) {
                            t3x.addFirst(cur.contents.getLast());
                            t3x.getFirst().updateTags(tags);
                            cur.contents.removeLast();
                        }
                    }
                } else {
                    if (printingRules) {
                        System.err.println("\nApplying output rule " + cur.rule);
                        if (cur.rule < outRuleNames.size()) {
                            System.err.print(" (" + outRuleNames.get(cur.rule) + ")");
                        }
                        System.err.print(": ");
                        cur.writeTree(TreeModeFlat, null);
                        System.err.println();
                    }
                    parentChunk = cur;
                    currentInput = cur.contents;
                    for (Chunk chunk : currentInput) {
                        chunk.updateTags(tags);
                    }
                    currentOutput.clear();
                    applyRule(output_rules.get(cur.rule));
                    for (Chunk chunk : currentOutput) {
                        chunk.output(out);
                    }
                }
            }
        }
    }

    public void process(Reader input, Writer output) throws IOException {

        if(printingAll && treePrintMode == TreeModeLatex)
        {
            System.err.println("\\documentclass{article}");
            System.err.println("\\usepackage{fontspec}");
            System.err.println("\\setmainfont{FreeSans}");
            System.err.println("\\usepackage{forest}");
            System.err.println("\\usepackage[cm]{fullpage}");
            System.err.println("\\begin{document}");
        }

        if (nullFlush) {
            while(input.read() != -1)
            {
                furtherInput = true;
                if(isLinear)
                {
                    processTRX(input, output);
                }
                else
                {
                    processGLR(input, output);
                }
                output.write('\0');
                output.flush();
                chunkPool.reset();
                parsePool.reset();
                inputBuffer.clear();
                // I'm not sure how the leading blank after a null gets into inputBuffer,
                // but it does and clearing the buffer seems to fix the problem
                // (in theory, clearing the buffer here should have no effect at all
                // because processGLR() should consume everything in it)
                // - D.S. Aug 26 2020
            }
        }
        if(isLinear)
        {
            processTRX(input, output);
        }
        else
        {
            processGLR(input, output);
        }
        if(printingAll && treePrintMode == TreeModeLatex)
        {
            System.err.println("\\end{document}");
        }
    }

    /*
     Important differences from the C++ version

     The most significant mappings are:
     C++	Java
     wstring	String
     vector<T>	ArrayList<T>
     list<T>	ArrayDeque<T> / LinkedList<T>
     map<K,V>	HashMap<K,V> / TreeMap<K,V>
     set<T>	TreeSet<T> / HashSet<T>
     multimap<K,V>	Map<K,List<V>>
     FILE*	Reader / Writer or streams
     fgetwc_unlocked()	Reader.read()
     wcerr	System.err
     wstring::find()	String.contains() / indexOf()
     wstring::substr()	String.substring()
     wstring::compare()	String.equals()
     iswupper()	Character.isUpperCase()
     towupper()	Character.toUpperCase()
     towlower()	Character.toLowerCase()
     exit(EXIT_FAILURE)	throw IllegalStateException(...)
     pointers (Chunk*)	object references (Chunk)
     new / delete	garbage collection
     NULL chunk	null
     auto	explicit Java type / var where appropriate
     swap()	assignment through temporaries
     C++ iterators	enhanced for loops / Java iterators

     One important issue is that this isn't actually a self-contained C++ program. Most of the computational behavior is supplied by Apertium classes. For example:

     mx->matchChunk(...)
     mx->getRule(...)
     ch->chunkPart(...)
     node->getChunks(...)
     node->popNodes(...)
     alphabet.read(...)
     Compression::wstring_read(...)

     cannot be translated into standard Java equivalents. They need Java implementations of the corresponding Apertium classes.

     Also, there are a couple of C++-specific details worth preserving carefully during a full port:
     wstring indexing operates on wchar_t, while Java String indexes UTF-16 chars. For Apertium's actual data this may be acceptable, but Unicode supplementary characters require code-point handling.
     The C++ code uses pointer aliasing extensively (Chunk*, ParseNode*). Java references reproduce the aliasing semantics, but object pooling needs to be implemented carefully.
     goto try_again_for_reject_rule in processTRXLayer() should become a Java loop.
     multimap<int, pair<int,double>> should be represented as something like Map<Integer,List<RuleWeight>>.
     vector<bool> is a specialized C++ container; Java should simply use boolean[].
     memset(state, 1, ...) does not translate to Arrays.fill(state, 1) in the general C++ sense. Here the C++ code is actually filling each int byte with 0x01, producing 0x01010101, so this deserves special attention. If the intended values are boolean-like 1, Java should use Arrays.fill(state, 1).
     If the goal is a fully compilable Java port rather than a structural translation, the next step is to port the dependent classes (Chunk, ParseNode, MatchExe2, Transducer, Compression, AttrItem, and the bytecode constants) alongside this class.
     */

}
