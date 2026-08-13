package kaptainwutax.seedcrackerX.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Finds random-spread grids compatible with positive structure-start observations. */
public final class PlacementInference {
    private PlacementInference() {
    }

    public static List<Candidate> infer(List<Observation> observations, int minSpacing, int maxSpacing) {
        if (observations.isEmpty()) return List.of();
        if (minSpacing < 2 || maxSpacing < minSpacing) {
            throw new IllegalArgumentException("Expected 2 <= minSpacing <= maxSpacing");
        }

        List<Candidate> candidates = new ArrayList<>();
        for (int spacing = minSpacing; spacing <= maxSpacing; spacing++) {
            int largestOffset = 0;
            Set<Region> regions = new HashSet<>();
            boolean duplicateRegion = false;
            for (Observation observation : observations) {
                int regionX = Math.floorDiv(observation.chunkX(), spacing);
                int regionZ = Math.floorDiv(observation.chunkZ(), spacing);
                largestOffset = Math.max(largestOffset, Math.floorMod(observation.chunkX(), spacing));
                largestOffset = Math.max(largestOffset, Math.floorMod(observation.chunkZ(), spacing));
                if (!regions.add(new Region(regionX, regionZ))) duplicateRegion = true;
            }
            int maximumSeparation = spacing - largestOffset - 1;
            if (!duplicateRegion && maximumSeparation >= 0) {
                candidates.add(new Candidate(spacing, 0, maximumSeparation, observations.size()));
            }
        }
        return candidates;
    }

    public record Observation(int chunkX, int chunkZ) {
    }

    /** Every separation in [minimumSeparation, maximumSeparation] remains possible. */
    public record Candidate(int spacing, int minimumSeparation, int maximumSeparation, int observationCount) {
    }

    private record Region(int x, int z) {
    }
}
