package org.apertium.grammar;

import static org.apertium.grammar.Cohort.DEP_NO_PARENT;
import static org.apertium.grammar.GrammarApplicator.*;
import static org.apertium.grammar.Strings.*;
import static org.apertium.grammar.Tag.*;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class GrammarApplicatorReflow {
    public void addTagToReading(Reading iter, int endtag) {

    }

    public int addTagToReading(Reading i, int e, boolean h){
        Tag tag = grammar.single_tags.find(utag).second;
        return addTagToReading(i, tag, h);
    }

    public int addTagToReading(Reading reading, Tag tag, boolean h){
        if (tag.type & T_VARSTRING) {
            tag = generateVarstringTag(tag);
        }

        byte[] it = grammar.sets_by_tag.get(tag.hash);
        if (it) {
            reading.parent.possible_sets.resize(std::max(reading.parent.possible_sets.size(), it.second.size()));
            reading.parent.possible_sets |= it.second;
        }
        reading.tags.add(tag.hash);
        reading.tags_list.addLast(tag.hash);
        reading.tags_bloom.add(tag.hash);
        // ToDo: Remove for real ordered mode
        if (ordered) {
            if (!reading.tags_string.empty()) {
                reading.tags_string += ' ';
            }
            reading.tags_string += tag.tag;
            reading.tags_string_hash = hash_value(reading.tags_string);
        }
        if (grammar.parentheses.find(tag.hash) != -1) {
            reading.parent.is_pleft = tag.hash;
        }
        if (grammar.parentheses_reverse.get(tag.hash) != -1) {
            reading.parent.is_pright = tag.hash;
        }

        if (tag.type & T_MAPPING || tag.tag.charAt(0) == grammar.mapping_prefix) {
            tag.type |= T_MAPPING;
            if (reading.mapping != null && reading.mapping != tag) {
                System.err.print("Error: addTagToReading() cannot add a mapping tag to a reading which already is mapped!\n");
                System.exit(1);
            }
            reading.mapping = tag;
        }
        if ((tag.type & (T_TEXTUAL | T_WORDFORM | T_BASEFORM)) != 0) {
            reading.tags_textual.add(tag.hash);
            reading.tags_textual_bloom.add(tag.hash);
        }
        if (tag.type & T_NUMERICAL) {
            reading.tags_numerical[tag.hash] = tag;
            reading.parent.type &= ~CT_NUM_CURRENT;
        }
        if (!reading.baseform && (tag.type & T_BASEFORM)) {
            reading.baseform = tag.hash;
        }
        if (parse_dep && (tag.type & T_DEPENDENCY) && !(reading.parent.type & CT_DEP_DONE)) {
            reading.parent.dep_self = tag.dep_self;
            reading.parent.dep_parent = tag.dep_parent;
            if (tag.dep_parent == tag.dep_self) {
                reading.parent.dep_parent = DEP_NO_PARENT;
            }
            has_dep = true;
        }
        if (grammar.has_relations && (tag.type & T_RELATION)) {
            if (tag.dep_parent && tag.comparison_hash) {
                reading.parent.relations_input[tag.comparison_hash].insert(tag.dep_parent);
            }
            if ((tag.dep_self) != 0) {
                gWindow.relation_map.put(tag.dep_self, reading.parent.global_number);
            }
            has_relations = true;
            reading.parent.setRelated();
        }
        if (!(tag.type & T_SPECIAL)) {
            reading.tags_plain.insert(tag.hash);
            reading.tags_plain_bloom.insert(tag.hash);
        }
        if (rehash) {
            reading.rehash();
        }

        if (grammar.has_bag_of_tags) {
            Reading bot = reading.parent.parent.bag_of_tags;
            bot.tags.add(tag.hash);
            bot.tags_list.addLast(tag.hash);
            bot.tags_bloom.add(tag.hash);

            if (tag.type & (T_TEXTUAL | T_WORDFORM | T_BASEFORM)) {
                bot.tags_textual.add(tag.hash);
                bot.tags_textual_bloom.add(tag.hash);
            }
            if (tag.type & T_NUMERICAL) {
                bot.tags_numerical.put(tag.hash, tag);
            }
            if (!reading.baseform && (tag.type & T_BASEFORM)) {
                bot.baseform = tag.hash;
            }
            if (!(tag.type & T_SPECIAL)) {
                bot.tags_plain.add(tag.hash);
                bot.tags_plain_bloom.add(tag.hash);
            }
            if (rehash) {
                bot.rehash();
            }
        }

        return tag.hash;
    }

    private Tag generateVarstringTag(Tag tag) {
        /* static thread_local UnicodeString tmp;
        tmp.remove();
        tmp.append(tag.tag.data(), SI32(tag.tag.size())); */
        boolean did_something = false;

        // Convert %[UuLl] and $1-9 markers to control codes to avoid having combined %$1 accidentally match %L or have $s in data be filled
        String[] raw = { STR_VSu_raw, STR_VSU_raw, STR_VSl_raw, STR_VSL_raw, STR_VS1_raw, STR_VS2_raw, STR_VS3_raw, 
                STR_VS4_raw, STR_VS5_raw, STR_VS6_raw, STR_VS7_raw, STR_VS8_raw, STR_VS9_raw };
        String[] x01 = { STR_VSu, STR_VSU, STR_VSl, STR_VSL, STR_VS1, STR_VS2, STR_VS3, STR_VS4, STR_VS5, STR_VS6, 
                STR_VS7, STR_VS8, STR_VS9 };
        for (int i = 0; i < 13; ++i) {
            findAndReplace(tmp, raw[i], x01[i]);
        }

        // Replace unified sets with their matching tags
        if (tag.vs_sets != null) {
            for (int i = 0; i < tag.vs_sets.size(); ++i) {
                auto tags = ss_taglist.get();
                getTagList(*(*tag.vs_sets)[i], tags);
                static thread_local UString rpl;
                rpl.clear();
                // If there are multiple tags, such as from CompositeTags, put _ between them
                foreach (iter, *tags) {
                    rpl += (*iter).tag;
                    if (std::distance(iter, iter_end) > 1) {
                        rpl += '_';
                    }
                }
                if (findAndReplace(tmp, (*tag.vs_names)[i].data(), rpl.data())) {
                    did_something = true;
                }
            }
        }

        // Replace $1-$9 with their respective match groups
        String[] grp = { STR_VS1, STR_VS2, STR_VS3, STR_VS4, STR_VS5, STR_VS6, STR_VS7, STR_VS8, STR_VS9 };
        for (size_t i = 0; i < context_stack.back().regexgrp_ct && i < 9; ++i) {
            if (findAndReplace(tmp, grp[i].data(), USV((*context_stack.back().regexgrps)[i]))) {
                did_something = true;
            }
        }

        // Handle %U %u %L %l markers.
        boolean found;
        do {
            found = false;
            int pos = -1, mpos = -1;
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

        if (tag.type & T_CASE_INSENSITIVE) {
            tmp += 'i';
        }
        if (tag.type & T_REGEXP) {
            tmp += 'r';
        }

	const UChar* nt = tmp.getTerminatedBuffer();
        if (!did_something && nt == tag.tag) {
            u_fprintf(ux_stderr, "Warning: Unable to generate from tag '%S'! Possibly missing KEEPORDER and/or capturing regex from grammar on line %u before input line %u.\n", tag.tag.data(), grammar.lines, numLines);
            u_fflush(ux_stderr);
        }
        return addTag(nt, tag.type);
    }
}
