package br.ufla.gat108.domain;

public record Place(double latitude, double longitude, long observedAtMillis) {
    public Place {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new InvalidDomainException("latitude must be finite and between -90 and 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new InvalidDomainException("longitude must be finite and between -180 and 180");
        }
        if (observedAtMillis < 0) {
            throw new InvalidDomainException("place timestamp must not be negative");
        }
    }
}
