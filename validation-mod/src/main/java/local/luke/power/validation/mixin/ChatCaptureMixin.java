package local.luke.power.validation.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.ChatChecks;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(InGameHud.class)
public class ChatCaptureMixin {
  @Unique private int power$chatDepth;
  @WrapMethod(method="addChatMessage")
  private void capture(String text, Operation<Void> original) {
    if (power$chatDepth++ == 0) ChatChecks.capture(text);
    try { original.call(text); } finally { power$chatDepth--; }
  }
}
