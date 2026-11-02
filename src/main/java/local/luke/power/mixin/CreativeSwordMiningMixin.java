package local.luke.power.mixin;

import local.luke.power.input.PlayerMode;
import net.minecraft.SingleplayerInteractionManager;
import net.minecraft.class_352;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mining stays main-hand owned; the native client still swings and attacks entities. */
@Mixin(value = SingleplayerInteractionManager.class, priority = 1200)
public abstract class CreativeSwordMiningMixin extends InteractionManager {
  protected CreativeSwordMiningMixin(Minecraft mc) { super(mc); }

  @Unique private boolean power$creativeSword() {
    if (minecraft.world == null || minecraft.world.isRemote
        || !(minecraft.player instanceof PlayerMode mode) || !mode.power$creative()) return false;
    var inventory = minecraft.player.inventory;
    ItemStack held = inventory.main[inventory.selectedSlot];
    return held != null && held.count > 0 && held.getItem() instanceof class_352;
  }

  @Inject(method = {"method_1707", "method_1721"}, at = @At("HEAD"), cancellable = true)
  private void power$skipSwordMining(int x, int y, int z, int face, CallbackInfo ci) {
    if (!power$creativeSword()) return;
    method_1705();
    ci.cancel();
  }

  @Inject(method = "method_1716", at = @At("HEAD"), cancellable = true)
  private void power$skipSwordBreak(int x, int y, int z, int face, CallbackInfoReturnable<Boolean> cir) {
    if (!power$creativeSword()) return;
    method_1705();
    cir.setReturnValue(false);
  }
}
