package org.apertium.grammar;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.floor;
import static java.lang.Math.log10;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class GrammarApplicator {

    public Grammar grammar;
    public List<Integer> sections;

    boolean always_span = false;
    boolean apply_mappings = true;
    boolean apply_corrections = true;
    boolean no_before_sections = false;
    boolean no_sections = false;
    boolean no_after_sections = false;
    boolean trace = false;
    boolean trace_name_only = false;
    boolean trace_no_removed = false;
    boolean trace_encl = false;
    boolean allow_magic_readings = true;
    boolean no_pass_origin = false;
    boolean unsafe = false;
    boolean ordered = false;
    boolean show_end_tags = false;
    boolean unicode_tags = false;
    boolean unique_tags = false;
    boolean dry_run = false;
    boolean owns_grammar = false;
    boolean input_eof = false;
    boolean seen_barrier = false;
    boolean is_conv = false;
    boolean split_mappings = false;
    boolean pipe_deleted = false;
    boolean add_spacing = true;
    boolean print_ids = false;

    boolean dep_has_spanned = false;
    int dep_delimit = 0;
    boolean dep_absolute = false;
    boolean dep_original = false;
    boolean dep_block_loops = true;
    boolean dep_block_crossing = false;

    int num_windows = 2;
    int soft_limit = 300;
    int hard_limit = 500;

    List<Integer> valid_rules;
    List<Integer> trace_rules;
    List<Integer> debug_rules;
    Map<String, Integer> variables = new HashMap<>();
    int verbosity_level = 0;
    int debug_level = 0;
    int section_max_count = 0;

    boolean has_dep = false;
    static boolean parse_dep = false;
    int dep_highest_seen = 0;
    static Window gWindow = null;

    static boolean has_relations = false;
    Profiler profiler = null;

    public GrammarApplicator(PrintStream err) {
        Window gWindow = null;
        gWindow = new Window(this);
    }

    public void runGrammarOnText(InputStream input, PrintStream output) throws IOException {

    }

    public Tag addTag(String tag){
        return new Tag();
    }

    public void index(){
        /* if (!add_spacing) {
            ws[2] = '\n';
        }
        if (did_index) {
            return;
        }

        if (grammar.ordered) {
            ordered = true;
        }
        if (grammar.has_dep || dep_delimit != 0) {
            parse_dep = true;
        }

        List<Integer> m = new ArrayList<>();
        if (!grammar.before_sections.isEmpty()) {
            m = runsections[-1];
            for (Rule r : grammar.before_sections) {
                m.add(r.number);
            }
        }

        if (!grammar.after_sections.isEmpty()) {
            m = runsections[-2];
            for (Rule r : grammar.after_sections) {
                m.add(r.number);
            }
        }

        if (!grammar.null_section.isEmpty()) {
            m = runsections[-3];
            for (Rule r : grammar.null_section) {
                m.add(r.number);
            }
        }

        if (sections.isEmpty()) {
            int smax = grammar.sections.size();
            for (int i = 0; i < smax; ++i) {
                for (Rule r : grammar.rules) {
                    if (r.section < 0 || r.section > i) {
                        continue;
                    }
                    m = runsections[i];
                    m.add(r.number);
                }
            }
        }
        else {
            int numsections = sections.size();
            for (int n = 0; n < numsections; ++n) {
                for (int e = 0; e <= n; ++e) {
                    for (Rule r : grammar.rules) {
                        if (r.section != (sections.get(e) - 1)) {
                            continue;
                        }
                        m = runsections[n];
                        m.insert(r.number);
                    }
                }
            }
        }

        if (!valid_rules.isEmpty()) {
            List<Integer> vr = new ArrayList<>();
            for (Rule iter : grammar.rule_by_number) {
                if (valid_rules.contains(iter.line)) {
                    vr.addLast(iter.number);
                }
            }
            valid_rules = vr;
        }

        char[] local_utf_pattern = { ' ', '#', '%', 'u', '%', '0', '?', 'u', '→', '%', 'u', '%', '0', '?', 'u', 0 };
        char[] local_latin_pattern = { ' ', '#', '%', 'u', '%', '0', '?', 'u', '-', '>', '%', 'u', '%', '0', '?', 'u', 0 };

        int w = (int) floor(log10(hard_limit)) + 1;
        local_utf_pattern[6] = local_utf_pattern[13] = (char) ('0' + w);
        local_latin_pattern[6] = local_latin_pattern[14] = (char) ('0' + w);
        did_index = true;*/
    }

    public void setGrammar(Grammar res) { /*
        grammar = res;

        Tag tag_begin = addTag(STR_BEGINTAG);
        Tag tag_end = addTag(STR_ENDTAG);
        Tag tag_subst = addTag(STR_DUMMY);
        int begintag = tag_begin.hash;
        int endtag = tag_end.hash;
        int substtag = tag_subst.hash;

        String mp = grammar.mapping_prefix;
        int mprefix_key = addTag("_MPREFIX").hash;
        int mprefix_value = addTag(mp).hash;

        index_readingSet_yes.clear();
        index_readingSet_yes.resize(grammar.sets_list.size());
        index_readingSet_no.clear();
        index_readingSet_no.resize(grammar.sets_list.size());

        if (res.text_delimiters != null) {
            List<Tag> theTags = List.of();
            trie_getTagList(res.text_delimiters.trie, theTags);
            trie_getTagList(res.text_delimiters.trie_special, theTags);
            for (Tag t : theTags) {
                UParseError pe = null;
                UErrorCode status = U_ZERO_ERROR;
                int flags = (t.type & T_CASE_INSENSITIVE) ? UREGEX_CASE_INSENSITIVE : 0;
                List<String> text_delimiters = new ArrayList<>();
                text_delimiters.addLast(t.tag, t.tag.length(), flags, pe, status);
                if (status != U_ZERO_ERROR) {
                    System.err.println("Error: uregex_open returned " + errorName(status) + " trying to parse pattern "+ t.tag +  "- cannot continue!\n");
                    System.exit(1);
                }
            }
        } */
    }

    public void setOptions() {
    }
}
