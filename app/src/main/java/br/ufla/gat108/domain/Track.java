package br.ufla.gat108.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Buffer circular de capacidade fixa para leituras do trabalhador.
 * Acesso à região crítica protegido com exclusão mútua refinada (ReentrantReadWriteLock).
 */
public final class Track {
    private final Fix[] entries;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private int nextIndex;
    private int size;
    private long latestTimestamp = -1;

    public Track(int capacity) {
        if (capacity <= 0) {
            throw new InvalidDomainException("track capacity must be positive");
        }
        entries = new Fix[capacity];
    }

    public void append(Fix fix) {
        if (fix == null) {
            throw new InvalidDomainException("track fix must not be null");
        }
        lock.writeLock().lock();
        try {
            if (fix.timestampMillis() <= latestTimestamp) {
                throw new InvalidDomainException("track timestamps must be strictly increasing");
            }
            entries[nextIndex] = fix;
            nextIndex = (nextIndex + 1) % entries.length;
            size = Math.min(size + 1, entries.length);
            latestTimestamp = fix.timestampMillis();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return size;
        } finally {
            lock.readLock().unlock();
        }
    }

    public int capacity() {
        return entries.length;
    }

    public Fix latest() {
        lock.readLock().lock();
        try {
            if (size == 0) {
                return null;
            }
            return entries[(nextIndex - 1 + entries.length) % entries.length];
        } finally {
            lock.readLock().unlock();
        }
    }

    public Fix[] snapshot() {
        lock.readLock().lock();
        try {
            Fix[] result = new Fix[size];
            int start = (nextIndex - size + entries.length) % entries.length;
            for (int i = 0; i < size; i++) {
                result[i] = entries[(start + i) % entries.length];
            }
            return Arrays.copyOf(result, result.length);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Retorna as N amostras mais recentes para reconciliação (cópia defensiva imutável). */
    public List<Fix> getLatestWindow(int windowSize) {
        if (windowSize <= 0) return Collections.emptyList();
        Fix[] all = snapshot();
        int actualSize = Math.min(all.length, windowSize);
        return Collections.unmodifiableList(
                Arrays.asList(Arrays.copyOfRange(all, all.length - actualSize, all.length))
        );
    }

    /** Esvazia o buffer de acompanhamento. */
    public void clear() {
        lock.writeLock().lock();
        try {
            Arrays.fill(entries, null);
            nextIndex = 0;
            size = 0;
            latestTimestamp = -1;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
