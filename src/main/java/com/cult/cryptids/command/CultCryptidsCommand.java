package com.cult.cryptids.command;

import com.cult.cryptids.event.BloodMoonEvent;
import com.cult.cryptids.event.TimeFreezeManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class CultCryptidsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cult_cryptids")
                .requires(src -> src.hasPermission(2));

        // /cult_cryptids bloodmoon start|stop|status
        root.then(Commands.literal("bloodmoon")
                .then(Commands.literal("start")
                        .executes(CultCryptidsCommand::bloodmoonStart))
                .then(Commands.literal("stop")
                        .executes(CultCryptidsCommand::bloodmoonStop))
                .then(Commands.literal("status")
                        .executes(CultCryptidsCommand::bloodmoonStatus))
        );

        // /cult_cryptids stoptime <dayTime>|resume|status
        root.then(Commands.literal("stoptime")
                // /cult_cryptids stoptime 12500
                .then(Commands.argument("dayTime", IntegerArgumentType.integer(0, 24000))
                        .executes(CultCryptidsCommand::stopTimeAt))
                .then(Commands.literal("resume")
                        .executes(CultCryptidsCommand::resumeTime))
                .then(Commands.literal("status")
                        .executes(CultCryptidsCommand::stopTimeStatus))
        );

        dispatcher.register(root);

        dispatcher.register(Commands.literal("cc").redirect(dispatcher.getRoot().getChild("cult_cryptids")));
        dispatcher.register(Commands.literal("cult").redirect(dispatcher.getRoot().getChild("cult_cryptids")));
    }

    // ================= BLOOD MOON =================

    private static int bloodmoonStart(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (BloodMoonEvent.isActive()) {
            src.sendFailure(Component.literal("§cКровавая Луна уже активна!"));
            return 0;
        }
        BloodMoonEvent.start(src.getServer());
        return 1;
    }

    private static int bloodmoonStop(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!BloodMoonEvent.isActive()) {
            src.sendFailure(Component.literal("§7Кровавая Луна не активна."));
            return 0;
        }
        BloodMoonEvent.stop(src.getServer());
        return 1;
    }

    private static int bloodmoonStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (BloodMoonEvent.isActive()) {
            int sec = BloodMoonEvent.getRemainingTicks() / 20;
            src.sendSuccess(() -> Component
                    .literal("§cКровавая Луна активна. Осталось: §e" + sec + " сек.")
                    .withStyle(ChatFormatting.DARK_RED), false);
        } else {
            src.sendSuccess(() -> Component
                    .literal("§7Кровавая Луна не активна.")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    // ================= STOPTIME =================

    /** /cult_cryptids stoptime <dayTime> */
    private static int stopTimeAt(CommandContext<CommandSourceStack> ctx) {
        int time = IntegerArgumentType.getInteger(ctx, "dayTime");
        CommandSourceStack src = ctx.getSource();

        TimeFreezeManager.freezeAt(src.getServer(), time);
        src.sendSuccess(() -> Component
                .literal("§aВремя остановлено на тике §e" + time + "§a.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** /cult_cryptids stoptime resume */
    private static int resumeTime(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!TimeFreezeManager.isFrozen()) {
            src.sendFailure(Component.literal("§7Время и так идёт."));
            return 0;
        }
        TimeFreezeManager.unfreeze();
        src.sendSuccess(() -> Component
                .literal("§aВремя снова идёт своим ходом.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** /cult_cryptids stoptime status */
    private static int stopTimeStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (TimeFreezeManager.isFrozen()) {
            long t = TimeFreezeManager.getFrozenDayTime();
            src.sendSuccess(() -> Component
                    .literal("§aВремя §cостановлено §aна тике §e" + t + "§a.")
                    .withStyle(ChatFormatting.GREEN), false);
        } else {
            src.sendSuccess(() -> Component
                    .literal("§7Время идёт.")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }
}