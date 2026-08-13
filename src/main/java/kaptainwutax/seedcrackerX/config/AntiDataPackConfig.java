package kaptainwutax.seedcrackerX.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Placement settings used by the seedfinding model, not server-side detection. */
public class AntiDataPackConfig {
    public static final Set<String> SUPPORTED_STRUCTURE_IDS = Set.of(
            "buried_treasure", "desert_pyramid", "end_city", "igloo", "jungle_pyramid",
            "monument", "pillager_outpost", "shipwreck", "swamp_hut", "village",
            "bastion", "fortress", "nether_fossil", "ocean_ruin", "ruined_portal",
            "mansion", "mineshaft", "trial_chambers");
    public Mode mode = Mode.VANILLA;
    public Map<String, Placement> custom = new LinkedHashMap<>();
    public boolean autoFallback = false;
    public boolean autoFallbackExhausted = false;

    public Placement resolve(String id) {
        return switch (mode) {
            case VANILLA -> null;
            case ULTIMATE_1_0_0 -> ultimateDefaults().get(id);
            case CUSTOM -> custom.get(id);
        };
    }

    public enum Mode {
        VANILLA,
        ULTIMATE_1_0_0,
        CUSTOM
    }

    /** Only profiles verified from their published archive belong in this sequence. */
    public static List<Mode> automaticProfiles() {
        return List.of(Mode.VANILLA, Mode.ULTIMATE_1_0_0);
    }

    public static Mode nextAutomaticProfile(Mode current) {
        List<Mode> profiles = automaticProfiles();
        int index = profiles.indexOf(current);
        return index < 0 || index + 1 >= profiles.size() ? null : profiles.get(index + 1);
    }

    public static class Placement {
        public int spacing;
        public int separation;
        public Float frequency;
        public Integer salt;

        public Placement() {
        }

        public Placement(int spacing, int separation) {
            this(spacing, separation, null);
        }

        public Placement(int spacing, int separation, Float frequency) {
            this(spacing, separation, frequency, null);
        }

        public Placement(int spacing, int separation, Float frequency, Integer salt) {
            if (spacing < 1 || separation < 0 || separation >= spacing) {
                throw new IllegalArgumentException("Expected spacing >= 1 and 0 <= separation < spacing");
            }
            if (frequency != null && (frequency <= 0.0F || frequency > 1.0F)) {
                throw new IllegalArgumentException("Expected 0 < frequency <= 1");
            }
            this.spacing = spacing;
            this.separation = separation;
            this.frequency = frequency;
            this.salt = salt;
        }
    }

    /** Values read from public UltimateAntiSeedCracker 1.0.0 ZIP. */
    public static Map<String, Placement> ultimateDefaults() {
        Map<String, Placement> values = new LinkedHashMap<>();
        values.put("buried_treasure", new Placement(1, 0, 0.012F, 10387320));
        values.put("desert_pyramid", new Placement(31, 9, null, 14357617));
        values.put("end_city", new Placement(21, 10, null, 10387313));
        values.put("igloo", new Placement(31, 9, null, 14357618));
        values.put("jungle_pyramid", new Placement(31, 9, null, 14357619));
        values.put("monument", new Placement(31, 6, null, 10387313));
        // Pack file keeps 32/8 and changes legacy_type_1 frequency to 0.22.
        // Current seedfinding dependency cannot model arbitrary legacy_type_1 frequency.
        values.put("pillager_outpost", new Placement(32, 8, 0.22F, 165745296));
        values.put("shipwreck", new Placement(23, 5, null, 165745295));
        values.put("swamp_hut", new Placement(31, 9, null, 14357620));
        values.put("village", new Placement(33, 9, null, 10387312));
        values.put("bastion", new Placement(26, 5, null, 30084232));
        values.put("fortress", new Placement(26, 5, null, 30084232));
        values.put("nether_fossil", new Placement(3, 1, null, 14357921));
        values.put("ocean_ruin", new Placement(19, 9, null, 14357621));
        values.put("ruined_portal", new Placement(39, 16, null, 34222645));
        values.put("mansion", new Placement(83, 19, null, 10387319));
        values.put("mineshaft", new Placement(1, 0, 0.0037F, 0));
        return values;
    }
}
