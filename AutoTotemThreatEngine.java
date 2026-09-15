package ru.meow.module.impl.combat;

import java.util.ArrayList;
import java.util.List;
import ru.meow.domain.IMinecraft;

final class AutoTotemThreatEngine implements IMinecraft {

    private final List<AutoTotemThreatChecker> checkers = new ArrayList<>();

    AutoTotemThreatEngine() {
        this.checkers.add(new AutoTotemCheckHealth());
        this.checkers.add(new AutoTotemCheckCrystal());
        this.checkers.add(new AutoTotemCheckFall());
        this.checkers.add(new AutoTotemCheckElytra());
        this.checkers.add(new AutoTotemCheckVoid());
        this.checkers.add(new AutoTotemCheckTnt());
        this.checkers.add(new AutoTotemCheckTrident());
        this.checkers.add(new AutoTotemCheckTntMinecart());
        this.checkers.add(new AutoTotemCheckAnchor());
    }

    boolean needsTotem(AutoTotem module) {
        if (mc.player == null || mc.world == null) {
            return false;
        }
        for (AutoTotemThreatChecker checker : this.checkers) {
            if (checker.check(module)) {
                return true;
            }
        }
        return false;
    }

    List<AutoTotemThreatChecker> getCheckers() {
        return this.checkers;
    }
}
