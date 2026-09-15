package ru.meow.module.impl.combat;

final class AutoTotemCheckHealth implements AutoTotemThreatChecker {

    @Override
    public String getKey() {
        return "autoTotemCheckHealth";
    }

    @Override
    public boolean check(AutoTotem module) {
        if (!module.getChecks().isSelected(getKey())) {
            return false;
        }
        if (module.isElytraCheck()) {
            return false;
        }
        return module.getHealthThresholdSetting() >= module.getHealth(false);
    }
}
