package ru.meow.module.impl.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.meow.domain.event.impl.EventRender2D;

final class AutoTotemHud {

    private static final float ICON_X_OFFSET = 9.0F;
    private static final float ICON_Y_OFFSET = 52.5F;
    private static final float TEXT_X_OFFSET = 16.0F;
    private static final float TEXT_Y_OFFSET = 9.0F;
    private static final float TEXT_SCALE = 200.0F;
    private static final int TEXT_COLOR = 0xFFFFFF;

    void render(EventRender2D.Post event, int count) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        float centerX = client.getWindow().getScaledWidth() / 2.0F;
        float centerY = client.getWindow().getScaledHeight();
        float iconX = centerX - ICON_X_OFFSET;
        float iconY = centerY - ICON_Y_OFFSET;
        ItemStack icon = new ItemStack(Items.TOTEM_OF_UNDYING);
        event.getContext().drawItem(icon, (int) iconX, (int) iconY);
        String text = String.valueOf(count);
        int textX = (int) (iconX + TEXT_X_OFFSET - client.textRenderer.getWidth(text));
        int textY = (int) (iconY + TEXT_Y_OFFSET);
        event.getContext().getMatrices().push();
        event.getContext().getMatrices().scale(TEXT_SCALE / 100.0F, TEXT_SCALE / 100.0F, 1.0F);
        event.getContext().drawText(
                client.textRenderer,
                text,
                (int) (textX / (TEXT_SCALE / 100.0F)),
                (int) (textY / (TEXT_SCALE / 100.0F)),
                TEXT_COLOR,
                true);
        event.getContext().getMatrices().pop();
    }
}
