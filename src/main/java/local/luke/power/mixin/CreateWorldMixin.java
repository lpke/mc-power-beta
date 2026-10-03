package local.luke.power.mixin;

import net.minecraft.class_180;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(class_180.class)
public class CreateWorldMixin {
  // The native title used height / 4 while the name/seed fields use fixed positions.
  @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_180;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"), index = 3)
  private int power$title(int y) { return 20; }
}
