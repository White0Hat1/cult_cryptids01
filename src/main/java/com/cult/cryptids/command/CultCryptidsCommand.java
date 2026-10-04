package com.cult.cryptids.command;

import com.cult.cryptids.event.BloodMoonEvent;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class CultCryptidsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Корень — /cult_cryptids
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cult_cryptids")
                .requires(src -> src.hasPermission(2));

        // =================================================
        // /cult_cryptids bloodmoon start|stop|status
        // =================================================
        root.then(Commands.literal("bloodmoon")
                .then(Commands.literal("start")
                        .executes(CultCryptidsCommand::bloodmoonStart))
                .then(Commands.literal("stop")
                        .executes(CultCryptidsCommand::bloodmoonStop))
                .then(Commands.literal("status")
                        .executes(CultCryptidsCommand::bloodmoonStatus))
        );

        // =================================================
        // /cult_cryptids time set <dayTime>
        // =================================================
        root.then(Commands.literal("time")
                .then(Commands.literal("set")
                        .then(Commands.argument("dayTime", IntegerArgumentType.integer(0, 24000))
                                .executes(CultCryptidsCommand::timeSet))
                )
        );

        // Регистрация корня
        dispatcher.register(root);

        // Алиасы — /cc и /cult
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
        // 🔇 Сообщение в чат убрано — вместо него кинематографичный текст.
        return 1;
    }

    private static int bloodmoonStop(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!BloodMoonEvent.isActive()) {
            src.sendFailure(Component.literal("§7Кровавая Луна не активна."));
            return 0;
        }
        BloodMoonEvent.stop(src.getServer());
        // 🔇 Сообщение в чат убрано.
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

    // ================= TIME =================

    private static int timeSet(CommandContext<CommandSourceStack> ctx) {
        int time = IntegerArgumentType.getInteger(ctx, "dayTime");
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        level.setDayTime(time);
        src.sendSuccess(() -> Component.literal("§aВремя установлено: §e" + time), true);
        return 1;
    }
}