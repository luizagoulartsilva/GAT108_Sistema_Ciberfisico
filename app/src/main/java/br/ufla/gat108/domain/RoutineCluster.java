package br.ufla.gat108.domain;

/**
 * Representa um cluster líder de rotina de imobilidade/permanência do trabalhador.
 *
 * @param clusterId              Identificador único do cluster no modelo O(1).
 * @param centerLatitude         Latitude do centroide (-90 a 90).
 * @param centerLongitude        Longitude do centroide (-180 a 180).
 * @param centerTimeOfDayMinutes Horário do dia médio do centroide em minutos (0 a 1440).
 * @param centerDurationSeconds  Duração média da permanência em segundos.
 * @param count                  Quantidade de observações absorvidas pelo cluster.
 * @param radiusMeters           Raio espacial de tolerância em metros.
 * @param label                  Rótulo descritivo da rotina (ex: "Posto de Vigilância", "Pausa").
 */
public record RoutineCluster(
        int clusterId,
        double centerLatitude,
        double centerLongitude,
        double centerTimeOfDayMinutes,
        double centerDurationSeconds,
        long count,
        double radiusMeters,
        String label
) {
    public RoutineCluster {
        if (clusterId < 0) {
            throw new InvalidDomainException("clusterId must not be negative");
        }
        if (!Double.isFinite(centerLatitude) || centerLatitude < -90 || centerLatitude > 90) {
            throw new InvalidDomainException("centerLatitude must be finite and between -90 and 90");
        }
        if (!Double.isFinite(centerLongitude) || centerLongitude < -180 || centerLongitude > 180) {
            throw new InvalidDomainException("centerLongitude must be finite and between -180 and 180");
        }
        if (!Double.isFinite(centerTimeOfDayMinutes) || centerTimeOfDayMinutes < 0 || centerTimeOfDayMinutes >= 1440) {
            throw new InvalidDomainException("centerTimeOfDayMinutes must be between 0 and 1440");
        }
        if (!Double.isFinite(centerDurationSeconds) || centerDurationSeconds < 0) {
            throw new InvalidDomainException("centerDurationSeconds must not be negative");
        }
        if (count < 0) {
            throw new InvalidDomainException("count must not be negative");
        }
        if (!Double.isFinite(radiusMeters) || radiusMeters <= 0) {
            throw new InvalidDomainException("radiusMeters must be positive");
        }
        if (label == null || label.isBlank()) {
            throw new InvalidDomainException("label must not be blank");
        }
    }
}
