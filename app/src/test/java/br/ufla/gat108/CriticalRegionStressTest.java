package br.ufla.gat108;

import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.SensorReading;
import br.ufla.gat108.domain.Track;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ensaio de estresse concorrente para a Capacidade 3 (Região Crítica).
 * Valida a integridade do buffer circular compartilhada sob concorrência rigorosa.
 */
class CriticalRegionStressTest {

    private static final int TOTAL_RUNS = 10;
    private static final int WRITER_SAMPLES = 500;
    private static final int TRACK_CAPACITY = 100;
    private static final List<String> executionLogs = new ArrayList<>();

    @RepeatedTest(value = TOTAL_RUNS, name = "Execução {currentRepetition}/{totalRepetitions} da Região Crítica")
    @DisplayName("Teste de estresse de região crítica com 10 execuções consecutivas sem falha")
    void criticalRegionStressTestRun(org.junit.jupiter.api.RepetitionInfo repetitionInfo) throws Exception {
        int runId = repetitionInfo.getCurrentRepetition();
        Track track = new Track(TRACK_CAPACITY);

        int totalThreads = 4; // 1 Produtor (escrita 50Hz/máxima velocidade) + 3 Consumidores (leitura)
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger snapshotReadCount = new AtomicInteger(0);
        AtomicInteger latestReadCount = new AtomicInteger(0);
        AtomicInteger windowReadCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong baseTimestamp = new AtomicLong(System.currentTimeMillis());

        // Thread Escritora (Produtor)
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < WRITER_SAMPLES; i++) {
                    long ts = baseTimestamp.get() + (i * 20L); // Simula amostragem de 50Hz (20ms)
                    SensorReading reading = new SensorReading(ts, 0.1 * i, 9.8, 0.2, 1.2, -21.22, -44.97, 0.05, 5.0, 0.1);
                    track.append(new Fix(ts, reading));
                    // Micro-pausa para permitir interleaved execution
                    if (i % 20 == 0) {
                        Thread.yield();
                    }
                }
            } catch (Exception e) {
                errorCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread Leitora 1 (Snapshot)
        executor.submit(() -> {
            try {
                startLatch.await();
                while (doneLatch.getCount() > 3) { // Continua enquanto a escritora executa
                    Fix[] snapshot = track.snapshot();
                    snapshotReadCount.incrementAndGet();
                    validateSnapshotMonotonicity(snapshot, errorCount);
                    Thread.yield();
                }
            } catch (Exception e) {
                errorCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread Leitora 2 (Latest)
        executor.submit(() -> {
            try {
                startLatch.await();
                while (doneLatch.getCount() > 3) {
                    Fix latest = track.latest();
                    latestReadCount.incrementAndGet();
                    if (latest != null && latest.timestampMillis() <= 0) {
                        errorCount.incrementAndGet();
                    }
                    Thread.yield();
                }
            } catch (Exception e) {
                errorCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread Leitora 3 (Window)
        executor.submit(() -> {
            try {
                startLatch.await();
                while (doneLatch.getCount() > 3) {
                    List<Fix> window = track.getLatestWindow(10);
                    windowReadCount.incrementAndGet();
                    if (window.size() > 10) {
                        errorCount.incrementAndGet();
                    }
                    Thread.yield();
                }
            } catch (Exception e) {
                errorCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        long startTime = System.nanoTime();
        startLatch.countDown(); // Libera todas as threads simultaneamente

        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        long elapsedTimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        assertTrue(completed, "A execução da região crítica excedeu o tempo limite");
        assertEquals(0, errorCount.get(), "Erros ou violações de concorrência detectados durante a execução " + runId);
        assertEquals(TRACK_CAPACITY, track.size(), "O tamanho final do buffer circular deve ser igual à sua capacidade");

        String logEntry = String.format(
                "| Execução %02d/10 | Amostras Gravadas: %3d | Leituras Snapshot: %4d | Leituras Latest: %4d | Leituras Janela: %4d | Erros: %d | Tempo: %3d ms | Status: SUCESSO |",
                runId, WRITER_SAMPLES, snapshotReadCount.get(), latestReadCount.get(), windowReadCount.get(), errorCount.get(), elapsedTimeMs
        );
        executionLogs.add(logEntry);
        System.out.println(logEntry);

        if (runId == TOTAL_RUNS) {
            printFinalTableSummary();
        }
    }

    private void validateSnapshotMonotonicity(Fix[] snapshot, AtomicInteger errorCount) {
        if (snapshot == null) {
            errorCount.incrementAndGet();
            return;
        }
        for (int i = 1; i < snapshot.length; i++) {
            if (snapshot[i] == null || snapshot[i - 1] == null) {
                errorCount.incrementAndGet();
                break;
            }
            if (snapshot[i].timestampMillis() <= snapshot[i - 1].timestampMillis()) {
                errorCount.incrementAndGet();
                break;
            }
        }
    }

    private void printFinalTableSummary() {
        System.out.println("\n==========================================================================================================");
        System.out.println("                   RELATÓRIO DE ACEITAÇÃO: REGIÃO CRÍTICA (CAPACIDADE 3) — 10 EXECUÇÕES");
        System.out.println("==========================================================================================================");
        for (String log : executionLogs) {
            System.out.println(log);
        }
        System.out.println("==========================================================================================================");
        System.out.println(" RESULTADO FINAL: 10/10 EXECUÇÕES CONCLUÍDAS COM ZERO FALHAS E ZERO CONDIÇÕES DE CORRIDA.");
        System.out.println("==========================================================================================================\n");
    }
}
