package br.ufla.gat108;

import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.SensorReading;

final class DomainFixtures {
    private DomainFixtures() { }

    static SensorReading reading(long timestamp) {
        return new SensorReading(timestamp, 0.0, 0.0, 9.81, 1.2,
                -21.245, -44.999, 0.15, 4.0, 0.2);
    }

    static Fix fix(long timestamp) {
        return new Fix(timestamp, reading(timestamp));
    }
}
