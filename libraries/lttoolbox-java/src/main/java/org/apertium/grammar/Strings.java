package org.apertium.grammar;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public interface Strings {

    List<char[]> cbuffers = new ArrayList<char[]>();

    // El enum tal como lo definimos antes (asumiendo que empieza en 0)
    enum Keywords {
        K_IGNORE,
        K_SETS,
        K_SOFT_DELIMITERS,
        K_PREFERRED_TARGETS,
        K_MAPPING_PREFIX,
        K_MAPPINGS,
        K_CONSTRAINTS,
        K_CORRECTIONS,
        K_SECTION,
        K_BEFORE_SECTIONS,
        K_AFTER_SECTIONS,
        K_NULL_SECTION,
        K_ADD,
        K_MAP,
        K_REPLACE,
        K_SELECT,
        K_REMOVE,
        K_IFF,
        K_APPEND,
        K_SUBSTITUTE,
        K_START,
        K_END,
        K_ANCHOR,
        K_EXECUTE,
        K_JUMP,
        K_REMVARIABLE,
        K_SETVARIABLE,
        K_DELIMIT,
        K_MATCH,
        K_SETPARENT,
        K_SETCHILD,
        K_ADDRELATION,
        K_SETRELATION,
        K_REMRELATION,
        K_ADDRELATIONS,
        K_SETRELATIONS,
        K_REMRELATIONS,
        K_TEMPLATE,
        K_MOVE,
        K_MOVE_AFTER,
        K_MOVE_BEFORE,
        K_SWITCH,
        K_REMCOHORT,
        K_STATIC_SETS,
        K_UNMAP,
        K_COPY,
        K_ADDCOHORT,
        K_ADDCOHORT_AFTER,
        K_ADDCOHORT_BEFORE,
        K_EXTERNAL,
        K_EXTERNAL_ONCE,
        K_EXTERNAL_ALWAYS,
        K_OPTIONS,
        K_STRICT_TAGS,
        K_REOPEN_MAPPINGS,
        K_SUBREADINGS,
        K_SPLITCOHORT,
        K_PROTECT,
        K_UNPROTECT,
        K_MERGECOHORTS,
        K_RESTORE,
        K_WITH,
        K_OLIST,
        K_OSET,
        K_CMDARGS,
        K_CMDARGS_OVERRIDE,
        K_COPYCOHORT,
        K_REMPARENT,
        K_SWITCHPARENT,
        KEYWORD_COUNT

    }

    // El equivalente a: constexpr UStringView keywords[KEYWORD_COUNT]
    String[] keywords = {
            "__CG3_DUMMY_KEYWORD__",
            // Agrega los demás strings en el mismo orden que las constantes del enum
            "SETS",
            "LIST",
            "SET",
            "DELIMITERS",
            "SOFT-DELIMITERS",
            "PREFERRED-TARGETS",
            "MAPPING-PREFIX",
            "MAPPINGS",
            "CONSTRAINTS",
            "CORRECTIONS",
            "SECTION",
            "BEFORE-SECTIONS",
            "AFTER-SECTIONS",
            "NULL-SECTION",
            "ADD",
            "MAP",
            "REPLACE",
            "SELECT",
            "REMOVE",
            "IFF",
            "APPEND",
            "SUBSTITUTE",
            "START",
            "END",
            "ANCHOR",
            "EXECUTE",
            "JUMP",
            "REMVARIABLE",
            "SETVARIABLE",
            "DELIMIT",
            "MATCH",
            "SETPARENT",
            "SETCHILD",
            "ADDRELATION",
            "SETRELATION",
            "REMRELATION",
            "ADDRELATIONS",
            "SETRELATIONS",
            "REMRELATIONS",
            "TEMPLATE",
            "MOVE",
            "MOVE-AFTER",
            "MOVE-BEFORE",
            "SWITCH",
            "REMCOHORT",
            "STATIC-SETS",
            "UNMAP",
            "COPY",
            "ADDCOHORT",
            "ADDCOHORT-AFTER",
            "ADDCOHORT-BEFORE",
            "EXTERNAL",
            "EXTERNAL-ONCE",
            "EXTERNAL-ALWAYS",
            "OPTIONS",
            "STRICT-TAGS",
            "REOPEN-MAPPINGS",
            "SUBREADINGS",
            "SPLITCOHORT",
            "PROTECT",
            "UNPROTECT",
            "MERGECOHORTS",
            "RESTORE",
            "WITH",
            "OLIST",
            "OSET",
            "CMDARGS",
            "CMDARGS-OVERRIDE",
            "COPYCOHORT",
            "REMPARENT",
            "SWITCHPARENT",

    };

    int S_IGNORE = 0;
    int S_OR = 3;
    int S_PLUS = 0;
    int S_MINUS = 0;
    int S_FAILFAST = 8;
    int S_SET_DIFF = 0;
    int S_SET_ISECT_U = 0;
    int S_SET_SYMDIFF_U = 0;

    String STR_TARGET = "TARGET" ;
    String STR_AND = "AND" ;
    String STR_IF = "IF" ;
    String STR_OR = "OR" ;
    String STR_TEXTNOT = "NOT" ;
    String STR_TEXTNEGATE = "NEGATE" ;
    String STR_ALL = "ALL" ;
    String STR_NONE = "NONE" ;
    String STR_LINK = "LINK" ;
    String STR_TO = "TO" ;
    String STR_FROM = "FROM" ;
    String STR_AFTER = "AFTER" ;
    String STR_BEFORE = "BEFORE" ;
    String STR_WITH = "WITH" ;
    String STR_ONCE = "ONCE" ;
    String STR_ALWAYS = "ALWAYS" ;
    String STR_EXCEPT = "EXCEPT" ;
    String STR_STATIC = "STATIC" ;
    String STR_ASTERIK = "*" ;
    String STR_BARRIER = "BARRIER" ;
    String STR_CBARRIER = "CBARRIER" ;
    String STR_CMD_FLUSH = "<STREAMCMD:FLUSH>" ;
    String STR_CMD_EXIT = "<STREAMCMD:EXIT>" ;
    String STR_CMD_IGNORE = "<STREAMCMD:IGNORE>" ;
    String STR_CMD_RESUME = "<STREAMCMD:RESUME>" ;
    String STR_CMD_SETVAR = "<STREAMCMD:SETVAR:" ;
    String STR_CMD_REMVAR = "<STREAMCMD:REMVAR:" ;
    String STR_DELIMITSET = "_S_DELIMITERS_" ;
    String STR_SOFTDELIMITSET = "_S_SOFT_DELIMITERS_" ;
    String STR_TEXTDELIMITSET = "_S_TEXT_DELIMITERS_" ;
    String STR_TEXTDELIM_DEFAULT = "/(^|\\n)</s/r" ;
    String STR_BEGINTAG = ">>>" ;
    String STR_ENDTAG = "<<<" ;
    String STR_UU_LEFT = "_LEFT_" ;
    String STR_UU_RIGHT = "_RIGHT_" ;
    String STR_UU_PAREN = "_PAREN_" ;
    String STR_UU_TARGET = "_TARGET_" ;
    String STR_UU_MARK = "_MARK_" ;
    String STR_UU_ATTACHTO = "_ATTACHTO_" ;
    String STR_UU_ENCL = "_ENCL_" ;
    String STR_UU_SAME_BASIC = "_SAME_BASIC_" ;
    String STR_UU_C1 = "_C1_" ;
    String STR_UU_C2 = "_C2_" ;
    String STR_UU_C3 = "_C3_" ;
    String STR_UU_C4 = "_C4_" ;
    String STR_UU_C5 = "_C5_" ;
    String STR_UU_C6 = "_C6_" ;
    String STR_UU_C7 = "_C7_" ;
    String STR_UU_C8 = "_C8_" ;
    String STR_UU_C9 = "_C9_" ;
    String STR_RXTEXT_ANY = "<.*>" ;
    String STR_RXBASE_ANY = "\".*\"" ;
    String STR_RXWORD_ANY = "\"<.*>\"" ;
    String STR_VS1_raw = "$1" ;
    String STR_VS2_raw = "$2" ;
    String STR_VS3_raw = "$3" ;
    String STR_VS4_raw = "$4" ;
    String STR_VS5_raw = "$5" ;
    String STR_VS6_raw = "$6" ;
    String STR_VS7_raw = "$7" ;
    String STR_VS8_raw = "$8" ;
    String STR_VS9_raw = "$9" ;
    String STR_VS1 = "\u00011";
    String STR_VS2 = "\u00012";
    String STR_VS3 = "\u00013";
    String STR_VS4 = "\u00014";
    String STR_VS5 = "\u00015";
    String STR_VS6 = "\u00016";
    String STR_VS7 = "\u00017";
    String STR_VS8 = "\u00018";
    String STR_VS9 = "\u00019";
    String STR_VSu_raw = "%" ;
    String STR_VSU_raw = "%" ;
    String STR_VSl_raw = "%l" ;
    String STR_VSL_raw = "%L" ;
    String STR_VSu = "\u0001" ;
    String STR_VSU = "\u0001" ;
    String STR_VSl = "\u0001l" ;
    String STR_VSL = "\u0001L" ;
    String STR_GPREFIX = "_G_" ;
    String STR_POSITIVE = "POSITIVE" ;
    String STR_NEGATIVE = "NEGATIVE" ;
    String STR_NO_ISETS = "no-inline-sets" ;
    String STR_NO_ITMPLS = "no-inline-templates" ;
    String STR_STRICT_WFORMS = "strict-wordforms" ;
    String STR_STRICT_BFORMS = "strict-baseforms" ;
    String STR_STRICT_SECOND = "strict-secondary" ;
    String STR_STRICT_REGEX = "strict-regex" ;
    String STR_STRICT_ICASE = "strict-icase" ;
    String STR_SELF_NO_BARRIER = "self-no-barrier" ;
    String STR_ORDERED = "ordered" ;
    String STR_ADDCOHORT_ATTACH = "addcohort-attach" ;
    String STR_SAFE_SETPARENT = "safe-setparent" ;
    String STR_DUMMY = "__CG3_DUMMY_STRINGBIT__";
}
