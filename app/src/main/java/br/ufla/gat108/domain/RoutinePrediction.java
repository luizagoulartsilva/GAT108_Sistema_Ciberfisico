package br.ufla.gat108.domain;

/**
 * Resultado da predição validada de rotina sobre uma observação do trabalhador.
 *
 * @param isRoutine                      Verdadeiro se a observação for rotineira, falso se for anômala.
 * @param confidenceScore            Pontuação de confiança da predição (0.0 a 1.0).
 * @param distanceToNearestClusterMeters Distância espacial ao centroide mais próximo em metros.
 * @param matchedCluster                 Cluster líder que absorveu a observação (null se anômalo).
 * @param explanation                    Justificativa descritiva da predição.
 */
public record RoutinePrediction(
        boolean isRoutine,
        double confidenceScore,
        double distanceToNearestClusterMeters,
        RoutineCluster matchedCluster,
        String explanation
) {
    public RoutinePrediction {
        if (!Double.isFinite(confidenceScore) || confidenceScore < 0.0 || confidenceScore > 1.0) {
            throw new InvalidDomainException("confidenceScore must be between 0.0 and 1.0");
        }
        if (!Double.isFinite(distanceToNearestClusterMeters) || distanceToNearestClusterMeters < 0) {
            throw new InvalidDomainException("distanceToNearestClusterMeters must not be negative");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new InvalidDomainException("explanation must not be blank");
        }
    }
}
