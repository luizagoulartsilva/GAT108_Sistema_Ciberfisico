package br.ufla.gat108.contracts;

import br.ufla.gat108.domain.Fix;
import java.util.List;

/** 
 * Camada 1 (Reconciliação): Aplica restrições físicas e cinemáticas sobre as leituras ruidosas.
 * Utiliza algoritmos como Mínimos Quadrados Ponderados (WLS) ou filtros de resíduo para
 * corrigir erros grosseiros e estimar a postura mais provável do trabalhador.
 */
public interface ReconciliationContract {
    /**
     * Processa uma janela de amostras para reduzir a incerteza inercial.
     * @param window Lista de {@link br.ufla.gat108.domain.Fix} capturados recentemente.
     * @return Resultado contendo os resíduos cinemáticos antes/depois e a lista corrigida.
     */
    ReconciliationResult reconcile(List<Fix> window);

    /**
     * Estrutura de retorno da reconciliação.
     * @param residualBefore Resíduo cinemático (erro) original.
     * @param residualAfter Resíduo cinemático após o ajuste/filtro.
     * @param corrected Lista de amostras com valores de aceleração/velocidade ajustados.
     */
    record ReconciliationResult(double residualBefore, double residualAfter, List<Fix> corrected) { }
}
