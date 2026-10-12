package local.luke.power.mixin;

import local.luke.power.gameplay.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public class ItemMergingMixin implements ItemMerging.Access {
  @Unique private final ItemMerging.Sweep power$sweep = new ItemMerging.Sweep();
  @Override public ItemMerging.Sweep power$itemSweep() { return power$sweep; }
  @Inject(method = "method_227", at = @At("HEAD"))
  private void power$merge(CallbackInfo ci) {
    power$sweep.tick((World)(Object)this,
        (Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance());
  }
  @Inject(method = "method_226", at = @At("HEAD"))
  private void power$remove(Entity entity, CallbackInfo ci) { power$sweep.removed(entity); }
}
