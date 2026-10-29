package org.apertium.grammar;

import org.apertium.lttoolbox.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.Math.floor;
import static java.lang.Math.log10;
import static org.apertium.grammar.Inlines.hash_value;
import static org.apertium.grammar.Strings.*;
import static org.apertium.grammar.Tag.*;
import static org.apertium.utils.MiscUtils.findAndReplace;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class GrammarApplicator {

    public Grammar grammar;
    public Profiler profiler;

    public boolean always_span = false;
    public boolean apply_mappings = true;
    public boolean apply_corrections = true;
    public boolean no_before_sections = false;
    public boolean no_sections = false;
    public boolean no_after_sections = false;
    public boolean trace = false;
    public boolean trace_name_only = false;
    public boolean trace_no_removed = false;
    public boolean trace_encl = false;
    public boolean allow_magic_readings = true;
    public boolean no_pass_origin = false;
    public boolean unsafe = false;
    public boolean ordered = false;
    public boolean show_end_tags = false;
    public boolean unicode_tags = false;
    public boolean unique_tags = false;
    public boolean dry_run = false;
    public boolean owns_grammar = false;
    public boolean input_eof = false;
    public boolean seen_barrier = false;
    public boolean is_conv = false;
    public boolean split_mappings = false;
    public boolean pipe_deleted = false;
    public boolean add_spacing = true;
    public boolean print_ids = false;

    public boolean dep_has_spanned = false;
    public int dep_delimit = 0;
    public boolean dep_absolute = false;
    public boolean dep_original = false;
    public boolean dep_block_loops = true;
    public boolean dep_block_crossing = false;

    public int num_windows = 2;
    public int soft_limit = 300;
    public int hard_limit = 500;
    public List<Integer> sections = new ArrayList<>();
    public List<Integer> valid_rules;
    public List<Integer> trace_rules;
    public List<Integer> debug_rules;
    public Map<String, Integer> variables = new HashMap<>();
    public int verbosity_level = 0;
    public int debug_level = 0;
    public int section_max_count = 0;

    public boolean has_dep = false;
    public static boolean parse_dep = false;
    public int dep_highest_seen = 0;
    public static Window gWindow = null;

    public static boolean has_relations = false;

    protected String span_pattern_latin;
    protected String span_pattern_utf;
    protected char[] ws = new char[]{' ', '\t', '0', 0};

    protected int numLines = 0;
    protected int numWindows = 0;
    protected int numCohorts = 0;
    protected int numReadings = 0;

    protected boolean did_index = false;
    protected List<Pair<Integer, Integer>> dep_deep_seen = new ArrayList<>();

    protected int numsections = 0;
    protected Map<Integer, List<Integer>> runsections;
    protected Map<Integer, Process> externals;

    List<Integer> ci_depths = new ArrayList<>();
    /* Map<Integer, CohortIterator> cohortIterators;
    std::map<uint32_t, TopologyLeftIter> topologyLeftIters;
    std::map<uint32_t, TopologyRightIter> topologyRightIters;
    std::map<uint32_t, DepParentIter> depParentIters;
    std::map<uint32_t, DepDescendentIter> depDescendentIters;
    std::map<uint32_t, DepAncestorIter> depAncestorIters;*/

    protected int match_single = 0, match_comp = 0, match_sub = 0;
    protected int begintag = 0, endtag = 0, substtag = 0;
    protected Tag tag_begin = null, tag_end = null, tag_subst = null;
    protected int par_left_tag = 0, par_right_tag = 0;
    protected int par_left_pos = 0, par_right_pos = 0;
    protected boolean did_final_enclosure = false;
    protected int mprefix_key = 0, mprefix_value = 0;

    // tmpl_context_t tmpl_cntx;

    // std::vector<regexgrps_t> regexgrps_store;
    // bc::flat_map<uint32_t, uint8_t> regexgrps_z;
    // bc::flat_map<uint32_t, regexgrps_t*> regexgrps_c;
    protected int same_basic = 0;
    protected Cohort rule_target = null;
    protected Cohort context_target = null;
    protected Cohort merge_with = null;
    protected Rule current_rule = null;
    protected List<RuleContext> context_stack;
    // List<CohortSet> cohortsets;*/
    protected List<Integer> rocits;

    protected Map<Integer, Reading> readings_plain;
    protected List<Pattern> text_delimiters;

    protected Map<Integer,Map<Integer, Object>> unif_tags_rs = new HashMap<>();
    protected List<Map<Integer, Object>> unif_tags_store = new ArrayList<>();

    protected Map<Integer, Map<Integer, List<Integer>>> unif_sets_rs = new HashMap<>();
    protected List<Map<Integer, List<Integer>>> unif_sets_store = new ArrayList<>();
    protected int unif_last_wordform = 0;
    protected int unif_last_baseform = 0;
    protected int unif_last_textual = 0;
    protected Map<Integer, Integer> rule_hits;

    protected Deque<List<Tag>> ss_taglist = new ArrayDeque<>();
    protected Deque<Map<Integer, Object>> ss_utags = new ArrayDeque<>();
    protected Deque<Map<Integer, List<Integer>>> ss_usets = new  ArrayDeque<>();
    protected Deque<List<Integer>> ss_u32sv;

    protected Set<Long> index_regexp_yes = new HashSet<>();
    protected Set<Long> index_regexp_no;
    protected Set<Long> index_icase_yes;
    protected Set<Long> index_icase_no;
    protected List<Set<Integer>> index_readingSet_yes;
    protected List<Set<Integer>> index_readingSet_no;
    protected Set<Integer> index_ruleCohort_no;

    // set these to false to break the 2 loops in runSingleRule()
    protected boolean reset_cohorts_for_loop = false;
    protected boolean finish_reading_loop = true;
    protected boolean finish_cohort_loop = true;
    protected boolean in_nested = false;
    protected int used_regex = 0;

    int TRV_BREAK         = 1;
    int TRV_BARRIER       = (1 <<  1);
    int TRV_BREAK_DEFAULT = (1 <<  2);

    Deque<Reading> subs_any = new ArrayDeque<>();

    static class ReadingSpec {
        Cohort cohort = null;
        Reading reading = null;
        Reading subreading = null;
    };

    public static class RuleContext {
        ReadingSpec target;
        List<Cohort> context;
        List<Cohort> dep_context;
        ReadingSpec attach_to;
        Cohort mark = null;
        Map<Integer, Object> unif_tags = null;
        Map<Integer, List<Integer>> unif_sets = null;
        int regexgrp_ct = 0;
        // regexgrps_t* regexgrps = nullptr;
        boolean is_with = false;
    };

    protected static class ST_RETVALS {
        int TRV_BREAK         = 1;
        int TRV_BARRIER       = (1 <<  1);
        int TRV_BREAK_DEFAULT = (1 <<  2);
    };

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
         if (!add_spacing) {
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

        List<Integer> m;
        if (!grammar.before_sections.isEmpty()) {
            m = runsections.get(-1);
            for (Rule r : grammar.before_sections) {
                m.add(r.number);
            }
        }

        if (!grammar.after_sections.isEmpty()) {
            m = runsections.get(-2);
            for (Rule r : grammar.after_sections) {
                m.add(r.number);
            }
        }

        if (!grammar.null_section.isEmpty()) {
            m = runsections.get(-3);
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
                    m = runsections.get(i);
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
                        m = runsections.get(n);
                        m.add(r.number);
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
        did_index = true;
    }

    public void setGrammar(Grammar res) {
        grammar = res;

        Tag tag_begin = addTag(STR_BEGINTAG);
        Tag tag_end = addTag(STR_ENDTAG);
        Tag tag_subst = addTag(STR_DUMMY);
        int begintag = tag_begin.hash;
        int endtag = tag_end.hash;
        int substtag = tag_subst.hash;

        char mp = grammar.mapping_prefix;
        int mprefix_key = addTag("_MPREFIX").hash;
        int mprefix_value = addTag(String.valueOf(mp)).hash;

        index_readingSet_yes = new ArrayList<>(grammar.sets_list.size());
        index_readingSet_no = new ArrayList<>(grammar.sets_list.size());

        /* if (res.text_delimiters != null) {
            List<Tag> theTags = List.of();
            trie_getTagList(res.text_delimiters.trie, theTags);
            trie_getTagList(res.text_delimiters.trie_special, theTags);
            for (Tag t : theTags) {
                UParseError pe = null;
                int status = 0;
                int flags = (t.type == T_CASE_INSENSITIVE) ? UREGEX_CASE_INSENSITIVE : 0;
                List<String> text_delimiters = new ArrayList<>();
                text_delimiters.addLast(t.tag, t.tag.length(), flags, pe, status);
                if (status != 0) {
                    System.err.println("Error: uregex_open returned " + errorName(status) + " trying to parse pattern "+ t.tag +  "- cannot continue!\n");
                    System.exit(1);
                }
            }
        } */
    }

    public void setOptions() {

    }

    public int addTagToReading(Reading reading, Tag tag, boolean rehash) {
        if (tag.type == T_VARSTRING) {
            tag = generateVarstringTag(tag);
        }

        byte[] it = grammar.sets_by_tag.get(tag.hash);
        /* if (it != null) {
            reading.parent.possible_sets
            reading.parent.possible_sets.resize(std::max(reading.parent->possible_sets.size(), it->second.size()));
            reading.parent.possible_sets |= it;
        } */
        reading.tags.add(tag.hash);
        reading.tags_list.addLast(tag.hash);
        reading.tags_bloom.insert(tag.hash);
        // ToDo: Remove for real ordered mode
        if (ordered) {
            if (!reading.tags_string.isEmpty()) {
                reading.tags_string += ' ';
            }
            reading.tags_string += tag.tag;
            reading.tags_string_hash = hash_value(reading.tags_string);
        }
        /* if (grammar.parentheses.find(tag.hash) != -1) {
            reading.parent.is_pleft = tag.hash;
        }
        if (grammar.parentheses_reverse.find(tag.hash) != -1) {
            reading.parent.is_pright = tag.hash;
        } */

        if (tag.type == T_MAPPING || tag.tag.charAt(0) == grammar.mapping_prefix) {
            tag.type |= T_MAPPING;
            if (reading.mapping != null && reading.mapping != tag) {
                System.err.print("Error: addTagToReading() cannot add a mapping tag to a reading which already is mapped!\n");
                System.exit(1);
            }
            reading.mapping = tag;
        }
        if (tag.type == (T_TEXTUAL | T_WORDFORM | T_BASEFORM)) {
            reading.tags_textual.add(tag.hash);
            reading.tags_textual_bloom.insert(tag.hash);
        }
        /* if (tag.type == T_NUMERICAL) {
            reading.tags_numerical.put(tag.hash, tag);
            reading.parent.type &= ~CT_NUM_CURRENT;
        }
        if (reading.baseform == 0 && (tag.type == T_BASEFORM)) {
            reading.baseform = tag.hash;
        }
        if (parse_dep && (tag.type & T_DEPENDENCY) && !(reading.parent.type & CT_DEP_DONE)) {
            reading.parent->dep_self = tag->dep_self;
            reading.parent->dep_parent = tag->dep_parent;
            if (tag->dep_parent == tag->dep_self) {
                reading.parent->dep_parent = DEP_NO_PARENT;
            }
            has_dep = true;
        }
        if (grammar.has_relations && (tag.type & T_RELATION)) {
            if (tag->dep_parent && tag->comparison_hash) {
                reading.parent->relations_input[tag->comparison_hash].insert(tag->dep_parent);
            }
            if (tag->dep_self) {
                gWindow->relation_map[tag->dep_self] = reading.parent->global_number;
            }
            has_relations = true;
            reading.parent.setRelated();
        }
        if (!(tag->type & T_SPECIAL)) {
            reading.tags_plain.insert(tag->hash);
            reading.tags_plain_bloom.add(tag.hash);
        }
        if (rehash) {
            reading.rehash();
        } */

        if (grammar.has_bag_of_tags) {
            Reading bot = reading.parent.parent.bag_of_tags;
            bot.tags.add(tag.hash);
            bot.tags_list.addLast(tag.hash);
            bot.tags_bloom.insert(tag.hash);

            if (tag.type == (T_TEXTUAL | T_WORDFORM | T_BASEFORM)) {
                bot.tags_textual.add(tag.hash);
                bot.tags_textual_bloom.insert(tag.hash);
            }
            if (tag.type == T_NUMERICAL) {
                bot.tags_numerical.put(tag.hash, tag);
            }
            if (reading.baseform != 1 && (tag.type == T_BASEFORM)) {
                bot.baseform = tag.hash;
            }
            if (!(tag.type == T_SPECIAL)) {
                bot.tags_plain.add(tag.hash);
                bot.tags_plain_bloom.insert(tag.hash);
            }
            if (rehash) {
                // bot.rehash();
            }
        }

        return tag.hash;
    }

    private Tag generateVarstringTag(Tag tag) {
        StringBuilder tmp = new StringBuilder();
        tmp.append(tag.tag);
        boolean did_something = false;
        // Convert %[UuLl] and $1-9 markers to control codes to avoid having combined %$1 accidentally match %L or have $s in data be filled
        String[] raw = { STR_VSu_raw, STR_VSU_raw, STR_VSl_raw, STR_VSL_raw, STR_VS1_raw, STR_VS2_raw, STR_VS3_raw, STR_VS4_raw, STR_VS5_raw, STR_VS6_raw, STR_VS7_raw, STR_VS8_raw, STR_VS9_raw };
        String[] x01 = { STR_VSu, STR_VSU, STR_VSl, STR_VSL, STR_VS1, STR_VS2, STR_VS3, STR_VS4, STR_VS5, STR_VS6, STR_VS7, STR_VS8, STR_VS9 };

        for(int i=0; i<13; ++i){
            findAndReplace(tmp, raw[i], x01[i]);
        }

        // Replace unified sets with their matching tags
        /* if (tag.vs_sets != null) {
            for (int i = 0; i < tag.vs_sets.size(); ++i) {
                boolean tags = ss_taglist.get();
                getTagList(tag.vs_sets.get(i), tags);
                StringBuilder rpl = new StringBuilder();
                // If there are multiple tags, such as from CompositeTags, put _ between them
                int j;
                for(j=0; j<tags.size(); j++){
                    rpl.append(tags.get(j).tag);
                    if(tags.size()-j > 1) {
                        rpl.append('_');
                    }
                }
                if (findAndReplace(tmp, tag.vs_names.get(i), rpl.toString()) == 1) {
                    did_something = true;
                }
            }
        }

        // Replace $1-$9 with their respective match groups
        constexpr UStringView grp[] = { STR_VS1, STR_VS2, STR_VS3, STR_VS4, STR_VS5, STR_VS6, STR_VS7, STR_VS8, STR_VS9 };
        for (size_t i = 0; i < context_stack.back().regexgrp_ct && i < 9; ++i) {
            if (findAndReplace(tmp, grp[i].data(), USV((*context_stack.back().regexgrps)[i]))) {
                did_something = true;
            }
        }

        // Handle %U %u %L %l markers.
        bool found;
        do {
            found = false;
            int32_t pos = -1, mpos = -1;
            if ((pos = tmp.lastIndexOf(STR_VSu.data(), SI32(STR_VSu.size()), 0)) != -1) {
                found = true;
                mpos = std::max(mpos, pos);
            }
            if ((pos = tmp.lastIndexOf(STR_VSU.data(), SI32(STR_VSU.size()), mpos)) != -1) {
                found = true;
                mpos = std::max(mpos, pos);
            }
            if ((pos = tmp.lastIndexOf(STR_VSl.data(), SI32(STR_VSl.size()), mpos)) != -1) {
                found = true;
                mpos = std::max(mpos, pos);
            }
            if ((pos = tmp.lastIndexOf(STR_VSL.data(), SI32(STR_VSL.size()), mpos)) != -1) {
                found = true;
                mpos = std::max(mpos, pos);
            }
            if (found && mpos != -1) {
                UChar mode = tmp[mpos + 1];
                tmp.remove(mpos, 2);
                if (mode == 'u') {
                    UnicodeString range(tmp, mpos, 1);
                    range.toUpper();
                    tmp.setCharAt(mpos, range[0]);
                }
                else if (mode == 'U') {
                    UnicodeString range(tmp, mpos);
                    range.toUpper();
                    tmp.truncate(mpos);
                    tmp.append(range);
                }
                else if (mode == 'l') {
                    UnicodeString range(tmp, mpos, 1);
                    range.toLower();
                    tmp.setCharAt(mpos, range[0]);
                }
                else if (mode == 'L') {
                    UnicodeString range(tmp, mpos);
                    range.toLower();
                    tmp.truncate(mpos);
                    tmp.append(range);
                }
                did_something = true;
            }
        } while (found);

        if (tag->type & T_CASE_INSENSITIVE) {
            tmp += 'i';
        }
        if (tag->type & T_REGEXP) {
            tmp += 'r';
        }

	const UChar* nt = tmp.getTerminatedBuffer();
        if (!did_something && nt == tag->tag) {
            u_fprintf(ux_stderr, "Warning: Unable to generate from tag '%S'! Possibly missing KEEPORDER and/or capturing regex from grammar on line %u before input line %u.\n", tag->tag.data(), grammar->lines, numLines);
            u_fflush(ux_stderr);
        }
        return addTag(nt, tag->type);
         */
        return addTag("0");
    }

    private List<Tag> getTagList(SetGrammar theSet, List<Tag> theTags, boolean unif_mode) {
        // if (theSet.type == ST_SET_UNIFY) {
		// int usets = (context_stack.back().unif_sets)[theSet.number];
		/* SetGrammar pSet = grammar.sets_list.get(theSet.sets.getFirst());
            for (int iter : pSet.sets) {
                if (usets.count(iter)) {
                    getTagList(grammar.sets_list.get(iter), theTags);
                }
            }
        }
        else if (theSet.type == ST_TAG_UNIFY) {
            for (int iter : theSet.sets) {
                getTagList(grammar.sets_list.get(iter), theTags, true);
            }
        }
        else if (!theSet.sets.isEmpty()) {
            for (int iter : theSet.sets) {
                getTagList(grammar.sets_list.get(iter), theTags, unif_mode);
            }
        }
        else if (unif_mode) {
            auto unif_tags = context_stack.back().unif_tags;
            int iter = unif_tags.find(theSet.number);
            if (iter != -1) {
                trie_getTagList(theSet.trie, theTags, iter->second);
                trie_getTagList(theSet.trie_special, theTags, iter->second);
            }
        }
        else {
            trie_getTagList(theSet.trie, theTags);
            trie_getTagList(theSet.trie_special, theTags);
        } */
        // Eliminate consecutive duplicates. Not all duplicates, since AddCohort and Append may have multiple readings with repeated tags
        if (theTags != null && theTags.size() > 1) {
            for (int i = 0; i < theTags.size() - 1; ) {
                // Compara el elemento actual con el siguiente (distancia == 1)
                if (Objects.equals(theTags.get(i), theTags.get(i + 1))) {
                    theTags.remove(i + 1); // Borra el duplicado consecutivo
                    // No incrementamos 'i' porque el siguiente elemento se desplazó a la posición i + 1
                } else {
                    i++; // Solo avanzamos si no hubo borrado
                }
            }
        }
        return theTags;
    }

    private List<Tag> getTagList(SetGrammar theSet, boolean unif_mode) {
        List<Tag> theTags = new ArrayList<>();
        getTagList(theSet, theTags, unif_mode);
        return theTags;
    }

    public int addTagToReading(Reading reading, int utag, boolean rehash){
        Tag tag = new Tag();
        // Tag tag = grammar.single_tags.find(utag).second;
        return addTagToReading(reading, tag, rehash);
    }
}
