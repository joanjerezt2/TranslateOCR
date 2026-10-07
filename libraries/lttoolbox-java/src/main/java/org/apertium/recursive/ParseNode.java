package org.apertium.recursive;

import org.apertium.lttoolbox.Pair;

import java.util.Deque;
import java.util.HashMap;
import java.util.List;

/**
 * @author Joan Jerez, ToBeIT
 * @serial GPL 3.0
 */

public class ParseNode {

    public final int RTXStateSize = 128;

    int[] state = new int[RTXStateSize];
    int first = 0;
    int last = 0;
    Chunk chunk;
    public int length;
    ParseNode prev;
    MatchExe2 mx;
    public double weight;
    int firstWord;
    public int lastWord = 0;
    public HashMap<String, String> stringVars;
    public HashMap<String, String> wblankVars;
    public int id = -1;
    public List<Chunk> chunkVars;

    public void init(MatchExe2 m, Chunk ch, double w){
        firstWord = 0;
        lastWord = 0;
        chunk = ch;
        length = 1;
        prev = null;
        mx = m;
        weight = w;
        if(chunk.isBlank)
        {
            mx.matchBlank(state, first, last);
        }
        else
        {
            mx.matchChunk(state, first, last, chunk.matchSurface());
        }
    }

    public void init(MatchExe2 mx, Chunk next){
        init(mx, next,0);
    }

    public void init(ParseNode prevNode, Chunk next, double w){
        chunk = next;
        prev = prevNode;
        firstWord = prev.lastWord + 1;
        lastWord = firstWord;
        for(int i = prevNode.first; i != prevNode.last; i = (i+1)%RTXStateSize)
        {
            state[last++] = prevNode.state[i];
        }
        mx = prevNode.mx;
        length = prev.length+1;
        stringVars = prev.stringVars;
        wblankVars = prev.wblankVars;
        chunkVars = prev.chunkVars;
        weight = (w == 0) ? prev.weight : w;
        if(next.isBlank)
        {
            mx.matchBlank(state, first, this.last);
        }
        else
        {
            mx.matchChunk(state, first, this.last, chunk.matchSurface());
        }
    }

    public void init(ParseNode p, Chunk next){
        init(p, next,0);
    }


    public void init(ParseNode prevNode, Chunk next, boolean prepared) {
        chunk = next;
        prev = prevNode;
        for(int i = prevNode.first; i != prevNode.last; i = (i+1)%RTXStateSize)
        {
            state[last++] = prevNode.state[i];
        }
        mx = prevNode.mx;
        length = prev.length+1;
        weight = prev.weight;
        firstWord = prev.lastWord+1;
        lastWord = firstWord;
        stringVars = prev.stringVars;
        wblankVars = prev.wblankVars;
        chunkVars = prev.chunkVars;
        if(next.isBlank)
        {
            mx.matchBlank(state, first, last);
        }
        else if(prepared)
        {
            mx.matchPreparedChunk(state, first, last);
        }
        else
        {
            mx.matchChunk(state, first, last, chunk.matchSurface());
        }
    }

    public void init(ParseNode other){
        for(int i = other.first; i != other.last; i = (i+1)%RTXStateSize)
        {
            state[last++] = other.state[i];
        }
        chunk = other.chunk;
        length = other.length;
        prev = other.prev;
        weight = other.weight;
        mx = other.mx;
        firstWord = other.firstWord;
        lastWord = other.lastWord;
        stringVars = other.stringVars;
        wblankVars = other.wblankVars;
        chunkVars = other.chunkVars;
    }

    void getChunks(Deque<Chunk> chls, int count)
    {
        chls.addFirst(chunk);
        if(count == 0) return;
        prev.getChunks(chls, count-1);
    }
    void getChunks(List<Chunk> chls, int count)
    {
        chls.set(count, chunk);
        if(count == 0) return;
        prev.getChunks(chls, count-1);
    }

    ParseNode popNodes(int n)
    {
        if(n == 1 && prev == null) return null;
        if(n == 0) return this;
        return prev.popNodes(n-1);
    }

    public Pair<Integer, Double> getRule()
    {
        return mx.getRule(state, first, last);
    }
    public boolean shouldShift()
    {
        return mx.shouldShift(state, first, last);
    }
    public boolean shouldShift(Chunk next)
    {
        return mx.shouldShift(state, first, last, next.matchSurface());
    }
    public boolean isDone()
    {
        return (first == last);
    }
}

