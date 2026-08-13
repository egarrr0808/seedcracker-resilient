package kaptainwutax.seedcrackerX.mixin;

import kaptainwutax.seedcrackerX.SeedCracker;
import kaptainwutax.seedcrackerX.config.Config;
import kaptainwutax.seedcrackerX.config.StructureSave;
import kaptainwutax.seedcrackerX.cracker.DataAddedEvent;
import kaptainwutax.seedcrackerX.cracker.HashedSeedData;
import kaptainwutax.seedcrackerX.finder.FinderQueue;
import kaptainwutax.seedcrackerX.finder.ReloadFinders;
import kaptainwutax.seedcrackerX.util.Database;
import kaptainwutax.seedcrackerX.util.Log;
import kaptainwutax.seedcrackerX.util.PlacementObservations;
import kaptainwutax.seedcrackerX.util.VillageProximityObservations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Shadow
    private ClientLevel level;

    @Shadow public abstract Connection getConnection();

    @Inject(method = "handleLevelChunkWithLight", at = @At(value = "TAIL"))
    private void onChunkData(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        int chunkX = packet.getX();
        int chunkZ = packet.getZ();
        FinderQueue.get().onChunkData(this.level, new ChunkPos(chunkX, chunkZ));
    }

    @Inject(method = "handleLogin", at = @At(value = "TAIL"))
    public void onGameJoin(ClientboundLoginPacket packet, CallbackInfo ci) {
        Config config = Config.get();
        Minecraft client = Minecraft.getInstance();
        String serverKey = client.getCurrentServer() == null
                ? this.getConnection().getRemoteAddress().toString()
                : client.getCurrentServer().ip;
        config.activateServerProfile(serverKey);
        Config.save();
        kaptainwutax.seedcrackerX.Features.init(config.getVersion());
        PlacementObservations.clear();
        VillageProximityObservations.clear();
        SeedCracker.get().reset();
        newDimension(new HashedSeedData(packet.commonPlayerSpawnInfo().seed()), false);
        tryDatabase();
        var preloaded = StructureSave.loadStructures();
        if (!preloaded.isEmpty()) {
            Log.warn("foundRestorableStructures", preloaded.size());
        }
    }

    @Inject(method = "handleRespawn", at = @At(value = "TAIL"))
    public void onPlayerRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        newDimension(new HashedSeedData(packet.commonPlayerSpawnInfo().seed()), true);
        tryDatabase();
    }

    @Unique
    private void newDimension(HashedSeedData hashedSeedData, boolean dimensionChange) {
        DimensionType dimension = Minecraft.getInstance().level.dimensionType();
        ReloadFinders.reloadHeight(dimension.minY(), dimension.minY() + dimension.logicalHeight());

        if (Config.get().resilientMode) {
            var storage = SeedCracker.get().getDataStorage();
            boolean firstObservation = storage.observeHashedSeed(hashedSeedData.getHashedSeed());
            storage.clearHashedSeedData();
            if (firstObservation && Config.get().debug) {
                Log.warn("Resilient mode ignored server hash [" + hashedSeedData.getHashedSeed() + "]");
            }
            return;
        }

        if (SeedCracker.get().getDataStorage().addHashedSeedData(hashedSeedData, DataAddedEvent.POKE_BIOMES) && Config.get().active && dimensionChange) {
            Log.error(Log.translate("fetchedHashedSeed"));
            if (Config.get().debug) {
                Log.error("Hashed seed [" + hashedSeedData.getHashedSeed() + "]");
            }
        }
    }

    @Unique
    private void tryDatabase() {
        if (Config.get().resilientMode || SeedCracker.get().getDataStorage().hashedSeedData == null) {
            return;
        }
        Long seed = Database.getSeed(this.getConnection().getRemoteAddress().toString(), SeedCracker.get().getDataStorage().hashedSeedData.getHashedSeed());
        if (seed == null) {
            return;
        }
        Log.printSeed("tmachine.foundWorldSeedFromDatabase", seed);
    }
}
