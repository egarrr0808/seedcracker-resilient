package kaptainwutax.seedcrackerX.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiDataPackConfigTest {
    @Test
    void publicPackProfileUsesActualZipValues() {
        var values = AntiDataPackConfig.ultimateDefaults();

        assertEquals(31, values.get("desert_pyramid").spacing);
        assertEquals(9, values.get("desert_pyramid").separation);
        assertEquals(32, values.get("pillager_outpost").spacing);
        assertEquals(8, values.get("pillager_outpost").separation);
        assertEquals(0.22F, values.get("pillager_outpost").frequency);
        assertEquals(33, values.get("village").spacing);
        assertEquals(9, values.get("village").separation);
    }

    @Test
    void trialChambersCanUseCustomPlacement() {
        assertTrue(AntiDataPackConfig.SUPPORTED_STRUCTURE_IDS.contains("trial_chambers"));
        AntiDataPackConfig config = new AntiDataPackConfig();
        config.mode = AntiDataPackConfig.Mode.CUSTOM;
        config.custom.put("trial_chambers", new AntiDataPackConfig.Placement(41, 7, null, 123456));

        assertEquals(41, config.resolve("trial_chambers").spacing);
        assertEquals(7, config.resolve("trial_chambers").separation);
        assertEquals(123456, config.resolve("trial_chambers").salt);
    }

    @Test
    void automaticProfilesAdvanceOnlyThroughVerifiedArchives() {
        assertEquals(java.util.List.of(AntiDataPackConfig.Mode.VANILLA,
                        AntiDataPackConfig.Mode.ULTIMATE_1_0_0),
                AntiDataPackConfig.automaticProfiles());
        assertEquals(AntiDataPackConfig.Mode.ULTIMATE_1_0_0,
                AntiDataPackConfig.nextAutomaticProfile(AntiDataPackConfig.Mode.VANILLA));
        assertNull(AntiDataPackConfig.nextAutomaticProfile(AntiDataPackConfig.Mode.ULTIMATE_1_0_0));
        assertNull(AntiDataPackConfig.nextAutomaticProfile(AntiDataPackConfig.Mode.CUSTOM));
    }
}
