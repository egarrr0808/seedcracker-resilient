package kaptainwutax.seedcrackerX.util;

import com.github.cubiomes.Cubiomes;
import com.github.cubiomes.Generator;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/** Exact Minecraft 26.2 biome/terrain checks supplied by optional SeedMapper. */
final class Cubiomes26Validator {
    private Cubiomes26Validator() {
    }

    static boolean village(long worldSeed, int chunkX, int chunkZ) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment generator = Generator.allocate(arena);
            Cubiomes.setupGenerator(generator, Cubiomes.MC_26_2(), 0);
            Cubiomes.applySeed(generator, Cubiomes.DIM_OVERWORLD(), worldSeed);
            int blockX = chunkX * 16;
            int blockZ = chunkZ * 16;
            return Cubiomes.isViableStructurePos(Cubiomes.Village(), generator, blockX, blockZ, 0) != 0
                    && Cubiomes.isViableStructureTerrain(Cubiomes.Village(), generator, blockX, blockZ) != 0;
        }
    }
}
