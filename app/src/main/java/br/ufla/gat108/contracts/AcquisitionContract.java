package br.ufla.gat108.contracts;

import br.ufla.gat108.domain.Fix;

/** 
 * Camada 1 (Aquisição): Responsável pela interface direta com os sensores físicos do smartphone.
 * Este contrato define a leitura bruta, preservando o instante temporal e a incerteza (accuracy)
 * informada pelas APIs do Android (SensorManager e Location).
 */
public interface AcquisitionContract {
    /** 
     * Realiza a captura de uma amostra normalizada dos sensores.
     * @return Um objeto {@link br.ufla.gat108.domain.Fix} contendo a telemetria e o timestamp.
     */
    Fix acquire();
}
