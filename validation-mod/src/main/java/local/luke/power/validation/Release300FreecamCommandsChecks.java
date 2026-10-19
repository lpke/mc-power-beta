package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.mixin.CullingChunkCacheAccessor;
import local.luke.power.rules.GameRule;
import local.luke.power.storage.PowerConfig;
import local.luke.power.worldedit.*;
import local.luke.power.worldedit.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;

/** Freecam commands use the real chat/engine path in one loaded chunk and an isolated edit session. */
final class Release300FreecamCommandsChecks {
  private Release300FreecamCommandsChecks() {}
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && !mc.player.dead && mc.player.health > 0,
        "open a disposable singleplayer world with a living player");
    check(!DetachedCamera.isActive() && mc.player.field_1595 == null && mc.player.container == mc.player.playerContainer,
        "leave freecam/vehicles and close inventory screens before freecam command checks");
    check(!WorldEditor.editing(), "finish the current WorldEdit transaction first");
    failures = 0;
    var rules = AccessRules.copy(mc); var settings = WorldEditor.settings();
    Screen screen = mc.currentScreen; boolean paused = mc.paused, focused = mc.field_2778;
    Object view = mc.field_2807, target = mc.field_2823;
    Object oldEditor = field(WorldEditor.class, "editor").get(null), oldWorld = field(WorldEditor.class, "world").get(null);
    Object camera = Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    Object cameraPosition = Editor230Checks.get(camera, "cameraPosition");
    byte[] preferences = Files.readAllBytes(PowerConfig.path());
    ItemStack[] inventory = DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer);
    List<String> stacks = new ArrayList<>(); for (ItemStack stack : inventory) stacks.add(stack == null ? null : DeathItems.encode(stack));
    String beforeInventory = DeathItems.inventory(inventory); int selected = mc.player.inventory.selectedSlot;
    var chunks = (CullingChunkCacheAccessor)mc.world.method_259(); var loaded = new HashSet<>(chunks.power$loadedChunks().keySet());
    int x = ((int)Math.floor(mc.player.x) >> 4) << 4, z = ((int)Math.floor(mc.player.z) >> 4) << 4;
    int y = Math.max(16, Math.min(120, (int)Math.floor(mc.player.boundingBox.minY) + 8));
    Editor fixture = null;
    try (var playerFields = new Fields(mc.player); var bounds = new Fields(mc.player.boundingBox);
        var cameraFields = new Fields(camera); var position = new Fields(cameraPosition); var options = new Fields(mc.options);
        var scene = new Scene(mc, x, y, z)) {
      try {
        mc.setScreen(null); mc.paused = false; mc.field_2807 = mc.player;
        var allowed = rules.copy(); allowed.set(GameRule.ALLOW_WORLD_EDITING, true); allowed.set(GameRule.ALLOW_TELEPORTATION, true);
        AccessRules.restore(mc, allowed);
        // A fresh session keeps the user's selection, clipboard and undo/redo history untouched.
        field(WorldEditor.class, "editor").set(null, null); field(WorldEditor.class, "world").set(null, null);
        var local = settings.copy(); local.blocksPerTick = 64; local.blockLimit = 256; WorldEditor.preview(local);
        fixture = WorldEditor.editor(mc); final Editor editor = fixture;
        scene.clear();
        int selectionY = (int)Math.floor(HeadGeometry.baseY(y + .05));
        Pos first = new Pos(x + 4, selectionY, z + 4), second = first.add(1, 0, 0);
        scene.set(first, new BlockValue(20, 0)); scene.set(second, new BlockValue(35, 14));
        double bodyX = mc.player.x, bodyY = mc.player.y, bodyZ = mc.player.z;
        FreecamPreviewChecks.pose(x + 4.25, y + .05, z + 4.25, 0, 90); FreecamPreviewChecks.active(true);
        test("freecam pos1/pos2 and permitted set/undo complete through the native command and tick gates", () -> {
          check(WorldEditor.commandAvailable(mc, "set"), "permitted freecam set remains unavailable");
          command(mc, "//pos1");
          FreecamPreviewChecks.pose(x + 5.25, y + .05, z + 4.25, 0, 90); command(mc, "//pos2");
          check(first.equals(editor.pos1) && second.equals(editor.pos2), "selection ignored detached head-base coordinates");
          command(mc, "//set stone"); check(editor.engine.busy(), "freecam set did not queue a transaction"); drain(mc, editor);
          check(scene.get(first).same(new BlockValue(1, 0)) && scene.get(second).same(new BlockValue(1, 0)), "permitted freecam transaction did not publish both blocks");
          check(editor.engine.undoSize() == 1, "completed freecam edit did not retain undo");
          command(mc, "//undo"); drain(mc, editor);
          check(scene.get(first).same(new BlockValue(20, 0)) && scene.get(second).same(new BlockValue(35, 14)), "freecam undo lost original blocks/metadata");
          check(mc.player.x == bodyX && mc.player.y == bodyY && mc.player.z == bodyZ, "selection/edit/undo moved the parked player");
          unchangedInventory(mc, beforeInventory, selected);
        });
        test("freecam cannot bypass a revoked editing rule and safe selection remains available", () -> {
          AccessRules.set(mc, GameRule.ALLOW_WORLD_EDITING, false);
          check(!WorldEditor.commandAvailable(mc, "set") && WorldEditor.commandAvailable(mc, "pos1"), "freecam command permission ownership disagrees");
          int undo = editor.engine.undoSize();
          var output = ChatChecks.submit(mc, "//set dirt");
          check(output.stream().anyMatch(line -> line.contains("allow_world_editing")), "denied freecam set omitted its rule requirement");
          check(!editor.engine.busy() && editor.engine.undoSize() == undo, "denied freecam set queued work/history");
          check(scene.get(first).same(new BlockValue(20, 0)) && scene.get(second).same(new BlockValue(35, 14)), "denied freecam set changed blocks");
          unchangedInventory(mc, beforeInventory, selected); AccessRules.set(mc, GameRule.ALLOW_WORLD_EDITING, true);
        });
        test("native jumpto traces from detached view and leaves inventory and camera position unchanged", () -> {
          Pos pillar = new Pos(x + 9, y + 1, z + 6);
          scene.set(pillar.add(0, -1, 0), new BlockValue(1, 0)); scene.set(pillar, new BlockValue(1, 0));
          FreecamPreviewChecks.pose(x + 5.5, y + 1.5, z + 6.5, 0, 90);
          var output = command(mc, "/jumpto");
          check(output.stream().anyMatch(line -> line.contains("Moved to")), "permitted detached navigation failed: " + output);
          check(Math.abs(mc.player.x - (x + 9.5)) < 1e-6 && Math.abs(mc.player.z - (z + 6.5)) < 1e-6
              && Math.abs(mc.player.boundingBox.minY - (y + 2)) < 1e-6, "navigation ray followed parked body instead of detached pillar");
          check(DetachedCamera.x() == x + 5.5 && DetachedCamera.y() == y + 1.5 && DetachedCamera.z() == z + 6.5, "navigation moved the detached camera");
          unchangedInventory(mc, beforeInventory, selected);
          AccessRules.set(mc, GameRule.ALLOW_TELEPORTATION, false);
          double parkedX = mc.player.x, parkedY = mc.player.y, parkedZ = mc.player.z;
          output = ChatChecks.submit(mc, "/jumpto");
          check(output.stream().anyMatch(line -> line.contains("allow_teleportation"))
              && mc.player.x == parkedX && mc.player.y == parkedY && mc.player.z == parkedZ, "freecam navigation bypassed teleport permission");
        });
        test("freecam command scene does not generate or load chunks", () ->
            check(loaded.equals(chunks.power$loadedChunks().keySet()), "bounded freecam commands changed the loaded chunk set"));
      } finally {
        if (fixture != null) fixture.engine.finishRollback();
      }
    } finally {
      WorldEditor.preview(settings); field(WorldEditor.class, "editor").set(null, oldEditor); field(WorldEditor.class, "world").set(null, oldWorld);
      AccessRules.restore(mc, rules); mc.field_2807 = (net.minecraft.entity.LivingEntity)view; mc.field_2823 = (net.minecraft.class_27)target;
      mc.paused = paused; mc.field_2778 = focused;
      if (!beforeInventory.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)))) restoreInventory(mc, stacks);
      mc.player.inventory.selectedSlot = selected; mc.setScreen(screen);
    }
    test("freecam command fixture leaves durable preferences unchanged", () -> check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "freecam commands wrote global preferences"));
    log("RELEASE300 FREECAM-COMMANDS failures=" + failures);
  }
  private static List<String> command(Minecraft mc, String text) throws Exception {
    check(DetachedCamera.isActive(), "freecam ended before " + text);
    var output = ChatChecks.submit(mc, text);
    check(DetachedCamera.isActive(), "command ended freecam: " + text);
    check(output.stream().noneMatch(line -> line.contains("§c")), text + " failed: " + output); return output;
  }
  private static void drain(Minecraft mc, Editor editor) {
    int ticks = 0;
    while (editor.engine.busy() && ticks++ < 64) {
      check(DetachedCamera.isActive(), "freecam ended during transaction");
      WorldEditor.tick(mc);
    }
    check(!editor.engine.busy(), "freecam transaction did not finish through WorldEditor.tick");
  }
  private static void unchangedInventory(Minecraft mc, String expected, int selected) throws Exception {
    check(expected.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)))
        && mc.player.inventory.selectedSlot == selected, "freecam command changed parked inventory/cursor/crafting/selected slot");
  }
  private static void restoreInventory(Minecraft mc, List<String> bytes) throws Exception {
    for (int i = 0; i < bytes.size(); i++) {
      ItemStack stack = bytes.get(i) == null ? null : DeathItems.decode(bytes.get(i));
      if (i < 36) mc.player.inventory.main[i] = stack;
      else if (i < 40) mc.player.inventory.armor[i - 36] = stack;
      else if (i == 40) mc.player.inventory.setCursorStack(stack);
      else mc.player.playerContainer.method_2084(i - 40).setStack(stack);
    }
  }
  private static Field field(Class<?> type, String name) throws Exception {
    for (; type != null; type = type.getSuperclass()) try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
    catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static final class Fields implements AutoCloseable {
    final Object target; final Map<Field, Object> values = new LinkedHashMap<>();
    Fields(Object target) throws Exception {
      this.target = target;
      for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) for (Field f : type.getDeclaredFields())
        if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())) { f.setAccessible(true); values.put(f, f.get(target)); }
    }
    public void close() throws Exception { for (var e : values.entrySet()) e.getKey().set(target, e.getValue()); }
  }
  private static final class Scene implements AutoCloseable {
    final MinecraftWorld adapter; final Map<Pos, BlockValue> original = new LinkedHashMap<>();
    Scene(Minecraft mc, int x, int y, int z) {
      adapter = new MinecraftWorld(mc.world);
      for (int px = x + 4; px <= x + 10; px++) for (int py = y - 2; py <= y + 4; py++) for (int pz = z + 4; pz <= z + 8; pz++) {
        Pos point = new Pos(px, py, pz); check(adapter.loaded(point), "fixture scene is not already loaded"); original.put(point, adapter.get(point));
      }
    }
    void clear() { original.keySet().forEach(p -> adapter.set(p, BlockValue.AIR)); }
    void set(Pos p, BlockValue value) { check(original.containsKey(p), "fixture write left its bounded scene"); adapter.set(p, value); }
    BlockValue get(Pos p) { return adapter.get(p); }
    public void close() { original.forEach(adapter::set); }
  }
}
