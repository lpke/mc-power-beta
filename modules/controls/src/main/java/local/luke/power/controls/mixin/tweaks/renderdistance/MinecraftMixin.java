package local.luke.power.controls.mixin.tweaks.renderdistance;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.util.ModOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Option;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;setInt(Lnet/minecraft/client/option/Option;I)V"))
    public void changeRenderDistance(GameOptions instance, Option option, int value, Operation<Void> original) {
        if (option == Option.RENDER_DISTANCE) {
            ModOptions.cycleRenderDistance();
            Minecraft mc = (Minecraft)(Object)this;
            if (mc.worldRenderer != null) mc.worldRenderer.reload();
            int chunks = ModOptions.getRenderDistanceChunks();
            String name = chunks >= 12 ? "Far" : chunks >= 8 ? "Normal" : chunks >= 4 ? "Short" : "Tiny";
            mc.inGameHud.addChatMessage("§7Render distance: §b" + name + " §7(" + chunks + " chunks)");
            instance.save();
            return;
        }

        original.call(instance, option, value);
    }
}
