package ru.meow.module.impl.combat;

import ru.meow.domain.IMinecraft;

final class AutoTotemCheckFall implements AutoTotemThreatChecker, IMinecraft {

    private static final float MIN_FALL = 3.0F;

    @Override
    public String getKey() {
        return "autoTotemCheckFall";
    }

    @Override
    public boolean check(AutoTotem module) {
        if (!module.getChecks().isSelected(getKey())) {
            return false;
        }
        if (mc.player.isOnGround() || mc.player.isGliding() || mc.player.hasVehicle()) {
            return false;
        }
        if (mc.player.fallDistance <= MIN_FALL) {
            return false;
        }
        float predicted = mc.player.fallDistance - MIN_FALL;
        if (predicted >= module.getEffectiveHealthThreshold()) {
            return true;
        }
        return mc.player.fallDistance > module.getFallDistance().getValue();
    }
}
