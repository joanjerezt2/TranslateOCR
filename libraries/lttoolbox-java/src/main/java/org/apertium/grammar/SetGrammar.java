package org.apertium.grammar;

import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apertium.grammar.Inlines.hash_value;
import static org.apertium.grammar.Strings.cbuffers;
import static org.apertium.grammar.Tag.T_MAPPING;
import static org.apertium.grammar.Tag.T_SPECIAL;
import static org.apertium.grammar.TagTrie.trie_markused;
import static org.apertium.grammar.TagTrie.trie_rehash;

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
    public PrintStream dump_hashes_out;
    public Set<Tag> ff_tags = new TreeSet<>(Comparator.comparingInt(a -> a.hash));
    public int number;

    public int type;
    public static int ST_ANY         = 1;
    public List<Integer> sets;

    static int ST_SPECIAL     = (1 <<  1);
    static int ST_TAG_UNIFY   = (1 <<  2);
    static int ST_SET_UNIFY   = (1 <<  3);
    static int ST_CHILD_UNIFY = (1 <<  4);
    static int ST_MAPPING     = (1 <<  5);
    static int ST_USED        = (1 <<  6);
    static int ST_STATIC      = (1 <<  7);
    static int ST_ORDERED     = (1 <<  8);
    public SortedMap<Tag, TrieNodeT> trie  = new TreeMap<>(Comparator.comparingInt(tag -> tag.hash));
    public SortedMap<Tag, TrieNodeT> trie_special = new TreeMap<>(Comparator.comparingInt(tag -> tag.hash));

    int ST_UNIFY  = ST_TAG_UNIFY | ST_SET_UNIFY | ST_CHILD_UNIFY;

    public int rehash() {
        int retval = 0;

        if ((type & (ST_TAG_UNIFY | ST_SET_UNIFY)) != 0) {
            if ((type & ST_TAG_UNIFY) != 0) {
                retval = hash_value(5153, retval);
            }
            if ((type & ST_SET_UNIFY) != 0) {
                retval = hash_value(5171, retval);
            }

            // Parse and incorporate multi-use identifier, if any
            int u;
            Pattern pattern = Pattern.compile("^&&\\s*(\\d+):\\S+");
            Pattern pattern2 = Pattern.compile("^\\$\\$\\s*(\\d+):\\S+");
            Matcher matcher = pattern.matcher(name);
            Matcher matcher2 = pattern2.matcher(name);
            // lookingAt() verifica que el patrón coincida desde el inicio, como sscanf
            if (matcher.lookingAt() && name.charAt(0) == '&') {
                // Extraemos el grupo 1 (los dígitos de %u)
                String numberStr = matcher.group(1);
                // En Java no hay primitivos unsigned, usa int (parseUnsignedInt) o long
                u = Integer.parseUnsignedInt(numberStr);
                System.out.print("Valor extraído: " + u); // Imprime: 42
                retval = hash_value(u, retval);
            } else {
                System.out.println("El formato no coincide.");
            }

            if (matcher2.lookingAt() && name.charAt(0) == '$') {
                // Extraemos el grupo 1 (los dígitos de %u)
                String numberStr = matcher2.group(1);
                // En Java no hay primitivos unsigned, usa int (parseUnsignedInt) o long
                u = Integer.parseUnsignedInt(numberStr);
                System.out.print("Valor extraído: " + u);
                retval = hash_value(u, retval);
            } else {
                System.out.println("El formato no coincide.");
            }
        }

        if (sets.isEmpty()) {
            retval = hash_value(3499, retval); // Combat hash-collisions
            if (!trie.isEmpty()) {
                retval = hash_value(trie_rehash(trie), retval);
            }
            if (!trie_special.isEmpty()) {
                retval = hash_value(trie_rehash(trie_special), retval);
            }
        }
        else {
            retval = hash_value(2683, retval); // Combat hash-collisions
            for (int i : sets) {
                retval = hash_value(i, retval);
            }
            for (int i : set_ops) {
                retval = hash_value(i, retval);
            }
        }
        hash = retval;

        if (dump_hashes_out != null) {
            if (sets.isEmpty()) {
                dump_hashes_out.printf("DEBUG: Hash %d for set %S (LIST)\n", hash, name);
            }
            else {
                dump_hashes_out.printf( "DEBUG: Hash %d for set %S (SET)\n", hash, name);
            }
        }

        return retval;
    }

    void reindex(Grammar grammar) {
        type &= ~ST_SPECIAL;
        type &= ~ST_CHILD_UNIFY;

        type |= trie_reindex(trie);
        type |= trie_reindex(trie_special);

        for (int s : sets) {
            SetGrammar set = (SetGrammar) grammar.sets_by_contents.get(s);
            set.reindex(grammar);
            if ((set.type & ST_SPECIAL) != 0) {
                type |= ST_SPECIAL;
            }
            if ((set.type & (ST_TAG_UNIFY | ST_SET_UNIFY | ST_CHILD_UNIFY)) != 0) {
                type |= ST_CHILD_UNIFY;
            }
            if ((set.type & ST_MAPPING) != 0) {
                type |= ST_MAPPING;
            }
        }

        if ((type & (ST_TAG_UNIFY | ST_SET_UNIFY | ST_CHILD_UNIFY)) != 0) {
            type |= ST_SPECIAL;
            type |= ST_CHILD_UNIFY;
        }
    }

    private int trie_reindex(SortedMap<Tag, TrieNodeT> trie) {
        int type = 0;
        for (Map.Entry<Tag, TrieNodeT> kv : trie.entrySet()) {
            if ((kv.getKey().type & T_SPECIAL) != 0) {
                type |= ST_SPECIAL;
            }
            if ((kv.getKey().type & T_MAPPING) != 0) {
                type |= ST_MAPPING;
            }
            if (!kv.getValue().trie.isEmpty()) {
                type |= trie_reindex(kv.getValue().trie);
            }
        }
        return type;
    }

    public static class TrieNodeT {
        boolean terminal = false;
        SortedMap<Tag, TrieNodeT> trie = new TreeMap<>(Comparator.comparingInt(tag -> tag.hash));
    }

    final boolean empty(){
        return (ff_tags.isEmpty() && trie.isEmpty() && trie_special.isEmpty() && sets.isEmpty());
    }

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
        type |= ST_USED;

        trie_markused(trie);
        trie_markused(trie_special);

        for (Tag tag : ff_tags) {
            tag.markUsed();
        }

        for (int s : sets) {
            SetGrammar set = (SetGrammar) grammar.sets_by_contents.get(s);
            set.markUsed(grammar);
        }
    }

    SortedMap<Tag, TrieNodeT> getNonEmpty() {
        if (!trie.isEmpty()) {
            return trie;
        }
        return trie_special;
    }


}
