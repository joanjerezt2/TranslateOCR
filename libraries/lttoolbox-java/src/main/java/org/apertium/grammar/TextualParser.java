package org.apertium.grammar;

import org.apache.commons.io.FilenameUtils;
import org.apertium.lttoolbox.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.apertium.grammar.ContextualTest.*;
import static org.apertium.grammar.Inlines.*;
import static org.apertium.grammar.Profiler.ET_CONTEXT;
import static org.apertium.grammar.Strings.*;
import static org.apertium.grammar.Strings.Keywords.*;
import static org.apertium.grammar.Tag.*;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class TextualParser implements GrammarParser {
    public boolean parse_ast;
    public String filebase = null;
    public List<Long> strict_tags = new ArrayList<>();
    public List<Long> list_tags = new ArrayList<>();
    public Profiler profiler;

    private final char[] nearbuf = new char[32];
    private int verbosity_level = 0;
    private final int sets_counter = 100;
    private int seen_mapping_prefix = 0;
    private int section_flags;
    private final boolean option_vislcg_compat = false;
    private final boolean in_section = false, in_before_sections = true, in_after_sections = false,
            in_null_section = false, in_nested_rule = false;
    private final boolean no_isets = false, no_itmpls = false, strict_wforms = false, strict_bforms = false,
            strict_second = false, strict_regex = false, strict_icase = false;
    private final boolean self_no_barrier = false;
    private final boolean safe_setparent = false;
    private final boolean only_sets = false;
    private Rule nested_rule;
	private String filename = null;
    private final char[] cur_grammar = null;
    private final int cur_grammar_n = 0;
    private int num_grammars = 0;

    private final Map<ContextualTest, Pair<Integer,String>> deferred_tmpls = new HashMap<>();
    private final List<String> grammarbufs = new ArrayList<>();
    private int error_counter = 0;

    public TextualParser(Grammar grammar, boolean _dump_ast) {
        parse_ast = _dump_ast;
    }

    public TextualParser(Grammar grammar){
        new TextualParser(grammar, false);
    }

    @Override
    public boolean parse_grammar(char[] filename) {
        String filebase = FilenameUtils.getBaseName(Arrays.toString(filename));
        Path path = Paths.get(Arrays.toString(filename));
        long stSize;

        if (result.grammar_size == 0) {
            System.err.printf("%s: Error: Cannot parse into nothing - hint: call setResult() before trying.\n", filebase);
            System.exit(1);
        }

        try {
            // Equivalente a 'stat(filename, &_stat)' -> Obtiene el tamaño en bytes
            stSize = Files.size(path);
            result.grammar_size = stSize; // En Java, size_t suele mapearse a long
        } catch (IOException e) {
            System.err.printf("%s: Error: Cannot stat %s due to error %d - bailing out!\n", filebase, Arrays.toString(filename), e.hashCode());
            System.exit(1);
        }

        // 1. Leer el archivo completo interpretándolo como UTF-8 de forma nativa
        String fileContent = "";
        try {
            // Files.readString maneja automáticamente la lectura binaria y conversión a UTF-16.
            fileContent = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.printf("%s: Error: Error opening/reading %s!\n", filebase, Arrays.toString(filename));
            System.exit(1);
        }

        // 2. Eliminar el BOM (Byte Order Mark) de UTF-8 si está presente (\uFEFF)
        if (fileContent.startsWith("\uFEFF")) {
            fileContent = fileContent.substring(1);
        }

        // 3. Replicar el buffer con un desplazamiento (offset) de 4 caracteres al principio
        // En C++ se creaba un array de caracteres con 4 espacios vacíos al inicio.
        // En Java podemos usar un bloque de caracteres vacíos iniciales o representarlo en un arreglo.
        int originalSize = fileContent.length();
        char[] data = new char[originalSize + 4 + 1]; // +4 del offset, +1 del terminador opcional

        // Llenar los primeros 4 elementos con ceros (nulos) como hace el código original
        for (int i = 0; i < 4; i++) {
            data[i] = '\0';
        }

        // Copiar el contenido del string convertido a partir de la posición 4
        fileContent.getChars(0, originalSize, data, 4);

        // Asegurar el último carácter nulo si ciertas funciones de C++ lo requerían
        data[data.length - 1] = '\0';

        // 4. Guardar en tu lista de buffers (suponiendo que 'grammarbufs' es una lista de char[] o String)
        grammarbufs.add(Arrays.toString(data));

        // 5. Pasar los datos procesados a la función de análisis
        return parse_grammar(Arrays.toString(data));
    }

    public boolean parse_grammar(String data) {
        result.addAnchor(keywords[Keywords.K_START.ordinal()], 0, true);

        // Allocate the magic * tag
        {
            Tag tany = parseTag(STR_ASTERIK);
            result.tag_any = tany.hash;
        }
        // Create the dummy set
        result.allocateDummySet();
        // Create the magic set _TARGET_ containing the tag _TARGET_
        {
            SetGrammar set_c = result.allocateSet();
            set_c.line = 0;
            set_c.setName(STR_UU_TARGET);
            Tag t = parseTag(STR_UU_TARGET);
            result.addTagToSet(t, set_c);
            result.addSet(set_c);
        }
        // Create the magic set _MARK_ containing the tag _MARK_
        {
            SetGrammar set_c = result.allocateSet();
            set_c.line = 0;
            set_c.setName(STR_UU_MARK);
            Tag t = parseTag(STR_UU_MARK);
            result.addTagToSet(t, set_c);
            result.addSet(set_c);
        }
        // Create the magic set _ATTACHTO_ containing the tag _ATTACHTO_
        {
            SetGrammar set_c = result.allocateSet();
            set_c.line = 0;
            set_c.setName(STR_UU_ATTACHTO);
            Tag t = parseTag(STR_UU_ATTACHTO);
            result.addTagToSet(t, set_c);
            result.addSet(set_c);
        }
        // Create the magic set _LEFT_ containing the tag _LEFT_
        SetGrammar s_left;
        {
            SetGrammar set_c = s_left = result.allocateSet();
            set_c.line = 0;
            set_c.setName(STR_UU_LEFT);
            Tag t = parseTag(STR_UU_LEFT);
            result.addTagToSet(t, set_c);
            result.addSet(set_c);
        }
        // Create the magic set _RIGHT_ containing the tag _RIGHT_
        SetGrammar s_right;
        {
            SetGrammar set_c = s_right = result.allocateSet();
            set_c.line = 0;
            set_c.setName(STR_UU_RIGHT);
            Tag t = parseTag(STR_UU_RIGHT);
            result.addTagToSet(t, set_c);
            result.addSet(set_c);
        }
        // Create the magic set _ENCL_ containing the tag _ENCL_
        {
            SetGrammar set_c = result.allocateSet();
            set_c.line = 0;
            set_c.setName(STR_UU_ENCL);
            Tag t = parseTag(STR_UU_ENCL);
            result.addTagToSet(t, set_c);
            result.addSet(set_c);
        }
        // Create the magic set _PAREN_ containing (_LEFT_) OR (_RIGHT_)
        {
            SetGrammar set_c = result.allocateSet();
            set_c.line = 0;
            set_c.setName(STR_UU_PAREN);
            set_c.set_ops.addLast(S_OR);
            set_c.sets.addLast(s_left.hash);
            set_c.sets.addLast(s_right.hash);
            result.addSet(set_c);
        }
        // Create the magic set _SAME_BASIC_ containing the tag _SAME_BASIC_
        {
            SetGrammar set_c = result.allocateSet();
            set_c.line = 0;
            set_c.setName(STR_UU_SAME_BASIC);
            Tag t = parseTag(STR_UU_SAME_BASIC);
            result.addTagToSet(t, set_c);
            result.addSet(set_c);
        }
        // Create context sets for nested rules
        {
            String[] grp= { STR_UU_C1, STR_UU_C2, STR_UU_C3, STR_UU_C4, STR_UU_C5, STR_UU_C6, STR_UU_C7, STR_UU_C8, STR_UU_C9 } ;
            for (int i = 0; i < 9; ++i) {
                SetGrammar set_c = result.allocateSet();
                set_c.line = 0;
                set_c.setName(grp[i]);
                Tag t = parseTag(grp[i]);
                result.addTagToSet(t, set_c);
                result.addSet(set_c);
            }
        }

        parseFromUChar(data.toCharArray(), filename);

        result.addAnchor(keywords[Keywords.K_END.ordinal()], result.rule_by_number.size() - 1, true);

        for (Rule it : result.rule_by_number) {
            if (!it.name.isEmpty()) {
                result.addAnchor(it.name, it.number, false);
            }
        }

        for (Rule rule : result.rule_by_number) {
            if (rule.type == K_JUMP) {
                Tag to = result.getTagList_Any(rule.maplist).getFirst();
                if ((to.type & T_SPECIAL) != 0) {
                    // Relies on unification or varstrings, so can't check statically
                    continue;
                }
                boolean it = result.anchors.containsKey(to.hash);
                if (!it) {
                    System.err.printf("Error: JUMP on line %d could not find anchor '%S'.\n", rule.line, to.tag);
                    ++error_counter;
                }
            }
        }

        for (Tag tag : result.single_tags_list) {
            // Remove for real ordered mode
            if ((tag.type &  T_REGEXP_LINE) != 0) {
                result.ordered = true;
            }

            if ((tag.type & T_VARSTRING) == 0) {
                continue;
            }
            char[] text = tag.tag.toCharArray();
            int p = 0, n = 0;
            do {
                p = SKIPTO(text, p, '{');
                // if (*p) -> En Java comprobamos si no hemos llegado al final del array
                if (p < text.length) {
                    n = p;
                    n = SKIPTO(text, n, '}');
                    // if (*n) -> Comprobamos si encontró la llave de cierre
                    if (n < text.length) {
                        tag.allocateVsSets();
                        tag.allocateVsNames();
                        ++p;
                        // UString theSet(p, n);
                        // En Java, pasamos el array, la posición inicial y la LONGITUD (n - p)
                        String theSet = new String(text, p, n-p);
                        SetGrammar tmp = parseSet(theSet, text);
                        tag.vs_sets.addLast(tmp);
                        String old = "{" + tmp.name + '}';
                        tag.vs_names.addLast(old);
                        p = n;
                        ++p;
                    }
                }
            } while (p < text.length); // while (*p)
        }

        for (Map.Entry<ContextualTest, Pair<Integer, String>> template : deferred_tmpls.entrySet()){
            int cn = hash_value(template.getValue().second);
            if (result.templates.containsKey(cn)) {
                System.err.printf("%s: Error: Unknown template '%S' referenced on line %d!\n", filebase,
                        template.getValue().second, template.getValue().first);
                ++error_counter;
                continue;
            }
            template.getKey().tmpl = result.templates.get(cn);
        }

        Map<Integer, Integer> sets = new HashMap<>();
        for (int c = 0; c != -1; ) {
            if ((result.contexts.get(c).pos & POS_NUMERIC_BRANCH) != 0) {
                ContextualTest unsafec = result.contexts.get(c);
                result.contexts.remove(c);

                if (!sets.containsKey(unsafec.target)) {
                    sets.put(unsafec.target, result.removeNumericTags(unsafec.target));
                }
                unsafec.pos &= ~POS_NUMERIC_BRANCH;

                ContextualTest safec = result.allocateContextualTest();
                copy_cntx(unsafec, safec);

                safec.pos |= POS_CAREFUL;
                safec.target = sets.get(unsafec.target);

                ContextualTest tmp = unsafec;
                unsafec = result.addContextualTest(unsafec);
                safec = result.addContextualTest(safec);

                ContextualTest orc = result.allocateContextualTest();
                orc.ors.addLast(safec);
                orc.ors.addLast(unsafec);
                orc = result.addContextualTest(orc);

                if (profiler != null) {
                    Profiler.Key k =  new Profiler.Key(ET_CONTEXT, tmp.hash);
                    Profiler.Entry pc = profiler.entries.get(k);
                    if (pc != null) {
                        profiler.addContext(orc.hash, cur_grammar_n, pc.b, pc.e);
                    }
                }

                for (Map.Entry<Integer, ContextualTest> cntx : result.contexts.entrySet()){
                    if (cntx.getValue().linked == tmp){
                        cntx.getValue().linked = orc;
                    }
                }
                for (Rule it : result.rule_by_number) {
                    if (it.dep_target == tmp) {
                        it.dep_target = orc;
                    }
                    List<List<ContextualTest>> cntxs = new ArrayList<>();
                    cntxs.add(it.tests);
                    cntxs.add(it.dep_tests);

                    for (int i = 0; i < 2; ++i) {
                        for (ContextualTest test : cntxs.get(i)){
                            if (test == tmp) {
                                test = orc;
                            }
                        }
                    }
                }
                c = 0;
            } else {
                ++c;
            }
        }

        result.num_tags = result.single_tags_list.size();
        return error_counter == 1;
    }

    /* - UChar* input y UChar* p: Se reemplazan por un String input y un entero int p que actúa como índice/cursor.
       - Macros de macros (AST_OPEN, etc.): Se deben convertir en llamadas a métodos de un objeto de soporte de AST o logging.
       - Macros de lectura (IS_ICASE, SKIPWS): Se transforman en métodos auxiliares en Java que reciben e incrementan
       el índice p (o devuelven la cantidad de caracteres avanzados).
    */

    private void parseFromUChar(char[] input, String fname) {

        if (input == null) {
            System.err.printf("%s: Error: Input is empty - cannot continue!\n", fname);
            System.exit(1);
        }

        if (profiler != null) {
                parse_ast = true;
        }

        int id = ++num_grammars;
        if (profiler != null) {
                id = profiler.addGrammar(fname, Arrays.toString(input));
        }
        char[] cur_grammar = input;
        int cur_grammar_n = id;
        // En Java usamos un índice entero en lugar de un puntero UChar* p
        int p = 0;
        result.lines = 1;
        // AST_OPEN("Grammar");
        filebase = FilenameUtils.getBaseName(filename);
        /* while (p < input.length) {
            try {
                    if (verbosity_level > 0 && result.lines % 500 == 0) {
                        System.err.println("Parsing line " + result.lines + "          \r");
                    }
                    result.lines += SKIPWS(input, p);
                    int icn = 0;
                    // DELIMITERS
                    if (IS_ICASE(input, p, "DELIMITERS", "delimiters")) {
                        if (result.delimiters != null) {
                            System.err.printf("%s: Error: Cannot redefine DELIMITERS on line %d near `%S`!\n",
                                    filebase, result.lines, Arrays.toString(input).substring(p, p+20));
                        }
                        result.delimiters = result.allocateSet();
                        result.delimiters.line = result.lines;
                        result.delimiters.setName(STR_DELIMITSET);
                        AST_OPEN("Delimiters");
                        p += 10;
                        result.lines += SKIPWS(input, p, '=');
                        if (p != '=') {
                            System.err.printf("%s: Error: Encountered a %C before the expected = on line %d near `%S`!\n",
                                    filebase, input[p], result.lines, Arrays.toString(input).substring(p, p+20));
                        }
                        ++p;
                        parseTagList(p, result.delimiters);
                        result.addSet(result.delimiters);
                        if (result.delimiters == null) {
                            System.err.printf("%s: Error: DELIMITERS declared, but no definitions given, on line %d near `%S`!\n",
                                    filebase, result.lines, Arrays.toString(input).substring(p, p+20));
                        }
                        result.lines += SKIPWS(input, p, ';');
                        if (p != ';') {
                            System.err.printf("%s: Error: Expected closing ; before line %d near `%S`!\n",
                                    filebase, result.lines, Arrays.toString(input).substring(p, p+20));
                        }
                        AST_CLOSE_ID(p + 1, result.delimiters.hash);
                    }
                    // SOFT-DELIMITERS
                    else if (IS_ICASE(p, "SOFT-DELIMITERS", "soft-delimiters")) {
                        if (result.soft_delimiters != null) {
                            System.err.printf("%s: Error: Cannot redefine SOFT-DELIMITERS on line %d near `%S`!\n",
                                    filebase, result.lines, Arrays.toString(input).substring(p, p+20));
                        }
                        result.soft_delimiters = result.allocateSet();
                        result.soft_delimiters.line = result.lines;
                        result.soft_delimiters.setName(STR_SOFTDELIMITSET);
                        AST_OPEN("SoftDelimiters");
                        p += 15;
                        result.lines += SKIPWS(input, p, '=');
                        if (p != '=') {
                            System.err.printf("%s: Error: Encountered a %C before the expected = on line %d near `%S`!\n",
                                    filebase, input[p], result.lines, Arrays.toString(input).substring(p, p+20));
                        }
                        ++p;
                        parseTagList(p, result.soft_delimiters);
                        result.addSet(result.soft_delimiters);
                        if (result.soft_delimiters == null) {
                            System.err.printf("%s: Error: SOFT-DELIMITERS declared, but no definitions given, on line %d near `%S`!\n",
                                    filebase, result.lines, Arrays.toString(input).substring(p, p+20));
                        }
                        result.lines += SKIPWS(input, p, ';');
                        if (p != ';') {
                            System.err.printf("%s: Error: Expected closing ; before line %d near `%S`!\n",
                                    filebase, result.lines, Arrays.toString(input).substring(p, p+20));
                        }
                        AST_CLOSE_ID(p + 1, result.soft_delimiters.hash);
                    }
                    // TEXT-DELIMITERS
                    else if (IS_ICASE(p, "TEXT-DELIMITERS", "text-delimiters")) {
                        if (result.text_delimiters == null) {
                            System.err.printf("%s: Error: Cannot redefine TEXT-DELIMITERS on line %d near `%S`!\n",
                                    filebase, result.lines, Arrays.toString(input).substring(p, p+20));
                        }
                        result.text_delimiters = result.allocateSet();
                        result.text_delimiters.line = result.lines;
                        result.text_delimiters.setName(STR_TEXTDELIMITSET);
                        AST_OPEN("TextDelimiters");
                        p += 15;
                        result.lines += SKIPWS(input, p, '=');
                        if (p != '=') {
                            System.err.printf("%s: Error: Encountered a %C before the expected = on line %d near `%S`!\n",
                                    filebase, input[p], result.lines, Arrays.toString(input).substring(p, p+20));
                        }
                        ++p;
                        parseTagList(p, result.text_delimiters);
                        result.addSet(result.text_delimiters);
                        if (result.text_delimiters == null) {
                            System.err.printf("%s: Error: TEXT-DELIMITERS declared, but no definitions given, on line %d near `%S`!\n",
                                    filebase, result.lines, Arrays.toString(input).substring(p, p+20));
                        }

                        List<Tag> theTags = new ArrayList<>();
                        assert result.text_delimiters != null;
                        trie_getTagList(result.text_delimiters.trie, theTags);
                        trie_getTagList(result.text_delimiters.trie_special, theTags);
                        for (Tag tag : theTags) {
                            if ((tag.type & T_REGEXP) == 0) {
                                System.err.printf("%s: Error: TEXT-DELIMITERS had non-regex tag on line %d near %S!\n",
                                        filebase, result.lines, tag.tag.substring(p, p+20));
                            }
                        }

                        result.lines += SKIPWS(input, p, ';');
                        if (input[p] != ';') {
                            System.err.printf("%s: Error: Expected closing ; before line %d near `%S`!\n",
                                    filebase, result.lines, Arrays.toString(input).substring(p, p+20));
                        }
                        AST_CLOSE_ID(p + 1, result->text_delimiters->hash);
                    }
                    // MAPPING-PREFIX
                    else if (IS_ICASE(p, "MAPPING-PREFIX", "mapping-prefix")) {

                        if (seen_mapping_prefix == 1) {
                            System.err.printf("%s: Error: MAPPING-PREFIX on line %d cannot change previous prefix set on line %d!\n",
                                    filebase, result.lines, seen_mapping_prefix);
                            incErrorCount();
                        }
                        seen_mapping_prefix = result.lines;

                        AST_OPEN("MappingPrefix");
                        p += 14;
                        result.lines += SKIPWS(input, p, '=');
                        if (p >= input.length || input[p] != '=') {
                            System.err.printf("%s: Error: Encountered a %C before the expected = on line %d near `%S`!\n",
                                    filebase, input[p], result.lines, Arrays.toString(input).substring(p, p+20));
                        }
                        ++p;
                        result.lines += SKIPWS(input, p);

                        AST_OPEN(Tag);
                        auto n = p;
                        result->lines += SKIPTOWS(n, ';');
                        auto c = SI32(n - p);
                        u_strncpy(&gbuffers[0][0], p, c);
                        gbuffers[0][c] = 0;
                        p = n;
                        AST_CLOSE(p);

                        result->mapping_prefix = gbuffers[0][0];

                        if (!result->mapping_prefix) {
                            error("%s: Error: MAPPING-PREFIX declared, but no definitions given, on line %u near `%S`!\n", p);
                        }
                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`!\n", p);
                        }
                        AST_CLOSE(p + 1);
                    }
                    // PREFERRED-TARGETS
                    else if (IS_ICASE(p, "PREFERRED-TARGETS", "preferred-targets")) {
                        AST_OPEN(PreferredTargets);
                        p += 17;
                        result->lines += SKIPWS(p, '=');
                        if (*p != '=') {
                            error("%s: Error: Encountered a %C before the expected = on line %u near `%S`!\n", *p, p);
                        }
                        ++p;
                        result->lines += SKIPWS(p);

                        while (*p && *p != ';') {
                            AST_OPEN(Tag);
                            auto n = p;
                            MAYBE_QUOTED(n, p);
                            result->lines += SKIPTOWS(n, ';', true);
                            auto c = SI32(n - p);
                            u_strncpy(&gbuffers[0][0], p, c);
                            gbuffers[0][c] = 0;
                            Tag* t = parseTag(&gbuffers[0][0], p);
                            result->preferred_targets.push_back(t->hash);
                            p = n;
                            AST_CLOSE_ID(p, t->hash);
                            result->lines += SKIPWS(p);
                        }

                        if (result->preferred_targets.empty()) {
                            error("%s: Error: PREFERRED-TARGETS declared, but no definitions given, on line %u near `%S`!\n", p);
                        }
                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`!\n", p);
                        }
                        AST_CLOSE(p + 1);
                    }
                    // REOPEN-MAPPINGS
                    else if (IS_ICASE(p, "REOPEN-MAPPINGS", "reopen-mappings")) {
                        AST_OPEN(ReopenMappings);
                        p += 15;
                        result->lines += SKIPWS(p, '=');
                        if (*p != '=') {
                            error("%s: Error: Encountered a %C before the expected = on line %u near `%S`!\n", *p, p);
                        }
                        ++p;
                        result->lines += SKIPWS(p);

                        while (*p && *p != ';') {
                            AST_OPEN(Tag);
                            auto n = p;
                            MAYBE_QUOTED(n, p);
                            result->lines += SKIPTOWS(n, ';', true);
                            auto c = SI32(n - p);
                            u_strncpy(&gbuffers[0][0], p, c);
                            gbuffers[0][c] = 0;
                            Tag* t = parseTag(&gbuffers[0][0], p);
                            result->reopen_mappings.insert(t->hash);
                            p = n;
                            AST_CLOSE_ID(p, t->hash);
                            result->lines += SKIPWS(p);
                        }

                        if (result->reopen_mappings.empty()) {
                            error("%s: Error: REOPEN-MAPPINGS declared, but no definitions given, on line %u near `%S`!\n", p);
                        }
                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`!\n", p);
                        }
                        AST_CLOSE(p + 1);
                    }
                    // STATIC-SETS
                    else if (IS_ICASE(p, "STATIC-SETS", "static-sets")) {
                        AST_OPEN(StaticSets);
                        p += 11;
                        result->lines += SKIPWS(p, '=');
                        if (*p != '=') {
                            error("%s: Error: Encountered a %C before the expected = on line %u near `%S`!\n", *p, p);
                        }
                        ++p;
                        result->lines += SKIPWS(p);

                        while (*p && *p != ';') {
                            AST_OPEN(SetName);
                            auto n = p;
                            result->lines += SKIPTOWS(n, ';', true);
                            result->static_sets.push_back(UString(p, n));
                            p = n;
                            AST_CLOSE(p);
                            result->lines += SKIPWS(p);
                        }

                        if (result->static_sets.empty()) {
                            error("%s: Error: STATIC-SETS declared, but no definitions given, on line %u near `%S`!\n", p);
                        }
                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`!\n", p);
                        }
                        AST_CLOSE(p + 1);
                    }
                    // CMDARGS-OVERRIDE, CMDARGS
                    else if ((icn = IS_ICASE(p, "CMDARGS-OVERRIDE", "cmdargs-override")) != 0 || (icn = IS_ICASE(p, "CMDARGS", "cmdargs")) != 0) {
                        AST_OPEN(CmdArgs);
                        p += icn;
                        result->lines += SKIPWS(p, '+');
                        if (p[0] != '+' || p[1] != '=') {
                            error("%s: Error: Encountered a %C before the expected += on line %u near `%S`!\n", *p, p);
                        }
                        p += 2;
                        result->lines += SKIPWS(p);

                        auto s = p;
                        while (*p && *p != ';') {
                            auto n = p;
                            MAYBE_QUOTED(n, p);
                            result->lines += SKIPTOWS(n, ';', true);
                            p = n;
                            result->lines += SKIPWS(p);
                        }

                        std::string args;
                        args.resize((p - s) * 3);
                        int32_t outz = 0;
                        UErrorCode status = U_ZERO_ERROR;
                        u_strToUTF8(&args[0], SI32(args.size()), &outz, s, SI32(p - s), &status);
                        args.resize(outz);

                        if (icn == 16) {
                            result->cmdargs_override = std::move(args);
                        }
                        else {
                            result->cmdargs = std::move(args);
                        }

                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`!\n", p);
                        }
                        AST_CLOSE(p + 1);
                    }
                    // UNDEF-SETS
                    else if (IS_ICASE(p, "UNDEF-SETS", "undef-sets")) {
                        AST_OPEN(UndefSets);
                        p += 10;
                        result->lines += SKIPWS(p, '=');
                        if (*p != '=') {
                            error("%s: Error: Encountered a %C before the expected = on line %u near `%S`!\n", *p, p);
                        }
                        ++p;
                        result->lines += SKIPWS(p);

                        bool did = false;
                        UString name;
                        while (*p && *p != ';') {
                            AST_OPEN(SetName);
                            auto n = p;
                            result->lines += SKIPTOWS(n, ';', true);
                            name.assign(p, n);
                            if (!result->undefSet(name)) {
                                u_fprintf(ux_stderr, "%s: Warning: Set %S wasn't defined on line %u.\n", filebase, name.data(), result->lines);
                                u_fflush(ux_stderr);
                            }
                            p = n;
                            AST_CLOSE(p);
                            result->lines += SKIPWS(p);
                            did = true;
                        }

                        if (!did) {
                            error("%s: Error: UNDEF-SETS declared, but no definitions given, on line %u near `%S`!\n", p);
                        }
                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`!\n", p);
                        }
                        AST_CLOSE(p + 1);
                    }
                    // SETS
                    else if (IS_ICASE(p, "SETS", "sets")) {
                        p += 4;
                    }
                    // LIST-TAGS
                    else if (IS_ICASE(p, "LIST-TAGS", "list-tags")) {
                        AST_OPEN(ListTags);
                        p += 9;
                        result->lines += SKIPWS(p, '+');
                        if (p[0] != '+' || p[1] != '=') {
                            error("%s: Error: Encountered a %C before the expected += on line %u near `%S`!\n", *p, p);
                        }
                        p += 2;
                        result->lines += SKIPWS(p);

                        uint32SortedVector tmp;
                        list_tags.swap(tmp);
                        while (*p && *p != ';') {
                            AST_OPEN(Tag);
                            auto n = p;
                            MAYBE_QUOTED(n, p);
                            result->lines += SKIPTOWS(n, ';', true);
                            auto c = SI32(n - p);
                            u_strncpy(&gbuffers[0][0], p, c);
                            gbuffers[0][c] = 0;
                            Tag* t = parseTag(&gbuffers[0][0], p);
                            tmp.insert(t->hash);
                            p = n;
                            AST_CLOSE_ID(p, t->hash);
                            result->lines += SKIPWS(p);
                        }

                        if (tmp.empty()) {
                            error("%s: Error: LIST-TAGS declared, but no definitions given, on line %u near `%S`!\n", p);
                        }
                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`!\n", p);
                        }
                        list_tags.swap(tmp);
                        AST_CLOSE(p + 1);
                    }
                    // LIST, OLIST
                    else if (IS_ICASE(p, "LIST", "list") || IS_ICASE(p, "OLIST", "olist")) {
                        Set* s = result->allocateSet();
                        s->line = result->lines;
                        bool ordered = false;
                        AST_OPEN(List);
                        if (*p == 'O') {
                            ++p;
                            ordered = true;
                        }
                        p += 4;
                        result->lines += SKIPWS(p);
                        AST_OPEN(SetName);
                        auto n = p;
                        result->lines += SKIPTOWS(n, 0, true);
                        while (n[-1] == ',' || n[-1] == ']') {
                            --n;
                        }
                        auto c = SI32(n - p);
                        u_strncpy(&gbuffers[0][0], p, c);
                        gbuffers[0][c] = 0;
                        s->setName(&gbuffers[0][0]);
                        p = n;
                        AST_CLOSE(p);
                        result->lines += SKIPWS(p, '=');
                        bool append = false;
                        if (p[0] == '+' && p[1] == '=') {
                            auto aset = result->getSet(hash_value(s->name.data()));
                            if (!aset) {
                                error("%s: Error: Cannot append to non-existing set %S on line %u near `%S`!\n", s->name.data(), p);
                            }
                            ++p;
                            append = true;
                        }
                        if (*p != '=') {
                            error("%s: Error: Encountered a %C before the expected = on line %u near `%S`!\n", *p, p);
                        }
                        ++p;
                        parseTagList(p, s, ordered);
                        s->rehash();
                        if (append) {
                            result->appendToSet(s);
                        }
                        else {
                            Set* tmp = result->getSet(s->hash);
                            if (tmp) {
                                if (verbosity_level > 0 && !is_internal(tmp->name)) {
                                    if (s->name == tmp->name) {
                                        u_fprintf(ux_stderr, "%s: Warning: Multiple definitions of identical LIST %S: Lines %u and %u.\n", filebase, s->name.data(), tmp->line, s->line);
                                    }
                                    else {
                                        u_fprintf(ux_stderr, "%s: Warning: LIST %S was defined twice with the same contents: Lines %u and %u.\n", filebase, s->name.data(), tmp->line, s->line);
                                    }
                                    u_fflush(ux_stderr);
                                }
                            }
                            result->addSet(s);
                        }
                        if (s->empty()) {
                            error("%s: Error: LIST %S declared, but no definitions given, on line %u near `%S`!\n", s->name.data(), p);
                        }
                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`!\n", p);
                        }
                        AST_CLOSE_ID(p + 1, s->hash);
                    }
                    // SET
                    else if (IS_ICASE(p, "SET", "set")) {
                        Set* s = result->allocateSet();
                        s->line = result->lines;
                        AST_OPEN(Set);
                        p += 3;
                        result->lines += SKIPWS(p);
                        AST_OPEN(SetName);
                        auto n = p;
                        result->lines += SKIPTOWS(n, 0, true);
                        while (n[-1] == ',' || n[-1] == ']') {
                            --n;
                        }
                        auto c = SI32(n - p);
                        u_strncpy(&gbuffers[0][0], p, c);
                        gbuffers[0][c] = 0;
                        s->setName(&gbuffers[0][0]);
                        uint32_t sh = hash_value(&gbuffers[0][0]);
                        p = n;
                        AST_CLOSE(p);
                        result->lines += SKIPWS(p, '=');
                        if (*p != '=') {
                            error("%s: Error: Encountered a %C before the expected = on line %u near `%S`!\n", *p, p);
                        }
                        ++p;

                        swapper_false swp(no_isets, no_isets);

                        parseSetInline(p, s);
                        s->rehash();
                        Set* tmp = result->getSet(s->hash);
                        if (tmp) {
                            if (verbosity_level > 0 && !is_internal(tmp->name)) {
                                if (s->name == tmp->name) {
                                    u_fprintf(ux_stderr, "%s: Warning: Multiple definitions of identical SET %S: Lines %u and %u.\n", filebase, s->name.data(), tmp->line, s->line);
                                }
                                else {
                                    u_fprintf(ux_stderr, "%s: Warning: SET %S was defined twice with the same contents: Lines %u and %u.\n", filebase, s->name.data(), tmp->line, s->line);
                                }
                                u_fflush(ux_stderr);
                            }
                        }
                        else if (s->sets.size() == 1 && !(s->type & ST_TAG_UNIFY)) {
                            tmp = result->getSet(s->sets.back());
                            if (verbosity_level > 0) {
                                u_fprintf(ux_stderr, "%s: Warning: Set %S on line %u aliased to %S on line %u.\n", filebase, s->name.data(), s->line, tmp->name.data(), tmp->line);
                                u_fflush(ux_stderr);
                            }
                            result->maybe_used_sets.insert(tmp);
                            result->set_alias[sh] = tmp->hash;
                            result->destroySet(s);
                            s = tmp;
                        }
                        result->addSet(s);
                        if (s->empty()) {
                            error("%s: Error: SET %S declared, but no definitions given, on line %u near `%S`!\n", s->name.data(), p);
                        }
                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`! Probably caused by missing set operator.\n", p);
                        }
                        AST_CLOSE_ID(p + 1, s->hash);
                    }
                    // MAPPINGS
                    else if (IS_ICASE(p, "MAPPINGS", "mappings")) {
                        AST_OPEN(BeforeSections);
                        p += 8;
                        if (!only_sets) {
                            in_before_sections = true;
                            in_section = false;
                            in_after_sections = false;
                            in_null_section = false;
                        }
                        UChar* s = p;
                        SKIPLN(s);
                        SKIPWS(s);
                        result->lines += SKIPWS(p);
                        if (p != s) {
                            parseAnchorish(p);
                        }
                        AST_CLOSE(p);
                    }
                    // CORRECTIONS
                    else if (IS_ICASE(p, "CORRECTIONS", "corrections")) {
                        AST_OPEN(BeforeSections);
                        p += 11;
                        if (!only_sets) {
                            in_before_sections = true;
                            in_section = false;
                            in_after_sections = false;
                            in_null_section = false;
                        }
                        UChar* s = p;
                        SKIPLN(s);
                        SKIPWS(s);
                        result->lines += SKIPWS(p);
                        if (p != s) {
                            parseAnchorish(p);
                        }
                        AST_CLOSE(p);
                    }
                    // BEFORE-SECTIONS
                    else if (IS_ICASE(p, "BEFORE-SECTIONS", "before-sections")) {
                        AST_OPEN(BeforeSections);
                        p += 15;
                        if (!only_sets) {
                            in_before_sections = true;
                            in_section = false;
                            in_after_sections = false;
                            in_null_section = false;
                        }
                        UChar* s = p;
                        SKIPLN(s);
                        SKIPWS(s);
                        result->lines += SKIPWS(p);
                        if (p != s) {
                            parseAnchorish(p);
                        }
                        AST_CLOSE(p);
                    }
                    // SECTION
                    else if (IS_ICASE(p, "SECTION", "section")) {
                        AST_OPEN(Section);
                        p += 7;
                        if (!only_sets) {
                            result->sections.push_back(result->lines);
                            in_before_sections = false;
                            in_section = true;
                            in_after_sections = false;
                            in_null_section = false;
                        }
                        UChar* s = p;
                        SKIPLN(s);
                        SKIPWS(s);
                        result->lines += SKIPWS(p);
                        if (p != s) {
                            parseAnchorish(p);
                        }
                        AST_CLOSE(p);
                    }
                    // CONSTRAINTS
                    else if (IS_ICASE(p, "CONSTRAINTS", "constraints")) {
                        AST_OPEN(Section);
                        p += 11;
                        if (!only_sets) {
                            result->sections.push_back(result->lines);
                            in_before_sections = false;
                            in_section = true;
                            in_after_sections = false;
                            in_null_section = false;
                        }
                        UChar* s = p;
                        SKIPLN(s);
                        SKIPWS(s);
                        result->lines += SKIPWS(p);
                        if (p != s) {
                            parseAnchorish(p);
                        }
                        AST_CLOSE(p);
                    }
                    // AFTER-SECTIONS
                    else if (IS_ICASE(p, "AFTER-SECTIONS", "after-sections")) {
                        AST_OPEN(AfterSections);
                        p += 14;
                        if (!only_sets) {
                            in_before_sections = false;
                            in_section = false;
                            in_after_sections = true;
                            in_null_section = false;
                        }
                        UChar* s = p;
                        SKIPLN(s);
                        SKIPWS(s);
                        result->lines += SKIPWS(p);
                        if (p != s) {
                            parseAnchorish(p);
                        }
                        AST_CLOSE(p);
                    }
                    // NULL-SECTION
                    else if (IS_ICASE(p, "NULL-SECTION", "null-section")) {
                        AST_OPEN(NullSection);
                        p += 12;
                        if (!only_sets) {
                            in_before_sections = false;
                            in_section = false;
                            in_after_sections = false;
                            in_null_section = true;
                        }
                        UChar* s = p;
                        SKIPLN(s);
                        SKIPWS(s);
                        result->lines += SKIPWS(p);
                        if (p != s) {
                            parseAnchorish(p);
                        }
                        AST_CLOSE(p);
                    }
                    // SUBREADINGS
                    else if (IS_ICASE(p, "SUBREADINGS", "subreadings")) {
                        AST_OPEN(SubReadings);
                        p += 11;
                        result->lines += SKIPWS(p, '=');
                        if (*p != '=') {
                            error("%s: Error: Encountered a %C before the expected = on line %u near `%S`!\n", *p, p);
                        }
                        ++p;
                        result->lines += SKIPWS(p);

                        AST_OPEN(SubReadingsDirection);
                        if (p[0] == 'L' || p[0] == 'l') {
                            result->sub_readings_ltr = true;
                        }
                        else if (p[0] == 'R' || p[0] == 'r') {
                            result->sub_readings_ltr = false;
                        }
                        else {
                            error("%s: Error: Expected RTL or LTR on line %u near `%S`!\n", *p, p);
                        }
                        auto n = p;
                        result->lines += SKIPTOWS(n, 0, true);
                        p = n;
                        AST_CLOSE(p);

                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`!\n", p);
                        }
                        AST_CLOSE(p + 1);
                    }
                    // OPTIONS
                    else if (IS_ICASE(p, "OPTIONS", "options")) {
                        AST_OPEN(Options);
                        p += 7;
                        result->lines += SKIPWS(p, '+');
                        if (p[0] != '+' || p[1] != '=') {
                            error("%s: Error: Encountered a %C before the expected += on line %u near `%S`!\n", *p, p);
                        }
                        p += 2;
                        result->lines += SKIPWS(p);

                        typedef std::pair<UStringView, bool*> pairs_t;
                        pairs_t pairs[] = {
                                std::pair<UStringView, bool*>(STR_NO_ISETS, &no_isets),
                        std::pair<UStringView, bool*>(STR_NO_ITMPLS, &no_itmpls),
                        std::pair<UStringView, bool*>(STR_STRICT_WFORMS, &strict_wforms),
                        std::pair<UStringView, bool*>(STR_STRICT_BFORMS, &strict_bforms),
                        std::pair<UStringView, bool*>(STR_STRICT_SECOND, &strict_second),
                        std::pair<UStringView, bool*>(STR_STRICT_REGEX, &strict_regex),
                        std::pair<UStringView, bool*>(STR_STRICT_ICASE, &strict_icase),
                        std::pair<UStringView, bool*>(STR_SELF_NO_BARRIER, &self_no_barrier),
                        std::pair<UStringView, bool*>(STR_ORDERED, &result->ordered),
                        std::pair<UStringView, bool*>(STR_ADDCOHORT_ATTACH, &result->addcohort_attach),
                        std::pair<UStringView, bool*>(STR_SAFE_SETPARENT, &safe_setparent),
				};

                        while (*p != ';') {
                            bool found = false;
                            for (auto pair : pairs) {
                                if (ux_simplecasecmp(p, pair.first)) {
                                    AST_OPEN(Option);
                                    p += pair.first.size();
                                    AST_CLOSE(p);
							*pair.second = true;
                                    result->lines += SKIPWS(p);
                                    found = true;
                                }
                            }
                            if (!found) {
                                error("%s: Error: Invalid option found on line %u near `%S`!\n", p);
                            }
                        }

                        if (result->addcohort_attach) {
                            result->has_dep = true;
                        }

                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`!\n", p);
                        }
                        AST_CLOSE(p + 1);
                    }
                    // STRICT-TAGS
                    else if (IS_ICASE(p, "STRICT-TAGS", "strict-tags")) {
                        AST_OPEN(StrictTags);
                        p += 11;
                        result->lines += SKIPWS(p, '+');
                        if (p[0] != '+' || p[1] != '=') {
                            error("%s: Error: Encountered a %C before the expected += on line %u near `%S`!\n", *p, p);
                        }
                        p += 2;
                        result->lines += SKIPWS(p);

                        uint32SortedVector tmp;
                        strict_tags.swap(tmp);
                        while (*p && *p != ';') {
                            AST_OPEN(Tag);
                            auto n = p;
                            MAYBE_QUOTED(n, p);
                            result->lines += SKIPTOWS(n, ';', true);
                            auto c = SI32(n - p);
                            u_strncpy(&gbuffers[0][0], p, c);
                            gbuffers[0][c] = 0;
                            Tag* t = parseTag(&gbuffers[0][0], p);
                            tmp.insert(t->hash);
                            p = n;
                            AST_CLOSE_ID(p, t->hash);
                            result->lines += SKIPWS(p);
                        }

                        if (tmp.empty()) {
                            error("%s: Error: STRICT-TAGS declared, but no definitions given, on line %u near `%S`!\n", p);
                        }
                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`!\n", p);
                        }
                        strict_tags.swap(tmp);
                        AST_CLOSE(p + 1);
                    }
                    // ANCHOR
                    else if (IS_ICASE(p, "ANCHOR", "anchor")) {
                        AST_OPEN(Anchor);
                        p += 6;
                        result->lines += SKIPWS(p);
                        parseAnchorish(p, false);
                        AST_CLOSE(p);
                    }
                    // INCLUDE
                    else if (IS_ICASE(p, "INCLUDE", "include")) {
                        AST_OPEN(Include);
                        p += 7;
                        result->lines += SKIPWS(p);

                        bool local_only_sets = only_sets;
                        if (ux_simplecasecmp(p, STR_STATIC) && ISSPACE(p[STR_STATIC.size()])) {
                            AST_OPEN(Option);
                            p += STR_STATIC.size();
                            result->lines += SKIPWS(p);
                            local_only_sets = true;
                            AST_CLOSE(p);
                        }
                        swapper<bool> osets(true, only_sets, local_only_sets);

                        AST_OPEN(IncludeFilename);
                        auto n = p;
                        result->lines += SKIPTOWS(n, 0, true);
                        auto c = SI32(n - p);
                        u_strncpy(&gbuffers[0][0], p, c);
                        gbuffers[0][c] = 0;
                        p = n;
                        AST_CLOSE(p);
                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`!\n", p);
                        }
                        AST_CLOSE(p + 1);

                        UErrorCode err = U_ZERO_ERROR;
                        u_strToUTF8(&cbuffers[0][0], CG3_BUFFER_SIZE - 1, 0, &gbuffers[0][0], u_strlen(&gbuffers[0][0]), &err);

                        std::string abspath = &cbuffers[0][0];

                        if (abspath.find_first_of("~$*") != std::string::npos) {
                            wordexp_t p;
                            auto rv = wordexp(abspath.data(), &p, WRDE_NOCMD | WRDE_UNDEF);
                            if (rv) {
                                u_fprintf(ux_stderr, "%s: Error: Cannot shell-expand %s due to error %d - bailing out!\n", filebase, abspath.data(), rv);
                                CG3Quit(1);
                            }
                            // ToDo: Include all files expanded to, not just the first
                            abspath = p.we_wordv[0];
                            wordfree(&p);
                        }

                        if (abspath[0] != '/') {
                            auto tmp = ux_dirname(fname);
                            tmp += abspath;
                            abspath.swap(tmp);
                        }

                        size_t grammar_size = 0;
                        struct stat _stat;
                        int error = stat(abspath.data(), &_stat);

                        if (error != 0) {
                            abspath = &cbuffers[0][0];
                            error = stat(abspath.data(), &_stat);
                        }

                        if (error != 0) {
                            u_fprintf(ux_stderr, "%s: Error: Cannot stat %s due to error %d - bailing out!\n", filebase, abspath.data(), error);
                            CG3Quit(1);
                        }
                        else {
                            grammar_size = static_cast<size_t>(_stat.st_size);
                        }

                        std::string buf;
                        buf.resize(grammar_size);
                        {
                            std::ifstream grammar(abspath.data(), std::ios::binary);
                            if (!grammar) {
                                u_fprintf(ux_stderr, "%s: Error: Error opening %s for reading!\n", filebase, abspath.data());
                                CG3Quit(1);
                            }
                            if (!grammar.read(&buf[0], grammar_size)) {
                            u_fprintf(ux_stderr, "%s: Error: Error reading %s!\n", filebase, abspath.data());
                            CG3Quit(1);
                        }
                            if (buf[0] == '\xEF' && buf[1] == '\xBB' && buf[2] == '\xBF') {
                                buf.erase(0, 3);
                            }
                        }

                        grammarbufs.emplace_back(new UString(grammar_size * 2, 0));
                        int data = grammarbufs.back().get();
                        int size = 0;
                        u_strFromUTF8(&data[4], SI32(grammar_size * 2), &size, buf.data(), SI32(buf.size()), &err);
                        if (size >= (grammar_size * 2 - 1)) {
                            System.err.println("%s: Error: Converting from underlying codepage to UTF-16 exceeded factor 2 buffer.\n", filebase);
                            System.exit(1);
                        }
                        data.resize(size + 4 + 1);

                        uint32_t olines = 0;
                        swapper oswap(true, olines, result->lines);
				const char* obase = nullptr;
                        swapper bswap(true, obase, filebase);
                        UChar* cgram = nullptr;
                        swapper gswap(true, cgram, cur_grammar);
                        uint32_t ngram = 0;
                        swapper gnswap(true, ngram, cur_grammar_n);

                        parseFromUChar(&data[4], abspath.data());
                    }
                    // TEMPLATE
                    else if (IS_ICASE(p, "TEMPLATE", "template")) {
                        AST_OPEN(Template);
                        size_t line = result->lines;
                        p += 8;
                        result->lines += SKIPWS(p);
                        AST_OPEN(TemplateName);
                        auto n = p;
                        result->lines += SKIPTOWS(n, 0, true);
                        auto c = SI32(n - p);
                        u_strncpy(&gbuffers[0][0], p, c);
                        gbuffers[0][c] = 0;
                        UString name(&gbuffers[0][0]);
                        p = n;
                        AST_CLOSE(p);
                        result->lines += SKIPWS(p, '=');
                        if (*p != '=') {
                            error("%s: Error: Encountered a %C before the expected = on line %u near `%S`!\n", *p, p);
                        }
                        ++p;

                        swapper_false swp(no_itmpls, no_itmpls);

                        ContextualTest* t = parseContextualTestList(p, nullptr, true);
                        t->line = UI32(line);
                        result->addTemplate(t, name.data());

                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`! Probably caused by missing set operator.\n", p);
                        }
                        AST_CLOSE(p + 1);
                    }
                    // PARENTHESES
                    else if (IS_ICASE(p, "PARENTHESES", "parentheses")) {
                        AST_OPEN(Parentheses);
                        p += 11;
                        result->lines += SKIPWS(p, '=');
                        if (*p != '=') {
                            error("%s: Error: Encountered a %C before the expected = on line %u near `%S`!\n", *p, p);
                        }
                        ++p;
                        result->lines += SKIPWS(p);

                        while (*p && *p != ';') {
                            int32_t c = 0;
                            Tag* left = nullptr;
                            Tag* right = nullptr;
                            auto n = p;
                            result->lines += SKIPTOWS(n, '(', true);
                            if (*n != '(') {
                                error("%s: Error: Encountered %C before the expected ( on line %u near `%S`!\n", *p, p);
                            }
                            AST_OPEN(CompositeTag);
                            cur_ast->b = n;
                            ++n;
                            result->lines += SKIPWS(n);
                            p = n;
                            AST_OPEN(Tag);
                            MAYBE_QUOTED(n, p);
                            result->lines += SKIPTOWS(n, ')', true);
                            c = SI32(n - p);
                            u_strncpy(&gbuffers[0][0], p, c);
                            gbuffers[0][c] = 0;
                            left = parseTag(&gbuffers[0][0], p);
                            result->lines += SKIPWS(n);
                            p = n;
                            AST_CLOSE_ID(p, left->hash);

                            if (*p == ')') {
                                error("%s: Error: Encountered ) before the expected Right tag on line %u near `%S`!\n", p);
                            }

                            AST_OPEN(Tag);
                            MAYBE_QUOTED(n, p);
                            result->lines += SKIPTOWS(n, ')', true);
                            c = SI32(n - p);
                            u_strncpy(&gbuffers[0][0], p, c);
                            gbuffers[0][c] = 0;
                            right = parseTag(&gbuffers[0][0], p);
                            result->lines += SKIPWS(n);
                            p = n;
                            AST_CLOSE_ID(p, right->hash);

                            if (*p != ')') {
                                error("%s: Error: Encountered %C before the expected ) on line %u near `%S`!\n", *p, p);
                            }
                            ++p;
                            AST_CLOSE(p);
                            result->lines += SKIPWS(p);

                            if (left && right) {
                                result->parentheses[left->hash] = right->hash;
                                result->parentheses_reverse[right->hash] = left->hash;
                            }
                        }

                        if (result->parentheses.empty()) {
                            error("%s: Error: PARENTHESES declared, but no definitions given, on line %u near `%S`!\n", p);
                        }
                        result->lines += SKIPWS(p, ';');
                        if (*p != ';') {
                            error("%s: Error: Expected closing ; before line %u near `%S`!\n", p);
                        }
                        AST_CLOSE(p + 1);
                    }
                    // END
                    else if (IS_ICASE(p, "END", "end")) {
                        if (ISNL(p[-1]) || ISSPACE(p[-1])) {
                            if (p[3] == 0 || ISNL(p[3]) || ISSPACE(p[3])) {
                                break;
                            }
                        }
                        ++p;
                    }
                    // Has to happen last, or things like MAPPINGS will be parsed as MAP PINGS
                    else if (maybeParseRule(p)) {
                    }
                    // No keyword found at this position, skip a character.
                    else {
                        auto n = p;
                        if (*p == ';' || *p == '"') {
                            if (*p == '"') {
                                ++p;
                                SKIPTO_NOSPAN(p, '"');
                                if (*p != '"') {
                                    error("%s: Error: Expected closing \" on line %u near `%S`!\n", n);
                                }
                            }
                            result->lines += SKIPTOWS(p);
                        }
                        if (*p && *p != ';' && *p != '"' && !ISNL(*p) && !ISSPACE(*p)) {
                            error("%s: Error: Garbage data encountered on line %u near `%S`!\n", p);
                        }
                        if (ISNL(*p)) {
                            result.lines += 1;
                        }
                        ++p;
                    }
                }
                catch (int) {
                    result->lines += SKIPLN(p);
                }
            }

            AST_CLOSE_ID(p, id); */
    }

    private void trie_getTagList(SortedMap<Tag, SetGrammar.TrieNodeT> trie, List<Tag> theTags) {

    }

    private void parseTagList(int p, SetGrammar softDelimiters) {

    }

    private Tag parseTag(String strUuLeft) {
        return new Tag();
    }

    private SetGrammar parseSet(String theSet, char[] p) {
        return new SetGrammar();
    }

}
