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
    public int baseform = 0;
    public int hash = 0;
    public int hash_plain = 0;
    public int number = 0;

    public Bloomish tags_bloom = new Bloomish();
    public Bloomish tags_plain_bloom = new Bloomish();
    public Bloomish tags_textual_bloom = new Bloomish();

    public Tag mapping = null;
    public Cohort parent = null;
    public Reading next = null;
    public List<Integer> hit_by;
    public List<Integer> tags_list;
    public List<Integer> tags;
    public List<Integer> tags_plain;
    public List<Integer> tags_textual;
    public Map<Integer, Tag> tags_numerical;

    // Remove for real ordered mode
    public String tags_string;
    public int tags_string_hash = 0;

    public boolean noprint;

}
