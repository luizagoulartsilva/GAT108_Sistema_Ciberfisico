package br.ufla.gat108.agent;

import br.ufla.gat108.contracts.AgentContract;
import br.ufla.gat108.domain.Event;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.Track;

import java.util.List;

/**
 * Laço de agente de alto nível que executa o ciclo Decidir -> Validar -> Abster-se (Capacidade 8).
 * Gerencia a substituição transparente de motores de inferência e a degradação graciosa.
 */
public class AgentLoop implements AgentContract {

    private final InferenceEngine primaryEngine;
    private final InferenceEngine fallbackEngine;
    private String lastUsedEngineName = "None";

    public AgentLoop() {
        this(null, new RuleEngine());
    }

    public AgentLoop(InferenceEngine primaryEngine, InferenceEngine fallbackEngine) {
        this.primaryEngine = primaryEngine;
        this.fallbackEngine = fallbackEngine != null ? fallbackEngine : new RuleEngine();
    }

    @Override
    public Event decide(Track track) {
        // Estágio 0: Verificação de evidência mínima no buffer
        if (track == null || track.size() == 0) {
            lastUsedEngineName = "None";
            return new Event(
                Event.EventType.INSUFFICIENT_EVIDENCE,
                System.currentTimeMillis(),
                "Abstenção: Buffer de trajetória ausente ou vazio"
            );
        }

        Fix latest = track.latest();
        if (latest == null || latest.reading() == null) {
            lastUsedEngineName = "None";
            return new Event(
                Event.EventType.INSUFFICIENT_EVIDENCE,
                System.currentTimeMillis(),
                "Abstenção: Amostra inercial mais recente é nula"
            );
        }

        // Estágio 1: Decisão / Palpite candidato com Fallback gracioso
        Event candidate = null;
        InferenceEngine activeEngine = null;

        if (primaryEngine != null) {
            try {
                if (primaryEngine.isAvailable()) {
                    candidate = primaryEngine.infer(track);
                    if (candidate != null) {
                        activeEngine = primaryEngine;
                    }
                }
            } catch (Exception ignored) {
                // Falha transparente no motor primário aciona o fallback
            }
        }

        if (candidate == null) {
            activeEngine = fallbackEngine;
            try {
                if (fallbackEngine != null) {
                    candidate = fallbackEngine.infer(track);
                }
            } catch (Exception ignored) {
                // Previne falha fatal se o motor de fallback falhar
            }
        }

        lastUsedEngineName = activeEngine != null ? activeEngine.getName() : "None";

        if (candidate == null) {
            return new Event(
                Event.EventType.INSUFFICIENT_EVIDENCE,
                latest.timestampMillis(),
                "Abstenção: Nenhum motor de inferência produziu candidato"
            );
        }

        // Estágio 2: Validação física e corroboração de sensores
        Event validated = validateCandidate(candidate, track);

        // Estágio 3: Emissão do evento validado ou abstenção
        return validated;
    }

    /**
     * Valida a decisão candidata contra as evidências cinemáticas reais do sensor no Track.
     */
    private Event validateCandidate(Event candidate, Track track) {
        if (candidate.type() == Event.EventType.FALL) {
            // Valida se há pico de aceleração nos sensores corroborando o evento de queda
            List<Fix> window = track.getLatestWindow(Math.min(10, track.size()));
            boolean hasImpactCorroboration = false;
            for (Fix f : window) {
                double ax = f.reading().accelerationX();
                double ay = f.reading().accelerationY();
                double az = f.reading().accelerationZ();
                double mag = Math.sqrt(ax * ax + ay * ay + az * az);
                if (mag >= 20.0) { // Limiar mínimo de corroboração física
                    hasImpactCorroboration = true;
                    break;
                }
            }

            if (!hasImpactCorroboration) {
                return new Event(
                    Event.EventType.INSUFFICIENT_EVIDENCE,
                    candidate.occurredAtMillis(),
                    "Abstenção por validação: Modelo indicou queda, mas os sensores não registraram pico de impacto"
                );
            }
        } else if (candidate.type() == Event.EventType.IMMOBILITY) {
            // Valida se a velocidade recente do sensor confirma parada
            Fix latest = track.latest();
            if (latest != null && latest.reading() != null && latest.reading().speedMetersPerSecond() > 1.2) {
                return new Event(
                    Event.EventType.INSUFFICIENT_EVIDENCE,
                    candidate.occurredAtMillis(),
                    "Abstenção por validação: Modelo indicou imobilidade, mas a velocidade GNSS é > 1.2 m/s"
                );
            }
        }

        return candidate;
    }

    public String getLastUsedEngineName() {
        return lastUsedEngineName;
    }

    public InferenceEngine getPrimaryEngine() {
        return primaryEngine;
    }

    public InferenceEngine getFallbackEngine() {
        return fallbackEngine;
    }
}
