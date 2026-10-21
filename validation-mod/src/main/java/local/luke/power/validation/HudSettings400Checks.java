package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.HudSettings400Ui.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.config.*;
import local.luke.power.input.DetachedCamera;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.storage.PowerConfig;
import local.luke.power.subtitles.*;
import local.luke.power.ui.*;
import local.luke.power.world.WorldCompatibility;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Real Options/HUD controls, isolated drafts, saved user data and renderer visibility. */
public final class HudSettings400Checks {
  private static final List<String> KEYS = List.of("swapHands", "restock", "recipeBook", "sortInventory", "depositMatching", "saveCreativeHotbar", "loadCreativeHotbar");
  private HudSettings400Checks() {}
  public static void run(Minecraft mc, String phase) throws Exception {
    check(Set.of("native", "defaults", "hud", "visibility", "placement").contains(phase), "expected native, defaults, hud, visibility or placement");
    check(mc.world != null && mc.player != null && !mc.world.isRemote && WorldCompatibility.ready(mc.world), "load a disposable singleplayer world first");
    check(mc.currentScreen == null && mc.player.container == mc.player.playerContainer && mc.field_2807 == mc.player
        && mc.player.health > 0 && !mc.player.dead && !DetachedCamera.isActive(), "use a living player camera, close menus and leave freecam first");
    check(Display.isActive() && Display.getWidth() >= 1280 && Display.getHeight() >= 720, "focus a muted disposable client at least1280x720");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0 && !NativeInventoryPlan.busy(), "mute the clone and finish inventory actions first");
    failures = 0; Path report;
    try (var state = new HudSettings400State(mc)) {
      report = state.report;
      state.seedBars();
      if (phase.equals("native") || phase.equals("defaults")) {
        for (var base : DefaultBases.Base.values()) optionDefaults(state, base);
        permissions(state); optionCaptures(state);
      }
      if (phase.equals("native") || phase.equals("hud")) {
        for (var base : DefaultBases.Base.values()) hudDrafts(state, base);
        commits(state); previewVisibility(state);
      }
      if (phase.equals("native") || phase.equals("visibility")) runtimeVisibility(state);
      if (phase.equals("native") || phase.equals("hud") || phase.equals("placement")) subtitlePlacement(state);
      state.unchangedItemsAndWorld();
    }
    Files.writeString(report.resolve("result.json"), "{\"phase\":\"" + phase + "\",\"failures\":" + failures + ",\"savedBarsRetained\":true,\"restored\":true}\n");
    log("SETTINGS400 " + phase.toUpperCase(Locale.ROOT) + " failures=" + failures + " reports=" + report);
  }

  private static Map<String, JsonElement> expected(DefaultBases.Base base) {
    boolean power = base == DefaultBases.Base.POWER_BETA; Map<String, JsonElement> values = new LinkedHashMap<>();
    for (String id : List.of("offhand.enabled", "heldLight.enabled", "inventoryTools.sorting", "inventoryTools.depositMatching", "inventoryTools.creativeHotbars",
        "inventoryTools.toolSwitch", "worldTools.backup", "worldTools.duplicate", "worldTools.recreate", "subtitles.enabled")) values.put(id, new JsonPrimitive(power));
    for (String id : List.of("recipeBook.enabled", "recipeBook.pauseMenu", "inventoryTools.restock", "inventoryTools.restockOtherHotbar", "subtitles.hideWithDebug")) values.put(id, new JsonPrimitive(false));
    for (String id : List.of("recipeBook.inventory", "inventoryTools.restockBeforeEmpty", "inventoryTools.restockTools", "inventoryTools.toolsFromInventory", "subtitles.showInInventory")) values.put(id, new JsonPrimitive(true));
    values.put("inventoryTools.restockThreshold", new JsonPrimitive(6)); values.put("inventoryTools.toolDurabilityReserve", new JsonPrimitive(1));
    values.put("subtitles.position", new JsonPrimitive(8)); values.put("subtitles.offsetX", new JsonPrimitive(-1)); values.put("subtitles.offsetY", new JsonPrimitive(-30));
    values.put("subtitles.textSize", new JsonPrimitive(1)); values.put("subtitles.backgroundOpacity", new JsonPrimitive(80)); values.put("subtitles.displaySeconds", new JsonPrimitive(3));
    check(values.size() == 28, "fixture must cover all28 modern preferences");
    for (String key : KEYS) values.put("keys.powerbeta." + key, new JsonPrimitive(0));
    values.put(OFF + "enableVisibility", new JsonPrimitive(true)); values.put(OFF + "horizontalPosition", new JsonPrimitive(1));
    values.put(OFF + "verticalPosition", new JsonPrimitive(0)); values.put(OFF + "horizontalPositionOffset", new JsonPrimitive(0)); values.put(OFF + "verticalPositionOffset", new JsonPrimitive(0));
    return values;
  }
  private static void optionDefaults(HudSettings400State state, DefaultBases.Base base) throws Exception {
    test("SETTINGS400 " + base.id + " actual Options resets cover28 preferences, seven unbound keys and five offhand positions", () -> {
      var options = state.options(base, false); byte[] before = Files.readAllBytes(PowerConfig.path());
      try {
        setting(options.session(), "rules.world.allow_creative_mode").value = new JsonPrimitive(true);
        setting(options.session(), HudEditorSettings.ENTRY).value = new JsonPrimitive(2);
        for (var entry : expected(base).entrySet()) {
          Setting setting = setting(options.session(), entry.getKey()); setting.value = different(setting, entry.getValue()); options.changed(setting);
          check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "manual preview saved " + setting.id);
          optionReset(options, setting.id); check(setting.value.equals(entry.getValue()), "native reset used the wrong base for " + setting.id);
          check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "manual reset saved " + setting.id);
        }
        state.unchangedItemsAndWorld();
      } finally { state.discard(options); }
    });
  }
  private static void permissions(HudSettings400State state) throws Exception {
    test("SETTINGS400 native saved-hotbar rows follow their Creative permission draft without changing data", () -> {
      var options = state.options(DefaultBases.Base.POWER_BETA, false); var rule = setting(options.session(), "rules.world.allow_creative_mode");
      List<String> ids = List.of("inventoryTools.creativeHotbars", "keys.powerbeta.saveCreativeHotbar", "keys.powerbeta.loadCreativeHotbar");
      try {
        rule.value = new JsonPrimitive(false); check(listed(options, ids).isEmpty(), "unavailable Creative controls remain listed");
        rule.value = new JsonPrimitive(true); check(new HashSet<>(listed(options, ids)).equals(new HashSet<>(ids)), "available Creative controls are missing");
        rule.value = new JsonPrimitive(false); check(listed(options, ids).isEmpty(), "revoked Creative controls retained rows");
        state.unchangedItemsAndWorld();
      } finally { state.discard(options); }
    });
  }
  private static void optionCaptures(HudSettings400State state) throws Exception {
    test("SETTINGS400 new Options sections render through native narrow and wide row geometry", () -> {
      var options = state.options(DefaultBases.Base.POWER_BETA, false);
      try {
        setting(options.session(), "rules.world.allow_creative_mode").value = new JsonPrimitive(true);
        Map<String, List<String>> groups = new LinkedHashMap<>();
        groups.put("inventory", expected(DefaultBases.Base.POWER_BETA).keySet().stream().filter(id -> id.startsWith("inventoryTools.") || id.equals("offhand.enabled")).toList());
        groups.put("recipe-book", List.of("recipeBook.enabled", "recipeBook.inventory", "recipeBook.pauseMenu"));
        groups.put("world-menu", List.of("worldTools.backup", "worldTools.duplicate", "worldTools.recreate"));
        groups.put("held-light", List.of("heldLight.enabled"));
        groups.put("controls", KEYS.stream().map(key -> "keys.powerbeta." + key).toList());
        for (var group : groups.entrySet()) for (int[] size : new int[][]{{320, 240}, {640, 360}}) {
          options.init(state.mc, size[0], size[1]); check(listed(options, group.getValue()).size() == group.getValue().size(), "capture has missing rows " + group.getKey());
          capture(state.mc, options, state.report.resolve("options-" + group.getKey() + "-" + size[0] + "-top.png"), size[0], size[1]);
          int maximum = Math.max(0, number(options, "contentHeight") - number(options, "bottom") + number(options, "top"));
          if (maximum > 0) { set(options, "scroll", (double)maximum); call(options, "layout"); capture(state.mc, options, state.report.resolve("options-" + group.getKey() + "-" + size[0] + "-bottom.png"), size[0], size[1]); }
        }
        state.unchangedItemsAndWorld();
      } finally { state.discard(options); }
    });
  }

  private static void hudDrafts(HudSettings400State state, DefaultBases.Base base) throws Exception {
    for (String section : List.of("subtitles", "native.6")) test("SETTINGS400 " + base.id + " " + section + " native resets, Undo and Cancel stay inside the HUD draft", () -> {
      var options = state.options(base, false); Map<String, JsonElement> parent = values(options.session()); byte[] bytes = Files.readAllBytes(PowerConfig.path());
      try {
        var hud = hud(state.mc, options); SettingsDraft draft = draft(hud);
        if (section.equals("native.6")) { select(hud, "layout"); edit(hud, draft.get(HudEditorSettings.ENTRY), new JsonPrimitive(2)); }
        select(hud, section);
        Map<String, JsonElement> expected = expected(base); String prefix = section.equals("subtitles") ? "subtitles." : OFF;
        List<String> ids = expected.keySet().stream().filter(id -> id.startsWith(prefix)).toList();
        for (String id : ids) draft.get(id).value = expected.get(id).deepCopy(); refresh(hud);
        check(((List<Setting>)call(hud, "controls")).stream().map(s -> s.id).collect(java.util.stream.Collectors.toSet()).equals(new HashSet<>(ids)), "HUD section contains missing or unrelated controls " + section);
        for (String id : ids) {
          Setting setting = draft.get(id); Map<String, JsonElement> initial = draft.snapshot();
          edit(hud, setting, different(setting, expected.get(id))); Map<String, JsonElement> edited = draft.snapshot();
          reset(hud, setting); check(setting.value.equals(expected.get(id)), "HUD reset used the wrong base for " + id);
          footer(hud, "Undo"); check(draft.snapshot().equals(edited), "native Undo failed after resetting " + id);
          footer(hud, "Undo"); check(draft.snapshot().equals(initial), "native Undo failed after editing " + id);
        }
        List<String> positions = section.equals("subtitles") ? List.of("subtitles.position", "subtitles.offsetX", "subtitles.offsetY")
            : List.of(OFF + "horizontalPosition", OFF + "horizontalPositionOffset", OFF + "verticalPosition", OFF + "verticalPositionOffset");
        for (String id : ids) edit(hud, draft.get(id), different(draft.get(id), expected.get(id)));
        Map<String, JsonElement> changed = draft.snapshot(); footer(hud, "Reset..."); resetChoice(hud, section.equals("subtitles") ? "Subtitles positions" : "Offhand positions");
        for (var entry : changed.entrySet()) check(draft.get(entry.getKey()).value.equals(positions.contains(entry.getKey()) ? expected.get(entry.getKey()) : entry.getValue()), "position reset changed the wrong scope " + entry.getKey());
        footer(hud, "Undo"); check(draft.snapshot().equals(changed), "position reset Undo changed unrelated HUD data");
        footer(hud, "Reset..."); resetChoice(hud, section.equals("subtitles") ? "Subtitles settings" : "Offhand settings");
        for (var entry : changed.entrySet()) check(draft.get(entry.getKey()).value.equals(ids.contains(entry.getKey()) ? expected.get(entry.getKey()) : entry.getValue()), "section reset changed the wrong scope " + entry.getKey());
        footer(hud, "Undo"); check(draft.snapshot().equals(changed), "section reset Undo lost its boundary");
        footer(hud, "Reset..."); key(hud, Keyboard.KEY_RIGHT); check(draft.snapshot().equals(changed), "reset modal leaked an arrow key"); resetChoice(hud, "Cancel");
        if (base == DefaultBases.Base.POWER_BETA) for (int[] size : new int[][]{{320, 240}, {640, 360}})
          capture(state.mc, hud, state.report.resolve("hud-" + section.replace('.', '-') + "-draft-" + size[0] + ".png"), size[0], size[1]);
        footer(hud, "Cancel"); check((boolean)call(get(hud, "exit"), "active"), "dirty HUD Cancel did not warn");
        key(hud, Keyboard.KEY_RIGHT); check(draft.snapshot().equals(changed), "exit modal leaked an arrow key");
        click(hud, hud.width / 2, hud.height / 2 + 18, 0);
        check(state.mc.currentScreen == options && values(options.session()).equals(parent), "HUD Discard leaked into its parent");
        check(Arrays.equals(bytes, Files.readAllBytes(PowerConfig.path())), "HUD draft reset/Undo/Discard wrote settings"); state.unchangedItemsAndWorld();
      } finally { state.discard(options); }
    });
  }

  private static void commits(HudSettings400State state) throws Exception {
    for (String action : List.of("discard", "apply", "automatic")) test("SETTINGS400 native HUD Done and parent " + action + " use the correct draft boundary", () -> {
      boolean automatic = action.equals("automatic"); var options = state.options(DefaultBases.Base.POWER_BETA, automatic);
      try {
        var original = SubtitleConfig.copy(); byte[] bytes = Files.readAllBytes(PowerConfig.path()); var hud = hud(state.mc, options); select(hud, "subtitles");
        Setting opacity = draft(hud).get("subtitles.backgroundOpacity"); JsonElement wanted = different(opacity, opacity.value);
        edit(hud, opacity, wanted); check(SubtitleConfig.current().backgroundOpacity == original.backgroundOpacity && options.session().changes() == 0, "nested HUD edit changed parent/live settings before Done");
        footer(hud, "Done"); check(state.mc.currentScreen == options && setting(options.session(), opacity.id).value.equals(wanted), "Done did not transfer its draft");
        if (!automatic) {
          check(Arrays.equals(bytes, Files.readAllBytes(PowerConfig.path())), "manual HUD Done saved before parent Apply");
          if (action.equals("discard")) {
            optionAction(options, 0); check((boolean)get(options, "confirmClose"), "parent Cancel skipped its dirty warning");
            click(options, options.width / 2 + 32, options.height / 2 + 20, 0);
            check(SubtitleConfig.current().backgroundOpacity == original.backgroundOpacity && Arrays.equals(bytes, Files.readAllBytes(PowerConfig.path())), "parent Discard retained its HUD preview");
          } else optionAction(options, 1);
        }
        if (!action.equals("discard")) check(PowerConfig.section("subtitles").get("backgroundOpacity").equals(wanted)
            && options.session().changes() == 0, "native Apply/automatic Done did not commit subtitle settings");
        state.unchangedItemsAndWorld();
      } finally { state.discard(options); }
    });
  }

  private static void previewVisibility(HudSettings400State state) throws Exception {
    test("SETTINGS400 native subtitle F3 preview and offhand Separate/Combined visibility follow local controls", () -> {
      var options = state.options(DefaultBases.Base.POWER_BETA, false); byte[] bytes = Files.readAllBytes(PowerConfig.path());
      try {
        var hud = hud(state.mc, options); var draft = draft(hud); select(hud, "subtitles");
        edit(hud, draft.get("subtitles.enabled"), new JsonPrimitive(true)); edit(hud, draft.get("subtitles.hideWithDebug"), new JsonPrimitive(false));
        hud.render(-1, -1, 0); check(!(boolean)call(element(hud, "subtitles"), "hidden"), "enabled subtitle preview is hidden");
        edit(hud, draft.get("subtitles.hideWithDebug"), new JsonPrimitive(true)); if ((int)get(hud, "scenario") != 2) toggle(hud, "f3");
        hud.render(-1, -1, 0); check((boolean)call(element(hud, "subtitles"), "hidden"), "F3-hidden preview remained visible");
        capture(state.mc, hud, state.report.resolve("hud-subtitles-f3-hidden.png"), 640, 360);
        toggle(hud, "f3"); edit(hud, draft.get("subtitles.enabled"), new JsonPrimitive(false)); hud.render(-1, -1, 0);
        check((boolean)call(element(hud, "subtitles"), "hidden"), "disabled subtitles lost their hidden outline");
        select(hud, "layout"); edit(hud, draft.get(HudEditorSettings.ENTRY), new JsonPrimitive(2)); select(hud, "native.6");
        edit(hud, draft.get(OFF + "enableVisibility"), new JsonPrimitive(true)); hud.render(-1, -1, 0); check(element(hud, "native.6") != null, "separate offhand frame is missing");
        edit(hud, draft.get(OFF + "enableVisibility"), new JsonPrimitive(false)); hud.render(-1, -1, 0); check(element(hud, "native.6") == null, "hidden separate offhand still renders");
        var hidden = draft.snapshot(); key(hud, Keyboard.KEY_RIGHT); check(draft.snapshot().equals(hidden), "arrow input moved an absent offhand element");
        edit(hud, draft.get(OFF + "enableVisibility"), new JsonPrimitive(true)); capture(state.mc, hud, state.report.resolve("hud-offhand-separate.png"), 640, 360);
        select(hud, "layout"); edit(hud, draft.get(HudEditorSettings.ENTRY), new JsonPrimitive(1)); select(hud, "combined"); hud.render(-1, -1, 0);
        check(element(hud, "combined") != null && ((List<Setting>)call(hud, "controls")).stream().noneMatch(s -> s.id.startsWith(OFF)), "Combined exposes separate offhand values");
        capture(state.mc, hud, state.report.resolve("hud-offhand-combined.png"), 320, 240);
        check(Arrays.equals(bytes, Files.readAllBytes(PowerConfig.path())) && options.session().changes() == 0, "visibility preview changed saved or parent preferences"); state.unchangedItemsAndWorld();
      } finally { state.discard(options); }
    });
  }

  private static void subtitlePlacement(HudSettings400State state) throws Exception {
    test("SETTINGS400 default subtitles avoid tweaks and first drag/nudge preserves the visible position with one Undo", () -> {
      var options = state.options(DefaultBases.Base.POWER_BETA, false); byte[] bytes = Files.readAllBytes(PowerConfig.path());
      try {
        var hud = hud(state.mc, options); hud.init(state.mc, 640, 360); var draft = draft(hud);
        select(hud, "tweaks");
        for (String id : List.of("enabled", "fakeSneak", "autoWalk", "freeLook")) edit(hud, draft.get("activeTweaks." + id), new JsonPrimitive(true));
        edit(hud, draft.get("activeTweaks.position"), new JsonPrimitive(8));
        edit(hud, draft.get("activeTweaks.offsetX"), new JsonPrimitive(0)); edit(hud, draft.get("activeTweaks.offsetY"), new JsonPrimitive(0));
        edit(hud, draft.get("activeTweaks.opacity"), new JsonPrimitive(100));
        if (!(boolean)get(get(hud, "preview"), "showTweaks")) toggle(hud, "tweaks");
        select(hud, "subtitles");
        edit(hud, draft.get("subtitles.enabled"), new JsonPrimitive(true)); edit(hud, draft.get("subtitles.hideWithDebug"), new JsonPrimitive(false));
        edit(hud, draft.get("subtitles.position"), new JsonPrimitive(8)); edit(hud, draft.get("subtitles.offsetX"), new JsonPrimitive(-1));
        edit(hud, draft.get("subtitles.offsetY"), new JsonPrimitive(-30)); edit(hud, draft.get("subtitles.textSize"), new JsonPrimitive(1));
        if ((boolean)get(hud, "snapping")) toggle(hud, "snap");
        hud.render(-1, -1, 0);
        Object caption = element(hud, "subtitles"), tweaks = element(hud, "tweaks");
        check(caption != null && tweaks != null && !(boolean)call(caption, "hidden"), "visible subtitle/tweak previews are missing");
        double x = coordinate(caption, "x"), y = coordinate(caption, "y");
        check(y + coordinate(caption, "height") <= coordinate(tweaks, "y") - 2, "default subtitle preview still overlaps Active tweaks");
        Map<String, JsonElement> original = draft.snapshot();
        capture(state.mc, hud, state.report.resolve("hud-subtitles-default-avoids-tweaks.png"), 640, 360);
        int step = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ? 10 : 1;
        for (int key : new int[]{Keyboard.KEY_RIGHT, Keyboard.KEY_UP}) {
          key(hud, key); hud.render(-1, -1, 0); Object moved = element(hud, "subtitles");
          check(close(coordinate(moved, "x"), key == Keyboard.KEY_RIGHT ? Math.min(hud.width - coordinate(caption, "width"), x + step) : x)
              && close(coordinate(moved, "y"), y - (key == Keyboard.KEY_UP ? step : 0)), "first subtitle nudge jumped from its visible position");
          check(draft.get("subtitles.offsetY").value.getAsInt() != -30, "first nudge lost the existing automatic lift");
          capture(state.mc, hud, state.report.resolve(key == Keyboard.KEY_RIGHT ? "hud-subtitles-first-right.png" : "hud-subtitles-first-up.png"), 640, 360);
          footer(hud, "Undo"); hud.render(-1, -1, 0);
          check(draft.snapshot().equals(original) && close(coordinate(element(hud, "subtitles"), "y"), y), "one nudge Undo did not restore default storage and visible placement");
        }
        int pressX = (int)Math.round(((Number)call(hud, "canvasX")).doubleValue() + (x + coordinate(caption, "width") / 2) * ((Number)call(hud, "scale")).doubleValue());
        int pressY = (int)Math.round(((Number)call(hud, "canvasY")).doubleValue() + (y + coordinate(caption, "height") / 2) * ((Number)call(hud, "scale")).doubleValue());
        for (int axis = -1; axis < 2; axis++) {
          click(hud, pressX, pressY, 0); check(get(hud, "dragging") != null, "native subtitle press did not begin a drag");
          ((local.luke.power.validation.mixin.ScreenInput)(Object)hud).power$release(pressX - (axis == 0 ? 10 : 0), pressY - (axis == 1 ? 10 : 0), 0);
          hud.render(-1, -1, 0); Object moved = element(hud, "subtitles");
          if (axis < 0) { check(draft.snapshot().equals(original), "click-only subtitle selection changed stored defaults"); continue; }
          check(axis == 0 ? coordinate(moved, "x") < x && close(coordinate(moved, "y"), y)
              : close(coordinate(moved, "x"), x) && coordinate(moved, "y") < y, "first subtitle drag jumped on its unchanged axis");
          capture(state.mc, hud, state.report.resolve(axis == 0 ? "hud-subtitles-first-horizontal-drag.png" : "hud-subtitles-first-vertical-drag.png"), 640, 360);
          footer(hud, "Undo"); hud.render(-1, -1, 0);
          check(draft.snapshot().equals(original) && close(coordinate(element(hud, "subtitles"), "y"), y), "one drag Undo did not restore default storage and visible placement");
        }
        capture(state.mc, hud, state.report.resolve("hud-subtitles-undo-default.png"), 640, 360);
        check(Arrays.equals(bytes, Files.readAllBytes(PowerConfig.path())) && options.session().changes() == 0, "subtitle movement draft changed saved or parent settings");
        state.unchangedItemsAndWorld();
      } finally { state.discard(options); }
    });
  }
  private static double coordinate(Object element, String name) throws Exception { return ((Number)call(element, name)).doubleValue(); }
  private static boolean close(double first, double second) { return Math.abs(first - second) < .01; }

  private static void runtimeVisibility(HudSettings400State state) throws Exception {
    test("SETTINGS400 actual subtitle renderer obeys F1, F3 and native inventory visibility", () -> {
      var mc = state.mc; var original = SubtitleConfig.copy(); Screen screen = mc.currentScreen;
      boolean f1 = mc.options.hideHud, f3 = mc.options.debugHud;
      try {
        for (String condition : List.of("world", "f1", "f3-hidden", "f3-visible", "inventory-hidden", "inventory-visible")) {
          var settings = SubtitleConfig.copy(); settings.enabled = true; settings.displaySeconds = 15;
          settings.hideWithDebug = condition.equals("f3-hidden"); settings.showInInventory = !condition.equals("inventory-hidden"); SubtitleConfig.preview(settings);
          mc.options.hideHud = condition.equals("f1"); mc.options.debugHud = condition.startsWith("f3-");
          mc.currentScreen = condition.startsWith("inventory-") ? new class_585(mc.player) : null;
          Subtitles.clear(); Subtitles.tick(mc); mc.soundManager.method_2013(mc.player, 1);
          mc.worldRenderer.method_1154("step.stone", mc.player.x + 1, mc.player.y, mc.player.z, 1, 1); Subtitles.tick(mc);
          var probe = new CaptionProbe(mc); Path path = state.report.resolve("caption-render-" + condition + ".png"); capture(mc, probe, path, 640, 360);
          var image = ImageIO.read(path.toFile()); int pixels = 0;
          for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) if ((image.getRGB(x, y) & 0xffffff) != 0) pixels++;
          boolean visible = Set.of("world", "f3-visible", "inventory-visible").contains(condition);
          check(visible ? pixels > 20 : pixels == 0, "actual caption visibility differs for " + condition + ", pixels=" + pixels);
        }
      } finally {
        mc.currentScreen = screen; mc.options.hideHud = f1; mc.options.debugHud = f3; SubtitleConfig.preview(original); Subtitles.clear(); Subtitles.tick(mc); mc.soundManager.method_2013(mc.player, 1);
      }
      state.unchangedItemsAndWorld();
    });
  }
  private static final class CaptionProbe extends Screen {
    private final Minecraft client;
    CaptionProbe(Minecraft client) { this.client = client; }
    @Override public void render(int x, int y, float delta) { fill(0, 0, width, height, 0xff000000); Subtitles.render(client); }
  }
}
