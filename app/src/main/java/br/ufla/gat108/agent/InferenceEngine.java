package br.ufla.gat108.agent;

import br.ufla.gat108.domain.Event;
import br.ufla.gat108.domain.Track;

/**
 * Contrato genérico para motores de inferência substituíveis do agente (Capacidade 8).
 * Permite intercalar motores determinísticos (regras) e modelos de IA/linguagem local.
 */
public interface InferenceEngine {

    /**
     * Tenta produzir uma decisão/palpite candidato a partir do histórico de trajetória.
     *
     * @param track Buffer circular de telemetria e postura.
     * @return {@link Event} candidato ou null se o motor não puder decidir.
     */
    Event infer(Track track);

    /**
     * Indica se o motor de inferência está operacional e disponível no ambiente.
     * Ex: Para modelos locais, verifica se o arquivo do modelo está presente no disco.
     *
     * @return true se o motor estiver disponível; false se desativado ou removido.
     */
    boolean isAvailable();

    /**
     * Retorna o nome/identificador do motor de inferência.
     */
    String getName();
}
