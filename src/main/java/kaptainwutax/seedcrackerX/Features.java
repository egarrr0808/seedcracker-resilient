package kaptainwutax.seedcrackerX;

import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.Feature;
import com.seedfinding.mcfeature.decorator.DesertWell;
import com.seedfinding.mcfeature.decorator.EndGateway;
import com.seedfinding.mcfeature.structure.BuriedTreasure;
import com.seedfinding.mcfeature.structure.DesertPyramid;
import com.seedfinding.mcfeature.structure.EndCity;
import com.seedfinding.mcfeature.structure.Igloo;
import com.seedfinding.mcfeature.structure.JunglePyramid;
import com.seedfinding.mcfeature.structure.Monument;
import com.seedfinding.mcfeature.structure.PillagerOutpost;
import com.seedfinding.mcfeature.structure.RegionStructure;
import com.seedfinding.mcfeature.structure.Shipwreck;
import com.seedfinding.mcfeature.structure.SwampHut;
import com.seedfinding.mcfeature.structure.Village;
import com.seedfinding.mcfeature.structure.BastionRemnant;
import com.seedfinding.mcfeature.structure.Fortress;
import com.seedfinding.mcfeature.structure.Mansion;
import com.seedfinding.mcfeature.structure.Mineshaft;
import com.seedfinding.mcfeature.structure.NetherFossil;
import com.seedfinding.mcfeature.structure.OceanRuin;
import com.seedfinding.mcfeature.structure.RuinedPortal;
import com.seedfinding.mcfeature.structure.Stronghold;
import com.seedfinding.mccore.state.Dimension;
import kaptainwutax.seedcrackerX.config.AntiDataPackConfig;
import kaptainwutax.seedcrackerX.config.Config;
import kaptainwutax.seedcrackerX.cracker.decorator.DeepDungeon;
import kaptainwutax.seedcrackerX.cracker.decorator.Dungeon;
import kaptainwutax.seedcrackerX.cracker.decorator.EmeraldOre;
import kaptainwutax.seedcrackerX.cracker.decorator.WarpedFungus;
import kaptainwutax.seedcrackerX.finder.Finder;
import kaptainwutax.seedcrackerX.structures.TrialChambers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Features {
    public static final ArrayList<RegionStructure<?, ?>> STRUCTURE_TYPES = new ArrayList<>();

    public static BuriedTreasure BURIED_TREASURE;
    public static DesertPyramid DESERT_PYRAMID;
    public static EndCity END_CITY;
    public static JunglePyramid JUNGLE_PYRAMID;
    public static Monument MONUMENT;
    public static Shipwreck SHIPWRECK;
    public static SwampHut SWAMP_HUT;
    public static PillagerOutpost PILLAGER_OUTPOST;
    public static Igloo IGLOO;
    public static TrialChambers TRIAL_CHAMBERS;
    public static Village VILLAGE;
    public static BastionRemnant BASTION;
    public static Fortress FORTRESS;
    public static NetherFossil NETHER_FOSSIL;
    public static OceanRuin OCEAN_RUIN;
    public static RuinedPortal RUINED_PORTAL;
    public static RuinedPortal RUINED_PORTAL_NETHER;
    public static Mansion MANSION;
    public static Mineshaft MINESHAFT;
    public static Stronghold STRONGHOLD;

    public static EndGateway END_GATEWAY;
    public static DesertWell DESERT_WELL;
    public static EmeraldOre EMERALD_ORE;
    public static Dungeon DUNGEON;
    public static DeepDungeon DEEP_DUNGEON;
    public static WarpedFungus WARPED_FUNGUS;

    public static void init(MCVersion version) {
        STRUCTURE_TYPES.clear();

        BURIED_TREASURE = safe(STRUCTURE_TYPES, Finder.Type.BURIED_TREASURE, () -> buriedTreasure(version));
        DESERT_PYRAMID = safe(STRUCTURE_TYPES, Finder.Type.DESERT_TEMPLE, () -> pyramid(version));
        END_CITY = safe(STRUCTURE_TYPES, Finder.Type.END_CITY, () -> endCity(version));
        JUNGLE_PYRAMID = safe(STRUCTURE_TYPES, Finder.Type.JUNGLE_TEMPLE, () -> jungle(version));
        MONUMENT = safe(STRUCTURE_TYPES, Finder.Type.MONUMENT, () -> monument(version));
        SHIPWRECK = safe(STRUCTURE_TYPES, Finder.Type.SHIPWRECK, () -> shipwreck(version));
        SWAMP_HUT = safe(STRUCTURE_TYPES, Finder.Type.SWAMP_HUT, () -> swampHut(version));
        PILLAGER_OUTPOST = safe(STRUCTURE_TYPES, Finder.Type.PILLAGER_OUTPOST, () -> outpost(version));
        IGLOO = safe(STRUCTURE_TYPES, Finder.Type.IGLOO, () -> igloo(version));
        TRIAL_CHAMBERS = safe(STRUCTURE_TYPES, Finder.Type.TRIAL_CHAMBERS, () -> trialChambers(version));
        VILLAGE = safe(STRUCTURE_TYPES, () -> village(version));
        BASTION = safeLocator(() -> bastion(version));
        FORTRESS = safeLocator(() -> fortress(version));
        NETHER_FOSSIL = safeLocator(() -> netherFossil(version));
        OCEAN_RUIN = safeLocator(() -> oceanRuin(version));
        RUINED_PORTAL = safeLocator(() -> ruinedPortal(Dimension.OVERWORLD, version));
        RUINED_PORTAL_NETHER = safeLocator(() -> ruinedPortal(Dimension.NETHER, version));
        MANSION = safeLocator(() -> mansion(version));
        MINESHAFT = safeLocator(() -> mineshaft(version));
        STRONGHOLD = safeLocator(() -> stronghold(version));

        END_GATEWAY = safe(Finder.Type.END_GATEWAY, () -> new EndGateway(version));
        DESERT_WELL = safe(Finder.Type.DESERT_WELL, () -> new DesertWell(version));
        EMERALD_ORE = safe(Finder.Type.EMERALD_ORE, () -> new EmeraldOre(version));
        DUNGEON = safe(Finder.Type.DUNGEON, () -> new Dungeon(version));
        DEEP_DUNGEON = safe(Finder.Type.DUNGEON, () -> new DeepDungeon(version));
        WARPED_FUNGUS = safe(Finder.Type.WARPED_FUNGUS, () -> new WarpedFungus(version));

        STRUCTURE_TYPES.trimToSize();
    }

    private static AntiDataPackConfig.Placement placement(String id) {
        return kaptainwutax.seedcrackerX.config.Config.get().getAntiDataPack().resolve(id);
    }

    private static RegionStructure.Config config(RegionStructure<?, ?> vanilla, String id) {
        AntiDataPackConfig.Placement value = placement(id);
        return value == null ? new RegionStructure.Config(vanilla.getSpacing(), vanilla.getSeparation(), vanilla.getSalt())
                : new RegionStructure.Config(value.spacing, value.separation,
                value.salt == null ? vanilla.getSalt() : value.salt);
    }

    private static DesertPyramid pyramid(MCVersion version) { DesertPyramid v = new DesertPyramid(version); return new DesertPyramid(config(v, "desert_pyramid"), version); }
    private static BuriedTreasure buriedTreasure(MCVersion version) {
        BuriedTreasure vanilla = new BuriedTreasure(version);
        AntiDataPackConfig.Placement value = placement("buried_treasure");
        return value == null ? vanilla : new BuriedTreasure(new BuriedTreasure.Config(
                value.frequency == null ? vanilla.getChance() : value.frequency,
                value.spacing, value.separation, value.salt == null ? vanilla.getSalt() : value.salt), version);
    }
    private static EndCity endCity(MCVersion version) { EndCity v = new EndCity(version); return new EndCity(config(v, "end_city"), version); }
    private static JunglePyramid jungle(MCVersion version) { JunglePyramid v = new JunglePyramid(version); return new JunglePyramid(config(v, "jungle_pyramid"), version); }
    private static Monument monument(MCVersion version) { Monument v = new Monument(version); return new Monument(config(v, "monument"), version); }
    private static Shipwreck shipwreck(MCVersion version) { Shipwreck v = new Shipwreck(version); return new Shipwreck(config(v, "shipwreck"), version); }
    private static SwampHut swampHut(MCVersion version) { SwampHut v = new SwampHut(version); return new SwampHut(config(v, "swamp_hut"), version); }
    private static Igloo igloo(MCVersion version) { Igloo v = new Igloo(version); return new Igloo(config(v, "igloo"), version); }
    private static Village village(MCVersion version) { Village v = new Village(version); return new Village(config(v, "village"), version); }
    private static TrialChambers trialChambers(MCVersion version) { TrialChambers v = new TrialChambers(version); return new TrialChambers(config(v, "trial_chambers"), version); }
    private static PillagerOutpost outpost(MCVersion version) {
        PillagerOutpost v = new PillagerOutpost(version);
        return new PillagerOutpost(config(v, "pillager_outpost"), version, v.getVillage());
    }
    private static BastionRemnant bastion(MCVersion version) { BastionRemnant v = new BastionRemnant(version); return new BastionRemnant(config(v, "bastion"), version); }
    private static Fortress fortress(MCVersion version) { Fortress v = new Fortress(version); return new Fortress(config(v, "fortress"), version); }
    private static NetherFossil netherFossil(MCVersion version) { NetherFossil v = new NetherFossil(version); return new NetherFossil(config(v, "nether_fossil"), version); }
    private static OceanRuin oceanRuin(MCVersion version) { OceanRuin v = new OceanRuin(version); return new OceanRuin(config(v, "ocean_ruin"), version); }
    private static RuinedPortal ruinedPortal(Dimension dimension, MCVersion version) { RuinedPortal v = new RuinedPortal(dimension, version); return new RuinedPortal(dimension, config(v, "ruined_portal"), version); }
    private static Mansion mansion(MCVersion version) { Mansion v = new Mansion(version); return new Mansion(config(v, "mansion"), version); }
    private static Mineshaft mineshaft(MCVersion version) {
        AntiDataPackConfig.Placement value = placement("mineshaft");
        return value != null && value.frequency != null
                ? new Mineshaft(new Mineshaft.Config(value.frequency), version) : new Mineshaft(version);
    }
    private static Stronghold stronghold(MCVersion version) {
        return Config.get().getAntiDataPack().mode == AntiDataPackConfig.Mode.ULTIMATE_1_0_0
                ? new Stronghold(new Stronghold.Config(32, 2, 120), version) : new Stronghold(version);
    }

    public static boolean isPlacementEvidenceSupported(String id) {
        AntiDataPackConfig.Placement value = placement(id);
        return !"pillager_outpost".equals(id)
                || value == null
                || value.frequency == null
                || value.frequency == 0.2F;
    }

    private static <F extends Feature<?, ?>> F safe(Finder.Type finderType, Supplier<F> lambda) {
        try {
            return lambda.get();
        } catch (Throwable t) {
            SeedCracker.LOGGER.error("Exception thrown loading feature", t);
            finderType.enabled.set(false);
            return null;
        }
    }

    private static <F extends RegionStructure<?, ?>> F safe(List<RegionStructure<?, ?>> list, Finder.Type finderType, Supplier<F> lambda) {
        F initializedFeature = safe(finderType, lambda);
        if (initializedFeature != null) list.add(initializedFeature);
        return initializedFeature;
    }

    private static <F extends RegionStructure<?, ?>> F safe(List<RegionStructure<?, ?>> list, Supplier<F> lambda) {
        try {
            F feature = lambda.get();
            list.add(feature);
            return feature;
        } catch (Throwable throwable) {
            SeedCracker.LOGGER.error("Exception thrown loading manual structure feature", throwable);
            return null;
        }
    }

    private static <F> F safeLocator(Supplier<F> lambda) {
        try {
            return lambda.get();
        } catch (Throwable throwable) {
            SeedCracker.LOGGER.error("Exception thrown loading locator feature", throwable);
            return null;
        }
    }

}
