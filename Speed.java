package ru.meow.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ru.meow.domain.event.impl.EventInput;
import ru.meow.domain.event.impl.EventJump;
import ru.meow.domain.event.impl.EventPacket;
import ru.meow.domain.event.impl.EventStrafe;
import ru.meow.domain.event.impl.EventUpdate;
import ru.meow.domain.module.Module;
import ru.meow.domain.module.ModuleCategory;
import ru.meow.domain.module.ModuleInfo;
import ru.meow.domain.setting.BindSetting;
import ru.meow.domain.setting.ModeSetting;
import ru.meow.domain.setting.SliderSetting;
import ru.meow.injection.ITimer;
import ru.meow.module.impl.movement.speed.SpeedBoost;
import ru.meow.module.impl.movement.speed.SpeedGround;
import ru.meow.module.impl.movement.speed.SpeedRwTimerBoost;
import ru.meow.module.impl.movement.speed.SpeedTimerBoostTest;
import ru.meow.module.impl.movement.speed.SpeedTimerFlag;
import ru.meow.util.FlyUtil;
import ru.meow.util.MoveUtil;
import ru.meow.util.TimerUtil;

public class Speed extends Module {

    public static int ticks;
    public static boolean flag = true;

    public final ModeSetting mode = new ModeSetting(
            "speedMode", "Collision", "Collision", "Grim", "Vanilla", "Grim Teleport");
    public final ModeSetting grimMode = new ModeSetting(
            "Grim Mode",
            "Boost",
            "Boost",
            "Ground",
            "Timer Flag",
            "RW Timer Boost",
            "Timer Boost Test")
            .visibleIf(() -> this.mode.is("Grim"));
    public final SliderSetting motionY = new SliderSetting("Motion Y", 0.0F, -0.03F, 0.03F, 0.001F)
            .visibleIf(() -> this.mode.is("Grim") && this.grimMode.is("Timer Flag"));
    public final BindSetting noAir = new BindSetting("No Air", true)
            .visibleIf(() -> this.mode.is("Grim") && this.grimMode.is("Ground"));
    public final BindSetting rwTimerBoost = new BindSetting("Буст от таймера", true)
            .visibleIf(() -> this.mode.is("Grim") && this.grimMode.is("RW Timer Boost"));
    public final SliderSetting rwTimerPower = new SliderSetting("Сила таймера", 1.7F, 1.0F, 5.0F, 0.1F)
            .visibleIf(() -> this.mode.is("Grim") && this.grimMode.is("RW Timer Boost") && this.rwTimerBoost.getValue());
    public final SliderSetting rwTimerReset = new SliderSetting("Сброс таймера", 0.3F, 0.0F, 0.9F, 0.0001F)
            .visibleIf(() -> this.mode.is("Grim") && this.grimMode.is("RW Timer Boost"));
    public final BindSetting testTimerBoost = new BindSetting("Буст от таймера", true)
            .visibleIf(() -> this.mode.is("Grim") && this.grimMode.is("Timer Boost Test"));
    public final SliderSetting testTimerPower = new SliderSetting("Сила таймера", 1.6F, 1.0F, 5.0F, 0.1F)
            .visibleIf(() -> this.mode.is("Grim") && this.grimMode.is("Timer Boost Test") && this.testTimerBoost.getValue());
    public final SliderSetting testTimerReset = new SliderSetting("Сброс таймера", 0.4F, 0.0F, 0.9F, 0.0001F)
            .visibleIf(() -> this.mode.is("Grim") && this.grimMode.is("Timer Boost Test"));
    public final SliderSetting speed = new SliderSetting("speedSliderSpeed", 3.0F, 1.0F, 10.0F, 0.1F)
            .visibleIf(() -> !this.mode.is("Grim Teleport") && !this.mode.is("Grim"));
    public final SliderSetting radius = new SliderSetting("speedSliderRadius", 0.5F, 0.1F, 15.0F, 0.1F)
            .visibleIf(() -> this.mode.is("Collision"));

    private final TimerUtil timer = new TimerUtil();
    private boolean teleporting;
    private boolean cancelJump;

    public Speed() {
        super(new ModuleInfo("Speed", ModuleCategory.Movement));
        addSettings(
                this.mode,
                this.grimMode,
                this.motionY,
                this.noAir,
                this.rwTimerBoost,
                this.rwTimerPower,
                this.rwTimerReset,
                this.testTimerBoost,
                this.testTimerPower,
                this.testTimerReset,
                this.speed,
                this.radius);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.timer.reset();
        this.teleporting = false;
        resetState();
        if (this.mode.is("Grim")) {
            switch (this.grimMode.getMode()) {
                case "Boost" -> SpeedBoost.onEnable();
                case "Timer Flag" -> SpeedTimerFlag.onEnable();
                case "RW Timer Boost" -> SpeedRwTimerBoost.onEnable();
                case "Timer Boost Test" -> SpeedTimerBoostTest.onEnable();
                default -> flag = false;
            }
        }
        if (nullCheck() && mc.getRenderTickCounter() instanceof RenderTickCounter.Dynamic dynamic
                && dynamic instanceof ITimer timerMixin) {
            timerMixin.setSpeedMultiplier(1.0F);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
        if (nullCheck() && mc.getRenderTickCounter() instanceof RenderTickCounter.Dynamic dynamic
                && dynamic instanceof ITimer timerMixin) {
            timerMixin.setSpeedMultiplier(1.0F);
        }
        this.teleporting = false;
    }

    @Subscribe
    public void onStrafe(EventStrafe event) {
        if (!this.mode.is("Grim") || !nullCheck()) {
            return;
        }
        double[] dir = MoveUtil.getMoveDirection();
        switch (this.grimMode.getMode()) {
            case "Boost" -> SpeedBoost.apply(dir[0], dir[1]);
            case "Ground" -> SpeedGround.apply(this, dir[0], dir[1]);
            case "Timer Flag" -> SpeedTimerFlag.apply(this, dir[0], dir[1]);
            case "RW Timer Boost" -> SpeedRwTimerBoost.apply(this, dir[0], dir[1]);
            case "Timer Boost Test" -> SpeedTimerBoostTest.apply(this, dir[0], dir[1]);
            default -> {
            }
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (event instanceof EventUpdate.Post) {
            return;
        }
        setSuffix(this.mode.is("Grim") ? this.grimMode.getMode() : this.mode.getMode());
        switch (this.mode.getMode()) {
            case "Collision" -> collision();
            case "Vanilla" -> FlyUtil.fly(this.speed.getValue() / 5.0F, 0.0F);
            case "Grim Teleport" -> grimTeleport();
            default -> {
            }
        }
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!this.mode.is("Grim") || !event.isReceive()) {
            return;
        }
        switch (this.grimMode.getMode()) {
            case "Boost" -> SpeedBoost.onPacket(event);
            case "Ground" -> SpeedGround.onPacket(event);
            case "Timer Flag" -> SpeedTimerFlag.onPacket(event);
            case "RW Timer Boost" -> SpeedRwTimerBoost.onPacket(event);
            case "Timer Boost Test" -> SpeedTimerBoostTest.onPacket(event);
            default -> {
            }
        }
    }

    @Subscribe
    public void onInput(EventInput event) {
        if (!this.mode.is("Grim")) {
            return;
        }
        this.cancelJump = false;
        switch (this.grimMode.getMode()) {
            case "Boost" -> SpeedBoost.input(this, event);
            case "Timer Flag" -> SpeedTimerFlag.input(this, event);
            case "RW Timer Boost" -> SpeedRwTimerBoost.input(this, event);
            case "Timer Boost Test" -> SpeedTimerBoostTest.input(this, event);
            default -> {
            }
        }
        if (this.cancelJump) {
            event.setJumping(false);
        }
    }

    @Subscribe
    public void onJump(EventJump event) {
        if (!this.mode.is("Grim") || !nullCheck()) {
            return;
        }
        switch (this.grimMode.getMode()) {
            case "Ground" -> SpeedGround.tick(this);
            case "Timer Flag" -> SpeedTimerFlag.tick();
            case "RW Timer Boost" -> SpeedRwTimerBoost.tick(this);
            case "Timer Boost Test" -> SpeedTimerBoostTest.tick(this);
            default -> {
            }
        }
    }

    public void requestCancelJump() {
        this.cancelJump = true;
    }

    private void resetState() {
        flag = true;
        ticks = 0;
        this.cancelJump = false;
        MoveUtil.resetTimer();
    }

    private void grimTeleport() {
        if (!nullCheck()) {
            return;
        }
        if (this.timer.hasTimeElapsed(100L)) {
            this.teleporting = true;
        }
        if (this.timer.hasTimeElapsed(1400L)) {
            this.teleporting = false;
            this.timer.reset();
        }
        if (this.teleporting) {
            if (mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) {
                mc.player.jump();
            }
            if (mc.getRenderTickCounter() instanceof RenderTickCounter.Dynamic dynamic
                    && dynamic instanceof ITimer timerMixin) {
                timerMixin.setSpeedMultiplier(mc.player.age % 2 == 0 ? 1.5F : 1.2F);
            }
        } else if (mc.getRenderTickCounter() instanceof RenderTickCounter.Dynamic dynamic
                && dynamic instanceof ITimer timerMixin) {
            timerMixin.setSpeedMultiplier(1.0F);
        }
    }

    private void collision() {
        if (!nullCheck() || mc.player.isOnGround()) {
            return;
        }
        Box box = mc.player.getBoundingBox().expand(this.radius.getValue());
        LivingEntity best = null;
        double bestDot = -1.0D;
        for (LivingEntity living : mc.world.getEntitiesByClass(LivingEntity.class, box, entity -> true)) {
            if (living instanceof ArmorStandEntity) {
                continue;
            }
            if (living == mc.player) {
                continue;
            }
            if (!living.isAlive()) {
                continue;
            }
            if (!living.getBoundingBox().intersects(box)) {
                continue;
            }
            Vec3d diff = mc.player.getPos().subtract(living.getPos());
            double dot = living.getVelocity().dotProduct(diff.normalize());
            if (dot > bestDot) {
                bestDot = dot;
                best = living;
            }
        }
        if (best == null) {
            return;
        }
        double[] direction = FlyUtil.forward(this.speed.getValue() * 0.01D);
        mc.player.addVelocity(new Vec3d(direction[0], 0.0D, direction[1]));
    }
}
