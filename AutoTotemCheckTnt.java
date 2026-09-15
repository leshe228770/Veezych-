package ru.meow.module.impl.combat;

import net.minecraft.entity.TntEntity;
import ru.meow.domain.IMinecraft;

final class AutoTotemCheckTnt implements AutoTotemThreatChecker, IMinecraft {

    @Override
    public String getKey() {
        return "autoTotemCheckTnt";
    }

    @Override
    public boolean check(AutoTotem module) {
        if (!module.getChecks().isSelected(getKey())) {
            return false;
        }
        float range = module.getTntRange().getValue();
        double rangeSq = range * range;
        for (TntEntity tnt : mc.world.getEntitiesByClass(
                TntEntity.class, mc.player.getBoundingBox().expand(range), e -> true)) {
            if (mc.player.squaredDistanceTo(tnt) <= rangeSq) {
                return true;
            }
        }
        return false;
    }
}
