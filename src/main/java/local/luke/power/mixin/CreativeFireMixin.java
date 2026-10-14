package local.luke.power.mixin;

import local.luke.power.input.PlayerMode;
import net.minecraft.SingleplayerInteractionManager;
import net.minecraft.block.Block;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingleplayerInteractionManager.class)
public abstract class CreativeFireMixin extends InteractionManager {
  @Shadow private int field_2187;

  protected CreativeFireMixin(Minecraft mc) { super(mc); }

  @Inject(method = {"method_1707", "method_1721"}, at = @At("HEAD"), cancellable = true)
  private void power$extinguish(int x, int y, int z, int face, CallbackInfo ci) {
    if (minecraft.world == null || minecraft.world.isRemote || face < 0 || face > 5
        || !(minecraft.player instanceof PlayerMode mode) || !mode.power$creative()) return;
    int fx = x + (face == 5 ? 1 : face == 4 ? -1 : 0);
    int fy = y + (face == 1 ? 1 : face == 0 ? -1 : 0);
    int fz = z + (face == 3 ? 1 : face == 2 ? -1 : 0);
    if (minecraft.world.getBlockId(fx, fy, fz) != Block.field_1892.id) return;
    minecraft.world.method_172(minecraft.player, x, y, z, face);
    method_1705();
    field_2187 = 5;
    ci.cancel();
  }
}
