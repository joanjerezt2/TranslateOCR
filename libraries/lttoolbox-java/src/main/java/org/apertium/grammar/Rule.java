package org.apertium.grammar;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class Rule {
    public String name;
    public int number;
    public boolean wordform;
    public Object type;
    public int target;
    public boolean childset1;
    public int[] tests;
    public boolean dep_target;
    public boolean sublist;
    public boolean maplist;
    public boolean childset2;
    public int[] dep_tests;
    public Object flags;
    public int section;
    public Integer line;
}
