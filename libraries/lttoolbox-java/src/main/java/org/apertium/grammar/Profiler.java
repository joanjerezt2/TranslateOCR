package org.apertium.grammar;

import org.apertium.lttoolbox.Pair;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class Profiler {

    static int ET_RULE = 0;
    static int ET_CONTEXT = 1;

    Map<String, Integer> strings = new TreeMap<>();
    Map<Integer, Integer> grammars = new HashMap<>();
    int grammar_ast = 0;
    StringBuilder buf;

    static class Key {
        int type = ET_RULE;
        int id = 0;

        public Key(int etRule, int n) {
            type = etRule;
            id = n;
        }

        boolean operator (Key o) {
            if (type == o.type) {
                return id < o.id;
            }
            return type < o.type;
        }
    }

    class Entry {
        int type = ET_RULE;
        int grammar = 0;
        int b = 0;
        int e = 0;
        int num_match = 0;
        int num_fail = 0;
        int example_window = 0;

        public Entry(int etRule, int g, int b2, int e2) {
            type = etRule;
            grammar = g;
            b = b2;
            e = e2;
        }
    }

    Map<Key, Entry> entries = new HashMap<>();
    Map<Pair<Integer, Integer>, Integer> rule_contexts;

    int addString(String str) {
        boolean it = strings.containsKey(str);
        if (it) {
            return strings.get(str);
        }
        int sz = strings.size() + 1;
        strings.put(str, sz);
        return sz;
    }

    int addGrammar(String fname, String grammar) {
        int f = addString(fname);
        int g = addString(grammar);
        grammars.put(f,g);
        return g;
    }

    void addRule(int n, int g, int b, int e) {
        Key k = new Key(ET_RULE, n);
        entries.put(k, new Entry(ET_RULE, g, b, e));
    }

    void addContext(int c, int g, int b, int e) {
        Key k = new Key(ET_CONTEXT, c);
        if (!entries.containsKey(k)) {
            entries.put(k, new Entry(ET_CONTEXT, g, b, e));
        }
    }

    void write(char[] fname){

    }

    void read(char[] fname){

    }

}
