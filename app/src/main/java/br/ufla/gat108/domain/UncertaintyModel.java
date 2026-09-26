package br.ufla.gat108.domain;

/**
 * Modelo para transformar acurácias dos sensores em matrizes de covariância e pesos.
 * Baseado na premissa de que a acurácia informada pela API (1-sigma) representa o desvio padrão.
 */
public record UncertaintyModel(
        double varianceAcceleration,
        double varianceSpeed,
        double variancePosition) {

    public static UncertaintyModel fromReading(SensorReading reading) {
        return new UncertaintyModel(
                Math.pow(reading.accelerationAccuracyMetersPerSecondSquared(), 2),
                Math.pow(reading.speedAccuracyMetersPerSecond(), 2),
                Math.pow(reading.positionAccuracyMeters(), 2)
        );
    }

    /** Peso para Mínimos Quadrados Ponderados (WLS). */
    public double getWeightAcceleration() {
        return 1.0 / (varianceAcceleration + 1e-9);
    }

    public double getWeightSpeed() {
        return 1.0 / (varianceSpeed + 1e-9);
    }
}
