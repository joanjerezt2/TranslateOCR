/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apertium.lttoolbox.collections;

import java.util.Collection;

/**
 *
 * @author Jacob Nordfalk
 */
public interface IntSet extends Iterable<Integer> {
  void clear();

  int size();

  void add(int i);

  void remove(int i);

  boolean contains(int i);

  int firstInt();

  boolean addAll(Collection<? extends Integer> c);

  boolean addAll(IntSet c);
}
