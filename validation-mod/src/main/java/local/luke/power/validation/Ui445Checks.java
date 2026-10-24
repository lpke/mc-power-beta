package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.config.*;
import local.luke.power.creative.CreativeTabsData;
import local.luke.power.creativeui.CreativeTabsBackend;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.recipes.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Native pixel references and isolated editor gestures for 4.4.5. Never install in a user instance. */
final class Ui445Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/ui-445");
  private Ui445Checks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(action.equals("capture") || action.equals("run"), "Use ui-445 capture or run");
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "Focus a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "Mute the disposable client");
    check(mc.player instanceof PlayerMode mode && !mode.power$creative() && !mode.power$spectator(), "Use actual Survival mode");
    check(mc.field_2807 == mc.player && !DetachedCamera.isActive() && !InteractionState.carryingContainer,
        "Use the physical player without freecam or carrying");
    check(!NativeInventoryPlan.busy() && !NativeInventoryPlan.failed() && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1),
        "Finish inventory actions and release pointer buttons");
    Files.createDirectories(REPORT.resolve(action));
    boolean blinking = InterfaceState.blinkingUiCursor;
    try {
      InterfaceState.blinkingUiCursor = false;
      if (action.equals("capture")) {
        // This stage prepares review images only. Acceptance checks belong to the later run.
        try (var state = new Recipe402Checks.State(mc)) {
          for (int width : List.of(320, 640)) captureRecipes(mc, state, width, REPORT.resolve(action));
        }
        searchImages(mc, REPORT.resolve(action), false);
        log("UI445 CAPTURE " + REPORT.resolve(action).toAbsolutePath());
        return;
      }
      failures = 0;
      try (var state = new Recipe402Checks.State(mc)) {
        for (int width : List.of(320, 640)) {
          test("UI445 native recipe slots return to cold-closed pixels at " + width, () -> recipePixels(mc, state, width));
          for (String close : List.of("toggle", "disabled", "removed"))
            test("UI445 " + close + " clears unavailable preview and retry state at " + width, () -> recipeCleanup(mc, state, width, close));
        }
        test("UI445 narrow Escape clears unavailable preview and retry state", () -> recipeCleanup(mc, state, 320, "escape"));
      }
      test("UI445 native Search font, shadow, selection and eight-pixel caret", () -> searchImages(mc, REPORT.resolve(action), true));
      editorChecks(mc);
      log("UI445 failures=" + failures + " reports=" + REPORT.resolve(action).toAbsolutePath());
      check(failures == 0, "UI445 failures=" + failures);
    } finally { InterfaceState.blinkingUiCursor = blinking; }
  }

  static void run446(Minecraft mc, String action) throws Exception {
    run(mc, action);
    Path directory = Path.of("power-beta-data/reports/ui-446").resolve(action);
    Files.createDirectories(directory);
    boolean verify = action.equals("run");
    Screen parent = mc.currentScreen;
    boolean repeat = Keyboard.areRepeatEventsEnabled();
    PowerConfig.flushPending(); byte[] disk = Files.readAllBytes(PowerConfig.path());
    String inventory = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
    try {
      for (int width : List.of(320, 640)) {
        Setting setting = editorSetting(64);
        if (!verify) {
          var clean = CreativeTabsData.read(setting.value.getAsJsonObject());
          var slots = CreativeTabsData.slots(clean.tab(0)); slots.set(2, "3:0");
          CreativeTabsData.slots(clean.tab(0), slots); setting.value = clean.json();
        }
        var editor = new CreativeTabsScreen(parent, setting, () -> { throw new AssertionError("Feedback preview published its draft"); });
        mc.setScreen(editor); editor.init(mc, width, 360);
        JsonObject original = document(editor);
        cells(editor).addAll(Set.of(0, 2));
        int px = x(editor, 1), py = y(editor, 1);
        var cold = render(mc, editor, width, directory, "empty-reference", px, py);
        cells(editor).add(1); editor.layerPress(px, py, 1);
        var press = render(mc, editor, width, directory, "empty-held", px, py);
        var within = render(mc, editor, width, directory, "empty-small-motion", px + 5, py);
        var drag = render(mc, editor, width, directory, "clearing-drag", x(editor, 3), py);
        var returned = render(mc, editor, width, directory, "clearing-return", px, py);
        if (verify) {
          int sx = (px - 7) * 2, sy = (py - 7) * 2;
          check(same(cold, press, sx, sy, 28, 28) && same(cold, within, sx, sy, 28, 28),
              "Empty-cell right-click or in-cell motion flashed deletion red");
          check(!cells(editor).contains(1) && cells(editor).containsAll(Set.of(0, 2)), "Empty press lost unrelated selection");
          check(!same(cold, drag, sx, sy, 28, 28) && !same(cold, returned, sx, sy, 28, 28),
              "Leaving the origin did not retain clearing feedback");
          check(document(editor).equals(original) && undoCount(editor) == 0, "Held clearing preview changed draft or Undo");
        }
        editor.layerRelease(x(editor, 3), py, 1);
        if (verify) {
          var slots = CreativeTabsData.slots(CreativeTabsData.read(document(editor)).tab(0));
          check(slots.get(1).isEmpty() && slots.get(2).isEmpty() && slots.get(3).isEmpty() && !slots.get(0).isEmpty(),
              "Clearing drag deleted the wrong cells");
          check(undoCount(editor) == 1, "Clearing drag must remain one Undo edit");
          call(editor, "undo"); check(document(editor).equals(original), "Clearing Undo lost original item IDs");
          editor.layerPress(px, py, 1); render(mc, editor, width, directory, "empty-second-press", px, py);
          editor.layerRelease(px, py, 1);
          check(document(editor).equals(original) && undoCount(editor) == 0, "Empty click added a phantom content edit");
          editor.layerPress(px, py, 1); render(mc, editor, width, directory, "empty-cancel", x(editor, 3), py);
          editor.removed();
          check(document(editor).equals(original) && undoCount(editor) == 0, "Canceled clearing changed content");
          check(setting.value.equals(original), "Feedback fixture published editor settings");
          log("PASS UI446 empty-cell feedback, clearing, Undo and cancellation at " + width);
        } else editor.removed();
        for (int occupied : verify ? List.of(0, 2) : List.of(0)) {
          var occupiedEditor = new CreativeTabsScreen(parent, setting, () -> { throw new AssertionError("Occupied preview published"); });
          mc.setScreen(occupiedEditor); occupiedEditor.init(mc, width, 360);
          int ox = x(occupiedEditor, occupied), oy = y(occupiedEditor, occupied);
          JsonObject originalOccupied = document(occupiedEditor);
          var idle = render(mc, occupiedEditor, width, directory, "occupied-" + occupied + "-idle", ox, oy);
          occupiedEditor.layerPress(ox, oy, 1);
          var held = render(mc, occupiedEditor, width, directory, "occupied-" + occupied + "-held", ox, oy);
          var moved = render(mc, occupiedEditor, width, directory, "occupied-" + occupied + "-small-motion", ox + 5, oy);
          if (verify) {
            check(same(idle, held, (ox - 7) * 2, (oy - 7) * 2, 28, 28)
                && same(idle, moved, (ox - 7) * 2, (oy - 7) * 2, 28, 28),
                "Occupied right press changed item pixels before leaving its cell");
            check(document(occupiedEditor).equals(originalOccupied) && undoCount(occupiedEditor) == 0,
                "Occupied press changed content before release");
            occupiedEditor.layerRelease(ox, oy, 1);
            check(CreativeTabsData.slots(CreativeTabsData.read(document(occupiedEditor)).tab(0)).get(occupied).isEmpty()
                && undoCount(occupiedEditor) == 1, "Occupied right-click no longer clears on release");
            call(occupiedEditor, "undo");
            check(document(occupiedEditor).equals(originalOccupied), "Occupied right-click Undo lost original data");
            check(setting.value.equals(originalOccupied), "Occupied right-click published its draft");
            log("PASS UI446 occupied-cell press feedback and release/Undo at " + width + " cell=" + occupied);
          }
          occupiedEditor.removed();
        }
      }
      check(inventory.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)))
          && Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "Feedback changed native inventory or persisted settings");
      log("UI446 " + action + " completed");
    } finally { mc.setScreen(parent); Keyboard.enableRepeatEvents(repeat); }
  }

  @SuppressWarnings("unchecked") private static RecipeDefinition unavailable(Recipe402Checks.State state) throws Exception {
    var recipes = (List<RecipeDefinition>)field(field(state.book(), "model"), "recipes");
    var recipe = recipes.stream().filter(value -> value.kind == RecipeDefinition.Kind.CRAFTING
        && value.output.itemId == 280 && value.layout(2, 2) != null).findFirst().orElseThrow();
    var choose = RecipeBookOverlay.class.getDeclaredMethod("choose", RecipeDefinition.class, boolean.class);
    choose.setAccessible(true); choose.invoke(state.book(), recipe, false); state.finish();
    return recipe;
  }
  private static BufferedImage render(Minecraft mc, Screen screen, int width, Path directory, String name) throws Exception {
    return render(mc, screen, width, directory, name, -1, -1);
  }
  private static BufferedImage render(Minecraft mc, Screen screen, int width, Path directory, String name, int mx, int my) throws Exception {
    Path path = directory.resolve(width + "-" + name + ".png");
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try { SettingsSnapshot.render(mc, screen, width, 360, path, mx, my); }
    finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
    return ImageIO.read(path.toFile());
  }
  private static void captureRecipes(Minecraft mc, Recipe402Checks.State state, int width, Path directory) throws Exception {
    state.reset(width); unavailable(state); render(mc, mc.currentScreen, width, directory, "recipe-open");
    state.book().toggle(); render(mc, mc.currentScreen, width, directory, "recipe-closed");
    state.reset(width); state.book().toggle(); render(mc, mc.currentScreen, width, directory, "recipe-cold-closed");
  }
  private static void recipePixels(Minecraft mc, Recipe402Checks.State state, int width) throws Exception {
    Path directory = REPORT.resolve("run");
    state.reset(width); String items = state.snapshot(); var recipe = unavailable(state);
    check(field(state.book(), "ghost") == recipe && field(state.book(), "lastUnavailable") == recipe, "Unavailable recipe did not prepare a real preview");
    var open = render(mc, mc.currentScreen, width, directory, "recipe-open"); var openLayout = state.book().layout();
    // A same-layout native reference proves the open preview actually rendered on a wide GUI.
    field(state.book(), "ghost", null);
    var bare = render(mc, mc.currentScreen, width, directory, "recipe-open-no-preview");
    if (!openLayout.narrow()) check(!sameSlots(open, bare, state.context(), openLayout), "Wide preview had no native slot pixels");
    field(state.book(), "ghost", recipe); state.book().toggle();
    var closed = render(mc, mc.currentScreen, width, directory, "recipe-closed"); var closedLayout = state.book().layout();
    // An accidental later stale assignment must also be hidden by the closed render path.
    field(state.book(), "ghost", recipe);
    var stale = render(mc, mc.currentScreen, width, directory, "recipe-closed-stale-preview");
    field(state.book(), "ghost", null);
    state.reset(width); state.book().toggle();
    var cold = render(mc, mc.currentScreen, width, directory, "recipe-cold-closed");
    check(sameSlots(closed, cold, state.context(), closedLayout) && sameSlots(stale, cold, state.context(), closedLayout),
        "Closed book tinted or drew an item in native crafting slots");
    check(items.equals(state.snapshot()), "Preview rendering or closing changed native inventory data");
  }
  private static boolean sameSlots(BufferedImage left, BufferedImage right, RecipeContext context, RecipeBookLayout layout) {
    for (var slot : context.inputs) if (!same(left, right, (layout.inventoryX() + slot.x - 1) * 2,
        (layout.inventoryY() + slot.y - 1) * 2, 36, 36)) return false;
    // The unavailable output tint extends four pixels beyond the native item cell.
    return same(left, right, (layout.inventoryX() + context.output.x - 4) * 2,
        (layout.inventoryY() + context.output.y - 4) * 2, 48, 48);
  }
  private static void recipeCleanup(Minecraft mc, Recipe402Checks.State state, int width, String close) throws Exception {
    state.reset(width); String items = state.snapshot(); var recipe = unavailable(state); var book = state.book();
    check(field(book, "ghost") == recipe && field(book, "lastUnavailable") == recipe, "No unavailable preview to close");
    Object otherEditor = new Object(); InventoryTextInput.focus(mc.currentScreen, otherEditor, true);
    try {
      switch (close) {
        case "toggle" -> book.toggle();
        case "escape" -> check(book.key('\0', Keyboard.KEY_ESCAPE), "Narrow book did not own Escape");
        case "disabled" -> { var config = RecipeBookConfig.copy(); config.enabled = false; RecipeBookConfig.preview(config); book.tick(); }
        case "removed" -> book.removed();
        default -> throw new AssertionError(close);
      }
      check(field(book, "ghost") == null && field(book, "lastUnavailable") == null, "Closure retained preview or unavailable-retry identity");
      check(InventoryTextInput.active(mc.currentScreen), "Recipe cleanup released another editor's typing ownership");
      if (close.equals("disabled")) { var config = RecipeBookConfig.copy(); config.enabled = true; RecipeBookConfig.preview(config); book.tick(); }
      if (!(boolean)field(book, "open")) book.toggle();
      check(field(book, "ghost") == null && field(book, "lastUnavailable") == null, "Reopening revived the old unavailable preview");
      check(items.equals(state.snapshot()), "Closing preview changed full native inventory data");
    } finally { InventoryTextInput.focus(mc.currentScreen, otherEditor, false); }
  }

  private static boolean same(BufferedImage left, BufferedImage right, int x, int y, int width, int height) {
    for (int dy = 0; dy < height; dy++) for (int dx = 0; dx < width; dx++)
      if (left.getRGB(x + dx, y + dy) != right.getRGB(x + dx, y + dy)) return false;
    return true;
  }
  private static void searchImages(Minecraft mc, Path directory, boolean verify) throws Exception {
    boolean repeat = Keyboard.areRepeatEventsEnabled(); Screen parent = mc.currentScreen;
    boolean textFocused = InventoryTextInput.active(parent);
    CreativeSearchInput component = null;
    try {
      // The real component binds its typing claim to the host present at construction.
      var host = new Screen() {}; host.init(mc, 320, 360); mc.currentScreen = host;
      var constructor = CreativeSearchInput.class.getDeclaredConstructor(Minecraft.class); constructor.setAccessible(true);
      component = constructor.newInstance(mc); var input = (TextInput)field(component, "search");
      for (int width : List.of(320, 640)) {
        var plate = new SearchPlate(component); plate.init(mc, width, 360);
        input.setText("Search"); input.focused = false;
        var text = render(mc, plate, width, directory, "search-native-text");
        plate.reference = true;
        var nativeText = render(mc, plate, width, directory, "search-reference-font");
        plate.reference = false; input.setText(""); input.focused = false;
        var empty = render(mc, plate, width, directory, "search-empty");
        input.focused = true;
        var caret = render(mc, plate, width, directory, "search-caret");
        input.setText("Search"); input.selectAll();
        var selected = render(mc, plate, width, directory, "search-selection");
        if (!verify) continue;
        check(same(text, nativeText, SearchPlate.LEFT * 2, SearchPlate.TOP * 2, SearchPlate.SPAN * 2, 18),
            "Search differs from the native shadowed font at the field's top row");
        for (int y = SearchPlate.TOP - 1; y <= SearchPlate.TOP + 9; y++)
          for (int x = SearchPlate.LEFT - 1; x <= SearchPlate.LEFT + SearchPlate.SPAN; x++)
            for (int sy = 0; sy < 2; sy++) for (int sx = 0; sx < 2; sx++) {
              int expected = x == SearchPlate.LEFT + 3 && y >= SearchPlate.TOP && y < SearchPlate.TOP + 8
                  ? 0xffffff : empty.getRGB(x * 2 + sx, y * 2 + sy) & 0xffffff;
              check((caret.getRGB(x * 2 + sx, y * 2 + sy) & 0xffffff) == expected, "Search caret changed its eight-pixel height or field frame");
            }
        // The space between S and e exposes selection background without covering native glyph pixels.
        int selectionPixel = SearchPlate.LEFT + 3 + mc.textRenderer.getWidth("S") - 1;
        check((selected.getRGB(selectionPixel * 2, SearchPlate.TOP * 2) & 0xffffff) == 0x335577,
            "Search selection did not include its top row");
        check((selected.getRGB(selectionPixel * 2, (SearchPlate.TOP + 8) * 2) & 0xffffff) != 0x335577,
            "Search selection still covers the field's ninth row");
        component.press(SearchPlate.LEFT + 4, SearchPlate.TOP + 8, 0);
        check(component.focused(), "Search ninth field row stopped accepting input");
        component.press(SearchPlate.LEFT + 4, SearchPlate.TOP + 9, 0);
        check(!component.focused(), "Search bottom border accepted input");
      }
    } finally {
      if (component != null) component.close();
      mc.currentScreen = parent; Keyboard.enableRepeatEvents(repeat);
      if (textFocused) InventoryTextInput.focus(parent, true);
    }
  }
  /** Reference text goes through the native font renderer, on the actual empty component frame. */
  private static final class SearchPlate extends UiScreen {
    static final int LEFT = 18, TOP = 48, SPAN = 130;
    final CreativeSearchInput component;
    boolean reference;
    SearchPlate(CreativeSearchInput component) { this.component = component; }
    @Override public void render(int mx, int my, float tick) {
      fill(0, 0, width, height, 0xff222222);
      drawStringWithShadow(textRenderer, "Creative Search", LEFT, 20, 0xffffff);
      if (reference) {
        TextInput input;
        try { input = (TextInput)field(component, "search"); } catch (Exception failure) { throw new IllegalStateException(failure); }
        String value = input.text(); boolean focused = input.focused;
        input.setText(""); input.focused = false;
        try { component.render(width, height, LEFT, TOP, SPAN, -1, -1); }
        finally { input.setText(value); input.focused = focused; }
        GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING);
        drawStringWithShadow(textRenderer, value, LEFT + 3, TOP, 0xffffff);
      } else component.render(width, height, LEFT, TOP, SPAN, -1, -1);
    }
  }

  private static Setting editorSetting(int count) {
    var session = new ConfigSession(); CreativeTabsBackend.register(session); Setting setting = session.settings().get(0);
    CreativeTabsData draft = CreativeTabsData.read(CreativeTabsData.defaults()); draft.customized(true);
    var slots = new ArrayList<>(Collections.nCopies(count, ""));
    if (count > 0) slots.set(0, "35:14");
    if (count > 3) { slots.set(2, "missing:preserved"); slots.set(3, "267:71"); }
    draft.append(CreativeTabsData.tab("ui445", "UI check", "1", slots)); setting.value = draft.json(); return setting;
  }
  @SuppressWarnings("unchecked") private static Set<Integer> cells(CreativeTabsScreen editor) throws Exception { return (Set<Integer>)field(editor, "cells"); }
  private static JsonObject document(CreativeTabsScreen editor) throws Exception { return ((CreativeTabsData)field(editor, "data")).json(); }
  private static int x(CreativeTabsScreen editor, int cell) throws Exception { return (int)call(editor, "gridLeft") + cell % 8 * 18 + 9; }
  private static int y(CreativeTabsScreen editor, int cell) throws Exception { return (int)call(editor, "gridTop") + cell / 8 * 18 + 9; }
  private static int undoCount(CreativeTabsScreen editor) throws Exception { return ((Deque<?>)field(field(editor, "history"), "states")).size(); }
  private static void right(CreativeTabsScreen editor, int from, int to) throws Exception {
    editor.layerPress(x(editor, from), y(editor, from), 1); editor.layerRelease(x(editor, to), y(editor, to), 1);
  }
  private static void editorChecks(Minecraft mc) throws Exception {
    Screen parent = mc.currentScreen; boolean repeat = Keyboard.areRepeatEventsEnabled();
    var keyField = Keyboard.class.getDeclaredField("keyDownBuffer"); keyField.setAccessible(true); var keys = (ByteBuffer)keyField.get(null);
    int[] modifiers = {Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL, Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT};
    byte[] previous = new byte[modifiers.length];
    for (int i = 0; i < modifiers.length; i++) { previous[i] = keys.get(modifiers[i]); keys.put(modifiers[i], (byte)0); }
    PowerConfig.flushPending(); byte[] disk = Files.readAllBytes(PowerConfig.path());
    String inventory = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
    try {
      for (int width : List.of(320, 640)) for (String gesture : List.of("selected-empty", "selected-empty-short-0", "selected-empty-short-1", "unselected-empty", "occupied", "empty-drag"))
        test("UI445 draft right-click " + gesture + " at " + width, () -> {
          Setting setting = editorSetting(gesture.endsWith("short-0") ? 0 : gesture.endsWith("short-1") ? 1 : 64);
          var editor = new CreativeTabsScreen(parent, setting, () -> { throw new AssertionError("Editor published draft"); });
          mc.setScreen(editor); editor.init(mc, width, 360); JsonObject original = document(editor);
          cells(editor).addAll(gesture.startsWith("selected-empty") ? Set.of(0, 1, 2) : Set.of(0, 2));
          Set<Integer> selected = Set.copyOf(cells(editor));
          right(editor, gesture.equals("occupied") ? 0 : 1, gesture.equals("empty-drag") ? 3 : gesture.equals("occupied") ? 0 : 1);
          var expected = CreativeTabsData.read(original);
          if (gesture.equals("occupied") || gesture.equals("empty-drag")) {
            var slots = CreativeTabsData.slots(expected.tab(0));
            for (int i = gesture.equals("occupied") ? 0 : 1; i <= (gesture.equals("occupied") ? 0 : 3); i++) slots.set(i, "");
            CreativeTabsData.slots(expected.tab(0), slots);
            check(undoCount(editor) == 1, "Content deletion was not one Undo step");
          } else check(undoCount(editor) == 0, "Empty-cell selection gesture created a content Undo entry");
          check(document(editor).equals(expected.json()), "Right-click changed the wrong draft cells or unknown IDs");
          var wantedSelection = new TreeSet<>(selected); if (gesture.startsWith("selected-empty")) wantedSelection.remove(1);
          check(cells(editor).equals(wantedSelection), "Right-click lost unrelated selected cells");
          check(setting.value.equals(original), "Right-click saved its isolated draft");
          if (gesture.equals("occupied") || gesture.equals("empty-drag")) {
            call(editor, "undo"); check(document(editor).equals(original) && cells(editor).equals(selected) && undoCount(editor) == 0,
                "Undo did not restore complete draft IDs and selection");
          }
          check(inventory.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)))
              && Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "Draft interaction changed native inventory/NBT or persisted settings");
        });
    } finally {
      for (int i = 0; i < modifiers.length; i++) keys.put(modifiers[i], previous[i]);
      mc.setScreen(parent); Keyboard.enableRepeatEvents(repeat);
    }
  }
}
