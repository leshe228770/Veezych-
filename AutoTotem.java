package ru.meow.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import ru.meow.domain.event.impl.EventPacket;
import ru.meow.domain.event.impl.EventRender2D;
import ru.meow.domain.event.impl.EventUpdate;
import ru.meow.domain.module.Module;
import ru.meow.domain.module.ModuleCategory;
import ru.meow.domain.module.ModuleInfo;
import ru.meow.domain.setting.BindSetting;
import ru.meow.domain.setting.MultiSelectSetting;
import ru.meow.domain.setting.SliderSetting;
import ru.meow.util.InventoryUtil;

/**
 * Offhand totem swapping with configurable threat checks and swap-back state.
 */
public class AutoTotem extends Module {

    private static final int OFFHAND_SLOT = 40;
    private static final long SWAP_DELAY_MS = 200L;
    private static final long SWAP_BACK_EXTRA_MS = 200L;
    private static final float HEALTH_BLOCK_SWAP_BACK = 2.8F;
    private static final float SWAP_BACK_MARGIN = 4.0F;
    private static final int SAFE_TICKS_BEFORE_SWAP_BACK = 10;
    private static final String DRAGON_TOTEM_NAME = "Драконида";

    private final MultiSelectSetting checks = new MultiSelectSetting(
            "autoTotemChecks",
            "autoTotemCheckHealth",
            "autoTotemCheckCrystal",
            "autoTotemCheckFall",
            "autoTotemCheckElytra",
            "autoTotemCheckVoid",
            "autoTotemCheckAnchor",
            "autoTotemCheckTntMinecart",
            "autoTotemCheckTrident")
            .withSelected(
                    "autoTotemCheckHealth",
                    "autoTotemCheckCrystal",
                    "autoTotemCheckFall",
                    "autoTotemCheckElytra",
                    "autoTotemCheckVoid",
                    "autoTotemCheckAnchor",
                    "autoTotemCheckTntMinecart",
                    "autoTotemCheckTrident");

    private final SliderSetting healthThreshold = new SliderSetting("autoTotemHealthThreshold", 6.0F, 1.0F, 20.0F, 0.5F);
    private final SliderSetting crystalRange = new SliderSetting("autoTotemCrystalRange", 6.0F, 1.0F, 12.0F, 0.5F);
    private final SliderSetting fallDistance = new SliderSetting("autoTotemFallDistance", 10.0F, 3.0F, 50.0F, 1.0F);
    private final SliderSetting elytraHealth = new SliderSetting("autoTotemElytraHealth", 10.0F, 1.0F, 20.0F, 0.5F);
    private final SliderSetting tntRange = new SliderSetting("autoTotemTntRange", 50.0F, 1.0F, 50.0F, 1.0F);
    private final SliderSetting tridentRange = new SliderSetting("autoTotemTridentRange", 50.0F, 1.0F, 50.0F, 1.0F);
    private final SliderSetting tntMinecartRange = new SliderSetting("autoTotemTntMinecartRange", 10.0F, 1.0F, 10.0F, 1.0F);
    private final SliderSetting anchorRange = new SliderSetting("autoTotemAnchorRange", 6.0F, 1.0F, 10.0F, 1.0F);
    private final BindSetting ballHpCheck = new BindSetting("autoTotemBallHpCheck", false);
    private final SliderSetting ballHpThreshold = new SliderSetting("autoTotemBallHpCheckCrystal", 10.0F, 1.0F, 20.0F, 0.5F);
    private final BindSetting swapBack = new BindSetting("autoTotemSwapBack", true);
    private final BindSetting noSwapIfEating = new BindSetting("autoTotemNoSwapIfEating", false);
    private final BindSetting saveEnchanted = new BindSetting("autoTotemSaveEnchanted", false);
    private final BindSetting showCount = new BindSetting("autoTotemShowCount", false);

    private final AutoTotemThreatEngine threatEngine = new AutoTotemThreatEngine();
    private final AutoTotemHud hud = new AutoTotemHud();

    private ItemStack savedOffhand = ItemStack.EMPTY;
    private int savedSlot = -1;
    private long lastSwapTime;
    private int totemCount;
    private boolean totemPopPending;
    private long totemPopTime;
    private boolean swappedByModule;
    private boolean awaitingSwapBack;
    private boolean holdSwapBack;
    private int safeTicks;
    private long lastThreatTime;

    public AutoTotem() {
        super(new ModuleInfo("AutoTotem", ModuleCategory.Combat));
        addSettings(
                this.checks,
                this.healthThreshold,
                this.crystalRange,
                this.fallDistance,
                this.elytraHealth,
                this.tntRange,
                this.tridentRange,
                this.tntMinecartRange,
                this.anchorRange,
                this.ballHpCheck,
                this.ballHpThreshold,
                this.swapBack,
                this.noSwapIfEating,
                this.saveEnchanted,
                this.showCount);
    }

    MultiSelectSetting getChecks() {
        return this.checks;
    }

    SliderSetting getCrystalRange() {
        return this.crystalRange;
    }

    SliderSetting getFallDistance() {
        return this.fallDistance;
    }

    SliderSetting getBallHpThreshold() {
        return this.ballHpThreshold;
    }

    BindSetting getBallHpCheck() {
        return this.ballHpCheck;
    }

    SliderSetting getTntRange() {
        return this.tntRange;
    }

    SliderSetting getTridentRange() {
        return this.tridentRange;
    }

    SliderSetting getTntMinecartRange() {
        return this.tntMinecartRange;
    }

    SliderSetting getAnchorRange() {
        return this.anchorRange;
    }

    ItemStack getOffhandPreview() {
        ItemStack offhand = mc.player.getOffHandStack();
        if (!offhand.isEmpty() && isValidTotem(offhand)) {
            return offhand;
        }
        if (this.savedOffhand != null && !this.savedOffhand.isEmpty()) {
            return this.savedOffhand;
        }
        return offhand;
    }

    public boolean isElytraCheck() {
        return this.checks.isSelected("autoTotemCheckElytra") && hasElytraEquipped();
    }

    public boolean isElytraLowHealth() {
        return isElytraCheck() && getHealth(false) <= this.elytraHealth.getValue();
    }

    public float getEffectiveHealthThreshold() {
        if (isElytraCheck()) {
            return this.elytraHealth.getValue();
        }
        if (hasElytraEquipped()) {
            return getHealth(false);
        }
        return this.healthThreshold.getValue();
    }

    public float getHealthThresholdSetting() {
        return this.healthThreshold.getValue();
    }

    public float getHealth(boolean absorption) {
        if (mc.player == null) {
            return 0.0F;
        }
        float health = mc.player.getHealth();
        if (absorption) {
            health += mc.player.getAbsorptionAmount();
        }
        return health;
    }

    public boolean needsTotem() {
        return this.threatEngine.needsTotem(this);
    }

    public int getTotemCount() {
        return this.totemCount;
    }

    @Override
    public void onDisable() {
        this.savedOffhand = ItemStack.EMPTY;
        this.savedSlot = -1;
        this.lastSwapTime = 0L;
        this.totemPopPending = false;
        this.swappedByModule = false;
        this.awaitingSwapBack = false;
        this.holdSwapBack = false;
        this.safeTicks = 0;
    }

    @Subscribe
    public void onUpdate(EventUpdate.Post event) {
        if (!nullCheck()) {
            return;
        }
        long now = System.currentTimeMillis();
        recountTotems();
        boolean threat = needsTotem();
        boolean hasTotemInInventory = findTotemContainerSlot() != -1;
        ItemStack offhand = mc.player.getOffHandStack();
        boolean offhandTotem = isValidTotem(offhand);

        if (threat) {
            this.lastThreatTime = now;
            this.safeTicks = 0;
        } else {
            this.safeTicks++;
        }

        if (isTotemInMainHand()) {
            return;
        }

        if (this.totemPopPending) {
            if (now - this.totemPopTime < SWAP_DELAY_MS) {
                return;
            }
            this.totemPopPending = false;
            if (hasTotemInInventory && this.holdSwapBack) {
                this.awaitingSwapBack = true;
            }
            if (canSwapBack(now) && swapBackToSaved()) {
                this.holdSwapBack = false;
                this.awaitingSwapBack = false;
            }
            return;
        }

        if (offhandTotem && !this.swappedByModule) {
            return;
        }

        if (!offhandTotem && !hasTotemInInventory && canSwapBack(now) && swapBackToSaved()) {
            this.holdSwapBack = false;
            return;
        }

        if (threat) {
            if (!offhandTotem && hasTotemInInventory) {
                this.holdSwapBack = true;
                swapTotemToOffhand();
            }
            return;
        }

        if (this.swappedByModule && this.savedOffhand != null && !this.savedOffhand.isEmpty() && !offhandTotem) {
            return;
        }

        if (now - this.lastSwapTime < SWAP_DELAY_MS) {
            return;
        }

        if (canSwapBack(now) && swapBackToSaved()) {
            this.holdSwapBack = false;
            this.awaitingSwapBack = false;
        }
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!isEnabled() || !event.isReceive()) {
            return;
        }
        if (!(event.getPacket() instanceof EntityStatusS2CPacket status)) {
            return;
        }
        if (status.getStatus() != 35) {
            return;
        }
        Entity entity = status.getEntity(mc.world);
        if (entity != mc.player) {
            return;
        }
        this.totemPopPending = true;
        this.totemPopTime = System.currentTimeMillis();
        this.lastThreatTime = this.totemPopTime;
        this.awaitingSwapBack = this.swappedByModule && this.savedOffhand != null && !this.savedOffhand.isEmpty();
    }

    @Subscribe
    public void onRender(EventRender2D.Post event) {
        if (!isEnabled() || !this.showCount.getValue()) {
            return;
        }
        this.hud.render(event, this.totemCount);
    }

    private boolean isTotemInMainHand() {
        return isValidTotem(mc.player.getMainHandStack());
    }

    private boolean canSwapBack(long now) {
        if (!this.swapBack.getValue()) {
            return false;
        }
        if (this.noSwapIfEating.getValue() && mc.player.isUsingItem()) {
            return false;
        }
        if (!canSwapBackByHealth()) {
            return false;
        }
        if (needsCrystalBallHold()) {
            return false;
        }
        if (now - this.lastThreatTime < SWAP_DELAY_MS) {
            return false;
        }
        if (this.safeTicks < SAFE_TICKS_BEFORE_SWAP_BACK) {
            return false;
        }
        if (this.holdSwapBack) {
            if (now - this.lastSwapTime < SWAP_BACK_EXTRA_MS) {
                return false;
            }
        }
        return true;
    }

    private boolean needsCrystalBallHold() {
        if (!this.checks.isSelected("autoTotemCheckCrystal")) {
            return false;
        }
        return this.threatEngine.needsTotem(this) && crystalThreatWithBall();
    }

    private boolean crystalThreatWithBall() {
        AutoTotemCheckCrystal crystal = new AutoTotemCheckCrystal();
        return crystal.check(this);
    }

    private boolean canSwapBackByHealth() {
        if (isHealthTooLowForSwapBack()) {
            return false;
        }
        if (isElytraCheck()) {
            return getHealth(false) > this.elytraHealth.getValue() + SWAP_BACK_MARGIN;
        }
        return getEffectiveHealthThreshold() + SWAP_BACK_MARGIN < getHealth(true);
    }

    private boolean isHealthTooLowForSwapBack() {
        return getHealth(false) <= HEALTH_BLOCK_SWAP_BACK;
    }

    private boolean swapTotemToOffhand() {
        ItemStack offhand = mc.player.getOffHandStack();
        if (isValidTotem(offhand)) {
            return false;
        }
        rememberOffhandIfNeeded(offhand);
        int slot = findTotemContainerSlot();
        if (slot == -1) {
            return false;
        }
        if (!InventoryUtil.clickSlotPair(slot, OFFHAND_SLOT, 0, SlotActionType.PICKUP)) {
            return false;
        }
        this.swappedByModule = true;
        this.awaitingSwapBack = false;
        this.savedSlot = slot;
        this.lastSwapTime = System.currentTimeMillis();
        this.lastThreatTime = this.lastSwapTime;
        return true;
    }

    private boolean swapBackToSaved() {
        if (this.savedOffhand == null || this.savedOffhand.isEmpty()) {
            return false;
        }
        ItemStack offhand = mc.player.getOffHandStack();
        if (isValidTotem(offhand) && !acceptsSavedSwap(offhand, true)) {
            return false;
        }
        if (offhand.isEmpty() || isValidTotem(offhand)) {
            int slot = resolveSavedSlot();
            if (slot == -1) {
                slot = findStackSlot(this.savedOffhand);
            }
            if (slot == -1) {
                return false;
            }
            if (!InventoryUtil.clickSlotPair(slot, OFFHAND_SLOT, 0, SlotActionType.PICKUP)) {
                return false;
            }
            this.swappedByModule = false;
            this.lastSwapTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    private void rememberOffhandIfNeeded(ItemStack offhand) {
        if (this.savedOffhand != null && !this.savedOffhand.isEmpty()) {
            return;
        }
        if (offhand.isEmpty() || isValidTotem(offhand)) {
            return;
        }
        this.savedOffhand = offhand.copy();
        this.savedSlot = findStackSlot(this.savedOffhand);
    }

    private int resolveSavedSlot() {
        if (this.savedSlot != -1 && slotMatches(this.savedSlot, this.savedOffhand)) {
            return this.savedSlot;
        }
        return findStackSlot(this.savedOffhand);
    }

    /**
     * Always resolved against the player's own inventory screen, not whatever
     * container is on screen, so a swap still finds the totem with a chest open.
     */
    private boolean slotMatches(int containerSlot, ItemStack template) {
        if (mc.player == null) {
            return false;
        }
        if (containerSlot < 0 || containerSlot >= mc.player.playerScreenHandler.slots.size()) {
            return false;
        }
        return ItemStack.areEqual(
                mc.player.playerScreenHandler.getSlot(containerSlot).getStack(), template);
    }

    private int findStackSlot(ItemStack template) {
        if (template == null || template.isEmpty()) {
            return -1;
        }
        boolean templateTotem = isValidTotem(template);
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (ItemStack.areEqual(stack, template)) {
                return i < 9 ? i + 36 : i;
            }
        }
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() != template.getItem()) {
                continue;
            }
            if (templateTotem) {
                if (!isValidTotem(stack)) {
                    continue;
                }
            } else if (!acceptsSavedSwap(stack, false)) {
                continue;
            }
            return i < 9 ? i + 36 : i;
        }
        return -1;
    }

    private boolean acceptsSavedSwap(ItemStack stack, boolean strictEnchant) {
        if (strictEnchant && this.saveEnchanted.getValue() && this.totemCount > 0 && stack.hasEnchantments()) {
            return false;
        }
        return true;
    }

    private int findTotemContainerSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isAcceptableTotem(stack)) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    private void recountTotems() {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isAcceptableTotem(stack) && !stack.hasEnchantments()) {
                count += stack.getCount();
            }
        }
        ItemStack offhand = mc.player.getOffHandStack();
        if (isAcceptableTotem(offhand) && !offhand.hasEnchantments()) {
            count += offhand.getCount();
        }
        this.totemCount = count;
    }

    private boolean isAcceptableTotem(ItemStack stack) {
        return isValidTotem(stack) && acceptsSavedSwap(stack, true);
    }

    private boolean isValidTotem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.TOTEM_OF_UNDYING) {
            return false;
        }
        if (stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            String name = stack.getName().getString();
            if (name.contains(DRAGON_TOTEM_NAME)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasElytraEquipped() {
        return mc.player.getInventory().getArmorStack(2).isOf(Items.ELYTRA);
    }
}
