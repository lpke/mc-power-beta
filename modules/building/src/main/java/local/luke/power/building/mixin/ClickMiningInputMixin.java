package local.luke.power.building.mixin;

import local.luke.power.building.config.Config;
import local.luke.power.input.ClickMining;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Remove only the native empty-swing penalty, where it originates. */
@Mixin(Minecraft.class)
public abstract class ClickMiningInputMixin {
  @ModifyConstant(method = "method_2107", constant = @Constant(intValue = 10))
  private int power$airGapDelay(int vanilla) {
    Minecraft mc = (Minecraft)(Object)this;
    return mc.world != null && !mc.world.isRemote && mc.currentScreen == null
        && ClickMining.enabled(Config.current().clickMining) ? 0 : vanilla;
  }
}
