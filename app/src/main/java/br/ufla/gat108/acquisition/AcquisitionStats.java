package br.ufla.gat108.acquisition;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitor e acumulador de métricas para aquisição periódica em tempo real (Capacidade 4).
 * Acompanha tempo decorrido, total de amostras, frequência observada e variação de tempo (jitter).
 */
public class AcquisitionStats {

    public static final double TARGET_FREQUENCY_HZ = 50.0; // Amostragem a 50Hz (20ms)
    public static final long TARGET_PERIOD_MS = 20L;

    private final long startTimeMillis;
    private final AtomicLong totalSamples = new AtomicLong(0);
    private final AtomicLong lastSampleTimestamp = new AtomicLong(0);
    private final AtomicLong maxJitterMs = new AtomicLong(0);
    private final AtomicLong totalJitterMs = new AtomicLong(0);

    public AcquisitionStats() {
        this.startTimeMillis = System.currentTimeMillis();
    }

    public AcquisitionStats(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    /** Registra uma nova amostra no loop de aquisição e calcula o desvio de tempo. */
    public void recordSample(long sampleTimestamp) {
        long count = totalSamples.incrementAndGet();
        long previous = lastSampleTimestamp.getAndSet(sampleTimestamp);

        if (count > 1 && previous > 0) {
            long delta = sampleTimestamp - previous;
            long jitter = Math.abs(delta - TARGET_PERIOD_MS);
            totalJitterMs.addAndGet(jitter);

            long currentMax;
            do {
                currentMax = maxJitterMs.get();
                if (jitter <= currentMax) break;
            } while (!maxJitterMs.compareAndSet(currentMax, jitter));
        }
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getTotalSamples() {
        return totalSamples.get();
    }

    public long getElapsedMillis() {
        return Math.max(0, System.currentTimeMillis() - startTimeMillis);
    }

    public double getElapsedSeconds() {
        return getElapsedMillis() / 1000.0;
    }

    public double getElapsedMinutes() {
        return getElapsedSeconds() / 60.0;
    }

    /** Calcula a taxa efetiva de amostragem em Hz. */
    public double getActualFrequencyHz() {
        double seconds = getElapsedSeconds();
        if (seconds <= 0.1) {
            return 0.0;
        }
        return totalSamples.get() / seconds;
    }

    public double getAverageJitterMs() {
        long count = totalSamples.get();
        if (count <= 1) return 0.0;
        return (double) totalJitterMs.get() / (count - 1);
    }

    public long getMaxJitterMs() {
        return maxJitterMs.get();
    }

    /** Retorna resumo formatado para exibição na notificação e UI. */
    public String getFormattedSummary() {
        long elapsedSec = (long) getElapsedSeconds();
        long minutes = elapsedSec / 60;
        long seconds = elapsedSec % 60;
        return String.format(java.util.Locale.US, "%02d:%02d min | %d amostras | %.1f Hz",
                minutes, seconds, totalSamples.get(), getActualFrequencyHz());
    }
}
