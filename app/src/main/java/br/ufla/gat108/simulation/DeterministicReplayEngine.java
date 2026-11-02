package br.ufla.gat108.simulation;

import br.ufla.gat108.agent.RuleAgent;
import br.ufla.gat108.contracts.AgentContract;
import br.ufla.gat108.domain.Event;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.Track;
import br.ufla.gat108.reconciliation.KinematicReconciler;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor de reprodução determinística (Capacidade 5).
 * Simula a execução contínua de sequências de dados brutos sobre o buffer circular,
 * aplicando injeção de falhas e registrando as decisões do agente de forma auditável.
 */
public class DeterministicReplayEngine {

    private final FaultInjector faultInjector;
    private final AgentContract agent;
    private final KinematicReconciler reconciler;

    public DeterministicReplayEngine(FaultInjector faultInjector) {
        this(faultInjector, new RuleAgent(), new KinematicReconciler());
    }

    public DeterministicReplayEngine(FaultInjector faultInjector, AgentContract agent, KinematicReconciler reconciler) {
        this.faultInjector = faultInjector;
        this.agent = agent;
        this.reconciler = reconciler;
    }

    /**
     * Executa o ensaio de reprodução determinística sobre uma lista de dados de sensores.
     * @param rawDataset Coleção de amostras do trabalhador (caminhada, parada, queda)
     * @param trackBufferCapacity Capacidade do buffer circular a ser utilizado
     * @return Lista de eventos críticos detectados durante o ensaio
     */
    public List<Event> replay(List<Fix> rawDataset, int trackBufferCapacity) {
        Track track = new Track(trackBufferCapacity);
        List<Event> detectedEvents = new ArrayList<>();

        if (rawDataset == null || rawDataset.isEmpty()) {
            return detectedEvents;
        }

        // Reseta o estado do injetor para a semente configurada
        faultInjector.reseed();

        for (Fix rawFix : rawDataset) {
            Fix injectedFix = faultInjector.inject(rawFix);
            if (injectedFix != null) {
                track.append(injectedFix);

                // Opcional: Reconciliação em janela deslizante
                if (track.size() >= 4) {
                    reconciler.reconcile(track.getLatestWindow(5));
                }

                // Avaliação do agente de decisão
                Event decision = agent.decide(track);
                if (decision != null && decision.type() != Event.EventType.INSUFFICIENT_EVIDENCE) {
                    detectedEvents.add(decision);
                }
            }
        }

        return detectedEvents;
    }

    public FaultInjector getFaultInjector() {
        return faultInjector;
    }
}
