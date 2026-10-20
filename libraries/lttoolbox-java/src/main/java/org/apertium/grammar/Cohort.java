package org.apertium.grammar;

import java.util.List;
import java.util.Map;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class Cohort {
    public static final int DEP_NO_PARENT = 0;
    int type = 0;
    // ToDo: Get rid of global_number in favour of Cohort* relations
    int global_number = 0;
    int local_number = 0;
    int enclosed = 0;
    Tag wordform = null;
    int dep_self = 0;
    int dep_parent = DEP_NO_PARENT;
    int is_pleft = 0;
    int is_pright = 0;
    SingleWindow parent = null;
    String text;
    String wblank;
    Cohort prev = null;
    Cohort next = null;
    Reading wread = null;
    List<Reading> readings;
    List<Reading>  deleted;
    List<Reading>  delayed;
    List<Reading>  ignored;
    Map<Integer, Double> num_max, num_min;
    List<Integer> dep_children;
    byte[] possible_sets;
    int relations;
    int relations_input;
    int line_number = 0;

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
