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

    /** Peso para Mínimos Quadrados Ponderados (WLS) da aceleração pontual. */
    public double getWeightAcceleration() {
        return 1.0 / (varianceAcceleration + 1e-9);
    }

    /** Peso para Mínimos Quadrados Ponderados (WLS) da aceleração integrada sobre o intervalo dt. */
    public double getWeightAcceleration(double dt) {
        double dtSq = dt * dt;
        return 1.0 / (varianceAcceleration * dtSq + 1e-9);
    }

    /** Peso para Mínimos Quadrados Ponderados (WLS) da velocidade. */
    public double getWeightSpeed() {
        return 1.0 / (2.0 * varianceSpeed + 1e-9);
    }

    /** Desvio padrão do resíduo cinemático combinado sobre dt (para escore-z). */
    public double getStandardResidualDev(double dt) {
        double varDeltaVAcc = varianceAcceleration * dt * dt;
        double varDeltaVGps = 2.0 * varianceSpeed;
        return Math.sqrt(varDeltaVAcc + varDeltaVGps + 1e-9);
    }
}
