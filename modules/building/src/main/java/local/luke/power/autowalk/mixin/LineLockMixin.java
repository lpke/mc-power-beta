package local.luke.power.autowalk.mixin;

import local.luke.power.autowalk.AutoWalk;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class LineLockMixin {
  @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 0)
  private double power$laneX(double delta) {
    Entity entity = (Entity)(Object)this;
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    return mc.player == entity ? AutoWalk.lockX(entity.x, (entity.boundingBox.maxX - entity.boundingBox.minX) / 2, delta) : delta;
  }
  @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 2)
  private double power$laneZ(double delta) {
    Entity entity = (Entity)(Object)this;
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    return mc.player == entity ? AutoWalk.lockZ(entity.z, (entity.boundingBox.maxZ - entity.boundingBox.minZ) / 2, delta) : delta;
  }
}
