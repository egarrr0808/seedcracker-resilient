package kaptainwutax.seedcrackerX.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.seedfinding.mcfeature.Feature;
import kaptainwutax.seedcrackerX.config.Config;
import kaptainwutax.seedcrackerX.cracker.decorator.Decorator;
import kaptainwutax.seedcrackerX.cracker.VillageProximityData;
import kaptainwutax.seedcrackerX.cracker.decorator.Dungeon;
import kaptainwutax.seedcrackerX.cracker.storage.DataStorage;
import kaptainwutax.seedcrackerX.cracker.storage.TimeMachine;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class EvidenceExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger("evidenceExporter");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "seedcracker-evidence-exporter");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicReference<DataStorage> LATEST = new AtomicReference<>();
    private static final AtomicBoolean SCHEDULED = new AtomicBoolean();

    private EvidenceExporter() {
    }

    public static void schedule(DataStorage storage) {
        LATEST.set(storage);
        if (SCHEDULED.compareAndSet(false, true)) {
            EXECUTOR.schedule(EvidenceExporter::exportLatest, 1, TimeUnit.SECONDS);
        }
    }

    public static Path getCurrentExportPath() {
        return getExportDir().resolve(sanitizeFileComponent(getWorldName()) + ".json");
    }

    private static Path getExportDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("seedcracker-resilient").resolve("evidence");
    }

    static String sanitizeFileComponent(String value) {
        String sanitized = value.replaceAll("[^A-Za-z0-9._-]", "_")
                .replaceAll("_+", "_");
        if (sanitized.isBlank() || sanitized.equals(".") || sanitized.equals("..")) {
            sanitized = "unknown-world";
        }
        return sanitized.substring(0, Math.min(96, sanitized.length()));
    }

    private static void exportLatest() {
        DataStorage storage = LATEST.getAndSet(null);
        try {
            if (storage != null) {
                TimeMachine machine = storage.getTimeMachine();
                if (machine.isRunning) {
                    LATEST.set(storage);
                } else {
                    writeSnapshot(storage, machine);
                }
            }
        } catch (Exception exception) {
            LOGGER.error("seedcracker couldn't export evidence", exception);
        } finally {
            SCHEDULED.set(false);
            if (LATEST.get() != null) {
                schedule(LATEST.get());
            }
        }
    }

    private static void writeSnapshot(DataStorage storage, TimeMachine machine) throws IOException {
        Files.createDirectories(getExportDir());
        Path destination = getCurrentExportPath();
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");

        List<Map<String, Object>> evidence = new ArrayList<>();
        for (DataStorage.Entry<Feature.Data<?>> entry : storage.getBaseSeedDataSnapshot()) {
            evidence.add(toEvidence(entry.data));
        }
        evidence.sort(Comparator.<Map<String, Object>, String>comparing(item -> String.valueOf(item.get("feature")))
                .thenComparingInt(item -> ((Number) item.get("chunkX")).intValue())
                .thenComparingInt(item -> ((Number) item.get("chunkZ")).intValue()));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", 2);
        root.put("generatedAt", Instant.now().toString());
        root.put("server", getWorldName());
        root.put("minecraftVersion", Config.get().getVersion().toString());
        root.put("resilientMode", Config.get().resilientMode);
        root.put("antiDataPackMode", Config.get().getAntiDataPack().mode);
        root.put("customPlacements", Config.get().getAntiDataPack().custom);
        root.put("placementObservations", PlacementObservations.snapshot());
        root.put("villageProximityObservations", VillageProximityObservations.snapshot());
        root.put("progress", Map.of(
                "structureBits", storage.getBaseBits(),
                "wantedStructureBits", storage.getWantedBits(),
                "liftingBits", storage.getLiftingBits(),
                "liftingResidueBits", storage.getLiftingResidueBits(),
                "wantedLiftingBits", 40,
                "decoratorBits", storage.getDecoratorBits(),
                "wantedDecoratorBits", 32));
        root.put("ignoredServerHashes", storage.getObservedHashedSeedsSnapshot().stream().sorted().toList());
        root.put("structureSeedCandidates", machine.structureSeeds.stream().sorted().toList());
        root.put("worldSeedCandidates", machine.worldSeeds.stream().sorted().toList());
        root.put("evidence", evidence);

        Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static Map<String, Object> toEvidence(Feature.Data<?> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("feature", data.feature.getName());
        result.put("featureClass", data.feature.getClass().getName());
        result.put("category", data instanceof Decorator.Data<?> ? "decorator" : "structure");
        result.put("chunkX", data.chunkX);
        result.put("chunkZ", data.chunkZ);
        result.put("version", data.feature.getVersion().toString());
        try {
            result.put("bits", DataStorage.getBits(data.feature, true));
        } catch (RuntimeException ignored) {
            result.put("bits", null);
        }

        if (data instanceof Decorator.Data<?> decorator) {
            result.put("biome", decorator.biome.getName());
        }
        if (data instanceof Dungeon.Data dungeon) {
            result.put("blockX", dungeon.blockX);
            result.put("blockY", dungeon.getBlockY());
            result.put("blockZ", dungeon.blockZ);
            result.put("floorCalls", dungeon.floorCalls == null ? null : Arrays.toString(dungeon.floorCalls));
            result.put("heightBottom", dungeon.heightContext == null ? null : dungeon.heightContext.getBottomY());
            result.put("heightTop", dungeon.heightContext == null ? null : dungeon.heightContext.getTopY());
        }
        if (data instanceof VillageProximityData proximity) {
            result.put("evidenceKind", "village_proximity");
            result.put("radiusChunks", proximity.radius());
            result.put("bits", proximity.estimatedBits());
        }
        return result;
    }

    private static String getWorldName() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            return "disconnected";
        }
        Connection connection = client.getConnection().getConnection();
        if (connection.isMemoryConnection() && client.getSingleplayerServer() != null) {
            return client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).getParent().getFileName().toString();
        }
        return connection.getRemoteAddress().toString();
    }
}
