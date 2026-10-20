package org.apertium.grammar;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class ContextualTest {

    int POS_CAREFUL        = (1); // C
    int POS_NEGATE         = (1 <<  1); // Prefix NEGATE
    int POS_NOT            = (1 <<  2); // Prefix NOT
    int POS_SCANFIRST      = (1 <<  3); // *
    int POS_SCANALL        = (1 <<  4); // **
    int POS_ABSOLUTE       = (1 <<  5); // @
    int POS_SPAN_RIGHT     = (1 <<  6); // >
    int POS_SPAN_LEFT      = (1 <<  7); // <
    int POS_SPAN_BOTH      = (1 <<  8); // W
    int POS_DEP_PARENT     = (1 <<  9); // p
    int POS_DEP_SIBLING    = (1 << 10); // s
    int POS_DEP_CHILD      = (1 << 11); // c
    int POS_PASS_ORIGIN    = (1 << 12); // o
    int POS_NO_PASS_ORIGIN = (1 << 13); // O
    int POS_LEFT_PAR       = (1 << 14); // L
    int POS_RIGHT_PAR      = (1 << 15); // R
    int POS_SELF           = (1 << 16); // S
    int POS_NONE           = (1 << 17); // Prefix NONE
    int POS_ALL            = (1 << 18); // Prefix ALL
    int POS_DEP_DEEP       = (1 << 19); // * or **
    int POS_MARK_SET       = (1 << 20); // X
    int POS_JUMP           = (1 << 21); // x, jM, jA, jT, jCn
    int POS_LOOK_DELETED   = (1 << 22); // D
    int  POS_LOOK_DELAYED   = (1 << 23); // d
    int POS_TMPL_OVERRIDE  = (1 << 24);
    int POS_UNKNOWN        = (1 << 25); // ?
    int POS_RELATION       = (1 << 26); // r:
    int POS_ATTACH_TO      = (1 << 27); // A
    int POS_NUMERIC_BRANCH = (1 << 28); // f
    int POS_BAG_OF_TAGS    = (1 << 29); // B
    int POS_DEP_GLOB       = (1 << 30); // pp or cc
    int POS_64BIT          = (1 << 31);
    long POS_LEFT           = (1L << 32); // l
    long POS_RIGHT          = (1L << 33); // r
    long POS_LEFTMOST       = (1L << 34); // ll
    long POS_RIGHTMOST      = (1L << 35); // rr
    long POS_NO_BARRIER     = (1L << 36); // N
    long POS_WITH           = (1L << 37); // w
    long POS_LOOK_IGNORED   = (1L << 38); // I
    long POS_INACTIVE       = (1L << 39); // t
    long  POS_ACTIVE         = (1L << 40); // T

    int MASK_POS_DEP       = POS_DEP_PARENT | POS_DEP_SIBLING | POS_DEP_CHILD | POS_DEP_GLOB;
    int MASK_POS_DEPREL    = MASK_POS_DEP | POS_RELATION;
    int MASK_POS_CDEPREL   = MASK_POS_DEPREL | POS_CAREFUL;
    long MASK_POS_LORR      = POS_LEFT | POS_RIGHT | POS_LEFTMOST | POS_RIGHTMOST;
    int MASK_POS_SCAN      = POS_SCANFIRST | POS_SCANALL | POS_DEP_DEEP | POS_DEP_GLOB;
    long MASK_SELF_NB       = POS_SELF | POS_NO_BARRIER;



}
