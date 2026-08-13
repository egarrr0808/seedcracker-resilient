package kaptainwutax.seedcrackerX.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientBiomeLocatorTest {
    @Test
    void supportsEveryVanilla262Biome() throws IllegalAccessException {
        List<String> unsupported = new ArrayList<>();
        for (var field : Biomes.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != ResourceKey.class) continue;
            ResourceKey<?> key = (ResourceKey<?>) field.get(null);
            String id = key.identifier().toString();
            if (!ClientBiomeLocator.supports(id)) unsupported.add(id);
        }
        assertTrue(unsupported.isEmpty(), "Unsupported vanilla biomes: " + unsupported);
    }
}
