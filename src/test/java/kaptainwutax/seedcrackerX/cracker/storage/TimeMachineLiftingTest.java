package kaptainwutax.seedcrackerX.cracker.storage;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.structure.RegionStructure;
import com.seedfinding.mcfeature.structure.Shipwreck;
import com.seedfinding.mcfeature.structure.Village;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeMachineLiftingTest {
    private static final long STRUCTURE_SEED = 0x1234_5678_9ABCL;

    @Test
    void acceptsModFourVillageResidue() {
        Village feature = new Village(new RegionStructure.Config(33, 9, 10387312), MCVersion.v1_21_3);
        CPos start = feature.getInRegion(STRUCTURE_SEED, 3, -2, new ChunkRand());
        assertTrue(TimeMachine.matchesLiftingResidue(feature.at(start.getX(), start.getZ()),
                STRUCTURE_SEED & ((1L << 19) - 1), new ChunkRand()));
    }

    @Test
    void acceptsModTwoCustomShipwreckResidue() {
        Shipwreck feature = new Shipwreck(new RegionStructure.Config(23, 5, 165745295), MCVersion.v1_21_3);
        CPos start = feature.getInRegion(STRUCTURE_SEED, -4, 5, new ChunkRand());
        assertTrue(TimeMachine.matchesLiftingResidue(feature.at(start.getX(), start.getZ()),
                STRUCTURE_SEED & ((1L << 19) - 1), new ChunkRand()));
    }
}
