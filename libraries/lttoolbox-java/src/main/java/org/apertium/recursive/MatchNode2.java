package org.apertium.recursive;

/**
 * @author Joan Jerez, ToBeIT
 * @serial GPL 3.0
 */

public class MatchNode2 {

    private static class Transition{
        int tag;
        int dest;
    }

    Transition[] trans;
    private int size = 0;
    public int rule_begin = -1;
    public int rule_end = -1;

    void setSize(int sz){
        size = sz;
        trans = new Transition[sz];
    }

    public void addTransition(int tag, int dest, int pos) {
        trans[pos] = new Transition();
        trans[pos].tag = tag;
        trans[pos].dest = dest;

    }

    public int search(int tag){
        int left = 0, right = size-1;
        while(left <= right)
        {
            int mid = (left+right)/2;
            if(trans[mid].tag == tag)
            {
                return trans[mid].dest;
            }
            if(trans[mid].tag > tag)
            {
                right = mid - 1;
            }
            else
            {
                left = mid + 1;
            }
        }

        return -1;
    }
}

