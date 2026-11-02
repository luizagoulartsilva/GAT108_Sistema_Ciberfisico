package br.ufla.gat108.contracts;

import br.ufla.gat108.domain.Place;
import br.ufla.gat108.domain.RoutineCluster;
import br.ufla.gat108.domain.RoutinePrediction;

/**
 * Camada 3 (Aprendizado): Contrato para algoritmos de aprendizado online e predição de rotina.
 * Permite que o sistema aprenda incrementalmente o envelope de imobilidade e rotina do trabalhador,
 * operando sob memória constante O(1).
 */
public interface RoutineLearningContract {

    /**
     * Atualiza o modelo incrementalmente com um novo local e duração observados.
     *
     * @param place           Localização geográfica e instante da observação.
     * @param durationSeconds Duração da imobilidade/permanência em segundos.
     * @return O {@link RoutineCluster} atualizado/criado ou null se rejeitado.
     */
    RoutineCluster trainIncremental(Place place, double durationSeconds);

    /**
     * Avalia se uma determinada imobilidade/permanência em local e horário é rotineira ou anômala.
     *
     * @param place           Localização e instante sob avaliação.
     * @param durationSeconds Duração da imobilidade em segundos.
     * @return {@link RoutinePrediction} contendo o resultado validado e grau de confiança.
     */
    RoutinePrediction predict(Place place, double durationSeconds);

    /**
     * Retorna a quantidade atual de clusters mantidos em memória de tamanho fixo.
     */
    int activeClusterCount();

    /**
     * Re-inicializa os clusters armazenados.
     */
    void reset();
}
