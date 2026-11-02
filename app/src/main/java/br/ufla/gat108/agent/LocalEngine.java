package br.ufla.gat108.agent;

import br.ufla.gat108.domain.Event;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.Track;

import java.io.File;
import java.util.Locale;

/**
 * Motor de IA Local (LiteRT / Modelo de Linguagem) (Capacidade 8).
 * Depende de um arquivo de modelo em tempo de execução.
 * Se o arquivo for removido ou corrompido, o motor sinaliza indisponibilidade.
 */
public class LocalEngine implements InferenceEngine {

    private final File modelFile;
    private final double customSensitivityThreshold;

    public LocalEngine(File modelFile) {
        this(modelFile, 25.0);
    }

    public LocalEngine(File modelFile, double customSensitivityThreshold) {
        this.modelFile = modelFile;
        this.customSensitivityThreshold = customSensitivityThreshold;
    }

    @Override
    public Event infer(Track track) {
        if (!isAvailable()) {
            return null; // Indica que o motor de IA não pôde responder por falta de arquivo
        }

        try {
            // Simulação / Execução da inferência do modelo local LiteRT
            if (track == null || track.size() == 0) {
                return new Event(Event.EventType.INSUFFICIENT_EVIDENCE,
                    System.currentTimeMillis(), "Modelo Local: Buffer sem dados");
            }

            Fix latest = track.latest();
            if (latest == null || latest.reading() == null) {
                return new Event(Event.EventType.INSUFFICIENT_EVIDENCE,
                    System.currentTimeMillis(), "Modelo Local: Amostra nula");
            }

            double ax = latest.reading().accelerationX();
            double ay = latest.reading().accelerationY();
            double az = latest.reading().accelerationZ();
            double magnitude = Math.sqrt(ax * ax + ay * ay + az * az);

            if (magnitude >= customSensitivityThreshold) {
                return new Event(
                    Event.EventType.FALL,
                    latest.timestampMillis(),
                    String.format(Locale.US, "Modelo Local (IA): Impacto detectado (%.2f m/s^2)", magnitude)
                );
            }

            return new Event(
                Event.EventType.INSUFFICIENT_EVIDENCE,
                latest.timestampMillis(),
                "Modelo Local (IA): Movimentação normal observada"
            );
        } catch (Exception e) {
            // Falha na inferência local (ex: arquivo removido no meio da leitura)
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return modelFile != null && modelFile.exists() && modelFile.isFile() && modelFile.length() > 0;
    }

    @Override
    public String getName() {
        return "LocalEngine(LiteRT)";
    }

    public File getModelFile() {
        return modelFile;
    }
}
