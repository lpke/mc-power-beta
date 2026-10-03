package local.luke.power.creative.mixin;

import local.luke.power.creative.command.CreativeCommands;
import net.minecraft.entity.living.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(value = AbstractClientPlayer.class, priority = 1200)
public abstract class CommandMixin {
  @WrapMethod(method = "sendChatMessage")
  private void power$command(String text, Operation<Void> original) {
    if (!CreativeCommands.execute(text)) original.call(text);
  }
}
