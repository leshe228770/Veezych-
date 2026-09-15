package ru.meow.domain.event.impl;

import net.minecraft.util.math.Vec3d;
import ru.meow.domain.event.IEvent;

/**
 * The inputs of the vanilla elytra glide step, before they are combined into the
 * new velocity. Rewriting the rotation vector steers the glide; rewriting the
 * multiplier scales the lift and drag terms.
 */
public class EventElytraFly implements IEvent {

    private Vec3d velocity;

    private Vec3d rotationVector;

    private Vec3d multiplier;

    public EventElytraFly(Vec3d velocity, Vec3d rotationVector, Vec3d multiplier) {
        this.velocity = velocity;
        this.rotationVector = rotationVector;
        this.multiplier = multiplier;
    }

    public Vec3d getVelocity() {
        return this.velocity;
    }

    public void setVelocity(Vec3d velocity) {
        this.velocity = velocity;
    }

    public Vec3d getRotationVector() {
        return this.rotationVector;
    }

    public void setRotationVector(Vec3d rotationVector) {
        this.rotationVector = rotationVector;
    }

    public Vec3d getMultiplier() {
        return this.multiplier;
    }

    public void setMultiplier(Vec3d multiplier) {
        this.multiplier = multiplier;
    }
}
