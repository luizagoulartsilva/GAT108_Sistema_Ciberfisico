package br.ufla.gat108;

import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.SensorReading;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixtures contendo três janelas de dados de testes físicos reais/verossímeis de caminhada.
 */
public final class RealWalkingDataFixtures {

    private RealWalkingDataFixtures() { }

    /**
     * Janela 1: Caminhada plana contínua a ritmo constante (~1.3 m/s).
     * Oscilações suaves de aceleração devido à passada do trabalhador.
     */
    public static List<Fix> window1FlatSteadyWalk() {
        List<Fix> window = new ArrayList<>();
        long baseTime = 1700000000000L;

        // [t_sec, v_gps, ax, ay, az, acc_accuracy, speed_accuracy]
        double[][] data = {
                {0.0, 1.20, 0.25,  0.15, 9.95, 0.20, 0.12},
                {1.0, 1.25, 0.30, -0.20, 9.88, 0.20, 0.12},
                {2.0, 1.30, -0.15, 0.22, 10.02, 0.18, 0.10},
                {3.0, 1.28, 0.20, -0.10, 9.75, 0.18, 0.10},
                {4.0, 1.32, -0.28, 0.18, 9.90, 0.19, 0.11},
                {5.0, 1.31, 0.18, -0.15, 10.05, 0.19, 0.11},
                {6.0, 1.35, -0.12, 0.10, 9.80, 0.20, 0.12},
                {7.0, 1.33, 0.22, -0.25, 9.98, 0.20, 0.12},
                {8.0, 1.36, -0.20, 0.15, 9.85, 0.21, 0.13},
                {9.0, 1.34, 0.10, -0.12, 10.00, 0.21, 0.13}
        };

        for (double[] row : data) {
            long t = baseTime + (long)(row[0] * 1000);
            SensorReading reading = new SensorReading(
                    t, row[2], row[3], row[4], row[1],
                    -21.2450, -44.9990, row[5], 3.5, row[6]
            );
            window.add(new Fix(t, reading));
        }
        return window;
    }

    /**
     * Janela 2: Caminhada com aceleração/variação de ritmo (transição de 0.8 m/s para 2.1 m/s).
     */
    public static List<Fix> window2AcceleratedWalk() {
        List<Fix> window = new ArrayList<>();
        long baseTime = 1700000010000L;

        double[][] data = {
                {0.0, 0.80, 0.40, 0.20, 9.90, 0.25, 0.15},
                {1.0, 1.05, 0.55, 0.30, 10.15, 0.25, 0.15},
                {2.0, 1.30, 0.60, 0.25, 10.20, 0.22, 0.14},
                {3.0, 1.55, 0.50, 0.35, 10.10, 0.22, 0.14},
                {4.0, 1.75, 0.45, 0.20, 10.05, 0.20, 0.12},
                {5.0, 1.90, 0.35, 0.25, 9.95, 0.20, 0.12},
                {6.0, 2.00, 0.25, 0.15, 9.88, 0.20, 0.12},
                {7.0, 2.08, 0.20, 0.10, 9.85, 0.20, 0.12},
                {8.0, 2.12, 0.15, 0.08, 9.82, 0.20, 0.12}
        };

        for (double[] row : data) {
            long t = baseTime + (long)(row[0] * 1000);
            SensorReading reading = new SensorReading(
                    t, row[2], row[3], row[4], row[1],
                    -21.2452, -44.9988, row[5], 3.8, row[6]
            );
            window.add(new Fix(t, reading));
        }
        return window;
    }

    /**
     * Janela 3: Caminhada em rampa/escada com forte componente vertical e alteração de postura.
     */
    public static List<Fix> window3InclineStairsWalk() {
        List<Fix> window = new ArrayList<>();
        long baseTime = 1700000020000L;

        double[][] data = {
                {0.0, 0.90, 0.35, 0.45, 10.45, 0.30, 0.18},
                {1.0, 0.95, -0.40, 0.50, 9.25, 0.30, 0.18},
                {2.0, 1.00, 0.50, 0.40, 10.60, 0.28, 0.16},
                {3.0, 0.98, -0.45, 0.55, 9.20, 0.28, 0.16},
                {4.0, 1.02, 0.42, 0.38, 10.50, 0.25, 0.15},
                {5.0, 1.05, -0.38, 0.42, 9.35, 0.25, 0.15},
                {6.0, 1.03, 0.38, 0.45, 10.40, 0.25, 0.15},
                {7.0, 1.08, -0.35, 0.40, 9.40, 0.25, 0.15}
        };

        for (double[] row : data) {
            long t = baseTime + (long)(row[0] * 1000);
            SensorReading reading = new SensorReading(
                    t, row[2], row[3], row[4], row[1],
                    -21.2455, -44.9985, row[5], 4.0, row[6]
            );
            window.add(new Fix(t, reading));
        }
        return window;
    }
}
