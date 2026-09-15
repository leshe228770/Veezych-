package ru.meow.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import ru.meow.domain.event.impl.EventUpdate;
import ru.meow.domain.module.Module;
import ru.meow.domain.module.ModuleCategory;
import ru.meow.domain.module.ModuleInfo;
import ru.meow.domain.setting.ModeSetting;
import ru.meow.domain.setting.SliderSetting;
import ru.meow.util.FlyUtil;

public class DragonFly extends Module {

    private final ModeSetting mode = new ModeSetting(
            "dragonFlyMode",
            "dragonFlyModeDefault",
            "dragonFlyModeDefault",
            "dragonFlyModeCustom");
    private final SliderSetting speedX = new SliderSetting("mainSpeedX", 1.012F, 0.1F, 5.0F, 0.1F);
    private final SliderSetting speedY = new SliderSetting("mainSpeedY", 1.0F, 0.1F, 5.0F, 0.1F);
    private final SliderSetting diagonalSpeed = new SliderSetting("dragonFlyDiagonalSpeed", 1.0109F, 0.1F, 5.0F, 0.1F)
            .visibleIf(() -> this.mode.is("dragonFlyModeCustom"));

    public DragonFly() {
        super(new ModuleInfo("DragonFly", ModuleCategory.Movement));
        addSettings(this.mode, this.speedX, this.speedY, this.diagonalSpeed);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (event instanceof EventUpdate.Post) {
            return;
        }
        if (mc.player == null || !mc.player.getAbilities().flying) {
            return;
        }
        if (this.mode.is("dragonFlyModeDefault")) {
            applyDefault();
        } else if (this.mode.is("dragonFlyModeCustom")) {
            applyCustom();
        }
    }

    private void applyDefault() {
        FlyUtil.fly(this.speedX.getValue(), this.speedY.getValue());
    }

    private void applyCustom() {
        boolean forward = mc.options.forwardKey.isPressed();
        boolean left = mc.options.leftKey.isPressed();
        boolean back = mc.options.backKey.isPressed();
        boolean right = mc.options.rightKey.isPressed();
        int pressed = 0;
        if (forward) {
            pressed++;
        }
        if (left) {
            pressed++;
        }
        if (back) {
            pressed++;
        }
        if (right) {
            pressed++;
        }
        int vertical = 0;
        float speed = this.speedX.getValue();
        if (pressed == 0 && vertical == 0) {
            FlyUtil.fly(speed, this.speedY.getValue());
            return;
        }
        if (pressed == 1) {
            speed = this.speedX.getValue();
        } else if (pressed == 2) {
            speed = (forward && left) || (forward && right) || (back && left) || (back && right)
                    ? this.diagonalSpeed.getValue()
                    : this.speedX.getValue();
        }
        if (pressed >= 1 && vertical >= 1) {
            speed *= 1.2F;
        }
        FlyUtil.fly(speed, this.speedY.getValue());
    }
}
