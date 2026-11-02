package br.ufla.gat108;

import br.ufla.gat108.acquisition.AndroidSensorAcquisition;
import br.ufla.gat108.contracts.AcquisitionContract;
import br.ufla.gat108.domain.Fix;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcquisitionContractTest {
    @Test
    void acquisitionMustReturnAValidatedFix() {
        AcquisitionContract acquisition = new AndroidSensorAcquisition();

        Fix fix = acquisition.acquire();

        assertNotNull(fix);
        assertNotNull(fix.reading());
        assertTrue(fix.timestampMillis() > 0);
    }
}
