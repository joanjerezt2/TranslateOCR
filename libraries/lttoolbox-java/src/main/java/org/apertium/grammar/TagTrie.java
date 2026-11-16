package org.apertium.grammar;

import java.util.*;

import static org.apertium.grammar.Inlines.hash_value;

public class TagTrie {

    static void trie_getTagList(SortedMap<Tag, SetGrammar.TrieNodeT> trie, List<Tag> theTags) {
        for (Map.Entry<Tag, SetGrammar.TrieNodeT> kv : trie.entrySet()) {
            theTags.addLast(kv.getKey());
            if (!kv.getValue().trie.isEmpty()) {
                trie_getTagList(kv.getValue().trie, theTags);
            }
        }
    }

    static void trie_markused(SortedMap<Tag, SetGrammar.TrieNodeT> trie) {
        for (Map.Entry<Tag, SetGrammar.TrieNodeT> kv : trie.entrySet()) {
            kv.getKey().markUsed();
            if (kv.getValue().trie != null) {
                trie_markused(kv.getValue().trie);
            }
        }
    }

    static boolean trie_insert(SortedMap<Tag, SetGrammar.TrieNodeT> trieSpecial, List<Tag> tv) {
        return trie_insert(trieSpecial, tv, 0);
    }

    static boolean trie_insert(SortedMap<Tag, SetGrammar.TrieNodeT> trieSpecial, List<Tag> tv, int w) {
        SetGrammar.TrieNodeT node = trieSpecial.get(tv.get(w));
        if (node.terminal) {
            return false;
        }
        if (w < tv.size() - 1) {
            if (!node.trie.isEmpty()) {
                node.trie = new TreeMap<>(Comparator.comparingInt(tag -> tag.hash));
                //std::cerr << "new Trie" << std::endl;
            }
            return trie_insert(node.trie, tv, w + 1);
        }
        node.terminal = true;
        node.trie.clear();
        return true;
    }

    static void trie_delete(SortedMap<Tag, SetGrammar.TrieNodeT> trie) {
        for (Map.Entry<Tag, SetGrammar.TrieNodeT> p : trie.entrySet()) {
            if (p.getValue().trie != null) {
                trie_delete(p.getValue().trie);
                p.getValue().trie.clear();
            }
        }
    }

    static int trie_rehash(SortedMap<Tag, SetGrammar.TrieNodeT> trie) {
        int retval = 0;
        for (Map.Entry<Tag, SetGrammar.TrieNodeT> kv : trie.entrySet()) {
            retval = hash_value(kv.getKey().hash, retval);
            if (!kv.getValue().trie.isEmpty()) {
                retval = hash_value(trie_rehash(kv.getValue().trie), retval);
            }
        }
        return retval;
    }

    static boolean trie_singular(SortedMap<Tag, SetGrammar.TrieNodeT> trie) {
        if (trie.size() != 1) {
            return false;
        }
        SetGrammar.TrieNodeT node = trie.entrySet().iterator().next().getValue();
        if (node.terminal) {
            return true;
        }
        if (node.trie.isEmpty()) {
            return trie_singular(node.trie);
        }
        return false;

    }

}
