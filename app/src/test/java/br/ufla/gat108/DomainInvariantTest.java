package br.ufla.gat108;

import br.ufla.gat108.domain.Event;
import br.ufla.gat108.domain.InvalidDomainException;
import br.ufla.gat108.domain.Place;
import br.ufla.gat108.domain.Track;
import br.ufla.gat108.domain.Worker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainInvariantTest {
    @Test
    void rejectsNonFiniteAcceleration() {
        assertThrows(InvalidDomainException.class, () ->
                new br.ufla.gat108.domain.SensorReading(0, Double.NaN, 0, 9.81,
                        0, 0, 0, 0.1, 1, 0.1));
    }

    @Test
    void rejectsInvalidWorkerAndPlace() {
        assertThrows(InvalidDomainException.class, () -> new Worker(" "));
        assertThrows(InvalidDomainException.class, () -> new Place(91, 0, 0));
    }

    @Test
    void rejectsNonIncreasingTrackTimestamps() {
        Track track = new Track(2);
        track.append(DomainFixtures.fix(100));

        assertThrows(InvalidDomainException.class, () -> track.append(DomainFixtures.fix(100)));
        assertThrows(InvalidDomainException.class, () -> track.append(DomainFixtures.fix(99)));
    }

    @Test
    void fixedCapacityTrackKeepsOnlyNewestSamples() {
        Track track = new Track(2);
        track.append(DomainFixtures.fix(100));
        track.append(DomainFixtures.fix(200));
        track.append(DomainFixtures.fix(300));

        assertEquals(2, track.size());
        assertEquals(200, track.snapshot()[0].timestampMillis());
        assertEquals(300, track.latest().timestampMillis());
    }

    @Test
    void eventRequiresActionableExplanation() {
        assertThrows(InvalidDomainException.class, () ->
                new Event(Event.EventType.FALL, 100, " "));
    }

    @Test
    void sensorReadingRejectsNegativeAccuracies() {
        assertThrows(InvalidDomainException.class, () ->
                new br.ufla.gat108.domain.SensorReading(0, 0, 0, 9.81,
                        0, 0, 0, -0.1, 1, 0.1));
        assertThrows(InvalidDomainException.class, () ->
                new br.ufla.gat108.domain.SensorReading(0, 0, 0, 9.81,
                        0, 0, 0, 0.1, -1, 0.1));
    }

    @Test
    void placeRejectsNegativeTimestamp() {
        assertThrows(InvalidDomainException.class, () -> new Place(0, 0, -1));
    }

    @Test
    void trackSnapshotReturnsDeepCopy() {
        Track track = new Track(2);
        track.append(DomainFixtures.fix(100));
        
        br.ufla.gat108.domain.Fix[] snapshot1 = track.snapshot();
        br.ufla.gat108.domain.Fix[] snapshot2 = track.snapshot();
        
        assert(snapshot1 != snapshot2);
        assertEquals(snapshot1[0].timestampMillis(), snapshot2[0].timestampMillis());
    }
}
