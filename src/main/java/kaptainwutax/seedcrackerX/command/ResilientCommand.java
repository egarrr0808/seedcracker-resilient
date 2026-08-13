package kaptainwutax.seedcrackerX.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import kaptainwutax.seedcrackerX.SeedCracker;
import kaptainwutax.seedcrackerX.config.Config;
import kaptainwutax.seedcrackerX.config.AntiDataPackConfig;
import kaptainwutax.seedcrackerX.cracker.storage.DataStorage;
import kaptainwutax.seedcrackerX.cracker.DataAddedEvent;
import kaptainwutax.seedcrackerX.cracker.VillageProximityData;
import kaptainwutax.seedcrackerX.util.CandidateValidator;
import kaptainwutax.seedcrackerX.util.AntiDataPackFallback;
import kaptainwutax.seedcrackerX.util.EvidenceExporter;
import kaptainwutax.seedcrackerX.util.PlacementInference;
import kaptainwutax.seedcrackerX.util.PlacementObservations;
import kaptainwutax.seedcrackerX.util.VillageProximityObservations;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static com.mojang.brigadier.arguments.LongArgumentType.longArg;
import static com.mojang.brigadier.arguments.LongArgumentType.getLong;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;

public class ResilientCommand extends ClientCommand {
    @Override
    public String getName() {
        return "resilient";
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        builder.then(literal("on").executes(context -> setEnabled(true)))
                .then(literal("off").executes(context -> setEnabled(false)))
                .then(literal("status").executes(context -> printStatus()))
                .then(literal("validate")
                        .then(argument("worldSeed", longArg()).executes(context -> validate(getLong(context, "worldSeed")))))
                .then(literal("export").executes(context -> exportEvidence()))
                .then(literal("antidatapack")
                        .then(literal("status").executes(context -> antiStatus()))
                        .then(literal("auto").executes(context -> startAutoProfiles()))
                        .then(literal("auto-off").executes(context -> stopAutoProfiles()))
                        .then(literal("profiles").executes(context -> listProfiles()))
                        .then(literal("off").executes(context -> setAntiMode(AntiDataPackConfig.Mode.VANILLA)))
                        .then(literal("default").executes(context -> setAntiMode(AntiDataPackConfig.Mode.ULTIMATE_1_0_0)))
                        .then(literal("custom").executes(context -> setAntiMode(AntiDataPackConfig.Mode.CUSTOM)))
                        .then(literal("set")
                                .then(argument("structure", word())
                                        .then(argument("spacing", integer(2, 4096))
                                                .then(argument("separation", integer(0, 4095))
                                                        .executes(context -> setCustomPlacement(
                                                                getString(context, "structure"),
                                                                getInteger(context, "spacing"),
                                                                getInteger(context, "separation")))))))
                        .then(literal("set-frequency")
                                .then(argument("structure", word())
                                        .then(argument("frequency", floatArg(0.000001F, 1.0F))
                                                .executes(context -> setCustomFrequency(
                                                        getString(context, "structure"),
                                                        getFloat(context, "frequency"))))))
                        .then(literal("set-salt")
                                .then(argument("structure", word())
                                        .then(argument("salt", integer())
                                                .executes(context -> setCustomSalt(
                                                        getString(context, "structure"),
                                                        getInteger(context, "salt"))))))
                        .then(literal("observe")
                                .then(argument("structure", word())
                                        .then(argument("chunkX", integer())
                                                .then(argument("chunkZ", integer())
                                                        .executes(context -> observe(
                                                                getString(context, "structure"),
                                                                getInteger(context, "chunkX"),
                                                                getInteger(context, "chunkZ")))))))
                        .then(literal("observe-block")
                                .then(argument("structure", word())
                                        .then(argument("blockX", integer())
                                                .then(argument("blockZ", integer())
                                                        .executes(context -> observeBlock(
                                                                getString(context, "structure"),
                                                                getInteger(context, "blockX"),
                                                                getInteger(context, "blockZ")))))))
                        .then(literal("village-here")
                                .executes(context -> observeVillageHere(VillageProximityData.DEFAULT_RADIUS))
                                .then(argument("radius", integer(1, 16))
                                        .executes(context -> observeVillageHere(getInteger(context, "radius")))))
                        .then(literal("analyze")
                                .then(argument("structure", word())
                                        .executes(context -> analyze(getString(context, "structure"), 2, 128))
                                        .then(argument("minSpacing", integer(2, 4096))
                                                .then(argument("maxSpacing", integer(2, 4096))
                                                        .executes(context -> analyze(
                                                                getString(context, "structure"),
                                                                getInteger(context, "minSpacing"),
                                                                getInteger(context, "maxSpacing")))))))
                        .then(literal("clear-observations").executes(context -> clearObservations()))
                        .executes(context -> antiStatus()))
                .executes(context -> printStatus());
    }

    private int setEnabled(boolean enabled) {
        Config config = Config.get();
        config.resilientMode = enabled;
        if (enabled) {
            config.applyResilientDefaults();
        }
        Config.save();
        SeedCracker.get().reset();
        sendFeedback("Resilient mode " + (enabled ? "enabled" : "disabled") + "; collected evidence reset.",
                enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
        return 1;
    }

    private int printStatus() {
        Config config = Config.get();
        DataStorage storage = SeedCracker.get().getDataStorage();
        sendFeedback("Resilient mode: " + (config.resilientMode ? "ON" : "OFF"), ChatFormatting.AQUA);
        sendFeedback("Evidence: " + (int) storage.getBaseBits() + "/" + (int) storage.getWantedBits()
                + " structure bits, " + (int) storage.getLiftingBits() + "/40 lifting bits, "
                + (int) storage.getDecoratorBits() + "/32 decorator bits.", ChatFormatting.AQUA);
        sendFeedback("Stable lifting residues: " + storage.getLiftingResidueBits() + "/20 bits.",
                ChatFormatting.AQUA);
        sendFeedback("Ignored server hashes this session: " + storage.getObservedHashedSeedCount(), ChatFormatting.AQUA);
        sendFeedback("Evidence export: " + EvidenceExporter.getCurrentExportPath(), ChatFormatting.GRAY);
        sendFeedback("Anti-datapack placement: " + config.getAntiDataPack().mode, ChatFormatting.AQUA);
        sendFeedback("Server profile: " + config.getActiveServerKey(), ChatFormatting.GRAY);
        return 1;
    }

    private int setAntiMode(AntiDataPackConfig.Mode mode) {
        Config config = Config.get();
        config.getAntiDataPack().autoFallback = false;
        config.getAntiDataPack().autoFallbackExhausted = false;
        config.getAntiDataPack().mode = mode;
        Config.save();
        kaptainwutax.seedcrackerX.Features.init(config.getVersion());
        SeedCracker.get().reset();
        int restored = restoreStructureObservations();
        sendFeedback("Anti-datapack placement set to " + mode + "; candidates reset; " + restored
                + " structure observations rebuilt.", ChatFormatting.GREEN);
        if (mode == AntiDataPackConfig.Mode.ULTIMATE_1_0_0) {
            sendFeedback("Pillager outpost evidence ignored: custom 0.22 legacy frequency is unsupported.", ChatFormatting.YELLOW);
        }
        return 1;
    }

    private int restoreStructureObservations() {
        int restored = 0;
        DataStorage storage = SeedCracker.get().getDataStorage();
        for (var feature : kaptainwutax.seedcrackerX.Features.STRUCTURE_TYPES) {
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
        if (kaptainwutax.seedcrackerX.Features.VILLAGE != null) {
            for (var hint : VillageProximityObservations.snapshot()) {
                if (storage.addBaseData(new VillageProximityData(kaptainwutax.seedcrackerX.Features.VILLAGE,
                        hint.chunkX(), hint.chunkZ(), hint.radius()), DataAddedEvent.POKE_STRUCTURES)) {
                    restored++;
                }
            }
        }
        return restored;
    }

    private int startAutoProfiles() {
        int restored = AntiDataPackFallback.start();
        sendFeedback("Automatic anti-datapack fallback enabled: VANILLA -> ULTIMATE_1_0_0.",
                ChatFormatting.GREEN);
        sendFeedback("Started with VANILLA; restored " + restored + " structure observations.",
                ChatFormatting.GRAY);
        return 1;
    }

    private int stopAutoProfiles() {
        AntiDataPackFallback.stop();
        sendFeedback("Automatic anti-datapack fallback disabled; current profile kept.", ChatFormatting.YELLOW);
        return 1;
    }

    private int listProfiles() {
        sendFeedback("Verified automatic profiles: VANILLA, ULTIMATE_1_0_0 (published ZIP).",
                ChatFormatting.AQUA);
        sendFeedback("Per-server randomized salts and unverified archives are intentionally not guessed.",
                ChatFormatting.GRAY);
        return 1;
    }

    private int setCustomPlacement(String id, int spacing, int separation) {
        if (!AntiDataPackConfig.SUPPORTED_STRUCTURE_IDS.contains(id)) {
            sendFeedback("No cracking model for " + id + ". Supported: "
                    + String.join(", ", AntiDataPackConfig.SUPPORTED_STRUCTURE_IDS.stream().sorted().toList()),
                    ChatFormatting.RED);
            return 0;
        }
        try {
            AntiDataPackConfig.Placement old = Config.get().getAntiDataPack().custom.get(id);
            Float frequency = old == null ? null : old.frequency;
            Integer salt = old == null ? null : old.salt;
            Config.get().getAntiDataPack().custom.put(id,
                    new AntiDataPackConfig.Placement(spacing, separation, frequency, salt));
            return setAntiMode(AntiDataPackConfig.Mode.CUSTOM);
        } catch (IllegalArgumentException exception) {
            sendFeedback(exception.getMessage(), ChatFormatting.RED);
            return 0;
        }
    }

    private int setCustomFrequency(String id, float frequency) {
        if (!"buried_treasure".equals(id)) {
            sendFeedback("Custom frequency cracking is supported only for buried_treasure; pillager_outpost is logged but ignored.",
                    ChatFormatting.RED);
            return 0;
        }
        AntiDataPackConfig.Placement old = Config.get().getAntiDataPack().custom.get(id);
        if (old == null) {
            sendFeedback("Set spacing/separation for " + id + " first.", ChatFormatting.RED);
            return 0;
        }
        try {
            Config.get().getAntiDataPack().custom.put(id,
                    new AntiDataPackConfig.Placement(old.spacing, old.separation, frequency, old.salt));
            return setAntiMode(AntiDataPackConfig.Mode.CUSTOM);
        } catch (IllegalArgumentException exception) {
            sendFeedback(exception.getMessage(), ChatFormatting.RED);
            return 0;
        }
    }

    private int setCustomSalt(String id, int salt) {
        AntiDataPackConfig.Placement old = Config.get().getAntiDataPack().custom.get(id);
        if (old == null) {
            sendFeedback("Set spacing/separation for " + id + " first.", ChatFormatting.RED);
            return 0;
        }
        Config.get().getAntiDataPack().custom.put(id,
                new AntiDataPackConfig.Placement(old.spacing, old.separation, old.frequency, salt));
        return setAntiMode(AntiDataPackConfig.Mode.CUSTOM);
    }

    private int antiStatus() {
        AntiDataPackConfig anti = Config.get().getAntiDataPack();
        sendFeedback("Anti-datapack placement: " + anti.mode + "; custom entries: " + anti.custom.size() + ".",
                ChatFormatting.AQUA);
        sendFeedback("Automatic fallback: " + (anti.autoFallback ? "ON" : "OFF")
                        + (anti.autoFallbackExhausted ? " (profiles exhausted)" : "") + ".",
                ChatFormatting.AQUA);
        sendFeedback("Observed starts are auto-recorded for supported finders; use observe for villages/other structures.",
                ChatFormatting.GRAY);
        return 1;
    }

    private int observe(String id, int chunkX, int chunkZ) {
        PlacementObservations.add(id, chunkX, chunkZ);
        boolean crackingEvidence = false;
        var feature = kaptainwutax.seedcrackerX.Features.STRUCTURE_TYPES.stream()
                .filter(candidate -> id.equals(candidate.getName()))
                .findFirst();
        if (feature.isPresent() && !(Config.get().resilientMode && "end_city".equals(id))) {
            crackingEvidence = SeedCracker.get().getDataStorage().addBaseData(
                    feature.get().at(chunkX, chunkZ), DataAddedEvent.POKE_LIFTING);
        }
        EvidenceExporter.schedule(SeedCracker.get().getDataStorage());
        sendFeedback("Observed " + id + " start chunk " + chunkX + "," + chunkZ + " ("
                + PlacementObservations.count(id) + " total)"
                + (crackingEvidence ? "; added as cracking evidence." : "."), ChatFormatting.GREEN);
        return 1;
    }

    private int observeBlock(String id, int blockX, int blockZ) {
        return observe(id, Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16));
    }

    private int observeVillageHere(int radius) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            sendFeedback("Join a world first.", ChatFormatting.RED);
            return 0;
        }
        ChunkPos hint = ChunkPos.containing(client.player.blockPosition());
        if (!VillageProximityObservations.add(hint.x(), hint.z(), radius)) {
            sendFeedback("Village proximity hint already recorded here.", ChatFormatting.YELLOW);
            return 0;
        }
        if (kaptainwutax.seedcrackerX.Features.VILLAGE == null) {
            sendFeedback("Village cracking model unavailable.", ChatFormatting.RED);
            return 0;
        }
        var data = new VillageProximityData(kaptainwutax.seedcrackerX.Features.VILLAGE,
                hint.x(), hint.z(), radius);
        boolean added = SeedCracker.get().getDataStorage().addBaseData(data, DataAddedEvent.POKE_STRUCTURES);
        EvidenceExporter.schedule(SeedCracker.get().getDataStorage());
        sendFeedback("Village proximity recorded at chunk " + hint.x() + "," + hint.z()
                + "; start searched within " + radius + " chunks; estimated "
                + String.format(java.util.Locale.ROOT, "%.2f", data.estimatedBits()) + " bits.",
                added ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
        return added ? 1 : 0;
    }

    private int analyze(String id, int minSpacing, int maxSpacing) {
        if (maxSpacing < minSpacing) {
            sendFeedback("maxSpacing must be >= minSpacing.", ChatFormatting.RED);
            return 0;
        }
        var observations = PlacementObservations.get(id);
        var candidates = PlacementInference.infer(observations, minSpacing, maxSpacing);
        sendFeedback(id + ": " + observations.size() + " observations; " + candidates.size()
                + " compatible spacing values.", ChatFormatting.AQUA);
        candidates.stream().limit(12).forEach(candidate -> sendFeedback(
                "spacing " + candidate.spacing() + ", separation 0.." + candidate.maximumSeparation(),
                ChatFormatting.GRAY));
        if (candidates.size() > 12) sendFeedback("Showing first 12; narrow min/max range.", ChatFormatting.YELLOW);
        if (!observations.isEmpty()) {
            sendFeedback("Positive sightings bound separation; they cannot uniquely prove it without absence/seed evidence.",
                    ChatFormatting.YELLOW);
        }
        return candidates.isEmpty() ? 0 : 1;
    }

    private int clearObservations() {
        PlacementObservations.clear();
        VillageProximityObservations.clear();
        EvidenceExporter.schedule(SeedCracker.get().getDataStorage());
        sendFeedback("Placement observations cleared.", ChatFormatting.GREEN);
        return 1;
    }

    private int validate(long worldSeed) {
        CandidateValidator.Result result = CandidateValidator.validate(SeedCracker.get().getDataStorage(), worldSeed);
        ChatFormatting color = result.matches() ? ChatFormatting.GREEN : ChatFormatting.RED;
        sendFeedback("Candidate " + worldSeed + ": " + (result.matches() ? "PASS" : "FAIL")
                + "; matched " + result.matched() + "/" + result.tested()
                + ", inconclusive " + result.inconclusive() + ".", color);
        if (!result.mismatches().isEmpty()) {
            sendFeedback("Mismatches: " + String.join(", ", result.mismatches().stream().limit(8).toList()), ChatFormatting.RED);
        }
        return result.matches() ? 1 : 0;
    }

    private int exportEvidence() {
        EvidenceExporter.schedule(SeedCracker.get().getDataStorage());
        sendFeedback("Evidence export queued: " + EvidenceExporter.getCurrentExportPath(), ChatFormatting.GREEN);
        return 1;
    }
}
