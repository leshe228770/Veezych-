package ru.meow.module.impl.combat;

import ru.meow.domain.IMinecraft;

final class AutoTotemCheckVoid implements AutoTotemThreatChecker, IMinecraft {

    @Override
    public String getKey() {
        return "autoTotemCheckVoid";
    }

    @Override
    public boolean check(AutoTotem module) {
        if (!module.getChecks().isSelected(getKey())) {
            return false;
        }
        if (mc.player.isOnGround() || mc.player.isGliding()) {
            return false;
        }
        return mc.player.getY() < mc.world.getBottomY() + 2.0D;
    }
}
