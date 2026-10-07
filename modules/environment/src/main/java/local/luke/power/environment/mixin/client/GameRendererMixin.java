package local.luke.power.environment.mixin.client;

import local.luke.power.environment.Config;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow private Minecraft client;

    @Unique private int currentX = 0;
    @Unique private int currentY = 0;

    @Inject(
            method = "renderSnow",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/Biome;canSnow()Z",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void powerEnvironment_getLocationSnow(float par1, CallbackInfo ci, float var2, LivingEntity var3, World var4, int var5, int var6, int var7, Tessellator var8, double var9, double var11, double var13, int var15, int var16, Biome[] var17, int var18, int var19, int var20, Biome var21)
    {
        currentX = var19;
        currentY = var20;
    }

    @WrapOperation(
            method = "renderSnow",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/Biome;canSnow()Z"
            )
    )
    private boolean powerEnvironment_renderSnow(Biome instance, Operation<Boolean> original) {
        boolean allowSnow = original.call(instance);

        if (Config.config.WEATHER_CONFIG.enableAlwaysSnowAboveSetYLevel) {
            if (Config.config.WEATHER_CONFIG.alwaysSnowAboveThisYLevel < this.client.world.getTopSolidBlockY(currentX, currentY)) {
                allowSnow = true;
            }
        }

        return allowSnow;
    }

    @Inject(
            method = "renderSnow",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/Biome;canRain()Z",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void powerEnvironment_getLocationRain(float par1, CallbackInfo ci, float var2, LivingEntity var3, World var4, int var5, int var6, int var7, Tessellator var8, double var9, double var11, double var13, int var15, int var16, Biome[] var17, int var18, int var19, int var20, Biome var21)
    {
        currentX = var19;
        currentY = var20;
    }

    @WrapOperation(
            method = "renderSnow",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/Biome;canRain()Z"
            )
    )
    private boolean powerEnvironment_renderRain(Biome instance, Operation<Boolean> original) {
        boolean allowRain = original.call(instance);

        if (Config.config.WEATHER_CONFIG.enableAlwaysSnowAboveSetYLevel) {
            if (Config.config.WEATHER_CONFIG.alwaysSnowAboveThisYLevel < this.client.world.getTopSolidBlockY(currentX, currentY)) {
                allowRain = false;
            }
        }

        return allowRain;
    }

    @Unique private int highestBlockYLocation = 0;

    @WrapOperation(
            method = "renderRain",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getTopSolidBlockY(II)I"
            )
    )
    private int powerEnvironment_tick(World instance, int x, int z, Operation<Integer> original) {
        highestBlockYLocation = original.call(instance, x, z);
        return highestBlockYLocation;
    }

    @WrapOperation(
            method = "renderRain",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/Biome;canRain()Z"
            )
    )
    private boolean powerEnvironment_tick(Biome instance, Operation<Boolean> original) {
        boolean allowRain = original.call(instance);

        if (Config.config.WEATHER_CONFIG.enableAlwaysSnowAboveSetYLevel) {
            if (Config.config.WEATHER_CONFIG.alwaysSnowAboveThisYLevel < highestBlockYLocation) {
                allowRain = false;
            }
        }

        return allowRain;
    }
}
