package ru.meow.module.impl.misc;

import ru.meow.domain.module.Module;
import ru.meow.domain.module.ModuleCategory;
import ru.meow.domain.module.ModuleInfo;
import ru.meow.util.BaritoneUtil;

/**
 * Master toggle for Baritone integration. Enabled by default in the original jar.
 */
public final class Baritone extends Module {

    public Baritone() {
        super(new ModuleInfo(
                "Baritone",
                "Полностью включает или отключает Baritone",
                ModuleCategory.Misc,
                -1));
        setEnabled(true);
    }

    @Override
    public void onDisable() {
        BaritoneUtil.shutdown();
        super.onDisable();
    }
}
