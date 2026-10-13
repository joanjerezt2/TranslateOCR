package org.apertium.recursive;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Joan Jerez, ToBeIT
 * @serial GPL 3.0
 */

public class Pool<T> {

    private final int bucketSize;
    private final List<Bucket> bucketList = new ArrayList<>();
    private final Supplier<T> factory;

    private int idx;
    private Bucket cur;

    private static class Bucket {
        final Object[] array;
        int inUse;
        int wasUsed;

        Bucket(int size) {
            array = new Object[size];
        }
    }

    public Pool(Supplier<T> factory) {
        this(factory, 64);
    }

    public Pool(Supplier<T> factory, int bucketSize) {
        this.factory = factory;
        this.bucketSize = bucketSize;

        cur = new Bucket(bucketSize);
        bucketList.add(cur);
        idx = 0;
    }

    private void getNextBucket() {
        idx++;

        if (idx == bucketList.size()) {
            cur = new Bucket(bucketSize);
            bucketList.add(cur);
        } else {
            cur = bucketList.get(idx);
            cur.inUse = 0;
        }
    }

    public int size() {
        return bucketSize * (bucketList.size() - 1)
                + cur.inUse;
    }

    public void reset() {
        bucketList.getFirst().inUse = 0;

        while (idx > 0) {
            bucketList.removeLast();
            cur = bucketList.get(--idx);
        }
    }

    @SuppressWarnings("unchecked")
    public T next() {
        if (cur.inUse == bucketSize) {
            getNextBucket();
        }

        int position = cur.inUse++;

        T value = factory.get();
        cur.array[position] = value;

        if (cur.inUse > cur.wasUsed) {
            cur.wasUsed++;
        }

        return (T) cur.array[position];
    }
}
