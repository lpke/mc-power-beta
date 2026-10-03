package local.luke.power.worldedit.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import local.luke.power.worldedit.WorldEditor;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = Minecraft.class, priority = 1200)
public abstract class MinecraftMixin {
  @Inject(method = "tick", at = @At("TAIL"))
  private void worldedit$tick(CallbackInfo ci) {
    local.luke.power.worldedit.carry.ContainerCarry.tick((Minecraft)(Object)this);
    WorldEditor.tick((Minecraft) (Object) this);
  }

  @Inject(method = "method_2115", at = @At("HEAD"))
  private void worldedit$leave(World world, String text, PlayerEntity player, CallbackInfo ci) {
    WorldEditor.leaveWorld();
  }

  @Inject(method = "method_2107", at = @At("HEAD"), cancellable = true)
  private void worldedit$click(int button, CallbackInfo ci) {
    if (local.luke.power.worldedit.carry.ContainerCarry.click((Minecraft)(Object)this, button)) { ci.cancel(); return; }
    if (WorldEditor.click((Minecraft) (Object) this, button)) ci.cancel();
  }

  @WrapWithCondition(
      method = "method_2110",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/InteractionManager;method_1721(IIII)V"))
  private boolean worldedit$hold(InteractionManager manager, int x, int y, int z, int face) {
    return !local.luke.power.worldedit.carry.ContainerCarry.carrying() && !WorldEditor.holdingWand((Minecraft) (Object) this);
  }
}
