package org.apertium.grammar;

import org.apertium.lttoolbox.collections.AbundantIntSet;

import java.util.List;
import java.util.Map;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class Reading {
    public AbundantIntSet tags;
    public List<Integer> tags_list;
    public List<Integer> tags_bloom;
    public String tags_string;
    public int tags_string_hash;
    public Tag mapping;
    public Cohort parent;
    public List<Integer> tags_textual;
    public List<Integer> tags_textual_bloom;
    public int baseform;
    public Map<Integer, Tag> tags_numerical;
    public List<Integer> tags_plain;
    public List<Integer> tags_plain_bloom;
    public boolean noprint;

    public void rehash() {

    }
}
