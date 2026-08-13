package kaptainwutax.seedcrackerX.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementObservationsTest {
    @AfterEach
    void clear() {
        PlacementObservations.clear();
    }

    @Test
    void ignoresUnnamedFeatures() {
        PlacementObservations.add(null, 1, 2);
        PlacementObservations.add("", 3, 4);

        assertTrue(PlacementObservations.get(null).isEmpty());
        assertTrue(PlacementObservations.snapshot().isEmpty());
    }
}
