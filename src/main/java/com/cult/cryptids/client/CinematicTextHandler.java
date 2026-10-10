package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.ModSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(
        modid = CultCryptids.MODID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class CinematicTextHandler {

    private static final ResourceLocation MOON_TEX =
            new ResourceLocation("cult_cryptids", "textures/gui/blood_moon_icon.png");

    private static String fullText = "";
    private static String fearText = "";
    private static int sequenceTick = 0;
    private static boolean cinematicActive = false;

    private static LoopingMusicSound musicSound = null;

    // ============ ТАЙМИНГИ ============
    private static final int FEAR_HOLD_END = 60;
    private static final int FEAR_FADE_END = 70;

    private static final int WARN_PRINT_START = FEAR_FADE_END;
    private static final int WARN_PRINT_TICKS = 90;
    private static final int WARN_PRINT_END = WARN_PRINT_START + WARN_PRINT_TICKS;
    private static final int WARN_HOLD_END = WARN_PRINT_END + 80;
    private static final int WARN_FADE_END = WARN_HOLD_END + 20;

    private static final int MOON_START = WARN_FADE_END + 10;
    private static final int MOON_END = MOON_START + 20;

    private static final int TITLES_START = MOON_END;
    private static final int TITLES_END = TITLES_START + 15;

    private static final int DIVIDER_START = TITLES_END;
    private static final int DIVIDER_END = DIVIDER_START + 15;

    private static final int FADE_OUT_START = DIVIDER_END + 120;
    private static final int FADE_OUT_END = FADE_OUT_START + 40;

    // ============ 🌕 УВЕЛИЧЕНИЕ ЛУНЫ ============
    /** 🎯 Начинаем увеличение ровно в момент появления надписи "BLOOD MOON". */
    private static final int MOON_SCALE_START = TITLES_START;
    /** ⏱️ Длительность увеличения — 5 секунд (100 тиков). */
    private static final int MOON_SCALE_DURATION = 100;

    // ============ 🎵 ЗВУКИ ============
    private static final int IMPACT_LENGTH_TICKS = 60;
    private static final int IMPACT_TRIGGER_TICK = MOON_START;
    private static final int MUSIC_TRIGGER_TICK = IMPACT_TRIGGER_TICK + IMPACT_LENGTH_TICKS;

    private static boolean impactPlayed = false;

    // ============ НЕБО ============
    private static final int SKY_FADE_START = TITLES_START;
    private static final int SKY_FADE_TICKS = 100;

    // ============ РАЗМЕРЫ ============
    private static final int MOON_SIZE = 64;
    private static final int MOON_Y = 40;
    private static final int TOP_TEXT_Y = 40;

    public static void say(String text) {
        fullText = text;
        fearText = Component.translatable("message.cult_cryptids.fear_warning").getString();
        sequenceTick = 0;
        cinematicActive = true;
        impactPlayed = false;
        stopMusic();
    }

    public static void say(Component text) {
        say(text.getString());
    }

    public static float getSkyProgress() {
        if (!cinematicActive) return 1.0F;
        if (sequenceTick < SKY_FADE_START) return 0.0F;
        float p = (sequenceTick - SKY_FADE_START) / (float) SKY_FADE_TICKS;
        return Mth.clamp(p, 0.0F, 1.0F);
    }

    /**
     * 🌕 Прогресс увеличения луны (0.0 = обычный размер, 1.0 = полный Blood Moon).
     * Стартует в момент появления надписи "BLOOD MOON", растёт 5 секунд.
     */
    public static float getMoonScaleProgress() {
        if (!BloodMoonClientState.isActive()) return 0.0F;

        // Кинематик уже закончился, но Blood Moon ещё идёт → полный размер.
        if (!cinematicActive || fullText.isEmpty()) return 1.0F;

        if (sequenceTick < MOON_SCALE_START) return 0.0F;
        if (sequenceTick >= MOON_SCALE_START + MOON_SCALE_DURATION) return 1.0F;

        float p = (sequenceTick - MOON_SCALE_START) / (float) MOON_SCALE_DURATION;
        // smoothstep для плавности
        return p * p * (3.0F - 2.0F * p);
    }

    public static void stopMusic() {
        if (musicSound != null) {
            Minecraft.getInstance().getSoundManager().stop(musicSound);
            musicSound = null;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (fullText.isEmpty()) return;

        sequenceTick++;

        Minecraft mc = Minecraft.getInstance();

        if (!impactPlayed && sequenceTick >= IMPACT_TRIGGER_TICK
                && mc.player != null && mc.level != null) {
            BloodMoonImpactSound impact = new BloodMoonImpactSound(ModSounds.BLOOD_MOON_IMPACT.get());
            mc.getSoundManager().play(impact);
            impactPlayed = true;
        }

        if (musicSound == null && sequenceTick >= MUSIC_TRIGGER_TICK) {
            if (mc.player != null) {
                LoopingMusicSound sound = new LoopingMusicSound(ModSounds.BLOOD_MOON_MUSIC.get());
                mc.getSoundManager().play(sound);
                musicSound = sound;
            }
        }

        if (sequenceTick >= FADE_OUT_END && cinematicActive) {
            fullText = "";
            fearText = "";
            sequenceTick = 0;
            cinematicActive = false;
            impactPlayed = false;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (fullText.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        if (mc.player == null || mc.level == null) return;

        GuiGraphics gfx = event.getGuiGraphics();
        int screenW = gfx.guiWidth();
        int s = sequenceTick;

        // FEAR
        float fearAlpha = 0.0F;
        if (s < FEAR_HOLD_END) {
            fearAlpha = 1.0F;
        } else if (s < FEAR_FADE_END) {
            fearAlpha = 1.0F - (s - FEAR_HOLD_END) / (float)(FEAR_FADE_END - FEAR_HOLD_END);
        }

        if (fearAlpha > 0.0F && !fearText.isEmpty()) {
            int fa = (int)(fearAlpha * 255);
            int fearColor = (fa << 24) | 0xFFFFFF;
            int markColor = (fa << 24) | 0xFF2222;
            int shadowColor = ((int)(fearAlpha * 200) << 24) | 0x000000;

            String mark = "!";
            int markW = mc.font.width(mark);
            int markX = (screenW - markW) / 2;
            int markY = TOP_TEXT_Y - 18;

            gfx.drawString(mc.font, mark, markX + 1, markY + 1, shadowColor, false);
            gfx.drawString(mc.font, mark, markX, markY, markColor, false);

            int fearW = mc.font.width(fearText);
            int fearX = (screenW - fearW) / 2;

            gfx.drawString(mc.font, fearText, fearX + 1, TOP_TEXT_Y + 1, shadowColor, false);
            gfx.drawString(mc.font, fearText, fearX, TOP_TEXT_Y, fearColor, false);
        }

        // WARNING
        float warnAlpha = 0.0F;
        if (s >= WARN_PRINT_START && s < WARN_HOLD_END) {
            warnAlpha = 1.0F;
        } else if (s >= WARN_HOLD_END && s < WARN_FADE_END) {
            warnAlpha = 1.0F - (s - WARN_HOLD_END) / (float)(WARN_FADE_END - WARN_HOLD_END);
        }

        if (warnAlpha > 0.0F) {
            float revealProgress = Mth.clamp((s - WARN_PRINT_START) / (float)WARN_PRINT_TICKS, 0.0F, 1.0F);
            int shown = Mth.clamp((int)(revealProgress * fullText.length()), 0, fullText.length());
            String visible = fullText.substring(0, shown);

            int wa = (int)(warnAlpha * 255);
            int textColor = (wa << 24) | 0xFF5555;
            int shadowColor = ((int)(warnAlpha * 0.8F * 255) << 24) | 0x000000;

            int textW = mc.font.width(visible);
            int textX = (screenW - textW) / 2;

            gfx.drawString(mc.font, visible, textX + 1, TOP_TEXT_Y + 1, shadowColor, false);
            gfx.drawString(mc.font, visible, textX, TOP_TEXT_Y, textColor, false);
        }

        float masterAlpha = 1.0F;
        if (s >= FADE_OUT_START) {
            masterAlpha = 1.0F - (s - FADE_OUT_START) / (float)(FADE_OUT_END - FADE_OUT_START);
        }
        masterAlpha = Mth.clamp(masterAlpha, 0.0F, 1.0F);

        // MOON (иконка на экране)
        float moonP = 0.0F;
        if (s >= MOON_START && s < MOON_END) {
            float t = (s - MOON_START) / (float)(MOON_END - MOON_START);
            moonP = t * t * (3.0F - 2.0F * t);
        } else if (s >= MOON_END) {
            moonP = 1.0F;
        }

        if (moonP > 0.0F) {
            float moonScale = 0.6F + 0.4F * moonP;
            int moonSize = (int)(MOON_SIZE * moonScale);
            int moonAlpha = (int)(255 * masterAlpha * moonP);

            int moonX = (screenW - moonSize) / 2;
            int moonY = MOON_Y;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, MOON_TEX);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, moonAlpha / 255.0F);

            PoseStack pose = gfx.pose();
            pose.pushPose();
            pose.translate(moonX, moonY, 0);
            pose.scale(moonSize, moonSize, 1.0F);

            Matrix4f mat = pose.last().pose();
            BufferBuilder buf = Tesselator.getInstance().getBuilder();
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buf.vertex(mat, 0, 0, 0).uv(0.0F, 0.0F).endVertex();
            buf.vertex(mat, 0, 1, 0).uv(0.0F, 1.0F).endVertex();
            buf.vertex(mat, 1, 1, 0).uv(1.0F, 1.0F).endVertex();
            buf.vertex(mat, 1, 0, 0).uv(1.0F, 0.0F).endVertex();
            BufferUploader.drawWithShader(buf.end());
            pose.popPose();

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }

        int moonBottom = MOON_Y + MOON_SIZE;
        int titleY = moonBottom + 10;
        int dividerY = titleY + 26;
        int subtitleY = dividerY + 10;

        // TITLES
        float titlesP = 0.0F;
        if (s >= TITLES_START && s < TITLES_END) {
            titlesP = (s - TITLES_START) / (float)(TITLES_END - TITLES_START);
        } else if (s >= TITLES_END) {
            titlesP = 1.0F;
        }

        if (titlesP > 0.0F) {
            String title = "BLOOD MOON";
            int titleAlpha = (int)(255 * masterAlpha * titlesP);
            int titleColor = (titleAlpha << 24) | 0xFFFFFF;
            int titleShadow = ((int)(titleAlpha * 0.9F) << 24) | 0x000000;

            PoseStack pose = gfx.pose();
            pose.pushPose();
            pose.translate(screenW / 2.0F, titleY, 0);
            pose.scale(2.0F, 2.0F, 1.0F);
            int titleW = mc.font.width(title);
            gfx.drawString(mc.font, title, -titleW / 2 + 1, 1, titleShadow, false);
            gfx.drawString(mc.font, title, -titleW / 2, 0, titleColor, false);
            pose.popPose();

            String sub = "Special Event";
            int subAlpha = (int)(255 * masterAlpha * titlesP);
            int subColor = (subAlpha << 24) | 0xFFFFFF;
            int subShadow = ((int)(subAlpha * 0.8F) << 24) | 0x000000;

            int subW = mc.font.width(sub);
            int subX = (screenW - subW) / 2;

            gfx.drawString(mc.font, sub, subX + 1, subtitleY + 1, subShadow, false);
            gfx.drawString(mc.font, sub, subX, subtitleY, subColor, false);
        }

        // DIVIDER
        float dividerP = 0.0F;
        if (s >= DIVIDER_START && s < DIVIDER_END) {
            dividerP = (s - DIVIDER_START) / (float)(DIVIDER_END - DIVIDER_START);
        } else if (s >= DIVIDER_END) {
            dividerP = 1.0F;
        }

        if (dividerP > 0.0F) {
            int lineMaxWidth = 200;
            int halfWidth = (int)(lineMaxWidth * 0.5F * dividerP);
            int centerX = screenW / 2;

            int lineAlpha = (int)(255 * masterAlpha * 0.9F);
            int lineColor = (lineAlpha << 24) | 0xFFFFFF;

            gfx.fill(centerX - halfWidth, dividerY, centerX + halfWidth, dividerY + 1, lineColor);
        }
    }
}