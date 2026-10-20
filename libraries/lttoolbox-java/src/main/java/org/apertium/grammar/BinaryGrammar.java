package org.apertium.grammar;

import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class BinaryGrammar implements GrammarParser {
    public BinaryGrammar(Grammar grammar, PrintStream err) {
    }

    @Override
    public boolean parse_grammar(String s) throws FileNotFoundException {
        return false;
    }
}
