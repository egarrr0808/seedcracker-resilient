package kaptainwutax.seedcrackerX.util;

import com.seedfinding.mcfeature.Feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class PlacementObservations {
    private static final Map<String, List<PlacementInference.Observation>> OBSERVATIONS = new ConcurrentHashMap<>();

    private PlacementObservations() {
    }

    public static void add(String id, int chunkX, int chunkZ) {
        if (id == null || id.isBlank()) return;
        List<PlacementInference.Observation> values = OBSERVATIONS.computeIfAbsent(id, ignored -> new ArrayList<>());
        synchronized (values) {
            PlacementInference.Observation observation = new PlacementInference.Observation(chunkX, chunkZ);
            if (!values.contains(observation)) values.add(observation);
        }
    }

    public static void add(Feature.Data<?> data) {
        add(data.feature.getName(), data.chunkX, data.chunkZ);
    }

    public static List<PlacementInference.Observation> get(String id) {
        if (id == null || id.isBlank()) return List.of();
        List<PlacementInference.Observation> values = OBSERVATIONS.get(id);
        if (values == null) return List.of();
        synchronized (values) {
            return List.copyOf(values);
        }
    }

    public static int count(String id) {
        return get(id).size();
    }

    public static void clear() {
        OBSERVATIONS.clear();
    }

    public static Map<String, List<PlacementInference.Observation>> snapshot() {
        Map<String, List<PlacementInference.Observation>> result = new LinkedHashMap<>();
        OBSERVATIONS.keySet().stream().sorted().forEach(id -> result.put(id, get(id)));
        return result;
    }
}
