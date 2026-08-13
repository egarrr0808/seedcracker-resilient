package kaptainwutax.seedcrackerX.util;

import com.github.cubiomes.Cubiomes;
import com.github.cubiomes.Generator;
import net.minecraft.world.level.Level;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact vanilla Minecraft 26.2 biome lookup supplied by SeedMapper's Cubiomes runtime. */
public final class ClientBiomeLocator {
    private static final int HORIZONTAL_STEP = 32;

    private ClientBiomeLocator() {
    }

    public static Result locate(long worldSeed, Level level, Collection<String> biomeIds,
                                int originBlockX, int originBlockY, int originBlockZ, int radiusBlocks) {
        int dimension = dimension(level);
        Map<Integer, String> targets = resolveTargets(biomeIds, dimension);
        if (targets.isEmpty()) return null;

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment generator = Generator.allocate(arena);
            Cubiomes.setupGenerator(generator, Cubiomes.MC_26_2(), 0);
            Cubiomes.applySeed(generator, dimension, worldSeed);

            int[] heights = heights(dimension, originBlockY);
            int maxRing = Math.max(1, Math.floorDiv(radiusBlocks + HORIZONTAL_STEP - 1, HORIZONTAL_STEP));
            long maxDistanceSquared = (long) radiusBlocks * radiusBlocks;
            Result nearest = null;
            for (int ring = 0; ring <= maxRing; ring++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    nearest = sample(generator, targets, heights, originBlockX, originBlockZ,
                            dx, -ring, maxDistanceSquared, nearest);
                    if (ring != 0) {
                        nearest = sample(generator, targets, heights, originBlockX, originBlockZ,
                                dx, ring, maxDistanceSquared, nearest);
                    }
                }
                for (int dz = -ring + 1; dz < ring; dz++) {
                    nearest = sample(generator, targets, heights, originBlockX, originBlockZ,
                            -ring, dz, maxDistanceSquared, nearest);
                    if (ring != 0) {
                        nearest = sample(generator, targets, heights, originBlockX, originBlockZ,
                                ring, dz, maxDistanceSquared, nearest);
                    }
                }
                if (nearest != null) {
                    long nextRingDistance = (long) (ring + 1) * HORIZONTAL_STEP;
                    if (nextRingDistance * nextRingDistance > nearest.distanceSquared()) return nearest;
                }
            }
            return nearest;
        }
    }

    public static boolean supports(String biomeId) {
        return resolveBiomeId(biomeId) != null;
    }

    private static Result sample(MemorySegment generator, Map<Integer, String> targets, int[] heights,
                                 int originX, int originZ, int dx, int dz, long maxDistanceSquared,
                                 Result nearest) {
        int blockX = originX + dx * HORIZONTAL_STEP;
        int blockZ = originZ + dz * HORIZONTAL_STEP;
        long distanceSquared = (long) (blockX - originX) * (blockX - originX)
                + (long) (blockZ - originZ) * (blockZ - originZ);
        if (distanceSquared > maxDistanceSquared
                || nearest != null && distanceSquared >= nearest.distanceSquared()) return nearest;

        for (int blockY : heights) {
            int biome = Cubiomes.getBiomeAt(generator, 4, Math.floorDiv(blockX, 4),
                    Math.floorDiv(blockY, 4), Math.floorDiv(blockZ, 4));
            String name = targets.get(biome);
            if (name != null) return new Result(name, blockX, blockY, blockZ, distanceSquared);
        }
        return nearest;
    }

    private static Map<Integer, String> resolveTargets(Collection<String> biomeIds, int dimension) {
        Map<Integer, String> result = new LinkedHashMap<>();
        for (String name : biomeIds) {
            Integer id = resolveBiomeId(name);
            if (id != null && Cubiomes.biomeExists(Cubiomes.MC_26_2(), id) != 0
                    && Cubiomes.getDimension(id) == dimension) {
                result.put(id, stripNamespace(name));
            }
        }
        return result;
    }

    private static Integer resolveBiomeId(String biomeId) {
        String path = stripNamespace(biomeId);
        if (biomeId.contains(":") && !biomeId.startsWith("minecraft:")) return null;
        try {
            Method method = Cubiomes.class.getMethod(path);
            if (method.getReturnType() != int.class || method.getParameterCount() != 0
                    || !Modifier.isStatic(method.getModifiers())) return null;
            if (!method.canAccess(null) && !method.trySetAccessible()) return null;
            return (Integer) method.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String stripNamespace(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    private static int dimension(Level level) {
        if (level.dimension() == Level.NETHER) return Cubiomes.DIM_NETHER();
        if (level.dimension() == Level.END) return Cubiomes.DIM_END();
        return Cubiomes.DIM_OVERWORLD();
    }

    private static int[] heights(int dimension, int originY) {
        if (dimension == Cubiomes.DIM_NETHER()) return orderedHeights(originY, 0, 256, 32);
        if (dimension == Cubiomes.DIM_END()) return orderedHeights(originY, 0, 256, 64);
        return orderedHeights(originY, -64, 320, 32);
    }

    private static int[] orderedHeights(int originY, int minY, int maxY, int step) {
        Map<Integer, Boolean> values = new HashMap<>();
        values.put(Math.max(minY, Math.min(maxY, originY)), true);
        for (int y = minY; y <= maxY; y += step) values.put(y, true);
        return values.keySet().stream()
                .sorted((a, b) -> Integer.compare(Math.abs(a - originY), Math.abs(b - originY)))
                .mapToInt(Integer::intValue)
                .toArray();
    }

    public record Result(String biomeId, int blockX, int blockY, int blockZ, long distanceSquared) {
        public int distanceBlocks() {
            return (int) Math.round(Math.sqrt(distanceSquared));
        }
    }
}
