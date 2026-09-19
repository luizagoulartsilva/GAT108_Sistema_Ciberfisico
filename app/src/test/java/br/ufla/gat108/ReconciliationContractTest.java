package br.ufla.gat108;

import br.ufla.gat108.contracts.ReconciliationContract;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.reconciliation.KinematicReconciler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconciliationContractTest {
    @Test
    void reconciliationMustReduceThePhysicalResidual() {
        ReconciliationContract reconciler = new KinematicReconciler();
        
        // Simulando cenário ruidoso: 
        // GPS diz que velocidade aumentou 2m/s em 1s (a=2)
        // Acelerômetro diz que aceleração foi 5m/s^2 (ruído alto)
        Fix f1 = DomainFixtures.fix(0, 0, 0, 0, 9.81); 
        Fix f2 = DomainFixtures.fix(1000, 2.0, 5.0 + 9.81, 0, 0); // dt=1s, v=2, a_linear=5
        
        List<Fix> window = List.of(f1, f2);

        ReconciliationContract.ReconciliationResult result = reconciler.reconcile(window);

        assertNotNull(result);
        // O resíduo original é |2 - 5*1| = 3. 
        // O resíduo após WLS deve ser próximo de 0 no modelo implementado (ajuste perfeito ao alvo calculado)
        assertTrue(result.residualAfter() < result.residualBefore(), 
            "Residual after (" + result.residualAfter() + ") should be less than before (" + result.residualBefore() + ")");
    }
}
