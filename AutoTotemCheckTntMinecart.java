package ru.meow.module.impl.combat;

import net.minecraft.entity.vehicle.TntMinecartEntity;
import ru.meow.domain.IMinecraft;

final class AutoTotemCheckTntMinecart implements AutoTotemThreatChecker, IMinecraft {

    @Override
    public String getKey() {
        return "autoTotemCheckTntMinecart";
    }

    @Override
    public boolean check(AutoTotem module) {
        if (!module.getChecks().isSelected(getKey())) {
            return false;
        }
        float range = module.getTntMinecartRange().getValue();
        double rangeSq = range * range;
        for (TntMinecartEntity cart : mc.world.getEntitiesByClass(
                TntMinecartEntity.class, mc.player.getBoundingBox().expand(range), e -> true)) {
            if (mc.player.squaredDistanceTo(cart) <= rangeSq) {
                return true;
            }
        }
        return false;
    }
}
