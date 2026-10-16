package org.apertium.lttoolbox.process;

/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
import org.apertium.lttoolbox.Alphabet;

import java.util.*;

import org.apertium.lttoolbox.Pair;
import org.apertium.lttoolbox.process.Node.Transition;

/**
 * Class to represent the set of alive states of a transducer
 *
 * @author Raah
 */
public class State {
  public static final boolean CONSISTENCY_CHECKS = false;
  /**
   * Whether object pooling should be done. Object allocation (of small objects) on modern JVMs is even so fast that
   * making a copy of immutable objects sometimes outperforms modification of mutable (and often old) objects. See also
   * "Object pooling is now a serious performance loss": <a href="http://www.theserverside.com/news/thread.tss?thread_id=37146">...</a>
   * Pooling seems to do not difference on JDK 1.6, but it might make a big difference for smaller VMs, like Androids or
   * J2ME JVMs in phones, so we turn it on for now
   */
  public static final boolean REUSE_OBJECTS = true;
  /**
   * Pre-allocate for this amount of elements in a sequence
   */
  public static final int INITAL_SEQUENCE_ALLOCATION = 50;
  public static boolean DEBUG = false;

  public void consistency_check() {
    //if (CONSISTENCY_CHECKS) for (TNodeState x : state) x.consistency_check();
  }

  /**
   * one state element in the current set of states of transducer processing
   *
   * @author Raah
   */
  static class TNodeState {
    int where_node_id;
    /**
     * Which transducer does this node belong to
     */
    final TransducerExe transducer;
    /*
     * Which node in the transducer we are currently visiting
     */
    /**
     * The list of output symbols we produced while getting to this node
     */
    ArrayList<Integer> sequence;
    /**
     * caseWasChanged means input was lowercased - thus we should consider uppercasing the output symbols before
     * outputting
     */
    boolean caseWasChanged;

    public TNodeState(TransducerExe transducer, int where_node_id, ArrayList<Integer> sequence, boolean caseWasChanged) {
      this.transducer = transducer;
      this.where_node_id = where_node_id;
      this.sequence = sequence;
      this.caseWasChanged = caseWasChanged;
    }

    public TNodeState(TransducerExe transducer) {
      this.transducer = transducer;
    }

    /**
     * special constructor to signal that the sequence list must be initialized
     */
    TNodeState(TransducerExe transducer, boolean b) {
      sequence = new ArrayList<>();
      this.transducer = transducer;
    }

    TNodeState(TransducerExe transducer, int sequence_size) {
      sequence = new ArrayList<>(sequence_size);
      this.transducer = transducer;
    }

    final void consistency_check() {
    }

    private boolean isFinal() {
      // return transducer.getFinals().contains(where);
      return transducer.isFinal(where_node_id);
    }

    void nodeStatePool_release() {
      transducer.nodeStatePool_release(this);
    }

    TNodeState nodeStatePool_get() {
      TNodeState nn = transducer.nodeStatePool_get();
      //nn.transducer = transducer;
      return nn;
    }
  }

  ArrayList<TNodeState> state = new ArrayList<>(50);

  State copy(State other_state) {

    //System.err.println("this.state = " + this.state);
    if (REUSE_OBJECTS) {
      for (int i = state.size(); i > 0;) {
        state.get(--i).nodeStatePool_release();
      }
    }
    state.clear();

    ArrayList<TNodeState> other_states = other_state.state;
    for (int i = 0, limit = other_states.size(); i != limit; i++) {
      TNodeState tn = other_states.get(i);
      if (REUSE_OBJECTS) {
        TNodeState copy = tn.nodeStatePool_get();
        //copy.transducer = tn.transducer;
        copy.caseWasChanged = tn.caseWasChanged;
        copy.sequence.addAll(tn.sequence);
        copy.where_node_id = tn.where_node_id;
        copy.consistency_check();
        tn.consistency_check();

        this.state.add(copy);
      } else {
        this.state.add(new TNodeState(tn.transducer, tn.where_node_id, new ArrayList<>(tn.sequence), tn.caseWasChanged));
      }
    }
    return this;
  }

  public State copy() {
    return new State().copy(this);
  }

  /**
   * Number of alive transductions
   *
   * @return the size
   */
  public int size() {
    return state.size();
  }

  /**
   * Init the state with the initial node and empty output
   *
   * @param transducerc collection of transducers to initialize from
   */
  void init(Collection<TransducerExe> transducerc) {
    State initial_state = this;
    initial_state.state.clear();
    for (TransducerExe transducer : transducerc) {
      State.TNodeState state_i = new State.TNodeState(transducer, true);
      //state_i.transducer = transducer;
      state_i.where_node_id = transducer.getInitialId();
      state_i.caseWasChanged = false;
      state_i.consistency_check();
      initial_state.state.add(state_i);
    }
    initial_state.epsilonClosure();
  }

  public void init(TransducerExe transducer) {
    ArrayList<TransducerExe> c = new ArrayList<>();
    c.add(transducer);
    init(c);
  }

  public String toString() {
    return this.state.toString();
  }
  /**
   * Reference to last discarded list of nodestates
   */
  private ArrayList<TNodeState> reusable_state = REUSE_OBJECTS ? new ArrayList<>(50) : null;

  /**
   * Make a transition, version for lowercase letters and symbols
   *
   * @param input the input symbol
   */
  private void apply(int input) {
    ArrayList<TNodeState> new_state;
    if (REUSE_OBJECTS) {
      reusable_state.clear();
      new_state = reusable_state;
    } else {
      new_state = new ArrayList<>(state.size() * 2);
    }
    consistency_check();

    if (input == 0) { // in transfer, it happens an unknown symbol is translated to 0. Avoid interpreting that as an epsilon.
      if (REUSE_OBJECTS)
        reusable_state = state;
      state = new_state;
      return;
    }

    for (int i = 0, limit = state.size(); i != limit; i++) {
      TNodeState state_i = state.get(i);
      /*
       * Transition it = state_i.where.transitions_get(input);
       * while (it != null) {
       * TNodeState tn = REUSE_OBJECTS?nodeStatePool_get(): new TNodeState(state_i.sequence.size()+1);
       * tn.where = it.node_dest;
       * tn.caseWasChanged = state_i.caseWasChanged;
       * tn.sequence.addAll(state_i.sequence);
       * tn.sequence.add(it.output_symbol);
       * new_state.add(tn);
       * it = it.next;
       * }
       */
      Transition ti = Node.transitions_getIterator(state_i.transducer, state_i.where_node_id, input);
      while (ti != null) {
        TNodeState tn = REUSE_OBJECTS ? state_i.nodeStatePool_get() : new TNodeState(state_i.transducer, state_i.sequence.size() + 1);
        //tn.transducer = state_i.transducer;
        tn.where_node_id = ti.node_dest;
        tn.consistency_check();
        tn.caseWasChanged = state_i.caseWasChanged;
        tn.sequence.addAll(state_i.sequence);
        tn.sequence.add(ti.output_symbol);
        new_state.add(tn);
        ti = ti.next;
      }

      if (REUSE_OBJECTS)
        state_i.nodeStatePool_release();
    }
    if (REUSE_OBJECTS)
      reusable_state = state;
    state = new_state;
  }

  /**
   * Make a transition, version for lowercase and uppercase letters
   *
   * @param input the input symbol (which is actually always uppercase)
   * @param lowerCasedInput the alternative input symbol (mostly its always Alphabet.toLowerCase(input))
   */
  private void apply(int input, int lowerCasedInput) {
    ArrayList<TNodeState> new_state;
    if (REUSE_OBJECTS) {
      reusable_state.clear();
      new_state = reusable_state;
    } else {
      new_state = new ArrayList<>(state.size() * 2);
    }
    //System.err.println("apply i="+input+ "  state.size()="+state.size());

    for (int i = 0, limit = state.size(); i != limit; i++) {
      TNodeState state_i = state.get(i);

      Transition ti = Node.transitions_getIterator(state_i.transducer, state_i.where_node_id, input);
      while (ti != null) {
        TNodeState tn = REUSE_OBJECTS ? state_i.nodeStatePool_get() : new TNodeState(state_i.transducer, state_i.sequence.size() + 1);
        //tn.transducer = state_i.transducer;
        tn.where_node_id = ti.node_dest;
        tn.consistency_check();
        tn.caseWasChanged = state_i.caseWasChanged;
        tn.sequence.addAll(state_i.sequence);
        tn.sequence.add(ti.output_symbol);
        new_state.add(tn);
        ti = ti.next;
      } //XXX no pool now: pool.release(state.get(i).sequence);

      // try also apply lowerCasedInput
      ti = Node.transitions_getIterator(state_i.transducer, state_i.where_node_id, lowerCasedInput);
      while (ti != null) {
        TNodeState tn = REUSE_OBJECTS ? state_i.nodeStatePool_get() : new TNodeState(state_i.transducer, state_i.sequence.size() + 1);
        //tn.transducer = state_i.transducer;
        tn.where_node_id = ti.node_dest;
        tn.consistency_check();
        tn.caseWasChanged = true; // lowercased version of input
        tn.sequence.addAll(state_i.sequence);
        tn.sequence.add(ti.output_symbol);
        new_state.add(tn);
        ti = ti.next;
      }

      if (REUSE_OBJECTS)
        state_i.nodeStatePool_release();
    }

    if (REUSE_OBJECTS)
      reusable_state = state;
    state = new_state;
    if (CONSISTENCY_CHECKS)
      consistency_check();
  }

  /**
   * Calculate the epsilon closure over the current state, replacing its content. i.e. expand to all states reachable
   * consuming θ (the empty input symbol)
   */
  public void epsilonClosure() {
    for (int i = 0; i != state.size(); i++) {
      TNodeState state_i = state.get(i);
      // get the transitions consuming θ (the empty input symbol)
      Transition ti = Node.transitions_getIterator(state_i.transducer, state_i.where_node_id, 0);
      while (ti != null) {
        TNodeState tn = REUSE_OBJECTS ? state_i.nodeStatePool_get() : new TNodeState(state_i.transducer, state_i.sequence.size() + 1);
        //tn.transducer = state_i.transducer;
        tn.where_node_id = ti.node_dest;
        tn.consistency_check();
        tn.caseWasChanged = state_i.caseWasChanged;
        tn.sequence.addAll(state_i.sequence);
        int output_symbol = ti.output_symbol;
        if (output_symbol != 0) {
          tn.sequence.add(output_symbol);
        }
        state.add(tn);
        ti = ti.next;
      }
    }
    if (CONSISTENCY_CHECKS)
      consistency_check();
  }

  /**
   * step = apply + epsilonClosure
   *
   * @param input the input symbol
   */
  public void step(int input) {
    if (DEBUG) {
      System.err.println("state f = " + state + "     - apply (" + (char) input);
    }
    apply(input);
    if (DEBUG) {
      System.err.println("state e1= " + state);
    }
    epsilonClosure();
    if (DEBUG) {
      System.err.println("state e2= " + state);
    }
  }

  /**
   * step = apply + epsilonClosure
   *
   * @param input the input symbol
   * @param lowerCasedInput the alternative input symbol (typically lowercase version of input symbol)
   */
  public void step(int input, int lowerCasedInput) {
    apply(input, lowerCasedInput);
    epsilonClosure();
  }

  public void step_case(char val, boolean caseSensitive) {
    if (!Alphabet.isUpperCase(val) || caseSensitive) {
      step(val);
    } else {
      step(val, Alphabet.toLowerCase(val));
    }
  }

  public void step_case(int val, boolean caseSensitive) {
    if (Alphabet.isTag(val) || !Alphabet.isUpperCase(val) || caseSensitive) {
      step(val);
    } else {
      step(val, Alphabet.toLowerCase(val));
    }
  }

  /**
   * Return true if at least one record of the state references a final node of the set
   *
   * @param transducers set of final nodes
   * @return true if the state is final
   */
  public boolean isFinal(Set<TransducerExe> transducers) {
    for (int i = 0, limit = state.size(); i != limit; i++) {
      TNodeState state_i = state.get(i);
      if (!transducers.contains(state_i.transducer))
        continue;
      if (state_i.isFinal()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return true if at least one record of the state references a final node
   *
   * @return true if the state is final
   */
  public boolean isFinal() {
    for (int i = 0, limit = state.size(); i != limit; i++) {
      if (state.get(i).isFinal()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Slower version of the StringBuilder filterFinals()
   */
  String filterFinals(Alphabet alphabet, SetOfCharacters escaped_chars, boolean uppercase, boolean firstupper) {
    return filterFinals(new StringBuilder(), alphabet, escaped_chars, uppercase, firstupper).toString();
  }

  /**
   * Print all outputs of current parsing, preceeded by a bar '/', from the final nodes of the state. Examples:
   * /le<prn><pro><p3><nt>/le<det><def><m><sg>/le<prn><pro><p3><m><sg> /domaine<n><m><sg> /,<cm>
   *
   * @param result append to this buffer
   * @param alphabet the alphabet to decode strings
   * @param escaped_chars the set of chars to be preceeded with one backslash
   * @param uppercase true if the word is uppercase
   * @param firstupper true if the first letter of a word is uppercase
   * @return the result of the transduction
   */
  public StringBuilder filterFinals(StringBuilder result, Alphabet alphabet, SetOfCharacters escaped_chars, boolean uppercase, boolean firstupper) {

    for (int i = 0, limit = state.size(); i != limit; i++) {
      TNodeState state_i = state.get(i);
      //if (finals.contains(state_i.where)) {
      if (state_i.isFinal()) {
        result.append('/');
        int first_char = result.length();

        // make all uppercase if original word was uppercase && case was changed to lowercase during match
        boolean upc = uppercase && state_i.caseWasChanged;

        for (int j = 0, limit2 = state_i.sequence.size(); j != limit2; j++) {
          int symbol = (state_i.sequence).get(j);
          if (escaped_chars.contains((char) symbol)) {
            result.append('\\');
          }
          result.append(alphabet.getSymbol(symbol, upc));
        }


        if (firstupper && state_i.caseWasChanged && result.length() > first_char) {
          if (result.charAt(first_char) == '~') {
            // skip post-generation mark
            result.setCharAt(first_char + 1, Alphabet.toUpperCase(result.charAt(first_char + 1)));
          } else {
            result.setCharAt(first_char, Alphabet.toUpperCase(result.charAt(first_char)));
          }
        }
      }
    }
    //System.err.println("filterFinals RET ( " + result);
    return result;
  }

  /**
   * For lexical selection. NOTE: Return type is incorrect for the time being
   */
  public StringBuilder filterFinalsLRX(StringBuilder result, Alphabet alphabet, SetOfCharacters escaped_chars) {

    for (int i = 0, limit = state.size(); i != limit; i++) {
      TNodeState state_i = state.get(i);
      if (state_i.isFinal()) {
        //if (finals.contains(state_i.where)) {
        result.append('/');

        for (int j = 0, limit2 = state_i.sequence.size(); j != limit2; j++) {
          int symbol = (state_i.sequence).get(j);
          if (escaped_chars.contains((char) symbol)) {
            result.append('\\');
          }
          result.append(alphabet.getSymbol(symbol));
        }
      }
    }
    //System.err.println("filterFinals RET ( " + result);
    return result;
  }

  /**
   * Find final states, remove those that not has a requiredSymbol and 'restart' each of them as the set of initial
   * states, but remembering the sequence and adding a separationSymbol
   */
  void restartFinals(int requiredSymbol, State restart_state, int separationSymbol) {

    ArrayList<TNodeState> added_states = new ArrayList<>();

    for (int i = 0; i < state.size(); i++) {
      TNodeState state_i = state.get(i);
      // A state can be a possible final state and still have transitions

      //if (finals.contains(state_i.where)) {
      if (state_i.isFinal()) {
        boolean restart = lastPartHasRequiredSymbol(state_i.sequence, requiredSymbol, separationSymbol);
        if (restart) {
          if (restart_state != null) {
            if (DEBUG) {
              System.err.println("restart state " + i + "= " + state_i);
            }
            for (TNodeState initst : restart_state.state) {
              TNodeState tn = REUSE_OBJECTS ? initst.nodeStatePool_get() : new TNodeState(initst.transducer, state_i.sequence.size() + 1);
              //tn.transducer = initst.transducer;
              tn.where_node_id = initst.where_node_id;
              tn.caseWasChanged = state_i.caseWasChanged;
              tn.sequence.addAll(state_i.sequence);
              tn.sequence.add(separationSymbol);
              added_states.add(tn);
            }
          }
        }
      }
    }
    state.addAll(added_states);
  }

  private boolean lastPartHasRequiredSymbol(List<Integer> seq, int requiredSymbol, int separationSymbol) {
    // state is final - it should be restarted it with all elements in stateset restart_state, with old symbols conserved
    boolean restart = false;
    for (int n = seq.size() - 1; n >= 0; n--) {
      int symbol = seq.get(n);
      if (symbol == requiredSymbol) {
        restart = true;
        break;
      }
      if (symbol == separationSymbol) {
        break;
      }
    }
    return restart;
  }

  void pruneStatesWithForbiddenSymbol(int forbiddenSymbol) {

    Integer forbiddenSymbolInteger = forbiddenSymbol;

    // remove states containing a forbidden symbol
    for (int i = state.size() - 1; i >= 0; i--) {
      TNodeState state_i = state.get(i);
      // TODO optimize: search from end,
      if (state_i.sequence.contains(forbiddenSymbolInteger)) {
        state.remove(i);
        if (REUSE_OBJECTS) {
          state_i.nodeStatePool_release();
        }
      }
    }
  }

  void pruneCompounds(int requiredSymbol, int separationSymbol, int compound_max_elements) {
    int minNoOfCompoundElements = compound_max_elements; //Integer.MAX_VALUE-1;
    int[] noOfCompoundElements = new int[state.size()];
    for (int i = 0; i < state.size(); i++) {
      List<Integer> seq = state.get(i).sequence;
      if (!lastPartHasRequiredSymbol(seq, requiredSymbol, separationSymbol)) {
        //
        noOfCompoundElements[i] = Integer.MAX_VALUE;
        continue;
      }
      int this_noOfCompoundElements = 0;
      for (int j = seq.size() - 2; j > 0; j--) {
        if (seq.get(j) == separationSymbol) {
          this_noOfCompoundElements++;
        }
      }
      noOfCompoundElements[i] = this_noOfCompoundElements;
      minNoOfCompoundElements = Math.min(minNoOfCompoundElements, this_noOfCompoundElements);
    }

    // remove states with more than minimum number of compounds
    for (int i = state.size() - 1; i >= 0; i--) {
      if (noOfCompoundElements[i] > minNoOfCompoundElements) {
        if (REUSE_OBJECTS) {
          state.remove(i).nodeStatePool_release();
        } else {
          state.remove(i);
        }
      }
    }
  }

  /**
   * Same as previous one, but the output is adapted to the SAO system
   *
   * @param alphabet the alphabet to decode strings
   * @param escaped_chars the set of chars to be preceeded with one backslash
   * @param uppercase true if the word is uppercase
   * @param firstupper true if the first letter of a word is uppercase
   * @param firstchar first character of the word
   * @return the result of the transduction
   */
  String filterFinalsSAO(Alphabet alphabet, SetOfCharacters escaped_chars, boolean uppercase, boolean firstupper, int firstchar) {
    StringBuilder result = new StringBuilder();

    for (int i = 0, limit = state.size(); i != limit; i++) {
      TNodeState state_i = state.get(i);
      //if (finals.contains(state_i.where)) {
      if (state_i.isFinal()) {
        result.append('/');
        int first_char = result.length() + firstchar;
        for (int j = 0, limit2 = state_i.sequence.size(); j != limit2; j++) {
          if (escaped_chars.contains((char) (state_i.sequence).get(j).intValue())) {
            result.append('\\');
          }
          if (Alphabet.isTag(((state_i.sequence)).get(j))) {
            result.append('&');
            result.append(alphabet.getSymbol(state_i.sequence.get(j)));
            result.setCharAt(result.length() - 1, ';');
          } else {
            result.append(alphabet.getSymbol(((state_i.sequence)).get(j), uppercase));
          }
        }
        if (firstupper) {
          if (result.charAt(first_char) == '~') {
            // skip post-generation mark
            result.setCharAt(first_char + 1, Alphabet.toUpperCase(result.charAt(first_char + 1)));
          } else {
            result.setCharAt(first_char, Alphabet.toUpperCase(result.charAt(first_char)));
          }
        }
      }
    }

    return result.toString();
  }

  /**
   * Same as previous one, but the output is adapted to the TM system
   *
   * @param numbers the set of final nodes
   * @param alphabet the alphabet to decode strings
   * @param escaped_chars the set of chars to be preceeded with one backslash
   * @return the result of the transduction
   */
  String filterFinalsTM(Alphabet alphabet, SetOfCharacters escaped_chars, LinkedList<String> blankqueue, ArrayList<String> numbers) {
    StringBuilder result = new StringBuilder();
      for (TNodeState state_i : state) {
          //if (finals.contains(state_i.where)) {
          if (state_i.isFinal()) {
              result.append('/');
              for (int j = 0, limit2 = state_i.sequence.size(); j < limit2; j++) {
                  if (escaped_chars.contains((char) state_i.sequence.get(j).intValue())) {
                      result.append('\\');
                  }
                  result.append(alphabet.getSymbol(state_i.sequence.get(j)));
              }
          }
      }
    String result2 = "";
    ArrayList<String> fragments = new ArrayList<>();
    fragments.add("");
    for (int i = 0, limit = result.length(); i < limit; i++) {
      if (result.charAt(i) == ')') {
        fragments.add("");
      } else {
        fragments.set(fragments.size() - 1, fragments.getLast() + result.charAt(i));
      }
    }

    for (int i = 0, limit = fragments.size(); i < limit; i++) {
      if (i < limit - 1) {
        if (fragments.get(i).length() >= 2
            && fragments.get(i).endsWith("(#")) {
          String whitespace = "";
          if (!blankqueue.isEmpty()) {
            whitespace = blankqueue.getFirst().substring(1);
            blankqueue.removeFirst();
            whitespace = whitespace.substring(0, whitespace.length() - 1);
          }
          fragments.set(i, fragments.get(i).substring(0, fragments.get(i).length() - 2) + whitespace);
        } else {
          boolean replaced = false;
          for (int j = fragments.size() - 1; j >= 0; j--) {
            if (fragments.get(i).length() > 3
                && fragments.get(i).charAt(j) == '\\'
                && fragments.get(i).charAt(j + 1) == '@'
                && fragments.get(i).charAt(j + 2) == '(') {
              int num = 0;
              boolean correct = true;
              for (int k = j + 3, limit2 = fragments.get(i).length(); k < limit2; k++) {

                if (iswdigit(fragments.get(i).charAt(k))) {
                  num *= 10;
                  num += ((int) fragments.get(i).charAt(k)) - 48;
                } else {
                  correct = false;
                  break;
                }
              }
              if (correct) {
                fragments.set(i, fragments.get(i).substring(0, j) + numbers.get(num - 1));
                replaced = true;
                break;
              }
            }
          }
          if (!replaced) {
            fragments.set(i, fragments.get(i) + ')');
          }
        }
      }
    }
    result = new StringBuilder();
      for (String fragment : fragments) {
          result.append(fragment);
      }
    return result.toString();
  }

  /**
   * Compute if a character is a digit (gives the same results as the c++ iswdigit() function
   *
   * @param c the character
   * @return true if the c is a digit
   */
  private boolean iswdigit(char c) {
      return (((int) c >= 48 && (int) c <= 57) || (int) c == 178 || (int) c == 179 || (int) c == 185);
  }

  /*
   * @author Joan Jerez, ToBeIT
   */

  List<List<Pair<Integer, Double>>> sequence_pool = new ArrayList<>();
  ArrayList<State.TNodeState2> state2 = new ArrayList<>(50);

  static class TNodeState2{
    Node where;
    List<Pair<Integer, Double>> sequence;
    // a state is "dirty" if it was introduced at runtime (case variants, etc.)
    boolean dirty;

    public TNodeState2(Node transducer, List<Pair<Integer, Double>> sequence, boolean caseWasChanged) {
      this.where = transducer;
      this.sequence = sequence;
      this.dirty = caseWasChanged;
    }
  }

  public void init(Node initial)
  {
    state2.clear();
    state2.addLast(new TNodeState2(initial, new_sequence(), false));
    state2.getFirst().sequence.clear();
    epsilonClosure();
  }

  List<Pair<Integer, Double>> new_sequence() {
    if (sequence_pool.isEmpty()) {
      sequence_pool.addLast(new ArrayList<>());
    }
    List<Pair<Integer, Double>> ret = sequence_pool.getLast();
    sequence_pool.removeLast();
    return ret;
  }

  public void step(int val, int val2, int caseSensitive) {
    if (!Character.isUpperCase(val) || caseSensitive!= 0) { // TODO: revisar
      step(val, val2);
    } else if(val != Character.toLowerCase(val)) {
      step(val, Character.toLowerCase(val), val2);
    } else {
      step(val, val2);
    }
  }

  /**
   * Returns true if at least one record of the state references a
   * final node of the set
   * @param allFinals set of final nodes
   * @return true if the state is final
   */
  public boolean isFinal2(Map<Node, Double> allFinals) {
    for(int i = 0, limit = state2.size(); i != limit; i++)
    {
      if(allFinals.containsKey(state2.get(i).where))
      {
        return true;
      }
    }
    return false;
  }

  public void step_override(int cast, int anyTag, int cast1) {
    apply_override(cast, anyTag, cast1);
    epsilonClosure();
  }

  public void step_override(int input, int alt, int old_sym, int new_sym)
  {
    apply_override(input, alt, old_sym, new_sym);
    epsilonClosure();
  }

  void apply_override(int input, int old_sym, int new_sym){
    if(input == 0 || old_sym == 0)
    {
      state2.clear();
      sequence_pool.clear();
      return;
    }
    ArrayList<TNodeState2> new_state = new ArrayList<>();
    for(int i = 0, limit = state2.size(); i != limit; i++)
    {
      apply_into_override(new_state, input, old_sym, new_sym, i, false);
      apply_into_override(new_state, old_sym, old_sym, new_sym, i, true);
      free_sequence(state2.get(i).sequence);
    }
    state2 = new_state;
  }

  private void free_sequence(List<Pair<Integer, Double>> sequence) {
    sequence_pool.addLast(sequence);
  }

  public void merge(State other) {
    for (int i=0; i<other.state2.size(); i++) {
      List<Pair<Integer, Double>> tmp;
      tmp = other.state2.get(i).sequence;
      TNodeState2 ns = new TNodeState2(other.state2.get(i).where, tmp, other.state2.get(i).dirty);
      this.state2.addLast(ns);
    }
  }

  /**
   * Print all outputs of current parsing, preceded by a bar '/',
   * from the final nodes of the state
   * @param finals the set of final nodes
   * @param alphabet the alphabet to decode strings
   * @param escaped_chars the set of chars to be preceded with one backslash
   * @param uppercase true if the word is uppercase
   * @param firstupper true if the first letter of a word is uppercase
   * @param firstchar first character of the word
   * @return the result of the transduction
   */
  public String filterFinals(Map<Node, Double> finals,
                             Alphabet alphabet,
                             Set<Integer> escaped_chars,
                             boolean display_weights, int max_analyses, int max_weight_classes,
                             boolean uppercase, boolean firstupper, int firstchar)
  {
    List<String> result = new ArrayList<>();
    result = filterFinalsArray(result, finals, alphabet, escaped_chars, display_weights,
            max_analyses, max_weight_classes, uppercase, firstupper,
            firstchar);

    StringBuilder ret = new StringBuilder();
    for (String it : result) {
      ret.append('/');
      ret.append(it);
    }
    return ret.toString();
  }

  public String filterFinals(Map<Node, Double> finals,
                             Alphabet alphabet,
                             Set<Integer> escaped_chars,
                             boolean display_weights, int max_analyses, int max_weight_classes,
                             boolean uppercase, boolean firstupper){
    return filterFinals(finals, alphabet, escaped_chars, display_weights, max_analyses, max_weight_classes, uppercase,
            firstupper, 0);
  }

  /**
   * filterFinals(), but write the results into `result`
   */
  public List<String> filterFinalsArray(
          List<String> result,
          Map<Node, Double> finals,
          Alphabet alphabet,
          Set<Integer> escapedChars, // UChar32 mapea a Integer en colecciones de Java
          boolean displayWeights,
          int maxAnalyses, int maxWeightClasses,
          boolean uppercase, boolean firstupper, int firstchar
    ) {
      List<Pair<String, Double>> response = new ArrayList<>();

      // --- 1. PRIMERA PARTE: Construcción de secuencias y costes ---
      for (TNodeState2 it : state2) {
        if (!finals.containsKey(it.where)) {
          continue;
        }

        StringBuilder temp = new StringBuilder();
        double cost = finals.get(it.where);

        for (Pair<Integer, Double> step : it.sequence) {
          if (escapedChars.contains(step.first)) {
            temp.append('\\');
          }

          /* Nota: Asegúrate de que la función getSymbol de tu clase Alphabet en Java
          acepte un StringBuilder para concatenar el símbolo internamente. */
          temp.append(alphabet.getSymbol(step.first, it.dirty && uppercase));
          cost += step.second;
        }

        if (it.dirty && firstupper) {
          int loc = firstchar;
          if (loc < temp.length() && temp.charAt(loc) == '~') {
            loc++;
          }
          if (loc < temp.length()) {
            char upperChar = Character.toUpperCase(temp.charAt(loc));
            temp.setCharAt(loc, upperChar);
          }
        }

        response.add(new Pair<>(temp.toString(), cost));
      }

      // --- 2. SEGUNDA PARTE: Filtrado NFinals ---
      // Asumiendo que tienes una función equivalente a NFinals implementada en Java
      response = NFinals(response, maxAnalyses, maxWeightClasses);

      // --- 3. TERCERA PARTE: Deduplicación y formateo final ---
      result.clear();

      // En C++ usan sorted_vector para evitar duplicados.
      // En Java un HashSet ofrece búsquedas e inserciones O(1) idóneas para esto.
      Set<String> seen = new HashSet<>();

      for (Pair<String, Double> it : response) {
        // La función .add() devuelve false si el elemento ya existía (equivale a !seen.insert(...).second)
        if (!seen.add(it.first)) {
          continue;
        }
        String finalString = it.first;
        if (displayWeights) {
          // Equivalente a u_sprintf(w, "<W:%f>", it.second);
          // Usamos Locale.US para asegurar que el separador decimal sea un punto '.' y no una coma ','
          String weightStr = String.format(Locale.US, "<W:%f>", it.second);
          finalString += weightStr;
        }
        result.add(finalString);
      }
      return result;
  }

  List<Pair< String, Double>> NFinals(List<Pair<String, Double>> lf, int maxAnalyses, int maxWeightClasses)
  {
    List<Pair<String, Double>> result = new ArrayList<>();
    // sort(lf.begin(), lf.end(), sort_weights<UString, double>());
    for(Pair<String, Double> it : lf)
    {
      double last_weight = 0.0000;
      if(maxAnalyses > 0 && maxWeightClasses > 0)
      {
        result.addLast(new Pair<>(it.first, it.second));
        maxAnalyses--;
        if(last_weight!=it.second)
        {
          maxWeightClasses--;
        }
      }
      else break;
    }
    return result;
  }

  /**
   * Helper functions for the various apply()s to reduce code duplication
   * @return whether any transitions were made
   */
  private boolean apply_into_override(ArrayList<TNodeState2> newState, int input, int oldSym, int newSym, int index, boolean dirty) {
    // En C++: std::map<int, Dest>::const_iterator it = state[index].where->transitions.find(input);
    // En Java usamos un Map estándar y comprobamos si la clave existe.
    var currentTransitions = state2.get(index).where.transitions2;

    if (currentTransitions.containsKey(input)) {
      Node.Dest destObj = currentTransitions.get(input); // Este es nuestro "it->second"

      for (int j = 0; j != destObj.size; j++) {
        // Obtener una lista limpia del pool
        List<Pair<Integer, Double>> newV = new_sequence();

        // En C++: *newV = *(state[index].sequence); (Copia profunda del contenido de la lista)
        newV.clear(); // Por seguridad si el pool tuviera basura
        newV.addAll(state2.get(index).sequence);

        // En C++: if(it->first != 0) -> it->first es la clave del mapa, que es 'input'
        if (input != 0) {
          if (destObj.out_tag[j] == oldSym) {
            // push_back({new_sym, ...}) instanciando nuestro objeto par
            newV.add(new Pair<>(newSym, destObj.out_weight[j]));
          } else {
            newV.add(new Pair<>(destObj.out_tag[j], destObj.out_weight[j]));
          }
        }

        // En C++: new_state->push_back(TNodeState(...))
        newState.addLast(new TNodeState2(destObj.dest[j], newV, state2.get(index).dirty || dirty));
      }
      return true;
    }
    return false;
  }

  /**
   * Make a transition, but overriding the output symbol
   * @param input symbol read from infile
   * @param old_sym symbol from the FST
   * @param new_sym symbol we want to appear in outfile
   */
  void apply_override(int input, int alt, int old_sym, int new_sym) {

    if(input == alt)
    {
      apply_override(input, old_sym, new_sym);
      return;
    }

    if(input == 0 || old_sym == 0)
    {
      state2.clear();
      sequence_pool.clear();
      return;
    }

    ArrayList<TNodeState2> new_state = new ArrayList<>();
    for(int i = 0, limit = state.size(); i != limit; i++)
    {
      apply_into_override(new_state, input, old_sym, new_sym, i, false);
      apply_into_override(new_state, alt, old_sym, new_sym, i, true);
      apply_into_override(new_state, old_sym, old_sym, new_sym, i, true);
      free_sequence(state2.get(i).sequence);
    }

    state2 = new_state;
  }
}
