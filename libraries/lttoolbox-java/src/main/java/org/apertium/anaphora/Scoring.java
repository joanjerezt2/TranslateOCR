package org.apertium.anaphora;

import java.util.*;

import static org.apertium.anaphora.ParseArx.print_tags;
import static org.apertium.anaphora.PatternArx.*;

public class Scoring {

    Deque<List<PatternArx.Unique_LU>> context = new ArrayDeque<>();
    List<PatternArx.Antecedent> antecedent_list;

    public String get_antecedent(int debug_flag) {
        PatternArx.Unique_LU final_antecedent_LU = new PatternArx.Unique_LU();
        PatternArx.Antecedent final_antecedent = new PatternArx.Antecedent(final_antecedent_LU, -5);

        if(debug_flag == 1)
        {
            System.err.print("\n** Final Scores **\n");
        }

        for (PatternArx.Antecedent it : antecedent_list) { //read from furthest to nearest
            if(debug_flag == 1)
            {
                System.err.print("\n" + it.LU.id + ": ");
                System.err.print(it.LU.wordform);
                System.err.print(" : " + it.score + "\n");
            }

            if(it.score >= final_antecedent.score)
                final_antecedent = it;
        }

        antecedent_list.clear();

        if(debug_flag == 1)
        {
            System.err.print("\n" + "** Final Antecedent: ");
            System.err.print(final_antecedent.LU.wordform);
            System.err.print("/");
            System.err.print(final_antecedent.LU.tl_wordform);
            System.err.print(" **\n");
        }
        return final_antecedent.LU.tl_wordform;
    }

    public void clear() {
        context.clear();
        antecedent_list.clear();
    }

    public int check_agreement(List<String> antecedent_tags, List<String> anaphor_tags){
        return 1; //for now, return 1 always
    }

    public void apply_indicators(PatternArx.Unique_LU anaphor, ParseArx arx_file, String parameter_name, int debug_flag){
        int distance_marker = 2; //starts from 2 for current sentence and reduces till -1 as we go to previous sentences
        int temp_score;
        int firstNP;

        antecedent_list.clear();

        Deque<List<PatternArx.Unique_LU> > context_with_prop = add_properties(context, arx_file);

        distance_marker = distance_marker - context_with_prop.size() + 1; //set distance to earliest sentence based on number of sentences in context

        Map<String, Integer> all_markables_score = arx_file.get_all_markables_score();
        Map<String, Integer> parameter_markables_score = arx_file.get_parameter_markables_score(parameter_name);

        if(debug_flag == 1)
        {
            System.err.print("\n** For anaphor: ");
            System.err.print(anaphor.wordform);
            System.err.print("/");
            System.err.print(anaphor.tl_wordform);
            System.err.print(", Context - with markables **\n");
        }

        //Start going through sentences(earliest to current) and apply all indicators to modify scores of the NPs

        for (List<PatternArx.Unique_LU> i : context_with_prop) {
            firstNP = 1;

            for (PatternArx.Unique_LU antecedent_LU : i) {
                if(debug_flag == 1)
                {
                    System.err.print("\n");
                    System.err.print(antecedent_LU.wordform);
                    System.err.print(": ");
                    print_tags(antecedent_LU.properties);
                    System.err.print("\n");
                }

                if(check_acceptable_tags(antecedent_LU.pos_tags, antecedent_LU.sl_lemma, arx_file.getParameters().get("antecedent").get("parameter_name")))
                {
                    temp_score = 0;

                    if(check_agreement(antecedent_LU.pos_tags, anaphor.pos_tags) == 1)
                    {
                        //Add or Remove Indicators Here
                        temp_score += distance_marker; //Referential Distance (based on how close the antecedent is to the pronoun)

                        //Boosting Indicators
                        if(firstNP == 1)
                        {
                            temp_score += 1;
                            firstNP = 0;
                        }
                        //Impeding Indicators

                        //Indicators from XML file (iterate through all markables that provided a score without mentioning parameter_name)
                        for (Map.Entry<String, Integer> x : all_markables_score.entrySet()) {
                            if(antecedent_LU.properties.contains(x.getKey()))
                            {
                                temp_score += x.getValue();
                            }
                        }
                        //Now get the scores from the markables that mentioned this specific parameter name
                        for (Map.Entry<String, Integer> x : parameter_markables_score.entrySet()) {
                            if(antecedent_LU.properties.contains(x.getKey()))
                            {
                                temp_score += x.getValue();
                            }
                        }
                        PatternArx.Antecedent antecedent_with_score = new PatternArx.Antecedent(antecedent_LU, temp_score);
                        antecedent_list.addLast(antecedent_with_score);
                    }
                }
            }
            if(distance_marker < 2)
                distance_marker++;
        }
    }



    public int add_word(int input_id, String input_wordform, List<String> input_pos_tags, String input_tl_wordform,
                        String input_sl_lemma, String input_tl_lemma,
                        ParseArx arxFile, int debugFlag) {
        List<String> temp_prop = new ArrayList<>();
        Map<String, Map<String, List<ParseArx.Item>>> arx_parameters = arxFile.getParameters();

        PatternArx.Unique_LU anaphor_LU = new PatternArx.Unique_LU(input_id, input_wordform, input_tl_wordform,
                input_sl_lemma, input_tl_lemma, input_pos_tags, temp_prop);

        if(context.isEmpty())
        {
            List<PatternArx.Unique_LU> sentence = new ArrayList<>();
            sentence.addLast(anaphor_LU);

            context.addLast(sentence);

            if(check_acceptable_tags(anaphor_LU.pos_tags, anaphor_LU.sl_lemma, arx_parameters.get("delimiter").get("default")))
            {
                List<PatternArx.Unique_LU> new_sentence = new ArrayList<>();
                context.addLast(new_sentence);
            }
        }
        else
        {
            if(check_acceptable_tags(anaphor_LU.pos_tags, anaphor_LU.sl_lemma, arx_parameters.get("delimiter").get("default")))
            {
                context.getLast().addLast(anaphor_LU);
                List<PatternArx.Unique_LU> new_sentence = new  ArrayList<>();
                context.addLast(new_sentence);
                if(context.size() > 4)
                    context.removeFirst();
                }
		        else {
                    ParameterReturn retval = check_pattern_name(anaphor_LU.pos_tags, anaphor_LU.sl_lemma,
                            arx_parameters.get("anaphor"));

                if(retval.found()) //check if tags,lemma of current word match with anaphor in arx file
                {
                    List<String> temp_pos_tags = anaphor_LU.pos_tags;
                    temp_pos_tags.addLast("anaphor");
                    anaphor_LU.pos_tags.addAll(temp_pos_tags);
                    context.getLast().addLast(anaphor_LU);
                    apply_indicators(anaphor_LU, arxFile, retval.parameterName(), debugFlag);
                    context.getLast().removeLast();
                    context.getLast().addLast(anaphor_LU);

                    return 1; //To show that something will be added in side ref
                }
                else
                {
                    context.getLast().addLast(anaphor_LU); //add word to the latest added sentence in the queue
                }
            }
        }
        return 0;
    }
}
