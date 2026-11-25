package org.apertium.grammar;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apertium.grammar.Cohort.free_cohort;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class SingleWindow implements Pool.Recyclable {
    public int number = 0;
    public boolean has_enclosures = false;
    public boolean flush_after = false;
    public SingleWindow next = null, previous = null;
    public Window parent;
    public String text;
    public String text_post;
    public List<Cohort> all_cohorts;
    public List<Cohort> cohorts;
    public List<Integer> valid_rules;
    SortedSet<Integer> hit_external;
    List<Set<Cohort>> rule_to_cohorts;
    // Used by GrammarApplicator::runSingleRule so that it doesn't need to allocate a new one or edit a rule's actual list when applying a subrule of WITH
    List<Set<Cohort>> nested_rule_to_cohorts;
    Map<Integer, Integer> variables_set = new HashMap<>();
    public Set<Integer> variables_rem;
    public Set<Integer> variables_output;
    public Reading bag_of_tags;

    static Pool<SingleWindow> pool_swindows;

    public SingleWindow(Window p) {
        this.parent = p;
    }

    public void appendCohort(Cohort cohort) {
        cohort.local_number = cohorts.size();
        cohort.parent = this;

        if (cohort.dep_self != 0) {
            parent.parent.dep_highest_seen = cohort.dep_self;
        }

        if (cohorts.isEmpty()) {
            if (previous != null && !previous.cohorts.isEmpty()) {
                previous.cohorts.getFirst().next = cohort;
                cohort.prev = previous.cohorts.getFirst();
            }
        }
        else {
            cohort.prev = cohorts.getFirst();
            cohorts.getFirst().next = cohort;
        }
        if (next != null && !next.cohorts.isEmpty()) {
            next.cohorts.getFirst().prev = cohort;
            cohort.next = next.cohorts.getFirst();
        }
        cohorts.addLast(cohort);
        all_cohorts.addLast(cohort);
        parent.cohort_map.put(cohort.global_number, cohort);
        parent.dep_window.put(cohort.global_number, cohort);
        if (cohort.local_number == 0) {
            parent.cohort_map.put(0, cohort);
        }
    }

    static void free_swindow(SingleWindow tmp) {
        if (tmp == null) {
            return;
        }
        pool_swindows.put(tmp);
        tmp.clear();
    }

    static SingleWindow alloc_swindow(Window p) {
        SingleWindow s = pool_swindows.get();
        if (s == null) {
            s = new SingleWindow(p);
        }
        else {
            s.parent = p;
        }
        return s;
    }

    @Override
    public void clear() {
        if (cohorts.size() > 1) {
            for (Map.Entry<Integer, Integer> iter : parent.relation_map.entrySet()) {
                if (iter.getValue() <= cohorts.getLast().global_number) {
                    parent.relation_map.remove(iter.getKey(), iter.getValue());
                }
            }
        }

        for (Cohort iter : all_cohorts) {
            free_cohort(iter);
        }

        if (next != null && previous != null) {
            next.previous = previous;
            previous.next = next;
        }
        else {
            if (next != null) {
                next.previous = null;
            }
            if (previous != null) {
                previous.next = null;
            }
        }

        number = 0;
        has_enclosures = false;
        flush_after = false;
        next = null;
        previous = null;
        parent = null;
        text = EMPTY;
        text_post = EMPTY;
        cohorts.clear();
        all_cohorts.clear();
        valid_rules.clear();
        hit_external.clear();

        for (Set<Cohort> cs : rule_to_cohorts) {
            cs.clear();
        }
        variables_set.clear();
        variables_rem.clear();
        variables_output.clear();
        bag_of_tags.clear();
    }

}
