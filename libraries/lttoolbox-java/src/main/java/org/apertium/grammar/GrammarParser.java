package org.apertium.grammar;

import java.io.FileNotFoundException;
import java.util.regex.Pattern;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public interface GrammarParser {
    Grammar result = new Grammar();
    int verbosity = 0;
    Pattern nrules = null;
    Pattern nrules_inv = null;

    boolean parse_grammar(char[] filename);

    boolean parse_grammar(String s) throws FileNotFoundException;
}
