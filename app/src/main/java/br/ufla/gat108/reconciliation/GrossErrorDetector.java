package br.ufla.gat108.reconciliation;

import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.UncertaintyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Detector de erro grosseiro baseado no resíduo cinemático normalizado (escore-z)
 * e limites físicos plausíveis de mobilidade humana.
 * Rejeita picos anômalos isolados do acelerômetro e saltos incoerentes de GPS.
 */
public class GrossErrorDetector {
    private static final double DEFAULT_Z_SCORE_THRESHOLD = 3.5; // Limite de 3.5 desvios padrão
    private static final double MAX_PHYSICAL_RESIDUAL_MS = 12.0; // m/s absoluto (~43 km/h em <1s)

    private final double zScoreThreshold;
    private boolean enabled = true;

    public GrossErrorDetector() {
        this(DEFAULT_Z_SCORE_THRESHOLD);
    }

    public GrossErrorDetector(double zScoreThreshold) {
        this.zScoreThreshold = zScoreThreshold;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public record DetectionResult(List<Fix> filteredFixes, List<Fix> rejectedFixes, int grossErrorCount) {}

    public DetectionResult detect(List<Fix> window) {
        if (window == null || window.size() < 2 || !enabled) {
            return new DetectionResult(window != null ? window : List.of(), List.of(), 0);
        }

        List<Fix> filtered = new ArrayList<>();
        List<Fix> rejected = new ArrayList<>();

        filtered.add(window.get(0));

        for (int i = 1; i < window.size(); i++) {
            Fix prev = filtered.get(filtered.size() - 1);
            Fix curr = window.get(i);

            double dt = (curr.timestampMillis() - prev.timestampMillis()) / 1000.0;
            if (dt <= 0) {
                rejected.add(curr);
                continue;
            }

            double dvGps = curr.reading().speedMetersPerSecond() - prev.reading().speedMetersPerSecond();

            double ax = curr.reading().accelerationX();
            double ay = curr.reading().accelerationY();
            double az = curr.reading().accelerationZ();
            double aMag = Math.sqrt(ax * ax + ay * ay + az * az) - 9.81;
            double dvAccel = aMag * dt;

            double absResidual = Math.abs(dvGps - dvAccel);

            UncertaintyModel model = UncertaintyModel.fromReading(curr.reading());
            double sigmaResidual = model.getStandardResidualDev(dt);
            double zScore = absResidual / (sigmaResidual + 1e-9);

            // Rejeita se o resíduo normalizado exceder o limite estatístico ou físico
            if (zScore > zScoreThreshold || absResidual > MAX_PHYSICAL_RESIDUAL_MS) {
                rejected.add(curr);
            } else {
                filtered.add(curr);
            }
        }

        return new DetectionResult(filtered, rejected, rejected.size());
    }

    public List<Fix> filter(List<Fix> window) {
        return detect(window).filteredFixes();
    }
}
