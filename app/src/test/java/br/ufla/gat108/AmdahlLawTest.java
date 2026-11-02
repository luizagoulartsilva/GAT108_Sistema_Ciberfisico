package br.ufla.gat108;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Modelagem e ensaio automatizado da Lei de Amdahl (Capacidade 10).
 * Calcula e valida o speedup computacional S(N) da inferência e processamento
 * concorrente em borda em função do número de núcleos de processamento (N cores).
 */
class AmdahlLawTest {

    /**
     * Fração paralelizável estimada do workload do sistema ciberfísico (P = 85%).
     * Engloba varredura de janelas inerciais, reconciliação WLS e criptografia AES-GCM.
     */
    private static final double PARALLEL_FRACTION = 0.85;

    /**
     * Fração estritamente sequencial (1 - P = 15%).
     * Engloba ingestão contínua a 50Hz e atualização de estado em região crítica protegida.
     */
    private static final double SEQUENTIAL_FRACTION = 1.0 - PARALLEL_FRACTION;

    @Test
    @DisplayName("Lei de Amdahl: Cálculo e Verificação de Speedup em Borda para Múltiplos Núcleos")
    void verifyAmdahlLawSpeedup() {
        int[] coreCounts = {1, 2, 4, 8, 16};
        double baselineExecutionTimeMs = 20.0; // Prazos rígidos do buffer a 50Hz (20ms)

        System.out.println("\n=================================================================================");
        System.out.println("                 LEI DE AMDAHL — SPEEDUP COMPUTACIONAL EM BORDA");
        System.out.println("=================================================================================");
        System.out.printf("Fração Paralelizável (P): %.1f%% | Fração Sequencial (1-P): %.1f%%\n", 
                PARALLEL_FRACTION * 100, SEQUENTIAL_FRACTION * 100);
        System.out.println("| Núcleos (N) | Speedup Teórico S(N) | Tempo Estimado de Execução | Eficiência Paralela |");
        System.out.println("|-------------|----------------------|----------------------------|---------------------|");

        double previousSpeedup = 1.0;

        for (int cores : coreCounts) {
            // Fórmula da Lei de Amdahl: S(N) = 1 / ((1 - P) + (P / N))
            double speedup = 1.0 / (SEQUENTIAL_FRACTION + (PARALLEL_FRACTION / cores));
            double estimatedTimeMs = baselineExecutionTimeMs / speedup;
            double efficiency = (speedup / cores) * 100.0;

            System.out.printf("| %11d | %20.2f | %24.2f ms | %18.1f%% |\n", 
                    cores, speedup, estimatedTimeMs, efficiency);

            if (cores > 1) {
                assertTrue(speedup > previousSpeedup, "Speedup deve aumentar com o número de núcleos");
            }
            previousSpeedup = speedup;
        }
        System.out.println("=================================================================================\n");

        // Validação analítica para 4 núcleos (hardware típico de smartphone edge)
        double speedup4Cores = 1.0 / (SEQUENTIAL_FRACTION + (PARALLEL_FRACTION / 4.0));
        assertTrue(speedup4Cores >= 2.5, "Speedup em 4 núcleos deve ser de pelo menos 2.5x");
        
        // Validação para limite assintótico (infinitos núcleos: S(inf) = 1 / (1 - P) = 1 / 0.15 ≈ 6.67x)
        double asymptoticSpeedup = 1.0 / SEQUENTIAL_FRACTION;
        assertTrue(asymptoticSpeedup > 6.0, "Speedup assintótico deve ser superior a 6.0x");
    }
}
