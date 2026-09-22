/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apertium.transfer;

import java.util.HashSet;

/**
 *
 * @author Jacob Nordfalk
 */
public class WordList {
  private final HashSet<String> elements;
  private final HashSet<String> elementsLowercase;

  public WordList(String[] list) {
    int cap = Math.max((int) (list.length / .75f) + 1, 16);
    elements = new HashSet<>(cap);
    elementsLowercase = new HashSet<>(cap);
    for (String e : list) {
      elements.add(e);
      elementsLowercase.add(e.toLowerCase());
    }
  }

  public boolean containsIgnoreCase(String source) {
    return elementsLowercase.contains(source.toLowerCase());
  }

  public boolean contains(String source) {
    return elements.contains(source);
  }

  public boolean containsIgnoreCaseBeginningWith(String source) {
    String s = source.toLowerCase();
    for (String e : elementsLowercase)
      if (e.startsWith(s))
        return true;
    return false;
  }

  public boolean containsBeginningWith(String source) {
    for (String e : elements){
      if (e.startsWith(source)){
        return true;
      }
    }
    return false;
  }

  public boolean containsIgnoreCaseEndingWith(String source) {
    String s = source.toLowerCase();
    for (String e : elementsLowercase)
      if (e.endsWith(s))
        return true;
    return false;
  }

  public boolean containsEndingWith(String source) {
    for (String e : elements){
      if (e.endsWith(source)){
        return true;
      }
    }
    return false;
  }
}
