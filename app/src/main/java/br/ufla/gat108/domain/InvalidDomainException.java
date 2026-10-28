package br.ufla.gat108.domain;

public final class InvalidDomainException extends IllegalArgumentException {
    public InvalidDomainException(String message) {
        super(message);
    }
}
