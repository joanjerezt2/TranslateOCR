package org.apertium.grammar;

import org.apertium.lttoolbox.process.InputFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apertium.grammar.Cohort.*;
import static org.apertium.grammar.Inlines.*;
import static org.apertium.grammar.Reading.alloc_reading;
import static org.apertium.grammar.Reading.free_reading;
import static org.apertium.grammar.SingleWindow.free_swindow;
import static org.apertium.grammar.Strings.*;
import static org.apertium.grammar.Tag.*;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class ApertiumApplicator  extends GrammarApplicator {
    char esc_lt = '\1';

    public boolean wordform_case = false;
    public boolean print_word_forms = true;
    public boolean print_only_first = false;
    public boolean delimit_lexical_units = true;
    public boolean surface_readings = false;

    public char c; // Current character
    public boolean in_blank = false;   // Are we in a superblank ?
    public boolean in_wblank = false;  // Are we in a word-bound blank ?
    public boolean in_cohort = false;  // Are we in a cohort ?

    SingleWindow cSWindow = null; // Current single window (Cohort frame)
    Cohort cCohort = null;        // Current cohort

    SingleWindow lSWindow = null; // Last seen single window
    Cohort lCohort = null;        // Last seen cohort

    Map<Integer, Integer> variables_set = new HashMap<>();
    Set<Integer> variables_rem = new HashSet<>();
    Set<Integer> variables_output = new TreeSet<>();

    StringBuilder token = new StringBuilder();           // Current token (cohort)

    enum ApertiumCasing { Nochange, Title, Upper }

    public ApertiumApplicator(PrintStream err) {
        super(err);
    }

    /**
     * Parse a stream variable from cleaned blank.
     * Tries not to do anything more than what happens in GrammarApplicator_runGrammar.cpp, for
     * easy merging.
     *
     * @param cleaned something like "<STREAMCMD:SETVAR:forskjell_skilnad" (note: no trailing >).
     */
    private void parseStreamVar(SingleWindow cSWindow, StringBuilder cleaned, Map<Integer, Integer> variablesSet,
                                Set<Integer> variablesRem, Set<Integer> variablesOutput) {
        if (cleaned.toString().startsWith(STR_CMD_SETVAR)) {
            String s = cleaned.substring(STR_CMD_SETVAR.length());

            for (int i = STR_CMD_SETVAR.length(); i < cleaned.length(); i++) {
                char ch = cleaned.charAt(i);
                if (ch == ',' || ch == '=') {
                    cleaned.setLength(i); // Trunca el StringBuilder en esta posición
                    break;
                }
            }

            String[] parts = s.split(",", -1);

            // Caso especial del original: no hay comas ni signos '=' y no hay ventana actual
            boolean hasNoDelimiters = parts.length == 1 && !s.contains("=");
            for (String part : parts) {
                int eqIdx = part.indexOf('=');
                if (eqIdx != -1) {
                    // Caso: clave=valor
                    String keyStr = part.substring(0, eqIdx);
                    String d = part.substring(eqIdx + 1);
                    int a;
                    if (keyStr.isEmpty()) {
                        System.err.print("Warning: SETVAR on line " + numLines + " had no identifier before the =! Defaulting to identifier *.\n");
                        a = grammar.tag_any;
                    } else {
                        a = addTag(s).hash;
                    }
                    int b;
                    if (d.isEmpty()) {
                        System.err.print("Warning: SETVAR on line " + numLines + " had no value after the =! Defaulting to value *.\n");
                        b = grammar.tag_any;
                    } else {
                        b = addTag(d + 1).hash;
                    }
                    variablesSet.put(a, b);
                    variablesRem.remove(a);
                    variablesOutput.add(a);

                } else {
                    // Caso: solo identificador (sin '=')
                    int a;
                    if (part.isEmpty()) {
                        // En C++ solo lanza aviso si venía precedido o seguido de coma
                        if (parts.length > 1) {
                            System.err.printf("Warning: SETVAR on line %d had no identifier after the ,! Defaulting to identifier *.%n", numLines);
                        }
                        a = grammar.tag_any;
                    } else {
                        a = addTag(part).hash;
                    }

                    variablesSet.put(a, grammar.tag_any);
                    variablesRem.remove(a);
                    variablesOutput.add(a);

                    if (hasNoDelimiters && cSWindow == null) {
                        this.variables.put(a, grammar.tag_any);
                    }
                }
            }
        }
        else if (cleaned.toString().startsWith(STR_CMD_REMVAR)) {
            String s = cleaned.substring(STR_CMD_REMVAR.length());

            for (int i = STR_CMD_REMVAR.length(); i < cleaned.length(); i++) {
                char ch = cleaned.charAt(i);
                if (ch == ',') {
                    cleaned.setLength(i); // Trunca el StringBuilder en esta posición
                    break;
                }
            }

            String[] parts = s.split(",");
            int a;
            for(String part: parts)
                if (!part.isEmpty()) {
                    a = addTag(s).hash;
                    variablesSet.remove(a);
                    variablesRem.add(a);
                    variablesOutput.add(a);
                }
            }
    }

    public void ensureEndTag() {
        if (!lSWindow.cohorts.isEmpty() &&
                !lSWindow.cohorts.getLast().readings.getFirst().tags.contains(endtag)) { // C++ count() == 0
            for (Reading iter : lSWindow.cohorts.getLast().readings) {
                addTagToReading(iter, endtag);
            }
        }
    }

    public void flush(StringBuilder blank, PrintStream output) {
        flush(false, blank, output);
    }

    public void flush(boolean n, StringBuilder blank, PrintStream output) {
        ensureEndTag();
        SingleWindow backSWindow = n ? gWindow.back() : null;
        if (backSWindow != null) {
            backSWindow.flush_after = true;
        }

        if (!blank.isEmpty()) {
            if (lCohort != null) {
                lCohort.text += blank;
            } else if (lSWindow != null && !lSWindow.cohorts.isEmpty()) {
                lSWindow.cohorts.getLast().text += blank;
            } else if (lSWindow != null) {
                lSWindow.text += blank;
            } else {
                printPlainTextLine(blank, output);
            }
            blank.setLength(0);

            // Run the grammar & print results
            while (!gWindow.next.isEmpty()) {
                gWindow.shuffleWindowsDown();
                runGrammarOnWindow();
            }

            gWindow.shuffleWindowsDown();
            while (!gWindow.previous.isEmpty()) {
                SingleWindow tmp = gWindow.previous.getFirst();
                printSingleWindow(tmp, output);
                free_swindow(tmp);
                gWindow.previous.remove(gWindow.previous.getFirst());
            }

            if (c != 0xffff) {
                // TODO this does not appear to show up in any test/**/input.txt processing so it is untested
                printPlainTextLine(new StringBuilder(String.valueOf(c)), output);
            }

            if (n && backSWindow == null) {
                output.write('\0');
            }
            output.flush();

            in_blank = false;
            in_wblank = false;
            in_cohort = false;
            lSWindow = null;
            lCohort = null;
            cSWindow = null;
            cCohort = null;
            token.setLength(0);
            variables_rem.clear();
            variables_set.clear();
            variables_output.clear();
            variables.clear();
        }
    }

    public void runGrammarOnText(InputStream input, PrintStream output) throws IOException {
        if (input == null) {
            System.err.print("Error: Input is null - nothing to parse!\n");
            System.exit(1);
        }
        InputFile input_file = new InputFile(input);
        if (input_file.eof()) {
            System.err.print("Error: Input is empty - nothing to parse!\n");
            System.exit(1);
        }

        if (output == null) {
            System.err.print("Error: Output is null - cannot write to nothing!\n");
            System.exit(1);
        }

        if (grammar == null) {
            System.err.print("Error: No grammar provided - cannot continue! Hint: call setGrammar() first.\n");
            System.exit(1);
        }

        if (grammar.delimiters == null || grammar.delimiters.empty()) {
            if (grammar.soft_delimiters == null || grammar.soft_delimiters.empty()) {
                System.err.printf("Warning: No soft or hard delimiters defined in grammar. Hard limit of %d cohorts may break windows in unintended places.\n", hard_limit);
            }
            else {
                System.err.printf("Warning: No hard delimiters defined in grammar. Soft limit of %d cohorts may break windows in unintended places.\n", soft_limit);
            }
        }


        StringBuilder blank = new StringBuilder();           // Blank between tokens, including superblanks
        String wblank = "";

        index();

        int resetAfter = ((num_windows + 4) * 2 + 1);

        String wb_start = "[[";
        String wb_end = "]]";

        int begintag = addTag(STR_BEGINTAG).hash; // Beginning of sentence tag
        int endtag = addTag(STR_ENDTAG).hash;     // End of sentence tag

        gWindow.window_span = num_windows;

        Inlines.ux_stripBOM(input);

        byte ci;
        while ((ci = (byte) input.read()) != -1) {
            c = (char) ci;
            if (c == '\n') {
                ++numLines;
            }

            if (c == '\\') {
                char n = (char) input.read();
                if (!in_cohort) {
                    blank.append(c);
                    blank.append(n);
                }
                else {
                    token.append(c);
                    token.append(n);
                }
                continue;
            }

            if (c == 0) {
                flush(true, blank, output);
                continue;
            }

            if (!in_cohort && c == '[') {
                if (in_blank) {
                    in_wblank = true;
                }
                in_blank = true;
            }
            else if (!in_blank && c == '^') {
                in_cohort = true;
            }

            if (!in_cohort) {
                blank.append(c);
            }
            else {
                token.append(c);
            }

            if (in_wblank && c == ']') {
                in_wblank = false;
            }
            else if (in_blank && c == ']') {
                in_blank = false;
                if (blank.length() > 14 // contains at least "[<STREAMCMD:>]"
                        && blank.charAt(1) == '<' && blank.charAt(blank.length() - 2) == '>') {
                    StringBuilder cleaned = new StringBuilder(blank.substring(1, blank.length() - 3 + 1));
                    parseStreamVar(cSWindow, cleaned, variables_set, variables_rem, variables_output);
                }
            }
            else if (!in_blank && c == '$') {
                if (!in_cohort) {
                    System.err.printf("Error: $ found without prior ^ on line %d.\n", numLines);
                    System.exit(1);
                }
                in_cohort = false;

                if (!blank.toString().isEmpty()) {
                    wblank = EMPTY;
                    int b = blank.indexOf(wb_start);
                    int e = blank.indexOf(wb_end, b);
                    if (b != -1 && e != -1) {
                        // Word-bound blanks are always immediately prior to the token they belong to,
                        // except closing word blanks which must be treated as normal blanks
                        if (e != b + 3 || blank.charAt(b + 2) != '/') {
                            wblank = blank.substring(b);
                            blank.delete(b, blank.length());
                        }
                    }
                }
                if (!wblank.isEmpty() && (wblank.charAt(wblank.length() - 1) != ']' || wblank.charAt(wblank.length() - 2) != ']')) {
                    System.err.print("Error: Word-bound blank was not immediately prior to token on line " + numLines + "\n");
                    System.exit(1);
                }

                if (cCohort != null) {
                    cCohort.text += blank;
                    blank = new StringBuilder(EMPTY);
                }
                else if (lCohort != null) {
                    lCohort.text += blank;
                    blank = new StringBuilder(EMPTY);
                }
                else if (lSWindow != null) {
                    lSWindow.text += blank;
                    blank = new StringBuilder(EMPTY);
                }

                // If we don't have a current window, create one
                if (cSWindow == null) {
                    if (lSWindow != null) {
                        ensureEndTag();
                    }
                    cSWindow = gWindow.allocAppendSingleWindow();

                    initEmptySingleWindow(cSWindow);

                    cSWindow.variables_set = variables_set;
                    variables_set.clear();
                    cSWindow.variables_rem = variables_rem;
                    variables_rem.clear();
                    cSWindow.variables_output = variables_output;
                    variables_output.clear();

                    lSWindow = cSWindow;
                    lSWindow.text = blank.toString();
                    blank = new StringBuilder(EMPTY);
                    ++numWindows;
                } // created at least one cSWindow by now

                lCohort = cCohort = alloc_cohort(cSWindow);
                cCohort.global_number = gWindow.cohort_counter++;
                ++numCohorts;

                cCohort.text = blank.toString();
                blank = new StringBuilder(EMPTY);
                cCohort.wblank = wblank;
                wblank = EMPTY;

                blank.append("\"<");
                int p = 1;
                for (; p < token.length(); ++p) {
                    c = token.charAt(p);
                    if (c == '/' || c == '<' || c == '$') {
                        break;
                    }
                    if (c == '\\') {
                        ++p;
                        // Protección importante en Java para evitar StringIndexOutOfBoundsException
                        // si la barra '\' es el último carácter de la cadena
                        if (p >= token.length()) {
                            break;
                        }
                    }
                    blank.append(token.charAt(p));
                }
                /* C++ style
                char[] p_text = token.toString().toCharArray();

                for (; p < p_text.length && p_text[p] != '/' && p_text[p] != '<' && p_text[p] != '$'; ++p) {
                    if (p_text[p] == '\\') {
                        ++p;
                    }
                    blank.append(p_text[p]);
                } */
                blank.append(">\"");
                cCohort.wordform = addTag(blank.toString());
                blank = new StringBuilder(EMPTY);

                // Handle the static reading of ^estació<n><f><sg>/season<n><sg>/station<n><sg>$
                // Gobble up all <tags> until the first / or $ and stuff them in the static reading
                if (token.charAt(p) == '<') {
                    ++p;
                    blank = new StringBuilder(EMPTY);
                    cCohort.wread = alloc_reading(cCohort);
                    for (; token.charAt(p) != '/' && token.charAt(p) != '$'; ++p) {
                        if (token.charAt(p) == '\\') {
                            ++p;
                            blank.append(token.charAt(p));
                            continue;
                        }
                        if (token.charAt(p) == '<') {
                            continue;
                        }
                        if (token.charAt(p) == '>') {
                            Tag t = addTag(blank.toString());
                            addTagToReading(cCohort.wread, t);
                            blank = new StringBuilder(EMPTY);
                            continue;
                        }
                        blank.append(token.charAt(p));
                    }
                }

                // Read in the readings
                if (token.charAt(p) == '/') {
                    ++p;
                    blank = new StringBuilder(EMPTY);

                    for (; token.length()>=p; ++p) {
                        if (token.charAt(p) == '\\') {
                            ++p;
                            if (token.charAt(p) == '<') {
                                blank.append(esc_lt);
                            }
                            else {
                                blank.append(token.charAt(p));
                            }
                            continue;
                        }
                        if (token.charAt(p) == '/' || token.charAt(p) == '$') {
                            Reading cReading = alloc_reading(cCohort);
                            processReading(cReading, blank, cCohort.wordform);

                            if (grammar.sub_readings_ltr && cReading.next != null) {
                                cReading = reverse(cReading);
                            }

                            if (cReading.deleted) {
                                cCohort.deleted.addLast(cReading);
                            }
                            else {
                                cCohort.appendReading(cReading);
                            }
                            ++numReadings;

                            if (cReading.baseform == 0) {
                                System.err.printf("Warning: Cohort %d on line %d had no valid baseform.\n", numCohorts, numLines);
                                System.err.flush();
                            }
                            blank = new StringBuilder(EMPTY);
                            continue;
                        }
                        blank.append(p);
                    }
                }

                // Create magic reading, if needed
                if (cCohort.readings.isEmpty()) {
                    initEmptyCohort(cCohort);
                }
                insert_if_exists(cCohort.possible_sets, BitSet.valueOf(grammar.sets_any));
                cSWindow.appendCohort(cCohort);
                if (cCohort.wordform.tag.charAt(2) == '@') {
                    cCohort.type |= CT_AP_UNKNOWN;
                }

                boolean did_delim = false;
                if (cSWindow.cohorts.size() >= soft_limit && grammar.soft_delimiters != null
                        && doesSetMatchCohortNormal(cCohort, grammar.soft_delimiters.number)) {
                    for (Reading iter : cCohort.readings) {
                        addTagToReading(iter, endtag);
                    }

                    lSWindow = cSWindow;
                    cSWindow = null;
                    cCohort = null;
                    did_delim = true;
                } // end >= soft_limit
                if (cCohort != null && (cSWindow.cohorts.size() >= hard_limit || (grammar.delimiters != null
                        && doesSetMatchCohortNormal(cCohort, grammar.delimiters.number)))) {
                    if (!is_conv && cSWindow.cohorts.size() >= hard_limit) {
                        System.err.printf("Warning: Hard limit of %d cohorts reached at cohort %S (#%d) on line %d - forcing break.\n",
                                hard_limit, cCohort.wordform.tag, numCohorts, numLines);
                        System.err.flush();
                    }
                    for (Reading iter : cCohort.readings) {
                        addTagToReading(iter, endtag);
                    }

                    lSWindow = cSWindow;
                    cSWindow = null;
                    cCohort = null;
                    did_delim = true;
                } // end >= hard_limit

                if (did_delim && gWindow.next.size() > num_windows) {
                    gWindow.shuffleWindowsDown();
                    runGrammarOnWindow();
                    if (numWindows % resetAfter == 0) {
                        resetIndexes();
                    }
                }
                token = new StringBuilder(EMPTY);
            }
        }
        flush(blank, output);
    }

    /*
     * Parse an Apertium reading into a CG Reading
     *
     * Examples:
     *   venir<vblex><imp><p2><sg>
     *   venir<vblex><inf>+lo<prn><enc><p3><nt><sg>
     *   be<vblex><inf># happy
     *   sellout<vblex><imp><p2><sg># ouzh+indirect<prn><obj><p3><m><sg>
     *   be# happy<vblex><inf> (for chaining cg-proc)
     */
    private void processReading(Reading cReading, StringBuilder token, Tag wform) {
        addTagToReading(cReading, wform);

        List<Tag> taglist = new ArrayList<>();
        StringBuilder bf = new StringBuilder("\"");
        List<Tag> tags = new ArrayList<>();
        List<Tag> prefix_tags = new ArrayList<>();
        int p = 0;
        while (p < token.length()) {
            int n = p;
            while (n < token.length() && token.charAt(n) != '#' && token.charAt(n) != '+' && token.charAt(n) != '<') {
                if (token.charAt(n) == esc_lt) {
                    token.setCharAt(n, '<');
                }
                ++n;
            }
            // Found a baseform (if something is between start and [#+<], it must be part of the baseform)
            if (n != p) {
                bf.append(token, p, n - p);
                p = n;
            }

            // Found a tag start, so read until >
            if (token.charAt(n)  == '<') {
                p = n + 1;
                while (n < token.length() || token.charAt(n) != '>') {
                    ++n;
                }
                if (token.charAt(n) != '>') {
                    System.err.printf("Warning: Did not find matching > to close the tag on line %d.\n", numLines);
                    p = n; // Importante: en C++ p se quedó en n+1, pero n llegó al final; igualar p = len evita bucle infinito
                    continue;
                }
                if (bf.length() == 1) {
                    prefix_tags.addLast(addTag(token.substring(p, n)));
                }
                else {
                    tags.addLast(addTag(token.substring(p, n)));
                }
                p = n + 1;
            }
            // Found a multiword marker, so read until [+<]
            if (token.charAt(n) == '#') {
                p = n;
                while (n < token.length() && token.charAt(n) != '<' && token.charAt(n) != '+') {
                    ++n;
                }
                bf.append(token, p, n-p);
                p = n;
            }
            // Found a sub-reading delimiter, so append current tags to the list in the correct order
            if (token.charAt(n) == '+') {
                bf.append('"');
                taglist.addLast(addTag(bf.toString()));
                taglist.addAll(tags);
                taglist.addAll(prefix_tags);
                bf.setLength(1); // Equivalente a bf.resize(1)
                tags.clear();
                prefix_tags.clear();
                p = n + 1;
            }
        }

        if (!bf.isEmpty() || !tags.isEmpty() || !prefix_tags.isEmpty()) {
            bf.append('"');
            taglist.addLast(addTag(bf.toString()));
            taglist.addAll(tags);
            taglist.addAll(prefix_tags);
        }

        // Search from the back until we find a baseform, then add all tags from there until the end onto the reading
        while (!taglist.isEmpty()) {
            Reading reading = cReading;
            for (int i = taglist.size() - 1; i >= 0; i--) {
                Tag riter = taglist.get(i);
                if ((riter.type & T_BASEFORM) != 0) {
                    // If current reading already has a baseform, instead create a sub-reading as target
                    if (reading.baseform != 0) {
                        Reading nr = reading.allocateReading(reading.parent);
                        reading.next = nr;
                        reading = nr;
                        addTagToReading(reading, wform);
                    }
                    // Add tags
                    List<Tag> mappings = new ArrayList<>();
                    for (int j = i; j < taglist.size(); j++) {
                        Tag iter = taglist.get(j);
                        if ((iter.type & T_MAPPING) != 0 || iter.tag.charAt(0) == grammar.mapping_prefix) {
                            mappings.addLast(iter);
                        }
                        else {
                            addTagToReading(reading, iter);
                        }
                    }
                    if (!mappings.isEmpty()) {
                        splitMappings(mappings, reading.parent, reading, true);
                    }
                    // Remove tags from list
                    while (!taglist.isEmpty() && (taglist.getLast().type & T_BASEFORM) == 0) {
                        taglist.removeLast();
                    }
                    taglist.removeLast();
                }
            }
        }
    }

    private StringBuilder getBfEscaped_new(Reading reading, ApertiumCasing casing, int firstlower) {
        StringBuilder bf = new StringBuilder();
        // UnicodeString bf(grammar->single_tags[reading->baseform]->tag.data() + 1, SI32(grammar->single_tags[reading->baseform]->tag.size() - 2));

        if (wordform_case) {
            if (casing == ApertiumCasing.Upper) {
                bf = new StringBuilder(bf.toString().toUpperCase()); // Perform a Unicode case folding to upper case -- Tino Didriksen
            }
            else if (casing == ApertiumCasing.Title && reading.next == null) {
                // static_cast<UChar>(u_toupper(bf[first])) gives strange output
                String b = String.valueOf(bf.charAt(firstlower)).toUpperCase();
                bf.setCharAt(firstlower, b.charAt(0));
            }
        }
        return createBfEscaped_new(reading, bf);
    }

    private StringBuilder createBfEscaped_new(Reading reading, StringBuilder bf) {
        StringBuilder bf_escaped = new StringBuilder();
        for (int i = 0; i < bf.length(); ++i) {
            if (bf.charAt(i) == '^' || bf.charAt(i) == '\\' || bf.charAt(i) == '/' || bf.charAt(i) == '$' ||
                    bf.charAt(i) == '[' || bf.charAt(i) == ']' || bf.charAt(i) == '{' || bf.charAt(i) == '}' ||
                    bf.charAt(i) == '<' || bf.charAt(i) == '>') {
                bf_escaped.append('\\');
            }
            if ((reading.parent.type & CT_AP_UNKNOWN) != 0 && bf.charAt(i) == '@') {
                bf_escaped.append('\\');
            }
            bf_escaped.append(bf.charAt(i));
        }
        if(surface_readings && !bf.isEmpty() && bf_escaped.charAt(0) == '@') {
            bf_escaped.setCharAt(0, '#');
        }
        return bf_escaped;
    }

    private List<Integer> createTagsList(Reading reading) {
        List<Integer> tags_list = new ArrayList<>();
        List<Integer> multitags_list = new ArrayList<>(); // everything after a +, until the first MAPPING tag
        boolean multi = false;

        for (int tter : reading.tags_list) {
            Tag tag = grammar.single_tags.get(tter);
            if (tag.tag.charAt(0) == '+') {
                multi = true;
            }
            else if ((tag.type & T_MAPPING) != 0) {
                multi = false;
            }

            if ((tag.type & T_DEPENDENCY) != 0 && has_dep && !dep_original) {
                continue;
            }

            if (multi) {
                multitags_list.addLast(tter);
            }
            else {
                tags_list.addLast(tter);
            }
        }
        tags_list.addAll(multitags_list);
        return tags_list;
    }

    private void printReading(Reading reading, PrintStream output, ApertiumCasing casing, int firstlower) {
        if (reading.next != null) {
            printReading(reading.next, output, casing, firstlower);
            output.write('+');
        }

        if (reading.baseform != 0) {
            // Lop off the initial and final '"' characters
            StringBuilder bf_escaped = getBfEscaped_new(reading, casing, firstlower);
            output.printf("%S", bf_escaped);
        }

        if(surface_readings && !trace) {
            // As with lt-proc -g, when output has tags (meaning
            // ungenerated words), don't print unless tracing (lt-proc -d)
            return;
        }

        // Reorder: MAPPING tags should appear before the join of multiword tags,
        // turn <vblex><actv><pri><p3><pl>+í<pr><@FMAINV><@FOO>$
        // into <vblex><actv><pri><p3><pl><@FMAINV><@FOO>+í<pr>$
        List<Integer> tags_list = createTagsList(reading);

        SortedVector<Integer> used_tags = new SortedVector<>();
        String escape = surface_readings ? "\\" : "";
        for (int tter : tags_list) {
            if (unique_tags) {
                if (used_tags.contains(tter)) {
                    continue;
                }
                used_tags.insert(tter);
            }
            if (tter == endtag || tter == begintag) {
                continue;
            }
            Tag tag = grammar.single_tags.get(tter);
            if ((tag.type & T_BASEFORM) == 0 && (tag.type & T_WORDFORM) == 0) {
                if (tag.tag.charAt(0) == '+') {
                    output.printf("%S", tag.tag);
                }
                else if (tag.tag.charAt(0) == '&') {
                    output.printf("%S<%S%S>", escape, tag.tag.substring(2), escape);
                }
                else {
                    output.printf("%S<%S%S>", escape, tag.tag, escape);
                }
            }
        }

        if (has_dep && ((reading.parent.type & CT_REMOVED) == 0) && reading.next == null) {
            if (reading.parent.dep_self == 0) {
                reading.parent.dep_self = reading.parent.global_number;
            }
            Cohort pr;
            pr = reading.parent;
            if (reading.parent.dep_parent != DEP_NO_PARENT) {
                if (reading.parent.dep_parent == 0) {
                    pr = reading.parent.parent.cohorts.getFirst();
                }
                else if (gWindow.cohort_map.containsKey(reading.parent.dep_parent)) {
                    pr = gWindow.cohort_map.get(reading.parent.dep_parent);
                }
            }

            // C++ constexpr UChar pattern[] = { '<', '#', '%', 'u', u'→', '%', 'u', '>', 0 };
            final char[] pattern = { '<', '#', '%', 'd', '→', '%', 'd', '>', 0 };
            output.printf(Arrays.toString(pattern), reading.parent.local_number, pr.local_number);
        }

        if (trace) {
            for (int iter_hb : reading.hit_by) {
                output.write('<');
                printTrace(output, iter_hb);
                output.write('>');
            }
        }
    }

    private void printReading(Reading reading, PrintStream output) {
        ApertiumCasing casing = ApertiumCasing.Nochange;

        if (wordform_case) {
            // Use surface/wordform case, e.g. if lt-proc
            // was called with "-w" option (which puts
            // dictionary case on lemma/basefrom)
            // cf. fst_processor.cc in lttoolbox
            Reading last = reading;
            while (last.next != null && last.next.baseform != 0) {
                last = last.next;
            }
            if (last.baseform != 0) {
                String wftag = reading.parent.wordform.tag.toString();
                int wf_length = wftag.length() - 4;

                int uppercaseseen = 0;
                int alphabeticsseen = 0;
                for (int i = 0; i < wf_length; ++i) {
                    char c = wftag.charAt(i+2); // skip the initial "<
                    if(Character.isAlphabetic(c)) {
                        ++alphabeticsseen;
                        if(Character.isUpperCase(c)) {
                            ++uppercaseseen;
                        }
                    }
                }

                // Require at least 2 characters, all upper, to call it UPPER:
                if (uppercaseseen == alphabeticsseen && uppercaseseen >= 2) {
                    casing = ApertiumCasing.Upper;
                }
                // Require only the very first character upper, to call it Title:
                else if (Character.isUpperCase(wftag.charAt(2)) && uppercaseseen == 1) {
                    casing = ApertiumCasing.Title;
                }
            }
        } // if (wordform_case)
        printReading(reading, output, casing, 0);
    }

    private static StringBuilder getWfEscaped_new(Cohort cohort) {
        // C++ UnicodeString wf(cohort->wordform->tag.data() + 2, SI32(cohort->wordform->tag.size() - 4));
        StringBuilder tag = cohort.wordform.tag;
        boolean isUnknown = (cohort.type & CT_AP_UNKNOWN) != 0;

        // Inicializamos con una capacidad estimada para evitar redimensionamientos en memoria
        StringBuilder wfEscaped = new StringBuilder(tag.length());

        // Iteramos directamente por los índices útiles (evitando substring y toArray)
        for (int i = 2, end = tag.length() - 2; i < end; i++) {
            char c = tag.charAt(i);
            if ("^\\/$[]{}<>".indexOf(c) != -1 || (isUnknown && c == '@')) {
                wfEscaped.append('\\');
            }
            wfEscaped.append(c);
        }

        return wfEscaped;
    }

    private void printCohort(Cohort cohort, PrintStream output, boolean profiling) {
        if (cohort.local_number == 0 || ((cohort.type & CT_REMOVED) != 0)) {
            if (!cohort.text.isEmpty()) {
                output.printf("%S", cohort.text);
            }
            return;
        }

        if (!profiling) {
            cohort.unignoreAll();

            if (!split_mappings) {
                mergeMappings(cohort);
            }
        }

        if (!cohort.wblank.isEmpty()) {
            output.printf("%S", cohort.wblank);
        }

        // Start of cohort
        if (delimit_lexical_units) {
            output.print("^");
        }

        if (print_word_forms) {
            // Lop off the initial and final '"' characters
            // ToDo: A copy does not need to be made here - use pointer offsets
            // C++ UnicodeString wf(cohort->wordform->tag.data() + 2, SI32(cohort->wordform->tag.size() - 4));
            StringBuilder wf_escaped = getWfEscaped_new(cohort);
            output.printf("%S", wf_escaped);

            // Print the static reading tags
            if (cohort.wread != null) {
                for (int tter : cohort.wread.tags_list) {
                    if (tter == cohort.wordform.hash) {
                        continue;
                    }
                    Tag tag = grammar.single_tags.get(tter);
                    output.printf("<%S>", tag.tag);
                }
            }
        }

        boolean need_slash = print_word_forms;

        cohort.readings.sort(Comparator.comparingInt(Reading::getNumber).thenComparingInt(Reading::getHash));
        // C++ std::sort(cohort->readings.begin(), cohort->readings.end(), Reading::cmp_number);
        for (Reading reading : cohort.readings) {
            if (reading.noprint) {
                continue;
            }
            if (need_slash) {
                output.print("/");
            }
            need_slash = true;
            if (grammar.sub_readings_ltr && reading.next != null) {
                reading = reverse(reading);
            }
            printReading(reading, output);
            if (print_only_first) {
                break;
            }
        }

        if (trace) {
            cohort.delayed.sort(Comparator.comparingInt(Reading::getNumber).thenComparingInt(Reading::getHash));
            // C++ std::sort(cohort.delayed.begin(), cohort.delayed.end(), Reading::cmp_number);
            for (Reading reading : cohort.delayed) {
                if (reading.noprint) {
                    continue;
                }
                if (need_slash) {
                    output.printf("/%C", not_sign);
                }
                need_slash = true;
                if (grammar.sub_readings_ltr && reading.next != null) {
                    reading = reverse(reading);
                }
                printReading(reading, output);
            }
            cohort.deleted.sort(Comparator.comparingInt(Reading::getNumber).thenComparingInt(Reading::getHash));
            // C++ std::sort(cohort->deleted.begin(), cohort->deleted.end(), Reading::cmp_number);
            for (Reading reading : cohort.deleted) {
                if (reading.noprint) {
                    continue;
                }
                if (need_slash) {
                    output.printf("/%C", not_sign);
                }
                need_slash = true;
                if (grammar.sub_readings_ltr && reading.next != null) {
                    reading = reverse(reading);
                }
                printReading(reading, output);
            }
        }

        if (delimit_lexical_units) {
            output.print("$");
        }
        // End of cohort

        if (!cohort.text.isEmpty()) {
            output.printf("%S", cohort.text);
        }
    }

    private void printSingleWindow(SingleWindow window, PrintStream output) {
        printSingleWindow(window, output, false);
    }

    private void printSingleWindow(SingleWindow window, PrintStream output, boolean profiling) {
        // Window text comes at the left
        if (!window.text.isEmpty()) {
            output.printf("%S", window.text);
        }

        for (Cohort cohort : window.all_cohorts) {
            printCohort(cohort, output, profiling);
            output.flush();
        }

        if (!window.text_post.isEmpty()) {
            output.printf("%S", window.text_post);
            output.flush();
        }

        if (window.flush_after) {
            output.write('\0');
        }
    }

    private int getHp_new(Reading iter) {
        int hp = iter.hash; // instead of hash_plain, which doesn't include mapping tags
        if (trace) {
            for (int iter_hb : iter.hit_by) {
                hp = hash_value(iter_hb, hp);
            }
        }
        Reading sub = iter.next;
        while (sub != null) {
            hp = hash_value(sub.hash, hp);
            if (trace) {
                for (int iter_hb : sub.hit_by) {
                    hp = hash_value(iter_hb, hp);
                }
            }
            sub = sub.next;
        }
        return hp;
    }

    private void mergeMappings(Cohort cohort) {
        // Only merge readings which are completely equal (including mapping tags)
        // foo<N><Sg><Acc><@←OBJ>/foo<N><Sg><Acc><@←OBJ>
        // => guovvamánnu<N><Sg><Acc><@←OBJ>
        // foo<N><Sg><Acc><@←SUBJ>/foo<N><Sg><Acc><@←OBJ>
        // => foo<N><Sg><Acc><@←SUBJ>/foo<N><Sg><Acc><@←OBJ>
        Map<Integer, List<Reading>> mlist = new TreeMap<>();
        for (Reading iter : cohort.readings) {
            int hp = getHp_new(iter);
            // C++ Equivalente exacto a mlist[hp].push_back(r);
            mlist.computeIfAbsent(hp, k -> new ArrayList<>()).add(iter);
        }

        if (mlist.size() == cohort.readings.size()) {
            return;
        }

        cohort.readings.clear();
        List<Reading> order = new ArrayList<>();

        for (Map.Entry<Integer, List<Reading>> miter : mlist.entrySet()) {
            List<Reading> clist = miter.getValue();
            // no merging of mapping tags, so just take first reading of the group
            order.addLast(clist.getFirst());

            clist.remove(clist.getFirst());
            for (Reading cit : clist) {
                free_reading(cit);
            }
        }
        order.sort(Comparator.comparingInt(Reading::getNumber).thenComparingInt(Reading::getHash)); // C++ std::sort(order.begin(), order.end(), Reading::cmp_number);
        cohort.readings.addAll(0, order); // C++ readings.insert(readings.begin(), order.begin(), order.end());
    }
}
