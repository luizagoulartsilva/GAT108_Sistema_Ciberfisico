package br.ufla.gat108;

import br.ufla.gat108.acquisition.AcquisitionStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste unitário automatizado para a Capacidade 4 (Aquisição Periódica em Tempo Real).
 * Valida os cálculos estatísticos, frequência observada e tempo decorrido do monitor.
 */
class PeriodicAcquisitionTest {

    @Test
    @DisplayName("1. Acumulador de estatísticas deve calcular frequência e tempo decorrido corretamente")
    void acquisitionStatsMustComputeCorrectFrequencyAndElapsedTime() {
        long startTime = System.currentTimeMillis() - 2000; // Simula 2 segundos decorridos
        AcquisitionStats stats = new AcquisitionStats(startTime);

        // Simula 100 amostras a 50Hz (20ms intervalo) = 2.0 segundos
        for (int i = 0; i < 100; i++) {
            stats.recordSample(startTime + (i * 20L));
        }

        assertEquals(100, stats.getTotalSamples());
        assertTrue(stats.getElapsedSeconds() >= 1.9, "Tempo decorrido deve ser ~2 segundos");
        assertTrue(stats.getActualFrequencyHz() >= 45.0 && stats.getActualFrequencyHz() <= 55.0,
                "Frequência efetiva (" + stats.getActualFrequencyHz() + " Hz) deve estar próxima de 50 Hz");

        String summary = stats.getFormattedSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("amostras"));
        assertTrue(summary.contains("Hz"));
    }

    @Test
    @DisplayName("2. Simulação de ensaio de 30 minutos deve acumular ~90.000 amostras a 50Hz")
    void simulated30MinRunMustAccumulateTargetSamples() {
        long simulatedStartTime = System.currentTimeMillis() - (30 * 60 * 1000L); // 30 min atrás
        AcquisitionStats stats = new AcquisitionStats(simulatedStartTime);

        int totalSimulatedSamples = 30 * 60 * 50; // 90.000 amostras
        for (int i = 0; i < totalSimulatedSamples; i++) {
            stats.recordSample(simulatedStartTime + (i * 20L));
        }

        assertEquals(90000, stats.getTotalSamples());
        assertTrue(stats.getElapsedMinutes() >= 29.9, "Tempo em minutos deve ser ~30 min");
        assertEquals(50.0, stats.getActualFrequencyHz(), 0.5, "Frequência média deve ser 50.0 Hz");

        String summary = stats.getFormattedSummary();
        assertTrue(summary.contains("30:00 min") || summary.contains("29:"), "Resumo deve indicar ~30 minutos");
        assertTrue(summary.contains("90000 amostras"), "Resumo deve exibir 90.000 amostras");
    }

    @Test
    @DisplayName("3. Monitoramento de desvio de tempo (jitter) deve registrar variações do período ideal de 20ms")
    void jitterMonitoringMustCaptureIntervalDeviations() {
        long start = System.currentTimeMillis();
        AcquisitionStats stats = new AcquisitionStats(start);

        stats.recordSample(start);
        stats.recordSample(start + 20); // ideal
        stats.recordSample(start + 45); // jitter = +5ms (intervalo 25ms)
        stats.recordSample(start + 60); // jitter = -5ms (intervalo 15ms)

        assertEquals(4, stats.getTotalSamples());
        assertTrue(stats.getMaxJitterMs() >= 5, "Maior jitter registrado deve ser >= 5ms");
    }
}
