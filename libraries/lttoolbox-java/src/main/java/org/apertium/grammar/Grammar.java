package org.apertium.grammar;

import org.apertium.lttoolbox.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apertium.grammar.Inlines.hash_value;
import static org.apertium.grammar.SetGrammar.*;
import static org.apertium.grammar.Strings.*;
import static org.apertium.grammar.Strings.Keywords.K_PROTECT;
import static org.apertium.grammar.Tag.*;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class Grammar {

    public boolean has_dep = false;
    public boolean has_bag_of_tags = false;
    public boolean has_relations = false;
    public boolean has_encl_final = false;
    public boolean has_protect = false;
    public boolean is_binary = false;
    public boolean sub_readings_ltr = false;
    public boolean ordered;
    public boolean addcohort_attach;
    public long grammar_size = 0;
    public int num_tags = 0;
    public char mapping_prefix = '@';
    public int lines = 0;
    public int verbosity_level = 0;
    public double total_time = 0;

    public List<String> cmdargs = new ArrayList<>();
    public List<String> cmdargs_override = new ArrayList<>();

    public List<Tag> single_tags_list = new ArrayList<>();
    public List<Tag> single_tags;

    public List<SetGrammar> sets_list;
    public String sets_all;
    public Map<String, SetGrammar> sets_by_name;
    public Map<String, Integer> set_name_seeds;
    public List<Pair<Integer, Object>> sets_by_contents;
    public Map<Integer, Integer>  set_alias;
    public List<SetGrammar> maybe_used_sets;

    public List<String> static_sets;
    public List<Pattern> regex_tags;
    public List<Tag> icase_tags = new ArrayList<>();

    public Map<Integer, ContextualTest> templates;
    public Map<Integer, ContextualTest> contexts;

    public Map<Integer, List<Integer>> rules_by_set;
    public Map<Integer, List<Integer>> rules_by_tag;
    public Map<Integer, byte[]> sets_by_tag;

    public List<Integer> rules_any = null;
    public byte[] sets_any = null;

    public SetGrammar delimiters = new SetGrammar();
    public SetGrammar soft_delimiters = new SetGrammar();
    public SetGrammar text_delimiters = new SetGrammar();
    public int tag_any = 0;
    public List<Integer> preferred_targets;
    public List<Integer> reopen_mappings;
    public List<Pair<Integer, Integer>> parentheses;
    public List<Pair<Integer, Integer>> parentheses_reverse;

    public List<Integer> sections = new ArrayList<>();
    public Map<Integer, Integer> anchors = new HashMap<>();

    public List<Rule> rule_by_number = new ArrayList<>();
    public List<Rule> before_sections = new ArrayList<>();
    public List<Rule> rules = new ArrayList<>();
    public  List<Rule> after_sections = new ArrayList<>();
    public List<Rule> null_section = new ArrayList<>();
    public List<Rule> wf_rules = new ArrayList<>();

    private static final int T_USED = 0;

    public void reindex() {
        for (Pair<Integer, Object> dset : sets_by_contents) {
            Tag tag = (Tag) dset.second;
            if (tag.number == Integer.MAX_VALUE) {
                tag.type |= ST_USED;
                continue;
            }
            if (!(tag.type == ST_STATIC)) {
                tag.type &= ~ST_USED;
            }
            tag.number = 0;
        }

        for (String sset : static_sets) {
            int sh = hash_value(sset);
            if (set_alias.containsValue(sh)) {
                System.err.printf("Error: Static set %S is an alias; only real sets may be made static!\n", sset);
                System.exit(1);
            }
            SetGrammar s = getSet(sh);
            if (s == null) {
                if (verbosity_level > 0) {
                    System.err.printf("Warning: Set %S was not defined, so cannot make it static.\n", sset);
                }
                continue;
            }
            if (!Objects.equals(s.name, sset)) {
                s.setName(sset);
            }
            s.markUsed(this);
            s.type |= ST_STATIC;
        }

        set_alias.clear();
        sets_by_name.clear();
        rules.clear();
        before_sections.clear();
        after_sections.clear();
        null_section.clear();
        sections.clear();
        if (!is_binary) {
            sets_list.getFirst().number = 0;
        }
        set_name_seeds.clear();
        sets_any = null;
        rules_any = null;

        for (Tag iter : single_tags_list) {
            if (iter.regexp != null && !is_textual(iter.tag)) {
                regex_tags.add(iter.regexp);
            }
            if ((iter.type == T_CASE_INSENSITIVE) && !is_textual(iter.tag)) {
                icase_tags.add(iter);
            }
            if (is_binary) {
                continue;
            }
            if (iter.vs_sets == null) {
                continue;
            }
            for (SetGrammar sit : iter.vs_sets) {
                sit.markUsed(this);
            }
        }

        for (Tag titer : single_tags_list) {
            // 1. Verificación de máscara de bits (Bitwise AND)
            /* La expresión (titer.type & T_TEXTUAL) != 0 verifica si el flag T_TEXTUAL está activo (encendido) dentro de la variable type, sin importar si hay otros flags activos al mismo tiempo.
            En cambio, titer.type == T_TEXTUAL exige que type sea exactamente igual a T_TEXTUAL, lo que significa que ningún otro flag puede estar activo.*/
            if ((titer.type & T_TEXTUAL) != 0) {
                continue;
            }

            // 2. Bloque de Expresiones Regulares
            // En Java, 'regex_tags' debe ser una colección de objetos 'java.util.regex.Pattern'
            for (Pattern pattern : regex_tags) {
                // Vincula el texto del tag actual al patrón de la expresión regular
                Matcher matcher = pattern.matcher(titer.tag);
                // uregex_find equivale a matcher.find()
                if (matcher.find()) {
                    titer.type |= T_TEXTUAL; // Operación Bitwise OR para encender el flag
                    break; // Opcional: si ya se activó el flag, puedes romper el bucle interno
                }
            }

            // 3. Bloque de comparación Case-Insensitive
            // En Java, 'icase_tags' contiene objetos con su propiedad 'tag' (String)
            for (Tag iter : icase_tags) {
                // ux_strCaseCompare equivale a equalsIgnoreCase en Java
                // Nota: Si en C++ 'ux_strCaseCompare' devuelve verdadero cuando son IGUALES:
                if (titer.tag.equalsIgnoreCase(iter.tag)) {
                    titer.type |= T_TEXTUAL;
                    break; // Opcional
                }
            }
        }

        for (Pair<Integer, Integer> it : parentheses) {
            single_tags.get(it.first).markUsed();
            single_tags.get(it.second).markUsed();
        }

        for (int it : preferred_targets) {
            single_tags.get(it).markUsed();
        }

        /* for (Rule rule : rule_by_number) {
            if (rule.wordform != null) {
                wf_rules.addLast(rule);
            }
            if (rule.type == K_PROTECT) {
                has_protect = true;
            }
            if (is_binary) {
                continue;
            }
            SetGrammar s = null;
            s = getSet(rule.target);
            s.markUsed(this);
            if (rule.childset1) {
                s = getSet(rule.childset1);
                s.markUsed(this);
            }
            if (rule.childset2) {
                s = getSet(rule.childset2);
                s.markUsed(this);
            }
            if (rule.maplist) {
                rule.maplist.markUsed(this);
            }
            if (rule.sublist) {
                rule.sublist.markUsed(this);
            }
            if (rule.dep_target != null) {
                rule.dep_target.markUsed(this);
            }
            for (ContextualTest it : rule.tests) {
                it.markUsed(this);
            }
            for (ContextualTest it : rule.dep_tests) {
                it.markUsed(this);
            }
        } */

        if (!is_binary) {
            if (delimiters != null) {
                delimiters.markUsed(this);
            }
            if (soft_delimiters != null) {
                soft_delimiters.markUsed(this);
            }
            if (text_delimiters != null) {
                text_delimiters.markUsed(this);
            }

            Map<Integer, ContextualTest> tosave = new HashMap<>();
            for (Map.Entry<Integer, ContextualTest> cntx : templates.entrySet()) {
                if (cntx.getValue().is_used) {
                    tosave.put(cntx.getKey(), cntx.getValue());
                }
            }
            templates = tosave;
            tosave.clear();
            for (Map.Entry<Integer, ContextualTest> cntx : contexts.entrySet()) {
                if (cntx.getValue().is_used) {
                    tosave.put(cntx.getKey(), cntx.getValue());
                    continue;
                }
            }
            contexts = tosave;
        }

        /* if (unused_sets != null) {
            System.out.print("Unused sets:\n");
            for (Pair<Integer, Object> rset : sets_by_contents) {
                Tag second = (Tag) rset.second;
                if (!((second.type & ST_USED) == -1) && !second.name && maybe_used_sets.contains(second)) {
                    if (!is_internal(second.name)) {
                        System.out.printf("Line %u set %S\n", second.line, second.name);
                    }
                }
            }
            System.out.print("End of unused sets.\n");
            System.out.flush();
        }

        // Stuff below this line is not optional...

        for (const auto& tset : sets_by_contents) {
            if (tset.second->type & ST_USED) {
                addSetToList(tset.second);
            }
        }

        for (const auto& iter_tags : single_tags) {
            Tag* tag = iter_tags.second;
            if (tag->tag[0] == mapping_prefix) {
                tag->type |= T_MAPPING;
            }
            else {
                tag->type &= ~T_MAPPING;
            }
        }

        if (!is_binary) {
            for (auto set : sets_list) {
                set->reindex(*this);
            }
            for (auto set : sets_list) {
                setAdjustSets(set);
            }
        }
        for (auto set : sets_list) {
            indexSets(set->number, set);
        }

        SortedSet<Integer> sects;

        for (Rule rule : rule_by_number) {
            if (rule.section == -1) {
                before_sections.addLast(rule);
            }
            else if (rule.section == -2) {
                after_sections.addLast(rule);
            }
            else if (rule.section == -3) {
                null_section.addLast(rule);
            }
            else {
                sects.add(rule.section);
                rules.addLast(rule);
            }
            if (rule->target) {
                Set* set = nullptr;
                if (is_binary) {
                    set = sets_list[rule->target];
                }
                else {
                    set = sets_by_contents.find(rule->target)->second;
                    rule->target = set->number;
                }
                indexSetToRule(rule->number, set);
                rules_by_set[rule->target].insert(rule->number);
            }
            else {
                u_fprintf(ux_stderr, "Warning: Rule on line %u had no target.\n", rule->line);
                u_fflush(ux_stderr);
            }
            if ((rule->maplist && (rule->maplist->type & ST_CHILD_UNIFY)) || (rule->sublist && (rule->sublist->type & ST_CHILD_UNIFY))) {
                rule->flags |= RF_CAPTURE_UNIF;
            }
            if (is_binary) {
                continue;
            }
            if (rule->childset1) {
                Set* set = sets_by_contents.find(rule->childset1)->second;
                rule->childset1 = set->number;
            }
            if (rule->childset2) {
                Set* set = sets_by_contents.find(rule->childset2)->second;
                rule->childset2 = set->number;
            }
            if (rule->dep_target) {
                contextAdjustTarget(rule->dep_target);
            }
            for (auto test : rule->tests) {
                contextAdjustTarget(test);
            }
            for (auto test : rule->dep_tests) {
                contextAdjustTarget(test);
            }
        }

        if (!sects.empty()) {
            for (uint32_t i = 0; i <= sects.back(); ++i) {
                sections.push_back(i);
            }
        }

        if (sets_by_tag.find(tag_any) != sets_by_tag.end()) {
            sets_any = sets_by_tag[tag_any];
        }
        if (rules_by_tag.find(tag_any) != rules_by_tag.end()) {
            rules_any = rules_by_tag[tag_any];
        }

        sets_by_contents.clear();

        for (auto to : sets_list) {
            if (to->type & ST_STATIC) {
                uint32_t nhash = hash_value(to->name);
			const uint32_t cnum = to->number;

                if (sets_by_name.find(nhash) == sets_by_name.end()) {
                    sets_by_name[nhash] = cnum;
                }
                else if (cnum != sets_list[sets_by_name.find(nhash)->second]->number) {
                    Set* a = sets_list[sets_by_name.find(nhash)->second];
                    if (a->name == to->name) {
                        u_fprintf(ux_stderr, "Error: Static set %S already defined. Redefinition attempted!\n", a->name.data());
                        CG3Quit(1);
                    }
                    else {
                        for (uint32_t seed = 0; seed < 1000; ++seed) {
                            if (sets_by_name.find(nhash + seed) == sets_by_name.end()) {
                                if (verbosity_level > 0) {
                                    u_fprintf(ux_stderr, "Warning: Static set %S got hash seed %u.\n", to->name.data(), seed);
                                    u_fflush(ux_stderr);
                                }
                                set_name_seeds[to->name] = seed;
                                sets_by_name[nhash + seed] = cnum;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Gather knowledge of which sets have varstrings
        boost::dynamic_bitset<> sets_vstr(sets_list.size());
        bool did = true;
        while (did) {
            did = false;
            for (auto set : sets_list) {
                if (sets_vstr.test(set->number)) {
                    continue;
                }
                for (auto iset : set->sets) {
                    if (sets_vstr.test(iset)) {
                        sets_vstr.set(set->number);
                        did = true;
                        break;
                    }
                }
                if (trie_hasType(set->trie, T_VARSTRING) || trie_hasType(set->trie_special, T_VARSTRING)) {
                    sets_vstr.set(set->number);
                    did = true;
                }
            }
        }

        // Gather knowledge of which contexts use unification or varstrings
        bc::flat_set<ContextualTest*> nk;
        did = true;
        while (did) {
            did = false;

            for (auto& cntx : contexts) {
                ContextualTest t = cntx.second;

                if (nk.count(t)) {
                    continue;
                }

                if (t->tmpl && nk.count(t->tmpl)) {
                    //u_fprintf(ux_stderr, "added tmpl %u\n", t->line);
                    did |= nk.insert(t).second;
                    continue;
                }
                if (t.linked && nk.count(t->linked)) {
                    //u_fprintf(ux_stderr, "added linked %u\n", t->line);
                    did |= nk.insert(t).second;
                    continue;
                }
                if (t->target && (sets_list[t->target]->type & MASK_ST_UNIFY)) {
                    //u_fprintf(ux_stderr, "added target %u\n", t->line);
                    did |= nk.insert(t).second;
                    continue;
                }
                if (t->target && sets_vstr.test(t->target)) {
                    //u_fprintf(ux_stderr, "added target %u\n", t->line);
                    did |= nk.insert(t).second;
                    continue;
                }
                if (t->barrier && (sets_list[t->barrier]->type & MASK_ST_UNIFY)) {
                    //u_fprintf(ux_stderr, "added barrier %u\n", t->line);
                    did |= nk.insert(t).second;
                    continue;
                }
                if (t->barrier && sets_vstr.test(t->barrier)) {
                    //u_fprintf(ux_stderr, "added target %u\n", t->line);
                    did |= nk.insert(t).second;
                    continue;
                }
                if (t->cbarrier && (sets_list[t->cbarrier]->type & MASK_ST_UNIFY)) {
                    //u_fprintf(ux_stderr, "added cbarrier %u\n", t->line);
                    did |= nk.insert(t).second;
                    continue;
                }
                if (t->cbarrier && sets_vstr.test(t->cbarrier)) {
                    //u_fprintf(ux_stderr, "added target %u\n", t->line);
                    did |= nk.insert(t).second;
                    continue;
                }
            }
        }

        for (Rule r : rule_by_number) {
            // Determine whether this rule probably needs KEEPORDER
            if (r.flags & RF_KEEPORDER) {
                continue;
            }
		/* While this is a good indication that the unified set is used in the target, it is not 100%
		if (r->target && (sets_list[r->target]->type & MASK_ST_UNIFY)) {
			continue;
		}
		//
            boolean needs = false;
            if (r.sublist && sets_vstr.test(r.sublist.number)) {
                needs = true;
            }
            if (r.maplist && sets_vstr.test(r.maplist.number)) {
                needs = true;
            }
            if (r->dep_target && nk.count(r->dep_target)) {
                needs = true;
            }
            for (int cntx : r.tests) {
                if (nk.count(cntx)) {
                    needs = true;
                }
            }
            for (int cntx : r.dep_tests) {
                if (nk.count(cntx)) {
                    needs = true;
                }
            }
            if (needs) {
                r.flags |= RF_KEEPORDER;
			/* We can do the whole thing fully automatically, so explicit KEEPORDER will eventually be deprecated
			if (verbosity_level) {
				u_fprintf(ux_stderr, "Warning: Rule on line %u probably needs KEEPORDER.\n", r->line);
				u_fflush(ux_stderr);
			}
			//
            }
        }

        if (used_tags) {
            for (Tag iter_tags : single_tags) {
                Tag tag = iter_tags;
                if (tag.type & T_USED) {
                    String tmp = tag.toString();
                    System.out.printf("%S\n", tmp);
                }
            }
            System.exit(0);
        }*/
    }

    private boolean is_textual(String tag) {
        return true;
    }

    private SetGrammar getSet(int which) {
        Pair<Integer, Object> iter = sets_by_contents.get(which);
        if (iter != null) {
            return (SetGrammar) iter.second;
        }
        /* else {
            SetGrammar iter2 = sets_by_name.get(which);
            if (iter != null) {
                boolean citer = sets_by_contents.get(iter2.second);
                if (citer != -1) {
                    int iter2 = set_name_seeds.find(citer.second.name);
                    if (iter2 != -1) {
                        return getSet(iter.second + iter2.second);
                    }
                    else {
                        return citer.second;
                    }
                }
            }
        }*/
        return new SetGrammar();
    }


    public void allocateDummySet() {
        SetGrammar set_c = allocateSet();
        set_c.line = 0;
        set_c.setName(STR_DUMMY);
        Tag t = allocateTag(STR_DUMMY);
        addTagToSet(t, set_c);
        addSet(set_c);
        set_c.number = Integer.MAX_VALUE;
        sets_list.addFirst(set_c);
    }

    public SetGrammar allocateSet() {
        return new SetGrammar();
    }

    public void addTagToSet(Tag t, SetGrammar setC) {

    }

    public void addAnchor(String to, int at, boolean primary) {
        int ah = allocateTag(to).hash;
        boolean it = anchors.containsKey(ah);
        if (primary && !it) {
            System.err.printf("Error: Redefinition attempt for anchor '%S' on line %d!\n", to, lines);
            System.exit(1);
        }
        if (at > rule_by_number.size()) {
            System.err.printf("Warning: No corresponding rule available for anchor '%S' on line %d!\n", to, lines);
            at = rule_by_number.size();
        }
        if (!it) {
            anchors.put(ah,at);
        }
    }

    private Tag allocateTag(String txt) {
        if (txt.charAt(0) == 0) {
            System.err.printf("Error: Empty tag on line %d! Forgot to fill in a ()?\n", lines);
            System.exit(1);
        }
        if (txt.charAt(0) == '(') {
            System.err.printf("Error: Tag '%S' cannot start with ( on line %d! Possible extra opening ( or missing closing ) to the left. If you really meant it, escape it as \\(.\n", txt, lines);
            System.exit(1);
        }

        int thash = hash_value(txt);
        Tag it;
        /* if ((it = single_tags.get(thash)) != -1 && !it->second->tag.empty() && it->second->tag == txt) {
            return it.second;
        } */

        Tag tag = new Tag();
        tag.parseTagRaw(txt.toCharArray(), this);
        return addTag(tag);
    }

    private Tag addTag(Tag tag) {
        int hash = tag.rehash();
        for (int seed = 0; seed < 10000; ++seed) {
            int ih = hash + seed;
            if (single_tags.contains(ih)) {
                Tag t = single_tags.get(ih);
                if (t == tag) {
                    return tag;
                }
                if (Objects.equals(t.tag, tag.tag)) {
                    hash += seed;
                    break;
                }
            }
            else {
                if (verbosity_level > 0 && seed != 0) {
                    System.err.printf("Warning: Tag %S got hash seed %d.\n", tag.tag, seed);
                    System.err.flush();
                }
                tag.seed = seed;
                hash = tag.rehash();
                single_tags_list.addLast(tag);
                tag.number = single_tags_list.size() - 1;
                single_tags.set(hash, tag);
                break;
            }
        }
        return single_tags.get(hash);
    }

    public List<Tag> getTagList_Any(boolean maplist) {
        return null;
    }

    public ContextualTest allocateContextualTest() {
        return null;
    }

    public Integer removeNumericTags(int target) {
        return 0;
    }

    public ContextualTest addContextualTest(ContextualTest unsafec) {
        return null;
    }

    public void addSet(SetGrammar to) {
        if (delimiters == null && Objects.equals(to.name, STR_DELIMITSET)) {
            delimiters = to;
        }
        else if (soft_delimiters == null && Objects.equals(to.name, STR_SOFTDELIMITSET)) {
            soft_delimiters = to;
        }
        else if (text_delimiters == null && Objects.equals(to.name, STR_TEXTDELIMITSET)) {
            text_delimiters = to;
        }
        if (verbosity_level > 0 && to.name.charAt(0) == 'T' && to.name.charAt(1) == ':') {
            System.err.printf("Warning: Set name %S looks like a misattempt of template usage on line %d.\n", to.name, to.line);
        }

        /* if (!to.sets.isEmpty() && !(to.type == (ST_TAG_UNIFY | ST_CHILD_UNIFY | ST_SET_UNIFY))) {
            boolean all_tags = true;
            for (int i = 0; i < to.sets.size(); ++i) {
                if (i > 0 && to.set_ops.get(i - 1) != S_OR) {
                    all_tags = false;
                    break;
                }
                SetGrammar s = getSet(to.sets.get(i));
                if (!s.sets.isEmpty()) {
                    all_tags = false;
                    break;
                }
                if (!s.trie.isEmpty() && !s.trie_special.isEmpty()) {
                    all_tags = false;
                    break;
                }
                if (s.getNonEmpty().size() != 1 || !trie_singular(s.getNonEmpty())) {
                    all_tags = false;
                    break;
                }
            }

            if (all_tags) {
                for (int i : to.sets) {
                    SetGrammar s = getSet(i);
                    maybe_used_sets.add(s);
                    List<Tag> tv = trie_getTagList(s.getNonEmpty());
                    if (tv.size() == 1) {
                        addTagToSet(tv.get(0), to);
                    }
                    else {
                        boolean special = false;
                        for (Tag tag : tv) {
                            if (tag.type == T_SPECIAL) {
                                special = true;
                                break;
                            }
                        }
                        if (special) {
                            trie_insert(to.trie_special, tv);
                        }
                        else {
                            trie_insert(to.trie, tv);
                        }
                    }
                }
                to->sets.clear();
                to->set_ops.clear();

                to->reindex(*this);
                if (verbosity_level > 1 && !is_internal(to->name)) {
                    u_fprintf(ux_stderr, "Info: SET %S on line %u changed to a LIST.\n", to->name.data(), to->line);
                    u_fflush(ux_stderr);
                }
            }
        }

        // If there are failfast tags, and if they don't comprise the whole of the set, split the set into Positive - Negative
        if (!to->ff_tags.empty() && to->ff_tags.size() < (to->trie.size() + to->trie_special.size())) {
            SetGrammar positive = allocateSet();
            SetGrammar negative = allocateSet();

            String str;
            str = STR_GPREFIX;
            str += to.name;
            str += '_';
            str += STR_POSITIVE;
            positive.setName(str);
            str = STR_GPREFIX;
            str += to.name;
            str += '_';
            str += STR_NEGATIVE;
            negative.setName(str);

            // positive.trie = to.trie;swap(to->trie);
            // positive->trie_special.swap(to->trie_special);

            for (auto iter : to->ff_tags) {
                auto pit = positive->trie_special.find(iter);
                if (pit != positive->trie_special.end()) {
                    if (pit->second.terminal) {
                        if (pit->second.trie) {
                            trie_delete(*pit->second.trie);
                        }
                        positive->trie_special.erase(pit);
                    }
                }
                Tag* tag = new Tag(*iter);
                tag->type &= ~T_FAILFAST;
                tag = addTag(tag);
                addTagToSet(tag, negative);
            }

            positive.reindex(this);
            negative.reindex(this);
            addSet(positive);
            addSet(negative);

            to.ff_tags.clear();

            to.sets.addLast(positive.hash);
            to.sets.addLast(negative.hash);
            to->set_ops.push_back(S_MINUS);

            to->reindex(*this);
            if (verbosity_level > 1) {
                u_fprintf(ux_stderr, "Info: LIST %S on line %u was split into two sets.\n", to->name.data(), to->line);
                u_fflush(ux_stderr);
            }
        }

        uint32_t chash = to->rehash();
        for (; !is_internal(to->name);) {
            uint32_t nhash = hash_value(to->name.data());
            if (sets_by_name.find(nhash) != sets_by_name.end()) {
                Set* a = sets_by_contents.find(sets_by_name.find(nhash)->second)->second;
                if (a == to || a->hash == to->hash) {
                    break;
                }
            }
            if (set_name_seeds.find(to->name) != set_name_seeds.end()) {
                nhash += set_name_seeds[to->name];
            }
            if (sets_by_name.find(nhash) == sets_by_name.end()) {
                sets_by_name[nhash] = chash;
                break;
            }
            else if (chash != sets_by_contents.find(sets_by_name.find(nhash)->second)->second->hash) {
                Set* a = sets_by_contents.find(sets_by_name.find(nhash)->second)->second;
                if (a->name == to->name) {
                    u_fprintf(ux_stderr, "Error: Set %S already defined at line %u. Redefinition attempted at line %u!\n", a->name.data(), a->line, to->line);
                    CG3Quit(1);
                }
                else {
                    for (uint32_t seed = 0; seed < 1000; ++seed) {
                        if (sets_by_name.find(nhash + seed) == sets_by_name.end()) {
                            if (verbosity_level > 0 && !is_internal(to->name)) {
                                u_fprintf(ux_stderr, "Warning: Set %S got hash seed %u.\n", to->name.data(), seed);
                                u_fflush(ux_stderr);
                            }
                            set_name_seeds[to.name] = seed;
                            sets_by_name[nhash + seed] = chash;
                            break;
                        }
                    }
                }
            }
            break;
        }
        if (sets_by_contents.find(chash) == sets_by_contents.end()) {
            sets_by_contents[chash] = to;
        }
        else {
            Set* a = sets_by_contents.find(chash)->second;
            if (a != to) {
                a->reindex(*this);
                to->reindex(*this);
                if ((a->type & (ST_SPECIAL | ST_TAG_UNIFY | ST_CHILD_UNIFY | ST_SET_UNIFY)) != (to->type & (ST_SPECIAL | ST_TAG_UNIFY | ST_CHILD_UNIFY | ST_SET_UNIFY)) || a->set_ops.size() != to->set_ops.size() || a->sets.size() != to->sets.size() || a->trie.size() != to->trie.size() || a->trie_special.size() != to->trie_special.size()) {
                    u_fprintf(ux_stderr, "Error: Content hash collision between set %S on line %u and %S on line %u!\n", a->name.data(), a->line, to->name.data(), to->line);
                    CG3Quit(1);
                }
                destroySet(to);
            }
        }
        to = sets_by_contents[chash]; */
    }
}
