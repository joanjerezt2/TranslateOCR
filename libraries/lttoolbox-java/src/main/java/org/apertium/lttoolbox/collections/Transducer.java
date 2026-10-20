package org.apertium.lttoolbox.collections;

import org.apertium.lttoolbox.Compression;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Transducer {
    /**
     * Initial state
     */
    protected Integer initial;
    /**
     * tag for epsilon transitions
     */
    protected Integer epsilon_tag = 0;
    /**
     * Final state set
     */
    protected AbundantIntSet finals = new AbundantIntSet();
    /**
     * Transitions of the transducer.
     * Before we used a Map here, but as the keys are 1, 2, 3, 4, 5, ...up to the number of states its been changed to an ArrayList
     */
    public ArrayList<Map<Integer, IntSet>> transitions = new ArrayList<>();

    public static boolean DEBUG = false;

    static long TDF_WEIGHTS = 1L;
    static long TDF_UNKNOWN = (1L << 1);
    static long TDF_RESERVED = (1L << 63);

    static double default_weight = 0;

    /**
     * Returns the initial state of a transducer
     *
     * @return the initial state identifier
     */
    public Integer getInitial() {
        return initial;
    }

    /**
     * New state creator
     *
     * @return the new state number
     */
    protected Integer newState() {
        Integer nstate = transitions.size();
        transitions.add(new HashMap<>());
        return nstate;
    }

    /**
     * Constructor
     */
    public Transducer() {
        initial = newState();
    }

    /**
     * Read method
     * @param input the stream to read from
     * @param decalage offset to sum to the tags
     */
    public static void read(Transducer t, InputStream input, int decalage) throws IOException {
        t.transitions.clear();
        boolean read_weights = false;

        input.mark(4);
        byte[] header = new byte[4];
        int r = input.read(header);
        if (Arrays.equals(header, Compression.HEADER_TRANSDUCER)) {
            long features = new DataInputStream(input).readLong();
            if (DEBUG) System.err.println("[DEBUG] Transducer.read() got features " + features);
            if(features==1){ System.out.println("[INFO] FST has features that are unknown to the old version of lttoolbox!");}
            if (features>=TDF_UNKNOWN) throw new IOException("[ERROR] FST has features that are unknown to this version of lttoolbox - upgrade!");
        } else {
            // Old binary format
            input.reset();
            if (DEBUG) System.err.println("Transducer.read() got Old binary format");
        }

        //reading the initial state
        t.initial = Compression.multibyte_read(input);
        int finals_size = Compression.multibyte_read(input);

        //reading the final states
        int base = 0;
        double base_weight = default_weight;

        while(finals_size > 0)
        {
            finals_size--;
            base += Compression.multibyte_read(input);
            t.finals.add(base);
        }

        //reading the transitions
        int number_of_states = Compression.multibyte_read(input);
        base = number_of_states;
        int current_state = 0;

        while(number_of_states > 0)
        {
            int number_of_local_transitions = Compression.multibyte_read(input);
            int tagbase = 0;
            Map<Integer, IntSet> place = t.getCreatePlace(current_state);
            while(number_of_local_transitions > 0)
            {
                number_of_local_transitions--;
                tagbase += Compression.multibyte_read(input) - decalage;
                int state = (current_state + Compression.multibyte_read(input)) % base;
                t.addTransition(place, tagbase, state);
            }
            number_of_states--;
            current_state++;
        }
    }

    /**
     * Add a transduction to the transducer
     *
     * @param place the source state
     * @param symbol the tag
     * @param state the target state
     */
    void addTransition(Map<Integer, IntSet> place, int symbol, int state) {

        ensureCreatedPlace(state);
        IntSet set = new SlowIntegerTreeSet();
        place.put(symbol, set);
        set.add(state);
    }

    private void ensureCreatedPlace(int state) {
        if (transitions.size() <= state) {
            transitions.add(new TreeMap<>());
        }
    }

    Map<Integer, IntSet> getCreatePlace(int state) {
        Map<Integer, IntSet> place;
        if (transitions.size() <= state) {
            place = new TreeMap<>();
            transitions.add(place);
        } else {
            place = transitions.get(state);
        }

        return place;
    }

    public static void read(Transducer t, InputStream input) throws IOException {
        read(t, input, 0);
    }

}
