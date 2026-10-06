package org.apertium.recursive;

import org.apertium.lttoolbox.Alphabet;
import org.apertium.lttoolbox.Pair;
import org.apertium.lttoolbox.collections.IntSet;
import org.apertium.lttoolbox.collections.Transducer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MatchExe2 {

    public final int RTXStateSize = 128;
    int RTXStackSize = 4096;

    List<MatchNode2> nodes = new ArrayList<>();
    int any_char;
    int any_tag;
    int lookahead;
    Alphabet alpha;
    int initial;
    int[] rejected = new int[RTXStackSize];
    int rejectedCount;
    int[] prematch = new int[RTXStackSize];
    int[] prematchAlt = new int[RTXStackSize];
    int prematchIdx;

    int rule_count;
    int[] rule_states;
    int[] rule_numbers;
    double[] rule_weights;
    int[] rule_lengths;

    /* ALPHA */
    public MatchExe2(Transducer t, Alphabet alphabet, Map<Integer, Pair<Integer, Double>> finals, List<Integer> patSize) {
        ArrayList<Map<Integer, IntSet>> trns = t.transitions;
        for(int outerKey = 0; outerKey < trns.size(); outerKey++) {
            Map<Integer, IntSet> innerMap = trns.get(outerKey);
            if(innerMap == null) {
                continue;
            }
            int totalTransitions = 0;
            for(IntSet set: innerMap.values()){
                if(set != null){
                    totalTransitions += set.size();
                }
            }
            MatchNode2 matchnode = new MatchNode2();
            matchnode.setSize(totalTransitions);
            nodes.set(outerKey, matchnode);

            int i = 0;
            for (Map.Entry<Integer, IntSet> entry : innerMap.entrySet()) {
                int innerKey = entry.getKey();
                IntSet intSet = entry.getValue();

                if (intSet == null) continue;

                // 3. Iteramos sobre los enteros guardados en el IntSet
                // Nota: Si usas librerías como FastUtil, puedes usar 'for (int target : intSet)'
                for (int target : intSet) {

                    // En C++: nodes[it->first].addTransition(it2->first, it2->second.first, i++);
                    // it->first  -> Se convierte en 'outerKey' (el índice del ArrayList)
                    // it2->first -> Se convierte en 'innerKey' (la llave del mapa interno)
                    // it2->second.first -> Se convierte en 'target' (el entero dentro del Set)
                    nodes.get(outerKey).addTransition(innerKey, target, i++);
                }
            }

            rule_count = finals.size();
            rule_states = new int[rule_count];
            rule_numbers = new int[rule_count];
            rule_weights = new double[rule_count];
            rule_lengths = new int[rule_count];
            i=0;
            for(int j=0; j < finals.size(); j++){
                if(nodes.get(j).rule_begin == -1){
                    nodes.get(j).rule_begin = i;
                }
                nodes.get(j).rule_end = i;
                rule_states[i] = j;
                rule_numbers[i] = finals.get(j).first;
                rule_weights[i] = finals.get(j).second;
                rule_lengths[i] = patSize.get(finals.get(j).first);
                i++;
            }

            initial = t.getInitial();
            any_char = alphabet.cast("<ANY_CHAR>");
            any_tag = alphabet.cast("<ANY_TAG>");
            lookahead = alphabet.cast("<LOOK:AHEAD>");

            prematchIdx = 0;


        }
    }

    /* COMPLETED */
    public void prepareChunk(String chunk) {
        prematchIdx = 0;
        for(int i = 0, limit = chunk.length(); i < limit; i++)
        {
            switch(chunk.charAt(i))
            {
                case '\\':
                    prematchAlt[prematchIdx] = any_char;
                    prematch[prematchIdx++] = chunk.charAt(++i);
                    break;
                case '<':
                    for(int j = i+1; j < chunk.length(); j++)
                    {
                        if(chunk.charAt(j) == '>')
                        {
                            int symbol = alpha.cast(chunk.substring(i, j-i+1));
                            prematchAlt[prematchIdx] = any_tag;
                            prematch[prematchIdx++] = (symbol!=0 ? symbol : any_tag);
                            i = j;
                            break;
                        }
                    }
                    break;
                default:
                    prematchAlt[prematchIdx] = any_char;
                    prematch[prematchIdx++] = chunk.charAt(i);
                    break;
            }
        }
    }

    public void resetRejected() {

    }
}
