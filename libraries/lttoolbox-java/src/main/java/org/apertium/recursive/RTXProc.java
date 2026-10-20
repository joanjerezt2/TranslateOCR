package org.apertium.recursive;

import org.apertium.lttoolbox.Getopt;

import java.io.*;
import java.util.Arrays;

import static java.lang.System.exit;
import static org.apertium.utils.IOUtils.openInFileReader;
import static org.apertium.utils.IOUtils.openOutFileWriter;

/**
 * @author Joan Jerez, ToBeIT
 * @serial GPL 3.0
 */

public class RTXProc {

    private static final int EXIT_FAILURE = 1;
    private static final int EXIT_SUCCESS = 0;
    private static final String PACKAGE_VERSION = "v1.2.1";

    private static void showHelp() {
        System.out.print("RTXProc " + PACKAGE_VERSION + ": perform structural transfer\n"
                + "USAGE: " + "rtx-proc" + " [-abefFrstTzh] [-m MODE] bytecode_file [input_file [output_file]]\n"
                + "Options:\n"
                + "  -a:   expect coreference LUs from apertium-anaphora\n"
                + "  -b:   print text (use with -T)\n"
                + "  -e:   print a complete trace of execution\n"
                + "  -f:   trace filterParseGraph()\n"
                + "  -F:   filter branches more often\n"
                + "  -m:   set the mode of tree output, options are 'flat', 'nest', 'latex', 'dot', 'box'\n"
                + "  -r:   print the rules that are being applied\n"
                + "  -s:   print the instructions executed by the stack machine\n"
                + "  -t:   mimic the behavior of apertium-transfer and apertium-interchunk\n"
                + "  -T:   print parse trees rather than apply output rules\n"
                + "  -z    flush output on \\0\n"
                + "  -h:   print this message and exit\n");
    }

    public static void main(String[] argv) throws Exception {
        System.setProperty("file.encoding", "UTF-8");
        doMain(argv);
    }

    private static void doMain(String[] argv) {

        final int argc = argv.length;
        RTXProcessor p = new RTXProcessor();

        Getopt getopt = new Getopt("apertium-recursive", argv, "FTabefmrstzh");
        boolean haveB = false;
        boolean haveT = false;

        while (true) {
            try {
                int c = getopt.getopt();
                if (c == -1) {
                    break;
                }
                switch (c) {
                    case 'a':
                        p.withoutCoref(false);
                        break;
                    case 'b':
                        haveB = true;
                        break;
                    case 'e':
                        p.completeTrace(true);
                        break;
                    case 'f':
                        p.printFilter(true);
                        break;
                    case 'F':
                        p.noFiltering(false);
                        break;
                    case 'm':
                        String optarg = "";
                        if(!p.setOutputMode(optarg))
                        {
                            System.out.print("\"" + optarg + "\" is not a recognized tree mode. Valid options are \"flat\", \"nest\", \"latex\", \"dot\", and \"box\".");
                            exit(EXIT_FAILURE);
                        }
                        break;
                    case 'r':
                        p.printRules(true);
                        break;
                    case 's':
                        p.printSteps(true);
                        break;
                    case 't':
                        p.mimicChunker(true);
                        break;
                    case 'T':
                        haveT = true;
                        break;
                    case 'z':
                        p.setNullFlush(true);
                        break;
                    case 'h':
                        showHelp();
                    default:
                        System.err.println("Unrecognized parameter: " + (char) c);
                        showHelp();
                }

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        p.printTrees(haveT);
        p.printText(haveB || !haveT);

        int optind = getopt.getOptind();
        if (argv.length == 0) {
            showHelp();
        }
        else if(optind > (argc - 1) || optind < (argc - 3))
        {
            showHelp();
        }
        else{
            String path = argv[optind].startsWith("'") ? argv[optind].substring(1, argv[optind].length() - 1) : argv[optind];
            p.read(path);

            // ... DEBUGGED ...

            if (argv.length > optind + 1) {
                try (
                        Reader input = openInFileReader(argv[optind + 1]);
                        Writer output = openOutFileWriter(argv[optind + 2])
                ) {
                    p.process(input, output);
                } catch (Exception e) {
                    System.out.print(Arrays.toString(e.getStackTrace()));
                    System.out.print(e.getMessage());
                    if (optind <= (argv.length - 2)) {
                        System.err.println("Unable to open " + argv[optind + 1] + " for reading.");
                        System.exit(EXIT_FAILURE);
                    }
                    if (optind <= (argv.length - 3)) {
                        System.err.println("Unable to open " + argv[optind + 2] + " for writing.");
                        System.exit(EXIT_FAILURE);
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        System.exit(EXIT_SUCCESS);
    }
}

