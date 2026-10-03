package local.luke.creative.mixin;

import local.luke.creative.command.CreativeCommands;
import net.minecraft.entity.living.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = AbstractClientPlayer.class, priority = 1200)
public abstract class CommandMixin {
  @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
  private void lpke$command(String text, CallbackInfo ci) {
    if (CreativeCommands.execute(text)) ci.cancel();
  }
}
