package local.luke.power.hud.mixin;

import local.luke.power.hud.Config;
import local.luke.power.hud.ModHelper;
import local.luke.power.hud.enums.CoordinateDisplayEnum;
import local.luke.power.hud.enums.HudPositioningSystemEnum;
import local.luke.power.hud.enums.ScreenPositionHorizontalEnum;
import local.luke.power.hud.enums.ScreenPositionVerticalEnum;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.client.util.ScreenScaler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class InGameHudMixin extends DrawContext implements local.luke.power.ui.StatusHudLayout {

    @Shadow private Minecraft minecraft;
    @Shadow private List messages;
    @Shadow private Random random;
    @Shadow private int overlayRemaining;
    @Shadow private String overlayMessage;

    @Shadow protected abstract void renderHotbarItem(int slot, int x, int y, float f);

    @Unique private int numberOfTurns    = 0;
    @Unique private int chatOffset       = 0;
    @Unique private int prevSelectedSlot = 0;
    @Unique private int scaledWidth    = 0;
    @Unique private int scaledHeight   = 0;
    @Unique private boolean isHotbarRaised = false;
    @Unique private boolean hotbarVisibility  = true;
    @Unique private int xOffsetHotbar  = 0;
    @Unique private int yOffsetHotbar  = 0;
    @Unique private boolean heartsVisibility  = true;
    @Unique private int xOffsetHearts  = 0;
    @Unique private int yOffsetHearts  = 0;
    @Unique private boolean armorVisibility   = true;
    @Unique private int xOffsetArmor   = 0;
    @Unique private int yOffsetArmor   = 0;
    @Unique private boolean oxygenVisibility  = true;
    @Unique private int xOffsetOxygen  = 0;
    @Unique private int yOffsetOxygen  = 0;
    @Unique private boolean overlayVisibility = true;
    @Unique private int xOffsetOverlay = 0;
    @Unique private int yOffsetOverlay = 0;

    @Unique private int xOffsetStamina, yOffsetStamina;
    @Unique private boolean staminaVisibility = true;
    public int power$staminaX() { return scaledWidth / 2 + 10 + xOffsetStamina; }
    public int power$staminaY() { return scaledHeight - 32 + yOffsetStamina; }
    public int power$staminaOffsetX() { return Config.hudpositions.hudPositioningSystem.ordinal() == 2
        ? Config.hudpositions.ADVANCED_HUD_POSITIONS_CONFIG.STAMINA_POSITION_CONFIG.horizontalPositionOffset : 0; }
    public int power$staminaOffsetY() { return Config.hudpositions.hudPositioningSystem.ordinal() == 2
        ? Config.hudpositions.ADVANCED_HUD_POSITIONS_CONFIG.STAMINA_POSITION_CONFIG.verticalPositionOffset : 0; }
    public boolean power$staminaVisible() { return staminaVisibility; }

    public int power$statusRight() { return scaledWidth / 2 + 91 + xOffsetHotbar; }
    public int power$statusY() {
        return scaledHeight - 32 + yOffsetHotbar
            + (Config.hudpositions.hudPositioningSystem.ordinal() != 0
                && Config.hudpositions.putStatusBarIconsBelowHotbar ? 33 : 0);
    }
    public int power$armorX() { return scaledWidth / 2 + 10 + xOffsetArmor; }
    public int power$armorY() { return scaledHeight - 32 + yOffsetArmor; }
    public boolean power$armorVisible() { return armorVisibility; }
    public boolean power$hotbarVisible() { return hotbarVisibility; }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void powerHud_render(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
        ScreenScaler screenScaler = new ScreenScaler(this.minecraft.options, this.minecraft.displayWidth, this.minecraft.displayHeight);

        scaledWidth = screenScaler.getScaledWidth();
        scaledHeight = screenScaler.getScaledHeight();
        local.luke.power.chat.ChatSelection.history.begin(
            minecraft.currentScreen instanceof ChatScreen && org.lwjgl.opengl.Display.isActive()
                ? minecraft.currentScreen : null);
        ModHelper.configUpdated = false;
        int mode = Config.hudpositions.hudPositioningSystem.ordinal();
        var simple = Config.hudpositions.SIMPLE_HUD_POSITION_CONFIG;
        int messageWidth = mode == 2 && Config.hudpositions.ADVANCED_HUD_POSITIONS_CONFIG.OVERLAY_MESSAGE_POSITION_CONFIG.horizontalPosition != ScreenPositionHorizontalEnum.CENTERED
            ? minecraft.textRenderer.getWidth(overlayMessage) : 0;
        var hotbar = Config.hudpositions.ADVANCED_HUD_POSITIONS_CONFIG.HOTBAR_POSITION_CONFIG;
        int hotbarHorizontal = (mode == 1 ? simple.horizontalPosition : hotbar.horizontalPosition).ordinal();
        int hotbarVertical = (mode == 1 ? simple.verticalPosition : hotbar.verticalPosition).ordinal();
        xOffsetHotbar = local.luke.power.ui.NativeHudLayout.xOffset(scaledWidth, 0, mode, hotbarHorizontal, messageWidth)
            + (mode == 0 ? 0 : mode == 1 ? simple.horizontalPositionOffset : hotbar.horizontalPositionOffset);
        yOffsetHotbar = local.luke.power.ui.NativeHudLayout.yOffset(scaledHeight, 0, mode, hotbarVertical,
            Config.hudpositions.putStatusBarIconsBelowHotbar, Config.hudpositions.putOverlayMessagesBelowHotbar)
            + (mode == 0 ? 0 : mode == 1 ? simple.verticalPositionOffset : hotbar.verticalPositionOffset);
        hotbarVisibility = mode == 0 || (mode == 1 ? simple.enableVisibility : hotbar.enableVisibility);
        var hearts = Config.hudpositions.ADVANCED_HUD_POSITIONS_CONFIG.HEARTS_POSITION_CONFIG;
        int heartsHorizontal = (mode == 1 ? simple.horizontalPosition : hearts.horizontalPosition).ordinal();
        int heartsVertical = (mode == 1 ? simple.verticalPosition : hearts.verticalPosition).ordinal();
        xOffsetHearts = local.luke.power.ui.NativeHudLayout.xOffset(scaledWidth, 1, mode, heartsHorizontal, messageWidth)
            + (mode == 0 ? 0 : mode == 1 ? simple.horizontalPositionOffset : hearts.horizontalPositionOffset);
        yOffsetHearts = local.luke.power.ui.NativeHudLayout.yOffset(scaledHeight, 1, mode, heartsVertical,
            Config.hudpositions.putStatusBarIconsBelowHotbar, Config.hudpositions.putOverlayMessagesBelowHotbar)
            + (mode == 0 ? 0 : mode == 1 ? simple.verticalPositionOffset : hearts.verticalPositionOffset);
        heartsVisibility = mode == 0 || (mode == 1 ? simple.enableVisibility : hearts.enableVisibility);
        var armor = Config.hudpositions.ADVANCED_HUD_POSITIONS_CONFIG.ARMOR_POSITION_CONFIG;
        int armorHorizontal = (mode == 1 ? simple.horizontalPosition : armor.horizontalPosition).ordinal();
        int armorVertical = (mode == 1 ? simple.verticalPosition : armor.verticalPosition).ordinal();
        xOffsetArmor = local.luke.power.ui.NativeHudLayout.xOffset(scaledWidth, 2, mode, armorHorizontal, messageWidth)
            + (mode == 0 ? 0 : mode == 1 ? simple.horizontalPositionOffset : armor.horizontalPositionOffset);
        yOffsetArmor = local.luke.power.ui.NativeHudLayout.yOffset(scaledHeight, 2, mode, armorVertical,
            Config.hudpositions.putStatusBarIconsBelowHotbar, Config.hudpositions.putOverlayMessagesBelowHotbar)
            + (mode == 0 ? 0 : mode == 1 ? simple.verticalPositionOffset : armor.verticalPositionOffset);
        armorVisibility = mode == 0 || (mode == 1 ? simple.enableVisibility : armor.enableVisibility);
        var oxygen = Config.hudpositions.ADVANCED_HUD_POSITIONS_CONFIG.OXYGEN_POSITION_CONFIG;
        int oxygenHorizontal = (mode == 1 ? simple.horizontalPosition : oxygen.horizontalPosition).ordinal();
        int oxygenVertical = (mode == 1 ? simple.verticalPosition : oxygen.verticalPosition).ordinal();
        xOffsetOxygen = local.luke.power.ui.NativeHudLayout.xOffset(scaledWidth, 3, mode, oxygenHorizontal, messageWidth)
            + (mode == 0 ? 0 : mode == 1 ? simple.horizontalPositionOffset : oxygen.horizontalPositionOffset);
        yOffsetOxygen = local.luke.power.ui.NativeHudLayout.yOffset(scaledHeight, 3, mode, oxygenVertical,
            Config.hudpositions.putStatusBarIconsBelowHotbar, Config.hudpositions.putOverlayMessagesBelowHotbar)
            + (mode == 0 ? 0 : mode == 1 ? simple.verticalPositionOffset : oxygen.verticalPositionOffset);
        oxygenVisibility = mode == 0 || (mode == 1 ? simple.enableVisibility : oxygen.enableVisibility);
        var overlay = Config.hudpositions.ADVANCED_HUD_POSITIONS_CONFIG.OVERLAY_MESSAGE_POSITION_CONFIG;
        int overlayHorizontal = (mode == 1 ? simple.horizontalPosition : overlay.horizontalPosition).ordinal();
        int overlayVertical = (mode == 1 ? simple.verticalPosition : overlay.verticalPosition).ordinal();
        xOffsetOverlay = local.luke.power.ui.NativeHudLayout.xOffset(scaledWidth, 4, mode, overlayHorizontal, messageWidth)
            + (mode == 0 ? 0 : mode == 1 ? simple.horizontalPositionOffset : overlay.horizontalPositionOffset);
        yOffsetOverlay = local.luke.power.ui.NativeHudLayout.yOffset(scaledHeight, 4, mode, overlayVertical,
            Config.hudpositions.putStatusBarIconsBelowHotbar, Config.hudpositions.putOverlayMessagesBelowHotbar)
            + (mode == 0 ? 0 : mode == 1 ? simple.verticalPositionOffset : overlay.verticalPositionOffset);
        overlayVisibility = mode == 0 || (mode == 1 ? simple.enableVisibility : overlay.enableVisibility);
        var stamina = Config.hudpositions.ADVANCED_HUD_POSITIONS_CONFIG.STAMINA_POSITION_CONFIG;
        xOffsetStamina = local.luke.power.ui.NativeHudLayout.xOffset(scaledWidth, 5, mode,
            (mode == 1 ? simple.horizontalPosition : stamina.horizontalPosition).ordinal(), 0)
            + (mode == 0 ? 0 : mode == 1 ? simple.horizontalPositionOffset : stamina.horizontalPositionOffset);
        yOffsetStamina = local.luke.power.ui.NativeHudLayout.yOffset(scaledHeight, 5, mode,
            (mode == 1 ? simple.verticalPosition : stamina.verticalPosition).ordinal(),
            Config.hudpositions.putStatusBarIconsBelowHotbar, Config.hudpositions.putOverlayMessagesBelowHotbar)
            + (mode == 0 ? 0 : mode == 1 ? simple.verticalPositionOffset : stamina.verticalPositionOffset);
        staminaVisibility = mode == 0 || (mode == 1 ? simple.enableVisibility : stamina.enableVisibility);
        isHotbarRaised = mode != 0 && (hotbarVertical != 0 || yOffsetHotbar < 0);

        local.luke.power.ui.HudElements.heartsX = xOffsetHearts;
        local.luke.power.ui.HudElements.heartsY = yOffsetHearts;
        local.luke.power.ui.HudElements.heartsVisible = heartsVisibility;
        if (Config.config.enableChatScroll) {
            int chatRangeTop = 20;

            if (this.minecraft.currentScreen instanceof ChatScreen) {
                int currentWheelDegrees = Mouse.getDWheel();
                numberOfTurns = Math.round((float)currentWheelDegrees / 120.0f);
                chatOffset = chatOffset + numberOfTurns;

                if (chatOffset < 0) {
                    chatOffset = 0;
                }

                if (chatRangeTop < this.messages.size()) {
                    if (this.messages.size() <= (chatRangeTop + chatOffset)) {
                        chatOffset = this.messages.size() - chatRangeTop;
                    }
                } else {
                    chatOffset = 0;
                }
            } else {
                chatOffset = 0;
            }
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;isFancyGraphicsEnabled()Z"
            )
    )
    private boolean powerHud_renderVignette(Operation<Boolean> original) {
        if (Config.config.disableVignette) {
            return false;
        } else {
            return original.call();
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 0
            )
    )
    private void powerHud_renderHotbar(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (hotbarVisibility) {
            x += xOffsetHotbar;
            y += yOffsetHotbar;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 1
            )
    )
    private void powerHud_renderSelectedItemBorder(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (hotbarVisibility) {
            x += xOffsetHotbar;
            y += yOffsetHotbar;

            if (isHotbarRaised) {
                height += 2;
            }

            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 3
            )
    )
    private void powerHud_renderArmor1(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (armorVisibility) {
            x += xOffsetArmor;
            y += yOffsetArmor;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 4
            )
    )
    private void powerHud_renderArmor2(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (armorVisibility) {
            x += xOffsetArmor;
            y += yOffsetArmor;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 5
            )
    )
    private void powerHud_renderArmor3(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (armorVisibility) {
            x += xOffsetArmor;
            y += yOffsetArmor;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 6
            )
    )
    private void powerHud_renderHearts1(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (heartsVisibility) {
            x += xOffsetHearts;
            y += yOffsetHearts;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 7
            )
    )
    private void powerHud_renderHearts2(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (heartsVisibility) {
            x += xOffsetHearts;
            y += yOffsetHearts;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 8
            )
    )
    private void powerHud_renderHearts3(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (heartsVisibility) {
            x += xOffsetHearts;
            y += yOffsetHearts;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 9
            )
    )
    private void powerHud_renderHearts4(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (heartsVisibility) {
            x += xOffsetHearts;
            y += yOffsetHearts;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 10
            )
    )
    private void powerHud_renderHearts5(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (heartsVisibility) {
            x += xOffsetHearts;
            y += yOffsetHearts;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 11
            )
    )
    private void powerHud_renderOxygen1(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (oxygenVisibility) {
            x += xOffsetOxygen;
            y += yOffsetOxygen;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 12
            )
    )
    private void powerHud_renderOxygen2(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (oxygenVisibility) {
            x += xOffsetOxygen;
            y += yOffsetOxygen;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @ModifyConstant(
            method = "render",
            constant = @Constant(intValue = 200)
    )
    private int powerHud_renderChatFadeTime(int value) {
        return (Config.config.chatFadeTime * 2);
    }

    @ModifyConstant(
            method = "render",
            constant = @Constant(doubleValue = 200.0)
    )
    private double powerHud_renderChatFadeTimeDivisor(double value) {
        return (Config.config.chatFadeTime * 2);
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 2
            )
    )
    private void powerHud_renderCursor(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) float delta) {
        if (!Config.config.disableCrosshair) {
            if (local.luke.power.ui.InterfaceState.debugAxes && minecraft.options.debugHud && !minecraft.options.thirdPerson) {
                var camera = minecraft.camera;
                float yaw = local.luke.power.input.DetachedCamera.isActive()
                    ? local.luke.power.input.DetachedCamera.facingYaw() : camera.prevYaw + (camera.yaw - camera.prevYaw) * delta;
                float pitch = local.luke.power.input.DetachedCamera.isActive()
                    ? (float)local.luke.power.input.DetachedCamera.pitch() : camera.prevPitch + (camera.pitch - camera.prevPitch) * delta;
                local.luke.power.ui.DebugAxes.draw(scaledWidth / 2, scaledHeight / 2,
                    local.luke.power.input.CameraView.yaw(yaw + 180) - 180,
                    local.luke.power.input.CameraView.pitch(pitch), (double)minecraft.displayWidth / scaledWidth);
            } else original.call(instance, x, y, u, v, width, height);
        } else {
            original.call(instance, 0, 0, 0, 0, 0, 0);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V",
                    ordinal = 2
            )
    )
    public void powerHud_renderDrawCoordinate_X(InGameHud instance, TextRenderer textRenderer, String s, int i, int j, int k, Operation<Void> original) {
        if (CoordinateDisplayEnum.HIDE == Config.config.coordinateDisplay) {
            original.call(instance, textRenderer, "", i, j, k);
        } else if (CoordinateDisplayEnum.RANDOMIZE == Config.config.coordinateDisplay) {
            double randomVal = random.nextDouble(-10000, 10000);
            original.call(instance, textRenderer, "x: " + randomVal, i, j, k);
        } else {
            original.call(instance, textRenderer, s, i, j, k);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V",
                    ordinal = 3
            )
    )
    public void powerHud_renderDrawCoordinate_Y(InGameHud instance, TextRenderer textRenderer, String s, int i, int j, int k, Operation<Void> original) {
        if (CoordinateDisplayEnum.HIDE == Config.config.coordinateDisplay) {
            original.call(instance, textRenderer, "", i, j, k);
        } else if (CoordinateDisplayEnum.RANDOMIZE == Config.config.coordinateDisplay) {
            double randomVal = random.nextDouble(-10000, 10000);
            original.call(instance, textRenderer, "y: " + randomVal, i, j, k);
        } else {
            original.call(instance, textRenderer, s, i, j, k);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V",
                    ordinal = 4
            )
    )
    public void powerHud_renderDrawCoordinate_Z(InGameHud instance, TextRenderer textRenderer, String s, int i, int j, int k, Operation<Void> original) {
        if (CoordinateDisplayEnum.HIDE == Config.config.coordinateDisplay) {
            original.call(instance, textRenderer, "", i, j, k);
        } else if (CoordinateDisplayEnum.RANDOMIZE == Config.config.coordinateDisplay) {
            double randomVal = random.nextDouble(-10000, 10000);
            original.call(instance, textRenderer, "z: " + randomVal, i, j, k);
        } else {
            original.call(instance, textRenderer, s, i, j, k);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(IIIF)V"
            )
    )
    public void powerHud_renderHotbarItem(InGameHud instance, int slot, int x, int y, float f, Operation<Void> original) {
        if (hotbarVisibility) {
            x += xOffsetHotbar;
            y += yOffsetHotbar;
            original.call(instance, slot, x, y, f);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/platform/Lighting;turnOff()V"
            )
    )
    public void powerHud_glClearAndItemSelectedTooltip(Operation<Void> original) {
        original.call();

        if (Config.config.enableHotbarBlockRenderingFix) {
            GL11.glClear(256);
        }

        if (Config.config.enableHotbarItemSelectionTooltips) {
            PlayerInventory playerInventory = this.minecraft.player.inventory;
            if (prevSelectedSlot != playerInventory.selectedSlot) {
                prevSelectedSlot = playerInventory.selectedSlot;
                ItemStack selectedItemStack = playerInventory.getSelectedItem();
                if (null != selectedItemStack) {
                    TranslationStorage translationStorage = TranslationStorage.getInstance();
                    this.overlayMessage = translationStorage.get(selectedItemStack.getTranslationKey() + ".name");
                    this.overlayRemaining = Config.config.hotbarItemSelectionFadeTime;
                }
            }
        }
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V", ordinal = 0),
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;overlayRemaining:I")))
    public void powerHud_overlayMesssagePosition(float x, float y, float z, Operation<Void> original) {
        x += xOffsetOverlay;
        y += yOffsetOverlay;
        original.call(x, y, z);
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;III)V"
            )
    )
    public void powerHud_overlayMesssagePosition(TextRenderer instance, String text, int x, int y, int color, Operation<Void> original) {
        if (overlayVisibility) {
            original.call(instance, text, x, y, color);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;get(I)Ljava/lang/Object;"
            )
    )
    public Object powerHud_renderChatOffset(List instance, int i, Operation<Void> original) {
        if (Config.config.enableChatScroll) {
            return original.call(instance, i + chatOffset);
        } else {
            return original.call(instance, i);
        }
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/font/TextRenderer;drawWithShadow(Ljava/lang/String;III)V"),
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;messages:Ljava/util/List;")))
    private void powerHud_selectChat(TextRenderer renderer, String text, int x, int y, int color, Operation<Void> original) {
        var selection = local.luke.power.chat.ChatSelection.history;
        int row = selection.line(text, renderer, x, scaledHeight - 48 + y, renderer::getWidth);
        if (row >= 0) {
            int start = selection.start(row), end = selection.end(row);
            if (start >= 0 && end > start) fill(selection.x(row, start), y - 1, selection.x(row, end), y + 8, 0xFF316AC5);
        }
        original.call(renderer, text, x, y, color);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void powerHud_finishChatSelection(float delta, boolean open, int x, int y, CallbackInfo ci) {
        local.luke.power.chat.ChatSelection.history.end();
    }

    @ModifyConstant(
            method = "addChatMessage",
            constant = @Constant(intValue = 50)
    )
    public int chatLog_addChatMessageLimit(int value) {
        return Integer.MAX_VALUE; // The shared retention boundary trims each message category after native wrapping.
    }

    @Inject(method = "render", at = @At("RETURN"), cancellable = true)
    public void powerHud_renderXboxButtons(float f, boolean bl, int i, int j, CallbackInfo ci) {
        if (Config.config.drawXboxXAndYButtons) {
            ScreenScaler var5 = new ScreenScaler(this.minecraft.options, this.minecraft.displayWidth, this.minecraft.displayHeight);
            //int var6 = var5.method_1857();
            int var7 = var5.getScaledHeight();
            GL11.glBindTexture(3553 /* GL_TEXTURE_2D */, minecraft.textureManager.getTextureId("/assets/power_hud/button_icons.png"));
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexture(10, var7 - 30, (2 % 12) * 20, (2 / 12) * 20, 20, 20);
            TextRenderer var8 = this.minecraft.textRenderer;
            var8.drawWithShadow("Crafting", 31, var7 - 24, 16777215);
            GL11.glBindTexture(3553 /* GL_TEXTURE_2D */, minecraft.textureManager.getTextureId("/assets/power_hud/button_icons.png"));
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexture(78, var7 - 30, (3 % 12) * 20, (3 / 12) * 20, 20, 20);
            var8.drawWithShadow("Inventory", 99, var7 - 24, 16777215);
        }
    }
}
