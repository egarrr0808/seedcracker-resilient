package kaptainwutax.seedcrackerX.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import kaptainwutax.seedcrackerX.SeedCracker;
import kaptainwutax.seedcrackerX.config.Config;
import kaptainwutax.seedcrackerX.cracker.storage.DataStorage;
import kaptainwutax.seedcrackerX.util.CandidateValidator;
import kaptainwutax.seedcrackerX.util.EvidenceExporter;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static com.mojang.brigadier.arguments.LongArgumentType.longArg;
import static com.mojang.brigadier.arguments.LongArgumentType.getLong;

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
        sendFeedback("Ignored server hashes this session: " + storage.getObservedHashedSeedCount(), ChatFormatting.AQUA);
        sendFeedback("Evidence export: " + EvidenceExporter.getCurrentExportPath(), ChatFormatting.GRAY);
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
