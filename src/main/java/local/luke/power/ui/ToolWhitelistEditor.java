package local.luke.power.ui;

import com.google.gson.JsonArray;
import java.util.*;
import local.luke.power.config.Setting;
import local.luke.power.inventory.tools.ToolWhitelist;
import net.minecraft.client.gui.screen.Screen;

/** The shared picker edits a temporary selection until Save. */
public final class ToolWhitelistEditor {
  private ToolWhitelistEditor() {}
  public static Screen open(Screen parent, Setting setting, Runnable changed) {
    List<String> stored = new ArrayList<>();
    for (var value : setting.value.getAsJsonArray()) stored.add(value.getAsString());
    var registered = ToolWhitelist.registered();
    var selected = stored.contains("*") ? registered : stored;
    return new ItemPickerScreen(parent, "Allowed tools", ItemCatalogue.Domain.ITEM, selected, true,
        new ItemPickerScreen.Options(false, entry -> ToolWhitelist.tool(entry.icon().getItem())), values -> {
          JsonArray next = new JsonArray();
          if (!registered.isEmpty() && new HashSet<>(values).equals(new HashSet<>(registered))) next.add("*");
          else values.forEach(next::add);
          setting.value = next; changed.run();
        });
  }
}
