package br.ufla.gat108.domain;

public record Event(EventType type, long occurredAtMillis, String explanation) {
    public Event {
        if (type == null) {
            throw new InvalidDomainException("event type must not be null");
        }
        if (occurredAtMillis < 0) {
            throw new InvalidDomainException("event timestamp must not be negative");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new InvalidDomainException("event explanation must not be blank");
        }
    }

    public enum EventType {
        FALL,
        IMMOBILITY,
        INSUFFICIENT_EVIDENCE
    }
}
