package kaptainwutax.seedcrackerX.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import kaptainwutax.seedcrackerX.config.Config;
import kaptainwutax.seedcrackerX.cracker.storage.TimeMachine;
import kaptainwutax.seedcrackerX.util.ClientBiomeLocator;
import kaptainwutax.seedcrackerX.util.ClientStructureLocator;
import kaptainwutax.seedcrackerX.util.SpawnHabitatLocator;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Set;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class LocateCommand extends ClientCommand {
    @Override
    public String getName() {
        return "locate";
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        builder.then(literal("seed")
                        .then(argument("world-seed", LongArgumentType.longArg())
                                .executes(context -> setSeed(LongArgumentType.getLong(context, "world-seed")))))
                .then(literal("biome")
                        .then(argument("biome", StringArgumentType.word())
                                .suggests((context, suggestions) -> suggestBiomes(suggestions))
                                .executes(context -> locateBiome(StringArgumentType.getString(context, "biome"),
                                        ClientStructureLocator.DEFAULT_RADIUS_BLOCKS))
                                .then(argument("radius-blocks", IntegerArgumentType.integer(128,
                                                ClientStructureLocator.MAX_RADIUS_BLOCKS))
                                        .executes(context -> locateBiome(StringArgumentType.getString(context, "biome"),
                                                IntegerArgumentType.getInteger(context, "radius-blocks"))))))
                .then(literal("spawn")
                        .then(argument("entity", StringArgumentType.word())
                                .suggests((context, suggestions) -> {
                                    SpawnHabitatLocator.supportedEntityIds().forEach(suggestions::suggest);
                                    return suggestions.buildFuture();
                                })
                                .executes(context -> locateSpawnHabitat(StringArgumentType.getString(context, "entity"),
                                        ClientStructureLocator.DEFAULT_RADIUS_BLOCKS))
                                .then(argument("radius-blocks", IntegerArgumentType.integer(128,
                                                ClientStructureLocator.MAX_RADIUS_BLOCKS))
                                        .executes(context -> locateSpawnHabitat(StringArgumentType.getString(context, "entity"),
                                                IntegerArgumentType.getInteger(context, "radius-blocks"))))))
                .then(argument("structure", StringArgumentType.word())
                        .suggests((context, suggestions) -> {
                            ClientStructureLocator.supportedIds().forEach(suggestions::suggest);
                            return suggestions.buildFuture();
                        })
                        .executes(context -> locate(StringArgumentType.getString(context, "structure"),
                                ClientStructureLocator.DEFAULT_RADIUS_BLOCKS))
                        .then(argument("radius-blocks", IntegerArgumentType.integer(128,
                                        ClientStructureLocator.MAX_RADIUS_BLOCKS))
                                .executes(context -> locate(StringArgumentType.getString(context, "structure"),
                                        IntegerArgumentType.getInteger(context, "radius-blocks")))));
    }

    private java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestBiomes(
            com.mojang.brigadier.suggestion.SuggestionsBuilder suggestions) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            client.level.registryAccess().lookupOrThrow(Registries.BIOME).keySet().stream()
                    .filter(id -> "minecraft".equals(id.getNamespace()))
                    .map(id -> id.getPath())
                    .sorted()
                    .forEach(suggestions::suggest);
        }
        return suggestions.buildFuture();
    }

    private int setSeed(long seed) {
        Config.get().setKnownWorldSeed(seed);
        Config.save();
        sendFeedback("Client locator seed saved: " + seed, ChatFormatting.GREEN);
        return 1;
    }

    private int locate(String id, int radiusBlocks) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            sendFeedback("Join a world first.", ChatFormatting.RED);
            return 0;
        }
        Long seed = Config.get().getKnownWorldSeed();
        if (seed == null) {
            sendFeedback("No known seed. Use /seedcracker locate seed <world-seed> first.", ChatFormatting.RED);
            return 0;
        }
        if (!ClientStructureLocator.supportedIds().contains(id)) {
            sendFeedback("Unsupported structure. Supported: "
                    + String.join(", ", ClientStructureLocator.supportedIds()), ChatFormatting.RED);
            return 0;
        }
        int x = client.player.getBlockX();
        int z = client.player.getBlockZ();
        sendFeedback("Searching locally for " + id + " within " + radiusBlocks + " blocks...", ChatFormatting.GRAY);
        TimeMachine.SERVICE.submit(() -> {
            try {
                ClientStructureLocator.Result result;
                if ("mineshaft".equals(id)) {
                    result = ClientStructureLocator.locateMineshaft(seed, kaptainwutax.seedcrackerX.Features.MINESHAFT,
                            x, z, radiusBlocks);
                } else if ("stronghold".equals(id)) {
                    result = ClientStructureLocator.locateStronghold(seed, kaptainwutax.seedcrackerX.Features.STRONGHOLD,
                            x, z, radiusBlocks);
                } else {
                    result = ClientStructureLocator.locate(seed, ClientStructureLocator.supported().get(id),
                            x, z, radiusBlocks);
                }
                if (result == null) {
                    sendFeedback("No " + id + " candidate found within " + radiusBlocks + " blocks.", ChatFormatting.YELLOW);
                } else {
                    boolean exact26Village = "village".equals(id)
                            && FabricLoader.getInstance().isModLoaded("seedmapper");
                    String label = exact26Village ? "Nearest " + id : "Nearest " + id + " candidate";
                    sendFeedback(label + ": block " + result.blockX() + ", " + result.blockZ()
                            + " (chunk " + result.chunkX() + "," + result.chunkZ() + "; ~"
                            + result.distanceBlocks() + " blocks).", ChatFormatting.GREEN);
                }
            } catch (Exception exception) {
                sendFeedback("Client locate failed: " + exception.getMessage(), ChatFormatting.RED);
            }
        });
        return 1;
    }

    private int locateBiome(String biomeId, int radiusBlocks) {
        Minecraft client = Minecraft.getInstance();
        if (!ready(client)) return 0;
        if (!FabricLoader.getInstance().isModLoaded("seedmapper")) {
            sendFeedback("Biome locating requires SeedMapper 2.29.1.", ChatFormatting.RED);
            return 0;
        }
        if (!ClientBiomeLocator.supports(biomeId)) {
            sendFeedback("Unsupported/custom biome for vanilla 26.2 prediction: " + biomeId, ChatFormatting.RED);
            return 0;
        }
        long seed = Config.get().getKnownWorldSeed();
        int x = client.player.getBlockX();
        int y = client.player.getBlockY();
        int z = client.player.getBlockZ();
        sendFeedback("Searching locally for biome " + biomeId + "...", ChatFormatting.GRAY);
        TimeMachine.SERVICE.submit(() -> {
            try {
                ClientBiomeLocator.Result result = ClientBiomeLocator.locate(seed, client.level, Set.of(biomeId),
                        x, y, z, radiusBlocks);
                if (result == null) {
                    sendFeedback("No " + biomeId + " found within " + radiusBlocks
                            + " blocks in this dimension.", ChatFormatting.YELLOW);
                } else {
                    sendFeedback("Nearest " + result.biomeId() + " sample: block " + result.blockX() + ", "
                            + result.blockY() + ", " + result.blockZ() + " (~" + result.distanceBlocks()
                            + " blocks).", ChatFormatting.GREEN);
                }
            } catch (Exception exception) {
                sendFeedback("Client biome locate failed: " + exception.getMessage(), ChatFormatting.RED);
            }
        });
        return 1;
    }

    private int locateSpawnHabitat(String entityId, int radiusBlocks) {
        Minecraft client = Minecraft.getInstance();
        if (!ready(client)) return 0;
        if (!FabricLoader.getInstance().isModLoaded("seedmapper")) {
            sendFeedback("Spawn-habitat locating requires SeedMapper 2.29.1.", ChatFormatting.RED);
            return 0;
        }
        Set<String> naturalBiomes = SpawnHabitatLocator.naturalBiomes(client.level, entityId);
        var specialStructures = SpawnHabitatLocator.specialStructures(entityId);
        if (naturalBiomes.isEmpty() && specialStructures.isEmpty()) {
            sendFeedback("No predictable natural habitat known for " + entityId + ".", ChatFormatting.RED);
            return 0;
        }

        long seed = Config.get().getKnownWorldSeed();
        int x = client.player.getBlockX();
        int y = client.player.getBlockY();
        int z = client.player.getBlockZ();
        sendFeedback("Searching for " + entityId + " spawn habitat (not an exact live spawn)...",
                ChatFormatting.GRAY);
        TimeMachine.SERVICE.submit(() -> {
            try {
                ClientBiomeLocator.Result nearestBiome = naturalBiomes.isEmpty() ? null
                        : ClientBiomeLocator.locate(seed, client.level, naturalBiomes, x, y, z, radiusBlocks);
                ClientStructureLocator.Result nearestStructure = null;
                String nearestStructureId = null;
                for (String structureId : specialStructures) {
                    var feature = ClientStructureLocator.supported().get(structureId);
                    if (feature == null) continue;
                    var candidate = ClientStructureLocator.locate(seed, feature, x, z, radiusBlocks);
                    if (candidate != null && (nearestStructure == null
                            || candidate.distanceSquared() < nearestStructure.distanceSquared())) {
                        nearestStructure = candidate;
                        nearestStructureId = structureId;
                    }
                }

                if (nearestBiome == null && nearestStructure == null) {
                    sendFeedback("No " + entityId + " habitat found within " + radiusBlocks + " blocks.",
                            ChatFormatting.YELLOW);
                } else if (nearestStructure != null && (nearestBiome == null
                        || nearestStructure.distanceSquared() <= nearestBiome.distanceSquared())) {
                    sendFeedback("Nearest " + entityId + " habitat: " + nearestStructureId + " near block "
                            + nearestStructure.blockX() + ", " + nearestStructure.blockZ() + " (~"
                            + nearestStructure.distanceBlocks() + " blocks). Spawn is not guaranteed.",
                            ChatFormatting.GREEN);
                } else {
                    sendFeedback("Nearest " + entityId + " natural-spawn biome: " + nearestBiome.biomeId()
                            + " near block " + nearestBiome.blockX() + ", " + nearestBiome.blockY() + ", "
                            + nearestBiome.blockZ() + " (~" + nearestBiome.distanceBlocks()
                            + " blocks). Light/block/cap rules still apply.", ChatFormatting.GREEN);
                }
            } catch (Exception exception) {
                sendFeedback("Client spawn-habitat locate failed: " + exception.getMessage(), ChatFormatting.RED);
            }
        });
        return 1;
    }

    private boolean ready(Minecraft client) {
        if (client.player == null || client.level == null) {
            sendFeedback("Join a world first.", ChatFormatting.RED);
            return false;
        }
        if (Config.get().getKnownWorldSeed() == null) {
            sendFeedback("No known seed. Use /seedcracker locate seed <world-seed> first.", ChatFormatting.RED);
            return false;
        }
        return true;
    }
}
