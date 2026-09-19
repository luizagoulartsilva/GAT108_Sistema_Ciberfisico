package br.ufla.gat108.reconciliation;

import br.ufla.gat108.contracts.ReconciliationContract;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.SensorReading;
import br.ufla.gat108.domain.UncertaintyModel;

import java.util.ArrayList;
import java.util.List;

public class KinematicReconciler implements ReconciliationContract {

    private final GrossErrorDetector errorDetector = new GrossErrorDetector();

    @Override
    public ReconciliationResult reconcile(List<Fix> window) {
        if (window == null || window.size() < 2) {
            return new ReconciliationResult(0, 0, window);
        }

        List<Fix> preFiltered = errorDetector.filter(window);
        if (preFiltered.size() < 2) {
            return new ReconciliationResult(0, 0, window);
        }

        double totalResidualBefore = 0;
        double totalResidualAfter = 0;
        List<Fix> corrected = new ArrayList<>();
        corrected.add(window.get(0));

        for (int i = 1; i < window.size(); i++) {
            Fix prev = window.get(i - 1);
            Fix curr = window.get(i);
            
            double dt = (curr.timestampMillis() - prev.timestampMillis()) / 1000.0;
            if (dt <= 0) {
                corrected.add(curr);
                continue;
            }

            // Modelo simplificado: Reconciliação da Velocidade Escalar
            double v_prev = prev.reading().speedMetersPerSecond();
            double v_curr = curr.reading().speedMetersPerSecond();
            
            // Aceleração linear estimada (magnitude - gravidade, simplificado para este exemplo)
            // Em uma implementação real, usaríamos a norma da aceleração triaxial
            double ax = curr.reading().accelerationX();
            double ay = curr.reading().accelerationY();
            double az = curr.reading().accelerationZ();
            double a_mag = Math.sqrt(ax*ax + ay*ay + az*az) - 9.81;

            double dv_gps = v_curr - v_prev;
            double dv_accel = a_mag * dt;
            
            double residual = dv_gps - dv_accel;
            totalResidualBefore += Math.abs(residual);

            // Pesos baseados na incerteza
            UncertaintyModel model = UncertaintyModel.fromReading(curr.reading());
            double w_gps = model.getWeightSpeed();
            double w_accel = model.getWeightAcceleration();

            // Mínimos Quadrados Ponderados (WLS) para o incremento de velocidade
            double dv_hat = (w_gps * dv_gps + w_accel * dv_accel) / (w_gps + w_accel);
            
            // Ajuste: Mantemos a velocidade do GPS (referência absoluta) e ajustamos a aceleração
            double a_hat = dv_hat / dt;
            
            // Reconstruindo a leitura com a aceleração "reconciliada"
            // Para simplicidade, escalonamos os componentes originais
            double scale = (a_hat + 9.81) / (a_mag + 9.81 + 1e-6);
            
            SensorReading r = curr.reading();
            SensorReading correctedReading = new SensorReading(
                r.timestampMillis(),
                r.accelerationX() * scale,
                r.accelerationY() * scale,
                r.accelerationZ() * scale,
                r.speedMetersPerSecond(), // Mantém velocidade GPS como âncora
                r.latitude(),
                r.longitude(),
                r.accelerationAccuracyMetersPerSecondSquared(),
                r.positionAccuracyMeters(),
                r.speedAccuracyMetersPerSecond()
            );

            corrected.add(new Fix(curr.timestampMillis(), correctedReading));
            
            // Novo resíduo (deve ser menor)
            double new_dv_accel = a_hat * dt;
            totalResidualAfter += Math.abs(dv_gps - new_dv_accel);
        }

        return new ReconciliationResult(totalResidualBefore, totalResidualAfter, corrected);
    }
}
