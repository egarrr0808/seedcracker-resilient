package kaptainwutax.seedcrackerX.util;

import com.seedfinding.mcbiome.biome.Biomes;
import com.seedfinding.mccore.version.MCVersion;
import kaptainwutax.seedcrackerX.cracker.decorator.Dungeon;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class EvidenceExporterTest {
    @Test
    void sanitizesRemoteAddressWithoutPathTraversal() {
        assertEquals("_example.org_25565", EvidenceExporter.sanitizeFileComponent("/example.org:25565"));
        assertEquals("unknown-world", EvidenceExporter.sanitizeFileComponent(".."));
        assertFalse(EvidenceExporter.sanitizeFileComponent("../../escape").contains("/"));
    }

    @Test
    void exportsPartiallyInitializedDungeonEvidence() {
        Dungeon feature = new Dungeon(MCVersion.v1_18);
        Dungeon.Data data = feature.at(16, -20, 32, new Vec3i(7, 5, 7), null, Biomes.PLAINS, null);

        Map<String, Object> evidence = EvidenceExporter.toEvidence(data);

        assertEquals(-20, evidence.get("blockY"));
        assertNull(evidence.get("heightBottom"));
        assertNull(evidence.get("heightTop"));
    }
}
