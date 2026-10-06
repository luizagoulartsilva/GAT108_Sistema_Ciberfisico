package br.ufla.gat108;

import br.ufla.gat108.domain.Event;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.SensorReading;
import br.ufla.gat108.simulation.DeterministicReplayEngine;
import br.ufla.gat108.simulation.FaultInjector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Ensaio automatizado para geração da TABELA DE SENSIBILIDADE (Capacidade 5).
 * Avalia quantitativamente a resiliência da detecção de queda à medida que
 * a intensidade de ruído Gaussiano e a taxa de perda de pacotes aumentam.
 */
class SensitivityAnalysisTest {

    private static final long TEST_SEED = 12345L;

    /** Prepara uma bateria de 10 ensaios com queda e 10 ensaios de caminhada normal sem queda. */
    private List<List<Fix>> createFallTrials() {
        List<List<Fix>> trials = new ArrayList<>();
        long baseTime = 1700000000000L;

        for (int t = 0; t < 10; t++) {
            List<Fix> trial = new ArrayList<>();
            long startTime = baseTime + (t * 10000L);

            // Caminhada normal
            for (int i = 0; i < 10; i++) {
                long time = startTime + (i * 20L);
                trial.add(new Fix(time, new SensorReading(time, 0.2, 0.1, 9.8, 1.2, -21.2, -44.9, 0.1, 3.0, 0.1)));
            }
            // Impacto de queda (a_z ~ 34.0 m/s^2)
            long fallTime = startTime + 200L;
            trial.add(new Fix(fallTime, new SensorReading(fallTime, 2.0, 3.0, 34.0, 0.1, -21.2, -44.9, 0.1, 3.0, 0.1)));

            // Postura imóvel
            for (int i = 1; i <= 5; i++) {
                long time = fallTime + (i * 20L);
                trial.add(new Fix(time, new SensorReading(time, 0.05, 0.05, 9.8, 0.0, -21.2, -44.9, 0.1, 3.0, 0.1)));
            }
            trials.add(trial);
        }
        return trials;
    }

    private List<List<Fix>> createNormalWalkTrials() {
        List<List<Fix>> trials = new ArrayList<>();
        long baseTime = 1700000500000L;

        for (int t = 0; t < 10; t++) {
            List<Fix> trial = new ArrayList<>();
            long startTime = baseTime + (t * 10000L);

            for (int i = 0; i < 15; i++) {
                long time = startTime + (i * 20L);
                // Oscilação de caminhada normal (< 15.0 m/s^2)
                trial.add(new Fix(time, new SensorReading(time, 0.3, -0.2, 10.2, 1.3, -21.2, -44.9, 0.1, 3.0, 0.1)));
            }
            trials.add(trial);
        }
        return trials;
    }

    @Test
    @DisplayName("Construção e impressão da Tabela de Sensibilidade (Taxa de acertos vs Severidade de Falhas)")
    void generateSensitivityTableReport() {
        List<List<Fix>> fallTrials = createFallTrials();
        List<List<Fix>> normalTrials = createNormalWalkTrials();

        double[] noiseSigmas = {0.0, 0.5, 1.0, 2.0, 5.0, 10.0};
        double[] dropRates = {0.0, 0.10, 0.25, 0.50};

        List<String> reportRows = new ArrayList<>();

        System.out.println("\n=============================================================================================================");
        System.out.println("                         TABELA DE SENSIBILIDADE (CAPACIDADE 5) — DETECÇÃO DE QUEDAS");
        System.out.println("=============================================================================================================");
        System.out.println("| Ruído (sigma) | Perda Pacotes | TP / Total Quedas | FP / Total Limpos | Sensibilidade | Precisão | F1-Score |");
        System.out.println("|---------------|---------------|-------------------|-------------------|---------------|----------|----------|");

        for (double sigma : noiseSigmas) {
            for (double drop : dropRates) {
                FaultInjector injector = new FaultInjector(TEST_SEED, sigma, drop, 0.0);
                DeterministicReplayEngine engine = new DeterministicReplayEngine(injector);

                // Avalia Casos Positivos (Quedas)
                int tp = 0;
                for (List<Fix> trial : fallTrials) {
                    List<Event> events = engine.replay(trial, 50);
                    boolean detectedFall = hasFallEvent(events);
                    if (detectedFall) {
                        tp++;
                    }
                }

                // Avalia Casos Negativos (Caminhada Limpa)
                int fp = 0;
                for (List<Fix> trial : normalTrials) {
                    List<Event> events = engine.replay(trial, 50);
                    boolean falseAlarm = hasFallEvent(events);
                    if (falseAlarm) {
                        fp++;
                    }
                }

                int totalPositives = fallTrials.size();
                int totalNegatives = normalTrials.size();

                double recall = (double) tp / totalPositives;
                double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 1.0;
                double f1 = (precision + recall) > 0 ? (2 * precision * recall) / (precision + recall) : 0.0;

                String row = String.format(Locale.US,
                        "| %5.1f m/s²   | %11.0f%% | %8d / %-2d   | %7d / %-2d   | %12.1f%% | %7.1f%% | %7.1f%% |",
                        sigma, drop * 100, tp, totalPositives, fp, totalNegatives, recall * 100, precision * 100, f1 * 100
                );
                reportRows.add(row);
                System.out.println(row);
            }
        }
        System.out.println("=============================================================================================================\n");

        assertFalse(reportRows.isEmpty());
        assertNotNull(reportRows.get(0));
    }

    private boolean hasFallEvent(List<Event> events) {
        if (events == null) return false;
        for (Event event : events) {
            if (event != null && event.type() == Event.EventType.FALL) {
                return true;
            }
        }
        return false;
    }
}
