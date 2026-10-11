package com.cult.cryptids.command;

import com.cult.cryptids.entity.SirenHeadEntity;
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
import net.minecraft.world.phys.AABB;

import java.util.List;

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
                .then(Commands.argument("dayTime", IntegerArgumentType.integer(0, 24000))
                        .executes(CultCryptidsCommand::stopTimeAt))
                .then(Commands.literal("resume")
                        .executes(CultCryptidsCommand::resumeTime))
                .then(Commands.literal("status")
                        .executes(CultCryptidsCommand::stopTimeStatus))
        );

        // /cult_cryptids siren crawl|resume|status
        root.then(Commands.literal("siren")
                .then(Commands.literal("crawl")
                        .executes(CultCryptidsCommand::sirenCrawl))
                .then(Commands.literal("resume")
                        .executes(CultCryptidsCommand::sirenResume))
                .then(Commands.literal("status")
                        .executes(CultCryptidsCommand::sirenStatus))
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

    private static int stopTimeAt(CommandContext<CommandSourceStack> ctx) {
        int time = IntegerArgumentType.getInteger(ctx, "dayTime");
        CommandSourceStack src = ctx.getSource();

        TimeFreezeManager.freezeAt(src.getServer(), time);
        src.sendSuccess(() -> Component
                .literal("§aВремя остановлено на тике §e" + time + "§a.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

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

    // ================= SIREN HEAD =================

    private static final double SIREN_SEARCH_RADIUS = 100.0D;

    /** /cult_cryptids siren crawl */
    private static int sirenCrawl(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        List<SirenHeadEntity> nearby = findNearbySirens(src);

        if (nearby.isEmpty()) {
            src.sendFailure(Component.literal("§7Рядом нет Siren Head в радиусе "
                    + (int) SIREN_SEARCH_RADIUS + " блоков."));
            return 0;
        }

        for (SirenHeadEntity siren : nearby) {
            siren.setForceCrawl(true);
        }

        src.sendSuccess(() -> Component
                .literal("§aАктивировано ползание у §e" + nearby.size() + "§a Siren Head.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** /cult_cryptids siren resume */
    private static int sirenResume(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        List<SirenHeadEntity> nearby = findNearbySirens(src);

        if (nearby.isEmpty()) {
            src.sendFailure(Component.literal("§7Рядом нет Siren Head в радиусе "
                    + (int) SIREN_SEARCH_RADIUS + " блоков."));
            return 0;
        }

        for (SirenHeadEntity siren : nearby) {
            siren.setForceCrawl(false);
        }

        src.sendSuccess(() -> Component
                .literal("§aSiren Head вернулись в обычный режим (§e" + nearby.size() + "§a).")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** /cult_cryptids siren status */
    private static int sirenStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        List<SirenHeadEntity> nearby = findNearbySirens(src);

        if (nearby.isEmpty()) {
            src.sendFailure(Component.literal("§7Рядом нет Siren Head в радиусе "
                    + (int) SIREN_SEARCH_RADIUS + " блоков."));
            return 0;
        }

        long crawling = nearby.stream().filter(SirenHeadEntity::isCrawling).count();
        int total = nearby.size();

        src.sendSuccess(() -> Component
                .literal("§aРядом §e" + total + "§a Siren. Ползут: §e" + crawling + "§a.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static List<SirenHeadEntity> findNearbySirens(CommandSourceStack src) {
        var pos = src.getPosition();
        AABB area = new AABB(
                pos.x - SIREN_SEARCH_RADIUS, pos.y - SIREN_SEARCH_RADIUS, pos.z - SIREN_SEARCH_RADIUS,
                pos.x + SIREN_SEARCH_RADIUS, pos.y + SIREN_SEARCH_RADIUS, pos.z + SIREN_SEARCH_RADIUS
        );
        return src.getLevel().getEntitiesOfClass(SirenHeadEntity.class, area);
    }
}