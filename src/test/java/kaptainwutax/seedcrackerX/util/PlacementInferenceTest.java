package kaptainwutax.seedcrackerX.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementInferenceTest {
    @Test
    void handlesNegativeChunksWithFloorDivision() {
        var observations = List.of(
                new PlacementInference.Observation(-31, -31),
                new PlacementInference.Observation(0, 0),
                new PlacementInference.Observation(31, 31));

        var candidates = PlacementInference.infer(observations, 31, 31);

        assertEquals(1, candidates.size());
        assertEquals(30, candidates.getFirst().maximumSeparation());
    }

    @Test
    void rejectsTwoStartsInSameCandidateRegion() {
        var observations = List.of(
                new PlacementInference.Observation(1, 1),
                new PlacementInference.Observation(2, 2));

        assertTrue(PlacementInference.infer(observations, 31, 31).isEmpty());
    }

    @Test
    void reportsRangeInsteadOfInventingExactSeparation() {
        var observations = List.of(
                new PlacementInference.Observation(21, 20),
                new PlacementInference.Observation(52, 51));

        var candidate = PlacementInference.infer(observations, 31, 31).getFirst();

        assertEquals(0, candidate.minimumSeparation());
        assertEquals(9, candidate.maximumSeparation());
    }
}
