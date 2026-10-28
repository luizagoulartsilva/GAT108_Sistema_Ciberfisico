package br.ufla.gat108.simulation;

import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Componente injetor de falhas determinísticas (Capacidade 5).
 * Controlado por semente fixa (seed) para injeção reprodutível de:
 * 1. Ruído Gaussiano N(0, sigma^2) nas acelerações;
 * 2. Perda de pacotes/amostras consecutivas;
 * 3. Saltos de posição e picos na telemetria GNSS.
 */
public class FaultInjector {

    private final long seed;
    private Random random;
    private double gaussianNoiseSigma; // Desvio padrão em m/s^2
    private double sampleDropRate;     // Taxa de perda (0.0 = 0%, 0.5 = 50%)
    private double gpsSpikeProbability; // Probabilidade de picos/saltos de GPS

    public FaultInjector(long seed) {
        this(seed, 0.0, 0.0, 0.0);
    }

    public FaultInjector(long seed, double gaussianNoiseSigma, double sampleDropRate, double gpsSpikeProbability) {
        this.seed = seed;
        this.gaussianNoiseSigma = Math.max(0.0, gaussianNoiseSigma);
        this.sampleDropRate = Math.min(1.0, Math.max(0.0, sampleDropRate));
        this.gpsSpikeProbability = Math.min(1.0, Math.max(0.0, gpsSpikeProbability));
        this.random = new Random(seed);
    }

    public void reseed() {
        this.random = new Random(seed);
    }

    public void reseed(long newSeed) {
        this.random = new Random(newSeed);
    }

    public void setGaussianNoiseSigma(double sigma) {
        this.gaussianNoiseSigma = Math.max(0.0, sigma);
    }

    public void setSampleDropRate(double dropRate) {
        this.sampleDropRate = Math.min(1.0, Math.max(0.0, dropRate));
    }

    public void setGpsSpikeProbability(double probability) {
        this.gpsSpikeProbability = Math.min(1.0, Math.max(0.0, probability));
    }

    public long getSeed() {
        return seed;
    }

    /**
     * Aplica injeção de falhas determinística sobre uma leitura imutável.
     * @return Novo Fix com falhas injetadas, ou null caso a amostra tenha sido perdida (drop).
     */
    public Fix inject(Fix originalFix) {
        if (originalFix == null) {
            return null;
        }

        // 1. Simulação de Perda de Pacotes/Amostras
        if (sampleDropRate > 0.0 && random.nextDouble() < sampleDropRate) {
            return null; // Amostra descartada
        }

        SensorReading r = originalFix.reading();

        // 2. Ruído Gaussiano na Aceleração
        double ax = r.accelerationX();
        double ay = r.accelerationY();
        double az = r.accelerationZ();

        if (gaussianNoiseSigma > 0.0) {
            ax += random.nextGaussian() * gaussianNoiseSigma;
            ay += random.nextGaussian() * gaussianNoiseSigma;
            az += random.nextGaussian() * gaussianNoiseSigma;
        }

        // 3. Saltos de Posicionamento e Picos de Velocidade GNSS
        double v = r.speedMetersPerSecond();
        double lat = r.latitude();
        double lon = r.longitude();

        if (gpsSpikeProbability > 0.0 && random.nextDouble() < gpsSpikeProbability) {
            v += random.nextDouble() * 10.0; // Pico súbito de velocidade
            lat += (random.nextGaussian() * 0.001); // Salto de ~100m na latitude
            lon += (random.nextGaussian() * 0.001); // Salto de ~100m na longitude
            // Mantém os limites válidos do domínio
            lat = Math.max(-90.0, Math.min(90.0, lat));
            lon = Math.max(-180.0, Math.min(180.0, lon));
        }

        SensorReading injectedReading = new SensorReading(
                r.timestampMillis(), ax, ay, az, v, lat, lon,
                r.accelerationAccuracyMetersPerSecondSquared(),
                r.positionAccuracyMeters(),
                r.speedAccuracyMetersPerSecond()
        );

        return new Fix(originalFix.timestampMillis(), injectedReading);
    }

    /** Processa uma sequência completa de dados de amostra e aplica a injeção determinística. */
    public List<Fix> injectSequence(List<Fix> originalSequence) {
        List<Fix> result = new ArrayList<>();
        if (originalSequence == null) return result;

        for (Fix fix : originalSequence) {
            Fix injected = inject(fix);
            if (injected != null) {
                result.add(injected);
            }
        }
        return result;
    }
}
