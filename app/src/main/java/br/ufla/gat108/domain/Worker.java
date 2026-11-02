package br.ufla.gat108.domain;

/** Identidade local do ativo observado, sem dados pessoais desnecessários. */
public record Worker(String workerId) {
    public Worker {
        if (workerId == null || workerId.isBlank()) {
            throw new InvalidDomainException("workerId must not be blank");
        }
    }
}
