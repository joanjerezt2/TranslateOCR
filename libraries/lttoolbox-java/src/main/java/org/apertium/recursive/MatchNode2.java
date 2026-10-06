package org.apertium.recursive;

class Transition{
    int tag;
    int dest;
}

public class MatchNode2 {
    Transition trans;
    int size;
    int rule_begin; // region of array in MatchExe2 which
    int rule_end;

    void setSize(int sz){
        size = sz;
    }

    public void addTransition(int tag, int dest, int pos) {
        Transition[] trans = new Transition[size];
        trans[pos] = new Transition();
        trans[pos].tag = tag;
        trans[pos].dest = dest;

    }
}
