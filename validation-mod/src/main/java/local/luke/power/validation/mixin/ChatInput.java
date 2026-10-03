package local.luke.power.validation.mixin;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(ChatScreen.class)
public interface ChatInput {
 @Accessor("text") String power$text();
 @Accessor("text") void power$text(String text);
 @Invoker("keyPressed") void power$type(char character, int code);
}
