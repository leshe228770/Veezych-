package ru.meow.module.impl.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import ru.meow.domain.event.impl.EventRender2D;

/** On-screen FOV ring for arc prediction ({@code sg.ec._Т.Ю}). */
final class BowAimBotHud {

    static final float MAX_FOV_DEGREES = 179.0F;

    private static final double FULL_CIRCLE = Math.PI * 2.0D;

    private static final int CIRCLE_SEGMENTS = 96;

    private BowAimBotHud() {
    }

    static void drawFovRing(EventRender2D.Post event, BowAimBot module, float radiusPx) {
        if (radiusPx <= 0.0F || !Float.isFinite(radiusPx)) {
            return;
        }
        float clampedFov = Math.min(module.getFovDegrees(), MAX_FOV_DEGREES);
        if (clampedFov <= 0.0F) {
            return;
        }
        MatrixStack matrices = event.getContext().getMatrices();
        float centerX = event.getContext().getScaledWindowWidth() / 2.0F;
        float centerY = event.getContext().getScaledWindowHeight() / 2.0F;
        int color = module.getTarget() != null ? 0xB45A5AF0 : 0x78FFFFFF;
        float a = ((color >> 24) & 0xFF) / 255.0F;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(1.5F);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double angle = FULL_CIRCLE * i / CIRCLE_SEGMENTS;
            float x = centerX + (float) (Math.cos(angle) * radiusPx);
            float y = centerY + (float) (Math.sin(angle) * radiusPx);
            builder.vertex(matrix, x, y, 0.0F).color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
