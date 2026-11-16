package org.apertium.grammar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apertium.grammar.SingleWindow.*;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class Window{
    // Declaración del atributo (en Java los punteros son referencias por defecto)
    public GrammarApplicator parent;
    public int cohort_counter = 0;
    public int window_counter = 0;
    public int window_span = 0;
    public boolean flush_after;

    Map<Integer, Cohort> cohort_map = new HashMap<>();

    /* - Eclipse Collections (IntObjectHashMap u IntIntHashMap): Implementa direccionamiento abierto (open addressing) con celdas contiguas en un arreglo nativo. Es lo más cercano al concepto "flat".
       - Fastutil (Int2ObjectOpenHashMap o Int2IntOpenHashMap): Altamente optimizado para rendimiento en Java, usa direccionamiento abierto y arrays nativos como contenedores planos.
       - Si no deseas dependencias externas y prefieres usar el SDK nativo, la única alternativa es: HashMap<Long, V>
     */
    public Map<Integer, Integer> dep_map = new HashMap<>();
    public Map<Integer, Cohort> dep_window = new HashMap<>();
    public Map<Integer, Integer> relation_map = new HashMap<>();

    public List<SingleWindow> previous;
    public SingleWindow current = null;
    public List<SingleWindow> next;

    SingleWindow allocSingleWindow() {
        SingleWindow swindow = alloc_swindow(this);
        ++window_counter;
        swindow.number = window_counter;
        return swindow;

    }

    SingleWindow allocPushSingleWindow() {
        SingleWindow swindow = alloc_swindow(this);
        ++window_counter;
        swindow.number = window_counter;
        if (!next.isEmpty()) {
            swindow.next = next.getFirst();
            next.getFirst().previous = swindow;
        }
        if (current != null) {
            swindow.previous = current;
            current.next = swindow;
        }
        next.addFirst(swindow);
        return swindow;
    }

    SingleWindow allocAppendSingleWindow() {
        SingleWindow swindow = alloc_swindow(this);
        ++window_counter;
        swindow.number = window_counter;
        if (!next.isEmpty()) {
            swindow.previous = next.getLast();
            next.getLast().next = swindow;
        }
        next.addLast(swindow);
        return swindow;
    }

    SingleWindow back() {
        if (!next.isEmpty()) {
            return next.getLast();
        }
        else if (current != null) {
            return current;
        }
        else if (!previous.isEmpty()) {
            return previous.getLast();
        }
        return null;
    }

    void shuffleWindowsDown() {
        if (current != null) {
            current.variables_set = parent.variables;
            current.variables_rem.clear();
            previous.addLast(current);
            current = null;
        }

        if (!next.isEmpty()) {
            current = next.getFirst();
            next.remove(next.getFirst());
        }
    }

    void rebuildSingleWindowLinks() {
        SingleWindow sWindow = null;

        for (SingleWindow iter : previous) {
            iter.previous = sWindow;
            if (sWindow != null) {
                sWindow.next = iter;
            }
            sWindow = iter;
        }

        if (current != null) {
            current.previous = sWindow;
            if (sWindow != null) {
                sWindow.next = current;
            }
            sWindow = current;
        }

        for (SingleWindow iter : next) {
            iter.previous = sWindow;
            if (sWindow != null) {
                sWindow.next = iter;
            }
            sWindow = iter;
        }

        if (sWindow != null) {
            sWindow.next = null;
        }
    }

    void rebuildCohortLinks() {
        SingleWindow sWindow = null;
        if (!previous.isEmpty()) {
            sWindow = previous.getFirst();
        }
        else if (current != null) {
            sWindow = current;
        }
        else if (!next.isEmpty()) {
            sWindow = next.getFirst();
        }

        Cohort prev = null;
        while (sWindow != null) {
            for (Cohort citer : sWindow.cohorts) {
                citer.prev = prev;
                citer.next = null;
                if (prev != null) {
                    prev.next = citer;
                }
                prev = citer;
            }
            sWindow = sWindow.next;
        }
    }

    public Window(GrammarApplicator grammarApplicator) {
        this.parent = grammarApplicator;
    }
}
