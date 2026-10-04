package local.luke.power.ui;

import com.google.gson.JsonPrimitive;
import local.luke.power.config.Setting;
import net.minecraft.client.gui.screen.pack.PackScreen;
import org.lwjgl.input.Keyboard;

/** Keep the native picker while preserving the parent menu's draft and return position. */
public final class TexturePackScreen extends PackScreen {
  private final PowerOptionsScreen parent;
  private final Setting setting;

  public TexturePackScreen(PowerOptionsScreen parent, Setting setting) {
    super(parent);
    this.parent = parent;
    this.setting = setting;
  }

  @Override
  public void removed() {
    setting.value = new JsonPrimitive(minecraft.field_2768.field_1175.field_1137);
    parent.changed(setting);
    super.removed();
  }

  @Override
  protected void keyPressed(char character, int key) {
    if (key == Keyboard.KEY_ESCAPE) minecraft.setScreen(parent);
    else super.keyPressed(character, key);
  }
}
