package local.luke.power.mixin;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ChatScreen.class)
public interface ChatTextAccessor {
  @Accessor("text") void power$text(String value);
}
