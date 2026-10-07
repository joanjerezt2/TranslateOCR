package org.apertium.recursive;

import org.apertium.lttoolbox.Alphabet;
import org.apertium.lttoolbox.Pair;
import org.apertium.lttoolbox.collections.IntSet;
import org.apertium.lttoolbox.collections.Transducer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Joan Jerez, ToBeIT
 * @serial GPL 3.0
 */

public class MatchExe2 {

    public final int RTXStateSize = 128;
    public final int RTXStackSize = 4096;

    public static class StateRange {
        int first;
        int last;

        StateRange(int first, int last) {
            this.first = first;
            this.last = last;
        }
    }

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

    private void applySymbol(int srcNode, int symbol, int[] state, StateRange range)
    {
        int res = nodes.get(srcNode).search(symbol);
        if(res != -1)
        {
            state[range.last] = res;
            range.last = (range.last+1) % RTXStateSize;
        }
    }

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

    int getInitial()
    {
        return initial;
    }

    void step(int[] state, StateRange range, int symbol)
    {
        int first = range.first;
        int loclast = range.last;
        for(int i = first; i != loclast; i = (i+1)%RTXStateSize)
        {
            applySymbol(state[i], symbol, state, range);
        }
        first = loclast;
    }

    void step(int[] state, StateRange range, int symbol, int alt)
    {
        int first = range.first;
        int loclast = range.last;
        for(int i = first; i != loclast; i = (i+1)%RTXStateSize)
        {
            applySymbol(state[i], symbol, state, range);
            applySymbol(state[i], alt, state, range);
        }
        first = loclast;
    }

    void matchBlank(int[] state, int first, int last)
    {
        StateRange range = new StateRange(first, last);
        step(state, range, ' ');
    }

    void matchChunk(int[] state, int first, int last, String ch){
        StateRange range = new StateRange(first, last);
        matchChunk(state, range, ch, true);
    }

    void matchChunk(int[] state, StateRange range, String ch, boolean addInit)
    {
        step(state, range, '^');
        if(addInit)
        {
            applySymbol(initial, '^', state, range);
        }
        for(int i = 0, limit = ch.length(); i < limit; i++)
        {
            switch(ch.charAt(i))
            {
                case '\\':
                    step(state, range, Character.toLowerCase(ch.charAt(++i)), any_char);
                    break;
                case '<':
                    for(int j = i+1; j < ch.length(); j++)
                    {
                    if(ch.charAt(j) == '>')
                    {
                        int symbol = alpha.cast(ch.substring(i, j - i + 1));
                        if(symbol != 0)
                        {
                            step(state, range, symbol, any_tag);
                        }
                        else
                        {
                            step(state, range, any_tag);
                        }
                        i = j;
                        break;
                    }
                }
                break;
                default:
                    step(state, range, Character.toLowerCase(ch.charAt(i)), any_char);
                    break;
            }
        }
        step(state, range, '$');
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

    public void matchPreparedChunk(int[] state, int first, int last){
        StateRange range = new StateRange(first, last);
        step(state, range, '^');
        applySymbol(initial, '^', state, range);
        for(int i = 0; i < prematchIdx; i++)
        {
            if(prematch[i] == any_tag)
            {
                step(state, range, any_tag);
            }
            else
            {
                step(state, range, prematch[i], prematchAlt[i]);
            }
        }
        step(state, range, '$');
    }

    public boolean shouldShift(int[] state, int first, int last){
        for(int i = first; i != last; i = (i+1)%RTXStateSize)
        {
            if(nodes.get(state[i]).search(' ') != -1)
            {
                return true;
            }
        }
        return false;
    }

    boolean shouldShift(int[] state, int local_first, int local_last, String chunk)
    {
        int[] localState = Arrays.copyOf(state, RTXStateSize);
        StateRange range = new StateRange(local_first, local_last);
        step(localState, range, lookahead);
        matchChunk(localState, range, chunk, false);
        return range.first != range.last;
    }

    Pair<Integer, Double> getRule(int[] state, int first, int last){
            int rule = -1;
            double weight = 0.0;
            int len = 0;
            for(int i = first; i != last; i = (i+1)%RTXStateSize)
            {
                MatchNode2 node = nodes.get(state[i]);
                if(node.rule_begin == -1) continue;
                for(int rl = node.rule_begin; rl <= node.rule_end; rl++)
                {
                    boolean rej = false;
                    for(int rj = 0; rj < rejectedCount; rj++)
                    {
                        if(rejected[rj] == rule_numbers[rl])
                        {
                            rej = true;
                            break;
                        }
                    }
                    if(rej) continue;
                    if(rule_lengths[rl] > len)
                    {
                        rule = rule_numbers[rl];
                        weight = rule_weights[rl];
                        len = rule_lengths[rl];
                        continue;
                    }
                    if(rule != -1 && rule_lengths[rl] < len) continue;
                    if(rule != -1 && rule_weights[rl] < weight) continue;
                    if(rule != -1 && rule_numbers[rl] > rule) continue;

                    rule = rule_numbers[rl];
                    weight = rule_weights[rl];
                }
            }
            return new Pair<>(rule, weight);
    }

    int getRuleUnweighted(int[] state, int first, int last)
        {
            for(int i = first; i != last; i = (i+1)%RTXStateSize)
            {
                if(nodes.get(state[i]).rule_begin != -1)
                {
                    return rule_numbers[nodes.get(state[i]).rule_begin];
                }
            }
            return -1;
    }

    public void resetRejected() {
        rejectedCount = 0;
    }

    void rejectRule(int rule){
        rejected[rejectedCount++] = rule;
    }
}

