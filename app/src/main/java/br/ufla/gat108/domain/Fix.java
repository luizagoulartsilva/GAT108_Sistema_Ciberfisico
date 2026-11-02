package br.ufla.gat108.domain;

/** Amostra normalizada que a camada de aquisição entrega ao restante do sistema. */
public record Fix(long timestampMillis, SensorReading reading) {
    public Fix {
        if (timestampMillis < 0) {
            throw new InvalidDomainException("fix timestamp must not be negative");
        }
        if (reading == null) {
            throw new InvalidDomainException("fix reading must not be null");
        }
        if (reading.timestampMillis() != timestampMillis) {
            throw new InvalidDomainException("fix and reading timestamps must match");
        }
    }
}
