package local.luke.creative.config;

import java.util.*;
import java.util.function.*;
import local.luke.creative.Keys;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widgets.Button;
import net.minecraft.client.options.KeyBinding;
import org.lwjgl.input.Keyboard;

public final class SettingsScreen extends Screen {
  private record Row(Supplier<String> label, Runnable change) {}

  private record Page(String title, List<Row> rows, String hint) {}

  private final Screen parent;
  private final Settings draft = Config.current().copy();
  private final Map<KeyBinding, Integer> keys = new LinkedHashMap<>();
  private final List<Page> pages = new ArrayList<>();
  private KeyBinding capture;
  private int page, left, span, capacity;
  private int direction = 1;
  private String error;

  public SettingsScreen(Screen parent) {
    this.parent = parent;
    for (KeyBinding k : Keys.ALL) keys.put(k, k.key);
  }

  private Row toggle(String label, BooleanSupplier value, Runnable change) {
    return new Row(() -> label + ": " + (value.getAsBoolean() ? "ON" : "OFF"), change);
  }

  private Row key(KeyBinding key, String label) {
    return new Row(
        () ->
            capture == key
                ? "Press key or mouse; Esc clears"
                : label + ": " + Keys.name(keys.get(key)),
        () -> capture = key);
  }

  private Row number(
      String label, IntSupplier get, IntConsumer set, int min, int max, int step, String suffix) {
    return new Row(
        () ->
            label
                + ": "
                + (suffix.equals(" blocks")
                    ? String.format(java.util.Locale.ROOT, "%.1f", get.getAsInt() / 10.0)
                    : get.getAsInt())
                + suffix,
        () -> {
          int next = get.getAsInt() + step * direction;
          set.accept(next > max ? min : next < min ? max : next);
        });
  }

  private void group(String name, String hint, Row... rows) {
    int count = (rows.length + capacity - 1) / capacity;
    for (int i = 0; i < count; i++)
      pages.add(
          new Page(
              name + (count > 1 ? " " + (i + 1) + "/" + count : ""),
              Arrays.asList(rows).subList(i * capacity, Math.min(rows.length, (i + 1) * capacity)),
              hint));
  }

  @Override
  public void init() {
    buttons.clear();
    pages.clear();
    span = Math.min(390, width - 20);
    left = (width - span) / 2;
    capacity = Math.max(3, Math.min(7, (height - 122) / 22));
    group(
        "Creative flight",
        "Double-tap jump to fly. Right-click values to decrease.",
        toggle("Creative flight", () -> draft.flight, () -> draft.flight = !draft.flight),
        number(
            "Flight speed", () -> draft.flightSpeed, v -> draft.flightSpeed = v, 25, 400, 25, "%"),
        number("Flight glide", () -> draft.glide, v -> draft.glide = v, 0, 5, 1, " / 5"),
        toggle(
            "Landing stops flight",
            () -> draft.landStopsFlight,
            () -> draft.landStopsFlight = !draft.landStopsFlight),
        number(
            "Double-tap window",
            () -> draft.doubleTapTicks,
            v -> draft.doubleTapTicks = v,
            2,
            20,
            1,
            " ticks"));
    group(
        "Flight sprint",
        "Choose whether the sprint key toggles or holds the boost.",
        toggle(
            "Flight sprint",
            () -> draft.sprintFlight,
            () -> draft.sprintFlight = !draft.sprintFlight),
        key(Keys.SPRINT, "Sprint key"),
        new Row(
            () -> "Sprint activation: " + (draft.sprintToggle ? "Toggle" : "Hold"),
            () -> draft.sprintToggle = !draft.sprintToggle),
        number(
            "Sprint speed",
            () -> draft.sprintMultiplier,
            v -> draft.sprintMultiplier = v,
            100,
            400,
            25,
            "%"));
    group(
        "Gamemodes",
        "Hold the modifier, press cycle, then release the modifier.",
        toggle(
            "Gamemode picker", () -> draft.modePicker, () -> draft.modePicker = !draft.modePicker),
        key(Keys.MODIFIER, "Picker modifier"),
        key(Keys.PICKER, "Cycle gamemode"));
    group(
        "Spectator",
        "Scroll to adjust speed. Spectators pass through blocks.",
        toggle(
            "Scroll adjusts speed",
            () -> draft.spectatorScroll,
            () -> draft.spectatorScroll = !draft.spectatorScroll),
        number(
            "Starting spectator speed",
            () -> draft.spectatorSpeed,
            v -> draft.spectatorSpeed = v,
            0,
            400,
            10,
            "%"),
        number(
            "Scroll speed step",
            () -> draft.spectatorScrollStep,
            v -> draft.spectatorScrollStep = v,
            1,
            100,
            1,
            "%"),
        toggle(
            "Show speed changes",
            () -> draft.showSpectatorSpeed,
            () -> draft.showSpectatorSpeed = !draft.showSpectatorSpeed));
    group(
        "Creative interaction",
        "Modern creative reach is 5 blocks.",
        number(
            "Block reach",
            () -> draft.blockReach,
            v -> draft.blockReach = v,
            30,
            100,
            5,
            " blocks"),
        number(
            "Entity reach",
            () -> draft.entityReach,
            v -> draft.entityReach = v,
            30,
            100,
            5,
            " blocks"),
        toggle(
            "Destroy item slot",
            () -> draft.destroySlot,
            () -> draft.destroySlot = !draft.destroySlot),
        toggle(
            "Shift-click clears inventory",
            () -> draft.shiftClearsInventory,
            () -> draft.shiftClearsInventory = !draft.shiftClearsInventory));
    page = Math.min(page, pages.size() - 1);
    List<Row> rows = pages.get(page).rows;
    for (int i = 0; i < rows.size(); i++)
      buttons.add(new Button(i, left, 48 + i * 22, span, 20, rows.get(i).label.get()));
    buttons.add(new Button(100, left, height - 51, 40, 20, "<"));
    buttons.add(new Button(101, left + span - 40, height - 51, 40, 20, ">"));
    buttons.add(new Button(102, left, height - 26, span / 2 - 2, 20, "Done"));
    buttons.add(new Button(103, left + span / 2 + 2, height - 26, span / 2 - 2, 20, "Cancel"));
  }

  @Override
  protected void buttonClicked(Button b) {
    if (b.id == 103) {
      minecraft.openScreen(parent);
      return;
    }
    if (b.id == 102) {
      try {
        Config.apply(draft);
        keys.forEach((k, v) -> k.key = v);
        minecraft.options.saveOptions();
        minecraft.openScreen(parent);
      } catch (java.io.IOException | IllegalArgumentException e) {
        error = e.getMessage();
        Config.LOG.error("Could not save settings", e);
      }
      return;
    }
    if (b.id == 100 || b.id == 101) {
      capture = null;
      page = Math.floorMod(page + (b.id == 100 ? -1 : 1), pages.size());
    } else pages.get(page).rows.get(b.id).change.run();
    init();
  }

  @Override
  protected void keyPressed(char c, int code) {
    if (capture != null) {
      keys.put(capture, code == Keyboard.KEY_ESCAPE ? 0 : code);
      capture = null;
      init();
    } else if (code == Keyboard.KEY_ESCAPE) minecraft.openScreen(parent);
    else if (code == Keyboard.KEY_LEFT || code == Keyboard.KEY_RIGHT) {
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
    if (button != 0 && button != 1) return;
    for (Object o : List.copyOf(buttons)) {
      Button b = (Button) o;
      if (b.isMouseOver(minecraft, x, y) && (button == 0 || b.id < 100)) {
        minecraft.soundHelper.playSound("random.click", 1, 1);
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
  public void render(int x, int y, float delta) {
    renderBackground();
    drawTextWithShadowCentred(textManager, "LpkeCreative", width / 2, 10, 0xFFFFFF);
    drawTextWithShadowCentred(textManager, pages.get(page).title, width / 2, 29, 0xDDDDDD);
    String hint = pages.get(page).hint;
    if (textManager.getTextWidth(hint) < width - 12)
      drawTextWithShadowCentred(textManager, hint, width / 2, height - 68, 0xAAAAAA);
    drawTextWithShadowCentred(
        textManager, (page + 1) + " / " + pages.size(), width / 2, height - 45, 0xFFFFFF);
    if (error != null)
      drawTextWithShadowCentred(textManager, error, width / 2, height - 81, 0xFF5555);
    super.render(x, y, delta);
  }
}
