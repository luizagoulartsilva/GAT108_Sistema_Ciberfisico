package br.ufla.gat108.agent;

import br.ufla.gat108.domain.Event;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.Track;

import java.util.List;
import java.util.Locale;

/**
 * Motor determinístico de regras baseado em limiares cinemáticos (Capacidade 8).
 * Serve como linha de base obrigatória e fallback resiliente quando o modelo local for removido.
 */
public class RuleEngine implements InferenceEngine {

    private static final double FALL_IMPACT_THRESHOLD = 28.0; // m/s^2 (~2.85g)
    private static final double IMMOBILITY_ACCEL_VARIANCE_THRESHOLD = 0.3; // m/s^2
    private static final double IMMOBILITY_SPEED_THRESHOLD = 0.3; // m/s

    @Override
    public Event infer(Track track) {
        if (track == null || track.size() == 0) {
            return new Event(Event.EventType.INSUFFICIENT_EVIDENCE,
                System.currentTimeMillis(), "Buffer vazio ou ausente");
        }

        Fix latest = track.latest();
        if (latest == null || latest.reading() == null) {
            return new Event(Event.EventType.INSUFFICIENT_EVIDENCE,
                System.currentTimeMillis(), "Amostra inercial inválida");
        }

        // 1. Verificação de pico de impacto para queda
        double ax = latest.reading().accelerationX();
        double ay = latest.reading().accelerationY();
        double az = latest.reading().accelerationZ();
        double magnitude = Math.sqrt(ax * ax + ay * ay + az * az);

        if (magnitude >= FALL_IMPACT_THRESHOLD) {
            return new Event(
                Event.EventType.FALL,
                latest.timestampMillis(),
                String.format(Locale.US, "Pico de impacto detectado via regra: %.2f m/s^2", magnitude)
            );
        }

        // 2. Verificação de imobilidade prolongada em janela do track
        List<Fix> window = track.getLatestWindow(Math.min(10, track.size()));
        if (window.size() >= 4) {
            boolean isStaticSpeed = true;
            double sumMag = 0.0;
            for (Fix f : window) {
                if (f.reading().speedMetersPerSecond() > IMMOBILITY_SPEED_THRESHOLD) {
                    isStaticSpeed = false;
                }
                double rx = f.reading().accelerationX();
                double ry = f.reading().accelerationY();
                double rz = f.reading().accelerationZ();
                sumMag += Math.sqrt(rx * rx + ry * ry + rz * rz);
            }
            double meanMag = sumMag / window.size();
            double varSum = 0.0;
            for (Fix f : window) {
                double rx = f.reading().accelerationX();
                double ry = f.reading().accelerationY();
                double rz = f.reading().accelerationZ();
                double mag = Math.sqrt(rx * rx + ry * ry + rz * rz);
                varSum += (mag - meanMag) * (mag - meanMag);
            }
            double variance = varSum / window.size();

            if (isStaticSpeed && variance <= IMMOBILITY_ACCEL_VARIANCE_THRESHOLD) {
                return new Event(
                    Event.EventType.IMMOBILITY,
                    latest.timestampMillis(),
                    String.format(Locale.US, "Imobilidade detectada via regra: variância = %.3f m/s^2", variance)
                );
            }
        }

        return new Event(
            Event.EventType.INSUFFICIENT_EVIDENCE,
            latest.timestampMillis(),
            "Evidências normais sem eventos de risco"
        );
    }

    @Override
    public boolean isAvailable() {
        return true; // Motor determinístico local sempre disponível
    }

    @Override
    public String getName() {
        return "RuleEngine";
    }
}
