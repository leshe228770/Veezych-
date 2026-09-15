package ru.meow.module.impl.combat;

import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import ru.meow.domain.IMinecraft;

final class AutoTotemCheckCrystal implements AutoTotemThreatChecker, IMinecraft {

    @Override
    public String getKey() {
        return "autoTotemCheckCrystal";
    }

    @Override
    public boolean check(AutoTotem module) {
        if (!module.getChecks().isSelected(getKey())) {
            return false;
        }
        float range = module.getCrystalRange().getValue();
        double rangeSq = range * range;
        for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(
                EndCrystalEntity.class,
                mc.player.getBoundingBox().expand(range),
                entity -> mc.player.squaredDistanceTo(entity) <= rangeSq)) {
            if (module.getBallHpCheck().getValue()
                    && module.getOffhandPreview().getItem() == Items.PLAYER_HEAD) {
                if (module.getHealth(false) > module.getBallHpThreshold().getValue()) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }
}
