package local.luke.power.validation.mixin;

import java.util.List;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InGameHud.class)
public interface ChatLinesAccess {
  @Accessor("messages") List<ChatHudLine> power$messages();
}
