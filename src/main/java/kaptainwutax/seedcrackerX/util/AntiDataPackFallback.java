package kaptainwutax.seedcrackerX.util;

import kaptainwutax.seedcrackerX.Features;
import kaptainwutax.seedcrackerX.SeedCracker;
import kaptainwutax.seedcrackerX.command.ClientCommand;
import kaptainwutax.seedcrackerX.config.AntiDataPackConfig;
import kaptainwutax.seedcrackerX.config.Config;
import kaptainwutax.seedcrackerX.cracker.DataAddedEvent;
import kaptainwutax.seedcrackerX.cracker.VillageProximityData;
import kaptainwutax.seedcrackerX.cracker.storage.DataStorage;
import kaptainwutax.seedcrackerX.cracker.storage.TimeMachine;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicBoolean;

/** Advances through verified placement profiles after a conclusive zero-candidate structure search. */
public final class AntiDataPackFallback {
    private static final AtomicBoolean TRANSITION_SCHEDULED = new AtomicBoolean();

    private AntiDataPackFallback() {
    }

    public static int start() {
        AntiDataPackConfig anti = Config.get().getAntiDataPack();
        anti.autoFallback = true;
        anti.autoFallbackExhausted = false;
        return apply(AntiDataPackConfig.automaticProfiles().getFirst());
    }

    public static void stop() {
        AntiDataPackConfig anti = Config.get().getAntiDataPack();
        anti.autoFallback = false;
        anti.autoFallbackExhausted = false;
        Config.save();
    }

    public static void searchFailed(TimeMachine failedSearch) {
        AntiDataPackConfig anti = Config.get().getAntiDataPack();
        if (!anti.autoFallback || anti.autoFallbackExhausted || !TRANSITION_SCHEDULED.compareAndSet(false, true)) {
            return;
        }
        Minecraft.getInstance().schedule(() -> {
            try {
                if (SeedCracker.get().getDataStorage().getTimeMachine() != failedSearch) return;
                AntiDataPackConfig current = Config.get().getAntiDataPack();
                AntiDataPackConfig.Mode next = AntiDataPackConfig.nextAutomaticProfile(current.mode);
                if (next == null) {
                    current.autoFallbackExhausted = true;
                    Config.save();
                    ClientCommand.sendFeedback("All verified anti-datapack profiles produced no structure seed.",
                            ChatFormatting.RED);
                    ClientCommand.sendFeedback("Custom/randomized pack likely. Record exact starts with "
                                    + "/seedcracker resilient antidatapack observe <structure> <chunk-x> <chunk-z>, "
                                    + "then run analyze and enter a custom profile.",
                            ChatFormatting.YELLOW);
                    return;
                }
                ClientCommand.sendFeedback("No structure seed with " + current.mode + "; trying " + next + ".",
                        ChatFormatting.YELLOW);
                int restored = apply(next);
                ClientCommand.sendFeedback("Automatic profile " + next + " active; restored " + restored
                        + " structure observations.", ChatFormatting.GREEN);
            } finally {
                TRANSITION_SCHEDULED.set(false);
            }
        });
    }

    public static int apply(AntiDataPackConfig.Mode mode) {
        Config config = Config.get();
        config.getAntiDataPack().mode = mode;
        Config.save();
        Features.init(config.getVersion());
        SeedCracker.get().reset();
        return restoreStructureObservations();
    }

    private static int restoreStructureObservations() {
        int restored = 0;
        DataStorage storage = SeedCracker.get().getDataStorage();
        for (var feature : Features.STRUCTURE_TYPES) {
            String id = feature.getName();
            if (id == null || id.isBlank()) continue;
            if (Config.get().resilientMode && "end_city".equals(id)) continue;
            for (var observation : PlacementObservations.get(id)) {
                if (storage.addBaseData(feature.at(observation.chunkX(), observation.chunkZ()),
                        DataAddedEvent.POKE_LIFTING)) {
                    restored++;
                }
            }
        }
        if (Features.VILLAGE != null) {
            for (var hint : VillageProximityObservations.snapshot()) {
                if (storage.addBaseData(new VillageProximityData(Features.VILLAGE,
                        hint.chunkX(), hint.chunkZ(), hint.radius()), DataAddedEvent.POKE_STRUCTURES)) {
                    restored++;
                }
            }
        }
        return restored;
    }
}
