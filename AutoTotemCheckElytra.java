package ru.meow.module.impl.combat;

final class AutoTotemCheckElytra implements AutoTotemThreatChecker {

    @Override
    public String getKey() {
        return "autoTotemCheckElytra";
    }

    @Override
    public boolean check(AutoTotem module) {
        if (!module.getChecks().isSelected(getKey())) {
            return false;
        }
        return module.isElytraLowHealth();
    }
}
