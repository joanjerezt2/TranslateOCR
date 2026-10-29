package org.apertium.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.apertium.grammar.Inlines.hash_value;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class Tag {

    public boolean s_sets;

    void parseNumeric() {

    }

    enum C_OPS {
        OP_NOP,
        OP_EQUALS,
        OP_LESSTHAN,
        OP_GREATERTHAN,
        OP_LESSEQUALS,
        OP_GREATEREQUALS,
        OP_NOTEQUALS,
        NUM_OPS,
    };
    
    static int T_ANY              = 1;
    static int T_NUMERICAL        = (1 <<  1);
    static int T_MAPPING          = (1 <<  2);
    static int T_VARIABLE         = (1 <<  3);
    static int T_META             = (1 <<  4);
    static int T_WORDFORM         = (1 <<  5);
    static int T_BASEFORM         = (1 <<  6);
    static int T_TEXTUAL          = (1 <<  7);
    static int T_DEPENDENCY       = (1 <<  8);
    static int T_SAME_BASIC       = (1 <<  9);
    static int T_FAILFAST         = (1 << 10);
    static int T_CASE_INSENSITIVE = (1 << 11);
    static int T_REGEXP           = (1 << 12);
    static int T_PAR_LEFT         = (1 << 13);
    static int T_PAR_RIGHT        = (1 << 14);
    static int T_REGEXP_ANY       = (1 << 15);
    static int T_VARSTRING        = (1 << 16);
    static int T_TARGET           = (1 << 17);
    static int T_MARK             = (1 << 18);
    static int T_ATTACHTO         = (1 << 19);
    static int T_SPECIAL          = (1 << 20);
    static int T_USED             = (1 << 21);
    static int T_LOCAL_VARIABLE   = (1 << 22);
    static int T_SET              = (1 << 23);
    static int T_VSTR             = (1 << 24);
    static int T_ENCL             = (1 << 25);
    static int T_RELATION         = (1 << 26);
    static int T_CONTEXT          = (1 << 27);
    static int T_NUMERIC_MATH     = (1 << 28);
    static int T_PRESERVE_ESC     = (1 << 29);
    static int T_REGEXP_LINE      = (1 << 31); // ToDo: Remove for real ordered mode
    static int MASK_TAG_SPECIAL   = T_ANY | T_TARGET | T_MARK | T_ATTACHTO | T_PAR_LEFT | T_PAR_RIGHT | T_NUMERICAL |
            T_VARIABLE | T_LOCAL_VARIABLE | T_META | T_FAILFAST | T_CASE_INSENSITIVE | T_REGEXP | T_REGEXP_ANY |
            T_VARSTRING | T_SET | T_ENCL | T_SAME_BASIC | T_CONTEXT;

    C_OPS comparison_op = C_OPS.OP_NOP;
    double comparison_val = 0;
    int type = 0;
    int comparison_hash = 0;
    int dep_self = 0;

    int dep_parent = 0;
    int variable_hash = 0;
    int context_ref_pos = 0;
    int comparison_offset = 0;

    int hash = 0;
    int plain_hash = 0;
    int number = 0;
    int seed = 0;
    String tag = "";
    String tag_raw = "";
    List<SetGrammar> vs_sets;
    List<String> vs_names = new ArrayList<>();
    Pattern regexp = null;

    void parseTagRaw(char[] to, Grammar grammar) {
        type = 0;
	    int length = to.length;
        if(length == 0) {
            System.out.print("parseTagRaw() will not work with empty strings.");
        }

        if (to[0] == '"' || to[0] == '<') {
            if ((to[0] == '"' && to[length - 1] == '"') || (to[0] == '<' && to[length - 1] == '>')) {
                type |= T_TEXTUAL;
                if (to[0] == '"' && to[length - 1] == '"') {
                    if (to[1] == '<' && to[length - 2] == '>' && length > 4) {
                        type |= T_WORDFORM;
                    }
                    else {
                        type |= T_BASEFORM;
                    }
                }
            }
        }

        tag = new String(to);

        /* for (Pattern iter : grammar.regex_tags) {
            UErrorCode status = U_ZERO_ERROR;
            uregex_setText(iter, tag.data(), SI32(tag.size()), &status);
            if (status == U_ZERO_ERROR) {
                if (uregex_find(iter, -1, &status)) {
                    type |= T_TEXTUAL;
                }
            }
        }
        for (auto iter : grammar->icase_tags) {
            if (ux_strCaseCompare(tag, iter.tag)) {
                type |= T_TEXTUAL;
            }
        }

        if (tag.charAt(0) == '<' && tag[length - 1] == '>') {
            parseNumeric(false);
        }
        if (tag[0] == '#') {
            if (u_sscanf(tag.data(), "#%i->%i", &dep_self, &dep_parent) == 2 && dep_self != 0) {
                type |= T_DEPENDENCY;
            }
            constexpr UChar local_dep_unicode[] = { '#', '%', 'i', u'\u2192', '%', 'i', 0 };
            if (u_sscanf_u(tag.data(), local_dep_unicode, &dep_self, &dep_parent) == 2 && dep_self != 0) {
                type |= T_DEPENDENCY;
            }
        }
        if (tag[0] == 'I' && tag[1] == 'D' && tag[2] == ':' && u_isdigit(tag[3])) {
            if (u_sscanf(tag.data(), "ID:%i", &dep_self) == 1 && dep_self != 0) {
                type |= T_RELATION;
            }
        }
        if (tag.charAt(0) == 'R' && tag.charAt(1) == ':') {
            char[] relname = new char[256];
            dep_parent = Integer.MAX_VALUE;
            if (u_sscanf(tag.data(), "R:%[^:]:%i", &relname, &dep_parent) == 2 && dep_parent != std::numeric_limits<uint32_t>::max()) {
                type |= T_RELATION;
                Tag reltag = grammar.allocateTag(relname);
                comparison_hash = reltag.hash;
            }
        }

        type &= ~T_SPECIAL;
        if (type & (T_NUMERICAL)) {
            type |= T_SPECIAL;
        }*/
    }

    int rehash() {
        hash = 0;
        plain_hash = 0;

        if (type == T_FAILFAST) {
            hash = hash_value("^", hash);
        }

        if (type == T_META) {
            hash = hash_value("META:", hash);
        }
        if (type == T_VARIABLE) {
            hash = hash_value("VAR:", hash);
        }
        if (type == T_LOCAL_VARIABLE) {
            hash = hash_value("LVAR:", hash);
        }
        if (type == T_SET) {
            hash = hash_value("SET:", hash);
        }

        plain_hash = hash_value(tag);
        if (hash != 0) {
            hash = hash_value(String.valueOf(plain_hash), hash);
        }
        else {
            hash = plain_hash;
        }

        if (type == T_CASE_INSENSITIVE) {
            hash = hash_value("i", hash);
        }
        if (type == T_REGEXP) {
            hash = hash_value("r", hash);
        }
        if (type == T_VARSTRING) {
            hash = hash_value("v", hash);
        }

        hash += seed;

        type &= ~T_SPECIAL;
        if (type == MASK_TAG_SPECIAL) {
            type |= T_SPECIAL;
        }

        /* if (dump_hashes_out) {
            System.out.printf("DEBUG: Hash %d with seed %d for tag %S\n", hash, seed, tag);
            System.out.printf("DEBUG: Plain hash %d with seed %d for tag %S\n", plain_hash, seed, tag);
        }*/

        return hash;
    }

    void markUsed() {

    }

    void allocateVsSets() {

    }

    void allocateVsNames() {

    }

    void parseNumeric(boolean trusted) {

    }
}
