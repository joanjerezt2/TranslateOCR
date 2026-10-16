package org.apertium.separable;

import org.apertium.lttoolbox.Getopt;

import java.io.*;
import java.nio.ByteBuffer;

import static org.apertium.utils.IOUtils.*;

/**
 * @author Joan Jerez, ToBeIT
 * @serial GPL 3.0
 */

public class LSXProc {
    private static final int EXIT_FAILURE = 1;
    private static final int EXIT_SUCCESS = 0;
    private static final String PACKAGE_VERSION = "v0.7.1";


    private static void showHelp() {
        System.out.print("LSXProc " + PACKAGE_VERSION + ": re-tokenize a stream\n"
                + "USAGE: " + "lsx-proc" + " [-prwzh] [-m MODE] fst_file [input_file [output_file]]\n"
                + "Options:\n"
                + "  -p:   act as a postgenerator\n"
                + "  -r:   continue applying rules until they match\n"
                + "  -w:   use dictionary case instead of surface case\n"
                + "  -z    flush output on the null character\n"
                + "  -h:   print this message and exit\n");
    }

    public static void main(String[] argv) throws Exception {
        System.setProperty("file.encoding", "UTF-8");
        doMain(argv);
    }

    private static void doMain(String[] argv) throws IOException {

        final int argc = argv.length;
        LSXProcessor p = new LSXProcessor();


        Getopt getopt = new Getopt("apertium-separable", argv, "prwzh");

        while (true) {
            try {
                int c = getopt.getopt();
                if (c == -1) {
                    break;
                }
                switch (c) {
                    case 'p':
                        p.setPostgenMode(true);
                        break;
                    case 'r':
                        p.setRepeatMode(true);
                        break;
                    case 'w':
                        p.setDictionaryCaseMode(true);
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

        int optind = getopt.getOptind() - 1;
        if (argv.length == 0) {
            showHelp();
        } else if (optind > (argc - 1) || optind < (argc - 3)) {
            showHelp();
        } else {
            final String filename = argv[optind];
            ByteBuffer fst = openFileAsByteBuffer(filename);
            try (
                    Reader input = openInFileReader(argv[optind + 1]);
                    Writer output = openOutFileWriter(argv[optind + 2])
            ) {
                p.load(fst);
                p.process(input, output);
                output.close();
                System.exit(EXIT_SUCCESS);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
