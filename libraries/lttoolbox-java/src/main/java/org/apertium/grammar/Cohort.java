package org.apertium.grammar;

import java.util.List;
import java.util.Map;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class Cohort {
    public static final int DEP_NO_PARENT = Integer.MAX_VALUE;
    public int type = 0;
    // ToDo: Get rid of global_number in favour of Cohort* relations
    public int global_number = 0;
    public int local_number = 0;
    public int enclosed = 0;
    public Tag wordform = null;
    public int dep_self = 0;
    public int dep_parent = DEP_NO_PARENT;
    public int is_pleft = 0;
    public int is_pright = 0;
    public SingleWindow parent = null;
    public String text;
    public String wblank;
    public Cohort prev = null;
    public Cohort next = null;
    public Reading wread = null;
    public List<Reading> readings;
    public List<Reading>  deleted;
    public List<Reading>  delayed;
    public List<Reading>  ignored;
    public Map<Integer, Double> num_max, num_min;
    public List<Integer> dep_children;
    public byte[] possible_sets;
    public int relations;
    public int relations_input;
    public int line_number = 0;

    int CT_ENCLOSED    = 1;
    int CT_RELATED     = (1 <<  1);
    int CT_REMOVED     = (1 <<  2);
    int CT_NUM_CURRENT = (1 <<  3);
    int CT_DEP_DONE    = (1 <<  4);
    int CT_AP_UNKNOWN  = (1 <<  5);
    int CT_IGNORED     = (1 <<  6);

    public void setRelated() {
        type |= CT_RELATED;
        for (Reading r : readings) {
            r.noprint = false;
        }
    }
}
