package local.luke.power.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.modificationstation.stationapi.api.client.gui.screen.EditWorldScreen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.*;

/** Match Done when leaving StationAPI's Edit menu with Escape. */
@Mixin(value = EditWorldScreen.class, remap = false)
public abstract class EditWorldEscapeMixin extends Screen {
  @Shadow @Final protected Screen parent;

  @Override
  protected void keyPressed(char character, int key) {
    if (key == Keyboard.KEY_ESCAPE) minecraft.setScreen(parent);
    else super.keyPressed(character, key);
  }
}
