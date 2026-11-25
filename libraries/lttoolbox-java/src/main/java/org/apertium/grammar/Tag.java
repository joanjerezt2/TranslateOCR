package org.apertium.grammar;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apertium.grammar.Inlines.*;
import static org.apertium.grammar.Tag.C_OPS.*;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class Tag {
    
    final long UINT32_MAX = 4294967295L;

    void parseNumeric() {
        parseNumeric(false);
    }

    public PrintStream dump_hashes_out = null;

    enum C_OPS {
        OP_NOP,
        OP_EQUALS,
        OP_LESSTHAN,
        OP_GREATERTHAN,
        OP_LESSEQUALS,
        OP_GREATEREQUALS,
        OP_NOTEQUALS,
        NUM_OPS,
    }
    
    static int T_ANY              = 1;
    static int T_NUMERICAL        = (1 <<  1);
    static int T_MAPPING          = (1 <<  2);
    static int T_VARIABLE         = (1 <<  3);
    static int T_META             = (1 <<  4);
    static int T_WORDFORM         = (1 <<  5);
    static int T_BASEFORM         = (1 <<  6);
    static int T_TEXTUAL          = (1 <<  7);
    static int T_DEPENDENCY       = (1 <<  8);
    static int T_SAME_BASIC       = (1 <<  9);
    static int T_FAILFAST         = (1 << 10);
    static int T_CASE_INSENSITIVE = (1 << 11);
    static int T_REGEXP           = (1 << 12);
    static int T_PAR_LEFT         = (1 << 13);
    static int T_PAR_RIGHT        = (1 << 14);
    static int T_REGEXP_ANY       = (1 << 15);
    static int T_VARSTRING        = (1 << 16);
    static int T_TARGET           = (1 << 17);
    static int T_MARK             = (1 << 18);
    static int T_ATTACHTO         = (1 << 19);
    static int T_SPECIAL          = (1 << 20);
    static int T_USED             = (1 << 21);
    static int T_LOCAL_VARIABLE   = (1 << 22);
    static int T_SET              = (1 << 23);
    static int T_VSTR             = (1 << 24);
    static int T_ENCL             = (1 << 25);
    static int T_RELATION         = (1 << 26);
    static int T_CONTEXT          = (1 << 27);
    static int T_NUMERIC_MATH     = (1 << 28);
    static int T_PRESERVE_ESC     = (1 << 29);
    static int T_REGEXP_LINE      = (1 << 31); // ToDo: Remove for real ordered mode
    static int MASK_TAG_SPECIAL   = T_ANY | T_TARGET | T_MARK | T_ATTACHTO | T_PAR_LEFT | T_PAR_RIGHT | T_NUMERICAL |
            T_VARIABLE | T_LOCAL_VARIABLE | T_META | T_FAILFAST | T_CASE_INSENSITIVE | T_REGEXP | T_REGEXP_ANY |
            T_VARSTRING | T_SET | T_ENCL | T_SAME_BASIC | T_CONTEXT;

    C_OPS comparison_op = C_OPS.OP_NOP;
    double comparison_val = 0;
    int type = 0;
    int comparison_hash = 0;
    int dep_self = 0;

    long dep_parent = 0;
    int variable_hash = 0;
    int context_ref_pos = 0;
    int comparison_offset = 0;

    int hash = 0;
    int plain_hash = 0;
    int number = 0;
    int seed = 0;
    StringBuilder tag;
    String tag_raw = "";
    List<SetGrammar> vs_sets;
    List<String> vs_names = new ArrayList<>();
    Pattern regexp = null;

    void parseTagRaw(char[] to, Grammar grammar) {
        type = 0;
	    int length = to.length;
        if(length == 0) {
            System.out.print("parseTagRaw() will not work with empty strings.");
        }

        if (to[0] == '"' || to[0] == '<') {
            if ((to[0] == '"' && to[length - 1] == '"') || (to[0] == '<' && to[length - 1] == '>')) {
                type |= T_TEXTUAL;
                if (to[0] == '"' && to[length - 1] == '"') {
                    if (to[1] == '<' && to[length - 2] == '>' && length > 4) {
                        type |= T_WORDFORM;
                    }
                    else {
                        type |= T_BASEFORM;
                    }
                }
            }
        }

        tag = to;

        /* for (Pattern iter : grammar.regex_tags) {
            UErrorCode status = U_ZERO_ERROR;
            uregex_setText(iter, tag.data(), SI32(tag.size()), &status);
            if (status == U_ZERO_ERROR) {
                if (uregex_find(iter, -1, &status)) {
                    type |= T_TEXTUAL;
                }
            }
        } */
        for (Tag iter : grammar.icase_tags) {
            if (Arrays.equals(tag, iter.tag)) {
                type |= T_TEXTUAL;
            }
        }

        if (tag[0] == '<' && tag[length - 1] == '>') {
            parseNumeric(false);
        }
        if (tag[0] == '#') {
            // C++ if (u_sscanf(tag.data(), "#%i->%i", &dep_self, &dep_parent) == 2 && dep_self != 0) {
            Matcher m1 = Pattern.compile("^#(-?\\d+)->(-?\\d+)").matcher(Arrays.toString(tag));
            if (m1.find()) {
                this.dep_self = Integer.parseInt(m1.group(1));
                this.dep_parent = Integer.parseInt(m1.group(2));
                if (this.dep_self != 0) {
                    // Condición 1 cumplida. Tu lógica aquí.
                    type |= T_DEPENDENCY;
                }
            }
            char[] local_dep_unicode = { '#', '%', 'i', '→', '%', 'i', 0 };
            // --------------------------------------------------------------------------------
            // LINEA 2: u_sscanf_u(tag.data(), local_dep_unicode, &dep_self, &dep_parent) == 2 && dep_self != 0
            // --------------------------------------------------------------------------------
            // Convertimos el formato estilo C (ej: "%i->%i") a un patrón Regex adecuado ("(-?\\d+)->(-?\\d+)")
            String regexFromFormat = Arrays.toString(local_dep_unicode).replace("%i", "(-?\\d+)");
            Matcher m2 = Pattern.compile("^" + regexFromFormat).matcher(Arrays.toString(tag));
            if (m2.find() && m2.groupCount() >= 2) {
                this.dep_self = Integer.parseInt(m2.group(1));
                this.dep_parent = Integer.parseInt(m2.group(2));
                if (this.dep_self != 0) {
                    // Condición 2 cumplida. Tu lógica aquí.
                    type |= T_DEPENDENCY;
                }
            }
        }
        if (tag[0] == 'I' && tag[1] == 'D' && tag[2] == ':' && Character.isDigit(tag[3])) {
            // --------------------------------------------------------------------------------
            // LINEA 3: u_sscanf(tag.data(), "ID:%i", &dep_self) == 1 && dep_self != 0
            // --------------------------------------------------------------------------------
            Matcher m3 = Pattern.compile("^ID:(-?\\d+)").matcher(Arrays.toString(tag));
            if (m3.find()) {
                this.dep_self  = Integer.parseInt(m3.group(1));
                if (this.dep_self  != 0) {
                    // Condición 3 cumplida. Tu lógica aquí.
                    type |= T_RELATION;
                }
            }
        }
        if (tag[0] == 'R' && tag[1] == ':') {
            String relname;
            dep_parent = UINT32_MAX;
            // --------------------------------------------------------------------------------
            // LINEA 4: u_sscanf(tag.data(), "R:%[^:]:%i", &relname, &dep_parent) == 2 && dep_parent != uint32_t::max()
            // --------------------------------------------------------------------------------
            Matcher m4 = Pattern.compile("^R:([^:]+):(-?\\d+)").matcher(Arrays.toString(tag));
            if (m4.find()) {
                relname = m4.group(1); // El grupo ([^:]+) captura to-do hasta encontrar un ':'
                // Usamos Long para poder evaluar el valor de un uint32_t sin problemas de signo en Java
                long parentValue = Long.parseLong(m4.group(2));
                if (parentValue != UINT32_MAX) {
                    this.dep_parent = parentValue;
                    // Condición 4 cumplida. Tu lógica aquí.
                    type |= T_RELATION;
                    Tag reltag = grammar.allocateTag(relname);
                    comparison_hash = reltag.hash;
                }
            }
        }
        type &= ~T_SPECIAL;
        if ((type & T_NUMERICAL) != 0) {
            type |= T_SPECIAL;
        }
    }

    public int rehash() {
        hash = 0;
        plain_hash = 0;

        if (type == T_FAILFAST) {
            hash = hash_value("^", hash);
        }

        if (type == T_META) {
            hash = hash_value("META:", hash);
        }
        if (type == T_VARIABLE) {
            hash = hash_value("VAR:", hash);
        }
        if (type == T_LOCAL_VARIABLE) {
            hash = hash_value("LVAR:", hash);
        }
        if (type == T_SET) {
            hash = hash_value("SET:", hash);
        }

        plain_hash = hash_value(Arrays.toString(tag));
        if (hash != 0) {
            hash = hash_value(String.valueOf(plain_hash), hash);
        }
        else {
            hash = plain_hash;
        }

        if (type == T_CASE_INSENSITIVE) {
            hash = hash_value("i", hash);
        }
        if (type == T_REGEXP) {
            hash = hash_value("r", hash);
        }
        if (type == T_VARSTRING) {
            hash = hash_value("v", hash);
        }

        hash += seed;

        type &= ~T_SPECIAL;
        if (type == MASK_TAG_SPECIAL) {
            type |= T_SPECIAL;
        }

        if (dump_hashes_out != null) {
            System.out.printf("DEBUG: Hash %d with seed %d for tag %S\n", hash, seed, tag);
            System.out.printf("DEBUG: Plain hash %d with seed %d for tag %S\n", plain_hash, seed, tag);
        }

        return hash;
    }

    void markUsed() {
        type |= T_USED;
    }

    void allocateVsSets() {
        if (vs_sets == null) {
            vs_sets = new ArrayList<>();
        }
    }

    void allocateVsNames() {
        if (vs_names == null) {
            vs_names = new ArrayList<>();
        }

    }

    void parseNumeric(boolean trusted) {
        /* 1. u_sscanf -> Pattern / Matcher: El patrón %*[<]%[^<>=:!]%[<>=:!] ignora un < opcional si existe al principio, captura to-
        do lo que no sea un operador de comparación en el primer grupo (tkey), y captura los operadores de comparación en el segundo grupo (top).
        2. std::copy y txval -> tag.substring(...): En C++, tenían que crear un buffer estático de UChar txval[256] y
        rellenarlo manualmente controlando el terminador nulo \0. En Java, String.substring() genera automáticamente una subcadena segura
        y dinámica sin peligro de desbordamiento de búfer (buffer overflow).
        3. u_strspn -> Bucle indexOf: Java no tiene un equivalente directo a strspn en su API estándar, pero se emula
        de manera muy eficiente recorriendo la cadena mientras los caracteres coincidan con tu lista de permitidos (-.0123456789).
        4. find_first_of -> .matches(".*[...]...*"): Las expresiones regulares nativas de Java determinan si existen
        los operadores matemáticos o si se está colando algún carácter inválido (como comillas o llaves) en la
        expresión matemática de la etiqueta.
        */

        if (tag.length >= 256) {
            return;
        }
        char[] tkey = new char[256];
        char[] top = new char[256];
        tkey[0] = 0;
        top[0] = 0;
        Pattern p = Pattern.compile("^<?([^<>=:!]+)([<>=:!]+)");
        Matcher matcher = p.matcher(Arrays.toString(tag));
        // sscanf == 2 && top[0] se cumple si la regex encuentra ambos grupos
        if (!matcher.find()) {
            return;
        }
        tkey = matcher.group(1).toCharArray();
        top = matcher.group(2).toCharArray();

        int tkz = tkey.length;
        int toz = top.length;

        // if (UIZ(tkz + toz + 1) >= tag.size())
        if ((tkz + toz + 1) >= tag.length) {
            return;
        }

        // Extrae 'txval': desde después de tkey y top (+1 por el '<' inicial opcional u omitido)
        // hasta el penúltimo carácter (tag.length() - 1 elimina el '>' de cierre habitual de las etiquetas CG3)
        // En Java, substring(inicio, fin) no incluye el índice de fin.
        String txval = Arrays.toString(tag).substring(tkz + toz + 1, tag.length - 1);

        if (txval.isEmpty()) {
            return;
        }

        double tval = 0;
        // u_strspn(txval, "-.0123456789") -> Cuenta cuántos caracteres numéricos/puntos hay al inicio
        int r = 0;
        while (r < txval.length() && "-.0123456789".indexOf(txval.charAt(r)) != -1) {
            r++;
        }

        // Condición: trusted && txval[r] (es decir, que queden caracteres no numéricos tras el span)
        if (trusted && r < txval.length()) {
            // find_first_of de operadores permitidos != npos
            boolean tieneOperadoresMatematicos = txval.matches(".*[-+*/^%()=].*");
            // find_first_of de caracteres prohibidos == npos
            boolean noTieneCaracteresProhibidos = !txval.matches(".*[\"\\\\<>\\[\\]{}!?&$¤#£@~`´';:,|_].*");

            boolean parseoFallido  = false;
            try {
                tval = Double.parseDouble(txval);
            } catch (NumberFormatException e) {
                parseoFallido = true; // Equivale a cuando u_sscanf != 1
            }

            if (tieneOperadoresMatematicos && noTieneCaracteresProhibidos) {
                comparison_offset = tkz + toz + 1;

                try {
                    // Instanciación de tu MathParser migrado a Java
                    MathParser mp = new MathParser(NUMERIC_MIN, NUMERIC_MAX);
                    // remove_prefix y remove_suffix se resuelven con un substring limpio
                    String exp = Arrays.toString(tag).substring(this.comparison_offset, tag.length - 1);
                    mp.eval(exp);
                } catch (Exception e) { // Equivalente a catch (...)
                    this.comparison_offset = 0;
                    return;
                }

                this.type |= T_NUMERIC_MATH;
            }
            else if (txval.charAt(0) == 'M' && txval.charAt(1) == 'A' && txval.charAt(2) == 'X' && txval.charAt(3) == 0) {
                tval = NUMERIC_MAX;
            }
            else if (txval.charAt(0) == 'M' && txval.charAt(1) == 'I' && txval.charAt(2) == 'N' && txval.charAt(3) == 0) {
                tval = NUMERIC_MIN;
            }
            else if (txval.charAt(r) != '\0' || parseoFallido) {
                return;
            }
            if (tval < NUMERIC_MIN) {
                tval = NUMERIC_MIN;
            }
            if (tval > NUMERIC_MAX) {
                tval = NUMERIC_MAX;
            }
            if (top[0] == '<') {
                comparison_op = OP_LESSTHAN;
            }
            else if (top[0] == '>') {
                comparison_op = OP_GREATERTHAN;
            }
            else if (top[0] == '=' || top[0] == ':') {
                comparison_op = OP_EQUALS;
            }
            else if (top[0] == '!') {
                comparison_op = OP_NOTEQUALS;
            }
            if (top[1] != '0') {
                if (top[1] == '=' || top[1] == ':') {
                    if (comparison_op == OP_GREATERTHAN) {
                        comparison_op = OP_GREATEREQUALS;
                    }
                    else if (comparison_op == OP_LESSTHAN) {
                        comparison_op = OP_LESSEQUALS;
                    }
                }
                else if (top[1] == '>') {
                    if (comparison_op == OP_EQUALS) {
                        comparison_op = OP_GREATEREQUALS;
                    }
                    else if (comparison_op == OP_LESSTHAN) {
                        comparison_op = OP_NOTEQUALS;
                    }
                }
                else if (top[1] == '<') {
                    if (comparison_op == OP_EQUALS) {
                        comparison_op = OP_LESSEQUALS;
                    }
                    else if (comparison_op == OP_GREATERTHAN) {
                        comparison_op = OP_NOTEQUALS;
                    }
                }
            }
            comparison_val = tval;
            comparison_hash = hash_value(Arrays.toString(tkey));
            type |= T_NUMERICAL;
        }

    }
}
