/*
 * Copyright (C) 2005 Universitat d'Alacant / Universidad de Alicante
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
package org.apertium.tagger;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.Reader;

import org.apertium.lttoolbox.Compression;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import org.apertium.utils.IOUtils;

/**
 *
 * @author jimregan
 */
public class HMM {

    static class IntVector {
    ArrayList<Integer> nodes = new ArrayList<>();
  }

    private final TaggerData td;
  private int eos;
  private boolean debug;
  private boolean show_sf;
  private boolean null_flush;
  static final double DBL_MIN = 2.2250738585072014E-308;

  HMM(TaggerData tdata) {
    this.td = tdata;

    this.debug = false;
    this.show_sf = false;
    this.null_flush = false;
    this.eos = td.getTagIndex().get("TAG_SENT");
  }

  /**
   * Used to set the end-of-sentence tag
   *
   * @param t the end-of-sentence tag
   */
  void set_eos(int t) {
    eos = t;
  }

  /**
   * Used to set the debug flag
   */
  void set_debug(boolean d) {
    debug = d;
  }

  /**
   * Used to set the show superficial forms flag
   */
  void set_show_sf(boolean sf) {
    show_sf = sf;
  }

  /**
   * Reads the ambiguity classes from the stream received as input
   *
   * @param in the input stream
   */
  void read_ambiguity_classes(InputStream in) throws IOException {
    while (true) {
      int ntags = Compression.multibyte_read(in);
      if (ntags == -1) { // EOF
        break;
      }

      Set<Integer> ambiguity_class = new LinkedHashSet<>();

      for (; ntags != 0; ntags--) {
        ambiguity_class.add(Compression.multibyte_read(in));
      }

      if (!ambiguity_class.isEmpty()) {
        td.getOutput().add(ambiguity_class);
      }
    }
    td.setProbabilities(td.getTagIndex().size(), td.getOutput().size());
  }

  /**
   * Writes the ambiguity classes to the stream received as
   * a parameter
   *
   * @param o the output stream
   */
  void write_ambiguity_classes(OutputStream o) throws IOException {
    for (int i = 0; i != td.getOutput().size(); i++) {
      Set<Integer> ac = td.getOutput().get(i);
      Compression.multibyte_write(ac.size(), o);
      for (int it : ac) {
        Compression.multibyte_write(it, o);
      }
    }
  }

  /**
   * Reads the probabilities (matrices a and b) from the stream
   * received as a parameter
   *
   * @param in the input stream
   */
  void read_probabilities(InputStream in) throws IOException {
    td.read(in);
  }

  /**
   * Writes the probabilities (matrices a and b) to the stream
   * received as a parameter
   *
   * @param out the output stream
   */
  void write_probabilities(OutputStream out) throws IOException {
    td.write(out);
  }

  /**
   * Initializes the transition (a) and emission (b) probabilities
   * from an untagged input text by means of Kupiec's method
   *
   */
  void init_probabilities_kupiec(Reader in) throws IOException {
    int N = td.getN();
    int M = td.getM();
    int i, j, k, k1, k2, nw = 0;
    /*
     * M = Number of ambiguity classes
     */
    double[] classes_occurrences = new double[M];
    double[][] classes_pair_occurrences = new double[M][M];
    /*
     * N = Number of tags (states)
     */
    double[] tags_estimate = new double[N];
    double[][] tags_pair_estimate = new double[N][N];

    Collection output = td.getOutput();

    MorphoStream lexmorfo = new MorphoStream(in, true, td);
    TaggerWord word;

    for (k = 0; k < M; k++) {
      classes_occurrences[k] = 1;
      for (k2 = 0; k2 < M; k2++) {
        classes_pair_occurrences[k][k2] = 1;
      }
    }

    Set<Integer> tags = new LinkedHashSet<>();
    tags.add(eos);
    k1 = output.get(tags);
    classes_occurrences[k]++;

    word = lexmorfo.get_next_word();
    while (word != null) {
      if (++nw % 10000 == 0) {
        System.err.print('.');
        System.err.flush();
      }

      tags = word.get_tags();

      if (tags.isEmpty()) {
        tags = td.getOpenClass();
      } else if (output.has_not(tags)) {
        String errors;
        errors = "A new ambiguity class was found. I cannot continue.\n";
        errors += "Word '" + word.get_superficial_form() + "' not found in the dictionary.\n";
        errors += "New ambiguity class: " + word.get_string_tags() + "\n";
        errors += "Take a look at the dictionary and at the training corpus. Then, retrain.";
        fatal_error(errors);
      }

      k2 = output.get(tags);

      classes_occurrences[k1]++;
      classes_pair_occurrences[k1][k2]++;
      word = lexmorfo.get_next_word();
      k1 = k2;
    }

    // Estimation of the number of time each tags occurs in the training text
    for (i = 0; i < N; i++) {
      tags_estimate[i] = 0;
      for (k = 0; k < M; k++) {
        if (output.get(k).contains(i)) {
          tags_estimate[i] += classes_occurrences[k] / output.get(k).size();
        }
      }
    }

    //Estimation of the number of times each tag pair occurs
    for (i = 0; i < N; i++) {
      for (j = 0; j < N; j++) {
        tags_pair_estimate[i][j] = 0;
      }
    }

    Set<Integer> tags1, tags2;
    for (k1 = 0; k1 < M; k1++) {
      tags1 = output.get(k1);
      for (k2 = 0; k2 < M; k2++) {
        tags2 = output.get(k2);
        double noccurrences = classes_pair_occurrences[k1][k2] / (double) (tags1.size() * tags2.size());
        for (Integer itag1 : tags1.toArray(new Integer[0])) {
          for (Integer itag2 : tags2.toArray(new Integer[0])) {
            tags_pair_estimate[itag1][itag2] += noccurrences;
          }
        }
      }
    }

    //a[i][j] estimation
    double sum;
    double[][] tmpA = td.getA();
    for (i = 0; i < N; i++) {
      sum = 0;
      for (j = 0; j < N; j++) {
        sum += tags_pair_estimate[i][j];
      }

      for (j = 0; j < N; j++) {
        if (sum > 0) {
          tmpA[i][j] = tags_pair_estimate[i][j] / sum;
        } else {
          tmpA[i][j] = 0;
        }
      }
    }
    td.setA(tmpA);

    //b[i][k] estimation
    double[][] tmpB = td.getB();
    for (i = 0; i < N; i++) {
      for (k = 0; k < M; k++) {
        if (output.get(k).contains(i)) {
          if (tags_estimate[i] > 0) {
            tmpB[i][k] = (classes_occurrences[k] / output.get(k).size()) / tags_estimate[i];
          } else {
            tmpB[i][k] = 0;
          }
        }
      }
    }
    td.setB(tmpB);

    System.err.println();
  }

  /**
   * Initializes the transtion (a) and emission (b) probabilities
   * from a tagged input text by means of the expected-likelihood
   * estimate (ELE) method
   *
   * @param ftagged the input reader with the tagged corpus to process
   * @param funtagged the same corpus to process but untagged
   */
  void init_probabilities_from_tagged_text(Reader ftagged, Reader funtagged) throws IOException {
    int i, j, k, nw = 0;
    int N = td.getN();
    int M = td.getM();
    double[][] tags_pair = new double[N][N];
    double[][] emission = new double[N][M];

    MorphoStream stream_tagged = new MorphoStream(ftagged, true, td);
    MorphoStream stream_untagged = new MorphoStream(funtagged, true, td);

    TaggerWord word_tagged;
    TaggerWord word_untagged;
    Collection output = td.getOutput();

    Set<Integer> tags = new LinkedHashSet<>();

    for (i = 0; i < N; i++) {
      for (j = 0; j < N; j++) {
        tags_pair[i][j] = 0;
      }
    }
    for (k = 0; k < M; k++) {
      for (i = 0; i < N; i++) {
        if (output.get(k).contains(i)) {
          emission[i][k] = 0;
        }
      }
    }

    int tag1, tag2;
    tag1 = eos;

    // FIXME check get_next_word()
    word_tagged = stream_tagged.get_next_word();
    word_untagged = stream_untagged.get_next_word();
    while (word_tagged != null) {
      System.err.print(word_tagged);
      System.err.println(" -- " + word_untagged);

      if (!word_untagged.get_superficial_form().equals(word_tagged.get_superficial_form())) {
        System.err.println();
        System.err.println("Tagged text (.tagged) and analyzed text (.untagged) streams are not aligned.");
        System.err.println("Take a look at tagged text (.tagged).");
        System.err.println("Perhaps this is caused by a multiword unit that is not a multiword unit in one of the two files.");
        System.err.println(word_tagged + " -- " + word_untagged);
        // exit() is not an option as we are a library
        throw new Error();
      }

      if (++nw % 100 == 0) {
        System.err.print(".");
        System.err.flush();
      }

      tag2 = tag1;

      if (word_tagged.get_tags().isEmpty()) { // Unknown word
        tag1 = -1;
      } else if (word_tagged.get_tags().size() > 1) { // Ambiguous word
        System.err.println("Error in tagged text. An ambiguous word was found: " + word_tagged.get_superficial_form());
      } else {
        tag1 = word_tagged.get_tags().iterator().next();
      }

      if ((tag1 >= 0) && (tag2 >= 0)) {
        tags_pair[tag2][tag1]++;
      }

      if (word_untagged.get_tags().isEmpty()) {
        tags = td.getOpenClass();
      } else if (output.has_not(word_untagged.get_tags())) {
        String errors;
        errors = "A new ambiguity class was found. I cannot continue.\n";
        errors += "Word '" + word_untagged.get_superficial_form() + "' not found in the dictionary.\n";
        errors += "New ambiguity class: " + word_untagged.get_string_tags() + "\n";
        errors += "Take a look at the dictionary, then retrain.";
        fatal_error(errors);
      } else {
        tags = word_untagged.get_tags();
      }

      k = output.get(tags);
      if (tag1 >= 0) {
        emission[tag1][k]++;
      }

      word_tagged = stream_tagged.get_next_word();
      word_untagged = stream_untagged.get_next_word();
    }

    //Estimate of a[i][j]
    for (i = 0; i < N; i++) {
      double sum = 0;
      for (j = 0; j < N; j++) {
        sum += tags_pair[i][j] + 1.0;
      }
      for (j = 0; j < N; j++) {
        td.setAElement(i, j, (tags_pair[i][j] + 1.0) / sum);
      }
    }

    //Estimate of b[i][k]
    for (i = 0; i < N; i++) {
      int nclasses_appear = 0;
      double times_appear = 0.0;
      for (k = 0; k < M; k++) {
        if (output.get(k).contains(i)) {
          nclasses_appear++;
          times_appear += emission[i][k];
        }
      }
      for (k = 0; k < M; k++) {
        if (output.get(k).contains(i)) {
          td.setBElement(i, k, (emission[i][k] + (1.0 / nclasses_appear)) / (times_appear + 1.0));
        }
      }
    }

    System.err.println();
  }

  /**
   * Applies the forbid and enforce rules found in tagger specification.
   * To do so the transition matrix is modified by introducing null probabilities
   * in the involved transitions.
   */
  void apply_rules() {
    List<TForbidRule> forbid_rules = td.getForbidRules();
    List<TEnforceAfterRule> enforce_rules = td.getEnforceRules();
    int N = td.getN();
    int i, j, j2;
    boolean found;

      double ZERO = 1e-10;
      for (i = 0; i < forbid_rules.size(); i++) {
      td.setAElement(forbid_rules.get(i).tagi, forbid_rules.get(i).tagj, ZERO);
    }

    for (i = 0; i < enforce_rules.size(); i++) {
      for (j = 0; j < N; j++) {
        found = false;
        for (j2 = 0; j2 < enforce_rules.get(i).tagsj.size(); j2++) {
          if (enforce_rules.get(i).tagsj.get(j2) == j) {
            found = true;
            break;
          }
        }
        if (!found) {
          td.setAElement(enforce_rules.get(i).tagi, j, ZERO);
        }
      }
    }

    // Normalize probabilities
    for (i = 0; i < N; i++) {
      double sum = 0;
      for (j = 0; j < N; j++) {
        sum += td.getA()[i][j];
      }
      for (j = 0; j < N; j++) {
        if (sum > 0) {
          td.setAElement(i, j, td.getA()[i][j] / sum);
        } else {
          td.setAElement(i, j, 0);
        }
      }
    }
  }

  /**
   * Reads the expanded dictionary received as a parameter and calculates
   * the set of ambiguity classes that the tagger will manage.
   *
   * @param fdic the input reader with the expanded dictionary to read
   */
  void read_dictionary(Reader fdic) throws IOException {
    int i, k, nw = 0;
    TaggerWord word;
    Set<Integer> tags;
    Collection output = td.getOutput();

    MorphoStream morpho_stream = new MorphoStream(fdic, true, td);

    word = morpho_stream.get_next_word();

    while (word != null) {
      if (++nw % 10000 == 0) {
        System.err.println(".");
        System.err.flush();
      }

      tags = word.get_tags();

      if (!tags.isEmpty()) {
        k = output.get(tags);
      }

      word = morpho_stream.get_next_word();
    }
    System.err.println();

    // OPEN AMBIGUITY CLASS
    // It contains all tags that are not closed.
    // Unknown words are assigned the open ambiguity class
    k = output.get(td.getOpenClass());

    int N = td.getTagIndex().size();

    // Create ambiguity class holding one single tag for each tag.
    // If not created yet
    for (i = 0; i != N; i++) {
      Set<Integer> amb_class = new LinkedHashSet<>();
      amb_class.add(i);
      k = output.get(amb_class);
    }

    int M = output.size();

    System.err.println(N + " states and " + M + " ambiguity classes");
    td.setProbabilities(N, M);
  }

  /**
   * Filters ambiguity classes
   *
   * @param in reader to filter
   * @param out Output
   */
  void filter_ambiguity_classes(Reader in, Appendable out) throws IOException {
    Set<Set<Integer>> ambiguity_classes = new LinkedHashSet<>();
    MorphoStream morpho_stream = new MorphoStream(in, true, td);

    TaggerWord word = morpho_stream.get_next_word();

    while (word != null) {
      Set<Integer> tags = word.get_tags();

      if (!tags.isEmpty()) {
        if (!ambiguity_classes.contains(tags)) {
          ambiguity_classes.add(tags);
          word.outputOriginal(out);
        }
      }
      word = morpho_stream.get_next_word();
    }
  }

  void train(Reader ftxt) throws UnsupportedOperationException {
      throw new UnsupportedOperationException("HMM training doesn't work, "
              + "it hasn't been fully ported, yet!");
      /* TODO This should be finished sometime if we decide we actually need the training
     * functionality for the HMM tagger.
     */
  }

  void tagger(Reader in, Appendable out, boolean show_all_good_first) throws IOException {
    int i, j, k;
    TaggerWord word;// new TaggerWord();  // word =null;
    Integer tag;

    Set<Integer> tags = new LinkedHashSet<>();
    Set<Integer> pretags;

    double prob, x;

    int N = td.getN();
    double[][] alpha = new double[2][N];
    IntVector[][] best = new IntVector[2][N];
    for (int myi = 0; myi != 2; myi++) {
      for (int myj = 0; myj != N; myj++) {
        best[myi][myj] = new IntVector();
      }
    }

    ArrayList<TaggerWord> wpend = new ArrayList<>();

    MorphoStream morpho_stream = new MorphoStream(in, debug, td);
    morpho_stream.setNullFlush(null_flush);

    Collection output = td.getOutput();

    double loli = 0;

    //Initialization
    tags.add(eos);
    alpha[0][eos] = 1;

    word = morpho_stream.get_next_word();
    // the main loop reading words until EOF
    while (word != null) {
      boolean DEBUG = false;
      if (DEBUG) {
        word.print();
      }
      wpend.add(word);
      final int nwpend = wpend.size();
      final int nwpend2 = nwpend % 2;

      pretags = tags; // Tags from the previous word

      tags = word.get_tags();

      if (tags.isEmpty()) // This is an unknown word
      {
        tags = td.getOpenClass();
      }

      if (output.has_not(tags)) {
        if (debug) {
          String errors;
          errors = "A new ambiguity class was found. \n";
          errors += "Retraining the tagger is neccessary to take it into account.\n";
          errors += "Word '" + word.get_superficial_form() + "'.\n";
          errors += "New ambiguity class: " + word.get_string_tags() + "\n";
          System.err.print(errors);
        }
        tags = find_similar_ambiguity_class(tags);
      }

      k = output.get(tags);  //Ambiguity class the word belongs to
      if (DEBUG)
        System.out.println("k: " + k);

      clear_array_double(alpha[nwpend2]);
      clear_array_vector(best[nwpend2]);

      //Induction
      for (Integer itag : tags) {
        i = itag;
        if (DEBUG)
          System.out.println("i: " + itag);
        for (Integer jtag : pretags) {
          j = jtag;
          x = alpha[1 - nwpend2][j] * td.getA()[j][i] * td.getB()[i][k];
          if (DEBUG)
            System.out.println("j: " + jtag + " nwpend: " + nwpend + " A[j][i]: " + td.getA()[j][i] + " B[i][k]: " + td.getB()[i][k] + "  x: " + x);
          if (alpha[nwpend2][i] <= x) {
            if (nwpend > 1) {
              /* This should be *replacing* nodes, not just adding them.
               * However, if we're replacing the nodes with themselves,
               * no need to do anything, and in fact the clear() call
               * would be detrimental.
               */
              if (((nwpend2) != (1 - nwpend2)) || (i != j)) {
                best[nwpend2][i].nodes.clear();
                best[nwpend2][i].nodes.addAll(best[1 - nwpend2][j].nodes);
              }
            }
            if (DEBUG)
              System.out.println("best: " + (nwpend2) + " " + i);
            best[nwpend2][i].nodes.add(i);
            alpha[nwpend2][i] = x;
          }
        }
      }

      //Backtracking
      if (tags.size() == 1) {
        tag = tags.iterator().next();

        prob = alpha[nwpend2][tag];

        if (prob > 0) {
          loli -= Math.log(prob);
        } else {
          if (debug) {
            System.err.println("Problem with word '" + word.get_superficial_form() + "' " + word.get_string_tags());
          }
        }

        for (int t = 0; t < best[nwpend2][tag].nodes.size(); t++) {
          if (show_all_good_first) {
            String micad = wpend.get(t).get_all_chosen_tag_first(best[nwpend2][tag].nodes.get(t), td.getTagIndex().get("TAG_kEOF"));
            out.append(micad);
          } else {
            //Split out the following line for debugging.
            //String micad = wpend.get(t).get_lexical_form(best[nwpend2][tag].nodes.get(t), td.getTagIndex().get("TAG_kEOF"));
            int tagkeof = td.getTagIndex().get("TAG_kEOF");
            int tagT = best[nwpend2][tag].nodes.get(t);
            TaggerWord tempWord = wpend.get(t);
            tempWord.set_show_sf(show_sf); //Was missing, show superficial forms option won't work w/o this line
            String micad = tempWord.get_lexical_form(tagT, tagkeof);
            out.append(micad);
          }
        }
        wpend.clear();
        alpha[0][tag] = 1;
      }

      if (morpho_stream.getEndOfFile()) {
        if (null_flush) {
          out.append((char) 0x00);
        }

        IOUtils.flush(out);
        morpho_stream.setEndOfFile(false);
      }
      word = morpho_stream.get_next_word();

    }

    if (tags.size() > 1 && debug) {
      String errors;
      // errors = "The text to disambiguate has finished, but there are ambiguous words that have not been disambiguated.\n";
      errors = "This message should never appear. If you are reading this ..... this is very bad news.\n";
      System.err.print("\nError: " + errors);
    }

  }

  void print_A() {
    int i, j;
    System.out.println("TRANSITION MATRIX (A)");
    System.out.println("-------------------------------");
    for (i = 0; i != td.getN(); i++) {
      for (j = 0; j != td.getN(); j++) {
        System.out.println("A[" + i + "][" + j + "] = " + td.getA()[i][j]);
      }
    }
  }

  void print_B() {
    int i, k;
    System.out.println("EMISSION MATRIX (B)");
    System.out.println("-------------------------------");
    for (i = 0; i != td.getN(); i++) {
      for (k = 0; k != td.getM(); k++) {
        Collection output = td.getOutput();
        if (output.get(k).contains(i)) {
          System.out.println("B[" + i + "][" + k + "] = " + td.getB()[i][k]);
        }
      }
    }
  }

  /**
   * Prints the ambiguity classes.
   */
  void print_ambiguity_classes() {
    Set<Integer> ambiguity_class;
    System.out.println("AMBIGUITY CLASSES");
    System.out.println("-------------------------------");
    for (int i = 0; i != td.getM(); i++) {
      ambiguity_class = td.getOutput().get(i);
      System.out.print(i + ": ");
      for (Integer it : ambiguity_class) {
        System.out.print(it + " ");
      }
      System.out.println();
    }
  }

  /**
   * This method returns a known ambiguity class that is a subset of
   * the one received as a parameter. This is useful when a new
   * ambiguity class is found because of changes in the morphological
   * dictionary used by the MT system.
   *
   * @param c set of tags (ambiguity class)
   * @return a known ambiguity class
   */
  Set<Integer> find_similar_ambiguity_class(Set<Integer> c) {
    int size_ret = -1;
    Set<Integer> ret = td.getOpenClass();//Se devolverá si no encontramos ninguna clase mejor
    boolean skip_class;
    Collection output = td.getOutput();

    for (int k = 0; k < td.getM(); k++) {
      if ((output.get(k).size() > size_ret) && (output.get(k).size() < c.size())) {
        skip_class = false;
        // Test if output[k] is a subset of class
        for (Integer it : output.get(k)) {
          if (!c.contains(it)) {
            skip_class = true; //output[k] is not a subset of class
            break;
          }
        }
        if (!skip_class) {
          size_ret = output.get(k).size();
          ret = output.get(k);
        }
      }
    }
    return ret;
  }

  /**
   * Used to set the null_flush flag
   */
  void setNullFlush(boolean nf) {
    this.null_flush = nf;
  }

  /**
   * Helper method - prints an error message and exits
   *
   * @param err The string to print
   */
  private void fatal_error(String err) {
    throw new Error(err);
    // NEVER do System.exit(1); as we are a library
  }

  /**
   * Make all array positions equal to zero
   *
   * @param a the array
   */
  void clear_array_double(double[] a) {
      Arrays.fill(a, 0.0);
  }

  void clear_array_vector(IntVector[] a) {
      for (IntVector intVector : a) {
          intVector.nodes.clear();
      }
  }
}
