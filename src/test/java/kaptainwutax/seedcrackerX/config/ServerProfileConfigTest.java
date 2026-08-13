package kaptainwutax.seedcrackerX.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServerProfileConfigTest {
    @Test
    void isolatesSeedAndPlacementByServer() {
        Config config = new Config();
        config.activateServerProfile("server-a:25565");
        config.getAntiDataPack().mode = AntiDataPackConfig.Mode.CUSTOM;
        config.getAntiDataPack().custom.put("trial_chambers",
                new AntiDataPackConfig.Placement(41, 7, null, 123456));
        config.setKnownWorldSeed(123L);

        config.activateServerProfile("server-b:25565");
        assertEquals(AntiDataPackConfig.Mode.VANILLA, config.getAntiDataPack().mode);
        assertNull(config.getKnownWorldSeed());

        config.getAntiDataPack().mode = AntiDataPackConfig.Mode.ULTIMATE_1_0_0;
        config.setKnownWorldSeed(456L);
        config.activateServerProfile("server-a:25565");

        assertEquals(AntiDataPackConfig.Mode.CUSTOM, config.getAntiDataPack().mode);
        assertEquals(41, config.getAntiDataPack().resolve("trial_chambers").spacing);
        assertEquals(123L, config.getKnownWorldSeed());
    }
}
