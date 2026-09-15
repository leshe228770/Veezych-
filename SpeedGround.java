package ru.meow.module.impl.movement.speed;

import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import ru.meow.domain.event.impl.EventPacket;
import ru.meow.module.impl.movement.Speed;
import ru.meow.util.MoveUtil;

/**
 * Grim {@code Ground} ({@code sg.ec._7}). No dump; jar references only from {@code c0165}.
 *
 * <p>Call sites: {@code А(Speed,DD)} strafe, {@code м(EventPacket)} receive,
 * {@code н(Speed)} on jump ({@code EventJump}).
 */
public final class SpeedGround {

    /** Inferred horizontal strafe factor. */
    private static final double HORIZONTAL_FACTOR = 0.2D;

    /** Inferred ground-tick threshold before {@link Speed#flag} is armed. */
    private static final int GROUND_TICKS_BEFORE_FLAG = 3;

    private SpeedGround() {
    }

    public static void apply(Speed speed, double x, double z) {
        if (speed.noAir.getValue() && !MoveUtil.mc.player.isOnGround()) {
            return;
        }
        MoveUtil.addVelocity(x * HORIZONTAL_FACTOR, z * HORIZONTAL_FACTOR);
    }

    public static void onPacket(EventPacket event) {
        if (!(event.getPacket() instanceof PlayerPositionLookS2CPacket)) {
            return;
        }
        Speed.ticks = 0;
        Speed.flag = false;
        MoveUtil.resetTimer();
    }

    public static void tick(Speed speed) {
        if (MoveUtil.mc.player == null) {
            return;
        }
        if (MoveUtil.mc.player.isOnGround()) {
            Speed.ticks++;
            if (Speed.ticks >= GROUND_TICKS_BEFORE_FLAG) {
                Speed.flag = true;
            }
        } else {
            Speed.ticks = 0;
        }
    }
}
