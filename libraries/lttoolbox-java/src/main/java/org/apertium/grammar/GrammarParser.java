package org.apertium.grammar;

import java.io.FileNotFoundException;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public interface GrammarParser {
    boolean parse_grammar(String s) throws FileNotFoundException;
}
