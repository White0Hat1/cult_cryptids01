package com.cult.cryptids.event;

import com.cult.cryptids.ModSounds;
import com.cult.cryptids.network.BloodMoonNetwork;
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

    /** 🕐 Время суток, с которого стартует Blood Moon (14000 = поздний закат / ранние сумерки). */
    private static final long START_DAY_TIME = 14000L;

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
     * Запускает событие.
     * Переводит время, задаёт длительность, играет музыку,
     * и шлёт пакет клиенту (красная луна, шейдер, кинематографичный текст).
     *
     * Ванильные title/subtitle убраны — их роль выполняет CinematicTextHandler на клиенте.
     */
    public static void start(MinecraftServer server) {
        if (active) return;
        active = true;
        remainingTicks = DURATION_TICKS;

        ServerLevel overworld = server.overworld();
        // 🕐 Ставим время начала события
        overworld.setDayTime(START_DAY_TIME);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // 🎵 Музыка — играем локально для каждого игрока
            Level level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.BLOOD_MOON_MUSIC.get(),
                    SoundSource.MUSIC,
                    1.0F, 1.0F);
        }

        // 📡 Отправляем пакет всем клиентам: "событие активно"
        //    → красная луна + шейдер + фильтр + CinematicTextHandler включаются
        BloodMoonNetwork.broadcast(true);
    }

    /**
     * Останавливает событие.
     */
    public static void stop(MinecraftServer server) {
        if (!active) return;
        active = false;
        remainingTicks = 0;

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