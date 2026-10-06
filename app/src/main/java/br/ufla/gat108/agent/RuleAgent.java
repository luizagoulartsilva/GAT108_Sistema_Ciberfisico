package br.ufla.gat108.agent;

import br.ufla.gat108.contracts.AgentContract;
import br.ufla.gat108.domain.Event;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.Track;

import java.util.Locale;

/**
 * Agente de regras (Baseline - Capacidade 8).
 * Detecta quedas através de limiares de impacto.
 */
public class RuleAgent implements AgentContract {

    private static final double FALL_THRESHOLD = 30.0; // m/s^2 (~3g)

    @Override
    public Event decide(Track track) {
        if (track == null || track.size() == 0) {
            return new Event(Event.EventType.INSUFFICIENT_EVIDENCE, 
                System.currentTimeMillis(), "Buffer vazio");
        }

        Fix latest = track.latest();
        if (latest == null || latest.reading() == null) {
            return new Event(Event.EventType.INSUFFICIENT_EVIDENCE,
                System.currentTimeMillis(), "Amostra inválida");
        }

        double ax = latest.reading().accelerationX();
        double ay = latest.reading().accelerationY();
        double az = latest.reading().accelerationZ();
        double magnitude = Math.sqrt(ax * ax + ay * ay + az * az);

        if (magnitude > FALL_THRESHOLD) {
            return new Event(Event.EventType.FALL, latest.timestampMillis(), 
                "Impacto detectado: " + String.format(Locale.US, "%.2f", magnitude) + " m/s^2");
        }

        return new Event(Event.EventType.INSUFFICIENT_EVIDENCE, 
            latest.timestampMillis(), "Nenhum evento crítico detectado");
    }
}
