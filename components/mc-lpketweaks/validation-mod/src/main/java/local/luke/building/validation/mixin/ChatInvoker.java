package local.luke.building.validation.mixin;

import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;

@Mixin(ChatScreen.class)
public interface ChatInvoker {
  @Invoker("keyPressed")
  void revision$key(char c, int key);

  @Accessor("text")
  void revision$text(String value);

  @Accessor("text")
  String revision$text();
}
