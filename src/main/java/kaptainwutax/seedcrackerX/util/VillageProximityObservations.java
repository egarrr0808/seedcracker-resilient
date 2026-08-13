package kaptainwutax.seedcrackerX.util;

import java.util.ArrayList;
import java.util.List;

public final class VillageProximityObservations {
    private static final List<Hint> HINTS = new ArrayList<>();

    private VillageProximityObservations() {
    }

    public static synchronized boolean add(int chunkX, int chunkZ, int radius) {
        Hint hint = new Hint(chunkX, chunkZ, radius);
        if (HINTS.contains(hint)) return false;
        HINTS.add(hint);
        return true;
    }

    public static synchronized List<Hint> snapshot() {
        return List.copyOf(HINTS);
    }

    public static synchronized void clear() {
        HINTS.clear();
    }

    public record Hint(int chunkX, int chunkZ, int radius) {
    }
}
