package org.apertium.anaphora;

/*
 * Copyright (C) 2019 Apertium Project Management Committee <apertium-pmc@dlsi.ua.es>,
 *               2019 Tanmai Khanna <khanna.tanmai@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import org.apache.commons.io.input.ReaderInputStream;
import org.apertium.lttoolbox.Getopt;
import org.apertium.lttoolbox.process.InputFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apertium.utils.IOUtils.openInFileReader;
import static org.apertium.utils.IOUtils.openOutFileWriter;

/**
 * @author Joan Jerez Torres, ToBeIT
 * Port to Java language
 */

public class ApertiumAnaphora {

    private static final int EXIT_FAILURE = 1;
    private static final String PACKAGE_VERSION = "1.1.1";
    private static final int U_EOF = -1;

    private static void showHelp() {

        System.out.print("apertium-anaphora " + PACKAGE_VERSION + "\n"
                + "USAGE: " + "apertium-anaphora" + " arx_file [input [output]]\n"
                + "       " + "apertium-anaphora" + "-z arx_file [input [output]]\n"
                + "  arx_file   Anaphora Resolution rules file (apertium-xxx-yyy.xxx-yyy.arx)"
                + "Options:\n"
                + "  -z:   null-flushing output on \\0\n"
                + "  -h:   show this help\n");
        System.exit(EXIT_FAILURE);
    }

    public static void main(String[] argv) throws Exception {
        System.setProperty("file.encoding", "UTF-8");
        doMain(argv);
    }

    private static void doMain(String[] argv) throws IOException {
        int debug_flag = 0;
        String arxFileName = null;
        int nullFlush = 0;
        int c;

        final int argc = argv.length;
        Getopt getopt = new Getopt("apertium-anaphora", argv, "dzh");

        while (true) {
            try {
                c = getopt.getopt();
                if (c == -1) {
                    break;
                }
                switch (c) {
                    case 'd':
                        debug_flag = 1;
                        break;
                    case 'z':
                        nullFlush = 1;
                        break;
                    case '?':
                        /* getopt_long already printed an error message. */
                        break;
                    case 'h':
                    default:
                        showHelp();
                        break;
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        int optind = getopt.getOptind();
        Reader input = new BufferedReader(new InputStreamReader(System.in));
        Writer output = new BufferedWriter(new OutputStreamWriter(System.out));
        switch(argc - optind)
            {
                case 1: // if only one argument left, it has to be the arx_file
                    arxFileName = argv[argc - 1];
                    break;

                case 2: // if two arguments, it has to be arx_file and input_file
                    arxFileName = argv[argc - 2];
                    try{
                        input = openInFileReader(argv[argc - 1]);
                        break;
                    } catch (UnsupportedEncodingException | FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                case 3: // if three arguments, it has to be arx_file, input file and output file
                    arxFileName = argv[argc - 3];
                    try {
                        input = openInFileReader(argv[argc - 2]);
                        output = openOutFileWriter(argv[argc - 1]);
                        break;
                    } catch (UnsupportedEncodingException | FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                default:
                    showHelp();
                    break;
            }

            byte input_char;
            StringBuilder input_stream = new StringBuilder();

            String final_ref;
            Scoring score_module = new Scoring();
            int gen_id = 0;

            String sl_form;
            String tl_form;
            List<String> sl_tags = new ArrayList<>();
            List<String> tl_tags = new ArrayList<>();
            String sl_lemma;
            String tl_lemma;

            ParseArx arx_file = new ParseArx();
            int parse_arx_retval = arx_file.parseDoc(arxFileName);

            if(parse_arx_retval != 0)
                System.exit(EXIT_FAILURE);

            int flag_LU = 0;

            ReaderInputStream in2 = ReaderInputStream.builder()
                    .setReader(input)
                    .setCharset(StandardCharsets.UTF_8)
                    .get();
            InputStream bufferedIn = new BufferedInputStream(in2);
            InputFile inputf = new InputFile(bufferedIn);

            input_char = inputf.get();

            while(input_char!=U_EOF)
            {
                if(nullFlush == 1 && input_char == '\0')
                {
                    output.write(input_char);
                    output.flush();

                    input_stream = new StringBuilder(EMPTY);
                    sl_tags.clear();
                    tl_tags.clear();
                    gen_id = 0;
                    score_module.clear();

                    flag_LU = 0;
                }

                else if(input_char == '\\')
                {
                    if(flag_LU == 0)
                    {
                        output.write(input_char);
                        input_char = inputf.get();
                        output.write(input_char);
                    }
                    else
                    {
                        input_stream.append(input_char);
                        output.write(input_char);
                        input_char = inputf.get();
                        output.write(input_char);
                        input_stream.append(input_char);
                    }
                }
                else
                {
                    if(flag_LU == 0)
                    {
                        output.write(input_char);

                        if(input_char == '^')
                            flag_LU = 1;
                    }
                    else {
                        if(input_char == '$')
                        {
                            gen_id++;
                            output.write('/'); //for adding ref
                            flag_LU = 0;
                            ParseLexicalUnit LU = new ParseLexicalUnit(input_stream.toString());
                            tl_form = LU.get_tl_form();
                            tl_tags = LU.get_tl_tags();
                            sl_form = LU.get_sl_form();
                            sl_tags = LU.get_sl_tags();
                            sl_lemma = LU.get_sl_lemma();
                            tl_lemma = LU.get_tl_lemma();

                            if(!tl_form.isEmpty())
                            {
                                int retval;
                                retval = score_module.add_word(gen_id, sl_form, sl_tags, tl_form, sl_lemma, tl_lemma, arx_file, debug_flag);
                                //If retval is 0, nothing will be added in side ref
                                //If retval is 1, we call get_antecedent() and add it to ref
                                if(retval == 1)
                                {
                                    final_ref = score_module.get_antecedent(debug_flag);
                                    output.write(final_ref);
                                }
                            }
                            input_stream = new StringBuilder(EMPTY);
                        }
                        else
                        {
                            input_stream.append(input_char);
                        }
                        output.write(input_char);
                    }
                }
                input_char = inputf.get();
            }
            output.close();
    }
}
