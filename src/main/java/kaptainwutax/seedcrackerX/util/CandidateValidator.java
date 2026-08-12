package kaptainwutax.seedcrackerX.util;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mcfeature.Feature;
import kaptainwutax.seedcrackerX.cracker.decorator.Decorator;
import kaptainwutax.seedcrackerX.cracker.storage.DataStorage;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class CandidateValidator {
    private static final long STRUCTURE_SEED_MASK = (1L << 48) - 1;

    private CandidateValidator() {
    }

    public static Result validate(DataStorage storage, long worldSeed) {
        int matched = 0;
        int inconclusive = 0;
        List<String> mismatches = new ArrayList<>();

        // Only validate against evidence already committed to the cracking pass.
        // Including ScheduledSet's pending entries races an in-flight search: a
        // candidate can be produced from the committed set and immediately be
        // rejected by an observation the search has not consumed yet.
        for (DataStorage.Entry<Feature.Data<?>> entry : storage.getCommittedBaseSeedDataSnapshot()) {
            Feature.Data<?> data = entry.data;
            String label = data.feature.getName() + "@" + data.chunkX + "," + data.chunkZ;
            try {
                boolean matches;
                if (data instanceof Decorator.Data<?> decoratorData) {
                    WorldgenRandom random = new WorldgenRandom(new XoroshiroRandomSource(0));
                    matches = decoratorData.testStart(worldSeed, random);
                } else {
                    matches = data.testStart(worldSeed & STRUCTURE_SEED_MASK, new ChunkRand());
                }

                if (matches) {
                    matched++;
                } else {
                    mismatches.add(label);
                }
            } catch (RuntimeException exception) {
                inconclusive++;
            }
        }

        return new Result(worldSeed, matched, mismatches, inconclusive);
    }

    public static int removeInvalid(DataStorage storage, Collection<Long> candidates) {
        int before = candidates.size();
        candidates.removeIf(candidate -> !validate(storage, candidate).matches());
        return before - candidates.size();
    }

    public record Result(long worldSeed, int matched, List<String> mismatches, int inconclusive) {
        public boolean matches() {
            return mismatches.isEmpty() && matched > 0;
        }

        public int tested() {
            return matched + mismatches.size();
        }
    }
}
