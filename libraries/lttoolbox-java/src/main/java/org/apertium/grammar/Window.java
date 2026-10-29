package org.apertium.grammar;

import java.util.HashMap;
import java.util.Map;

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

    Map<Integer, Cohort> cohort_map = new HashMap<>();

    /* - Eclipse Collections (IntObjectHashMap u IntIntHashMap): Implementa direccionamiento abierto (open addressing) con celdas contiguas en un arreglo nativo. Es lo más cercano al concepto "flat".
       - Fastutil (Int2ObjectOpenHashMap o Int2IntOpenHashMap): Altamente optimizado para rendimiento en Java, usa direccionamiento abierto y arrays nativos como contenedores planos.
       - Si no deseas dependencias externas y prefieres usar el SDK nativo, la única alternativa es: HashMap<Long, V>
     */
    public int dep_map;
    public Map<Integer, Cohort> dep_window = new HashMap<>();
    public Map<Integer, Integer> relation_map = new HashMap<>();

    public int previous;
    public SingleWindow current = null;
    public int next;

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
        this.parent = grammarApplicator;
    }
}
