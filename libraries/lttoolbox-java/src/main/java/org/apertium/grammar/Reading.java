package org.apertium.grammar;

import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apertium.grammar.Inlines.hash_value;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class Reading implements Pool.Recyclable {
    public int baseform = 0;
    public int hash = 0;
    public int hash_plain = 0;
    public int number;

    public Bloomish tags_bloom = new Bloomish();
    public Bloomish tags_plain_bloom = new Bloomish();
    public Bloomish tags_textual_bloom = new Bloomish();

    public Tag mapping = null;
    public Cohort parent;
    public Reading next = null;
    public List<Integer> hit_by;
    public List<Integer> tags_list;
    public List<Integer> tags;
    public List<Integer> tags_plain;
    public List<Integer> tags_textual;
    public Map<Integer, Tag> tags_numerical;
    static Pool<Reading> pool_readings;

    // Remove for real ordered mode
    public String tags_string;
    public int tags_string_hash = 0;

    public boolean noprint;
    public boolean deleted;
    public boolean mapped;
    public boolean matched_target;
    public boolean matched_tests;
    public boolean immutable;
    public boolean active;

    public Reading(Cohort p) {
        this.mapped = false;
        this.deleted = false;
        this.noprint = false;
        this.matched_target = false;
        this.matched_tests = false;
        this.immutable = false;
        this.active = false;
        this.number = p != null ? (p.readings.size() * 1000 + 1000) : 0;
        this.parent = p;
    }

    public Reading(Reading r){
        this.mapped = r.mapped;
        this.deleted = r.deleted;
        this.noprint = r.noprint;
        this.matched_target = false;
        this.matched_tests = false;
        this.immutable = r.immutable;
        this.active = r.active;
        this.baseform = r.baseform;
        this.hash = r.hash;
        this.hash_plain = r.hash_plain;
        this.number = r.number + 100;
        this.tags_bloom = r.tags_bloom;
        this.tags_plain_bloom = r.tags_plain_bloom;
        this.tags_textual_bloom = r.tags_textual_bloom;
        this.mapping = r.mapping;
        this.parent = r.parent;
        this.next = r.next;
        this.hit_by = r.hit_by;
        this.tags_list = r.tags_list;
        this.tags = r.tags;
        this.tags_plain = r.tags_plain;
        this.tags_textual = r.tags_textual;
        this.tags_numerical = r.tags_numerical;
        this.tags_string = r.tags_string;
        this.tags_string_hash = r.tags_string_hash;

        if (next != null) {
            next = allocateReading(next);
        }
    }

    static Reading alloc_reading(Cohort p) {
        Reading r = pool_readings.get();
        if (r == null) {
            r = new Reading(p);
        }
        else {
            r.number = p != null ? (p.readings.size() * 1000 + 1000) : 0;
            r.parent = p;
        }
        return r;
    }

    static Reading alloc_reading(Reading o) {
        Reading r = pool_readings.get();
        if (r == null) {
            r = new Reading(o);
        }
        else {
            r.mapped = o.mapped;
            r.deleted = o.deleted;
            r.noprint = o.noprint;
            r.matched_target = false;
            r.matched_tests = false;
            r.immutable = false;
            r.active = false;
            r.baseform = o.baseform;
            r.hash = o.hash;
            r.hash_plain = o.hash_plain;
            r.number = o.number + 100;
            r.tags_bloom = o.tags_bloom;
            r.tags_plain_bloom = o.tags_plain_bloom;
            r.tags_textual_bloom = o.tags_textual_bloom;
            r.mapping = o.mapping;
            r.parent = o.parent;
            r.next = o.next;
            r.hit_by = o.hit_by;
            r.tags_list = o.tags_list;
            r.tags = o.tags;
            r.tags_plain = o.tags_plain;
            r.tags_textual = o.tags_textual;
            r.tags_numerical = o.tags_numerical;
            r.tags_string = o.tags_string;
            r.tags_string_hash = o.tags_string_hash;
            if (r.next != null) {
                r.next = alloc_reading(r.next);
            }
        }
        return r;
    }

    @Override
    public void clear() {
        mapped = false;
        deleted = false;
        noprint = false;
        matched_target = false;
        matched_tests = false;
        immutable = false;
        active = false;
        baseform = 0;
        hash = 0;
        hash_plain = 0;
        number = 0;
        tags_bloom.clear();
        tags_plain_bloom.clear();
        tags_textual_bloom.clear();
        mapping = null;
        parent = null;
        free_reading(next);
        next = null;
        hit_by.clear();
        tags_list.clear();
        tags.clear();
        tags_plain.clear();
        tags_textual.clear();
        tags_numerical.clear();
        tags_string = EMPTY;
        tags_string_hash = 0;

    }

    static void free_reading(Reading cit) {
        if (cit == null) {
            return;
        }
        pool_readings.put(cit);
        cit.clear();
    }

    public static int getNumber(Reading o) {
        return o.number;
    }

    public int getHash() {
        return hash;
    }

    public int rehash() {
        hash = 0;
        hash_plain = 0;
        for (int iter : tags) {
            if (mapping == null || mapping.hash != iter) {
                hash = hash_value(iter, hash);
            }
        }
        hash_plain = hash;
        if (mapping != null) {
            hash = hash_value(mapping.hash, hash);
        }
        if (next != null) {
            next.rehash();
            hash = hash_value(next.hash, hash);
        }
        return hash;
    }

    public Reading allocateReading(Reading r) {
        return alloc_reading(r);
    }

    public Reading allocateReading(Cohort parent) {
        return alloc_reading(parent);
    }
}
