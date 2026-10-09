package com.cult.cryptids.event;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.ModEntities;
import com.cult.cryptids.ModSounds;
import com.cult.cryptids.entity.SirenHeadEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 🌙 Ночной автоспавн Siren Head.
 *
 * Логика:
 *   - В 12500 (сумерки) спавним ОДНОГО Siren Head около одного случайного игрока.
 *   - Дистанция спавна — 100–150 блоков от игрока.
 *   - FOLLOW_RANGE ночного Siren — 200 блоков.
 *   - Деспавн — в конце ночи (dayTime < 12000), обрабатывается в SirenHeadEntity.tick().
 *   - Телепорт к игроку при отрыве > 150 блоков — тоже в SirenHeadEntity.
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SirenNightSpawnHandler {

    private static final long SPAWN_TIME = 12500L;

    /** 🎯 Минимальная дистанция спавна от игрока. */
    private static final double MIN_SPAWN_DISTANCE = 100.0D;

    /** 🎯 Разброс: итог в [MIN, MIN + RANGE] = 100..150 блоков. */
    private static final double SPAWN_DISTANCE_RANGE = 50.0D;

    /** 🔁 Попыток найти позицию. */
    private static final int SPAWN_ATTEMPTS = 30;

    private static boolean firedThisCycle = false;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;

        ServerLevel overworld = server.overworld();
        long dayTime = overworld.getDayTime() % 24000L;

        if (dayTime < SPAWN_TIME) {
            firedThisCycle = false;
        }

        if (!firedThisCycle && dayTime >= SPAWN_TIME && dayTime < SPAWN_TIME + 20) {
            firedThisCycle = true;
            spawnOne(server, overworld);
        }
    }

    private static void spawnOne(MinecraftServer server, ServerLevel level) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        // 🛡️ Уже есть живой ночной Siren — не спавним второго.
        boolean alreadySpawned = !level.getEntitiesOfClass(
                SirenHeadEntity.class,
                AABB.ofSize(Vec3.ZERO, 1_000_000, 1_000_000, 1_000_000),
                s -> s.getPersistentData().getBoolean(SirenHeadEntity.NBT_NIGHT_SPAWNED)
        ).isEmpty();
        if (alreadySpawned) return;

        RandomSource rng = level.random;
        ServerPlayer chosen = players.get(rng.nextInt(players.size()));

        BlockPos spawnPos = findSpawnPos(level, chosen, rng);
        if (spawnPos == null) return;

        SirenHeadEntity siren = ModEntities.SIREN_HEAD.get().create(level);
        if (siren == null) return;

        siren.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
        siren.setPersistenceRequired();

        // 🏷️ Метка ночного Siren.
        siren.getPersistentData().putBoolean(SirenHeadEntity.NBT_NIGHT_SPAWNED, true);

        // 👁️ Расширенное зрение — видеть игрока на 200 блоков.
        if (siren.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            siren.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(200.0D);
        }

        level.addFreshEntity(siren);

        for (ServerPlayer player : players) {
            player.level().playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    ModSounds.SIREN_FAR.get(), SoundSource.HOSTILE, 8.0F, 1.0F);
        }
    }

    /** 📏 Ищет позицию в 100–150 блоках от игрока. */
    private static BlockPos findSpawnPos(ServerLevel level, ServerPlayer player, RandomSource rng) {
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double dist = MIN_SPAWN_DISTANCE + rng.nextDouble() * SPAWN_DISTANCE_RANGE;

            int x = (int) (player.getX() + Math.cos(angle) * dist);
            int z = (int) (player.getZ() + Math.sin(angle) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);

            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }
        return null;
    }
}