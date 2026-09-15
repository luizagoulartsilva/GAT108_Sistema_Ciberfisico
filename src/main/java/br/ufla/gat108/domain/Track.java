package br.ufla.gat108.domain;

import java.util.Arrays;

/** Buffer circular de capacidade fixa para leituras do trabalhador. */
public final class Track {
    private final Fix[] entries;
    private int nextIndex;
    private int size;
    private long latestTimestamp = -1;

    public Track(int capacity) {
        if (capacity <= 0) {
            throw new InvalidDomainException("track capacity must be positive");
        }
        entries = new Fix[capacity];
    }

    public synchronized void append(Fix fix) {
        if (fix == null) {
            throw new InvalidDomainException("track fix must not be null");
        }
        if (fix.timestampMillis() <= latestTimestamp) {
            throw new InvalidDomainException("track timestamps must be strictly increasing");
        }
        entries[nextIndex] = fix;
        nextIndex = (nextIndex + 1) % entries.length;
        size = Math.min(size + 1, entries.length);
        latestTimestamp = fix.timestampMillis();
    }

    public synchronized int size() {
        return size;
    }

    public int capacity() {
        return entries.length;
    }

    public synchronized Fix latest() {
        if (size == 0) {
            return null;
        }
        return entries[(nextIndex - 1 + entries.length) % entries.length];
    }

    public synchronized Fix[] snapshot() {
        Fix[] result = new Fix[size];
        int start = (nextIndex - size + entries.length) % entries.length;
        for (int i = 0; i < size; i++) {
            result[i] = entries[(start + i) % entries.length];
        }
        return Arrays.copyOf(result, result.length);
    }
}
