package com.cult.cryptids.event;

import com.cult.cryptids.ModSounds;
import com.cult.cryptids.network.BloodMoonNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Менеджер события "Blood Moon".
 * Работает через статический флаг + сетевые пакеты для синхронизации с клиентом.
 */
public class BloodMoonEvent {

    // ⚙️ Настройки события
    public static final int DURATION_TICKS = 24000;  // 1 игровой день (20 минут)

    // 🔴 Состояние
    private static boolean active = false;
    private static int remainingTicks = 0;

    // =================== ПУБЛИЧНЫЕ МЕТОДЫ ===================

    public static boolean isActive() {
        return active;
    }

    public static int getRemainingTicks() {
        return remainingTicks;
    }

    /**
     * Запускает событие. Показывает Title + subtitle всем игрокам,
     * переводит время в ночь, задаёт длительность, играет музыку,
     * и шлёт пакет клиенту (красная луна, шейдер, фильтр).
     */
    public static void start(MinecraftServer server) {
        if (active) return;
        active = true;
        remainingTicks = DURATION_TICKS;

        ServerLevel overworld = server.overworld();
        // 🕐 Переводим время на ночь (13000 — начало ночи)
        overworld.setDayTime(13000L);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // 📢 Сообщение в чат (курсивом, тёмно-красным)
            player.sendSystemMessage(Component
                    .literal("Небо затягивают тучи, и небо начинает краснеть... С небес спустился кошмар")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));

            // 🎬 Большой титр по центру
            player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 80, 40));
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("BLOOD MOON")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));

            // 📝 Подзаголовок
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.literal("EVENT | BLOOD MOON")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));

            // 🎵 Музыка — играем локально для каждого игрока
            Level level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.BLOOD_MOON_MUSIC.get(),
                    SoundSource.MUSIC,
                    1.0F, 1.0F);
        }

        // 📡 Отправляем пакет всем клиентам: "событие активно"
        //    → красная луна + шейдер + фильтр включаются
        BloodMoonNetwork.broadcast(true);
    }

    /**
     * Останавливает событие.
     */
    public static void stop(MinecraftServer server) {
        if (!active) return;
        active = false;
        remainingTicks = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 60, 40));
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("Blood Moon has ended")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD)));
        }

        // 📡 Отправляем пакет: "событие закончилось"
        //    → луна обратно ванильная, шейдер выключается
        BloodMoonNetwork.broadcast(false);
    }

    /**
     * Вызывается каждый тик на сервере. Уменьшает таймер, авто-стопает по концу.
     */
    public static void tick(MinecraftServer server) {
        if (!active) return;

        remainingTicks--;
        if (remainingTicks <= 0) {
            stop(server);
        }
    }
}