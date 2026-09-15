package ru.meow.util;

import net.minecraft.util.math.MathHelper;
import ru.meow.domain.IMinecraft;

public final class FlyUtil implements IMinecraft {

    private FlyUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static double[] forward(double speed) {
        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();
        double sin = Math.sin(Math.toRadians(yaw + 90.0F));
        double cos = Math.cos(Math.toRadians(yaw + 90.0F));
        return new double[] {
                forward * speed * cos + sideways * speed * sin,
                forward * speed * sin - sideways * speed * cos
        };
    }

    public static boolean isMoving() {
        return mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F;
    }

    public static void fly(float speed, float verticalSpeed) {
        double forward = mc.player.input.movementForward;
        double sideways = mc.player.input.movementSideways;
        boolean jump = mc.options.jumpKey.isPressed();
        boolean sneak = mc.options.sneakKey.isPressed();
        float yaw = mc.player.getYaw();
        float vertical = 0.0F;
        if (jump) {
            vertical = verticalSpeed;
        }
        if (sneak) {
            vertical = -verticalSpeed;
        }
        if (!jump && !sneak) {
            vertical = 0.0F;
        }
        if (forward == 0.0D && sideways == 0.0D) {
            mc.player.setVelocity(0.0D, mc.player.getVelocity().y + vertical, 0.0D);
            return;
        }
        if (forward != 0.0D) {
            if (sideways > 0.0D) {
                yaw += forward > 0.0D ? -45.0F : 45.0F;
            } else if (sideways < 0.0D) {
                yaw += forward > 0.0D ? 45.0F : -45.0F;
            }
            sideways = 0.0D;
            if (forward > 0.0D) {
                forward = 1.0D;
            } else if (forward < 0.0D) {
                forward = -1.0D;
            }
        }
        mc.player.setVelocity(
                forward * speed * MathHelper.cos((float) Math.toRadians(yaw + 90.0F))
                        + sideways * speed * MathHelper.sin((float) Math.toRadians(yaw + 90.0F)),
                mc.player.getVelocity().y + vertical,
                forward * speed * MathHelper.sin((float) Math.toRadians(yaw + 90.0F))
                        - sideways * speed * MathHelper.cos((float) Math.toRadians(yaw + 90.0F)));
    }

    public static void flyPosition(double speed) {
        double forward = mc.player.input.movementForward;
        double sideways = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();
        double vertical = 0.0D;
        if (mc.options.jumpKey.isPressed()) {
            vertical = speed;
        } else if (mc.options.sneakKey.isPressed()) {
            vertical = -speed;
        }
        if (forward == 0.0D && sideways == 0.0D && vertical == 0.0D) {
            mc.player.setVelocity(0.0D, mc.player.getVelocity().y, 0.0D);
            return;
        }
        if (forward != 0.0D) {
            if (sideways > 0.0D) {
                yaw += forward > 0.0D ? -45.0F : 45.0F;
            } else if (sideways < 0.0D) {
                yaw += forward > 0.0D ? 45.0F : -45.0F;
            }
            sideways = 0.0D;
            if (forward > 0.0D) {
                forward = 1.0D;
            } else if (forward < 0.0D) {
                forward = -1.0D;
            }
        }
        double dx = forward * speed * MathHelper.cos((float) Math.toRadians(yaw + 90.0F))
                + sideways * speed * MathHelper.sin((float) Math.toRadians(yaw + 90.0F));
        double dz = forward * speed * MathHelper.sin((float) Math.toRadians(yaw + 90.0F))
                - sideways * speed * MathHelper.cos((float) Math.toRadians(yaw + 90.0F));
        mc.player.setPos(mc.player.getX() + dx, mc.player.getY() + vertical, mc.player.getZ() + dz);
    }

    public static double directionRadians(float yaw, float forward, float sideways) {
        if (forward < 0.0F) {
            yaw += 180.0F;
        }
        float multiplier = 1.0F;
        if (forward < 0.0F) {
            multiplier = -0.5F;
        }
        if (forward > 0.0F) {
            multiplier = 0.5F;
        }
        if (sideways > 0.0F) {
            yaw -= 90.0F * multiplier;
        }
        if (sideways < 0.0F) {
            yaw += 90.0F * multiplier;
        }
        return Math.toRadians(yaw);
    }
}
