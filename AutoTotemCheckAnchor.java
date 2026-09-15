package ru.meow.module.impl.combat;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import ru.meow.domain.IMinecraft;

final class AutoTotemCheckAnchor implements AutoTotemThreatChecker, IMinecraft {

    private static final double HALF = 0.5D;

    @Override
    public String getKey() {
        return "autoTotemCheckAnchor";
    }

    @Override
    public boolean check(AutoTotem module) {
        if (!module.getChecks().isSelected(getKey())) {
            return false;
        }
        float range = module.getAnchorRange().getValue();
        BlockPos origin = mc.player.getBlockPos();
        int radius = (int) Math.ceil(range);
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = origin.add(x, y, z);
                    if (!mc.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)) {
                        continue;
                    }
                    int charges = mc.world.getBlockState(pos).get(net.minecraft.block.RespawnAnchorBlock.CHARGES);
                    if (charges <= 0) {
                        continue;
                    }
                    double dx = pos.getX() + HALF;
                    double dy = pos.getY() + HALF;
                    double dz = pos.getZ() + HALF;
                    if (mc.player.squaredDistanceTo(dx, dy, dz) <= range * range) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
