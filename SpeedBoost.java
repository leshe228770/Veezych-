package ru.meow.module.impl.movement.speed;

import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import ru.meow.domain.event.impl.EventInput;
import ru.meow.domain.event.impl.EventPacket;
import ru.meow.module.impl.movement.Speed;
import ru.meow.util.MoveUtil;

/**
 * Grim {@code Boost} ({@code sg.ec.фе}). No {@code cNNNN} dump — class absent from jar;
 * only {@code c0165} references it.
 *
 * <p>Call sites: {@code щ(DD)} strafe, {@code С(EventPacket)} receive, {@code нш()} on enable,
 * {@code ц(Speed)} input (sets {@link Speed#requestCancelJump()}).
 */
public final class SpeedBoost {

    /** Inferred horizontal strafe factor (not present in any surviving dump). */
    private static final double HORIZONTAL_FACTOR = 0.28D;

    private SpeedBoost() {
    }

    public static void apply(double x, double z) {
        if (!Speed.flag) {
            return;
        }
        MoveUtil.addVelocity(x * HORIZONTAL_FACTOR, z * HORIZONTAL_FACTOR);
    }

    public static void onPacket(EventPacket event) {
        if (!(event.getPacket() instanceof PlayerPositionLookS2CPacket)) {
            return;
        }
        Speed.ticks = 0;
        Speed.flag = true;
        MoveUtil.resetTimer();
    }

    public static void onEnable() {
        Speed.flag = true;
        Speed.ticks = 0;
        MoveUtil.resetTimer();
    }

    public static void input(Speed speed, EventInput event) {
        if (MoveUtil.isJumping()) {
            speed.requestCancelJump();
        }
    }
}
