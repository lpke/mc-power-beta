package local.luke.power.worldedit.mixin;

import local.luke.power.worldedit.config.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldProperties.class)
public abstract class WorldPropertiesMixin implements WorldSettings {
  @Unique private WorldOverride worldedit$access = WorldOverride.INHERIT;

  @Override
  public WorldOverride worldedit$override() {
    return worldedit$access;
  }

  @Override
  public void worldedit$override(WorldOverride value) {
    worldedit$access = value;
  }

  @Inject(method = "<init>(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("RETURN"))
  private void worldedit$read(NbtCompound tag, CallbackInfo ci) {
    worldedit$access = WorldOverride.decode(tag.getInt("PowerBetaWorldEditAccess"));
  }

  @Inject(method = "<init>(Lnet/minecraft/world/WorldProperties;)V", at = @At("RETURN"))
  private void worldedit$copy(WorldProperties other, CallbackInfo ci) {
    worldedit$access = ((WorldSettings) other).worldedit$override();
  }

  @Inject(method = "updateProperties", at = @At("RETURN"))
  private void worldedit$write(NbtCompound tag, NbtCompound player, CallbackInfo ci) {
    tag.putInt("PowerBetaWorldEditAccess", worldedit$access.ordinal());
  }
}
