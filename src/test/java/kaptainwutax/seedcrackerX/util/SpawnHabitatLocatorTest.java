package kaptainwutax.seedcrackerX.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnHabitatLocatorTest {
    @Test
    void catsUseBothDeterministicStructureHabitats() {
        assertEquals(List.of("village", "swamp_hut"), SpawnHabitatLocator.specialStructures("cat"));
        assertEquals(List.of("village", "swamp_hut"), SpawnHabitatLocator.specialStructures("minecraft:cat"));
    }

    @Test
    void unknownEntityHasNoHardCodedStructureHabitat() {
        assertTrue(SpawnHabitatLocator.specialStructures("allay").isEmpty());
    }
}
