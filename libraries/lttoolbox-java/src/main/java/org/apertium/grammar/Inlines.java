package org.apertium.grammar;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

public class Inlines {

    static double NUMERIC_MIN = -(1L << 48L);
    static double NUMERIC_MAX = ((1L<< 48L) - 1);
    static int CG3_HASH_SEED = 705577479;

    static int hash_value(Long l){
        return SuperFastHash(l.toString().toCharArray(), l.toString().length());
    }
    static int hash_value(int c, int h) {
        if (h == 0) {
            h = CG3_HASH_SEED;
        }
        h = c + (h << 6) + (h << 16) - h;
        if (h == 0 || h == Integer.MAX_VALUE || h == Integer.MAX_VALUE - 1) {
            h = CG3_HASH_SEED;
        }
        return h;
    }

    static int hash_value(String s, int hash) {
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

    static int SuperFastHash(char[] data){
        return SuperFastHash(data, 0, CG3_HASH_SEED);
    }

    static int SuperFastHash(char[] data, int length){
        return SuperFastHash(data, length, CG3_HASH_SEED);
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

    public static int SKIPWS(char[] data, int pointer, char a, char b) {
        return SKIPWS(data, pointer, a, b, false);
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

    static boolean ISDELIM(char c){
        return c == '(' || c == ')' || c == '+' || c == '-' || c == '*' || c == '/' || c == '^' || c == '%' || c == '=';
    }

    static boolean ISSPACE(char c) {
        if (c <= 0xFF && c != 0x09 && c != 0x0A && c != 0x0D && c != 0x20 && c != 0xA0) {
            return false;
        }
        return (c == 0x20 || c == 0x09 || c == 0x0A || c == 0x0D || c == 0xA0 || Character.isWhitespace(c));
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

    public static int SKIPTOWS(char[] text, int p, char a, boolean allowhash, boolean allowscol){
        return 0;
    }

    public static int IS_ICASE(char[] input, int p, String delimiters, String delimiters1) {
        // N - 1 due to null terminator for string constants
        if (ISSTRING(Arrays.toString(input), p, delimiters.length()-1)) {
            return 0;
        }
        int i = 0;
        for (; i < delimiters.length()-1; ++i) {
            if (input[i] != delimiters.charAt(i) && input[i] != delimiters1.charAt(i)) {
                return 0;
            }
        }

        if (!Character.isAlphabetic(input[delimiters.length()-1]) && !Character.isDigit(input[delimiters.length()-1])) {
            return i;
        }
        return 0;
    }

    private static boolean ISSTRING(String text, int currentPos, int length) {
        // 1. Validar límites de seguridad para no salirnos de la cadena
        if (currentPos - 1 < 0 || currentPos + length + 1 > text.length()) {
            return false;
        }

        char charBefore = text.charAt(currentPos - 1); // Equivalente a p[-1]
        char charAfter = text.charAt(currentPos + length + 1); // Equivalente a p[c+1]

        if (charBefore == '"' && charAfter == '"') {
            return true;
        }
        return charBefore == '<' && charAfter == '>';
    }

    public static int SKIPTOWS(char[] text, int p, char c) {
        return SKIPTOWS(text, p, c, false, false);
    }

    public static int SKIPTOWS(char[] text, int p, char c, boolean allowhash) {
        return SKIPTOWS(text, p, c, allowhash, false);
    }

    public static int SKIPTOWS(char[] text, int p) {
        return SKIPTOWS(text, p, '0');
    }

    // uextras.hpp

    public static boolean ux_stripBOM(InputStream input) throws IOException {
            PushbackInputStream stream = new PushbackInputStream(input, 3);
            int a = stream.read();
            if (a == -1) { // -1 es el equivalente a std::istream::traits_type::eof()
                return false;
            }
            if (a != 0xEF) {
                stream.unread(a); // Equivalente a putback
                return false;
            }

            int b = stream.read();
            if (b == -1) {
                stream.unread(a);
                return false;
            }
            if (b != 0xBB) {
                stream.unread(b); // En Java se devuelven en orden inverso al que se leyeron
                stream.unread(a);
                return false;
            }

            int c = stream.read();
            if (c == -1) {
                stream.unread(b);
                stream.unread(a);
                return false;
            }
            if (c != 0xBF) {
                stream.unread(c);
                stream.unread(b);
                stream.unread(a);
                return false;
            }

            return true; // Se detectó y descartó el BOM con éxito
    }

    static void insert_if_exists(BitSet cont, BitSet other) {
        // En Java, los objetos se pasan por referencia.
        // Comprobamos que 'other' no sea nulo y que contenga al menos un bit en 'true'.
        if (other != null && !other.isEmpty()) {
            // El méto-do .or() de Java realiza la operación Bitwise OR (cont |= other)
            // y expande automáticamente el tamaño interno de 'cont' si 'other' es más grande.
            cont.or(other);
        }
    }

    public static Reading reverse(Reading head) {
        Reading nr = null;
        while (head != null) {
            Reading next = head.next;
            head.next = nr;
            nr = head;
            head = next;
        }
        return nr;
    }

    static boolean is_textual(String s) {
        return (s.charAt(0) == '"' && s.charAt(s.length()-1) == '"') || (s.charAt(0) == '<' && s.charAt(s.length()-1) == '>');
    }

    static boolean is_not_internal(String s) {
        return (s.charAt(0) != '_' || s.charAt(1) != 'G' || s.charAt(2) != '_');
    }


}

