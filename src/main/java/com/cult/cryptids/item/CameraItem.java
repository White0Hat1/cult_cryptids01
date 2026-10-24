package com.cult.cryptids.item;

import com.cult.cryptids.reg.ModSounds;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Stream;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CameraItem extends Item implements GeoItem, FirstPersonAnimatedItem {
    /** Расстояние, на котором камера может найти сущность. */
    private static final double SHOOT_DISTANCE = 64;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private long lastShootGameTime = -100;
    private long lastInspectGameTime = -100;
    private long lastDrawGameTime = -100;

    /** Ссылка на контроллер — для forceAnimationReset(). */
    private AnimationController<CameraItem> controller;

    public CameraItem(Properties props) {
        super(props);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public void markShoot(long gameTime)   { this.lastShootGameTime = gameTime; }
    public void markInspect(long gameTime) { this.lastInspectGameTime = gameTime; }
    @Override
    public void markDraw(long gameTime)    { this.lastDrawGameTime = gameTime; }

    public boolean isShooting(long now)   { return (now - lastShootGameTime) < 8; }
    public boolean isInspecting(long now) { return (now - lastInspectGameTime) < 105; }
    public boolean isDrawing(long now)    { return (now - lastDrawGameTime) < 33; }

    @Override
    public void resetAnimation() {
        if (controller != null) controller.forceAnimationReset();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.CAMERA_SHUTTER.get(), SoundSource.PLAYERS, 0.5F, 1.5F);
            if (level.isClientSide) markInspect(level.getGameTime());
            return InteractionResultHolder.success(stack);
        }

        if (level.isClientSide) {
            Vec3 src = player.getEyePosition(), angle = player.getLookAngle();
            List<Component> names = new ArrayList<>();
            Map<EntityType<?>, Integer> entities = new HashMap<>();

            for (Entity entity: level.getEntities(player, AABB.ofSize(src, SHOOT_DISTANCE, SHOOT_DISTANCE, SHOOT_DISTANCE))) {
                Vec3 vec3 = entity.getEyePosition().subtract(src);
                double dist = vec3.length();
                if (dist > SHOOT_DISTANCE) continue; // Проверка на расстояние до сущности
                // Косинус угла между направлением взгляда и вектором до сущности должен быть больше 0.5 (угол < 60 градусов)
                if (vec3.dot(angle) / dist < 0.5) continue;

                if (entity instanceof Player player1) names.add(player1.getName());
                else if (entity.hasCustomName()) names.add(entity.getCustomName());
                else entities.merge(entity.getType(), 1, Integer::sum);
            }

            if (names.isEmpty() && entities.isEmpty()) player.displayClientMessage(Component.translatable(getDescriptionId() + ".on_photo.no_entities"), true);
            else {
                String count = getDescriptionId() + ".on_photo.count", delimiter = getDescriptionId() + ".on_photo.delimiter";
                player.displayClientMessage(Component.translatable(getDescriptionId() + ".on_photo",
                        Stream.concat(names.stream(), entities.entrySet().stream().map(entry -> {
                            Component component = entry.getKey().getDescription();
                            return entry.getValue() > 1 ? Component.translatable(count, component, entry.getValue()) : component;
                        })).reduce((c1, c2) -> Component.translatable(delimiter, c1, c2))
                                .orElseThrow()), true);
            }
            markShoot(level.getGameTime());
        }

        else level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.CAMERA_SHUTTER.get(), SoundSource.PLAYERS, 1, 1);
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        this.controller = new AnimationController<>(this, "controller", 1, state -> {
            AnimationController<CameraItem> ctrl = state.getController();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                ctrl.setAnimationSpeed(0.33);
                return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }

            long now = mc.level.getGameTime();

            if (isDrawing(now)) {
                ctrl.setAnimationSpeed(1);
                return state.setAndContinue(RawAnimation.begin().thenPlay("draw"));
            }

            if (isShooting(now)) {
                ctrl.setAnimationSpeed(1);
                return state.setAndContinue(RawAnimation.begin().thenPlay("shoot"));
            }

            if (isInspecting(now)) {
                ctrl.setAnimationSpeed(1);
                return state.setAndContinue(RawAnimation.begin().thenPlay("inspect"));
            }

            boolean pressingMove = mc.player.xxa != 0.0F
                    || mc.player.zza != 0.0F
                    || mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.0005;

            if (pressingMove) {
                ctrl.setAnimationSpeed(1);
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }

            ctrl.setAnimationSpeed(0.33);
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        });

        controllers.add(this.controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}