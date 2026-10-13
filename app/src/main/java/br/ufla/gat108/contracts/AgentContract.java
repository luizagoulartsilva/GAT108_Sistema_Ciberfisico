package br.ufla.gat108.contracts;

import br.ufla.gat108.domain.Event;
import br.ufla.gat108.domain.Track;

/** 
 * Camada 3 (Agente): Laço de decisão de alto nível do sistema ciberfísico.
 * O agente analisa a trajetória acumulada no {@link br.ufla.gat108.domain.Track} para 
 * disparar alertas de queda, imobilidade ou decidir pela abstenção caso a evidência 
 * dos sensores seja insuficiente ou incoerente.
 */
public interface AgentContract {
    /**
     * Analisa o buffer de trajetória e decide a próxima ação ou evento.
     * @param track O buffer circular contendo o histórico recente de telemetria.
     * @return Um {@link br.ufla.gat108.domain.Event} representando a decisão do agente.
     */
    Event decide(Track track);
}
