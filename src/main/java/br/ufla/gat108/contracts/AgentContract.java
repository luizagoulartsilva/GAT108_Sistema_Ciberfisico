package br.ufla.gat108.contracts;

import br.ufla.gat108.domain.Event;
import br.ufla.gat108.domain.Track;

/** Camada 3: decide, valida e pode abster-se sem depender de modelo de linguagem. */
public interface AgentContract {
    Event decide(Track track);
}
