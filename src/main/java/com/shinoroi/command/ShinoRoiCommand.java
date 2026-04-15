package com.shinoroi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import com.shinoroi.network.ModNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Registers the {@code /shinonoroi} command tree.
 *
 * <pre>
 * /shinonoroi skillpoints set   <player> <amount>   — set points to exact value
 * /shinonoroi skillpoints add   <player> <amount>   — add points (use set to reduce)
 * /shinonoroi skillpoints query <player>            — query current points
 * </pre>
 *
 * All sub-commands require operator permission level 2.
 */
public final class ShinoRoiCommand {

    private ShinoRoiCommand() {}

    /** Called from {@link com.shinoroi.ShinoRoi} via NeoForge.EVENT_BUS.addListener. */
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("shinonoroi")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("skillpoints")
                    .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> executeSet(ctx,
                                    EntityArgument.getPlayer(ctx, "player"),
                                    IntegerArgumentType.getInteger(ctx, "amount"))))))
                    .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> executeAdd(ctx,
                                    EntityArgument.getPlayer(ctx, "player"),
                                    IntegerArgumentType.getInteger(ctx, "amount"))))))
                    .then(Commands.literal("query")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> executeQuery(ctx,
                                EntityArgument.getPlayer(ctx, "player"))))))
        );
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private static int executeSet(CommandContext<CommandSourceStack> ctx,
                                   ServerPlayer target, int amount) {
        PlayerData data = target.getData(ModAttachments.PLAYER_DATA.get());
        data.setSkillPoints(amount);
        target.setData(ModAttachments.PLAYER_DATA.get(), data);
        ModNetwork.syncToClient(target, data);

        ctx.getSource().sendSuccess(
            () -> Component.literal("Set " + target.getScoreboardName()
                + "'s points to " + amount + "."),
            true);
        return amount;
    }

    private static int executeAdd(CommandContext<CommandSourceStack> ctx,
                                   ServerPlayer target, int amount) {
        PlayerData data = target.getData(ModAttachments.PLAYER_DATA.get());
        data.addSkillPoints(amount);
        target.setData(ModAttachments.PLAYER_DATA.get(), data);
        ModNetwork.syncToClient(target, data);

        int total = data.getSkillPoints();
        ctx.getSource().sendSuccess(
            () -> Component.literal("Added " + amount + " points to "
                + target.getScoreboardName() + " (now " + total + ")."),
            true);
        return total;
    }

    private static int executeQuery(CommandContext<CommandSourceStack> ctx,
                                     ServerPlayer target) {
        int points = target.getData(ModAttachments.PLAYER_DATA.get()).getSkillPoints();
        ctx.getSource().sendSuccess(
            () -> Component.literal(target.getScoreboardName()
                + " has " + points + " point" + (points == 1 ? "" : "s") + "."),
            false);
        return points;
    }
}
