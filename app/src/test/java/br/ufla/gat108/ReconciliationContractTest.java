package br.ufla.gat108;

import br.ufla.gat108.contracts.ReconciliationContract;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.SensorReading;
import br.ufla.gat108.reconciliation.GrossErrorDetector;
import br.ufla.gat108.reconciliation.KinematicReconciler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconciliationContractTest {

    @Test
    @DisplayName("1. Reconciliação WLS reduz resíduo na Janela Real 1 (Caminhada plana contínua)")
    void reconciliationMustReduceResidualInRealWindow1FlatSteadyWalk() {
        KinematicReconciler reconciler = new KinematicReconciler();
        List<Fix> window = RealWalkingDataFixtures.window1FlatSteadyWalk();

        ReconciliationContract.ReconciliationResult result = reconciler.reconcile(window);

        assertNotNull(result);
        assertTrue(result.residualAfter() < result.residualBefore(),
                String.format("Janela 1: Resíduo após (%.4f) deve ser menor que antes (%.4f)",
                        result.residualAfter(), result.residualBefore()));
        assertEquals(0, result.grossErrorsDetected(), "Não deve haver erros grosseiros na caminhada limpa");
    }

    @Test
    @DisplayName("2. Reconciliação WLS reduz resíduo na Janela Real 2 (Caminhada com aceleração de ritmo)")
    void reconciliationMustReduceResidualInRealWindow2AcceleratedWalk() {
        KinematicReconciler reconciler = new KinematicReconciler();
        List<Fix> window = RealWalkingDataFixtures.window2AcceleratedWalk();

        ReconciliationContract.ReconciliationResult result = reconciler.reconcile(window);

        assertNotNull(result);
        assertTrue(result.residualAfter() < result.residualBefore(),
                String.format("Janela 2: Resíduo após (%.4f) deve ser menor que antes (%.4f)",
                        result.residualAfter(), result.residualBefore()));
        assertEquals(0, result.grossErrorsDetected(), "Não deve haver erros grosseiros na caminhada acelerada limpa");
    }

    @Test
    @DisplayName("3. Reconciliação WLS reduz resíduo na Janela Real 3 (Caminhada em rampa/escada)")
    void reconciliationMustReduceResidualInRealWindow3InclineStairsWalk() {
        KinematicReconciler reconciler = new KinematicReconciler();
        List<Fix> window = RealWalkingDataFixtures.window3InclineStairsWalk();

        ReconciliationContract.ReconciliationResult result = reconciler.reconcile(window);

        assertNotNull(result);
        assertTrue(result.residualAfter() < result.residualBefore(),
                String.format("Janela 3: Resíduo após (%.4f) deve ser menor que antes (%.4f)",
                        result.residualAfter(), result.residualBefore()));
        assertEquals(0, result.grossErrorsDetected(), "Não deve haver erros grosseiros na rampa/escada limpa");
    }

    @Test
    @DisplayName("4. Detector de erro grosseiro deve identificar e sinalizar pico anômalo injetado")
    void grossErrorDetectorMustDetectAndFilterAnomalousSpike() {
        KinematicReconciler reconciler = new KinematicReconciler();
        List<Fix> cleanWindow = RealWalkingDataFixtures.window1FlatSteadyWalk();

        // Injetando pico falso anômalo de aceleração no meio da caminhada (a_z = 35 m/s^2)
        List<Fix> corruptedWindow = new ArrayList<>(cleanWindow);
        Fix origFix = corruptedWindow.get(4);
        SensorReading origReading = origFix.reading();
        SensorReading corruptedReading = new SensorReading(
                origReading.timestampMillis(),
                origReading.accelerationX(),
                origReading.accelerationY(),
                35.0, // Pico anômalo de ~35 m/s^2 (erro grosseiro)
                origReading.speedMetersPerSecond(),
                origReading.latitude(),
                origReading.longitude(),
                origReading.accelerationAccuracyMetersPerSecondSquared(),
                origReading.positionAccuracyMeters(),
                origReading.speedAccuracyMetersPerSecond()
        );
        corruptedWindow.set(4, new Fix(origFix.timestampMillis(), corruptedReading));

        ReconciliationContract.ReconciliationResult result = reconciler.reconcile(corruptedWindow);

        assertNotNull(result);
        assertTrue(result.grossErrorsDetected() > 0, "Deve detectar o erro grosseiro injetado");
        assertTrue(result.corrected().size() < corruptedWindow.size(), "Lista corrigida deve ter removido a amostra anômala");
    }

    @Test
    @DisplayName("5. Desativar o detector de erro grosseiro degrada quantitativamente a qualidade da reconciliação")
    void disablingGrossErrorDetectorDegradesReconciliationQualityOnCorruptedData() {
        GrossErrorDetector detector = new GrossErrorDetector();
        KinematicReconciler reconciler = new KinematicReconciler(detector);

        // Prepara janela corrompida com pico anômalo
        List<Fix> cleanWindow = RealWalkingDataFixtures.window1FlatSteadyWalk();
        List<Fix> corruptedWindow = new ArrayList<>(cleanWindow);
        Fix origFix = corruptedWindow.get(4);
        SensorReading origReading = origFix.reading();
        SensorReading corruptedReading = new SensorReading(
                origReading.timestampMillis(),
                origReading.accelerationX(),
                origReading.accelerationY(),
                30.0, // Pico anômalo de 30 m/s^2
                origReading.speedMetersPerSecond(),
                origReading.latitude(),
                origReading.longitude(),
                origReading.accelerationAccuracyMetersPerSecondSquared(),
                origReading.positionAccuracyMeters(),
                origReading.speedAccuracyMetersPerSecond()
        );
        corruptedWindow.set(4, new Fix(origFix.timestampMillis(), corruptedReading));

        // 1. Executa COM o detector ativado
        reconciler.setGrossErrorDetectionEnabled(true);
        ReconciliationContract.ReconciliationResult resultWithDetector = reconciler.reconcile(corruptedWindow);

        // 2. Executa SEM o detector ativado
        reconciler.setGrossErrorDetectionEnabled(false);
        ReconciliationContract.ReconciliationResult resultWithoutDetector = reconciler.reconcile(corruptedWindow);

        // O resíduo final SEM o detector deve ser substancialmente MAIOR do que COM o detector (degradação da estimativa)
        assertTrue(resultWithoutDetector.residualAfter() > resultWithDetector.residualAfter(),
                String.format("Resíduo SEM detector (%.4f) deve ser maior que COM detector (%.4f)",
                        resultWithoutDetector.residualAfter(), resultWithDetector.residualAfter()));
    }
}
