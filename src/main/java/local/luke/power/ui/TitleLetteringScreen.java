package local.luke.power.ui;

import com.google.gson.*;
import java.util.*;
import local.luke.power.config.Setting;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Tile painting and native 3D preview share an isolated, undoable title draft. */
public final class TitleLetteringScreen extends DraftEditorScreen {
  private static final String LINES = "logo.logo.lines", PALETTE = "logo.blocks.map";
  private final ItemIcons icons = new ItemIcons();
  private final Map<Integer, ItemCatalogue.Entry> blocks = new HashMap<>();
  private final TitlePreview.Renderer renderer;
  private final Setting brush = temporary("brush", "Brush block", Setting.Kind.TEXT, 1, 1, 1);
  private final Setting columns = temporary("columns", "Grid columns", Setting.Kind.INTEGER, 38, 1, 80);
  private final Setting rows = temporary("rows", "Grid rows", Setting.Kind.INTEGER, 5, 1, 12);
  private final Setting presetName = new Setting("titleEditor.name", "", "", "", "Preset name", "", Setting.Kind.TEXT,
      new JsonPrimitive(""), new JsonPrimitive(""), 0, 0, 1, List.of(), false);
  private final List<Setting> rotation = new ArrayList<>();
  private int tab, presetPage, lastCell = -1, strokeBlock, strokeButton;
  private boolean painting, replaying;

  public TitleLetteringScreen(PowerOptionsScreen parent) {
    super(parent, new SettingsDraft(parent.session().settings(), HudEditorSettings::lettering));
    renderer = TitlePreview.factory == null ? null : TitlePreview.factory.get();
    for (var entry : ItemCatalogue.entries(ItemCatalogue.Domain.BLOCK)) if (TitlePreview.renderableBlock.test(entry.id())) blocks.put(entry.id(), entry);
    for (int axis = 0; axis < 3; axis++) {
      JsonElement angle = draft.get("logo.logo.rotation").defaultValue.getAsJsonArray().get(axis);
      rotation.add(new Setting("titleEditor.rotation" + axis, "", "", "", "Rotate " + "XYZ".charAt(axis), "",
          Setting.Kind.DECIMAL, angle, angle, -360, 360, 1, List.of(), false));
    }
    columns.validator(v -> TitleTiles.resize(lines(), v.getAsInt(), lines().size()));
    rows.validator(v -> TitleTiles.resize(lines(), TitleTiles.width(lines()), v.getAsInt()));
    presetName.validator(v -> { if (v.getAsString().isBlank() || v.getAsString().length() > 48
        || v.getAsString().chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Use a name up to 48 characters"); });
    afterEdit();
  }
  private static Setting temporary(String id, String label, Setting.Kind kind, int value, int min, int max) {
    return new Setting("titleEditor." + id, "", "", "", label, "", kind,
        kind == Setting.Kind.TEXT ? new JsonPrimitive(Integer.toString(value)) : new JsonPrimitive(value),
        new JsonPrimitive(value), min, max, 1, List.of(), false);
  }
  protected String title() { return "Title lettering"; }
  private JsonArray lines() { return draft.get(LINES).value.getAsJsonArray(); }
  private JsonObject palette() { return draft.get(PALETTE).value.getAsJsonObject(); }
  private JsonArray presets() { return draft.get("logo.presets").value.getAsJsonArray(); }
  private JsonArray swatches() { return draft.get("logo.swatches").value.getAsJsonArray(); }
  protected List<Setting> controls() {
    if (tab == 0) return List.of(brush, columns, rows);
    if (tab == 2) return List.of();
    List<Setting> controls = new ArrayList<>(rotation);
    draft.settings().stream().filter(s -> !Set.of(LINES, PALETTE, "logo.logo.rotation", "logo.presets", "logo.swatches", "logo.selectedPreset", "logo.retroBase").contains(s.id)).forEach(controls::add);
    return controls;
  }
  protected int controlsBottom() {
    int desired = tab == 2 ? 130 : controls().size() * 23;
    int minimumPreview = Math.min(170, Math.max(90, height / 3));
    return Math.max(controlsTop() + (tab == 2 ? 42 : 23), Math.min(controlsTop() + desired, previewBottom() - minimumPreview - 12));
  }
  protected boolean usesSlider(Setting s) { return s == rows || s == columns || rotation.contains(s) || !s.id.startsWith("titleEditor.") && super.usesSlider(s); }
  protected boolean resettable(Setting s) { return s == rows || s == columns || rotation.contains(s) || !s.id.startsWith("titleEditor."); }
  protected String display(Setting s) {
    if (s == brush) { var block = blocks.get(s.value.getAsInt()); return block == null ? "Choose..." : block.name(); }
    return super.display(s);
  }
  protected void afterEdit() {
    columns.value = new JsonPrimitive(TitleTiles.width(lines())); rows.value = new JsonPrimitive(lines().size());
    JsonArray angles = draft.get("logo.logo.rotation").value.getAsJsonArray();
    for (int axis = 0; axis < 3; axis++) rotation.get(axis).value = angles.get(axis).deepCopy();
  }
  protected void valueChanged(Setting s) {
    if (s == columns || s == rows) draft.get(LINES).value = TitleTiles.resize(lines(),
        s == columns ? s.value.getAsInt() : TitleTiles.width(lines()), s == rows ? s.value.getAsInt() : lines().size());
    else if (rotation.contains(s)) draft.get("logo.logo.rotation").value.getAsJsonArray().set(rotation.indexOf(s), s.value.deepCopy());
    markCustom();
    if (s.id.equals("logo.logo.animation.enabled") && !s.value.getAsBoolean()) replaying = false;
    afterEdit();
  }
  private void markCustom() {
    draft.get("logo.selectedPreset").value = new JsonPrimitive("");
    draft.get("logo.retroBase").value = TitleSelection.design(draft.settings());
  }
  protected void edit(Setting s, int direction, int y) {
    if (s == brush) { click(); pick(); return; }
    super.edit(s, direction, y);
  }
  protected void renderControl(Setting s, int y, int mx, int my) {
    if (s != brush) { super.renderControl(s, y, mx, my); return; }
    button("", controlX(), y, controlWidth(), 18, mx, my, true);
    var block = blocks.get(brush.value.getAsInt());
    String name = fit(display(s), controlWidth() - 28);
    int start = controlX() + (controlWidth() - textRenderer.getWidth(name) - 22) / 2;
    if (block != null) icons.draw(this, minecraft, block, start, y + 1);
    text(name, start + 22, y + 5, inside(mx, my, controlX(), y, controlWidth(), 18) ? 0xffffa0 : 0xe0e0e0);
  }
  private int tabWidth() { return Math.min(115, (span() - 8) / 3); }
  protected void renderToolbar(int x, int y) {
    String[] labels = {"Paint", "Appearance", "Presets"}; int w = tabWidth();
    for (int i = 0; i < labels.length; i++) tabButton(labels[i], left() + i * (w + 4), 30, w, x, y, tab == i);
  }
  protected boolean toolbarClick(int x, int y, int button) {
    if (button != 0) return false;
    int w = tabWidth();
    for (int i = 0; i < 3; i++) if (inside(x, y, left() + i * (w + 4), 30, w, 18)) { click(); tab = i; stopGesture(); controlsChanged(); return true; }
    return false;
  }
  private int swatchColumns() { return Math.max(1, span() / 22); }
  private int reservedSwatches() { return ((swatches().size() + 1 + swatchColumns() - 1) / swatchColumns()) * 22; }
  private int gridTop() { return previewTop() + Math.max(8, Math.min(62, (previewBottom() - reservedSwatches() - previewTop()) / 3)); }
  private int tileSize() { return Math.max(1, Math.min(18, Math.min(span() / TitleTiles.width(lines()), Math.max(1, previewBottom() - reservedSwatches() - gridTop() - 6) / lines().size()))); }
  private int gridLeft() { return width / 2 - tileSize() * TitleTiles.width(lines()) / 2; }
  private int swatchTop() { return gridTop() + tileSize() * lines().size() + 6; }
  private void pick() {
    minecraft.setScreen(new ItemPickerScreen(this, "Brush block", ItemCatalogue.Domain.BLOCK,
        List.of(brush.value.getAsString()), false, new ItemPickerScreen.Options(true, entry -> blocks.containsKey(entry.id())), chosen ->
          brush.value = new JsonPrimitive(Integer.toString(ItemCatalogue.resolve(ItemCatalogue.Domain.BLOCK, chosen.get(0)).id()))));
  }
  protected void previewClick(int x, int y, int button) {
    if (swatchClick(x, y, button)) { click(); return; }
    int tile = tileSize(), cols = TitleTiles.width(lines());
    if ((button != 0 && button != 1) || !inside(x, y, gridLeft(), gridTop(), cols * tile, lines().size() * tile)) return;
    draft.checkpoint(); painting = true; lastCell = -1; strokeButton = button;
    strokeBlock = button == 1 ? 0 : brush.value.getAsInt(); paintAt(x, y);
  }
  private void paintAt(int x, int y) {
    int tile = tileSize(), cols = TitleTiles.width(lines());
    if (!inside(x, y, gridLeft(), gridTop(), cols * tile, lines().size() * tile)) { lastCell = -1; return; }
    int col = (x - gridLeft()) / tile, row = (y - gridTop()) / tile, cell = row * cols + col;
    if (cell == lastCell) return;
    try {
      int fromX = lastCell < 0 ? col : lastCell % cols, fromY = lastCell < 0 ? row : lastCell / cols;
      int steps = Math.max(Math.abs(col - fromX), Math.abs(row - fromY));
      for (int step = 0; step <= steps; step++) TitleTiles.paint(lines(), palette(), steps == 0 ? col : fromX + Math.round((col - fromX) * step / (float)steps),
          steps == 0 ? row : fromY + Math.round((row - fromY) * step / (float)steps), strokeBlock);
      markCustom();
      lastCell = cell; error = "";
    } catch (IllegalArgumentException e) { error = e.getMessage(); stopGesture(); }
  }
  protected void finishGesture(int x, int y, int button) {
    if (painting && button == strokeButton) { paintAt(x, y); stopGesture(); }
  }
  protected void stopGesture() { painting = false; lastCell = -1; }
  public void tick() {
    super.tick();
    if (renderer != null && (replaying || draft.get("logo.logo.animation.enabled").value.getAsBoolean())) { renderer.tick(); if (renderer.settled()) replaying = false; }
  }
  protected String resetLabel() { return "Reset logo"; }
  protected String extraFooterLabel() { return "Play animation"; }
  protected void extraFooterAction() { if (renderer != null) { renderer.replay(); replaying = true; } }
  protected void resetFinished() { extraFooterAction(); }
  protected void reset() { draft.settings().stream().filter(s -> !Set.of("logo.presets", "logo.swatches").contains(s.id)).forEach(Setting::reset); }
  protected String hint() { return tab == 2 ? "Click a preset to load it. Right-click to remove it. Done saves your changes."
      : "Left-click to paint. Right-click to erase. Drag either button. + saves the brush as a swatch."; }
  protected void renderPreview(int mx, int my, float delta) {
    if (tab == 2) renderPresets(mx, my);
    fill(left(), previewTop(), right(), previewBottom(), 0x80202020);
    if (renderer != null) {
      Map<Character, Integer> map = new LinkedHashMap<>(); palette().entrySet().forEach(e -> map.put(e.getKey().charAt(0), e.getValue().getAsInt()));
      String[] lines = new String[lines().size()]; for (int i = 0; i < lines.length; i++) lines[i] = lines().get(i).getAsString();
      JsonArray angles = draft.get("logo.logo.rotation").value.getAsJsonArray(); float[] rotation = {angles.get(0).getAsFloat(), angles.get(1).getAsFloat(), angles.get(2).getAsFloat()};
      renderer.draw(left(), previewTop(), span(), gridTop() - previewTop() - 3, width, height, lines, map, rotation,
          draft.get("logo.blocks.inverted").value.getAsBoolean(), draft.get("logo.logo.shadow").value.getAsBoolean(),
          replaying || draft.get("logo.logo.animation.enabled").value.getAsBoolean(), draft.get("logo.logo.animation.speed").value.getAsFloat(), delta);
    }
    if (painting) { if (Mouse.isButtonDown(strokeButton) && !modal()) paintAt(mx, my); else stopGesture(); }
    int tile = tileSize(), cols = TitleTiles.width(lines());
    for (int row = 0; row < lines().size(); row++) for (int col = 0; col < cols; col++) {
      int x = gridLeft() + col * tile, y = gridTop() + row * tile;
      fill(x, y, x + tile, y + tile, 0xff555555); fill(x + 1, y + 1, x + tile - 1, y + tile - 1, 0xff161616);
      var entry = blocks.get(TitleTiles.block(lines(), palette(), col, row));
      if (entry != null) {
        GL11.glPushMatrix();
        try { GL11.glTranslatef(x + 1, y + 1, 0); GL11.glScalef(Math.max(1, tile - 2) / 16f, Math.max(1, tile - 2) / 16f, 1); icons.draw(this, minecraft, entry, 0, 0); }
        finally { GL11.glPopMatrix(); }
      }
      if (!modal() && inside(mx, my, x, y, tile, tile)) fill(x, y, x + tile, y + tile, 0x60ffffaa);
    }
    renderSwatches(mx, my);
  }
  private boolean hasSwatch() { for (JsonElement id : swatches()) if (id.getAsInt() == brush.value.getAsInt()) return true; return false; }
  private boolean swatchClick(int x, int y, int button) {
    if ((button != 0 && button != 1) || y < swatchTop()) return false;
    int columns = swatchColumns(), index = (y - swatchTop()) / 22 * columns + (x - left()) / 22;
    if (x < left() || x >= left() + columns * 22 || index > swatches().size() || (x - left()) % 22 >= 20 || (y - swatchTop()) % 22 >= 20) return false;
    if (index == swatches().size()) {
      if (button == 0 && swatches().size() < 16 && !hasSwatch()) { draft.checkpoint(); swatches().add(brush.value.getAsInt()); }
    } else if (button == 1) { draft.checkpoint(); swatches().remove(index); }
    else brush.value = new JsonPrimitive(swatches().get(index).getAsString());
    return true;
  }
  private void renderSwatches(int mx, int my) {
    for (int i = 0; i <= swatches().size(); i++) {
      int x = left() + i % swatchColumns() * 22, y = swatchTop() + i / swatchColumns() * 22;
      boolean add = i == swatches().size(); button(add ? "+" : "", x, y, 20, 20, mx, my, !add || swatches().size() < 16 && !hasSwatch());
      if (!add) {
        var entry = blocks.get(swatches().get(i).getAsInt()); if (entry != null) icons.draw(this, minecraft, entry, x + 2, y + 2);
        if (swatches().get(i).getAsInt() == brush.value.getAsInt()) fill(x + 2, y + 18, x + 18, y + 19, 0xffffff88);
      }
      if (inside(mx, my, x, y, 20, 20) && !modal()) tooltip(add ? "Save brush as a swatch" : "Use this brush. Right-click to remove the swatch.", mx, my);
    }
  }
  private int presetRows() { return Math.max(1, (controlsBottom() - controlsTop() - 24) / 22); }
  private boolean presetClick(int x, int y, int button) {
    if (button == 0 && inside(x, y, left() + span() / 2, controlsTop(), span() / 2 - 10, 18)) {
      if (presets().size() >= 24) { error = "Save up to 24 title presets. Right-click a preset to remove it."; return true; }
      presetName.value = new JsonPrimitive(""); draft.checkpoint();
      openInline(presetName, () -> {
        JsonObject preset = new JsonObject(), values = new JsonObject();
        preset.addProperty("id", UUID.randomUUID().toString()); preset.addProperty("name", presetName.value.getAsString().strip());
        TitleTiles.DESIGN_KEYS.forEach(id -> values.add(id, draft.get(id).value.deepCopy())); preset.add("values", values); presets().add(preset);
      }, left() + span() / 2, controlsTop(), span() / 2 - 10); return true;
    }
    if (button == 0 && inside(x, y, left(), controlsTop(), 48, 18)) { presetPage = Math.max(0, presetPage - 1); return true; }
    if (button == 0 && inside(x, y, left() + 52, controlsTop(), 48, 18)) { presetPage = Math.min(Math.max(0, (presets().size() - 1) / presetRows()), presetPage + 1); return true; }
    if ((button == 0 || button == 1) && inside(x, y, left(), controlsTop() + 24, span(), Math.max(0, controlsBottom() - controlsTop() - 24))) {
      int index = presetPage * presetRows() + (y - controlsTop() - 24) / 22;
      if (index < presets().size()) {
        draft.checkpoint();
        JsonObject preset = presets().get(index).getAsJsonObject();
        if (button == 1) {
          if (draft.get("logo.selectedPreset").value.equals(preset.get("id"))) markCustom();
          presets().remove(index);
        } else {
          if (draft.get("logo.selectedPreset").value.getAsString().isEmpty()) draft.get("logo.retroBase").value = TitleSelection.design(draft.settings());
          preset.getAsJsonObject("values").entrySet().forEach(e -> draft.get(e.getKey()).value = e.getValue().deepCopy());
          draft.get("logo.selectedPreset").value = preset.get("id").deepCopy();
        }
        afterEdit();
      }
      return true;
    }
    return false;
  }
  protected void mouseClicked(int x, int y, int button) {
    if (!modal() && inline.press(x, y, button)) return;
    if (!modal() && tab == 2 && presetClick(x, y, button)) { click(); return; }
    super.mouseClicked(x, y, button);
  }
  private void renderPresets(int x, int y) {
    button("<", left(), controlsTop(), 48, 18, x, y, presetPage > 0);
    button(">", left() + 52, controlsTop(), 48, 18, x, y, (presetPage + 1) * presetRows() < presets().size());
    if (inline.editing(presetName)) inline.render(this, left() + span() / 2, controlsTop(), span() / 2 - 10, x, y, controlsTop(), controlsBottom());
    else button("Save preset...", left() + span() / 2, controlsTop(), span() / 2 - 10, 18, x, y, presets().size() < 24);
    presetPage = Math.min(presetPage, Math.max(0, (presets().size() - 1) / presetRows()));
    for (int row = 0; row < presetRows(); row++) {
      int index = presetPage * presetRows() + row; if (index >= presets().size()) break;
      button(fit(presets().get(index).getAsJsonObject().get("name").getAsString(), span() - 20), left(), controlsTop() + 24 + row * 22, span() - 10, 18, x, y, true);
    }
    if (presets().isEmpty()) text("No saved presets. Right-click removes a saved preset.", left() + 4, controlsTop() + 28, 0xaaaaaa);
  }
}
