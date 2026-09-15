package br.ufla.gat108;

import br.ufla.gat108.contracts.AcquisitionContract;
import br.ufla.gat108.domain.Fix;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AcquisitionContractTest {
    @Test
    void acquisitionMustReturnAValidatedFix() {
        AcquisitionContract acquisition = () -> {
            throw new UnsupportedOperationException("S1 contract: acquisition not implemented");
        };

        Fix fix = acquisition.acquire();

        assertNotNull(fix);
    }
}
