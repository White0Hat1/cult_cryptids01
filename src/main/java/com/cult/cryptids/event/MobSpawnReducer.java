package com.cult.cryptids.event;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.entity.SirenHeadEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * 🌙 Уменьшает спавн обычных враждебных монстров.
 *
 * Работает через EntityJoinLevelEvent — срабатывает в момент появления сущности в мире.
 * Не трогает:
 *   - сущности, загруженные из сохранения (loadedFromDisk);
 *   - клиентские сущности;
 *   - животных и нейтральных (CREATURE, WATER_CREATURE и т.д.);
 *   - Siren Head (свой мод).
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobSpawnReducer {

    /**
     * 🎲 Шанс ОТМЕНЫ спавна враждебного моба.
     *   0.0 = не трогать (всё как было)
     *   0.5 = отменить 50% спавнов
     *   0.7 = отменить 70% спавнов
     *   0.9 = почти никого не спавнится
     */
    private static final double CANCEL_CHANCE = 0.7D;

    private static final Random RNG = new Random();

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        // Только сервер.
        if (event.getLevel().isClientSide()) return;

        // Не трогаем сущности, загруженные из сохранения.
        if (event.loadedFromDisk()) return;

        // Только мобы.
        if (!(event.getEntity() instanceof Mob mob)) return;

        // Только враждебные (зомби, скелеты, пауки, криперы).
        if (mob.getType().getCategory() != MobCategory.MONSTER) return;

        // 🛡️ Не трогаем наших мобов.
        if (mob instanceof SirenHeadEntity) return;

        // 🎲 Отменяем с шансом CANCEL_CHANCE.
        if (RNG.nextDouble() < CANCEL_CHANCE) {
            event.setCanceled(true);
        }
    }
}