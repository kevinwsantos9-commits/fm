package com.nivek.flash.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class FlashHud {
    private FlashHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        boolean on = ClientState.enabled;
        int level = ClientState.level;

        int pw = 168, ph = 36;
        int x = ctx.getScaledWindowWidth() - pw - 6;
        int y = ctx.getScaledWindowHeight() - ph - 6;
        int red = 0xFFCC0000;

        ctx.fill(x, y, x + pw, y + ph, 0xC0000000);
        ctx.fill(x, y, x + pw, y + 1, red);
        ctx.fill(x, y + ph - 1, x + pw, y + ph, red);
        ctx.fill(x, y, x + 1, y + ph, red);
        ctx.fill(x + pw - 1, y, x + pw, y + ph, red);

        ctx.drawText(tr, on ? "FLASH  LV " + level : "FLASH  OFF", x + 4, y + 4, on ? 0xFFFFFFFF : 0xFF777777, true);
        ctx.fill(x + pw - 14, y + 4, x + pw - 6, y + 12, 0xFF000000 | ClientState.color);

        for (int i = 0; i < 20; i++) {
            int sx = x + 4 + i * 8;
            int col = on && i < level ? 0xFFE01010 : 0xFF2A2A2A;
            ctx.fill(sx, y + 16, sx + 6, y + 22, col);
        }

        ctx.drawText(tr, "VIB", x + 4, y + 26, ClientState.vibrating ? 0xFFFF3030 : 0xFF555555, false);
        ctx.drawText(tr, "H2O", x + 28, y + 26, on && level >= 5 ? 0xFFFFFFFF : 0xFF555555, false);
    }
}
