package org.apertium.grammar;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apertium.grammar.Reading.free_reading;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class Cohort implements Pool.Recyclable {
    public static final int DEP_NO_PARENT = Integer.MAX_VALUE;
    static Pool<Cohort> pool_cohorts;

    public int type = 0;
    // ToDo: Get rid of global_number in favour of Cohort* relations
    public int global_number = 0;
    public int local_number = 0;
    public int enclosed = 0;
    public Tag wordform = null;
    public int dep_self = 0;
    public long dep_parent = DEP_NO_PARENT;
    public int is_pleft = 0;
    public int is_pright = 0;
    public SingleWindow parent;
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
    public BitSet possible_sets;
    public int relations;
    public int relations_input;
    public int line_number = 0;

    static int CT_ENCLOSED    = 1;
    static int CT_RELATED     = (1 <<  1);
    static int CT_REMOVED     = (1 <<  2);
    static int CT_NUM_CURRENT = (1 <<  3);
    static int CT_DEP_DONE    = (1 <<  4);
    static int CT_AP_UNKNOWN  = (1 <<  5);
    static int CT_IGNORED     = (1 <<  6);

    public Cohort(SingleWindow p) {
        this.parent = p;
    }

    public void setRelated() {
        type |= CT_RELATED;
        for (Reading r : readings) {
            r.noprint = false;
        }
    }

    public void appendReading(Reading read, List<Reading> readings) {
        readings.addLast(read);
        if (read.number == 0) {
            read.number = readings.size() * 1000 + 1000;
        }
        type &= ~CT_NUM_CURRENT;
    }

    public void appendReading(Reading read) {
        appendReading(read, readings);
    }

    public void unignoreAll() {
        if (!ignored.isEmpty()) {
            for (Reading r : ignored) {
                r.deleted = false;
                readings.add(r);
            }
            ignored.clear();
        }
    }

    static Cohort alloc_cohort(SingleWindow p) {
        Cohort c = pool_cohorts.get();
        if (c == null) {
            c = new Cohort(p);
        }
        else {
            c.parent = p;
        }
        return c;
    }

    @Override
    public void clear() {
        if (parent != null && parent.parent != null) {
            parent.parent.cohort_map.remove(global_number);
            parent.parent.dep_window.remove(global_number);
        }
        detach();

        type = 0;
        global_number = 0;
        local_number = 0;
        enclosed = 0;
        wordform = null;
        dep_self = 0;
        dep_parent = DEP_NO_PARENT;
        is_pleft = 0;
        is_pright = 0;
        parent = null;
        line_number = 0;

        text = EMPTY;
        wblank = EMPTY;
        num_max.clear();
        num_min.clear();
        dep_children.clear();
        possible_sets = null;
        relations = 0;
        relations_input = 0;

        for (Reading iter : readings) {
            free_reading(iter);
        }
        for (Reading iter : deleted) {
            free_reading(iter);
        }
        for (Reading iter : delayed) {
            free_reading(iter);
        }
        for (Reading iter : ignored) {
            free_reading(iter);
        }
        free_reading(wread);

        readings.clear();
        deleted.clear();
        delayed.clear();
        wread = null;
    }

    private void detach() {
        if (prev != null) {
            prev.next = next;
        }
        if (next != null) {
            next.prev = prev;
        }
        prev = next = null;
    }

    static void free_cohort(Cohort c) {
        if (c == null) {
            return;
        }
        pool_cohorts.put(c);
        c = null;
    }
}
