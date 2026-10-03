package local.luke.power.flexible.mixin;

import local.luke.power.flexible.FlexiblePlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
  @Inject(method = "tick", at = @At("HEAD"))
  private void input(CallbackInfo ci) {
    FlexiblePlacement.tick((Minecraft) (Object) this);
  }

  @Inject(method = "setScreen", at = @At("HEAD"))
  private void screen(Screen screen, CallbackInfo ci) {
    if (screen != null) FlexiblePlacement.interrupt();
  }

  @Inject(method = "method_2115", at = @At("HEAD"))
  private void world(World world, String message, PlayerEntity player, CallbackInfo ci) {
    FlexiblePlacement.interrupt();
  }
}
