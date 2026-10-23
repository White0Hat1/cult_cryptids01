package com.cult.cryptids.command;

import com.cult.cryptids.CultCryptidsSavedData;
import com.cult.cryptids.network.BloodMoonNetwork;
import com.mojang.brigadier.CommandDispatcher;
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
                .then(Commands.literal("start").executes(CultCryptidsCommand::bloodmoonStart))
                .then(Commands.literal("stop").executes(CultCryptidsCommand::bloodmoonStop))
                .then(Commands.literal("status").executes(CultCryptidsCommand::bloodmoonStatus))
        );

        dispatcher.register(root);

        dispatcher.register(Commands.literal("cc").redirect(dispatcher.getRoot().getChild("cult_cryptids")));
        dispatcher.register(Commands.literal("cult").redirect(dispatcher.getRoot().getChild("cult_cryptids")));
    }

    // ================= BLOOD MOON =================

    private static int bloodmoonStart(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        CultCryptidsSavedData data = CultCryptidsSavedData.get(src.getServer());
        if (data.isBloodMoon()) {
            src.sendFailure(Component.literal("§cКровавая Луна уже активна!"));
            return 0;
        }
        src.getServer().overworld().setDayTime(13000);
        data.startBloodMoon();
        BloodMoonNetwork.broadcast(true, false);
        return 1;
    }

    private static int bloodmoonStop(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        CultCryptidsSavedData data = CultCryptidsSavedData.get(src.getServer());
        if (!data.isBloodMoon()) {
            src.sendFailure(Component.literal("§7Кровавая Луна не активна."));
            return 0;
        }
        data.stopBloodMoon();
        BloodMoonNetwork.broadcast(false, false);
        return 1;
    }

    private static int bloodmoonStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        CultCryptidsSavedData data = CultCryptidsSavedData.get(src.getServer());
        if (data.isBloodMoon()) {
            src.sendSuccess(() -> Component
                    .literal("§cКровавая Луна активна.")
                    .withStyle(ChatFormatting.DARK_RED), false);
        } else {
            src.sendSuccess(() -> Component
                    .literal("§7Кровавая Луна не активна.")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }
}