package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.JsonPrimitive;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.config.backend.MapBackend;
import local.luke.power.input.*;
import local.luke.power.map.*;
import local.luke.power.rules.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Commands prepare state; only native window input supplies the wheel event. Disposable worlds only. */
final class Minimap441Checks {
  private static final String ROWS = "power_building.hotbar.scroll";
  private static final Set<String> CASES = Set.of("enabled", "disabled", "nohold", "hud-hidden",
      "minimap-off", "debug", "clamp", "fov", "spectator", "chord-modifier-first", "chord-key-first", "rows", "guards", "menu");
  private static Saved saved;
  private static String active;
  private static boolean releaseStage, observedOffset;
  private static List<String> beforeItems;
  private static float beforeSpeed;
  private static String beforeCreative;
  private static MapSettings beforeMap;

  private static Field field(Object owner, String name) throws Exception {
    for (Class<?> type = owner instanceof Class<?> c ? c : owner.getClass(); type != null; type = type.getSuperclass()) {
      try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
      catch (NoSuchFieldException ignored) {}
    }
    throw new NoSuchFieldException(name);
  }
  private static Class<?> controls() throws Exception { return Class.forName("local.luke.power.controls.util.ModOptions"); }
  private static float offset() throws Exception { return controls().getField("zoomFovOffset").getFloat(null); }
  private static Object creative() throws Exception { return Class.forName("local.luke.power.creative.config.Config").getMethod("current").invoke(null); }
  private static float speed(Minecraft mc) throws Exception { return (float)mc.player.getClass().getMethod("power_spectatorSpeed").invoke(mc.player); }
  private static KeyBinding rows(Minecraft mc) {
    return Arrays.stream(mc.options.allKeys).filter(key -> key instanceof Binding b && b.power$id().equals(ROWS))
        .map(key -> (KeyBinding)key).findFirst().orElseThrow();
  }
  private static KeyBinding fov() throws Exception {
    return (KeyBinding)Class.forName("local.luke.power.controls.tweaks.morekeybinds.KeyBindingListener").getField("zoom").get(null);
  }
  private static void cancelRows() throws Exception { Class.forName("local.luke.power.building.hotbar.Hotbars").getMethod("cancelPending").invoke(null); }
  private static List<String> items(Minecraft mc) throws Exception {
    List<String> result = new ArrayList<>();
    for (Object value : mc.player.playerContainer.slots) {
      var slot = (net.minecraft.screen.slot.Slot)value;
      result.add(bytes(slot.getStack()));
    }
    result.add(bytes(mc.player.inventory.getCursorStack()));
    return result;
  }
  private static String bytes(ItemStack stack) throws Exception {
    if (stack == null) return "";
    var bytes = new ByteArrayOutputStream();
    NbtIo.write(stack.writeNbt(new NbtCompound()), new DataOutputStream(bytes));
    return stack.count + ":" + HexFormat.of().formatHex(bytes.toByteArray());
  }

  /** Validation.tick calls this before processing fixture commands, after native gameplay input. */
  static void observe(Minecraft mc) {
    if (!"fov".equals(active)) return;
    try { observedOffset |= offset() != 0; }
    catch (Exception error) { throw new IllegalStateException(error); }
  }

  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("stop")) {
      if (saved != null) {
        Saved restore = saved;
        try { cancelRows(); check(beforeItems == null || beforeItems.equals(items(mc)), "Inventory changed before cleanup"); }
        finally { active = null; saved = null; restore.restore(mc, true); }
      }
      log("MINIMAP441 stopped; live preferences, bindings, HUD, mode and selection restored"); return;
    }
    if (action.startsWith("prepare ")) {
      String name = action.substring(8).trim();
      check(CASES.contains(name), "Unknown minimap case " + name);
      check(active == null, "Finish check/check-release or stop before another case");
      check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "Focus a disposable singleplayer world");
      check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "Mute the disposable client");
      check(!Bindings.physical(Keyboard.KEY_Z) && !Bindings.physical(Keyboard.KEY_F9)
          && !Bindings.physical(Keyboard.KEY_LCONTROL) && !Bindings.physical(Keyboard.KEY_RCONTROL), "Release Z, F9 and Ctrl before prepare");
      check(Maps.cache() != null, "Wait for the native minimap cache");
      if (saved == null) { saved = new Saved(mc); previewCancel(); }
      saved.restore(mc, false);
      AccessRules.set(mc, GameRule.ALLOW_CREATIVE_MODE, true);
      AccessRules.set(mc, GameRule.ALLOW_SPECTATOR_MODE, true);
      CreativeVehicleChecks.mode(mc, name.equals("spectator") ? "SPECTATOR" : "SURVIVAL");
      mc.setScreen(null); mc.paused = false; mc.options.hideHud = name.equals("hud-hidden"); mc.options.debugHud = name.equals("debug");
      mc.player.inventory.selectedSlot = 0;
      // Silence colliding actions while keeping the bindings under test registered normally.
      for (Object key : mc.options.allKeys) if (key instanceof KeyBinding binding
          && (binding.code == Keyboard.KEY_Z || binding.code == Keyboard.KEY_F9)) binding.code = 0;
      MapKeys.ENLARGE.code = name.startsWith("chord-") ? Keyboard.KEY_F9 : Keyboard.KEY_Z;
      rows(mc).code = name.equals("rows") ? MapKeys.ENLARGE.code : 0;
      fov().code = name.equals("fov") ? MapKeys.ENLARGE.code : 0;
      Map<String, Integer> modifiers = new HashMap<>(saved.modifiers);
      modifiers.remove(MapKeys.ENLARGE.translationKey); modifiers.remove(ROWS); modifiers.remove(((Binding)(Object)fov()).power$id());
      if (name.startsWith("chord-")) modifiers.put(MapKeys.ENLARGE.translationKey, Chord.CTRL);
      Bindings.configure(modifiers); Bindings.register(mc.options.allKeys);
      var map = MapConfig.copy(); map.minimap = !name.equals("minimap-off"); map.hideWithDebug = true;
      map.zoomWhileExpanded = !name.equals("disabled"); map.zoom = name.equals("clamp") ? 4 : 1.25;
      MapConfig.preview(map);
      controls().getField("zoomFovOffset").setFloat(null, 0);
      if (name.equals("fov")) field(mc.field_2818, "fovZoom").setFloat(mc.field_2818, 0);
      if (name.equals("rows")) check((boolean)Class.forName("local.luke.power.building.hotbar.Hotbars").getMethod("eligible", Minecraft.class).invoke(null, mc), "Hotbar row selector is not eligible");
      beforeItems = items(mc); beforeSpeed = speed(mc); beforeCreative = Catalog.JSON.toJson(creative()); beforeMap = MapConfig.current();
      observedOffset = false; releaseStage = false; active = name;
      if (name.equals("menu")) mc.setScreen(new Screen() {});
      String input = name.equals("nohold") || name.equals("menu") ? "hold no keys" : name.startsWith("chord-") ? "hold Ctrl_L, then F9" : "hold z";
      log("MINIMAP441 READY " + name + " " + input + "; send exactly one wheelup/button4; keep keys held; run check. Expected zoom="
          + expectedZoom(name) + " hotbar=" + expectedSlot(name));
      return;
    }
    check(active != null, "Run minimap-441 prepare CASE first");
    if (action.equals("check-release")) {
      check(releaseStage, "Run chord check first");
      check(!Bindings.down(MapKeys.ENLARGE), "Released chord still owns input");
      check(Math.abs(MapConfig.current().zoom - 1.375) < 1e-9 && mc.player.inventory.selectedSlot == 8,
          "Released chord consumed native hotbar wheel: zoom=" + MapConfig.current().zoom + " slot=" + mc.player.inventory.selectedSlot);
      unchanged(mc); log("PASS MINIMAP441 " + active + " partial release returns wheel to native hotbar");
      finish(mc); return;
    }
    check(action.equals("check") && !releaseStage, "Use check, check-release or stop");
    String name = active;
    if (!name.equals("nohold") && !name.equals("menu"))
      check(Bindings.physical(MapKeys.ENLARGE.code), "Expected physical Enlarge binding held");
    check(Math.abs(MapConfig.current().zoom - expectedZoom(name)) < 1e-9, "Wrong zoom=" + MapConfig.current().zoom + " expected=" + expectedZoom(name));
    check(mc.player.inventory.selectedSlot == expectedSlot(name), "Wrong native hotbar slot=" + mc.player.inventory.selectedSlot + " expected=" + expectedSlot(name));
    if (Set.of("disabled", "nohold", "hud-hidden", "minimap-off", "clamp", "rows", "menu").contains(name))
      check(MapConfig.current() == beforeMap, "Unchanged zoom published a replacement settings snapshot");
    unchanged(mc);
    if (name.equals("fov")) {
      check(Bindings.down(fov()), "Camera zoom binding did not share the physical Enlarge press");
      check(!observedOffset && offset() == 0 && field(mc.field_2818, "fovZoom").getFloat(mc.field_2818) == 0,
          "Expanded wheel changed camera FOV offset or accumulated zoom");
    }
    if (name.equals("rows")) {
      try {
        check(Bindings.down(ROWS), "Native row scroll binding not held");
        check(!Minimap.expandedWheel(mc, 120), "Map helper stole held row-selector input");
      } finally { cancelRows(); }
    }
    if (name.equals("guards")) guards(mc);
    log("PASS MINIMAP441 " + name + " native wheel owner, zoom=" + MapConfig.current().zoom
        + " selected=" + mc.player.inventory.selectedSlot + " complete inventory/NBT, spectator speed/config unchanged");
    if (name.startsWith("chord-")) {
      releaseStage = true;
      log("MINIMAP441 RELEASE " + (name.equals("chord-modifier-first") ? "release Ctrl_L; KEEP F9 held" : "release F9; KEEP Ctrl_L held")
          + "; send exactly one wheelup/button4; run check-release; expected zoom1.375 hotbar8");
    } else finish(mc);
  }

  private static double expectedZoom(String name) {
    return name.equals("clamp") ? 4 : Set.of("disabled", "nohold", "hud-hidden", "minimap-off", "rows", "menu").contains(name) ? 1.25 : 1.375;
  }
  private static int expectedSlot(String name) { return Set.of("disabled", "nohold", "hud-hidden", "minimap-off").contains(name) ? 8 : 0; }
  private static void unchanged(Minecraft mc) throws Exception {
    check(beforeItems.equals(items(mc)), "Native wheel changed complete inventory/cursor NBT");
    check(speed(mc) == beforeSpeed, "Wheel changed spectator speed");
    check(beforeCreative.equals(Catalog.JSON.toJson(creative())), "Wheel changed Creative preferences");
  }
  private static void finish(Minecraft mc) throws Exception {
    cancelRows(); active = null; saved.restore(mc, false);
    log("MINIMAP441 case restored; RELEASE ALL keys before next prepare; stop restores queued map preferences");
  }
  private static void previewCancel() throws Exception {
    boolean before = MapConfig.current().zoomWhileExpanded;
    ConfigSession session = new ConfigSession(); MapBackend.register(session);
    Setting entry = session.settings().stream().filter(s -> s.id.equals("maps.zoomWhileExpanded")).findFirst().orElseThrow();
    check(entry.defaultValue.getAsBoolean(), "Expanded map wheel default is not enabled");
    entry.value = new JsonPrimitive(!before); session.preview();
    check(MapConfig.current().zoomWhileExpanded != before, "Map setting draft did not preview");
    session.discard(); check(MapConfig.current().zoomWhileExpanded == before, "Cancel did not restore map wheel preference");
    log("PASS MINIMAP441 normal ConfigSession preview/Cancel restores wheel preference");
  }
  private static void guards(Minecraft mc) throws Exception {
    double zoom = MapConfig.current().zoom;
    Screen screen = mc.currentScreen; boolean paused = mc.paused, focus = mc.field_2778, hidden = mc.options.hideHud, dead = mc.player.dead;
    int health = mc.player.health;
    try {
      mc.currentScreen = new Screen() {}; check(!Minimap.expandedWheel(mc, 120), "Menu wheel claimed"); mc.currentScreen = screen;
      mc.paused = true; check(!Minimap.expandedWheel(mc, 120), "Paused wheel claimed"); mc.paused = paused;
      mc.field_2778 = false; check(!Minimap.expandedWheel(mc, 120), "Unfocused wheel claimed"); mc.field_2778 = focus;
      mc.options.hideHud = true; check(!Minimap.expandedWheel(mc, 120), "Hidden HUD wheel claimed"); mc.options.hideHud = hidden;
      mc.player.dead = true; check(!Minimap.expandedWheel(mc, 120), "Dead player wheel claimed"); mc.player.dead = dead;
      mc.player.health = 0; check(!Minimap.expandedWheel(mc, 120), "Zero-health wheel claimed"); mc.player.health = health;
      check(!Minimap.expandedWheel(mc, 0), "Zero wheel claimed");
      check(MapConfig.current().zoom == zoom, "Rejected helper changed map zoom");
    } finally { mc.currentScreen = screen; mc.paused = paused; mc.field_2778 = focus; mc.options.hideHud = hidden; mc.player.dead = dead; mc.player.health = health; }
    log("PASS MINIMAP441 direct menu/pause/focus/HUD/death/zero guards preserve zoom");
  }

  private static final class Saved {
    final MapSettings map = MapConfig.copy();
    final Map<String, Integer> modifiers = Bindings.modifiers();
    final Map<KeyBinding, Integer> codes = new IdentityHashMap<>();
    final RuleSet rules;
    final Screen screen;
    final String mode;
    final int slot;
    final boolean hideHud, debugHud, paused;
    final float fovOffset, fovZoom, spectatorSpeed;
    Saved(Minecraft mc) throws Exception {
      for (Object key : mc.options.allKeys) if (key instanceof KeyBinding binding) codes.put(binding, binding.code);
      rules = AccessRules.copy(mc); screen = mc.currentScreen; slot = mc.player.inventory.selectedSlot;
      hideHud = mc.options.hideHud; debugHud = mc.options.debugHud; paused = mc.paused;
      mode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
      spectatorSpeed = speed(mc); fovOffset = offset(); fovZoom = field(mc.field_2818, "fovZoom").getFloat(mc.field_2818);
    }
    void restore(Minecraft mc, boolean queueMap) throws Exception {
      cancelRows(); codes.forEach((key, code) -> key.code = code); Bindings.configure(modifiers); Bindings.register(mc.options.allKeys);
      CreativeVehicleChecks.mode(mc, mode); AccessRules.restore(mc, rules.copy());
      mc.player.getClass().getMethod("power_spectatorSpeed", float.class).invoke(mc.player, spectatorSpeed);
      mc.player.inventory.selectedSlot = slot; mc.options.hideHud = hideHud; mc.options.debugHud = debugHud; mc.paused = paused;
      mc.setScreen(screen); controls().getField("zoomFovOffset").setFloat(null, fovOffset);
      field(mc.field_2818, "fovZoom").setFloat(mc.field_2818, fovZoom);
      if (queueMap) MapConfig.queued(map); else MapConfig.preview(map);
    }
  }
}
