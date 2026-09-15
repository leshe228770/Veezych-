package ru.meow.module.impl.movement.speed;

import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import ru.meow.domain.event.impl.EventInput;
import ru.meow.domain.event.impl.EventPacket;
import ru.meow.module.impl.movement.Speed;
import ru.meow.util.MoveUtil;

/**
 * Grim Timer Boost Test ({@code sg.ec.Рд}). No dump; jar references only from {@code c0165}.
 *
 * <p>Call sites: {@code 必(Speed,DD)} strafe, {@code с(EventPacket)} receive, {@code ья()} on enable,
 * {@code Ж(Speed)} on jump, {@code с(Speed)} input (overload).
 */
public final class SpeedTimerBoostTest {

    /** Inferred horizontal strafe factor. */
    private static final double HORIZONTAL_FACTOR = 0.24D;

    private static int phase;

    private SpeedTimerBoostTest() {
    }

    public static void apply(Speed speed, double x, double z) {
        MoveUtil.addVelocity(x * HORIZONTAL_FACTOR, z * HORIZONTAL_FACTOR);
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

    public static void tick(Speed speed) {
        if (!speed.testTimerBoost.getValue()) {
            MoveUtil.resetTimer();
            return;
        }
        phase++;
        MoveUtil.setTimer(
                phase % 2 == 0 ? speed.testTimerPower.getValue() : speed.testTimerReset.getValue());
    }
}
