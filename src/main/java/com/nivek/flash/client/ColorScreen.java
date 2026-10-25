package com.nivek.flash.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class ColorScreen extends Screen {
    private static final int[] COLORS = {0xFF2020, 0xFFE000, 0x2080FF, 0xFFFFFF, 0xA040FF, 0x30FF50, 0xFF8020};
    private static final String[] NAMES = {"Vermelho", "Amarelo", "Azul", "Branco", "Roxo", "Verde", "Laranja"};

    public ColorScreen() {
        super(Text.literal("Nivek Flash - Cor dos raios"));
    }

    @Override
    protected void init() {
        int cx = width / 2, y0 = height / 2 - 80;
        for (int i = 0; i < COLORS.length; i++) {
            final int c = COLORS[i];
            addDrawableChild(ButtonWidget.builder(
                            Text.literal(NAMES[i]).styled(s -> s.withColor(c)),
                            b -> { ClientState.setColor(c); clearAndInit(); })
                    .dimensions(cx - 156 + (i % 4) * 78, y0 + (i / 4) * 24, 74, 20).build());
        }
        addDrawableChild(new Chan(cx - 100, y0 + 60, "R", 16));
        addDrawableChild(new Chan(cx - 100, y0 + 84, "G", 8));
        addDrawableChild(new Chan(cx - 100, y0 + 108, "B", 0));
        addDrawableChild(ButtonWidget.builder(Text.literal("Fechar"), b -> close())
                .dimensions(cx - 50, y0 + 136, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 100, 0xFFFFFFFF);
        ctx.fill(width / 2 + 106, height / 2 - 20, width / 2 + 156, height / 2 + 48, 0xFF000000 | ClientState.color);
    }

    private static class Chan extends SliderWidget {
        private final String name;
        private final int shift;

        Chan(int x, int y, String name, int shift) {
            super(x, y, 200, 20, Text.empty(), ((ClientState.color >> shift) & 255) / 255.0);
            this.name = name;
            this.shift = shift;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(name + ": " + (int) Math.round(value * 255)));
        }

        @Override
        protected void applyValue() {
            int c = ClientState.color & ~(255 << shift);
            ClientState.setColor(c | ((int) Math.round(value * 255) << shift));
        }
    }
}
