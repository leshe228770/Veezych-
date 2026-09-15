package ru.meow.module.impl.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import ru.meow.domain.event.impl.EventGamma;
import ru.meow.domain.event.impl.EventUpdate;
import ru.meow.domain.module.Module;
import ru.meow.domain.module.ModuleCategory;
import ru.meow.domain.module.ModuleInfo;
import ru.meow.domain.setting.ModeSetting;
import ru.meow.domain.setting.SliderSetting;

/**
 * Night-vision style full bright, either by rewriting the lightmap gamma or by
 * applying a synthetic night-vision effect the module owns.
 */
public class FullBright extends Module {

    private final ModeSetting mode = new ModeSetting(
            "fullBrightMode",
            "fullBrightModeEffect",
            "fullBrightModeEffect",
            "fullBrightModeGamma");

    private final SliderSetting brightness = new SliderSetting(
            "fullBrightSliderBright", 1.5F, 0.0F, 1.5F, 0.1F);

    /** True while the night-vision instance currently on the player is ours. */
    private boolean applied;

    public FullBright() {
        super(new ModuleInfo(
                "FullBright",
                "Позволяет видеть в темноте",
                ModuleCategory.Render,
                -1));
        addSettings(this.mode, this.brightness);
    }

    @Override
    public void onEnable() {
        this.applied = false;
        tickEffect();
    }

    @Override
    public void onDisable() {
        removeEffect();
    }

    @Subscribe
    public void onUpdate(EventUpdate.Post event) {
        if (!isEnabled()) {
            return;
        }
        tickEffect();
    }

    @Subscribe
    public void onGamma(EventGamma event) {
        if (!isEnabled() || !this.mode.is("fullBrightModeGamma")) {
            return;
        }
        event.setValue(this.brightness.getValue());
        event.setMode(1);
    }

    private boolean isOurs(StatusEffectInstance effect) {
        return effect != null
                && effect.getAmplifier() == 2
                && effect.isAmbient()
                && !effect.shouldShowParticles();
    }

    private void applyEffect() {
        if (mc.player == null) {
            return;
        }
        mc.player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION, 420, 2, true, false));
        this.applied = true;
    }

    private void removeEffect() {
        if (mc.player == null) {
            this.applied = false;
            return;
        }
        StatusEffectInstance effect = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (this.applied && isOurs(effect)) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        this.applied = false;
    }

    private void tickEffect() {
        if (mc.player == null) {
            this.applied = false;
            return;
        }
        if (!this.mode.is("fullBrightModeEffect")) {
            removeEffect();
            return;
        }
        StatusEffectInstance effect = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (effect == null) {
            applyEffect();
            return;
        }
        if (this.applied && !isOurs(effect)) {
            this.applied = false;
            return;
        }
        if (this.applied && effect.getDuration() <= 240) {
            applyEffect();
        }
    }
}
