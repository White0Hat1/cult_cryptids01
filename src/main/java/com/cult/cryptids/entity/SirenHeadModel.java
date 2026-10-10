package com.cult.cryptids.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import java.util.HashMap;
import java.util.Map;

public class SirenHeadModel extends DefaultedEntityGeoModel<SirenHeadEntity> {
    private final Map<String, Float> baseRotY = new HashMap<>();

    // 🎡 Кручение сиренов: раз в 20 секунд (400 тиков), длится 2 секунды (40 тиков)
    private static final int SPIN_PERIOD_TICKS = 400;   // период = 20 сек
    private static final int SPIN_DURATION_TICKS = 40;  // длительность = 2 сек

    public SirenHeadModel() {
        super(ResourceLocation.fromNamespaceAndPath("cult_cryptids", "siren_head"), false);
    }

    @Override
    public void setCustomAnimations(SirenHeadEntity animatable, long instanceId,
                                    AnimationState<SirenHeadEntity> animationState) {
        // Восстанавливаем базу — чтобы наш поворот не накапливался
        restoreBase("head");
        restoreBase("torso");
        restoreBase("body");
        restoreBase("rightSiren");
        restoreBase("leftSiren");

        // Играем анимацию из JSON
        super.setCustomAnimations(animatable, instanceId, animationState);

        // Запоминаем базу
        saveBase("head");
        saveBase("torso");
        saveBase("body");
        saveBase("rightSiren");
        saveBase("leftSiren");

        // Во время казни не добавляем своих модификаций
        if (animatable.isGrabbing() || animatable.isSlamming() || animatable.isReaching()) {
            return;
        }

        // 🎡 Кручение сиренов — короткий импульс раз в 20 сек
        long cycleTick = animatable.tickCount % SPIN_PERIOD_TICKS;
        if (cycleTick < SPIN_DURATION_TICKS) {
            float progress = (float) cycleTick / (float) SPIN_DURATION_TICKS;  // 0.0 → 1.0
            float spinDeg = progress * 360.0F;                                  // 0° → 360°
            float spinRad = (float) Math.toRadians(spinDeg);

            CoreGeoBone rightSiren = this.getAnimationProcessor().getBone("rightSiren");
            if (rightSiren != null) {
                rightSiren.setRotY(rightSiren.getRotY() + spinRad);
            }

            CoreGeoBone leftSiren = this.getAnimationProcessor().getBone("leftSiren");
            if (leftSiren != null) {
                leftSiren.setRotY(leftSiren.getRotY() + spinRad);
            }
        }

        // === Поворот головы/торса/тела за игроком ===
        float deltaYaw = Mth.wrapDegrees(animatable.getYHeadRot() - animatable.yBodyRot);

        CoreGeoBone head = this.getAnimationProcessor().getBone("head");
        if (head != null) {
            float yaw = Mth.clamp(deltaYaw, -80F, 80F);
            head.setRotY(head.getRotY() + (float) Math.toRadians(yaw));
        }

        float partial = Mth.clamp(deltaYaw * 0.4F, -30F, 30F);
        float partialRad = (float) Math.toRadians(partial);

        CoreGeoBone torso = this.getAnimationProcessor().getBone("torso");
        if (torso != null) {
            torso.setRotY(torso.getRotY() + partialRad);
        }

        CoreGeoBone body = this.getAnimationProcessor().getBone("body");
        if (body != null) {
            body.setRotY(body.getRotY() + partialRad);
        }
    }

    private void saveBase(String name) {
        CoreGeoBone bone = this.getAnimationProcessor().getBone(name);
        if (bone != null) {
            baseRotY.put(name, bone.getRotY());
        }
    }

    private void restoreBase(String name) {
        CoreGeoBone bone = this.getAnimationProcessor().getBone(name);
        if (bone != null && baseRotY.containsKey(name)) {
            bone.setRotY(baseRotY.get(name));
        }
    }
}