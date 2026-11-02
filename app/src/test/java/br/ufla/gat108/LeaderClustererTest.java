package br.ufla.gat108;

import br.ufla.gat108.domain.InvalidDomainException;
import br.ufla.gat108.domain.Place;
import br.ufla.gat108.domain.RoutineCluster;
import br.ufla.gat108.domain.RoutinePrediction;
import br.ufla.gat108.learning.LeaderClusterer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderClustererTest {

    private LeaderClusterer clusterer;

    // Posto 1: Guarita Principal (08:00 UTC = 28.800.000 ms)
    private static final double LAT_POSTO_1 = -21.229000;
    private static final double LON_POSTO_1 = -44.978000;
    private static final long TIME_08_00_MS = 8 * 3600 * 1000L; // 08:00 AM in UTC

    // Posto 2: Refeitório (12:00 UTC = 43.200.000 ms)
    private static final double LAT_POSTO_2 = -21.231000;
    private static final double LON_POSTO_2 = -44.980000;
    private static final long TIME_12_00_MS = 12 * 3600 * 1000L; // 12:00 PM in UTC

    @BeforeEach
    void setUp() {
        clusterer = new LeaderClusterer(20, 100.0, 60.0);
    }

    @Test
    @DisplayName("1. Formação incremental de clusters líderes e adaptação de centroide")
    void incrementalClusterFormationAndCentroidAdaptation() {
        Place initialPlace = new Place(LAT_POSTO_1, LON_POSTO_1, TIME_08_00_MS);
        RoutineCluster cluster1 = clusterer.trainIncremental(initialPlace, 1800.0);

        assertNotNull(cluster1);
        assertEquals(1, clusterer.activeClusterCount());
        assertEquals(1L, cluster1.count());
        assertEquals(LAT_POSTO_1, cluster1.centerLatitude(), 0.000001);

        // Treina segunda amostragem com leve deslocamento no mesmo posto (ex: 10 metros de distância)
        Place secondPlace = new Place(LAT_POSTO_1 + 0.00005, LON_POSTO_1 + 0.00005, TIME_08_00_MS + 120000);
        RoutineCluster updatedCluster = clusterer.trainIncremental(secondPlace, 1800.0);

        assertEquals(1, clusterer.activeClusterCount(), "Deve manter o mesmo cluster sem criar outro");
        assertEquals(2L, updatedCluster.count());
        assertTrue(updatedCluster.centerLatitude() > LAT_POSTO_1, "Centroide de latitude deve ter sido ajustado pela média móvel");
    }

    @Test
    @DisplayName("2. Predição de imobilidade rotineira vs. anomalia fora do envelope")
    void routineVsAnomalyPrediction() {
        // Treina o modelo com o Posto 1 (08:00) e Posto 2 (12:00)
        for (int i = 0; i < 5; i++) {
            clusterer.trainIncremental(new Place(LAT_POSTO_1, LON_POSTO_1, TIME_08_00_MS + i * 1000), 1800.0);
            clusterer.trainIncremental(new Place(LAT_POSTO_2, LON_POSTO_2, TIME_12_00_MS + i * 1000), 2700.0);
        }

        assertEquals(2, clusterer.activeClusterCount());

        // Teste 1: Observação próxima ao Posto 1 às 08:05 AM -> Deve ser classificada como Rotina
        Place routinePlace = new Place(LAT_POSTO_1 + 0.00002, LON_POSTO_1 + 0.00002, TIME_08_00_MS + 300000);
        RoutinePrediction routinePred = clusterer.predict(routinePlace, 1800.0);

        assertTrue(routinePred.isRoutine(), "Imobilidade próxima ao Posto 1 no horário previsto deve ser rotineira");
        assertNotNull(routinePred.matchedCluster());
        assertTrue(routinePred.confidenceScore() > 0.70);

        // Teste 2: Observação em local totalmente remoto (ex: 3 km de distância) -> Deve ser Anomalia
        Place anomalousPlace = new Place(-21.260000, -44.950000, TIME_08_00_MS);
        RoutinePrediction anomalyPred = clusterer.predict(anomalousPlace, 1800.0);

        assertFalse(anomalyPred.isRoutine(), "Imobilidade em local distante de todos os postos deve ser anômala");
        assertNull(anomalyPred.matchedCluster());
    }

    @Test
    @DisplayName("3. Validação fora da amostra (out-of-sample) extraindo Precisão, Recall, Acurácia e F1-Score (>90%)")
    void outOfSampleValidationMetrics() {
        // 1. Fase de Treinamento (Aprende 3 postos de rotina)
        long baseTime = 8 * 3600 * 1000L;
        for (int i = 0; i < 10; i++) {
            clusterer.trainIncremental(new Place(-21.2290, -44.9780, baseTime), 1800.0); // Posto A
            clusterer.trainIncremental(new Place(-21.2350, -44.9820, baseTime + 4 * 3600 * 1000L), 2400.0); // Posto B
            clusterer.trainIncremental(new Place(-21.2400, -44.9850, baseTime + 8 * 3600 * 1000L), 1200.0); // Posto C
        }

        // 2. Construção da Amostra de Validação Retida (Out-Of-Sample): 50 Casos de Rotina + 50 Casos Anômalos
        int truePositives = 0;
        int falsePositives = 0;
        int trueNegatives = 0;
        int falseNegatives = 0;

        Random rand = new Random(42); // Semente fixa para reprodutibilidade

        // 50 Amostras Retidas de Rotina (Com pequeno ruído em torno dos postos/horários)
        for (int i = 0; i < 50; i++) {
            double latNoise = (rand.nextDouble() - 0.5) * 0.0003; // ~30m
            double lonNoise = (rand.nextDouble() - 0.5) * 0.0003;
            long timeNoiseMs = (long) ((rand.nextDouble() - 0.5) * 20 * 60 * 1000); // +/- 10 min

            double baseLat = (i % 3 == 0) ? -21.2290 : ((i % 3 == 1) ? -21.2350 : -21.2400);
            double baseLon = (i % 3 == 0) ? -44.9780 : ((i % 3 == 1) ? -44.9820 : -44.9850);
            long baseT = (i % 3 == 0) ? baseTime : ((i % 3 == 1) ? baseTime + 4 * 3600 * 1000L : baseTime + 8 * 3600 * 1000L);

            Place testPlace = new Place(baseLat + latNoise, baseLon + lonNoise, Math.max(0, baseT + timeNoiseMs));
            RoutinePrediction pred = clusterer.predict(testPlace, 1800.0);

            if (pred.isRoutine()) {
                truePositives++;
            } else {
                falseNegatives++;
            }
        }

        // 50 Amostras Retidas Anômalas (Locais isolados desconhecidos ou horários incompatíveis)
        for (int i = 0; i < 50; i++) {
            double remoteLat = -21.2800 + (rand.nextDouble() * 0.05); // Longe dos postos
            double remoteLon = -44.9000 + (rand.nextDouble() * 0.05);
            long timeMs = (long) (rand.nextDouble() * 24 * 3600 * 1000);

            Place testPlace = new Place(remoteLat, remoteLon, timeMs);
            RoutinePrediction pred = clusterer.predict(testPlace, 1800.0);

            if (pred.isRoutine()) {
                falsePositives++;
            } else {
                trueNegatives++;
            }
        }

        // 3. Cálculo das Métricas Quantitativas
        double precision = (double) truePositives / (truePositives + falsePositives);
        double recall = (double) truePositives / (truePositives + falseNegatives);
        double accuracy = (double) (truePositives + trueNegatives) / (truePositives + falsePositives + trueNegatives + falseNegatives);
        double f1Score = (2 * precision * recall) / (precision + recall);

        System.out.printf("--- MÉTRICAS DE VALIDAÇÃO OUT-OF-SAMPLE ---\n");
        System.out.printf("Acurácia:     %.2f%%\n", accuracy * 100);
        System.out.printf("Precisão:     %.2f%%\n", precision * 100);
        System.out.printf("Sensibilidade:%.2f%%\n", recall * 100);
        System.out.printf("F1-Score:     %.2f%%\n", f1Score * 100);

        assertTrue(accuracy >= 0.90, "Acurácia deve ser >= 90%");
        assertTrue(f1Score >= 0.90, "F1-Score deve ser >= 90%");
    }

    @Test
    @DisplayName("4. Teste de estresse de memória constante O(1) e vazamento de RAM (15.000 amostras contínuas)")
    void constantMemoryAndNoLeakStressTest() {
        int sampleCount = 15_000;
        Random rand = new Random(12345);

        // Força coleta de lixo inicial para baseline
        System.gc();
        long initialHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < sampleCount; i++) {
            double lat = -21.2000 + (rand.nextDouble() * 0.1);
            double lon = -44.9000 + (rand.nextDouble() * 0.1);
            long timestamp = i * 2000L; // Cada 2 segundos

            Place place = new Place(lat, lon, timestamp);
            clusterer.trainIncremental(place, 600.0);

            if (i % 100 == 0) {
                clusterer.predict(place, 600.0);
            }
        }
        long elapsedTime = System.currentTimeMillis() - startTime;

        System.gc();
        long finalHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long heapDifferenceBytes = Math.abs(finalHeap - initialHeap);

        System.out.printf("--- RESULTADO DO ENSAIO DE MEMÓRIA CONSTANTE O(1) ---\n");
        System.out.printf("Amostras processadas: %d\n", sampleCount);
        System.out.printf("Tempo total:           %d ms (%.2f ms/amostra)\n", elapsedTime, (double) elapsedTime / sampleCount);
        System.out.printf("Clusters ativos:       %d/%d (Capped at Max Capacity)\n", clusterer.activeClusterCount(), LeaderClusterer.DEFAULT_MAX_CLUSTERS);
        System.out.printf("Variação do Heap:      %d bytes (%.2f KB)\n", heapDifferenceBytes, heapDifferenceBytes / 1024.0);

        assertEquals(20, clusterer.activeClusterCount(), "Quantidade de clusters ativos deve ter atingido e mantido o limite K=20");
        assertTrue(heapDifferenceBytes < 10 * 1024 * 1024, "Variação do heap deve ser insignificante (< 10 MB) indicando memória estritamente constante O(1)");
    }

    @Test
    @DisplayName("5. Validação de invariantes do domínio e exceções do clusterer")
    void domainInvariantsAndExceptions() {
        assertThrows(InvalidDomainException.class, () -> new LeaderClusterer(0, 100.0, 60.0));
        assertThrows(InvalidDomainException.class, () -> new LeaderClusterer(20, -10.0, 60.0));
        assertThrows(InvalidDomainException.class, () -> new LeaderClusterer(20, 100.0, -1.0));

        assertThrows(InvalidDomainException.class, () -> clusterer.trainIncremental(null, 100.0));
        assertThrows(InvalidDomainException.class, () -> clusterer.trainIncremental(new Place(0, 0, 0), -5.0));

        assertThrows(InvalidDomainException.class, () -> clusterer.predict(null, 100.0));
        assertThrows(InvalidDomainException.class, () -> clusterer.predict(new Place(0, 0, 0), -1.0));
    }
}
