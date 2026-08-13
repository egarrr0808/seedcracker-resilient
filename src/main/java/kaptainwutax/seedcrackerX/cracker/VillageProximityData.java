package kaptainwutax.seedcrackerX.cracker;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mcfeature.Feature;
import com.seedfinding.mcfeature.structure.Village;
import com.seedfinding.mccore.util.pos.CPos;

/** Disjunctive evidence: village start exists near a known village chunk. */
public final class VillageProximityData extends Feature.Data<Village> {
    public static final int DEFAULT_RADIUS = 8;
    private final int radius;

    public VillageProximityData(Village feature, int hintChunkX, int hintChunkZ, int radius) {
        super(feature, hintChunkX, hintChunkZ);
        if (radius < 1 || radius > 16) throw new IllegalArgumentException("Expected village radius 1..16");
        this.radius = radius;
    }

    public int radius() {
        return radius;
    }

    @Override
    public boolean testStart(long structureSeed, ChunkRand rand) {
        int minX = chunkX - radius;
        int maxX = chunkX + radius;
        int minZ = chunkZ - radius;
        int maxZ = chunkZ + radius;
        int spacing = feature.getSpacing();
        for (int regionX = Math.floorDiv(minX, spacing); regionX <= Math.floorDiv(maxX, spacing); regionX++) {
            for (int regionZ = Math.floorDiv(minZ, spacing); regionZ <= Math.floorDiv(maxZ, spacing); regionZ++) {
                CPos start = feature.getInRegion(structureSeed, regionX, regionZ, rand);
                if (start != null && start.getX() >= minX && start.getX() <= maxX
                        && start.getZ() >= minZ && start.getZ() <= maxZ) return true;
            }
        }
        return false;
    }

    public double estimatedBits() {
        int minX = chunkX - radius;
        int maxX = chunkX + radius;
        int minZ = chunkZ - radius;
        int maxZ = chunkZ + radius;
        int spacing = feature.getSpacing();
        int offset = feature.getOffset();
        double noMatch = 1.0D;
        for (int regionX = Math.floorDiv(minX, spacing); regionX <= Math.floorDiv(maxX, spacing); regionX++) {
            int allowedX = overlap(minX, maxX, regionX * spacing, regionX * spacing + offset - 1);
            for (int regionZ = Math.floorDiv(minZ, spacing); regionZ <= Math.floorDiv(maxZ, spacing); regionZ++) {
                int allowedZ = overlap(minZ, maxZ, regionZ * spacing, regionZ * spacing + offset - 1);
                double match = ((double) allowedX * allowedZ) / ((double) offset * offset);
                noMatch *= 1.0D - match;
            }
        }
        double probability = 1.0D - noMatch;
        return probability <= 0.0D ? 0.0D : -Math.log(probability) / Math.log(2);
    }

    private static int overlap(int firstMin, int firstMax, int secondMin, int secondMax) {
        return Math.max(0, Math.min(firstMax, secondMax) - Math.max(firstMin, secondMin) + 1);
    }
}
