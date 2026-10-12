package local.luke.power.mixin;

import local.luke.power.gameplay.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public class ItemMergingMixin {
  @Unique private int power$mergeTick;
  @Inject(method = "method_227", at = @At("HEAD"))
  private void power$merge(CallbackInfo ci) {
    if (GameplayConfig.current().mergeItems && ++power$mergeTick % 10 == 0)
      ItemMerging.merge((World)(Object)this,
          (Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance());
  }
}
