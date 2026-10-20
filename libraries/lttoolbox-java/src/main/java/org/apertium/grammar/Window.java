package org.apertium.grammar;

import java.util.Map;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class Window{
    GrammarApplicator parent = null;
    int cohort_counter = 0;
    int window_counter = 0;
    int window_span = 0;

    Map<Integer, Cohort> cohort_map;
    int dep_map;
    Map<Integer, Cohort> dep_window;
    Map<Integer, Integer> relation_map;

    int previous;
    SingleWindow current = null;
    int next;

    SingleWindow allocSingleWindow() {
        return null;
    }

    SingleWindow allocPushSingleWindow() {
        return null;
    }

    SingleWindow allocAppendSingleWindow() {
        return null;
    }

    SingleWindow back() {
        return null;
    }

    void shuffleWindowsDown() {

    }

    void rebuildSingleWindowLinks() {

    }

    void rebuildCohortLinks() {

    }

    public Window(GrammarApplicator grammarApplicator) {

    }
}
