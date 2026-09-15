package ru.meow.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.meow.domain.event.impl.EventPlayerUpdate;
import ru.meow.domain.event.impl.EventRender2D;
import ru.meow.domain.event.impl.EventUpdate;
import ru.meow.domain.module.Module;
import ru.meow.domain.module.ModuleCategory;
import ru.meow.domain.module.ModuleInfo;
import ru.meow.domain.setting.ModeSetting;
import ru.meow.domain.setting.SliderSetting;
import ru.meow.service.Managers;
import ru.meow.util.Rotation;
import ru.meow.util.RotationUtil;

/** Aims the bow at nearby players with physical or arc prediction. */
public class BowAimBot extends Module {

    private static final double ARC_CHARGE_BASE = 0.8D;
    private static final double ARC_CHARGE_SCALE = 8.0D;
    private static final float GRAVITY = 0.05F;
    private static final int PREDICT_STEPS = 5;
    private static final float ROT_SPEED = 360.0F;

    /** Minimum charge-scaled projectile speed ({@code ъп}, {@code ън}). */
    private static final float MIN_PROJECTILE_SPEED = 5.5F;

    private static final float DISTANCE_DIVISOR = 5.0F;

    /** Distance tiers for body height bias ({@code ъл}, {@code ъЫ}). */
    private static final float HEIGHT_BIAS_NEAR = 1.8F;

    private static final float HEIGHT_BIAS_FAR = 1.2F;

    private static final double DIST_TIER_NEAR = 10.0D;

    private static final double DIST_TIER_MID = 20.0D;

    private static final double DIST_TIER_FAR = 30.0D;

    private final ModeSetting predictMode = new ModeSetting(
            "bowAimBotPredict",
            "bowAimBotPredictPhys",
            "bowAimBotPredictPhys",
            "bowAimBotPredictArc");

    private final SliderSetting fov = new SliderSetting("bowAimBotFov", 100.0F, 1.0F, 180.0F, 1.0F);

    private LivingEntity target;

    public BowAimBot() {
        super(new ModuleInfo("BowAimBot", ModuleCategory.Combat));
        addSettings(this.predictMode, this.fov);
    }

    @Subscribe
    public void onUpdate(EventUpdate.Post event) {
        if (!nullCheck()) {
            return;
        }
        if (!isHoldingRangedWeapon()) {
            this.target = null;
            return;
        }
        if (getActiveWeaponStack().getItem() instanceof BowItem && !mc.player.isUsingItem()) {
            this.target = null;
            return;
        }
        this.target = findTarget();
    }

    @Subscribe
    public void onPlayerUpdate(EventPlayerUpdate event) {
        if (!nullCheck() || !isHoldingRangedWeapon() || this.target == null) {
            return;
        }
        Vec3d aim = computeAimPoint(this.target);
        if (aim == null) {
            return;
        }
        Rotation rotation = RotationUtil.toRotation(aim.subtract(mc.player.getEyePos()));
        if (!isWithinFov(rotation)) {
            return;
        }
        Rotation current = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        mc.options.getMouseSensitivity().getValue();
        Rotation stepped = current.stepTowards(rotation, ROT_SPEED, ROT_SPEED).applyGcd(current);
        mc.player.setYaw(stepped.getYaw());
        mc.player.setPitch(MathHelper.clamp(stepped.getPitch(), -90.0F, 90.0F));
        CombatRotations.apply(stepped, ROT_SPEED, ROT_SPEED, 3, 6);
    }

    @Subscribe
    public void onRender(EventRender2D.Post event) {
        if (!isEnabled() || !nullCheck() || !isHoldingRangedWeapon() || !this.predictMode.is("bowAimBotPredictArc")) {
            return;
        }
        BowAimBotHud.drawFovRing(event, this, computeScreenFovRadius());
    }

    float getFovDegrees() {
        return this.fov.getValue();
    }

    LivingEntity getTarget() {
        return this.target;
    }

    private ItemStack getActiveWeaponStack() {
        ItemStack main = mc.player.getMainHandStack();
        if (main.getItem() instanceof BowItem || main.getItem() instanceof CrossbowItem) {
            return main;
        }
        ItemStack off = mc.player.getOffHandStack();
        if (off.getItem() instanceof BowItem || off.getItem() instanceof CrossbowItem) {
            return off;
        }
        return main;
    }

    private boolean isHoldingRangedWeapon() {
        ItemStack stack = getActiveWeaponStack();
        Item item = stack.getItem();
        return item == Items.BOW || item == Items.CROSSBOW || item instanceof BowItem || item instanceof CrossbowItem;
    }

    private LivingEntity findTarget() {
        LivingEntity aura = Managers.module(Aura.class).map(Aura::getTarget).orElse(null);
        if (aura != null && isValidTarget(aura) && isWithinFov(toTargetRotation(aura))) {
            return aura;
        }
        LivingEntity best = null;
        double bestAngle = this.fov.getValue();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!isValidTarget(player)) {
                continue;
            }
            Rotation rot = toTargetRotation(player);
            float angle = new Rotation(mc.player.getYaw(), mc.player.getPitch()).distanceTo(rot);
            if (angle <= bestAngle) {
                bestAngle = angle;
                best = player;
            }
        }
        return best;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == mc.player || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof PlayerEntity player) {
            if (Managers.friends().isFriend(player) || AntiBot.isBot(player)) {
                return false;
            }
        }
        return true;
    }

    private Rotation toTargetRotation(LivingEntity entity) {
        Vec3d aim = computeAimPoint(entity);
        if (aim == null) {
            EntityPose pose = entity.getPose();
            aim = entity.getPos().add(0.0D, entity.getEyeHeight(pose), 0.0D);
        }
        return RotationUtil.toRotation(aim.subtract(mc.player.getEyePos()));
    }

    private boolean isWithinFov(Rotation rotation) {
        Rotation current = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        return current.distanceTo(rotation) <= this.fov.getValue();
    }

    private Vec3d computeAimPoint(LivingEntity entity) {
        Vec3d point = pickBodyPoint(entity);
        if (this.predictMode.is("bowAimBotPredictArc")) {
            return predictArc(point, entity);
        }
        return predictPhysical(point, entity);
    }

    private Vec3d pickBodyPoint(LivingEntity entity) {
        Box box = entity.getBoundingBox();
        double heightFactor = heightBiasForDistance(entity);
        EntityPose pose = entity.getPose();
        float eyeHeight = entity.getEyeHeight(pose);
        Vec3d[] points = new Vec3d[] {
            new Vec3d(box.getCenter().x, box.minY + entity.getHeight() * 0.15D * heightFactor, box.getCenter().z),
            new Vec3d(box.getCenter().x, box.minY + eyeHeight * 0.5D, box.getCenter().z),
            new Vec3d(box.getCenter().x, box.minY + eyeHeight * 0.85D / heightFactor, box.getCenter().z)
        };
        Vec3d eyes = mc.player.getEyePos();
        Vec3d best = points[0];
        double bestScore = -1.0D;
        for (Vec3d sample : points) {
            double vertical = sample.y - entity.getY();
            double dist = eyes.distanceTo(sample);
            double score = vertical * 0.5D - dist;
            if (score > bestScore) {
                bestScore = score;
                best = sample;
            }
        }
        return best;
    }

    private Vec3d predictPhysical(Vec3d point, LivingEntity entity) {
        Vec3d frameDelta = new Vec3d(
                entity.getX() - entity.prevX,
                entity.getY() - entity.prevY,
                entity.getZ() - entity.prevZ);
        Vec3d relative = frameDelta.subtract(mc.player.getVelocity());
        return point.add(relative.multiply(this.predictMode.is("bowAimBotPredictPhys") ? 6.0D : 4.0D));
    }

    private Vec3d predictArc(Vec3d point, LivingEntity entity) {
        double speed = computeProjectileSpeed(entity);
        Vec3d frameDelta = new Vec3d(
                entity.getX() - entity.prevX,
                entity.getY() - entity.prevY,
                entity.getZ() - entity.prevZ);
        Vec3d velocity = frameDelta.subtract(mc.player.getVelocity());
        Vec3d predicted = point;
        for (int i = 0; i < PREDICT_STEPS; i++) {
            double time = mc.player.getEyePos().distanceTo(predicted) / speed;
            predicted = point.add(velocity.multiply(time));
            predicted = predicted.subtract(0.0D, 0.5D * GRAVITY * time * time, 0.0D);
        }
        return predicted;
    }

    private double computeProjectileSpeed(LivingEntity entity) {
        ItemStack weapon = getActiveWeaponStack();
        float pull;
        if (weapon.getItem() instanceof CrossbowItem) {
            pull = CrossbowItem.isCharged(weapon)
                    ? 1.0F
                    : BowItem.getPullProgress(CrossbowItem.getPullTime(weapon, mc.player));
            pull *= Math.max(mc.player.distanceTo(entity) / DISTANCE_DIVISOR, MIN_PROJECTILE_SPEED);
        } else {
            pull = BowItem.getPullProgress(mc.player.getItemUseTime());
            if (pull <= 0.01F) {
                pull = 1.0F;
            }
            pull *= Math.max(mc.player.distanceTo(entity) / DISTANCE_DIVISOR, MIN_PROJECTILE_SPEED);
        }
        double arrowFactor = arrowSpeedFactor();
        return (ARC_CHARGE_BASE + ARC_CHARGE_SCALE * pull) * arrowFactor;
    }

    private double arrowSpeedFactor() {
        int slot = findArrowSlot();
        if (slot == -1) {
            return 1.0D;
        }
        Item item = mc.player.getInventory().getStack(slot).getItem();
        if (item == Items.SPECTRAL_ARROW) {
            return 1.0D;
        }
        if (item == Items.TIPPED_ARROW) {
            return 1.0D;
        }
        return item == Items.ARROW ? 1.0D : 1.0D;
    }

    private int findArrowSlot() {
        for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
            Item item = mc.player.getInventory().getStack(slot).getItem();
            if (item == Items.ARROW || item == Items.TIPPED_ARROW || item == Items.SPECTRAL_ARROW) {
                return slot;
            }
        }
        return -1;
    }

    private float heightBiasForDistance(LivingEntity entity) {
        double distance = mc.player.distanceTo(entity);
        if (distance < DIST_TIER_NEAR) {
            return 1.5F;
        }
        if (distance < DIST_TIER_MID) {
            return HEIGHT_BIAS_NEAR;
        }
        if (distance < DIST_TIER_FAR) {
            return 2.5F;
        }
        return HEIGHT_BIAS_FAR;
    }

    private float computeScreenFovRadius() {
        float halfFov = Math.min(this.fov.getValue(), BowAimBotHud.MAX_FOV_DEGREES) / 2.0F;
        int guiScale = Math.max(1, mc.options.getGuiScale().getValue());
        double halfWidth = mc.getWindow().getScaledWidth() / 2.0D;
        return (float) (halfWidth * Math.tan(Math.toRadians(halfFov)) / guiScale);
    }
}
