package org.apertium.recursive;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Joan Jerez, ToBeIT
 * @serial GPL 3.0
 */

public class Pool<ElementType> {

    private final int bucketSize = 64;
    private final List<Bucket> bucketList = new ArrayList<>();
    private final Supplier<ElementType> factory;
    private int idx;
    private Bucket cur;

    private static class Bucket {
        Object[] array;
        int inUse;
        int wasUsed;

        public Bucket() {}
    }

    private void getNextBucket() {
        idx++;

        if (idx == bucketList.size()) {
            cur = new Bucket();
            cur.inUse = 0;
            cur.wasUsed = 0;
            bucketList.addLast(cur);
        } else {
            cur = bucketList.get(idx);
            cur.inUse = 0;
        }
    }

    public Pool(Supplier<ElementType> factory) {
        this.factory = factory;
        idx = 0;
        Bucket b = new Bucket();
        b.inUse = 0;
        b.wasUsed = 0;
        bucketList.addLast(b);
        cur = b;
    }

    public int size() {
        return bucketSize * (bucketList.size() - 1) + cur.inUse;
    }

    public void reset() {
        bucketList.getFirst().inUse = 0;

        while (idx > 0) {
            bucketList.removeLast();
            cur = bucketList.get(--idx);
        }
    }

    public ElementType next() {
        // 1. Verificar si el bucket actual está lleno
        if (cur.inUse == bucketSize) {
            getNextBucket();
        }

        ElementType ch = factory.get();

        if (cur.inUse > cur.wasUsed) {
            cur.wasUsed++;
        }
        return ch;
        }
}