package br.ufla.gat108;

import br.ufla.gat108.contracts.ReconciliationContract;
import br.ufla.gat108.domain.Fix;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconciliationContractTest {
    @Test
    void reconciliationMustReduceThePhysicalResidual() {
        ReconciliationContract reconciler = window -> {
            throw new UnsupportedOperationException("S1 contract: reconciliation not implemented");
        };
        List<Fix> window = List.of(DomainFixtures.fix(0), DomainFixtures.fix(20));

        ReconciliationContract.ReconciliationResult result = reconciler.reconcile(window);

        assertTrue(result.residualAfter() < result.residualBefore());
    }
}
