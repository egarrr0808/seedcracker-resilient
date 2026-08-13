package kaptainwutax.seedcrackerX.util;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Converts an entity ID into predictable biome and structure habitats, never exact spawn blocks. */
public final class SpawnHabitatLocator {
    private static final Map<String, List<String>> SPECIAL_STRUCTURES = specialStructures();

    private SpawnHabitatLocator() {
    }

    public static Set<String> naturalBiomes(ClientLevel level, String entityId) {
        Identifier id = Identifier.tryParse(entityId.contains(":") ? entityId : "minecraft:" + entityId);
        if (id == null) return Set.of();
        EntityType<?> entity = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (entity == null) return Set.of();

        Set<String> result = new LinkedHashSet<>();
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        for (var entry : biomes.entrySet()) {
            boolean present = entry.getValue().getMobSettings().getMobs(entity.getCategory()).unwrap().stream()
                    .anyMatch(weighted -> weighted.value().type() == entity);
            if (present) result.add(entry.getKey().identifier().toString());
        }
        return result;
    }

    public static List<String> specialStructures(String entityId) {
        String path = entityId.startsWith("minecraft:") ? entityId.substring("minecraft:".length()) : entityId;
        return SPECIAL_STRUCTURES.getOrDefault(path, List.of());
    }

    public static Set<String> supportedEntityIds() {
        Set<String> result = new LinkedHashSet<>();
        BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .filter(id -> "minecraft".equals(id.getNamespace()))
                .map(Identifier::getPath)
                .sorted()
                .forEach(result::add);
        return result;
    }

    private static Map<String, List<String>> specialStructures() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put("cat", List.of("village", "swamp_hut"));
        result.put("villager", List.of("village"));
        result.put("iron_golem", List.of("village"));
        result.put("witch", List.of("swamp_hut"));
        result.put("guardian", List.of("monument"));
        result.put("elder_guardian", List.of("monument"));
        result.put("shulker", List.of("end_city"));
        result.put("blaze", List.of("fortress"));
        result.put("wither_skeleton", List.of("fortress"));
        return Map.copyOf(result);
    }
}
