package local.luke.power.building.config;

import java.util.*;
import java.util.function.*;
import local.luke.power.fastplace.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** A single draft across grouped pages. Native Controls and this screen share key objects. */
public final class SettingsScreen extends Screen {
  private record Row(Supplier<String> label, Runnable click) {}

  private record Page(String title, List<Row> rows, String hint, boolean filters) {}

  private final Screen parent;
  private final Settings draft = Config.current().copy();
  private final LinkedHashMap<KeyBinding, Integer> keys = new LinkedHashMap<>();
  private final List<Page> pages = new ArrayList<>();
  private KeyBinding capture;
  private int page, left, span, capacity;
  private int direction = 1;
  private String error, blackText, whiteText;
  private TextFieldWidget black, white;

  public SettingsScreen(Screen parent) {
    this.parent = parent;
    blackText = local.luke.power.fastplace.config.Settings.formatFilters(draft.placement.blacklist);
    whiteText = local.luke.power.fastplace.config.Settings.formatFilters(draft.placement.whitelist);
  }

  private String on(boolean b) {
    return b ? "ON" : "OFF";
  }

  private Row toggle(String label, BooleanSupplier get, Runnable change) {
    return new Row(() -> label + ": " + on(get.getAsBoolean()), change);
  }

  private Row key(KeyBinding k, String label) {
    keys.putIfAbsent(k, k.code);
    return new Row(
        () ->
            capture == k
                ? "Press key or mouse; Esc clears"
                : label + ": " + local.luke.power.flexible.Input.name(keys.get(k)),
        () -> capture = k);
  }

  private void group(String name, String hint, Row... rows) {
    int count = (rows.length + capacity - 1) / capacity;
    for (int i = 0; i < count; i++)
      pages.add(
          new Page(
              name + (count > 1 ? " " + (i + 1) + "/" + count : ""),
              Arrays.asList(rows).subList(i * capacity, Math.min(rows.length, (i + 1) * capacity)),
              hint,
              false));
  }

  private void buildPages() {
    pages.clear();
    var p = draft.placement;
    var f = draft.flexible;
    var s = draft.sneak;
    var slab = draft.slabs;
    group(
        "Fast placement",
        "Hold right-click to continue placing.",
        toggle("Fast place", () -> p.enabled, () -> p.setEnabled(!p.enabled)),
        key(FastPlace.KEY, "Toggle key"),
        new Row(
            () -> "Blocks per tick: " + p.attemptsPerTick,
            () -> p.attemptsPerTick = Math.floorMod(p.attemptsPerTick - 1 + direction, 16) + 1),
        new Row(
            () -> "Slabs: " + p.slabMode.label,
            () ->
                p.slabMode = SlabMode.values()[Math.floorMod(p.slabMode.ordinal() + direction, 3)]),
        toggle(
            "Remember orientation",
            () -> p.rememberOrientation,
            () -> p.rememberOrientation = !p.rememberOrientation));
    group(
        "Placement restrictions",
        "Face, plane, layer, column, line or diagonal.",
        toggle(
            "Placement restriction",
            () -> p.restrictionEnabled,
            () -> p.restrictionEnabled = !p.restrictionEnabled),
        new Row(
            () -> "Restriction mode: " + p.restrictionMode,
            () ->
                p.restrictionMode =
                    RestrictionMode.values()[
                        Math.floorMod(p.restrictionMode.ordinal() + direction, 6)]),
        toggle(
            "Tie restriction to fast place",
            () -> p.restrictionTiedToFast,
            () -> p.restrictionTiedToFast = !p.restrictionTiedToFast));
    pages.add(
        new Page(
            "Placement filters",
            List.of(
                new Row(
                    () -> "Item filter: " + p.listMode,
                    () ->
                        p.listMode =
                            local.luke.power.fastplace.config.Settings.ListMode.values()[
                                Math.floorMod(p.listMode.ordinal() + direction, 3)])),
            "Item IDs, e.g. 1, 44:2. Empty whitelist allows nothing.",
            true));
    group(
        "Slab completion",
        "Complete matching slabs through adjacent faces.",
        toggle("Adjacent slab completion", () -> slab.enabled, () -> slab.enabled = !slab.enabled),
        key(local.luke.power.slabplacement.Keys.ALL[0], "Toggle key"),
        toggle(
            "Toggle messages",
            () -> slab.announceToggle,
            () -> slab.announceToggle = !slab.announceToggle));
    group(
        "Flexible placement",
        "Hold a modifier and aim at a block face.",
        toggle("Flexible placement", () -> f.enabled, () -> f.enabled = !f.enabled),
        key(local.luke.power.flexible.Keys.ALL[0], "Toggle key"),
        key(local.luke.power.flexible.Keys.ALL[1], "Offset, hold"),
        key(local.luke.power.flexible.Keys.ALL[2], "Adjacent, hold"),
        key(local.luke.power.flexible.Keys.ALL[3], "Rotation, hold"),
        key(local.luke.power.flexible.Keys.ALL[4], "Reverse, hold"),
        key(local.luke.power.flexible.Keys.ALL[5], "Into face, hold"),
        toggle(
            "Place against containers",
            () -> f.placeAgainstContainers,
            () -> f.placeAgainstContainers = !f.placeAgainstContainers));
    String[] colors = {"Blue", "Cyan", "Orange", "Green"};
    group(
        "Placement visuals",
        "Green previews can be placed; red previews are blocked.",
        toggle("Target overlay", () -> f.showOverlay, () -> f.showOverlay = !f.showOverlay),
        toggle("Destination preview", () -> f.showPreview, () -> f.showPreview = !f.showPreview),
        new Row(
            () -> "Overlay colour: " + colors[f.overlayColor],
            () -> f.overlayColor = Math.floorMod(f.overlayColor + direction, 4)),
        new Row(
            () -> "Overlay opacity: " + f.overlayOpacity + "%",
            () ->
                f.overlayOpacity =
                    Math.floorMod(f.overlayOpacity / 10 - 1 + direction, 9) * 10 + 10));
    group(
        "Movement",
        "Fake sneak protects walking edges at normal speed.",
        toggle("Auto-walk", () -> draft.autoWalk, () -> draft.autoWalk = !draft.autoWalk),
        key(local.luke.power.autowalk.AutoWalk.KEY, "Auto-walk key"),
        toggle("Fake sneak", () -> s.enabled, () -> s.enabled = !s.enabled),
        key(local.luke.power.fakesneak.Keys.ALL[0], "Fake sneak key"),
        toggle(
            "Fake sneak messages",
            () -> s.announceToggle,
            () -> s.announceToggle = !s.announceToggle));
    group(
        "Camera",
        "Look around without turning the player.",
        toggle("Free look", () -> draft.freeLook, () -> draft.freeLook = !draft.freeLook),
        new Row(
            () -> "Activation: " + (draft.freeLookToggle ? "Toggle" : "Hold"),
            () -> draft.freeLookToggle = !draft.freeLookToggle),
        new Row(
            () -> "Perspective: " + draft.freeLookPerspective.label,
            () -> draft.freeLookPerspective = draft.freeLookPerspective.next()),
        toggle(
            "Follow current third-person view",
            () -> draft.freeLookFollowThirdPerson,
            () -> draft.freeLookFollowThirdPerson = !draft.freeLookFollowThirdPerson),
        key(local.luke.power.building.camera.FreeLook.KEY, "Free look key"));
    var hotbar = draft.hotbar;
    group(
        "Hotbar swapping",
        "Hold the row modifier and press 1, 2 or 3.",
        toggle("Hotbar swap", () -> hotbar.swap, () -> hotbar.swap = !hotbar.swap),
        key(local.luke.power.building.hotbar.Hotbars.BASE, "Preview / row modifier"),
        toggle(
            "Modifier + 1/2/3 swaps rows",
            () -> hotbar.numberRowKeys,
            () -> hotbar.numberRowKeys = !hotbar.numberRowKeys),
        key(local.luke.power.building.hotbar.Hotbars.ROWS[0], "Top row, direct"),
        key(local.luke.power.building.hotbar.Hotbars.ROWS[1], "Middle row, direct"),
        key(local.luke.power.building.hotbar.Hotbars.ROWS[2], "Bottom row, direct"));
    group(
        "Hotbar scrolling",
        "Hold, scroll to select a row, release to swap.",
        toggle("Hotbar scroll", () -> hotbar.scroll, () -> hotbar.scroll = !hotbar.scroll),
        key(local.luke.power.building.hotbar.Hotbars.SCROLL, "Scroll modifier"),
        toggle(
            "Reverse scroll direction",
            () -> hotbar.reverseScroll,
            () -> hotbar.reverseScroll = !hotbar.reverseScroll),
        toggle(
            "Remember selected row",
            () -> hotbar.rememberRow,
            () -> hotbar.rememberRow = !hotbar.rememberRow),
        new Row(
            () -> "Starting row: " + (hotbar.selectedRow + 1),
            () -> hotbar.selectedRow = Math.floorMod(hotbar.selectedRow + direction, 3)));
    group(
        "Hotbar preview",
        "Preview placement uses scaled screen pixels.",
        toggle("Show row previews", () -> hotbar.overlay, () -> hotbar.overlay = !hotbar.overlay),
        new Row(
            () -> "Alignment: " + hotbar.alignment,
            () ->
                hotbar.alignment =
                    local.luke.power.building.hotbar.HotbarSettings.Alignment.values()[
                        Math.floorMod(hotbar.alignment.ordinal() + direction, 5)]),
        new Row(
            () -> "Horizontal offset: " + hotbar.offsetX,
            () -> hotbar.offsetX = Math.floorMod(hotbar.offsetX + 4 * direction, 260)),
        new Row(
            () -> "Vertical offset: " + hotbar.offsetY,
            () -> hotbar.offsetY = Math.floorMod(hotbar.offsetY + 4 * direction, 260)));
    group(
        "Transport",
        "Boat steering and minecart changes apply in singleplayer.",
        toggle(
            "Boat steering and collision protection",
            () -> draft.boatSteering,
            () -> draft.boatSteering = !draft.boatSteering),
        toggle(
            "Faster minecarts",
            () -> draft.fastMinecarts,
            () -> draft.fastMinecarts = !draft.fastMinecarts));
    group(
        "Mining",
        "Remove the block-break delay in singleplayer.",
        toggle(
            "Click mining forever",
            () -> draft.clickMining,
            () -> draft.clickMining = !draft.clickMining));
  }

  private void rememberText() {
    if (black != null) {
      blackText = black.getText();
      whiteText = white.getText();
    }
  }

  @Override
  public void init() {
    rememberText();
    black = white = null;
    buttons.clear();
    span = Math.min(390, width - 20);
    left = (width - span) / 2;
    capacity = Math.max(3, Math.min(7, (height - 122) / 22));
    buildPages();
    page = Math.min(page, pages.size() - 1);
    Page current = pages.get(page);
    for (int i = 0; i < current.rows.size(); i++)
      buttons.add(
          new ButtonWidget(i, left, 48 + i * 22, span, 20, current.rows.get(i).label.get()));
    if (current.filters) {
      black = new TextFieldWidget(this, textRenderer, left + 2, 91, span - 4, 20, blackText);
      white = new TextFieldWidget(this, textRenderer, left + 2, 135, span - 4, 20, whiteText);
      black.setMaxLength(4096);
      white.setMaxLength(4096);
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
    if (b.id == 103) {
      minecraft.setScreen(parent);
      return;
    }
    if (b.id == 102) {
      rememberText();
      try {
        draft.placement.blacklist = local.luke.power.fastplace.config.Settings.parseFilters(blackText);
        draft.placement.whitelist = local.luke.power.fastplace.config.Settings.parseFilters(whiteText);
        Config.apply(draft);
        keys.forEach((k, v) -> k.code = v);
        minecraft.options.save();
        minecraft.setScreen(parent);
      } catch (java.io.IOException | IllegalArgumentException e) {
        error =
            e instanceof java.io.IOException
                ? "Could not save settings. See game log."
                : e.getMessage();
        Config.LOG.error(error, e);
      }
      return;
    }
    if (b.id == 100 || b.id == 101) {
      capture = null;
      page = Math.floorMod(page + (b.id == 100 ? -1 : 1), pages.size());
    } else pages.get(page).rows.get(b.id).click.run();
    init();
  }

  @Override
  protected void keyPressed(char c, int code) {
    if (capture != null) {
      keys.put(capture, code == Keyboard.KEY_ESCAPE ? 0 : code);
      capture = null;
      init();
    } else if (code == Keyboard.KEY_ESCAPE) minecraft.setScreen(parent);
    else if (black != null) {
      black.keyPressed(c, code);
      white.keyPressed(c, code);
    } else if (code == Keyboard.KEY_LEFT || code == Keyboard.KEY_RIGHT) {
      page = Math.floorMod(page + (code == Keyboard.KEY_LEFT ? -1 : 1), pages.size());
      init();
    }
  }

  @Override
  protected void mouseClicked(int x, int y, int button) {
    if (capture != null) {
      keys.put(capture, button - 100);
      capture = null;
      init();
      return;
    }
    if (black != null) {
      black.mouseClicked(x, y, button);
      white.mouseClicked(x, y, button);
    }
    if (button != 0 && button != 1) return;
    for (Object value : List.copyOf(buttons)) {
      ButtonWidget b = (ButtonWidget) value;
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
    if (black != null) {
      black.tick();
      white.tick();
    }
  }

  private void field(TextFieldWidget f, int y) {
    GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
    GL11.glEnable(GL11.GL_SCISSOR_TEST);
    GL11.glScissor(
        left * minecraft.displayWidth / width,
        (height - y - 22) * minecraft.displayHeight / height,
        span * minecraft.displayWidth / width,
        24 * minecraft.displayHeight / height);
    f.render();
    GL11.glPopAttrib();
  }

  @Override
  public void render(int x, int y, float delta) {
    renderBackground();
    Page current = pages.get(page);
    drawCenteredTextWithShadow(textRenderer, "BuildingFeatures", width / 2, 10, 0xFFFFFF);
    drawCenteredTextWithShadow(textRenderer, current.title, width / 2, 29, 0xDDDDDD);
    if (black != null) {
      drawStringWithShadow(textRenderer, "Blacklist", left, 79, 0xCCCCCC);
      drawStringWithShadow(textRenderer, "Whitelist", left, 123, 0xCCCCCC);
      field(black, 91);
      field(white, 135);
    }
    String hint = current.hint;
    if (current.title.equals("Fast placement"))
      hint =
          switch (draft.placement.slabMode) {
            case CONTINUOUS -> "Hold to build through half, double, then the next layer.";
            case DOUBLE -> "Place a full block using two slabs at a time.";
            case MATCH_FIRST -> "Keep the starting layer and half/completion action.";
          };
    if (textRenderer.getWidth(hint) < width - 12)
      drawCenteredTextWithShadow(textRenderer, hint, width / 2, height - 68, 0xAAAAAA);
    drawCenteredTextWithShadow(
        textRenderer, (page + 1) + " / " + pages.size(), width / 2, height - 45, 0xFFFFFF);
    if (error != null)
      drawCenteredTextWithShadow(textRenderer, error, width / 2, height - 81, 0xFF5555);
    super.render(x, y, delta);
  }
}
