package ru.meow.gui.hud.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.Text;
import ru.meow.service.Managers;
import ru.meow.service.NotificationService;
import ru.meow.service.notification.NotificationPosition;

/**
 * Stand-in for dump {@code sg.ec.ул} (missing from extracted class dumps).
 * Handles totem-pop entity status for {@link NotificationsElement}.
 */
final class TotemStatusNotifier {

    private static final long DURATION_MS = 2500L;

    private TotemStatusNotifier() {
    }

    static void handle(EntityStatusS2CPacket status) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            return;
        }
        if (status.getStatus() != 35) {
            return;
        }
        if (status.getEntity(mc.world) != mc.player) {
            return;
        }
        NotificationService service = Managers.notifications();
        if (service == null) {
            return;
        }
        service.add(
                new ItemStack(Items.TOTEM_OF_UNDYING),
                Text.translatable("death.attack.totem"),
                NotificationPosition.RightTop,
                DURATION_MS);
    }
}
