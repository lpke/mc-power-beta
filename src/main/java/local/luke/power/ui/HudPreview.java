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
      String offsetX, String offsetY, boolean hidden) {
    Element(String id, String label, double x, double y, double width, double height, String offsetX, String offsetY) {
      this(id, label, x, y, width, height, offsetX, offsetY, false);
    }
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
  private boolean sampledItem;
  boolean suppressDebug, showToast = true, showChat = true, showTweaks = true;
  private final DebugPreview debugPreview;
  HudPreview(SettingsDraft draft) {
    this.draft = draft;
    debugPreview = new DebugPreview(draft);
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
  void draw(UiScreen screen, Minecraft mc, int width, int height, int scenario, String selected) { draw(screen, mc, width, height, scenario, selected, false); }
  void draw(UiScreen screen, Minecraft mc, int width, int height, int scenario, String selected, boolean background) {
    elements.clear();
    if (!sampledItem) {
      sampledItem = true;
      if (mc.player != null && mc.player.inventory.getSelectedItem() != null) {
        var item = mc.player.inventory.getSelectedItem();
        try { samples.set(0, ItemCatalogue.resolve(ItemCatalogue.Domain.FILTER, item.itemId + ":" + item.getDamage())); }
        catch (IllegalArgumentException ignored) { /* Keep the registry-validated sample. */ }
      }
    }
    GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glAlphaFunc(GL11.GL_GREATER, 0);
    for (int i = 0; i < 5; i++) {
      if (i == 4 && !bool(HudEditorSettings.ITEM_NAMES)) continue;
      String prefix = mode() == 1 ? SIMPLE : nativePrefix(i);
      if (mode() != 0 && !bool(prefix + "enableVisibility")) continue;
      String message = samples.get(0).name(); int textWidth = mc.textRenderer.getWidth(message);
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
    if (bool(HudEditorSettings.HUD + "disableCrosshair")) {
      GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR);
      texture(screen, mc, "/gui/icons.png", width / 2 - 7, height / 2 - 7, 0, 0, 16, 16);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      elements.add(new Element("crosshair", "Crosshair", width / 2 - 7, height / 2 - 7, 16, 16, null, null));
    }
    if (bool(HudEditorSettings.HUD + "drawXboxXAndYButtons")) {
      texture(screen, mc, "/assets/power_hud/button_icons.png", 10, height - 30, 40, 0, 20, 20);
      screen.text("Crafting", 31, height - 24, 0xffffff);
      texture(screen, mc, "/assets/power_hud/button_icons.png", 78, height - 30, 60, 0, 20, 20);
      screen.text("Inventory", 99, height - 24, 0xffffff);
      elements.add(new Element("controller", "Controller prompts", 10, height - 30, 145, 20, null, null));
    }
    if (showToast && bool(HudEditorSettings.TOAST)) {
      int x = width - 160;
      texture(screen, mc, "/achievement/bg.png", x, 0, 96, 202, 160, 32);
      screen.text("Achievement get!", x + 30, 7, 0xffff00);
      screen.text("Getting Wood", x + 30, 18, 0xffffff);
      icons.draw(screen, mc, samples.get(2), x + 8, 8);
      elements.add(new Element("toast", "Achievement toast", x, 0, 160, 32, null, null));
    }
    if (showChat) {
      String[] messages = {"<Player> Hello!", "<Player> Welcome back."};
      int count = Math.min(messages.length, Math.max(0, integer(HudEditorSettings.HUD + "chatHistorySize")));
      for (int i = 0; i < count; i++) {
        int y = height - 48 - i * 9;
        screen.rectangle(2, y - 1, Math.min(width - 2, 322), y + 8, 0x80000000);
        screen.text(messages[i], 2, y, 0xffffff);
      }
      int chatWidth = Math.min(width - 4, 320);
      screen.rectangle(2, height - 14, 2 + chatWidth, height - 2, 0x80000000);
      boolean cursor = !bool("interface.blinkingChatCursor") || System.nanoTime() / 500_000_000L % 2 == 0;
      screen.text("> Hello" + (cursor ? "|" : ""), 4, height - 12, 0xffffff);
      elements.add(new Element("chat", "Chat", 2, height - 49 - count * 9, chatWidth, 47 + count * 9, null, null));
    }
    for (int cluster = 0; cluster < 4; cluster++) {
      String prefix = "infoHud.clusters." + cluster + ".";
      InfoHudSettings.Cluster state = cluster(cluster);
      InfoHudFrame frame = InfoHudFrame.at(state, InfoHudData.sample(), width, height, mc.textRenderer::getWidth);
      List<String> lines = frame.lines();
      boolean visible = bool("infoHud.enabled") && state.enabled && state.opacity > 0
          && (!state.hideWithDebug || scenario != 2) && !lines.isEmpty();
      int textWidth = lines.isEmpty() ? mc.textRenderer.getWidth("Info cluster " + (cluster + 1)) : frame.textWidth();
      int rows = Math.max(1, lines.size());
      var layout = InfoHudLayout.at(width, height, textWidth, rows, state);
      if (visible) InfoHudRenderer.draw(mc, frame);
      elements.add(new Element("cluster." + cluster, "Info cluster " + (cluster + 1), layout.x(), layout.y(),
          textWidth * state.textSize, rows * 10 * state.textSize, prefix + "offsetX", prefix + "offsetY", !visible));
    }
    StatusSettings status = status();
    if (showTweaks && status.enabled && status.opacity > 0) {
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
    if (scenario == 2 && background && bool("power_controls:userinterface.disableDebugEntityIdTags")) EntityTagPreview.draw(screen, mc, width, height);
    if (scenario == 2 && !suppressDebug) debug(screen, mc, width);
    for (Element element : elements) if (!element.id.equals("layout") && element.id.equals(selected) && !element.hidden) {
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
    var lines = debugPreview.draw(mc, width).stream().filter(line -> !line.id().isEmpty()).toList();
    double right = lines.stream().mapToDouble(line -> line.x() + line.width()).max().orElse(20);
    double bottom = lines.stream().mapToDouble(line -> line.y() + 10 * draft.get("interface.debugTextSize").value.getAsDouble()).max().orElse(12);
    elements.add(new Element("debug", "F3 information", 2, 2, Math.max(18, right - 2), Math.max(10, bottom - 2), null, null));
  }
}
