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
  private final List<Setting> rotation = new ArrayList<>();
  private int tab, presetPage, lastCell = -1, strokeBlock;
  private boolean painting;

  public TitleLetteringScreen(PowerOptionsScreen parent) {
    super(parent, new SettingsDraft(parent.session().settings(), HudEditorSettings::lettering));
    renderer = TitlePreview.factory == null ? null : TitlePreview.factory.get();
    for (var entry : ItemCatalogue.entries(ItemCatalogue.Domain.BLOCK)) blocks.put(entry.id(), entry);
    for (int axis = 0; axis < 3; axis++) rotation.add(temporary("rotation" + axis,
        "Rotate " + "XYZ".charAt(axis), Setting.Kind.DECIMAL, axis == 0 ? 15 : 0, -360, 360));
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
  protected List<Setting> controls() {
    if (tab == 0) return List.of(brush, columns, rows);
    if (tab == 2) return List.of();
    List<Setting> controls = new ArrayList<>(rotation);
    draft.settings().stream().filter(s -> !Set.of(LINES, PALETTE, "logo.logo.rotation", "logo.presets").contains(s.id)).forEach(controls::add);
    return controls;
  }
  protected boolean usesSlider(Setting s) { return !s.id.startsWith("titleEditor.") && super.usesSlider(s); }
  protected String display(Setting s) {
    if (s == brush) { var block = blocks.get(s.value.getAsInt()); return block == null ? "Choose..." : block.name(); }
    return super.display(s);
  }
  protected void afterEdit() {
    columns.value = new JsonPrimitive(TitleTiles.width(lines())); rows.value = new JsonPrimitive(lines().size());
    JsonArray angles = draft.get("logo.logo.rotation").value.getAsJsonArray();
    for (int axis = 0; axis < 3; axis++) rotation.get(axis).value = angles.get(axis).deepCopy();
  }
  protected void edit(Setting s, int direction) {
    if (s == brush) { pick(-1, -1); return; }
    if (s == columns || s == rows || rotation.contains(s)) {
      Setting value = temporary("value", s.label, s.kind, 0, (int)s.min, (int)s.max); value.value = s.value.deepCopy();
      minecraft.setScreen(new ValueScreen(value, this, () -> {
        draft.checkpoint();
        if (s == columns || s == rows) draft.get(LINES).value = TitleTiles.resize(lines(),
            s == columns ? value.value.getAsInt() : TitleTiles.width(lines()), s == rows ? value.value.getAsInt() : lines().size());
        else draft.get("logo.logo.rotation").value.getAsJsonArray().set(rotation.indexOf(s), value.value.deepCopy());
        afterEdit();
      }));
      return;
    }
    super.edit(s, direction);
  }
  private int tabWidth() { return Math.min(115, (span() - 8) / 3); }
  protected void renderToolbar(int x, int y) {
    String[] labels = {"Paint", "Appearance", "Presets"}; int w = tabWidth();
    for (int i = 0; i < labels.length; i++) button((tab == i ? "> " : "") + labels[i], left() + i * (w + 4), 30, w, 18, x, y, true);
  }
  protected boolean toolbarClick(int x, int y, int button) {
    if (button != 0) return false;
    int w = tabWidth();
    for (int i = 0; i < 3; i++) if (inside(x, y, left() + i * (w + 4), 30, w, 18)) { tab = i; stopGesture(); controlsChanged(); return true; }
    return false;
  }
  private int gridTop() { return previewTop() + Math.min(62, Math.max(30, (previewBottom() - previewTop()) / 2)); }
  private int tileSize() { return Math.max(2, Math.min(18, Math.min(span() / TitleTiles.width(lines()), Math.max(1, previewBottom() - gridTop()) / lines().size()))); }
  private int gridLeft() { return width / 2 - tileSize() * TitleTiles.width(lines()) / 2; }
  private void pick(int col, int row) {
    int current = col < 0 ? brush.value.getAsInt() : TitleTiles.block(lines(), palette(), col, row);
    minecraft.setScreen(new ItemPickerScreen(this, "Title block", ItemCatalogue.Domain.BLOCK,
        List.of(Integer.toString(current > 0 ? current : brush.value.getAsInt())), false, chosen -> {
          int id = ItemCatalogue.resolve(ItemCatalogue.Domain.BLOCK, chosen.get(0)).id();
          brush.value = new JsonPrimitive(Integer.toString(id));
          if (col >= 0) { draft.checkpoint(); TitleTiles.paint(lines(), palette(), col, row, id); }
        }));
  }
  protected void previewClick(int x, int y, int button) {
    if (tab == 2 && presetClick(x, y, button)) return;
    int tile = tileSize(), cols = TitleTiles.width(lines());
    if (!inside(x, y, gridLeft(), gridTop(), cols * tile, lines().size() * tile)) return;
    int col = (x - gridLeft()) / tile, row = (y - gridTop()) / tile;
    if (button == 1) { pick(col, row); return; }
    if (button != 0) return;
    draft.checkpoint(); painting = true; lastCell = -1;
    strokeBlock = TitleTiles.block(lines(), palette(), col, row) == 0 ? brush.value.getAsInt() : 0;
    paintAt(x, y);
  }
  private void paintAt(int x, int y) {
    int tile = tileSize(), cols = TitleTiles.width(lines());
    if (!inside(x, y, gridLeft(), gridTop(), cols * tile, lines().size() * tile)) return;
    int col = (x - gridLeft()) / tile, row = (y - gridTop()) / tile, cell = row * cols + col;
    if (cell == lastCell) return;
    try { TitleTiles.paint(lines(), palette(), col, row, strokeBlock); lastCell = cell; error = ""; }
    catch (IllegalArgumentException e) { error = e.getMessage(); stopGesture(); }
  }
  protected void stopGesture() { painting = false; lastCell = -1; }
  public void tick() { if (renderer != null && draft.get("logo.logo.animation.enabled").value.getAsBoolean()) renderer.tick(); }
  protected String resetLabel() { return "Reset logo"; }
  protected String resetQuestion() { return "Restore the default Minecraft logo and appearance?"; }
  protected void reset() { draft.settings().stream().filter(s -> !s.id.equals("logo.presets")).forEach(Setting::reset); }
  protected String hint() { return tab == 2 ? "Click a preset to load it. Right-click to remove it. Done saves your changes."
      : "Click to paint or erase. Drag to paint. Right-click a tile to choose its block."; }
  protected void renderPreview(int mx, int my, float delta) {
    if (tab == 2) renderPresets(mx, my);
    fill(left(), previewTop(), right(), previewBottom(), 0x80202020);
    if (renderer != null) {
      Map<Character, Integer> map = new LinkedHashMap<>(); palette().entrySet().forEach(e -> map.put(e.getKey().charAt(0), e.getValue().getAsInt()));
      String[] lines = new String[lines().size()]; for (int i = 0; i < lines.length; i++) lines[i] = lines().get(i).getAsString();
      JsonArray angles = draft.get("logo.logo.rotation").value.getAsJsonArray(); float[] rotation = {angles.get(0).getAsFloat(), angles.get(1).getAsFloat(), angles.get(2).getAsFloat()};
      renderer.draw(left(), previewTop(), span(), gridTop() - previewTop() - 3, width, height, lines, map, rotation,
          draft.get("logo.blocks.inverted").value.getAsBoolean(), draft.get("logo.logo.shadow").value.getAsBoolean(),
          draft.get("logo.logo.animation.enabled").value.getAsBoolean(), draft.get("logo.logo.animation.speed").value.getAsFloat(), delta);
    }
    if (painting) { if (Mouse.isButtonDown(0) && !modal()) paintAt(mx, my); else stopGesture(); }
    int tile = tileSize(), cols = TitleTiles.width(lines());
    for (int row = 0; row < lines().size(); row++) for (int col = 0; col < cols; col++) {
      int x = gridLeft() + col * tile, y = gridTop() + row * tile;
      fill(x, y, x + tile, y + tile, 0xff555555); fill(x + 1, y + 1, x + tile - 1, y + tile - 1, 0xff161616);
      int block = TitleTiles.block(lines(), palette(), col, row);
      var entry = blocks.get(block);
      if (entry != null) {
        GL11.glPushMatrix();
        try { GL11.glTranslatef(x + 1, y + 1, 0); GL11.glScalef(Math.max(1, tile - 2) / 16f, Math.max(1, tile - 2) / 16f, 1); icons.draw(this, minecraft, entry, 0, 0); }
        finally { GL11.glPopMatrix(); }
      }
      if (!modal() && inside(mx, my, x, y, tile, tile)) fill(x, y, x + tile, y + tile, 0x60ffffaa);
    }
  }
  private int presetRows() { return Math.max(1, (controlsBottom() - controlsTop() - 24) / 22); }
  private boolean presetClick(int x, int y, int button) {
    if (button == 0 && inside(x, y, left() + span() / 2, controlsTop(), span() / 2 - 10, 18)) {
      if (presets().size() >= 24) { error = "Save up to 24 title presets. Right-click a preset to remove it."; return true; }
      Setting name = new Setting("titleEditor.name", "", "", "", "Preset name", "", Setting.Kind.TEXT,
          new JsonPrimitive(""), new JsonPrimitive(""), 0, 0, 1, List.of(), false);
      name.validator(v -> { if (v.getAsString().isBlank() || v.getAsString().length() > 48
          || v.getAsString().chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Use a name up to 48 characters"); });
      minecraft.setScreen(new ValueScreen(name, this, () -> {
        draft.checkpoint(); JsonObject preset = new JsonObject(), values = new JsonObject();
        preset.addProperty("id", UUID.randomUUID().toString()); preset.addProperty("name", name.value.getAsString().strip());
        TitleTiles.DESIGN_KEYS.forEach(id -> values.add(id, draft.get(id).value.deepCopy())); preset.add("values", values); presets().add(preset);
      })); return true;
    }
    if (button == 0 && inside(x, y, left(), controlsTop(), 48, 18)) { presetPage = Math.max(0, presetPage - 1); return true; }
    if (button == 0 && inside(x, y, left() + 52, controlsTop(), 48, 18)) { presetPage = Math.min(Math.max(0, (presets().size() - 1) / presetRows()), presetPage + 1); return true; }
    if ((button == 0 || button == 1) && inside(x, y, left(), controlsTop() + 24, span(), controlsBottom() - controlsTop() - 24)) {
      int index = presetPage * presetRows() + (y - controlsTop() - 24) / 22;
      if (index < presets().size()) {
        draft.checkpoint();
        if (button == 1) presets().remove(index);
        else presets().get(index).getAsJsonObject().getAsJsonObject("values").entrySet().forEach(e -> draft.get(e.getKey()).value = e.getValue().deepCopy());
        afterEdit();
      }
      return true;
    }
    return false;
  }
  protected void mouseClicked(int x, int y, int button) {
    if (!modal() && tab == 2 && presetClick(x, y, button)) return;
    super.mouseClicked(x, y, button);
  }
  private void renderPresets(int x, int y) {
    button("<", left(), controlsTop(), 48, 18, x, y, presetPage > 0);
    button(">", left() + 52, controlsTop(), 48, 18, x, y, (presetPage + 1) * presetRows() < presets().size());
    button("Save preset...", left() + span() / 2, controlsTop(), span() / 2 - 10, 18, x, y, presets().size() < 24);
    for (int row = 0; row < presetRows(); row++) {
      int index = presetPage * presetRows() + row;
      if (index >= presets().size()) break;
      button(fit(presets().get(index).getAsJsonObject().get("name").getAsString(), span() - 20), left(), controlsTop() + 24 + row * 22, span() - 10, 18, x, y, true);
    }
    if (presets().isEmpty()) text("No saved presets. Right-click removes a saved preset.", left() + 4, controlsTop() + 28, 0xaaaaaa);
  }
}
