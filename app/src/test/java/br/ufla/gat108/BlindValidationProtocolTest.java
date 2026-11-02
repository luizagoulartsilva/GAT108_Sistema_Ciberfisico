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
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Implementação do Protocolo Cego de Validação e Métricas (Capacidade 10).
 * Avalia o sistema em um conjunto de dados retido out-of-sample nunca utilizado
 * durante o treinamento ou sintonia de parâmetros, extraindo a matriz de confusão
 * e métricas estatísticas (Acurácia, Precisão, Sensibilidade, Especificidade, F1-Score).
 */
class BlindValidationProtocolTest {

    private static final long BLIND_SEED = 999888777L;

    @Test
    @DisplayName("Protocolo Cego: Validação de Quedas e Imobilidade com Dados Retidos Out-Of-Sample")
    void executeBlindValidationProtocol() {
        // 1. Geração de Janelas de Teste Cego Retidas (50 quedas simuladas e 50 caminhadas normais)
        Random rand = new Random(BLIND_SEED);
        List<List<Fix>> blindFallScenarios = generateBlindFallScenarios(rand, 50);
        List<List<Fix>> blindNormalScenarios = generateBlindNormalScenarios(rand, 50);

        FaultInjector injector = new FaultInjector(BLIND_SEED, 0.0, 0.0, 0.0);
        DeterministicReplayEngine engine = new DeterministicReplayEngine(injector);

        // Contadores para Matriz de Confusão
        int tp = 0; // Queda real detectada
        int fn = 0; // Queda real não detectada (falso negativo)
        int tn = 0; // Caminhada normal sem alarme (verdadeiro negativo)
        int fp = 0; // Caminhada normal com alarme falso (falso positivo)

        // Avaliação Cega de Quedas (Casos Positivos)
        for (List<Fix> scenario : blindFallScenarios) {
            List<Event> events = engine.replay(scenario, 50);
            boolean detected = hasFallEvent(events);
            if (detected) {
                tp++;
            } else {
                fn++;
            }
        }

        // Avaliação Cega de Caminhadas Normais (Casos Negativos)
        for (List<Fix> scenario : blindNormalScenarios) {
            List<Event> events = engine.replay(scenario, 50);
            boolean detected = hasFallEvent(events);
            if (detected) {
                fp++;
            } else {
                tn++;
            }
        }

        // 2. Cálculo das Métricas Quantitativas de Classificação
        int totalPositives = tp + fn;
        int totalNegatives = tn + fp;
        int totalSamples = totalPositives + totalNegatives;

        double accuracy = (double) (tp + tn) / totalSamples;
        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 1.0;
        double sensitivity = totalPositives > 0 ? (double) tp / totalPositives : 1.0; // Recall / True Positive Rate
        double specificity = totalNegatives > 0 ? (double) tn / totalNegatives : 1.0; // True Negative Rate
        double f1Score = (precision + sensitivity) > 0 ? (2 * precision * sensitivity) / (precision + sensitivity) : 0.0;

        // 3. Exibição Consolidada do Relatório do Protocolo Cego
        System.out.println("\n=================================================================================");
        System.out.println("                 PROTOCOLO CEGO DE VALIDAÇÃO (CAPACIDADE 10)");
        System.out.println("=================================================================================");
        System.out.printf("Total de Amostras Retidas (Out-Of-Sample): %d (50 Quedas, 50 Normais)\n", totalSamples);
        System.out.printf("Verdadeiros Positivos (TP): %d | Falsos Negativos (FN): %d\n", tp, fn);
        System.out.printf("Verdadeiros Negativos (TN): %d | Falsos Positivos (FP): %d\n", tn, fp);
        System.out.println("---------------------------------------------------------------------------------");
        System.out.printf("Acurácia:       %.2f%%\n", accuracy * 100);
        System.out.printf("Precisão:       %.2f%%\n", precision * 100);
        System.out.printf("Sensibilidade:  %.2f%% (Recall)\n", sensitivity * 100);
        System.out.printf("Especificidade: %.2f%%\n", specificity * 100);
        System.out.printf("F1-Score:       %.2f%%\n", f1Score * 100);
        System.out.println("=================================================================================\n");

        // Critérios rigorosos de aceitação TRL 4+
        assertTrue(accuracy >= 0.90, "Acurácia no protocolo cego deve ser >= 90%");
        assertTrue(f1Score >= 0.90, "F1-Score no protocolo cego deve ser >= 90%");
        assertTrue(sensitivity >= 0.85, "Sensibilidade deve ser >= 85%");
        assertTrue(specificity >= 0.85, "Especificidade deve ser >= 85%");
    }

    private boolean hasFallEvent(List<Event> events) {
        if (events == null) return false;
        for (Event e : events) {
            if (e != null && e.type() == Event.EventType.FALL) {
                return true;
            }
        }
        return false;
    }

    private List<List<Fix>> generateBlindFallScenarios(Random rand, int count) {
        List<List<Fix>> scenarios = new ArrayList<>();
        long baseTime = 1800000000000L;

        for (int c = 0; c < count; c++) {
            List<Fix> scenario = new ArrayList<>();
            long t0 = baseTime + (c * 15000L);

            // Pré-queda com ruído estocástico usando rand
            for (int i = 0; i < 8; i++) {
                long time = t0 + (i * 20L);
                double ax = (rand.nextDouble() - 0.5) * 0.2;
                double ay = (rand.nextDouble() - 0.5) * 0.2;
                double az = 9.8 + (rand.nextDouble() - 0.5) * 0.2;
                scenario.add(new Fix(time, new SensorReading(time, ax, ay, az, 1.2, -21.229, -44.978, 0.1, 3.0, 0.1)));
            }

            // Impacto de queda (az ~ 35 m/s^2)
            long fallTime = t0 + 160L;
            scenario.add(new Fix(fallTime, new SensorReading(fallTime, 2.0, 3.0, 35.0, 0.1, -21.229, -44.978, 0.1, 3.0, 0.1)));

            // Pós-queda imóvel
            for (int i = 1; i <= 15; i++) {
                long time = fallTime + (i * 200L);
                scenario.add(new Fix(time, new SensorReading(time, 0.05, 0.05, 9.8, 0.0, -21.229, -44.978, 0.1, 3.0, 0.1)));
            }
            scenarios.add(scenario);
        }
        return scenarios;
    }

    private List<List<Fix>> generateBlindNormalScenarios(Random rand, int count) {
        List<List<Fix>> scenarios = new ArrayList<>();
        long baseTime = 1800005000000L;

        for (int c = 0; c < count; c++) {
            List<Fix> scenario = new ArrayList<>();
            long t0 = baseTime + (c * 15000L);

            for (int i = 0; i < 20; i++) {
                long time = t0 + (i * 20L);
                double ax = (rand.nextDouble() - 0.5) * 1.0;
                double ay = (rand.nextDouble() - 0.5) * 1.0;
                double az = 10.2 + (rand.nextDouble() - 0.5) * 1.0;
                scenario.add(new Fix(time, new SensorReading(time, ax, ay, az, 1.3, -21.235, -44.982, 0.1, 3.0, 0.1)));
            }
            scenarios.add(scenario);
        }
        return scenarios;
    }
}
