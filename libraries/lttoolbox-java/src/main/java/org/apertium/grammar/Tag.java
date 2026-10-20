package org.apertium.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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
    List<Set<Integer>> vs_sets = new ArrayList<>();
    List<String> vs_names = new ArrayList<>();
    Pattern regexp = null;

    void parseTagRaw(char to, Grammar grammar) {

    }

    int rehash() {
        return 0;
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
