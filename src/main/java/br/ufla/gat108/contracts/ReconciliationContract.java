package br.ufla.gat108.contracts;

import br.ufla.gat108.domain.Fix;
import java.util.List;

/** Camada 1: reconcilia leituras sob a restrição cinemática do domínio. */
public interface ReconciliationContract {
    ReconciliationResult reconcile(List<Fix> window);

    record ReconciliationResult(double residualBefore, double residualAfter, List<Fix> corrected) { }
}
