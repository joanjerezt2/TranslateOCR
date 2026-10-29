package org.apertium.grammar;

import java.util.Random;

public class Inlines {

    static double NUMERIC_MIN = -(1L << 48L);
    static double NUMERIC_MAX = ((1L<< 48L) - 1);
    static int CG3_HASH_SEED = 705577479;

    public static int hash_value(String s, int hash) {
        return hash_value(s.toCharArray(), hash, 0);
    }

    public static int hash_value(String s) {
        return hash_value(s, 0);
    }

    public static int hash_value(char[] str, int hash, int len) {
        if (hash == 0) {
            hash = new Random().nextInt();
        }
        if (len == 0) {
            len = str.length;
        }
        return Inlines.SuperFastHash(str, len, hash);
    }

    static int SuperFastHash(char[] data, int len, int hash) {
        if (hash == 0) {
            hash = len;
        }
        int tmp;
        int rem;

        if (len == 0 || data == null) {
            return 0;
        }

        rem = len & 1;
        len >>= 1;

        /* Main loop */
        for (; len > 0; len--) {
            hash += data[0];
            tmp = (data[1] << 11) ^ hash;
            hash = (hash << 16) ^ tmp;
            // TODO data += 2;
            hash += hash >> 11;
        }

        /* Handle end cases */
        if (rem == 1) {
            hash += data[0];
            hash ^= hash << 11;
            hash += hash >> 17;
        }

        /* Force "avalanching" of final 127 bits */
        hash ^= hash << 3;
        hash += hash >> 5;
        hash ^= hash << 4;
        hash += hash >> 17;
        hash ^= hash << 25;
        hash += hash >> 6;

        if (hash == 0 || hash == Integer.MAX_VALUE || hash == Integer.MAX_VALUE - 1) {
            hash = new Random().nextInt();
        }

        return hash;
    }

    public static int SKIPWS(char[] data, int pointer) {
        return SKIPWS(data, pointer, '0', '0', false);
    }

    public static int SKIPWS(char[] data, int pointer, char a) {
        return SKIPWS(data, pointer, a, '0', false);
    }

    public static int SKIPWS(char[] text, int p, char a, char b, boolean allowhash) {
        int s = 0;
        while (text[p] != a && text[p] != b) {
            if (ISNL(text[p])) {
                ++s;
            }
            if (!allowhash && p == '#' && !ISESC(text[p])) {
                s += SKIPLN(text, p);
                p--;
            }
            if (!ISSPACE(text[p])) {
                break;
            }
            ++p;
        }
        return s;
    }

    private static boolean ISSPACE(char c) {
        return false;
    }

    private static int SKIPLN(char[] text, int p) {
        return 0;
    }

    public static int SKIPTO(char[] p, int pOffset, char a) {
        int s = 0;
        // Mientras el índice no supere el tamaño del array (equivalente a *p != '\0')
        // y se cumpla la condición de parada
        while (pOffset < p.length && (p[pOffset] != a || ISESC(p[pOffset]))) {
            if (ISNL(p[pOffset])) {
                ++s;
            }
            // ++p en C++: Modifica el "puntero" sumando 1 al índice
            pOffset++;
        }
        return s;
    }

    private static boolean ISNL(char c) {
        return (
                c == 0x2028L    // Unicode Line Seperator
                || c == 0x2029L // Unicode Paragraph Seperator
                || c == 0x000CL // Form Feed
                || c == 0x000BL // Vertical Tab
                || c == 0x000AL // ASCII \n
        );
    }

    private static boolean ISESC(char p) {
        int a = 1;
        while ((p- a) == '\\') {
            ++a;
        }
        return (a % 2 == 0);
    }

    public static boolean is_cg3b(byte[] s)
    {
        return (s[0] == 'C' && s[1] == 'G' && s[2] == '3' && s[3] == 'B');
    }
}
