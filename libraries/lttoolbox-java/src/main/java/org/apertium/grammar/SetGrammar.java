package org.apertium.grammar;

/**
 * @author Joan Jerez Torres, ToBeIT
 * @serial GPL 3.0
 * Port to Java language
 */

public class SetGrammar {
    int ST_ANY         = (1);
    int ST_SPECIAL     = (1 <<  1);
    int ST_TAG_UNIFY   = (1 <<  2);
    int ST_SET_UNIFY   = (1 <<  3);
    int ST_CHILD_UNIFY = (1 <<  4);
    int ST_MAPPING     = (1 <<  5);
    int ST_USED        = (1 <<  6);
    int ST_STATIC      = (1 <<  7);
    int ST_ORDERED     = (1 <<  8);

    int ST_UNIFY  = ST_TAG_UNIFY | ST_SET_UNIFY | ST_CHILD_UNIFY;
}
