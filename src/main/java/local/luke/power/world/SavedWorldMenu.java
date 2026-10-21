package local.luke.power.world;

import java.util.Objects;
import local.luke.power.ui.*;
import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.modificationstation.stationapi.api.client.event.gui.screen.EditWorldScreenEvent;
import net.modificationstation.stationapi.api.client.gui.widget.ButtonWidgetDetachedContext;
import net.modificationstation.stationapi.api.client.gui.widget.ButtonWidgetAttachedContext;

/** Add to StationAPI's actual world Edit menu using its scrollable button registry. */
public final class SavedWorldMenu {
  private enum Tool { BACKUP, DUPLICATE, RECREATE }
  private static boolean enabled(Tool tool) {
    var settings = WorldToolsConfig.current();
    return switch (tool) { case BACKUP -> settings.backup; case DUPLICATE -> settings.duplicate; case RECREATE -> settings.recreate; };
  }
  private static final class ToolButton extends ButtonWidget {
    final Tool tool;
    ToolButton(int id, String label, Tool tool) { super(id, 0, 0, label); this.tool = tool; }
  }
  /** StationAPI registers factories once; each native init gets a new compact list. */
  public static void filter(java.util.List<ButtonWidgetAttachedContext> buttons) {
    buttons.removeIf(entry -> entry.button() instanceof ToolButton tool && !enabled(tool.tool));
  }
  private static final class NativeMenu {
    static final java.lang.reflect.Field BUTTONS = buttons();
    private static java.lang.reflect.Field buttons() {
      try { var field = net.modificationstation.stationapi.api.client.gui.screen.EditWorldScreen.class.getDeclaredField("buttonMenu"); field.setAccessible(true); return field; }
      catch (ReflectiveOperationException missing) { throw new IllegalStateException("The saved-world button list is unavailable", missing); }
    }
  }
  public static void filterScreen(net.modificationstation.stationapi.api.client.gui.screen.EditWorldScreen screen) {
    try { filter(((WorldEditButtons)NativeMenu.BUTTONS.get(screen)).power$editButtons()); }
    catch (IllegalAccessException inaccessible) { throw new IllegalStateException("The saved-world button list could not be read", inaccessible); }
  }
  @EventListener public void buttons(EditWorldScreenEvent.ScrollableButtonContextRegister event) {
    event.contexts.add(screen -> new ButtonWidgetDetachedContext(
        id -> new ButtonWidget(id, 0, 0, "Edit game rules"),
        button -> {
          Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
          try { PowerOptionsScreen.openSavedWorldRules(mc, screen, screen.worldData.method_1956()); }
          catch (Exception e) {
            mc.setScreen(new DataProblemScreen(Objects.toString(e.getMessage(), "World metadata could not be read."), false));
          }
        }));
    event.contexts.add(screen -> new ButtonWidgetDetachedContext(
        id -> new ToolButton(id, "Back up world", Tool.BACKUP), button -> {
          if (!enabled(Tool.BACKUP)) return;
          Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
          mc.setScreen(new WorldOperationScreen(screen, screen.worldData.method_1956(), screen.worldData.method_1958(), WorldSnapshots.Action.BACKUP));
        }));
    event.contexts.add(screen -> new ButtonWidgetDetachedContext(
        id -> new ToolButton(id, "Duplicate world", Tool.DUPLICATE), button -> {
          if (!enabled(Tool.DUPLICATE)) return;
          Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
          mc.setScreen(new WorldOperationScreen(screen, screen.worldData.method_1956(), screen.worldData.method_1958(), WorldSnapshots.Action.DUPLICATE));
        }));
    event.contexts.add(screen -> new ButtonWidgetDetachedContext(
        id -> new ToolButton(id, "Re-create from seed", Tool.RECREATE), button -> {
          if (!enabled(Tool.RECREATE)) return;
          Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
          try {
            var draft = WorldRecreation.read(FabricLoader.getInstance().getGameDir(), screen.worldData.method_1956(), () -> mc.world == null);
            var creation = new net.minecraft.class_180(screen);
            WorldRecreation.prepare(creation, draft); mc.setScreen(creation);
          } catch (Exception failure) {
            mc.setScreen(new DataProblemScreen(Objects.toString(failure.getMessage(), "World metadata could not be read."), false));
          }
        }));
  }
}
