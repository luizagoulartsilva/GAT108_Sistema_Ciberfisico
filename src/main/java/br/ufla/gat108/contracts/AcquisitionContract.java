package br.ufla.gat108.contracts;

import br.ufla.gat108.domain.Fix;

/** Camada 1: lê o sensor e preserva o instante e a incerteza informada por ele. */
public interface AcquisitionContract {
    Fix acquire();
}
