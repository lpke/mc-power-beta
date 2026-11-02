package local.luke.power.validation.mixin;

import local.luke.power.validation.TerrainDirty470Checks;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class TerrainDirty470WorldCapture {
  @Inject(method = "method_1537", at = @At("HEAD"))
  private void grid(CallbackInfo ci) { TerrainDirty470Checks.refresh("grid-reload"); }
  @Inject(method = "method_1148", at = @At("HEAD"))
  private void daylight(CallbackInfo ci) { TerrainDirty470Checks.refresh("daylight"); }
  @Inject(method = "method_1149", at = @At("HEAD"))
  private void point(int x, int y, int z, CallbackInfo ci) { TerrainDirty470Checks.refresh("block-point"); }
  @Inject(method = "method_1150", at = @At("HEAD"))
  private void area(int x, int y, int z, int maxX, int maxY, int maxZ, CallbackInfo ci) { TerrainDirty470Checks.refresh("block-area"); }
}
