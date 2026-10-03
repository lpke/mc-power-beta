package local.luke.power.controls.mixin.tweaks.bedtweaks;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.world.NaturalSpawner;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(NaturalSpawner.class)
public class class_567Mixin {
    @Inject(method = "spawnMonstersAndWakePlayers", at = @At("HEAD"), cancellable = true)
    private static void disableSleepSpawning(World arg, List list, CallbackInfoReturnable<Boolean> cir) {
        if (ControlFeatures.TWEAKS_CONFIG.disableSleepSpawning) {
            cir.setReturnValue(false);
        }
    }
}
