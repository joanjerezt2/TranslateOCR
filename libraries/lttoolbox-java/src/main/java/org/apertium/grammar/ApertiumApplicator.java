package org.apertium.grammar;

import java.io.PrintStream;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class ApertiumApplicator  extends GrammarApplicator{
    public boolean wordform_case;
    public boolean print_word_forms;
    public boolean print_only_first;
    public boolean delimit_lexical_units;
    public boolean surface_readings;

    public ApertiumApplicator(PrintStream err) {
        super(err);
    }
}
