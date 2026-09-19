package br.ufla.gat108.domain;

/** Uma leitura bruta, imutável, dos sensores do trabalhador. */
public record SensorReading(
        long timestampMillis,
        double accelerationX,
        double accelerationY,
        double accelerationZ,
        double speedMetersPerSecond,
        double latitude,
        double longitude,
        double accelerationAccuracyMetersPerSecondSquared,
        double positionAccuracyMeters,
        double speedAccuracyMetersPerSecond) {

    public SensorReading {
        if (timestampMillis < 0) {
            throw new InvalidDomainException("timestamp must not be negative");
        }
        validateFinite(accelerationX, "accelerationX");
        validateFinite(accelerationY, "accelerationY");
        validateFinite(accelerationZ, "accelerationZ");
        validateFinite(speedMetersPerSecond, "speedMetersPerSecond");
        validateFinite(latitude, "latitude");
        validateFinite(longitude, "longitude");
        validateNonNegative(accelerationAccuracyMetersPerSecondSquared, "acceleration accuracy");
        validateNonNegative(positionAccuracyMeters, "position accuracy");
        validateNonNegative(speedAccuracyMetersPerSecond, "speed accuracy");
        if (speedMetersPerSecond < 0) {
            throw new InvalidDomainException("speed must not be negative");
        }
        if (latitude < -90 || latitude > 90) {
            throw new InvalidDomainException("latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new InvalidDomainException("longitude must be between -180 and 180");
        }
    }

    private static void validateFinite(double value, String field) {
        if (!Double.isFinite(value)) {
            throw new InvalidDomainException(field + " must be finite");
        }
    }

    private static void validateNonNegative(double value, String field) {
        validateFinite(value, field);
        if (value < 0) {
            throw new InvalidDomainException(field + " must not be negative");
        }
    }
}
