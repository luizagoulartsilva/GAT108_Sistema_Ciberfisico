package br.ufla.gat108;

import br.ufla.gat108.contracts.AgentContract;
import br.ufla.gat108.domain.Event;
import br.ufla.gat108.domain.Track;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentContractTest {
    @Test
    void agentMustReturnAnEventOrAbstention() {
        AgentContract agent = track -> {
            throw new UnsupportedOperationException("S1 contract: agent not implemented");
        };

        Event decision = agent.decide(new Track(4));

        assertNotNull(decision);
    }
}
