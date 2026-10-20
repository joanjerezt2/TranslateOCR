package org.apertium.grammar;

import org.apertium.lttoolbox.Getopt;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.apertium.utils.IOUtils.*;

/*
 * Copyright (C) 2007-2026, GrammarSoft ApS
 * Developed by Tino Didriksen <mail@tinodidriksen.com>
 * Design by Eckhard Bick <eckhard.bick@mail.dk>, Tino Didriksen <mail@tinodidriksen.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this progam.	If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * @author Joan Jerez Torres, ToBeIT
 * Port to Java language
 */

public class CGProc {

    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_FAILURE = 1;
    private static final String PACKAGE_VERSION = "1.5.1";

    private static void showHelp() {

        System.out.print("CGProc " + PACKAGE_VERSION + ": process a stream with a constraint grammar\n"
                + "USAGE: " + "cg-proc" + " [-t] [-s] [-d] [-g] [-r rule] grammar_file [input_file [output_file]]\n"
                + "Options:\n"
                + "  -d:   morphological disambiguation (default behaviour)\n"
                + "  -s:   specify number of sections to process\n"
                + "  -f:   set the format of the I/O stream to NUM,\n"
                + "\t\t   where `0' is VISL format, `1' is \n"
                + "\t\t   Apertium format and `2' is Matxin (default: 1)\n"
                + "  -r:   run only the named rule\n"
                + "  -t:   print debug output on stderr\n"
                + "  -w:   enforce surface case on lemma/baseform \n"
                + "\t\t   (to work with -w option of lt-proc)\n"
                + "  -n:   do not print out the word form of each cohort\n"
                + "  -g:   do not surround lexical units in ^$\n"
                + "  -1:   only output the first analysis if ambiguity remains\n"
                + "  -z:   flush output on the null character\n"
                + "  -v:   version\n"
                + "  -h:   show this help\n");
    }

    public static void main(String[] argv) throws Exception {
        System.setProperty("file.encoding", "UTF-8");
        doMain(argv);
    }

    private static void doMain(String[] argv) throws IOException {

        boolean trace = false;
        boolean wordform_case = false;
        boolean print_word_forms = true;
        boolean delimit_lexical_units = true;
        boolean surface_readings = false;
        boolean only_first = false;
        int cmd = 0;
        int sections = 0;
        int stream_format = 1;
        String single_rule = "";

        final int argc = argv.length;

        Getopt getopt = new Getopt("cg-proc", argv, "ds:f:tr:ing1wvhz");
        String optarg = "";

        while (true) {
            try {
                int c = getopt.getopt();
                if (c == -1) {
                    break;
                }
                switch (c) {
                    case 'd':
                        if (cmd == 0) {
                            cmd = c;
                        } else {
                            showHelp();
                        }
                        break;

                    case 'f':
                        optarg = argv[1];
                        stream_format = Integer.parseInt(optarg);
                        break;

                    case 't':
                        trace = true;
                        break;

                    case 'r': {
                        // strdup() is Posix
                        int len = optarg.length() + 1;
                        single_rule = optarg.substring(0, len);
                        break;
                    }
                    case 's':
                        sections = Integer.parseInt(optarg);
                        break;

                    case 'n':
                        print_word_forms = false;
                        break;

                    case 'g':
                        delimit_lexical_units = false;
                        surface_readings = true;
                        break;

                    case '1':
                        only_first = true;
                        break;

                    case 'w':
                        wordform_case = true;
                        break;

                    case 'v':
                        System.out.print("\"VISL CG-3 Disambiguator version " + PACKAGE_VERSION);
                        System.exit(EXIT_SUCCESS);
                        break;
                    case 'z':
                        // Null-flush is default
                        break;
                    case 'h':
                    default:
                        showHelp();
                        break;

                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }

            Grammar grammar = new Grammar();
            int optind = getopt.getOptind();
            List<byte[]> cbuffers = new ArrayList<>();
            if (optind <= (argc - 1)) {
                ByteBuffer input;
                try {
                    input = openFileAsByteBuffer(argv[optind]);
                    input.mark();
                    byte[] header = new byte[4];
                    try {
                        input.get(header);
                        cbuffers.add(header);
                    } catch (Exception e) {
                        System.err.println("Error: Error reading first 4 bytes from grammar! " + e.getMessage());
                        System.exit(EXIT_FAILURE);
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    showHelp();
                }
            } else {
                showHelp();
            }
            Reader input;
            Writer output;
            if (optind <= (argc - 2)) {
                input = getStdinReader();
            }
            if (optind <= (argc - 3)) {
                output = getStdoutWriter();
            }

            GrammarParser parser;
            if (is_cg3b(cbuffers.getFirst())) {
                parser = new BinaryGrammar(grammar, System.err);
            } else {
                // Forbidding text grammars makes debugging very annoying
                System.err.println("Warning: Text grammar detected - to better process textual " +
                        "grammars, use `vislcg3'; to compile this grammar, use `cg-comp'");
                parser = new TextualParser(grammar, System.err);
            }

            grammar.err = System.err;

            if (!parser.parse_grammar(argv[optind])) {
                System.err.print("Error: Grammar could not be parsed - exiting!");
                System.exit(EXIT_FAILURE);
            }

            grammar.reindex();

            GrammarApplicator applicator = new GrammarApplicator(System.err);
            if (stream_format == 1) {
                ApertiumApplicator apertiumApplicator = new ApertiumApplicator(System.err);
                apertiumApplicator.wordform_case = wordform_case;
                apertiumApplicator.print_word_forms = print_word_forms;
                apertiumApplicator.print_only_first = only_first;
                apertiumApplicator.delimit_lexical_units = delimit_lexical_units;
                apertiumApplicator.surface_readings = surface_readings;
                applicator = apertiumApplicator;
            } else {
                System.err.print("VISL and Matxin format is not supported");
                System.exit(EXIT_FAILURE);
            }

            applicator.setGrammar(grammar);
            applicator.setOptions();
            for (int i = 1; i <= sections; ++i) {
                applicator.sections.addLast(i);
            }

            applicator.trace = trace;
            applicator.unicode_tags = true;
            applicator.unique_tags = false;

            // This is if we want to run a single rule  (-r option)
            if (!single_rule.isEmpty()) {
                for (Rule rule : applicator.grammar.rule_by_number) {
                    if (Objects.equals(rule.name, single_rule)) {
                        applicator.valid_rules.addLast(rule.number);
                    }
                }
            }

            try {
                applicator.runGrammarOnText(System.in, System.out);
            } catch (Exception e) {
                System.err.print(e.getLocalizedMessage());
                System.exit(1);
            }
        }
    }

    private static boolean is_cg3b(byte[] s) {
            return (s[0] == 'C' && s[1] == 'G' && s[2] == '3' && s[3] == 'B');
        }
}

