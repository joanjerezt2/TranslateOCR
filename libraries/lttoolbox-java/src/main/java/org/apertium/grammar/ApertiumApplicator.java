package org.apertium.grammar;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import static org.apertium.grammar.Strings.STR_BEGINTAG;
import static org.apertium.grammar.Strings.STR_ENDTAG;

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

    public void runGrammarOnText(InputStream input, PrintStream output) throws IOException {
        if (input == null) {
            System.err.print("Error: Input is null - nothing to parse!\n");
            System.exit(1);
        }
        if (input.available() == 0) {
            System.err.print("Error: Input is empty - nothing to parse!\n");
            System.exit(1);
        }
        if (!output.checkError()) {
            System.err.print("Error: Output is null - cannot write to nothing!\n");
            System.exit(1);
        }

        if (grammar == null) {
            System.err.print("Error: No grammar provided - cannot continue! Hint: call setGrammar() first.\n");
            System.exit(1);
        }

        if (grammar.delimiters == null) {
            if (grammar.soft_delimiters == null) {
                System.err.print("Warning: No soft or hard delimiters defined in grammar. Hard limit of " + hard_limit + " cohorts may break windows in unintended places.\n");
            }
            else {
                System.err.print("Warning: No hard delimiters defined in grammar. Soft limit of " + soft_limit + " cohorts may break windows in unintended places.\n");
            }
        }

        char c = 0;        // Current character
        final boolean in_blank = false;   // Are we in a superblank ?
        final boolean in_wblank = false;  // Are we in a word-bound blank ?
        final boolean in_cohort = false;  // Are we in a cohort ?
        StringBuilder blank = new StringBuilder();           // Blank between tokens, including superblanks
        String wblank = "";
        final StringBuilder token = new StringBuilder();           // Current token (cohort)

        index();

        int resetAfter = ((num_windows + 4) * 2 + 1);

        String wb_start = "[[";
        String wb_end = "]]";

        int begintag = addTag(STR_BEGINTAG).hash; // Beginning of sentence tag
        int endtag = addTag(STR_ENDTAG).hash;     // End of sentence tag

        final SingleWindow cSWindow = null; // Current single window (Cohort frame)
        final Cohort cCohort = null;        // Current cohort

        final SingleWindow lSWindow = null; // Last seen single window
        final Cohort lCohort = null;        // Last seen cohort

        gWindow.window_span = num_windows;

        int variables_set;
        int variables_rem;
        int variables_output;

        // ux_stripBOM(input);

        /* Runnable ensureEndtag = () -> {
            if (lSWindow != null &&
                    !lSWindow.cohorts.isEmpty() &&
                    lSWindow.cohorts.get(lSWindow.cohorts.size() - 1).readings.get(0).tags.contains(endtag) == false) { // C++ count() == 0
                for (Reading iter : lSWindow.cohorts.getLast().readings) {
                    addTagToReading(iter, endtag);
                }
            }
        }; */

    }


}
