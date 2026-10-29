package org.apertium.grammar;

import javax.swing.text.html.parser.ContentModel;
import java.util.*;

import static org.apertium.grammar.Strings.cbuffers;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class SetGrammar {


    public int line;
    public String name;
    public List<Integer> set_ops;
    public Integer hash;
    public SetGrammar dump_hashes_out;
    public ContentModel ff_tags;
    public int number;

    public void setName(int to){
        if (to == 0) {
            to = new Random().nextInt();
        }
        cbuffers.addFirst(("_G_"+line+"_"+to+"_").toCharArray());
        name = Arrays.toString(Strings.cbuffers.getFirst());
    }

    public void setName(){
        setName(0);
    }

    public void setName(String to) {
        if (!to.isEmpty()) {
            name = to;
        }
        else {
            setName();
        }
    }

    public void markUsed(Grammar grammar) {

    }

    public static class TrieNodeT {
        boolean terminal = false;
        SortedMap<Tag, TrieNodeT> trie = new TreeMap<>(Comparator.comparingInt(tag -> tag.hash));
    }


    public int type;

    public int ST_ANY         = 1;
    public List<Integer> sets;

    static int ST_SPECIAL     = (1 <<  1);
    static int ST_TAG_UNIFY   = (1 <<  2);
    static int ST_SET_UNIFY   = (1 <<  3);
    static int ST_CHILD_UNIFY = (1 <<  4);
    static int ST_MAPPING     = (1 <<  5);
    static int ST_USED        = (1 <<  6);
    static int ST_STATIC      = (1 <<  7);
    static int ST_ORDERED     = (1 <<  8);
    public SortedMap<Tag, TrieNodeT> trie  = new TreeMap<>(Comparator.comparingInt(tag -> tag.hash));;
    public SortedMap<Tag, TrieNodeT> trie_special = new TreeMap<>(Comparator.comparingInt(tag -> tag.hash));;

    int ST_UNIFY  = ST_TAG_UNIFY | ST_SET_UNIFY | ST_CHILD_UNIFY;
}
