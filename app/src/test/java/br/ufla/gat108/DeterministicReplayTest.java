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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Teste de aceitação para a Capacidade 5 (Ensaio Reprodutível e Injeção de Falhas).
 * Executa o ensaio 3 vezes consecutivas com a mesma semente e valida saídas idênticas bit a bit.
 */
class DeterministicReplayTest {

    private static final long FIXED_SEED = 42108L;

    /** Gera um dataset simulado de caminhada seguido de evento de queda (impacto az = 35.0 m/s^2). */
    private List<Fix> generateFallDataset() {
        List<Fix> dataset = new ArrayList<>();
        long startTime = 1700000000000L;

        // 1. Caminhada Normal (10 amostras)
        for (int i = 0; i < 10; i++) {
            long t = startTime + (i * 20L);
            SensorReading r = new SensorReading(t, 0.2, 0.1, 9.81, 1.2, -21.245, -44.999, 0.1, 3.0, 0.1);
            dataset.add(new Fix(t, r));
        }

        // 2. Queda Súdita / Impacto no instante t = t0 + 200ms
        long fallTime = startTime + 200L;
        SensorReading fallReading = new SensorReading(fallTime, 1.5, 2.0, 35.5, 0.2, -21.245, -44.999, 0.1, 3.0, 0.1);
        dataset.add(new Fix(fallTime, fallReading));

        // 3. Imobilidade Pós-Queda (10 amostras)
        for (int i = 1; i <= 10; i++) {
            long t = fallTime + (i * 20L);
            SensorReading r = new SensorReading(t, 0.01, 0.01, 9.80, 0.0, -21.245, -44.999, 0.1, 3.0, 0.1);
            dataset.add(new Fix(t, r));
        }

        return dataset;
    }

    @Test
    @DisplayName("1. Ensaio executado 3 vezes com a mesma semente deve gerar saídas idênticas bit a bit")
    void replayExecutedThreeTimesWithSameSeedMustBeBitIdentical() {
        List<Fix> fallDataset = generateFallDataset();

        // Configuração de falhas: Ruído Gaussiano = 0.5 m/s^2, Perda de pacotes = 10%, GPS Spike = 5%
        FaultInjector injector = new FaultInjector(FIXED_SEED, 0.5, 0.10, 0.05);
        DeterministicReplayEngine engine = new DeterministicReplayEngine(injector);

        // Execução 1
        List<Event> run1 = engine.replay(fallDataset, 50);

        // Execução 2
        List<Event> run2 = engine.replay(fallDataset, 50);

        // Execução 3
        List<Event> run3 = engine.replay(fallDataset, 50);

        // Validação de presença de eventos
        assertNotNull(run1);
        assertFalse(run1.isEmpty(), "O ensaio deve detectar ao menos um evento de queda");

        // Validação de idêntica contagem de eventos
        assertEquals(run1.size(), run2.size(), "Execução 2 deve ter número de eventos idêntico à Execução 1");
        assertEquals(run1.size(), run3.size(), "Execução 3 deve ter número de eventos idêntico à Execução 1");

        // Comparação bit a bit de todos os eventos nas 3 execuções
        for (int i = 0; i < run1.size(); i++) {
            Event e1 = run1.get(i);
            Event e2 = run2.get(i);
            Event e3 = run3.get(i);

            assertEquals(e1.type(), e2.type(), "Tipo do evento " + i + " difere entre Run 1 e Run 2");
            assertEquals(e1.type(), e3.type(), "Tipo do evento " + i + " difere entre Run 1 e Run 3");

            assertEquals(e1.occurredAtMillis(), e2.occurredAtMillis(), "Timestamp do evento " + i + " difere entre Run 1 e Run 2");
            assertEquals(e1.occurredAtMillis(), e3.occurredAtMillis(), "Timestamp do evento " + i + " difere entre Run 1 e Run 3");

            assertEquals(e1.explanation(), e2.explanation(), "Explicação do evento " + i + " difere entre Run 1 e Run 2");
            assertEquals(e1.explanation(), e3.explanation(), "Explicação do evento " + i + " difere entre Run 1 e Run 3");
        }

        System.out.println("=================================================================================");
        System.out.println(" ENSAIO REPRODUTÍVEL (CAPACIDADE 5): 3/3 EXECUÇÕES IDÊNTICAS BIT A BIT");
        System.out.println("=================================================================================");
        System.out.println("Semente utilizada: " + FIXED_SEED);
        System.out.println("Eventos detectados por execução: " + run1.size());
        for (Event e : run1) {
            System.out.println(" -> Evento: " + e.type() + " | Instante: " + e.occurredAtMillis() + " ms | " + e.explanation());
        }
        System.out.println("=================================================================================\n");
    }

    @Test
    @DisplayName("2. Alterar a semente deve produzir saídas deterministicamente distintas sob ruído")
    void changingSeedMustProduceDeterministicallyDifferentInjections() {
        List<Fix> fallDataset = generateFallDataset();

        FaultInjector injectorSeed1 = new FaultInjector(1111L, 1.0, 0.20, 0.0);
        FaultInjector injectorSeed2 = new FaultInjector(9999L, 1.0, 0.20, 0.0);

        List<Fix> seq1 = injectorSeed1.injectSequence(fallDataset);
        List<Fix> seq2 = injectorSeed2.injectSequence(fallDataset);

        // As sequências geradas com sementes diferentes sob 20% de perda não devem ser rigorosamente idênticas
        boolean exactMatch = seq1.size() == seq2.size();
        if (exactMatch) {
            for (int i = 0; i < seq1.size(); i++) {
                if (seq1.get(i).reading().accelerationZ() != seq2.get(i).reading().accelerationZ()) {
                    exactMatch = false;
                    break;
                }
            }
        }
        assertFalse(exactMatch, "Sementes diferentes devem gerar valores de injeção distintos");
    }
}
