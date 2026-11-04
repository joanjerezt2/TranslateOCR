package org.apertium.anaphora;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class PatternArx {

    public static class Unique_LU {
        int id;
        String wordform;
        String tl_wordform;
        String sl_lemma;
        String tl_lemma;
        List<String> pos_tags;
        List<String> properties;

        public Unique_LU(int id, String wordform, String tl_wordform,
                         String sl_lemma, String tl_lemma, List<String> pos_tags, List<String> properties) {
            this.id = id;
            this.wordform = wordform;
            this.tl_wordform = tl_wordform;
            this.sl_lemma = sl_lemma;
            this.tl_lemma = tl_lemma;
            this.pos_tags = pos_tags;
            this.properties = properties;
        }

        public Unique_LU(){
            this.id = 0;
            this.wordform = EMPTY;
            this.tl_wordform = EMPTY;
            this.sl_lemma = EMPTY;
            this.tl_lemma = EMPTY;
            this.pos_tags = new ArrayList<>();
            this.properties = new ArrayList<>();
        }
    }

    static class Antecedent {
        Unique_LU LU;
        int score;

        public Antecedent(Unique_LU finalAntecedentLu, int i) {
            this.LU = finalAntecedentLu;
            this.score = i;
        }
    }

    public static boolean contains_any(List<String> tags, List<String> candidates) {
        // Convertir a Set para búsquedas en tiempo constante O(1)
        Set<String> tagSet = new HashSet<>(tags);
        return candidates.stream().anyMatch(tagSet::contains);
    }

    public static boolean check_acceptable_tags(List<String> input_tags, String input_sl_lemma, List<ParseArx.Item> check_tags) {

        for (ParseArx.Item i : check_tags) {
            boolean todasLasTagsValidas = true;

            for (String j : i.has_tags) {
                if (Objects.equals(j, "*")) {
                    continue; // Ignorar el asterisco
                }

                if (!input_tags.contains(j)) {
                    todasLasTagsValidas = false;
                    break; // Falta una etiqueta obligatoria, salimos del bucle interno
                }
            }

            // 1. Si no contiene todas las tags obligatorias, saltamos al siguiente Item
            if (!todasLasTagsValidas) {
                continue;
            }

            // 2. Si contiene alguna de las excluidas, saltamos al siguiente Item
            if (contains_any(input_tags, i.exclude_tags)) {
                continue;
            }

            // 3. Si tiene lemma y coincide con el de entrada, saltamos al siguiente Item
            if (!i.lemma.isEmpty() && Objects.equals(input_sl_lemma, i.lemma)) {
                continue;
            }

            // Si superó los 3 filtros anteriores con éxito, encontramos una coincidencia pura
            return true;
        }
        return false;
    }

    static Deque<List<PatternArx.Unique_LU>> add_properties(Deque<List<PatternArx.Unique_LU>> context, ParseArx arx_file) {
        Map<String, List<List<ParseArx.MarkablePattern>>> arx_markables = arx_file.get_markables();
        Map<String, List<ParseArx.Item>> arx_cats = arx_file.get_cats();

        for (Map.Entry<String, List<List<ParseArx.MarkablePattern>>> it : arx_markables.entrySet()) {
            //for each markable
            String markable_name = it.getKey();
            List<List<ParseArx.MarkablePattern>> patterns_list = it.getValue();
            for (List<ParseArx.MarkablePattern> patterns : patterns_list) { //go through patterns in the markable
                //for each pattern
                int len_pattern = patterns.size();
                for (List<Unique_LU> m : context) { //go through sentences in the queue of context
                    if (len_pattern > m.size()) { //if pattern is longer then sentence length then skip
                        continue;
                    }
                    for (int n = 0; n + len_pattern - 1 != -1; ++n) //go through LUs in sentence to look for the pattern with a sliding window of size = pattern length
                    {
                        int match_flag = 0;
                        for (int x = 0; x < len_pattern; ++x) {
                            List<ParseArx.Item> pattern_item_tags = arx_cats.get(patterns.get(x).name);
                            if (check_acceptable_tags(m.get(n + x).pos_tags, m.get(n + x).sl_lemma, pattern_item_tags)) //comparing current LU tags to pattern tags and lemma
                            {
                                match_flag = 1;
                            } else {
                                match_flag = 0;
                                break;
                            }
                        }

                        if (match_flag == 1) {
                            //Add Property to the LUs

                            for (int x = 0; x < len_pattern; ++x) {
                                m.get(n + x).properties.addLast(markable_name);
                                if (patterns.get(x).head == 1) {
                                    m.get(n + x).properties.addLast("head");
                                }
                            }
                        }
                    }
                }
            }

        }
        return context;
    }

    public record ParameterReturn(boolean found, String parameterName) {
    }

    static ParameterReturn check_pattern_name(List<String> input_tags, String input_sl_lemma, Map<String,
            List<ParseArx.Item>> parameter_names)
    {
        for (Map.Entry<String, List<ParseArx.Item>> it : parameter_names.entrySet()) {
            if(check_acceptable_tags(input_tags, input_sl_lemma, it.getValue()))
            {
                return new ParameterReturn(true, it.getKey());
            }
        }

        return new ParameterReturn(false, null);
    }
}
