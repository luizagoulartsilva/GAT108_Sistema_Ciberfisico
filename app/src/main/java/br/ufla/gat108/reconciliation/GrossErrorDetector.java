package br.ufla.gat108.reconciliation;

import br.ufla.gat108.domain.Fix;

import java.util.ArrayList;
import java.util.List;

/**
 * Detector de erro grosseiro baseado no resíduo cinemático normalizado.
 * Rejeita picos anômalos que fogem do envelope físico esperado.
 */
public class GrossErrorDetector {
    private static final double MAX_RESIDUAL_THRESHOLD = 15.0; // m/s^2 * s = m/s

    public List<Fix> filter(List<Fix> window) {
        if (window.size() < 2) return window;

        List<Fix> filtered = new ArrayList<>();
        filtered.add(window.get(0));

        for (int i = 1; i < window.size(); i++) {
            Fix prev = filtered.get(filtered.size() - 1);
            Fix curr = window.get(i);

            double dt = (curr.timestampMillis() - prev.timestampMillis()) / 1000.0;
            if (dt <= 0) continue;

            double dv_gps = curr.reading().speedMetersPerSecond() - prev.reading().speedMetersPerSecond();
            double ax = curr.reading().accelerationX();
            double ay = curr.reading().accelerationY();
            double az = curr.reading().accelerationZ();
            double a_mag = Math.sqrt(ax * ax + ay * ay + az * az) - 9.81;
            double dv_accel = a_mag * dt;

            // Se o resíduo for absurdo, ignoramos esta amostra (erro grosseiro)
            if (Math.abs(dv_gps - dv_accel) > MAX_RESIDUAL_THRESHOLD) {
                // Log ou métrica de rejeição
                continue;
            }
            filtered.add(curr);
        }
        return filtered;
    }
}
