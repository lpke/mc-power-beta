package local.luke.power.ui;

import java.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.hud.*;
import local.luke.power.status.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Texture-pack-aware sample HUD. Its data never reaches inventories or gameplay toggles. */
final class HudPreview {
  record Element(String id, String label, double x, double y, double width, double height,
      String offsetX, String offsetY) {
    boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
  }
  static final String NATIVE = "power_hud:hudpositions.";
  static final String SIMPLE = NATIVE + "SIMPLE_HUD_POSITION_CONFIG.";
  static final String[] PARTS = {"HOTBAR", "HEARTS", "ARMOR", "OXYGEN", "OVERLAY_MESSAGE"};
  static final String[] NAMES = {"Hotbar", "Hearts", "Armour", "Oxygen", "Messages"};
  private final SettingsDraft draft;
  private final List<Element> elements = new ArrayList<>();
  private final ItemIcons icons = new ItemIcons();
  private final List<ItemCatalogue.Entry> samples = new ArrayList<>();
  private final InfoHudSettings.Cluster[] clusters = new InfoHudSettings.Cluster[4];
  private StatusSettings status;
  HudPreview(SettingsDraft draft) {
    this.draft = draft;
    for (int id : new int[]{1, 3, 5, 45, 50, 257, 267, 260, 345})
      samples.add(ItemCatalogue.resolve(ItemCatalogue.Domain.ITEM, Integer.toString(id)));
    rebuild();
  }
  void rebuild() {
    for (int i = 0; i < 4; i++) { clusters[i] = new InfoHudSettings.Cluster(); populate(clusters[i], "infoHud.clusters." + i + "."); }
    status = new StatusSettings(); populate(status, "activeTweaks.");
  }
  int integer(String id) { return draft.get(id).value.getAsInt(); }
  boolean bool(String id) { return draft.get(id).value.getAsBoolean(); }
  String string(String id) { return draft.get(id).value.getAsString(); }
  int mode() { return integer(HudEditorSettings.ENTRY); }
  static String nativePrefix(int element) { return NATIVE + "ADVANCED_HUD_POSITIONS_CONFIG." + PARTS[element] + "_POSITION_CONFIG."; }
  List<Element> elements() { return List.copyOf(elements); }
  private void texture(UiScreen screen, Minecraft mc, String path, int x, int y, int u, int v, int w, int h) {
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.textureManager.getTextureId(path)); GL11.glColor4f(1, 1, 1, 1);
    screen.drawTexture(x, y, u, v, w, h);
  }
  void draw(UiScreen screen, Minecraft mc, int width, int height, int scenario, String selected) {
    elements.clear();
    GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    for (int i = 0; i < 5; i++) {
      if (i == 3 && scenario != 1) continue;
      String prefix = mode() == 1 ? SIMPLE : nativePrefix(i);
      if (mode() != 0 && !bool(prefix + "enableVisibility")) continue;
      String message = "Now playing: C418 - Sweden"; int textWidth = mc.textRenderer.getWidth(message);
      int x = NativeHudLayout.baseX(width, i, textWidth), y = NativeHudLayout.baseY(height, i);
      int w = i == 0 ? 182 : i == 4 ? textWidth : 81, h = i == 0 ? 22 : 9;
      if (mode() != 0) {
        x += NativeHudLayout.xOffset(width, i, mode(), integer(prefix + "horizontalPosition"), textWidth) + integer(prefix + "horizontalPositionOffset");
        y += NativeHudLayout.yOffset(height, i, mode(), integer(prefix + "verticalPosition"),
            bool(NATIVE + "putStatusBarIconsBelowHotbar"), bool(NATIVE + "putOverlayMessagesBelowHotbar")) + integer(prefix + "verticalPositionOffset");
      }
      if (i == 0) {
        texture(screen, mc, "/gui/gui.png", x, y, 0, 0, 182, 22);
        texture(screen, mc, "/gui/gui.png", x - 1, y - 1, 0, 22, 24, 22);
        for (int slot = 0; slot < samples.size(); slot++) icons.draw(screen, mc, samples.get(slot), x + 3 + slot * 20, y + 3);
      } else if (i == 4) screen.text(message, x, y, 0xffffff);
      else for (int slot = 0; slot < 10; slot++) {
        int u = i == 1 ? slot < 7 ? 52 : 16 : i == 2 ? slot < 6 ? 34 : 16 : 16;
        int v = i == 1 ? 0 : i == 2 ? 9 : 18;
        texture(screen, mc, "/gui/icons.png", x + slot * 8, y, u, v, 9, 9);
      }
      elements.add(new Element(mode() == 1 ? "combined" : "native." + i, mode() == 1 ? "Combined HUD" : NAMES[i],
          x, y, w, h, mode() == 0 ? null : prefix + "horizontalPositionOffset", mode() == 0 ? null : prefix + "verticalPositionOffset"));
    }
    if (mode() == 1) {
      List<Element> parts = List.copyOf(elements);
      if (!parts.isEmpty()) {
        double x = parts.stream().mapToDouble(Element::x).min().orElse(0), y = parts.stream().mapToDouble(Element::y).min().orElse(0);
        double right = parts.stream().mapToDouble(e -> e.x + e.width).max().orElse(0), bottom = parts.stream().mapToDouble(e -> e.y + e.height).max().orElse(0);
        elements.clear(); elements.add(new Element("combined", "Combined HUD", x, y, right - x, bottom - y,
            SIMPLE + "horizontalPositionOffset", SIMPLE + "verticalPositionOffset"));
      }
    }
    for (int cluster = 0; cluster < 4; cluster++) {
      String prefix = "infoHud.clusters." + cluster + ".";
      InfoHudSettings.Cluster state = cluster(cluster);
      if (!bool("infoHud.enabled") || !state.enabled || state.opacity == 0 || state.hideWithDebug && scenario == 2) continue;
      List<String> lines = new ArrayList<>();
      if (state.coordinates || state.direction) lines.add(HudText.location("XYZ: 128 / 64 / -32", "South (+Z)", state.coordinates, state.direction));
      if (state.biome) lines.add("Biome: Forest");
      if (state.timeOfDay) lines.add("Time: 10:30 AM");
      if (state.day) lines.add("Day: 12");
      if (state.chunk) lines.add("Chunk: 8 / -2");
      if (state.fps) lines.add("FPS: 120");
      if (lines.isEmpty()) continue;
      int textWidth = lines.stream().mapToInt(mc.textRenderer::getWidth).max().orElse(1);
      var layout = InfoHudLayout.at(width, height, textWidth, lines.size(), state);
      GL11.glPushMatrix();
      try {
        GL11.glTranslated(layout.x(), layout.y(), 0); GL11.glScaled(state.textSize, state.textSize, 1);
        for (int row = 0; row < lines.size(); row++) screen.text(lines.get(row), layout.lineX(textWidth, mc.textRenderer.getWidth(lines.get(row))), row * 10, state.argb());
      } finally { GL11.glPopMatrix(); }
      elements.add(new Element("cluster." + cluster, "Cluster " + (cluster + 1), layout.x(), layout.y(),
          textWidth * state.textSize, lines.size() * 10 * state.textSize, prefix + "offsetX", prefix + "offsetY"));
    }
    StatusSettings status = status();
    if (status.enabled && status.opacity > 0) {
      List<String> lines = new ArrayList<>();
      for (var tweak : local.luke.power.input.TweakIndicators.Tweak.values()) if (status.includes(tweak))
        lines.add(switch (tweak) { case AUTO_WALK -> "Auto-walk"; case AUTO_MINE -> "Auto-mine";
          case FREE_LOOK -> "Free look"; default -> Catalog.words(tweak.name()); });
      if (!lines.isEmpty()) {
        int textWidth = lines.stream().mapToInt(mc.textRenderer::getWidth).max().orElse(1);
        var layout = StatusLayout.at(width, height, textWidth, lines.size(), status);
        for (int row = 0; row < lines.size(); row++) screen.text(lines.get(row), layout.lineX(textWidth, mc.textRenderer.getWidth(lines.get(row))), layout.y() + row * 10, status.argb());
        elements.add(new Element("tweaks", "Active tweaks", layout.x(), layout.y(), textWidth, lines.size() * 10, "activeTweaks.offsetX", "activeTweaks.offsetY"));
      }
    }
    String versionPrefix = "power_controls:userinterface.versionTextConfig.";
    if (scenario != 2 && bool(versionPrefix + "showVersionTextIngame")) {
      String version = bool(versionPrefix + "enableCustomVersionText") ? string(versionPrefix + "customVersionText") : "Minecraft Beta 1.7.3";
      screen.text(version, 2, 2, 0xffffff);
      if (bool(versionPrefix + "unlicensedCopy")) screen.text("Unlicensed copy", 2, 12, 0xffffff);
      elements.add(new Element("version", "Version label", 2, 2, Math.max(8, mc.textRenderer.getWidth(version)), bool(versionPrefix + "unlicensedCopy") ? 20 : 10, null, null));
    }
    if (scenario == 2) debug(screen, mc, width);
    for (Element element : elements) if (element.id.equals(selected)) {
      int x = (int)element.x - 2, y = (int)element.y - 2, r = (int)Math.ceil(element.x + element.width) + 2, b = (int)Math.ceil(element.y + element.height) + 2;
      screen.rectangle(x, y, r, y + 1, 0xffffff66); screen.rectangle(x, b - 1, r, b, 0xffffff66);
      screen.rectangle(x, y, x + 1, b, 0xffffff66); screen.rectangle(r - 1, y, r, b, 0xffffff66);
    }
  }
  InfoHudSettings.Cluster cluster(int index) {
    return clusters[index];
  }
  private StatusSettings status() { return status; }
  private void populate(Object value, String prefix) {
    try {
      for (var field : value.getClass().getFields()) {
        var setting = draft.settings().stream().filter(s -> s.id.equals(prefix + field.getName())).findFirst().orElse(null);
        if (setting != null) field.set(value, Catalog.JSON.fromJson(setting.value, field.getType()));
      }
    } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
  }
  private void debug(UiScreen screen, Minecraft mc, int width) {
    List<DebugOverlay.Line> lines = new ArrayList<>();
    String[] nativeLines = {"Minecraft Beta 1.7.3 (120 fps)", "C: 256/1024. F: 64, O: 12, E: 2", "E: 8/32. B: 0, I: 32"};
    for (int row = 0; row < nativeLines.length; row++) lines.add(new DebugOverlay.Line(nativeLines[row], 2, 2 + row * 10, 0));
    int coordinates = integer("power_hud:config.coordinateDisplay");
    if (coordinates != 1) {
      String[] location = coordinates == 2 ? new String[]{"x: -182.137", "y: 372.069", "z: -41.925"}
          : new String[]{"x: 128.000", bool("interface.useFeetYCoordinates") ? "y: 64.000" : "y: 65.620", "z: -32.000"};
      for (int row = 0; row < location.length; row++) lines.add(new DebugOverlay.Line(location[row], 2, 64 + row * 8, 0));
    }
    int extra = integer("power_compat:config.overlayAdditionsYOffset");
    String[] ids = {"addTotalPlayTimeToDebugOverlay", "addBlockLightToDebugOverlay", "addBiomeToDebugOverlay",
        "addDayCounterToDebugOverlay", "addSlimeChunkToDebugOverlay", "addFacingToDebugOverlay"};
    String[] labels = {"Play Time: 3 hours", "Block light: 0", "Biome: Forest", "Day: 12", "Slime Chunk: false", "Facing: south (+Z)"};
    int[] ys = {96, 112, 120, 128, 136, 144};
    for (int i = 0; i < ids.length; i++) if (bool("power_compat:config." + ids[i]))
      lines.add(new DebugOverlay.Line(labels[i], 2, ys[i] + extra, 0));
    if (bool("interface.debugTimeOfDay")) lines.add(new DebugOverlay.Line("Time: 10:30 AM", 2, 144, 0));
    if (bool("entityculling:config.showF3Info")) lines.add(new DebugOverlay.Line("Culling: 8 visible, 24 hidden", 2, 160 + integer("entityculling:config.f3InfoYOffset"), 0));
    if (bool("power_environment:config.MUSIC_CONFIG.overlayForMusicInDebug"))
      lines.add(new DebugOverlay.Line("Music: C418 - Sweden", 2, 152 + integer("power_environment:config.MUSIC_CONFIG.overlayForMusicInDebugYOffset"), 0));
    if (bool("power_client_fixes:config.enableDebugMenuWorldSeed")) lines.add(new DebugOverlay.Line("Seed: 123456789", 2, 88, 0));
    lines.add(new DebugOverlay.Line("64-bit Java", Math.max(2, width - mc.textRenderer.getWidth("64-bit Java") - 2), 2, 0));
    int color = local.luke.power.light.LightSettings.rgb(string("interface.debugTextColor"));
    for (var line : DebugOverlay.arrange(lines, width, mc.textRenderer::getWidth)) screen.text(line.text(), line.x(), line.y(), color);
    elements.add(new Element("debug", "F3 information", 2, 2, Math.min(width - 4, 210), 154 + Math.max(0, extra), null, null));
  }
}
