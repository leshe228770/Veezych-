package ru.meow.module.impl.movement.speed;

import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import ru.meow.domain.event.impl.EventInput;
import ru.meow.domain.event.impl.EventPacket;
import ru.meow.module.impl.movement.Speed;
import ru.meow.util.MoveUtil;

/**
 * Grim {@code Timer Flag} ({@code sg.ec.ым}). No dump; jar references only from {@code c0165}.
 *
 * <p>Call sites: {@code 4(Speed,DD)} strafe, {@code ь(EventPacket)} receive, {@code Ог()} on enable,
 * {@code Оа()} on jump, {@code 弟(Speed)} input.
 */
public final class SpeedTimerFlag {

    /** Inferred horizontal strafe factor. */
    private static final double HORIZONTAL_FACTOR = 0.22D;

    /**
     * Inferred alternating timer multipliers — Timer Flag exposes only {@link Speed#motionY}, not timer
     * sliders (those belong to RW / Test modes in {@code c0165} clinit).
     */
    private static final float TIMER_HIGH = 1.6F;

    private static final float TIMER_LOW = 0.4F;

    private static int phase;

    private SpeedTimerFlag() {
    }

    public static void apply(Speed speed, double x, double z) {
        MoveUtil.addVelocity(x * HORIZONTAL_FACTOR, z * HORIZONTAL_FACTOR);
        if (MoveUtil.mc.player != null) {
            MoveUtil.addVelocityY(speed.motionY.getValue());
        }
    }

    public static void onPacket(EventPacket event) {
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            phase = 0;
            Speed.ticks = 0;
            MoveUtil.resetTimer();
        }
    }

    public static void onEnable() {
        phase = 0;
        Speed.ticks = 0;
        MoveUtil.resetTimer();
    }

    public static void input(Speed speed, EventInput event) {
        if (MoveUtil.isJumping()) {
            speed.requestCancelJump();
        }
    }

    public static void tick() {
        phase++;
        MoveUtil.setTimer(phase % 2 == 0 ? TIMER_HIGH : TIMER_LOW);
    }
}
