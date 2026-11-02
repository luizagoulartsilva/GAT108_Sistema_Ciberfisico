package br.ufla.gat108.learning;

import br.ufla.gat108.contracts.RoutineLearningContract;
import br.ufla.gat108.domain.InvalidDomainException;
import br.ufla.gat108.domain.Place;
import br.ufla.gat108.domain.RoutineCluster;
import br.ufla.gat108.domain.RoutinePrediction;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Algoritmo de agrupamento incremental Leader Clusterer (Capacidade 7).
 * Opera sob memória estritamente constante O(1) usando um vetor de capacidade fixa pré-alocado,
 * sem registrar histórico bruto de observações e sem gerar vazamentos de memória.
 */
public class LeaderClusterer implements RoutineLearningContract {

    public static final int DEFAULT_MAX_CLUSTERS = 20;
    public static final double DEFAULT_SPATIAL_RADIUS_METERS = 100.0;
    public static final double DEFAULT_TIME_WINDOW_MINUTES = 60.0;

    private final int maxClusters;
    private final double spatialRadiusMeters;
    private final double timeWindowMinutes;

    private final RoutineCluster[] clusters;
    private int activeClustersCount = 0;
    private int nextClusterId = 1;

    public LeaderClusterer() {
        this(DEFAULT_MAX_CLUSTERS, DEFAULT_SPATIAL_RADIUS_METERS, DEFAULT_TIME_WINDOW_MINUTES);
    }

    public LeaderClusterer(int maxClusters, double spatialRadiusMeters, double timeWindowMinutes) {
        if (maxClusters <= 0) {
            throw new InvalidDomainException("maxClusters must be greater than zero");
        }
        if (spatialRadiusMeters <= 0) {
            throw new InvalidDomainException("spatialRadiusMeters must be greater than zero");
        }
        if (timeWindowMinutes <= 0) {
            throw new InvalidDomainException("timeWindowMinutes must be greater than zero");
        }
        this.maxClusters = maxClusters;
        this.spatialRadiusMeters = spatialRadiusMeters;
        this.timeWindowMinutes = timeWindowMinutes;
        this.clusters = new RoutineCluster[maxClusters];
    }

    @Override
    public synchronized RoutineCluster trainIncremental(Place place, double durationSeconds) {
        if (place == null) {
            throw new InvalidDomainException("place must not be null");
        }
        if (durationSeconds < 0) {
            throw new InvalidDomainException("durationSeconds must not be negative");
        }

        double timeOfDayMin = extractTimeOfDayMinutes(place.observedAtMillis());
        int matchIdx = findBestMatchingClusterIndex(place.latitude(), place.longitude(), timeOfDayMin);

        if (matchIdx >= 0) {
            // Atualização centroidal incremental online
            RoutineCluster existing = clusters[matchIdx];
            long newCount = existing.count() + 1;

            double newLat = existing.centerLatitude() + (place.latitude() - existing.centerLatitude()) / newCount;
            double newLon = existing.centerLongitude() + (place.longitude() - existing.centerLongitude()) / newCount;
            
            // Atualização circular do horário do dia
            double deltaMin = circularMinutesDifference(timeOfDayMin, existing.centerTimeOfDayMinutes());
            double newTimeMin = normalizeMinutes(existing.centerTimeOfDayMinutes() + deltaMin / newCount);

            double newDur = existing.centerDurationSeconds() + (durationSeconds - existing.centerDurationSeconds()) / newCount;

            RoutineCluster updated = new RoutineCluster(
                    existing.clusterId(),
                    newLat,
                    newLon,
                    newTimeMin,
                    newDur,
                    newCount,
                    existing.radiusMeters(),
                    existing.label()
            );
            clusters[matchIdx] = updated;
            return updated;
        } else {
            // Criação de novo cluster líder
            if (activeClustersCount < maxClusters) {
                int id = nextClusterId++;
                String label = String.format(Locale.US, "Posto de Rotina #%d", id);
                RoutineCluster newCluster = new RoutineCluster(
                        id,
                        place.latitude(),
                        place.longitude(),
                        timeOfDayMin,
                        durationSeconds,
                        1L,
                        spatialRadiusMeters,
                        label
                );
                clusters[activeClustersCount++] = newCluster;
                return newCluster;
            } else {
                // Capacidade estática O(1) atingida: substitui o cluster menos visitado
                int weakestIdx = findLeastVisitedClusterIndex();
                int id = nextClusterId++;
                String label = String.format(Locale.US, "Posto de Rotina #%d", id);
                RoutineCluster newCluster = new RoutineCluster(
                        id,
                        place.latitude(),
                        place.longitude(),
                        timeOfDayMin,
                        durationSeconds,
                        1L,
                        spatialRadiusMeters,
                        label
                );
                clusters[weakestIdx] = newCluster;
                return newCluster;
            }
        }
    }

    @Override
    public synchronized RoutinePrediction predict(Place place, double durationSeconds) {
        if (place == null) {
            throw new InvalidDomainException("place must not be null");
        }
        if (durationSeconds < 0) {
            throw new InvalidDomainException("durationSeconds must not be negative");
        }

        if (activeClustersCount == 0) {
            return new RoutinePrediction(
                    false,
                    0.5,
                    Double.POSITIVE_INFINITY,
                    null,
                    "Nenhum padrão de rotina aprendido até o momento"
            );
        }

        double timeOfDayMin = extractTimeOfDayMinutes(place.observedAtMillis());
        double minSpatialDist = Double.POSITIVE_INFINITY;
        RoutineCluster bestMatch = null;

        for (int i = 0; i < activeClustersCount; i++) {
            RoutineCluster c = clusters[i];
            double distMeters = haversineDistanceMeters(place.latitude(), place.longitude(), c.centerLatitude(), c.centerLongitude());
            double timeDiffMin = Math.abs(circularMinutesDifference(timeOfDayMin, c.centerTimeOfDayMinutes()));

            if (distMeters <= c.radiusMeters() && timeDiffMin <= timeWindowMinutes) {
                if (distMeters < minSpatialDist) {
                    minSpatialDist = distMeters;
                    bestMatch = c;
                }
            } else if (distMeters < minSpatialDist) {
                minSpatialDist = distMeters;
            }
        }

        if (bestMatch != null) {
            // Regra de confiança para rotina confirmada
            double spatialFactor = Math.max(0.0, 1.0 - (minSpatialDist / bestMatch.radiusMeters()));
            double countFactor = Math.min(1.0, 0.5 + (bestMatch.count() / 10.0));
            double confidence = Math.min(0.99, Math.max(0.5, spatialFactor * countFactor));

            String explanation = String.format(Locale.US,
                    "Imobilidade rotineira confirmada em %s (Dist: %.1fm, Visitas: %d)",
                    bestMatch.label(), minSpatialDist, bestMatch.count());

            return new RoutinePrediction(true, confidence, minSpatialDist, bestMatch, explanation);
        } else {
            // Anomalia de imobilidade (fora da rotina)
            double confidence = 0.85;
            String explanation = Double.isInfinite(minSpatialDist) ?
                    "Imobilidade fora do envelope de rotina habitual" :
                    String.format(Locale.US, "Desvio anômalo de imobilidade detectado (Menor distância: %.1fm)", minSpatialDist);

            return new RoutinePrediction(false, confidence, Double.isInfinite(minSpatialDist) ? 9999.0 : minSpatialDist, null, explanation);
        }
    }

    @Override
    public synchronized int activeClusterCount() {
        return activeClustersCount;
    }

    @Override
    public synchronized void reset() {
        for (int i = 0; i < activeClustersCount; i++) {
            clusters[i] = null;
        }
        activeClustersCount = 0;
        nextClusterId = 1;
    }

    private int findBestMatchingClusterIndex(double lat, double lon, double timeOfDayMin) {
        int bestIdx = -1;
        double minSpatialDist = Double.POSITIVE_INFINITY;

        for (int i = 0; i < activeClustersCount; i++) {
            RoutineCluster c = clusters[i];
            double distMeters = haversineDistanceMeters(lat, lon, c.centerLatitude(), c.centerLongitude());
            double timeDiffMin = Math.abs(circularMinutesDifference(timeOfDayMin, c.centerTimeOfDayMinutes()));

            if (distMeters <= c.radiusMeters() && timeDiffMin <= timeWindowMinutes) {
                if (distMeters < minSpatialDist) {
                    minSpatialDist = distMeters;
                    bestIdx = i;
                }
            }
        }
        return bestIdx;
    }

    private int findLeastVisitedClusterIndex() {
        int weakestIdx = 0;
        long minCount = Long.MAX_VALUE;
        for (int i = 0; i < activeClustersCount; i++) {
            if (clusters[i].count() < minCount) {
                minCount = clusters[i].count();
                weakestIdx = i;
            }
        }
        return weakestIdx;
    }

    public static double haversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0; // Raio da Terra em metros
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public static double circularMinutesDifference(double time1Min, double time2Min) {
        double diff = time1Min - time2Min;
        while (diff < -720.0) diff += 1440.0;
        while (diff > 720.0) diff -= 1440.0;
        return diff;
    }

    private static double extractTimeOfDayMinutes(long timestampMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(timestampMillis);
        int hours = cal.get(Calendar.HOUR_OF_DAY);
        int minutes = cal.get(Calendar.MINUTE);
        return hours * 60.0 + minutes;
    }

    private static double normalizeMinutes(double minutes) {
        double m = minutes % 1440.0;
        if (m < 0) m += 1440.0;
        return m;
    }
}
