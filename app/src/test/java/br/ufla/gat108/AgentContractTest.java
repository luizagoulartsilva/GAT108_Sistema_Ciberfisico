package br.ufla.gat108;

import br.ufla.gat108.agent.AgentLoop;
import br.ufla.gat108.agent.InferenceEngine;
import br.ufla.gat108.agent.LocalEngine;
import br.ufla.gat108.agent.RuleAgent;
import br.ufla.gat108.agent.RuleEngine;
import br.ufla.gat108.contracts.AgentContract;
import br.ufla.gat108.domain.Event;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.SensorReading;
import br.ufla.gat108.domain.Track;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentContractTest {

    @Test
    void agentMustReturnAnEventOrAbstention() {
        AgentContract agent = new RuleAgent();
        Event decision = agent.decide(new Track(4));
        assertNotNull(decision);
        assertEquals(Event.EventType.INSUFFICIENT_EVIDENCE, decision.type());
    }

    @Test
    void testGracefulDegradationWhenModelIsRemoved(@TempDir Path tempDir) throws IOException {
        // Setup: Criar um arquivo de modelo temporário fictício
        File modelFile = tempDir.resolve("local_litert_model.tflite").toFile();
        try (FileWriter writer = new FileWriter(modelFile)) {
            writer.write("FAKE_MODEL_WEIGHTS_DATA");
        }

        LocalEngine localEngine = new LocalEngine(modelFile, 25.0);
        RuleEngine ruleEngine = new RuleEngine();
        AgentLoop agentLoop = new AgentLoop(localEngine, ruleEngine);

        // Prepara um Track com uma leitura de impacto
        Track track = new Track(5);
        SensorReading impactReading = new SensorReading(
            1000L, 0.0, 0.0, 32.0, 0.0, -21.2294, -44.9785, 0.1, 2.0, 0.1
        );
        track.append(new Fix(1000L, impactReading));

        // 1. Execução com o arquivo de modelo PRESENTE
        Event decision1 = agentLoop.decide(track);
        assertNotNull(decision1);
        assertEquals(Event.EventType.FALL, decision1.type());
        assertEquals("LocalEngine(LiteRT)", agentLoop.getLastUsedEngineName());

        // 2. Ação de remoção/deleção do modelo em tempo de execução
        boolean deleted = modelFile.delete();
        assertTrue(deleted, "O arquivo de modelo temporário deve ser deletado");
        assertFalse(modelFile.exists());

        // 3. Execução APÓS a remoção do modelo: Deve chavear para RuleEngine sem exceções!
        Event decision2 = agentLoop.decide(track);
        assertNotNull(decision2);
        assertEquals(Event.EventType.FALL, decision2.type());
        assertEquals("RuleEngine", agentLoop.getLastUsedEngineName());
    }

    @Test
    void testPrimaryEngineThrowingExceptionFallbackToRuleEngine() {
        InferenceEngine crashingEngine = new InferenceEngine() {
            @Override
            public Event infer(Track track) {
                throw new RuntimeException("Simulação de Falha Crítica no Modelo de IA");
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public String getName() {
                return "CrashingEngine";
            }
        };

        AgentLoop agentLoop = new AgentLoop(crashingEngine, new RuleEngine());

        Track track = new Track(5);
        SensorReading impactReading = new SensorReading(
            1000L, 0.0, 0.0, 30.0, 0.0, -21.2294, -44.9785, 0.1, 2.0, 0.1
        );
        track.append(new Fix(1000L, impactReading));

        // A exceção no motor primário deve ser capturada de forma transparente e acionar o RuleEngine
        Event decision = agentLoop.decide(track);
        assertNotNull(decision);
        assertEquals(Event.EventType.FALL, decision.type());
        assertEquals("RuleEngine", agentLoop.getLastUsedEngineName());
    }

    @Test
    void testValidationRejectsIncoherentFallPrediction() {
        // Motor primário fictício ("alucinado") que tenta emitir FALL sem impacto real nos sensores
        InferenceEngine hallucinatingEngine = new InferenceEngine() {
            @Override
            public Event infer(Track track) {
                return new Event(Event.EventType.FALL, 1000L, "Alucinação: Queda detectada");
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public String getName() {
                return "HallucinatingEngine";
            }
        };

        AgentLoop agentLoop = new AgentLoop(hallucinatingEngine, new RuleEngine());

        // Sensor registra gravidade normal (9.8 m/s^2) sem pico de impacto
        Track track = new Track(5);
        SensorReading normalReading = new SensorReading(
            1000L, 0.0, 0.0, 9.8, 0.0, -21.2294, -44.9785, 0.1, 2.0, 0.1
        );
        track.append(new Fix(1000L, normalReading));

        Event decision = agentLoop.decide(track);

        assertNotNull(decision);
        // A validação do agente DEVE rejeitar a queda e se abster
        assertEquals(Event.EventType.INSUFFICIENT_EVIDENCE, decision.type());
    }

    @Test
    void testValidationRejectsIncoherentImmobilityPrediction() {
        // Motor primário que prevê imobilidade apesar do trabalhador estar em movimento rápido
        InferenceEngine badEngine = new InferenceEngine() {
            @Override
            public Event infer(Track track) {
                return new Event(Event.EventType.IMMOBILITY, 1000L, "Falsa Imobilidade");
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public String getName() {
                return "BadEngine";
            }
        };

        AgentLoop agentLoop = new AgentLoop(badEngine, new RuleEngine());

        // Leitura com velocidade de 5.0 m/s (caminhada/corrida rápida)
        Track track = new Track(5);
        SensorReading movingReading = new SensorReading(
            1000L, 0.0, 0.0, 9.8, 5.0, -21.2294, -44.9785, 0.1, 2.0, 0.1
        );
        track.append(new Fix(1000L, movingReading));

        Event decision = agentLoop.decide(track);

        assertNotNull(decision);
        // A validação DEVE rejeitar a imobilidade devido à velocidade GNSS
        assertEquals(Event.EventType.INSUFFICIENT_EVIDENCE, decision.type());
    }

    @Test
    void testAbstentionOnEmptyOrNullTrack() {
        AgentLoop agentLoop = new AgentLoop();

        Event decisionNull = agentLoop.decide(null);
        assertEquals(Event.EventType.INSUFFICIENT_EVIDENCE, decisionNull.type());

        Event decisionEmpty = agentLoop.decide(new Track(5));
        assertEquals(Event.EventType.INSUFFICIENT_EVIDENCE, decisionEmpty.type());
    }

    @Test
    void testExecutionTimeConstraint() {
        AgentLoop agentLoop = new AgentLoop();
        Track track = new Track(10);
        SensorReading reading = new SensorReading(
            1000L, 1.0, 1.0, 9.8, 0.0, -21.2294, -44.9785, 0.1, 2.0, 0.1
        );
        track.append(new Fix(1000L, reading));

        // Warmup
        for (int i = 0; i < 100; i++) {
            agentLoop.decide(track);
        }

        long startNanos = System.nanoTime();
        int iterations = 1000;
        for (int i = 0; i < iterations; i++) {
            agentLoop.decide(track);
        }
        long totalNanos = System.nanoTime() - startNanos;
        double avgMillis = (totalNanos / 1e6) / iterations;

        // O laço de decisão do agente deve rodar em frações de milissegundo (< 5ms)
        assertTrue(avgMillis < 5.0, "O tempo médio de decisão (" + avgMillis + "ms) excedeu o limite");
    }
}
