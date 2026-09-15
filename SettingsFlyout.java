package ru.meow.gui.clickgui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import ru.meow.domain.animation.Animation;
import ru.meow.domain.animation.Easings;
import ru.meow.domain.setting.BindSetting;
import ru.meow.domain.setting.ModeSetting;
import ru.meow.domain.setting.SliderSetting;
import ru.meow.gui.clickgui.widget.Widget;
import ru.meow.gui.clickgui.widget.setting.BooleanRow;
import ru.meow.gui.clickgui.widget.setting.ModeRow;
import ru.meow.gui.clickgui.widget.setting.SliderRow;
import ru.meow.render.Fonts;
import ru.meow.render.RenderUtil;
import ru.meow.render.theme.GuiTheme;
import ru.meow.service.LanguageService;
import ru.meow.service.Managers;
import ru.meow.util.ColorUtil;
import ru.meow.util.MathUtil;

public final class SettingsFlyout {

    private static final SettingsFlyout INSTANCE = new SettingsFlyout();
    private static final String COG = "b";

    private final ModeSetting swapMode = new ModeSetting("swapMode", "swapModeBypass",
            "swapModeBypass", "swapModeDefault", "swapModeSilent");
    private final ModeSetting auraReset = new ModeSetting("auraReset", "auraResetNone",
            "auraResetNone", "auraResetAlways", "auraResetCustom");
    private final SliderSetting guiYawSpeed = new SliderSetting("guiYawSpeed", 180.0F, 1.0F, 360.0F, 1.0F);
    private final SliderSetting guiPitchSpeed = new SliderSetting("guiPitchSpeed", 180.0F, 1.0F, 360.0F, 1.0F);
    private final SliderSetting guiTimeout = new SliderSetting("guiTimeout", 1.0F, 1.0F, 20.0F, 1.0F);
    private final BindSetting guiDragging = new BindSetting("guiDragging", true);
    private final BindSetting guiSavePosition = new BindSetting("guiSavePosition", false);
    private final BindSetting guiOpenAnimation = new BindSetting("guiOpenAnimation", true);

    private static final float PANEL_WIDTH = 125.0F;
    private static final float PANEL_PADDING = 8.0F;
    private static final float ROW_PITCH = 7.0F;
    private static final float LABEL_COLUMN = 32.0F;
    private static final float FONT = 7.0F;
    private static final float ROW_SCALE = 0.95F;
    private static final float COG_FONT = 7.2F;
    private static final float ABOUT_TITLE_FONT = 13.0F;
    private static final float ABOUT_SECTION_Y = 22.0F;
    private static final float PANEL_SECTION_GAP = 10.0F;
    private static final float SETTINGS_SECTION_Y = 63.0F;
    private static final float WIDGETS_SECTION_Y = 66.0F;

    private final Animation openAnim = new Animation();

    /**
     * Built once on first render, because the rows need the panel width and that
     * is not known until the GUI has laid itself out.
     */
    private final List<Widget> rows = new ArrayList<>();

    private float x;
    private float y;
    private float width;
    private float height;
    private boolean panelOpen;

    private SettingsFlyout() {
    }

    private void buildRows() {
        if (!this.rows.isEmpty()) {
            return;
        }

        this.guiYawSpeed.visibleIf(this::isCustomReset);
        this.guiPitchSpeed.visibleIf(this::isCustomReset);
        this.guiTimeout.visibleIf(this::isCustomReset);

        this.rows.add(new ModeRow(this.swapMode, ROW_SCALE));
        this.rows.add(new ModeRow(this.auraReset, ROW_SCALE));
        this.rows.add(new SliderRow(this.guiYawSpeed, ROW_SCALE));
        this.rows.add(new SliderRow(this.guiPitchSpeed, ROW_SCALE));
        this.rows.add(new SliderRow(this.guiTimeout, ROW_SCALE));
        this.rows.add(new BooleanRow(this.guiDragging, ROW_SCALE));
        this.rows.add(new BooleanRow(this.guiSavePosition, ROW_SCALE));
        this.rows.add(new BooleanRow(this.guiOpenAnimation, ROW_SCALE));
    }

    private boolean isCustomReset() {
        return this.auraReset.is("auraResetCustom");
    }

    public static SettingsFlyout get() {
        return INSTANCE;
    }

    public BindSetting dragging() {
        return guiDragging;
    }

    public BindSetting savePosition() {
        return guiSavePosition;
    }

    public BindSetting openAnimation() {
        return guiOpenAnimation;
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(DrawContext context, float mouseX, float mouseY) {
        boolean hover = MathUtil.isHovered(mouseX, mouseY, x, y, width, height);
        int col = ColorUtil.mix(ColorUtil.rgba(86, 86, 86, 30), GuiTheme.accent(50), hover ? 0.5F : 0.0F);
        RenderUtil.roundedRect(context.getMatrices(), x, y, width, height, 2.5F, col);
        Fonts.MEOW.drawFullCenteredString(context.getMatrices().peek().getPositionMatrix(), COG,
                x + width / 2.0F, y + height / 2.0F, ColorUtil.fullAlpha(-1), COG_FONT);
        openAnim.update();
        if (openAnim.getValueFloat() > 0.005F) {
            renderPanel(context, openAnim.getValueFloat(), mouseX, mouseY);
        }
    }

    private void renderPanel(DrawContext context, float alpha, float mouseX, float mouseY) {
        buildRows();

        float panelX = panelX(PANEL_WIDTH);
        float panelY = y + height + 4.0F;
        float panelHeight = PANEL_PADDING * 2.0F + ABOUT_SECTION_Y + PANEL_SECTION_GAP + visibleRowHeight();

        RenderUtil.roundedRect(context.getMatrices(), panelX, panelY, PANEL_WIDTH, panelHeight, 4.0F,
                ColorUtil.rgba(15, 15, 15, (int) (230 * alpha)));

        var matrix = context.getMatrices().peek().getPositionMatrix();
        LanguageService lang = LanguageService.get();
        int text = ColorUtil.fullAlpha(-1);

        Fonts.MEDIUM.drawString(matrix, lang.tr("guiAboutClient"),
                panelX + PANEL_PADDING, panelY + PANEL_PADDING, text, ABOUT_TITLE_FONT);

        float row = panelY + PANEL_PADDING + ABOUT_SECTION_Y;
        row = about(matrix, lang.tr("guiVersion"), "19/03/2026", panelX, row, text);
        row = about(matrix, lang.tr("guiBuild"), "Alpha", panelX, row, text);
        row = about(matrix, lang.tr("guiMCVersion"), "1.21.4", panelX, row, text);
        row = about(matrix, lang.tr("guiTelegram"), "t.me/soezproject", panelX, row, text);

        int divider = GuiTheme.outline((int) (255 * alpha));
        RenderUtil.roundedRect(context.getMatrices(), panelX + PANEL_PADDING, panelY + SETTINGS_SECTION_Y,
                PANEL_WIDTH - PANEL_PADDING * 2.0F, 1.0F, 0.0F, divider);

        row = panelY + WIDGETS_SECTION_Y;
        for (Widget widget : this.rows) {
            if (!widget.isVisible()) {
                continue;
            }
            widget.setX(panelX + PANEL_PADDING);
            widget.setY(row);
            widget.setWidth(PANEL_WIDTH - PANEL_PADDING * 2.0F);
            widget.render(context, mouseX, mouseY);
            row += widget.getHeight();
        }
    }

    private float about(org.joml.Matrix4f matrix, String label, String value,
            float panelX, float row, int color) {
        Fonts.MEDIUM.drawString(matrix, label, panelX + PANEL_PADDING, row, color, FONT);
        Fonts.MEDIUM.drawString(matrix, value, panelX + LABEL_COLUMN + PANEL_PADDING, row, color, FONT);
        return row + ROW_PITCH;
    }

    private float visibleRowHeight() {
        float total = 0.0F;
        for (Widget widget : this.rows) {
            if (widget.isVisible()) {
                total += widget.getHeight();
            }
        }
        return total;
    }

    private float panelX(float panelWidth) {
        float pad = 4.0F;
        float guiX = Managers.clickGui().screen().getLayout().getX();
        float left = guiX - panelWidth - pad;
        if (left >= 0.0F) {
            return left;
        }
        return guiX + Managers.clickGui().screen().getLayout().getWidth() + pad;
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (MathUtil.isHovered(mouseX, mouseY, x, y, width, height) && button == 0) {
            panelOpen = !panelOpen;
            openAnim.animate(panelOpen ? 1.0D : 0.0D, 0.15D, Easings.LINEAR);
            return true;
        }

        if (!isOpen()) {
            return false;
        }

        boolean handled = false;
        for (Widget widget : this.rows) {
            if (widget.isVisible() && widget.isHovered(mouseX, mouseY)) {
                widget.mouseClicked(mouseX, mouseY, button);
                handled = true;
            }
        }
        return handled;
    }

    public void mouseReleased(float mouseX, float mouseY, int button) {
        for (Widget widget : this.rows) {
            widget.mouseReleased(mouseX, mouseY, button);
        }
    }

    public void keyPressed(int key, int scanCode, int modifiers) {
        for (Widget widget : this.rows) {
            widget.keyPressed(key, scanCode, modifiers);
        }
    }

    public void charTyped(char character, int modifiers) {
        for (Widget widget : this.rows) {
            widget.charTyped(character, modifiers);
        }
    }

    public boolean isOpen() {
        return this.panelOpen || this.openAnim.getValueFloat() > 0.005F;
    }

    /** Restores every flyout setting to the value the module shipped with. */
    public void resetAll() {
        this.swapMode.setMode("swapModeBypass");
        this.auraReset.setMode("auraResetNone");
        this.guiYawSpeed.setValue(180.0D);
        this.guiPitchSpeed.setValue(180.0D);
        this.guiTimeout.setValue(1.0D);
        this.guiDragging.setValue(true);
        this.guiSavePosition.setValue(false);
        this.guiOpenAnimation.setValue(true);
    }

    public ModeSetting getSwapMode() {
        return this.swapMode;
    }

    public ModeSetting getAuraReset() {
        return this.auraReset;
    }

    public SliderSetting getYawSpeed() {
        return this.guiYawSpeed;
    }

    public SliderSetting getPitchSpeed() {
        return this.guiPitchSpeed;
    }

    public SliderSetting getTimeout() {
        return this.guiTimeout;
    }

    public void closeFlyout() {
        panelOpen = false;
        openAnim.animate(0.0D, 0.15D, Easings.LINEAR);
    }

    public boolean isHovered(float mouseX, float mouseY) {
        if (MathUtil.isHovered(mouseX, mouseY, x, y, width, height)) {
            return true;
        }
        if (!isOpen()) {
            return false;
        }
        float panelX = panelX(PANEL_WIDTH);
        float panelY = y + height + 4.0F;
        float panelHeight = PANEL_PADDING * 2.0F + ABOUT_SECTION_Y + PANEL_SECTION_GAP + visibleRowHeight();
        return MathUtil.isHovered(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, panelHeight);
    }
}
