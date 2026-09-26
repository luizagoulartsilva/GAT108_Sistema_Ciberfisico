package br.ufla.gat108.reconciliation;

import br.ufla.gat108.contracts.ReconciliationContract;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.SensorReading;
import br.ufla.gat108.domain.UncertaintyModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KinematicReconciler implements ReconciliationContract {

    private final GrossErrorDetector errorDetector;

    public KinematicReconciler() {
        this(new GrossErrorDetector());
    }

    public KinematicReconciler(GrossErrorDetector errorDetector) {
        this.errorDetector = Objects.requireNonNullElseGet(errorDetector, GrossErrorDetector::new);
    }

    public GrossErrorDetector getErrorDetector() {
        return errorDetector;
    }

    public void setGrossErrorDetectionEnabled(boolean enabled) {
        this.errorDetector.setEnabled(enabled);
    }

    @Override
    public ReconciliationResult reconcile(List<Fix> window) {
        if (window == null || window.size() < 2) {
            return new ReconciliationResult(0, 0, window != null ? window : List.of(), 0);
        }

        // 1. Cálculo do resíduo original (antes do filtro e antes do WLS)
        double totalResidualBefore = calculateKinematicResidual(window);

        // 2. Filtragem de Erros Grosseiros
        GrossErrorDetector.DetectionResult detection = errorDetector.detect(window);
        List<Fix> preFiltered = detection.filteredFixes();

        if (preFiltered.size() < 2) {
            return new ReconciliationResult(totalResidualBefore, totalResidualBefore, window, detection.grossErrorCount());
        }

        // 3. Reconciliação por Mínimos Quadrados Ponderados (WLS)
        double totalResidualAfter = 0;
        List<Fix> corrected = new ArrayList<>();
        corrected.add(preFiltered.get(0));

        for (int i = 1; i < preFiltered.size(); i++) {
            Fix prev = corrected.get(corrected.size() - 1);
            Fix curr = preFiltered.get(i);

            double dt = (curr.timestampMillis() - prev.timestampMillis()) / 1000.0;
            if (dt <= 0) {
                corrected.add(curr);
                continue;
            }

            double vPrev = prev.reading().speedMetersPerSecond();
            double vCurr = curr.reading().speedMetersPerSecond();
            double dvGps = vCurr - vPrev;

            double ax = curr.reading().accelerationX();
            double ay = curr.reading().accelerationY();
            double az = curr.reading().accelerationZ();
            double currentNorm = Math.sqrt(ax * ax + ay * ay + az * az);
            double aMag = currentNorm - 9.81;
            double dvAccel = aMag * dt;

            // Pesos WLS dinâmicos baseados na matriz de incerteza e intervalo dt
            UncertaintyModel model = UncertaintyModel.fromReading(curr.reading());
            double wGps = model.getWeightSpeed();
            double wAccel = model.getWeightAcceleration(dt);

            // Incremento de velocidade ponderado (WLS)
            double dvHat = (wGps * dvGps + wAccel * dvAccel) / (wGps + wAccel + 1e-9);

            // Aceleração e velocidade reconciliadas
            double aHat = dvHat / dt;
            double vHat = Math.max(0.0, vPrev + dvHat);

            // Escalonamento triaxial da aceleração para preservar direção e ajustar magnitude
            double targetNorm = aHat + 9.81;
            double scale = currentNorm > 1e-6 ? targetNorm / currentNorm : 1.0;

            SensorReading r = curr.reading();
            SensorReading correctedReading = new SensorReading(
                    r.timestampMillis(),
                    r.accelerationX() * scale,
                    r.accelerationY() * scale,
                    r.accelerationZ() * scale,
                    vHat,
                    r.latitude(),
                    r.longitude(),
                    r.accelerationAccuracyMetersPerSecondSquared(),
                    r.positionAccuracyMeters(),
                    r.speedAccuracyMetersPerSecond()
            );

            Fix correctedFix = new Fix(curr.timestampMillis(), correctedReading);
            corrected.add(correctedFix);

            // Resíduo recalculado após a reconciliação WLS
            double newDvAccel = aHat * dt;
            double newDvGps = vHat - vPrev;
            totalResidualAfter += Math.abs(newDvGps - newDvAccel);
        }

        return new ReconciliationResult(totalResidualBefore, totalResidualAfter, corrected, detection.grossErrorCount());
    }

    private double calculateKinematicResidual(List<Fix> window) {
        double totalResidual = 0;
        for (int i = 1; i < window.size(); i++) {
            Fix prev = window.get(i - 1);
            Fix curr = window.get(i);

            double dt = (curr.timestampMillis() - prev.timestampMillis()) / 1000.0;
            if (dt <= 0) continue;

            double dvGps = curr.reading().speedMetersPerSecond() - prev.reading().speedMetersPerSecond();

            double ax = curr.reading().accelerationX();
            double ay = curr.reading().accelerationY();
            double az = curr.reading().accelerationZ();
            double aMag = Math.sqrt(ax * ax + ay * ay + az * az) - 9.81;
            double dvAccel = aMag * dt;

            totalResidual += Math.abs(dvGps - dvAccel);
        }
        return totalResidual;
    }
}
