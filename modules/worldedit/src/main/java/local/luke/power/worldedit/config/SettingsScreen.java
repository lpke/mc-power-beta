package local.luke.power.worldedit.config;

import local.luke.power.worldedit.WorldEditor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import org.lwjgl.input.Keyboard;

public final class SettingsScreen extends Screen {
  private final Screen parent;
  private final Settings draft = WorldEditor.settings();
  private int page, left, span;
  private int direction = 1;
  private WorldOverride override;
  private net.minecraft.world.World editedWorld;
  private String error;
  private String wand = Integer.toString(draft.wandItem);
  private TextFieldWidget field;

  public SettingsScreen(Screen parent) {
    this.parent = parent;
  }

  private String on(boolean b) {
    return b ? "ON" : "OFF";
  }

  private void row(int id, int n, String text) {
    buttons.add(new ButtonWidget(id, left, 49 + n * 22, span, 20, text));
  }

  @Override
  public void init() {
    if (override == null) {
      editedWorld = minecraft.world;
      override = WorldEditor.worldOverride(minecraft);
    }
    if (field != null) wand = field.getText();
    field = null;
    buttons.clear();
    span = Math.min(370, width - 20);
    left = (width - span) / 2;
    if (page == 0) {
      row(10, 0, "WorldEdit: " + on(draft.enabled));
      row(12, 2, "This world: " + (editedWorld == null ? "Open a world first" : override.label));
    }
    if (page == 1) {
      row(0, 0, "Selection wand: " + on(draft.wandEnabled));
      field = new TextFieldWidget(this, textRenderer, left + 2, 95, span - 4, 20, wand);
      field.setMaxLength(5);
      row(1, 4, "Set wand to held item");
    }
    if (page == 2) {
      row(2, 0, "Selection outline: " + on(draft.showSelection));
      row(3, 1, "Visible through blocks: " + on(draft.throughWalls));
      row(4, 2, "Colour: " + new String[] {"Cyan", "Green", "Orange", "Purple"}[draft.color]);
      row(5, 3, "Opacity: " + draft.opacity + "%");
      row(6, 4, "Line width: " + draft.lineWidth);
    }
    if (page == 3) {
      row(7, 0, "Block limit: " + draft.blockLimit);
      row(8, 1, "Blocks per tick: " + draft.blocksPerTick);
      row(9, 2, "Undo history: " + draft.historySize + " edits");
    }
    buttons.add(new ButtonWidget(100, left, height - 51, 40, 20, "<"));
    buttons.add(new ButtonWidget(101, left + span - 40, height - 51, 40, 20, ">"));
    buttons.add(new ButtonWidget(102, left, height - 26, span / 2 - 2, 20, "Done"));
    buttons.add(
        new ButtonWidget(103, left + span / 2 + 2, height - 26, span / 2 - 2, 20, "Cancel"));
  }

  @Override
  protected void buttonClicked(ButtonWidget b) {
    error = null;
    switch (b.id) {
      case 0 -> draft.wandEnabled = !draft.wandEnabled;
      case 1 -> {
        if (minecraft.player != null && minecraft.player.inventory.getSelectedItem() != null) {
          wand = Integer.toString(minecraft.player.inventory.getSelectedItem().itemId);
          field = null;
        } else error = "Hold the item you want to use as the wand.";
      }
      case 2 -> draft.showSelection = !draft.showSelection;
      case 3 -> draft.throughWalls = !draft.throughWalls;
      case 4 -> draft.color = Math.floorMod(draft.color + direction, 4);
      case 5 -> draft.opacity = Math.floorMod(draft.opacity / 10 - 1 + direction, 10) * 10 + 10;
      case 6 -> draft.lineWidth = Math.floorMod(draft.lineWidth - 1 + direction, 4) + 1;
      case 7 ->
          draft.blockLimit =
              direction > 0
                  ? (draft.blockLimit >= 262144 ? 4096 : draft.blockLimit * 2)
                  : (draft.blockLimit <= 4096 ? 262144 : draft.blockLimit / 2);
      case 8 ->
          draft.blocksPerTick =
              direction > 0
                  ? (draft.blocksPerTick >= 8192 ? 64 : draft.blocksPerTick * 2)
                  : (draft.blocksPerTick <= 64 ? 8192 : draft.blocksPerTick / 2);
      case 9 ->
          draft.historySize = Math.floorMod(draft.historySize / 5 - 1 + direction, 20) * 5 + 5;
      case 10 -> draft.enabled = !draft.enabled;
      case 12 -> {
        if (editedWorld != null && !editedWorld.isRemote)
          override =
              WorldOverride.values()[
                  Math.floorMod(override.ordinal() + direction, WorldOverride.values().length)];
      }
      case 100 -> page = (page + 3) % 4;
      case 101 -> page = (page + 1) % 4;
      case 102 -> {
        if (field != null) wand = field.getText();
        try {
          draft.wandItem = Integer.parseInt(wand);
          WorldEditor.apply(draft);
          if (editedWorld != null && minecraft.world == editedWorld && !editedWorld.isRemote)
            WorldEditor.worldOverride(minecraft, override);
          minecraft.setScreen(parent);
        } catch (java.io.IOException | IllegalArgumentException e) {
          error = "Could not save: check the wand ID and settings.";
          WorldEditor.LOG.error(error, e);
        }
        return;
      }
      case 103 -> {
        minecraft.setScreen(parent);
        return;
      }
    }
    init();
  }

  @Override
  protected void keyPressed(char c, int code) {
    if (code == Keyboard.KEY_ESCAPE) minecraft.setScreen(parent);
    else if (field != null) field.keyPressed(c, code);
    else if (code == Keyboard.KEY_LEFT || code == Keyboard.KEY_RIGHT) {
      page = Math.floorMod(page + (code == Keyboard.KEY_LEFT ? -1 : 1), 4);
      init();
    }
  }

  @Override
  protected void mouseClicked(int x, int y, int button) {
    if (field != null) field.mouseClicked(x, y, button);
    if (button != 0 && button != 1) return;
    for (Object v : java.util.List.copyOf(buttons)) {
      ButtonWidget b = (ButtonWidget) v;
      if (b.isMouseOver(minecraft, x, y) && (button == 0 || b.id < 100)) {
        minecraft.soundManager.method_2009("random.click", 1, 1);
        direction = button == 1 ? -1 : 1;
        try {
          buttonClicked(b);
        } finally {
          direction = 1;
        }
        return;
      }
    }
  }

  @Override
  public void tick() {
    if (field != null) field.tick();
  }

  @Override
  public void render(int x, int y, float delta) {
    renderBackground();
    drawCenteredTextWithShadow(textRenderer, "WorldEdit Beta", width / 2, 10, 0xFFFFFF);
    drawCenteredTextWithShadow(
        textRenderer,
        new String[] {"Access", "Selection wand", "Selection visuals", "Edit limits"}[page],
        width / 2,
        29,
        0xDDDDDD);
    if (field != null) {
      drawStringWithShadow(textRenderer, "Wand item ID, wooden axe = 271", left, 81, 0xCCCCCC);
      field.render();
    }
    drawCenteredTextWithShadow(
        textRenderer,
        page == 0
            ? "Master switch, then world override, then creative integration."
            : page == 1
                ? "Left-click pos1; right-click pos2. //help lists commands."
                : page == 2
                    ? "Corners are red and green; the region uses your colour."
                    : "Edits run in batches. //cancel restores an unfinished edit.",
        width / 2,
        height - 68,
        0xAAAAAA);
    drawCenteredTextWithShadow(textRenderer, (page + 1) + " / 4", width / 2, height - 45, 0xFFFFFF);
    if (error != null)
      drawCenteredTextWithShadow(textRenderer, error, width / 2, height - 82, 0xFF5555);
    super.render(x, y, delta);
  }
}
