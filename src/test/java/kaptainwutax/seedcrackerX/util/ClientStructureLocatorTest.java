package kaptainwutax.seedcrackerX.util;

import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.structure.RegionStructure;
import com.seedfinding.mcfeature.structure.Village;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClientStructureLocatorTest {
    @Test
    void locatesKnownUltimateProfileVillage() {
        Village village = new Village(new RegionStructure.Config(33, 9, 10387312), MCVersion.v1_21_3);
        ClientStructureLocator.Result result = ClientStructureLocator.locate(
                840022063519098338L, village, 233 * 16 + 8, -21 * 16 + 8, 2_000, MCVersion.v1_21_3);
        assertNotNull(result);
        assertEquals(233, result.chunkX());
        assertEquals(-19, result.chunkZ());
    }
}
