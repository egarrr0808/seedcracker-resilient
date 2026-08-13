package kaptainwutax.seedcrackerX.cracker;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.structure.Village;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillageProximityDataTest {
    @Test
    void acceptsHintNearActualStartWithoutPretendingHintIsStart() {
        Village village = new Village(MCVersion.latest());
        long seed = 123456789L;
        CPos start = village.getInRegion(seed, 3, -2, new ChunkRand());
        VillageProximityData near = new VillageProximityData(village, start.getX() + 5, start.getZ() - 4, 8);
        VillageProximityData far = new VillageProximityData(village, start.getX() + 20, start.getZ() + 20, 8);

        assertTrue(near.testStart(seed, new ChunkRand()));
        assertFalse(far.testStart(seed, new ChunkRand()));
        assertTrue(near.estimatedBits() > 0.0D);
    }
}
