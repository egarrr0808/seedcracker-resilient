package kaptainwutax.seedcrackerX.util;

import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mcbiome.source.EndBiomeSource;
import com.seedfinding.mcbiome.source.NetherBiomeSource;
import com.seedfinding.mcbiome.source.OverworldBiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.structure.RegionStructure;
import com.seedfinding.mcfeature.structure.Mineshaft;
import com.seedfinding.mcfeature.structure.Stronghold;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mcseed.rand.JRand;
import kaptainwutax.seedcrackerX.Features;
import kaptainwutax.seedcrackerX.config.Config;
import net.fabricmc.loader.api.FabricLoader;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientStructureLocator {
    public static final int DEFAULT_RADIUS_BLOCKS = 10_000;
    public static final int MAX_RADIUS_BLOCKS = 100_000;

    private ClientStructureLocator() {
    }

    public static Map<String, RegionStructure<?, ?>> supported() {
        Map<String, RegionStructure<?, ?>> result = new LinkedHashMap<>();
        add(result, "buried_treasure", Features.BURIED_TREASURE);
        add(result, "desert_pyramid", Features.DESERT_PYRAMID);
        add(result, "igloo", Features.IGLOO);
        add(result, "jungle_pyramid", Features.JUNGLE_PYRAMID);
        add(result, "monument", Features.MONUMENT);
        add(result, "shipwreck", Features.SHIPWRECK);
        add(result, "swamp_hut", Features.SWAMP_HUT);
        add(result, "village", Features.VILLAGE);
        add(result, "end_city", Features.END_CITY);
        add(result, "ocean_ruin", Features.OCEAN_RUIN);
        add(result, "mansion", Features.MANSION);
        add(result, "ruined_portal", Features.RUINED_PORTAL);
        add(result, "ruined_portal_nether", Features.RUINED_PORTAL_NETHER);
        add(result, "fortress", Features.FORTRESS);
        add(result, "bastion", Features.BASTION);
        add(result, "nether_fossil", Features.NETHER_FOSSIL);
        return result;
    }

    public static java.util.Set<String> supportedIds() {
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>(supported().keySet());
        if (Features.MINESHAFT != null) result.add("mineshaft");
        if (Features.STRONGHOLD != null) result.add("stronghold");
        return result;
    }

    private static void add(Map<String, RegionStructure<?, ?>> result, String id, RegionStructure<?, ?> feature) {
        if (feature != null) result.put(id, feature);
    }

    public static Result locate(long worldSeed, RegionStructure<?, ?> feature,
                                int originBlockX, int originBlockZ, int radiusBlocks) {
        return locate(worldSeed, feature, originBlockX, originBlockZ, radiusBlocks, Config.get().getVersion());
    }

    static Result locate(long worldSeed, RegionStructure<?, ?> feature,
                         int originBlockX, int originBlockZ, int radiusBlocks, MCVersion version) {
        int radiusChunks = Math.max(1, Math.floorDiv(radiusBlocks + 15, 16));
        int originChunkX = Math.floorDiv(originBlockX, 16);
        int originChunkZ = Math.floorDiv(originBlockZ, 16);
        int spacing = feature.getSpacing();
        int minRegionX = Math.floorDiv(originChunkX - radiusChunks, spacing);
        int maxRegionX = Math.floorDiv(originChunkX + radiusChunks, spacing);
        int minRegionZ = Math.floorDiv(originChunkZ - radiusChunks, spacing);
        int maxRegionZ = Math.floorDiv(originChunkZ + radiusChunks, spacing);
        long structureSeed = worldSeed & ((1L << 48) - 1);
        ChunkRand rand = new ChunkRand();
        BiomeSource biomes = biomeSource(feature.getValidDimension(), version, worldSeed);
        Result nearest = null;
        long maxDistanceSquared = (long) radiusBlocks * radiusBlocks;
        for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
            for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                CPos start = feature.getInRegion(structureSeed, regionX, regionZ, rand);
                if (start == null) continue;
                int blockX = start.getX() * 16 + 8;
                int blockZ = start.getZ() * 16 + 8;
                long dx = (long) blockX - originBlockX;
                long dz = (long) blockZ - originBlockZ;
                long distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > maxDistanceSquared || nearest != null && distanceSquared >= nearest.distanceSquared()) {
                    continue;
                }
                if (feature == Features.VILLAGE && FabricLoader.getInstance().isModLoaded("seedmapper")) {
                    if (!Cubiomes26Validator.village(worldSeed, start.getX(), start.getZ())) continue;
                } else {
                    try {
                        if (!feature.at(start.getX(), start.getZ()).testBiome(biomes)) continue;
                    } catch (RuntimeException exception) {
                        // Never turn failed validation into a claimed structure.
                        continue;
                    }
                }
                nearest = new Result(start.getX(), start.getZ(), blockX, blockZ, distanceSquared);
            }
        }
        return nearest;
    }

    public static Result locateMineshaft(long worldSeed, Mineshaft feature,
                                         int originBlockX, int originBlockZ, int radiusBlocks) {
        int originChunkX = Math.floorDiv(originBlockX, 16);
        int originChunkZ = Math.floorDiv(originBlockZ, 16);
        int radiusChunks = Math.max(1, Math.floorDiv(radiusBlocks + 15, 16));
        long structureSeed = worldSeed & ((1L << 48) - 1);
        ChunkRand rand = new ChunkRand();
        OverworldBiomeSource biomes = new OverworldBiomeSource(Config.get().getVersion(), worldSeed);
        for (int ring = 0; ring <= radiusChunks; ring++) {
            Result nearest = null;
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (ring != 0 && Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
                    int chunkX = originChunkX + dx;
                    int chunkZ = originChunkZ + dz;
                    var data = feature.at(chunkX, chunkZ);
                    if (!data.testStart(structureSeed, rand) || !data.testBiome(biomes)) continue;
                    Result candidate = result(originBlockX, originBlockZ, chunkX, chunkZ);
                    if (candidate.distanceSquared() <= (long) radiusBlocks * radiusBlocks
                            && (nearest == null || candidate.distanceSquared() < nearest.distanceSquared())) {
                        nearest = candidate;
                    }
                }
            }
            if (nearest != null) return nearest;
        }
        return null;
    }

    public static Result locateStronghold(long worldSeed, Stronghold feature,
                                          int originBlockX, int originBlockZ, int radiusBlocks) {
        OverworldBiomeSource biomes = new OverworldBiomeSource(Config.get().getVersion(), worldSeed);
        Result nearest = null;
        for (CPos start : feature.getAllStarts(biomes, new JRand(worldSeed))) {
            Result candidate = result(originBlockX, originBlockZ, start.getX(), start.getZ());
            if (candidate.distanceSquared() <= (long) radiusBlocks * radiusBlocks
                    && (nearest == null || candidate.distanceSquared() < nearest.distanceSquared())) {
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static Result result(int originBlockX, int originBlockZ, int chunkX, int chunkZ) {
        int blockX = chunkX * 16 + 8;
        int blockZ = chunkZ * 16 + 8;
        long dx = (long) blockX - originBlockX;
        long dz = (long) blockZ - originBlockZ;
        return new Result(chunkX, chunkZ, blockX, blockZ, dx * dx + dz * dz);
    }

    private static BiomeSource biomeSource(Dimension dimension, MCVersion version, long worldSeed) {
        if (dimension == Dimension.NETHER) return new NetherBiomeSource(version, worldSeed);
        if (dimension == Dimension.END) return new EndBiomeSource(version, worldSeed);
        return new OverworldBiomeSource(version, worldSeed);
    }

    public record Result(int chunkX, int chunkZ, int blockX, int blockZ, long distanceSquared) {
        public int distanceBlocks() {
            return (int) Math.round(Math.sqrt(distanceSquared));
        }
    }
}
