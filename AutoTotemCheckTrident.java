package ru.meow.module.impl.combat;

import net.minecraft.entity.projectile.TridentEntity;
import ru.meow.domain.IMinecraft;

final class AutoTotemCheckTrident implements AutoTotemThreatChecker, IMinecraft {

    @Override
    public String getKey() {
        return "autoTotemCheckTrident";
    }

    @Override
    public boolean check(AutoTotem module) {
        if (!module.getChecks().isSelected(getKey())) {
            return false;
        }
        float range = module.getTridentRange().getValue();
        double rangeSq = range * range;
        for (TridentEntity trident : mc.world.getEntitiesByClass(
                TridentEntity.class, mc.player.getBoundingBox().expand(range), e -> true)) {
            if (mc.player.squaredDistanceTo(trident) <= rangeSq) {
                return true;
            }
        }
        return false;
    }
}
