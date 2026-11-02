package br.ufla.gat108.agent;

import br.ufla.gat108.contracts.AgentContract;
import br.ufla.gat108.domain.Event;
import br.ufla.gat108.domain.Track;

/**
 * Agente de regras (Baseline e Fachada da Capacidade 8).
 * Utiliza o {@link AgentLoop} com o {@link RuleEngine} para executar o ciclo
 * Decidir -> Validar -> Abster-se.
 */
public class RuleAgent implements AgentContract {

    private final AgentLoop agentLoop;

    public RuleAgent() {
        this(null);
    }

    public RuleAgent(InferenceEngine primaryEngine) {
        this.agentLoop = new AgentLoop(primaryEngine, new RuleEngine());
    }

    @Override
    public Event decide(Track track) {
        return agentLoop.decide(track);
    }

    public AgentLoop getAgentLoop() {
        return agentLoop;
    }
}
