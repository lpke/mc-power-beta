package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.awt.MouseInfo;
import java.nio.file.*;
import java.util.*;
import local.luke.power.autowalk.InventoryLook;
import local.luke.power.building.config.Config;
import local.luke.power.death.DeathItems;
import local.luke.power.storage.PowerConfig;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Holds the game thread before GUI release while the private X driver queues real motion. */
public final class GuiCursor472Checks {
  private static final Path STATUS = Path.of("power-beta-gui-cursor-472.json");
  private static final Path ACK = Path.of("power-beta-gui-cursor-472.ack");
  private static State saved;
  private GuiCursor472Checks() {}

  static void run(Minecraft mc, String command) throws Exception {
    if (command.equals("finish")) {
      GuiCursor472StaleQueryFault.disarm();
      if (saved != null) { var old = saved; saved = null; old.close(); }
      Files.deleteIfExists(ACK); log("CURSOR472 restored"); return;
    }
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    if (saved == null) saved = new State(mc);
    saved.owner();
    if (command.equals("state")) { write(state(mc, "state")); return; }
    if (command.equals("close")) { saved.gameplay(); return; }
    if (command.startsWith("helper ")) {
      saved.gameplay();
      mc.field_2767 = command.equals("helper native") ? new class_596(mc.canvas)
          : (class_596)Class.forName("local.luke.power.controls.tweaks.rawinput.RawMouseHelper")
              .getConstructor(java.awt.Component.class).newInstance(mc.canvas);
      return;
    }
    if (command.equals("inventory")) {
      var config = Config.current().copy(); config.inventoryMouseLook = config.inventoryWhileMoving = true; Config.preview(config);
      saved.gameplay(); saved.open("inventory"); return;
    }
    if (command.equals("menu")) { mc.setScreen(new class_525()); return; }
    check(command.startsWith("race "), "use gui-cursor-472 race native|raw inventory|chest|pause|chat, state|close|inventory|menu|finish");
    var args = command.substring(5).split(" ");
    check((args.length == 2 || args.length == 3 && args[2].equals("stale-query")) && Set.of("native", "raw").contains(args[0])
        && Set.of("inventory", "chest", "pause", "chat").contains(args[1]), "invalid cursor race");
    boolean staleQuery = args.length == 3;
    saved.gameplay();
    mc.field_2767 = args[0].equals("native") ? new class_596(mc.canvas)
        : (class_596)Class.forName("local.luke.power.controls.tweaks.rawinput.RawMouseHelper")
            .getConstructor(java.awt.Component.class).newInstance(mc.canvas);
    // Force one clean native capture, with a noncentral saved GUI cursor.
    mc.field_2767.method_1971(); Mouse.setCursorPosition(90, 110); poll();
    mc.field_2767.method_1970(); mc.field_2778 = true;
    Files.deleteIfExists(ACK);
    var ready = state(mc, "queue"); ready.addProperty("helper", args[0]); ready.addProperty("kind", args[1]); write(ready);
    await("queue");
    try {
      if (staleQuery) {
        var beforeRelease = state(mc, "pre-release-fault-source");
        GuiCursor472StaleQueryFault.arm(beforeRelease.get("physicalX").getAsInt(), beforeRelease.get("physicalY").getAsInt());
      }
      saved.open(args[1]);
    } finally { GuiCursor472StaleQueryFault.disarm(); }
    var samples = new JsonArray(); samples.add(state(mc, "immediate"));
    var openingEvents = new JsonArray(); events("immediate", openingEvents);
    for (int i = 0; i < 6; i++) {
      poll(); samples.add(state(mc, "poll-" + i)); events("poll-" + i, openingEvents); Thread.sleep(3);
    }
    var move = state(mc, "move"); move.add("samples", samples); write(move);
    await("move"); poll();
    var afterMove = state(mc, "after-move");
    var deliberateEvents = new JsonArray(); events("after-move", deliberateEvents);
    int x = Mouse.getX(), y = Mouse.getY();
    mc.setScreen(new class_525()); poll();
    var navigation = state(mc, "menu-navigation");
    var result = state(mc, "done"); result.add("samples", samples); result.add("afterMove", afterMove); result.add("navigation", navigation);
    result.addProperty("helper", args[0]); result.addProperty("kind", args[1]);
    result.addProperty("testMode", staleQuery ? "stale-query fault injection" : "native queued physical motion");
    if (staleQuery) result.add("staleQueryFault", GuiCursor472StaleQueryFault.report());
    result.add("openingEvents", openingEvents); result.add("deliberateEvents", deliberateEvents);
    var violations = new JsonArray();
    if (staleQuery && GuiCursor472StaleQueryFault.report().get("applications").getAsInt() != 1)
      violations.add("the validation-only stale-query fault did not execute exactly once");
    int centerX = mc.displayWidth / 2, centerY = mc.displayHeight / 2;
    for (var sample : samples) {
      var s = sample.getAsJsonObject();
      if (s.get("mouseX").getAsInt() != centerX || s.get("mouseY").getAsInt() != centerY
          || s.get("physicalX").getAsInt() != centerX || s.get("physicalY").getAsInt() != mc.displayHeight - 1 - centerY)
        violations.add(s.get("phase").getAsString() + " failed to center: " + s);
    }
    for (var event : openingEvents) {
      var e = event.getAsJsonObject();
      if (e.get("button").getAsInt() == -1 && e.get("wheel").getAsInt() == 0
          && (e.get("x").getAsInt() != centerX || e.get("y").getAsInt() != centerY))
        violations.add("queued gameplay motion reached the GUI: " + e);
    }
    boolean deliberateEvent = false;
    for (var event : deliberateEvents) {
      var e = event.getAsJsonObject();
      if (e.get("button").getAsInt() == -1 && e.get("x").getAsInt() == centerX + 19
          && e.get("y").getAsInt() == centerY - 7) deliberateEvent = true;
    }
    if (!deliberateEvent) violations.add("deliberate movement did not reach the GUI event queue: " + deliberateEvents);
    if (x != centerX + 19 || y != centerY - 7) violations.add("deliberate movement was lost: " + afterMove);
    if (Mouse.getX() != x || Mouse.getY() != y) violations.add("menu navigation warped the pointer: " + navigation);
    result.add("violations", violations); result.addProperty("passed", violations.size() == 0); write(result);
    saved.preserved();
    log("CURSOR472 observed " + args[0] + " " + args[1] + " mode=" + result.get("testMode").getAsString()
        + " violations=" + violations.size() + (staleQuery ? "; fault injection, Xwayland warp delay not reproduced" : ""));
  }

  private static void await(String phase) throws Exception {
    long deadline = System.nanoTime() + 5_000_000_000L;
    while (System.nanoTime() < deadline) {
      if (Files.exists(ACK) && Files.readString(ACK).trim().equals(phase)) { Files.delete(ACK); return; }
      Thread.sleep(1);
    }
    throw new AssertionError("physical driver did not acknowledge " + phase);
  }
  private static void poll() { Display.processMessages(); Mouse.poll(); }
  private static void events(String phase, JsonArray target) {
    // Observe exactly the events a normal Screen would receive, before dispatch.
    // The opening phase has no new physical movement after its queue handshake.
    while (Mouse.next()) {
      var event = new JsonObject(); event.addProperty("phase", phase);
      event.addProperty("x", Mouse.getEventX()); event.addProperty("y", Mouse.getEventY());
      event.addProperty("dx", Mouse.getEventDX()); event.addProperty("dy", Mouse.getEventDY());
      event.addProperty("button", Mouse.getEventButton()); event.addProperty("down", Mouse.getEventButtonState());
      event.addProperty("wheel", Mouse.getEventDWheel()); event.addProperty("ns", Mouse.getEventNanoseconds());
      target.add(event);
    }
  }
  private static void write(JsonObject value) throws Exception {
    Path temporary = STATUS.resolveSibling(STATUS.getFileName() + ".tmp");
    Files.writeString(temporary, value.toString()); Files.move(temporary, STATUS, StandardCopyOption.REPLACE_EXISTING);
  }
  private static JsonObject state(Minecraft mc, String phase) {
    var value = new JsonObject(); value.addProperty("phase", phase);
    value.addProperty("mouseX", Mouse.getX()); value.addProperty("mouseY", Mouse.getY());
    value.addProperty("width", mc.displayWidth); value.addProperty("height", mc.displayHeight);
    var pointer = MouseInfo.getPointerInfo().getLocation();
    int left = Display.getX(), top = Display.getY();
    if (mc.canvas != null && mc.canvas.isShowing()) { var p = mc.canvas.getLocationOnScreen(); left = p.x; top = p.y; }
    value.addProperty("physicalX", pointer.x - left); value.addProperty("physicalY", pointer.y - top);
    value.addProperty("windowX", left); value.addProperty("windowY", top);
    value.addProperty("grabbed", Mouse.isGrabbed()); value.addProperty("looking", InventoryLook.active());
    value.addProperty("screen", mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getName());
    value.addProperty("yaw", mc.player.yaw); value.addProperty("pitch", mc.player.pitch);
    return value;
  }
  private static final class State implements AutoCloseable {
    final Minecraft mc; final Object world, player;
    final Screen screen; final net.minecraft.inventory.Container container; final class_596 helper;
    final boolean focused, paused, grabbed; final int x, y;
    final local.luke.power.building.config.Settings building;
    final byte[] config; final String inventory;
    State(Minecraft mc) throws Exception {
      this.mc = mc; world = mc.world; player = mc.player; screen = mc.currentScreen;
      container = mc.player.container; helper = mc.field_2767;
      focused = mc.field_2778; paused = mc.paused; grabbed = Mouse.isGrabbed(); x = Mouse.getX(); y = Mouse.getY();
      building = Config.current().copy(); config = Files.readAllBytes(PowerConfig.path());
      inventory = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
    }
    void owner() { check(mc.world == world && mc.player == player, "keep cursor checks in their original world/player"); }
    void gameplay() {
      owner(); InventoryLook.stop();
      if (mc.currentScreen != null) mc.setScreen(null);
      mc.player.container = mc.player.playerContainer; mc.field_2778 = true; mc.paused = false;
      if (!Mouse.isGrabbed()) mc.field_2767.method_1970();
    }
    void open(String kind) {
      Screen next = switch (kind) {
        case "inventory" -> new class_585(mc.player);
        case "chest" -> new class_264(mc.player.inventory, new EmptyChest());
        case "pause" -> new class_525();
        case "chat" -> new ChatScreen();
        default -> throw new IllegalArgumentException(kind);
      };
      mc.setScreen(next);
      if (next instanceof ContainerScreen host) mc.player.container = host.container;
    }
    void preserved() throws Exception {
      check(Arrays.equals(config, Files.readAllBytes(PowerConfig.path())), "cursor check wrote preferences");
      check(inventory.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer))), "cursor check changed native inventory");
    }
    public void close() throws Exception {
      owner(); InventoryLook.stop(); preserved(); Config.preview(building);
      mc.currentScreen = screen; mc.player.container = container; mc.field_2767 = helper;
      mc.field_2778 = focused; mc.paused = paused;
      Mouse.setCursorPosition(x, y); Mouse.setGrabbed(grabbed);
    }
  }
  private static final class EmptyChest implements Inventory {
    private final ItemStack[] items = new ItemStack[27];
    public int size() { return items.length; }
    public ItemStack getStack(int index) { return items[index]; }
    public ItemStack removeStack(int index, int count) { var item = items[index]; if (item == null) return null; if (item.count <= count) { items[index] = null; return item; } return item.split(count); }
    public void setStack(int index, ItemStack item) { items[index] = item; }
    public String getName() { return "Cursor472"; }
    public int getMaxCountPerStack() { return 64; }
    public void markDirty() {}
    public boolean canPlayerUse(PlayerEntity player) { return true; }
  }
}
