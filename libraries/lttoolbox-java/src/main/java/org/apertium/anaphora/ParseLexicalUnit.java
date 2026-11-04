package org.apertium.anaphora;

import java.util.ArrayList;
import java.util.List;

public class ParseLexicalUnit {

    /**
     * Source language word and tags
     */
    private final StringBuilder sl_form = new StringBuilder();

    /**
     * Target language word and tags
     */
    private final StringBuilder tl_form = new StringBuilder();

    /**
     * Source language tags
     */
    List<String> sl_tags = new ArrayList<>();

    /**
     * Target language tags
     */
    List<String> tl_tags = new ArrayList<>();

    /**
     * Source language lemma
     */
    StringBuilder sl_lemma = new StringBuilder();

    /**
     * Target language lemma
     */
    StringBuilder tl_lemma = new StringBuilder();

    public ParseLexicalUnit(String input_LU) {
        int seenSlash = 0;
        int seenTag = 0;
        StringBuilder temptag = new StringBuilder();

        for(int i=0; i < input_LU.length(); i++) {
            if (input_LU.charAt(i) == '\\') {
                if (seenSlash == 0) {
                    if (seenTag == 1) {
                        temptag.append(input_LU.charAt(i));
                        sl_form.append(input_LU.charAt(i));
                        ++i;
                        temptag.append(input_LU.charAt(i));
                        sl_form.append(input_LU.charAt(i));
                    } else {
                        sl_form.append(input_LU.charAt(i));
                        sl_lemma.append(input_LU.charAt(i));
                        ++i;
                        sl_form.append(input_LU.charAt(i));
                        sl_lemma.append(input_LU.charAt(i));
                    }
                } else if (seenSlash == 1) //tl (only first entry)
                {
                    if (seenTag == 1) {
                        temptag.append(input_LU.charAt(i));
                        tl_form.append(input_LU.charAt(i));
                        ++i;
                        temptag.append(input_LU.charAt(i));
                        tl_form.append(input_LU.charAt(i));
                    } else {
                        tl_form.append(input_LU.charAt(i));
                        tl_lemma.append(input_LU.charAt(i));
                        ++i;
                        tl_form.append(input_LU.charAt(i));
                        tl_lemma.append(input_LU.charAt(i));
                    }
                } else {
                    break;
                }
            }
            else if (input_LU.charAt(i) == '/') {
                seenSlash++;
            }
            else if (seenSlash == 0) {
                sl_form.append(input_LU.charAt(i));

                if (input_LU.charAt(i) == '<') {
                    seenTag++;
                } else if (seenTag == 1) {
                    if (input_LU.charAt(i) == '>') {
                        seenTag--;
                        sl_tags.addLast(String.valueOf(temptag));

                        temptag.setLength(0);
                    } else {
                        temptag.append(input_LU.charAt(i));
                    }
                } else {
                    sl_lemma.append(input_LU.charAt(i));
                }
            }
            else
            {
                break;
            }
        }
    }

    public String get_tl_form() {
        return tl_form.toString();
    }

    public List<String> get_tl_tags() {
        return tl_tags;

    }

    public String get_sl_form() {
        return sl_form.toString();
    }

    public List<String> get_sl_tags() {
        return sl_tags;
    }

    public String get_sl_lemma() {
        return sl_lemma.toString();
    }

    public String get_tl_lemma() {
        return tl_lemma.toString();
    }
}
