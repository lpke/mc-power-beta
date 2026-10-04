package local.luke.power.worldedit;

import local.luke.power.worldedit.chat.ChatFormat;

import java.io.IOException;
import java.lang.reflect.*;
import local.luke.power.worldedit.config.*;
import local.luke.power.worldedit.core.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Item;
import net.minecraft.block.Block;
import net.minecraft.class_212;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.apache.logging.log4j.*;

public final class WorldEditor {
  public static final Logger LOG = LogManager.getLogger("WorldEdit Beta");
  private static Settings settings = load();
  private static World world;
  private static Editor editor;
  private static Field freecam;
  private static Method freecamActive;
  private static boolean brokenFreecam;

  static {
    try {
      freecam = Class.forName("local.luke.power.camera.Freecam").getField("freecamController");
      freecamActive = freecam.getType().getMethod("isActive");
    } catch (ClassNotFoundException ignored) {
    } catch (ReflectiveOperationException e) {
      brokenFreecam = true;
    }
  }

  private static Settings load() {
    try {
      Settings value = local.luke.power.storage.PowerConfig.read("editor", Settings.class);
      ConfigStore.validate(value);
      return value;
    } catch (IllegalArgumentException e) {
      LOG.error("Could not load WorldEdit Beta settings; preserving file", e);
      return new Settings();
    }
  }

  public static Settings settings() {
    return settings.copy();
  }

  public static void apply(Settings next) throws IOException {
    ConfigStore.validate(next);
    if (next.wandItem >= Item.ITEMS.length || Item.ITEMS[next.wandItem] == null)
      throw new IllegalArgumentException("Wand item ID is not registered.");
    local.luke.power.storage.PowerConfig.save("editor", next);
    preview(next);
  }

  public static void preview(Settings next) {
    ConfigStore.validate(next);
    if (next.wandItem >= Item.ITEMS.length || Item.ITEMS[next.wandItem] == null)
      throw new IllegalArgumentException("Wand item ID is not registered.");
    settings = next.copy();
    if (editor != null) {
      editor.limit = settings.blockLimit;
      editor.engine.configure(settings.blockLimit, settings.historySize);
    }
  }

  public static boolean editing() { return editor != null && editor.engine.busy(); }

  public static boolean freecam() {
    if (brokenFreecam) return true;
    if (freecam == null) return false;
    try {
      Object c = freecam.get(null);
      return c != null && Boolean.TRUE.equals(freecamActive.invoke(c));
    } catch (ReflectiveOperationException e) {
      return true;
    }
  }

  public static WorldOverride worldOverride(Minecraft mc) {
    return mc != null && mc.world != null && !mc.world.isRemote
        ? ((WorldSettings) mc.world.method_262()).worldedit$override()
        : WorldOverride.INHERIT;
  }

  public static void worldOverride(Minecraft mc, WorldOverride value) {
    if (mc == null || mc.world == null || mc.world.isRemote)
      throw new IllegalArgumentException("Open a singleplayer world first.");
    ((WorldSettings) mc.world.method_262()).worldedit$override(value);
  }

  public static boolean permitted(Minecraft mc) {
    return AccessPolicy.allows(
        settings.enabled,
        worldOverride(mc),
        settings.creativeOnly,
        CreativeAccess.installed(),
        mc != null && CreativeAccess.creative(mc.player));
  }

  public static boolean available(Minecraft mc) {
    return localPlayer(mc) && permitted(mc);
  }

  private static boolean localPlayer(Minecraft mc) {
    return mc != null
        && mc.world != null
        && !mc.world.isRemote
        && mc.player != null
        && !mc.player.dead
        && mc.player.health > 0
        && mc.player.world == mc.world
        && Double.isFinite(mc.player.x)
        && Double.isFinite(mc.player.y)
        && Double.isFinite(mc.player.z)
        && Float.isFinite(mc.player.yaw)
        && Float.isFinite(mc.player.pitch)
        && mc.field_2807 == mc.player
        && !freecam();
  }

  public static Editor editor(Minecraft mc) {
    if (world != mc.world) {
      leaveWorld();
      world = mc.world;
      BlockParser parser =
          new BlockParser(id -> id < Block.BLOCKS.length && Block.BLOCKS[id] != null);
      editor =
          new Editor(
              new MinecraftWorld(world), parser, s -> mc.inGameHud.addChatMessage(ChatFormat.info(s)));
      editor.limit = settings.blockLimit;
      editor.engine.configure(settings.blockLimit, settings.historySize);
    }
    return editor;
  }

  public static Editor current() {
    return editor;
  }

  public static void tick(Minecraft mc) {
    if (editor != null && world != mc.world) {
      leaveWorld();
      return;
    }
    if (editor != null && !mc.paused) {
      if (!available(mc)) editor.engine.cancel();
      // Rollbacks must progress after a mode change or master toggle.
      editor.engine.tick(settings.blocksPerTick);
    }
  }

  public static void leaveWorld() {
    if (editor != null) editor.engine.finishRollback();
    editor = null;
    world = null;
  }

  public static boolean command(Minecraft mc, String text) {
    if (local.luke.power.input.InteractionState.carryingContainer && local.luke.power.worldedit.chat.CommandCatalog.owns(text)) {
      message(mc,"Place your carried container before using editing commands."); return true;
    }
    if (!local.luke.power.worldedit.chat.CommandCatalog.owns(text)) return false;
    if (!available(mc)) {
      if (mc.inGameHud != null)
        mc.inGameHud.addChatMessage(
            ChatFormat.error(!localPlayer(mc)
                    ? "World editing requires a live local singleplayer world."
                    : !settings.enabled
                        ? "World editing is disabled in Options."
                        : worldOverride(mc) == WorldOverride.DISABLED
                            ? "WorldEdit is disabled in this world."
                            : "WorldEdit requires creative mode. Change access in Options."));
      return true;
    }
    try {
      new Commands(editor(mc), player(mc)).run(text);
    } catch (IllegalArgumentException | ArithmeticException e) {
      mc.inGameHud.addChatMessage(ChatFormat.error(e.getMessage()));
    } catch (RuntimeException e) {
      LOG.error("WorldEdit command failed", e);
      mc.inGameHud.addChatMessage(ChatFormat.error("Command failed. See game log."));
    }
    return true;
  }

  private static Commands.Player player(Minecraft mc) {
    return new Commands.Player() {
      public Pos feet() {
        return new Pos(
            (int) Math.floor(mc.player.x),
            (int) Math.floor(mc.player.boundingBox.minY + .00001),
            (int) Math.floor(mc.player.z));
      }

      public Pos target() {
        var h = mc.field_2823;
        return h != null && h.field_1983 == class_212.TILE
            ? new Pos(h.field_1984, h.field_1985, h.field_1986)
            : null;
      }

      public Pos facing() {
        if (mc.player.pitch > 60) return new Pos(0, -1, 0);
        if (mc.player.pitch < -60) return new Pos(0, 1, 0);
        int d = Math.floorMod((int) Math.floor(mc.player.yaw / 90 + .5), 4);
        return switch (d) {
          case 0 -> new Pos(0, 0, 1);
          case 1 -> new Pos(-1, 0, 0);
          case 2 -> new Pos(0, 0, -1);
          default -> new Pos(1, 0, 0);
        };
      }

      public void wand() {
        int id = settings.wandItem;
        if (id >= Item.ITEMS.length || Item.ITEMS[id] == null)
          throw new IllegalArgumentException("Configure a registered wand item first.");
        for (int i = 0; i < mc.player.inventory.main.length; i++) {
          var s = mc.player.inventory.main[i];
          if (s != null && s.itemId == id) {
            if (i < 9) mc.player.inventory.selectedSlot = i;
            message(mc, "Wand already in your inventory. Left-click pos1, right-click pos2.");
            return;
          }
        }
        ItemStack stack = new ItemStack(id, 1, 0);
        if (!mc.player.inventory.method_671(stack))
          throw new IllegalArgumentException("Make room in your inventory for the wand.");
        message(mc, "Wand added. Left-click pos1, right-click pos2.");
      }

      public void toggleWand() {
        Settings next = settings();
        next.wandEnabled = !next.wandEnabled;
        save(next);
        message(mc, "Selection wand: " + (settings.wandEnabled ? "ON" : "OFF"));
      }

      public void navigate(String command, int amount, boolean flight, boolean glass) {
        new NavigationActions(mc, editor(mc)).run(command, amount, flight, glass);
      }

      public int entities(EntityQuery query, boolean remove) {
        return EntityEdits.apply(mc, query, remove);
      }

      public void drawSelection() {
        Settings next = settings();
        next.showSelection = !next.showSelection;
        save(next);
        message(mc, "Selection outline: " + (settings.showSelection ? "ON" : "OFF"));
      }
    };
  }

  private static void save(Settings s) {
    try {
      apply(s);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not save settings.", e);
    }
  }

  private static void message(Minecraft mc, String s) {
    mc.inGameHud.addChatMessage(ChatFormat.info(s));
  }

  public static boolean holdingWand(Minecraft mc) {
    return settings.wandEnabled
        && available(mc)
        && mc.player.inventory.getSelectedItem() != null
        && mc.player.inventory.getSelectedItem().itemId == settings.wandItem;
  }

  public static boolean click(Minecraft mc, int button) {
    if (button < 0 || button > 1 || mc.currentScreen != null || !holdingWand(mc)) return false;
    Pos p = player(mc).target();
    if (p != null) editor(mc).position(button == 0 ? 1 : 2, p);
    return true;
  }
}
