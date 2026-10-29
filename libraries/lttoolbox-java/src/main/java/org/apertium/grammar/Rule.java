package org.apertium.grammar;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class Rule {
    
    static class RULE_FLAGS{
        int RF_NEAREST      = 1;
        int RF_ALLOWLOOP    = (1 <<  1);
        int RF_DELAYED      = (1 <<  2);
        int RF_IMMEDIATE    = (1 <<  3);
        int RF_LOOKDELETED  = (1 <<  4);
        int RF_LOOKDELAYED  = (1 <<  5);
        int RF_UNSAFE       = (1 <<  6);
        int RF_SAFE         = (1 <<  7);
        int RF_REMEMBERX    = (1 <<  8);
        int RF_RESETX       = (1 <<  9);
        int RF_KEEPORDER    = (1 << 10);
        int RF_VARYORDER    = (1 << 11);
        int RF_ENCL_INNER   = (1 << 12);
        int RF_ENCL_OUTER   = (1 << 13);
        int RF_ENCL_FINAL   = (1 << 14);
        int RF_ENCL_ANY     = (1 << 15);
        int RF_ALLOWCROSS   = (1 << 16);
        int RF_WITHCHILD    = (1 << 17);
        int RF_NOCHILD      = (1 << 18);
        int RF_ITERATE      = (1 << 19);
        int RF_NOITERATE    = (1 << 20);
        int RF_UNMAPLAST    = (1 << 21);
        int RF_REVERSE      = (1 << 22);
        int RF_SUB          = (1 << 23);
        int RF_OUTPUT       = (1 << 24);
        int RF_CAPTURE_UNIF = (1 << 25);
        int RF_REPEAT       = (1 << 26);
        int RF_BEFORE       = (1 << 27);
        int RF_AFTER        = (1 << 28);
        int RF_IGNORED      = (1 << 29);
        int RF_LOOKIGNORED  = (1 << 30);
        long RF_NOMAPPED     = (1L << 31);
        long RF_NOPARENT     = (1L << 32);
        long RF_DETACH       = (1L << 33);
    }

    public String name;
    public Tag wordform = null;
    public int number;
    public Object type;
    public int target;
    public boolean childset1;
    public List<ContextualTest> tests = new LinkedList<>();
    public ContextualTest dep_target;
    public boolean sublist;
    public boolean maplist;
    public boolean childset2;
    public List<ContextualTest> dep_tests = new LinkedList<>();;
    public Object flags;
    public int section;
    public Integer line;
}
