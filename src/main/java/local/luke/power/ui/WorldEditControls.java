package local.luke.power.ui;

import com.google.gson.JsonPrimitive;
import java.util.*;
import local.luke.power.config.Setting;
import local.luke.power.world.SavedWorldRules;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_629;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.modificationstation.stationapi.api.client.gui.screen.EditWorldScreen;
import net.modificationstation.stationapi.api.client.gui.widget.ButtonWidgetAttachedContext;

/** Compose the shared inline editor into StationAPI's existing scrollable world menu. */
public final class WorldEditControls extends UiScreen {
  private final InlineValueEditor inline = new InlineValueEditor();
  private List<ButtonWidgetAttachedContext> prepared;
  private ButtonWidget rename;
  private boolean ownsNativePress;
  private String name, error = "";
  public boolean active() { return inline.active(); }
  public boolean ownsPress() { return active() || ownsNativePress; }
  public String title(EditWorldScreen screen) {
    if (name == null) name = screen.worldData.method_1958();
    return "Edit " + (name == null || name.isBlank() ? screen.worldData.method_1956() : name);
  }
  public void prepare(EditWorldScreen screen, WorldEditButtons list) {
    list.power$renameOwner(this);
    var entries = list.power$editButtons();
    if (prepared == entries) return;
    prepared = entries;
    for (int i = 0; i < entries.size(); i++) {
      var entry = entries.get(i);
      if (entry.button().text.equals(class_629.method_2049("selectWorld.rename"))) {
        rename = entry.button();
        entries.set(i, new ButtonWidgetAttachedContext(rename, button -> begin(screen)));
      }
    }
    var convert = entries.stream().filter(e -> e.button().text.toLowerCase(Locale.ROOT).contains("mcregion")).findFirst().orElse(null);
    if (convert != null) {
      entries.remove(convert);
      int rules = -1;
      for (int i = 0; i < entries.size(); i++) if (entries.get(i).button().text.equals("Edit game rules")) rules = i;
      entries.add(rules < 0 ? entries.size() : rules + 1, convert);
    }
  }
  private void begin(EditWorldScreen screen) {
    try {
      SavedWorldRules world = new SavedWorldRules(FabricLoader.getInstance().getGameDir(), screen.worldData.method_1956(), () -> minecraft.world == null);
      Setting value = new Setting("worldEditor.name", "", "", "", "World name", "", Setting.Kind.TEXT,
          new JsonPrimitive(world.name()), new JsonPrimitive(world.name()), 0, 0, 1, List.of(), false);
      value.validator(v -> { if (v.getAsString().isBlank() || v.getAsString().strip().length() > 128
          || v.getAsString().chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Use a world name of 1 to 128 characters."); });
      inline.begin(value, value, () -> {
        try { world.rename(value.value.getAsString()); name = world.name(); error = ""; }
        catch (Exception e) { error = Objects.toString(e.getMessage(), "Could not rename the world. Original metadata preserved."); }
      }, rename.x, rename.y, 200, 32, height - 64);
      ownsNativePress = true;
      error = "";
    } catch (Exception e) { error = Objects.toString(e.getMessage(), "Could not read world metadata."); }
  }
  public boolean press(int x, int y, int button) {
    boolean consumed = inline.press(x, y, button);
    if (consumed && button == 0) ownsNativePress = true;
    return consumed;
  }
  public void release(int button) { if (button == 0) ownsNativePress = false; }
  public boolean key(char c, int key) { return inline.key(c, key); }
  public void cancel() { inline.cancel(); ownsNativePress = false; TextDrag.cancel(); }
  public void render(int x, int y, float delta) {
    inline.beginFrame();
    if (inline.active() && rename != null) inline.render(this, rename.x, rename.y, 200, x, y, 32, height - 64);
    inline.endFrame(); inline.renderError(this);
    if (!error.isEmpty()) centeredText(fit(error, width - 24), height - 72, 0xff8888);
  }
}
