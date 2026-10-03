package io.github.yunivers.appleslices.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.yunivers.appleslices.AppleSlices;
import java.util.Random;
import net.minecraft.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.class_554;
import net.minecraft.class_564;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={InGameHud.class})
public abstract class InGameHudMixin
extends DrawableHelper {
    @Shadow
    private Minecraft minecraft;
    @Shadow
    private Random random;
    @Shadow
    private int ticks;

    @Inject(method={"render"}, at={@At(value="INVOKE", target="Lnet/minecraft/entity/player/ClientPlayerEntity;isInFluid(Lnet/minecraft/block/Material;)Z")})
    public void renderAppleSlices(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci, @Local class_564 scaler) {
        Item class_1242;
        ClientPlayerEntity player = this.minecraft.player;
        ItemStack heldItem = player.inventory.getSelectedItem();
        if (heldItem == null || !((class_1242 = heldItem.getItem()) instanceof class_554)) {
            return;
        }
        class_554 food = (class_554)class_1242;
        int healAmount = food.method_1835();
        int screenWidth = scaler.method_1857();
        int screenHeight = scaler.method_1858();
        int hearts = player.health;
        int healedHearts = hearts + healAmount;
        this.random.setSeed((long)this.ticks * 312871L);
        GL11.glEnable((int)3042);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glColor4d((double)1.0, (double)1.0, (double)1.0, (double)AppleSlices.pulseValue);
        for (int heart = 0; heart < 10; ++heart) {
            int drawX = screenWidth / 2 - 91 + heart * 8;
            int drawY = screenHeight - 32;
            if (hearts <= 4) {
                drawY += this.random.nextInt(2);
            }
            if (heart * 2 + 1 < hearts) continue;
            if (heart * 2 + 1 < healedHearts) {
                this.drawTexture(drawX, drawY, 52, 0, 9, 9);
            }
            if (heart * 2 + 1 != healedHearts) continue;
            this.drawTexture(drawX, drawY, 61, 0, 9, 9);
        }
        GL11.glDisable((int)3042);
        GL11.glBlendFunc((int)775, (int)769);
        GL11.glColor4d((double)1.0, (double)1.0, (double)1.0, (double)1.0);
    }
}

