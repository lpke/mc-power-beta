package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.Setting;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.inventory.tools.*;
import local.luke.power.mixin.DeathCraftingAccessor;
import local.luke.power.offhand.Offhand;
import local.luke.power.recipes.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.class_277;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Real native-host dispatch with an isolated tagged inventory and fully restored settings. */
final class SortingLayer402Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/sorting-layer-402");
  private static final int[][] SIZES = {{320, 240}, {640, 360}};
  private SortingLayer402Checks() {}
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable loaded singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable clone before checks");
    check(Display.getWidth() >= 1280 && Display.getHeight() >= 720, "resize the disposable display to at least 1280x720");
    check(mc.player instanceof PlayerMode mode && !mode.power$creative() && !mode.power$spectator(), "use actual Survival mode");
    check(!DetachedCamera.isActive() && !InteractionState.carryingContainer && !NativeInventoryPlan.busy() && !NativeInventoryPlan.failed(), "start with the physical player and idle inventory plans");
    check(!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1) && Bindings.heldModifiers() == 0, "release mouse buttons and modifiers");
    Files.createDirectories(REPORT); failures = 0;
    try (var state = new State(mc)) {
      test("SORTING_LAYER402 native F10 hotkey preserves the host and full inventory", () -> {
        for (int[] size : SIZES) {
          state.open(size); String inventory = state.inventory();
          check(state.page() instanceof SortingPreferencesScreen && state.hostOpen(), "F10 replaced or closed the native inventory");
          check(state.tools.modal() && InventoryPanels.modal(state.screen) && InventoryTextInput.active(state.screen), "sorting layer did not own native input");
          capture(mc, state.screen, "layout-" + size[0]); state.unchanged(inventory); state.unsaved();
        }
      });
      test("SORTING_LAYER402 picker and custom ID return to the original embedded editor", () -> {
        for (int[] size : SIZES) {
          state.open(size); mc.player.inventory.setCursorStack(DeathChecks.tagged(35, 117, 6, "sorting-layer-nested-cursor"));
          String inventory = state.inventory(); var editor = state.editor();
          state.press(left(editor) + 10, controls(editor) + 29, 0);
          check(state.page() instanceof ItemPickerScreen && state.hostOpen(), "preferred item picker replaced the native host");
          var picker = (ItemPickerScreen)state.page(); capture(mc, state.screen, "picker-" + size[0]);
          check(((ItemPickerScreen.Options)read(picker, "options")).instant(), "embedded picker requires an extra confirmation");
          @SuppressWarnings("unchecked") var shown = (List<ItemCatalogue.Entry>)read(picker, "shown");
          check(!shown.isEmpty(), "picker has no native item rows"); String chosen = shown.get(0).value();
          state.press((int)UiChecks.call(picker, "left") + 12, 78, 0);
          check(state.page() == editor && chosen.equals(preferred(editor).get(9)), "row click did not return immediately to the original editor");
          state.press(left(editor) + 10, controls(editor) + 29, 0); picker = (ItemPickerScreen)state.page();
          state.press((int)UiChecks.call(picker, "left") + 10, picker.height - 20, 0);
          check(state.page() instanceof ValueScreen && state.hostOpen(), "Custom ID left the native host");
          for (char character : "35:4".toCharArray()) state.key(character, 0);
          state.key('\0', Keyboard.KEY_RETURN);
          check(state.page() == editor && "35:4".equals(preferred(editor).get(9)), "Custom ID did not return to the same editor with native metadata");
          capture(mc, state.screen, "custom-id-" + size[0]); state.unchanged(inventory); state.unsaved();
        }
      });
      test("SORTING_LAYER402 right-click clears only a preferred-item draft", () -> {
        for (int[] size : SIZES) {
          state.open(size); String inventory = state.inventory(); var editor = state.editor();
          preferred(editor).put(9, "35:4"); state.press(left(editor) + 10, controls(editor) + 9, 0);
          check(protectedSlots(editor).equals(Set.of(9)), "protection button did not change the local draft");
          state.press(left(editor) + 11, (int)UiChecks.call(editor, "gridTop") + 11, 1);
          editor.layerRelease(left(editor) + 11, (int)UiChecks.call(editor, "gridTop") + 11, 1);
          check(preferred(editor).isEmpty() && protectedSlots(editor).equals(Set.of(9)), "clearing the preferred item changed protection");
          check(((Setting)read(editor, "protectedSetting")).value.getAsJsonArray().size() == 0
              && ((Setting)read(editor, "preferredSetting")).value.getAsJsonObject().size() == 0, "right-click published the editor draft");
          state.unchanged(inventory); state.unsaved();
        }
      });
      test("SORTING_LAYER402 mouse and keyboard cannot reach native slots behind the layer", () -> {
        for (int[] size : SIZES) {
          state.open(size); var editor = state.editor();
          mc.player.inventory.setCursorStack(DeathChecks.tagged(35, 117, 6, "sorting-layer-cursor")); String inventory = state.inventory();
          var slot = state.screen.container.method_2084(36);
          int x = (state.screen.width - 176) / 2 + InventoryPanels.offset(state.screen) + slot.x + 4;
          int y = (state.screen.height - 166) / 2 + slot.y + 4;
          state.press(x, y, 0); state.release(x, y, 0);
          state.key('\0', mc.options.inventoryKey.code); state.key('q', Keyboard.KEY_Q); state.key('2', Keyboard.KEY_2);
          check(state.hostOpen() && state.page() == editor && state.tools.modal(), "native inventory-close/drop/swap key escaped modal ownership");
          state.unchanged(inventory); state.unsaved();
        }
      });
      test("SORTING_LAYER402 preset dropdown and Load remain local through Discard", () -> {
        for (int[] size : SIZES) {
          state.open(size); String inventory = state.inventory(); var editor = state.editor();
          write(editor, "presets", presets()); write(editor, "presetIndex", 0);
          state.press(left(editor) + 10, presetsTop(editor) + 9, 0);
          check((boolean)read(editor, "choosingPreset"), "preset dropdown did not open"); capture(mc, state.screen, "dropdown-" + size[0]);
          state.key('\0', Keyboard.KEY_END); state.key('\0', Keyboard.KEY_RETURN);
          check((int)read(editor, "presetIndex") == 2 && preferred(editor).isEmpty() && protectedSlots(editor).isEmpty(), "choosing a preset loaded it before Load");
          state.press(left(editor) + 120, presetsTop(editor) + 9, 0);
          check(protectedSlots(editor).equals(Set.of(11)) && preferred(editor).equals(Map.of(12, "35:4")), "Load did not copy both local preset values");
          state.press(editor.width / 2 + 12, editor.height - 18, 0); state.release(editor.width / 2 + 12, editor.height - 18, 0);
          state.closed(); state.unchanged(inventory); state.unsaved();
        }
      });
      test("SORTING_LAYER402 Escape prompts preserve drafts until explicit Discard", () -> {
        for (int[] size : SIZES) {
          state.open(size); String inventory = state.inventory(); var editor = state.editor();
          state.press(left(editor) + 10, controls(editor) + 9, 0); state.key('\0', Keyboard.KEY_ESCAPE);
          check((boolean)read(read(editor, "exit"), "active") && state.hostOpen(), "dirty Escape bypassed its save prompt");
          state.key('\0', Keyboard.KEY_RETURN); check(state.page() == editor && state.tools.modal(), "prompt leaked Enter into a save or native screen");
          state.key('\0', Keyboard.KEY_ESCAPE); check(!(boolean)read(read(editor, "exit"), "active") && protectedSlots(editor).contains(9), "prompt Escape discarded the draft");
          state.key('\0', Keyboard.KEY_ESCAPE); prompt(state, editor, 2);
          check(!(boolean)read(read(editor, "exit"), "active") && protectedSlots(editor).contains(9), "Keep editing lost the draft");
          state.key('\0', Keyboard.KEY_ESCAPE); prompt(state, editor, 1); state.closed(); state.unchanged(inventory); state.unsaved();
        }
      });
      test("SORTING_LAYER402 Done and prompt Save change only layout settings and presets", () -> {
        state.open(new int[]{640, 360}); String inventory = state.inventory(); var editor = state.editor();
        protectedSlots(editor).add(10); preferred(editor).put(11, "35:4"); var drafts = presets(); write(editor, "presets", drafts);
        var before = PowerConfig.document().deepCopy();
        state.press(editor.width / 2 - 60, editor.height - 18, 0); state.release(editor.width / 2 - 60, editor.height - 18, 0);
        state.closed(); state.unchanged(inventory); verifySave(before, Set.of(10), Map.of(11, "35:4"), drafts.json());
        check(InventoryToolsConfig.current().protectedSlots.equals(List.of(10))
            && InventoryToolsConfig.current().preferredItems.equals(Map.of(11, "35:4")), "Done did not apply the two layout settings");
        state.open(new int[]{640, 360}); editor = state.editor(); protectedSlots(editor).add(12); preferred(editor).put(13, "4");
        before = PowerConfig.document().deepCopy(); var expectedPresets = SortingLayoutPresets.snapshot();
        state.key('\0', Keyboard.KEY_ESCAPE); prompt(state, editor, 0); state.closed(); state.unchanged(inventory);
        verifySave(before, Set.of(12), Map.of(13, "4"), expectedPresets.getAsJsonObject());
      });
      test("SORTING_LAYER402 screen ownership releases all modal and text owners", () -> {
        state.open(new int[]{640, 360}); String inventory = state.inventory(); var layer = state.layer();
        mc.currentScreen = null; state.tools.prepare();
        check(!layer.active() && !InventoryPanels.modal(state.screen) && !InventoryTextInput.active(state.screen), "changing screens retained sorting input ownership");
        mc.currentScreen = state.screen; state.tools.prepare(); state.closed(); state.unchanged(inventory);
        mc.player.inventory.setCursorStack(DeathChecks.tagged(5, 2, 0, "sorting-layer-held")); inventory = state.inventory();
        check(!((InventoryToolsHost)state.screen).power$openSortingLayout(), "a carried cursor admitted a new layout editor"); state.unchanged(inventory);
        mc.player.inventory.setCursorStack(null); mc.currentScreen = null;
        check(!InventorySortingLayer.available(mc) && !Bindings.matches("powerbeta.sortingLayout", Keyboard.KEY_F10), "sorting layout claimed gameplay input outside a native inventory");
        mc.currentScreen = state.screen;
      });
    }
    log("SORTING_LAYER402 failures=" + failures + " captures=" + REPORT);
    check(failures == 0, "Sorting layer regression failures=" + failures);
  }
  private static int left(SortingPreferencesScreen editor) throws Exception { return (int)UiChecks.call(editor, "left"); }
  private static int controls(SortingPreferencesScreen editor) throws Exception { return (int)UiChecks.call(editor, "controlsTop"); }
  private static int presetsTop(SortingPreferencesScreen editor) throws Exception { return (int)UiChecks.call(editor, "presetsTop"); }
  @SuppressWarnings("unchecked") private static Map<Integer,String> preferred(SortingPreferencesScreen editor) throws Exception { return (Map<Integer,String>)read(editor, "preferredItems"); }
  @SuppressWarnings("unchecked") private static Set<Integer> protectedSlots(SortingPreferencesScreen editor) throws Exception { return (Set<Integer>)read(editor, "protectedSlots"); }
  private static void prompt(State state, SortingPreferencesScreen editor, int button) {
    int width = Math.min(76, Math.max(10, (editor.width - 32) / 3));
    state.press((editor.width - (width * 3 + 8)) / 2 + button * (width + 4) + width / 2, editor.height / 2 + 18, 0);
  }
  private static SortingLayoutsData presets() {
    var data = SortingLayoutsData.read(new JsonObject());
    for (int i = 0; i < 3; i++) { var locked = new JsonArray(); locked.add(9 + i); var preferred = new JsonObject(); preferred.addProperty("12", "35:4"); data = data.save("Layer fixture " + i, locked, preferred); }
    return data;
  }
  private static void verifySave(JsonObject before, Set<Integer> locked, Map<Integer,String> preferred, JsonObject presets) {
    var expected = before.deepCopy(); var settings = expected.getAsJsonObject("settings");
    var tools = settings.getAsJsonObject("inventoryTools");
    tools.add("protectedSlots", local.luke.power.config.Catalog.JSON.toJsonTree(locked));
    tools.add("preferredItems", local.luke.power.config.Catalog.JSON.toJsonTree(preferred)); settings.add("sortingLayouts", presets.deepCopy());
    check(expected.equals(PowerConfig.document()), "layout save changed another settings section or preference");
    check(presets.equals(SortingLayoutPresets.snapshot()), "saved presets and the hotkey cache disagree");
  }
  private static void capture(Minecraft mc, Screen host, String name) throws Exception {
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try { SettingsSnapshot.render(mc, host, host.width, host.height, REPORT.resolve(name + ".png")); }
    finally { GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPopAttrib(); GL11.glMatrixMode(mode); }
  }
  private static Field field(Object object, String name) throws Exception {
    for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) try {
      var field = type.getDeclaredField(name); field.setAccessible(true); return field;
    } catch (NoSuchFieldException next) {}
    throw new NoSuchFieldException(name);
  }
  private static Object read(Object object, String name) throws Exception { return field(object, name).get(object); }
  private static void write(Object object, String name, Object value) throws Exception { field(object, name).set(object, value); }
  private static String bytes(ItemStack stack) throws Exception { return stack == null ? null : DeathItems.encode(stack); }
  private static final class State implements AutoCloseable {
    final Minecraft mc;
    final Screen originalScreen;
    final net.minecraft.inventory.Container originalContainer;
    final ItemStack[] originalMain, originalArmor, grid, originalGrid;
    final ItemStack originalCursor, originalOffhand, originalOutput;
    final int originalSelected, originalKey;
    final boolean originalGrabbed, originalRepeat, originalFocused, originalPaused, originalTextFocus;
    final JsonObject document;
    final byte[] disk;
    final InventoryToolsSettings settings;
    final RecipeBookSettings recipes;
    final JsonElement cachedPresets;
    final Map<String,Integer> modifiers;
    final Set<Object> originalEntities = Collections.newSetFromMap(new IdentityHashMap<>());
    final List<class_585> screens = new ArrayList<>();
    class_585 screen;
    InventoryToolsOverlay tools;
    State(Minecraft mc) throws Exception {
      this.mc = mc; originalEntities.addAll(mc.world.field_198); PowerConfig.flushPending(); document = PowerConfig.document().deepCopy(); disk = Files.readAllBytes(PowerConfig.path());
      settings = InventoryToolsConfig.copy(); recipes = RecipeBookConfig.copy(); cachedPresets = SortingLayoutPresets.snapshot();
      originalScreen = mc.currentScreen; originalContainer = mc.player.container;
      originalMain = mc.player.inventory.main; originalArmor = mc.player.inventory.armor; originalCursor = mc.player.inventory.getCursorStack(); originalOffhand = Offhand.get(mc.player.inventory);
      originalSelected = mc.player.inventory.selectedSlot; originalKey = GameplayKeys.SORTING_LAYOUT.code; modifiers = Bindings.modifiers();
      originalGrabbed = Mouse.isGrabbed(); originalRepeat = Keyboard.areRepeatEventsEnabled(); originalFocused = mc.field_2778; originalPaused = mc.paused; originalTextFocus = InventoryTextInput.active(originalScreen);
      check(mc.player.playerContainer.getClass() == class_277.class, "expected the exact native player container");
      grid = ((DeathCraftingAccessor)(Object)((class_277)mc.player.playerContainer).field_1124).power$deathStacks(); originalGrid = grid.clone(); originalOutput = mc.player.playerContainer.method_2084(0).getStack();
      mc.player.inventory.main = new ItemStack[36]; mc.player.inventory.armor = new ItemStack[4]; mc.player.inventory.selectedSlot = 0;
      for (int i = 0; i < 36; i++) mc.player.inventory.main[i] = DeathChecks.tagged(5, i + 1, 0, "sorting-layer-main-" + i);
      for (int i = 0; i < 4; i++) mc.player.inventory.armor[i] = DeathChecks.tagged(301 - i, 1, i + 1, "sorting-layer-armor-" + i);
      Arrays.fill(grid, null); ((class_277)mc.player.playerContainer).field_1124.setStack(0, DeathChecks.tagged(5, 3, 0, "sorting-layer-craft"));
      ((class_277)mc.player.playerContainer).field_1124.setStack(2, DeathChecks.tagged(5, 3, 0, "sorting-layer-craft"));
      Offhand.set(mc.player.inventory, DeathChecks.tagged(35, 17, 2, "sorting-layer-offhand"));
      var unmodified = new HashMap<>(modifiers); unmodified.remove("powerbeta.sortingLayout"); Bindings.configure(unmodified); GameplayKeys.SORTING_LAYOUT.code = Keyboard.KEY_F10;
      var quietBook = RecipeBookConfig.copy(); quietBook.inventory = false; RecipeBookConfig.preview(quietBook);
    }
    void open(int[] size) throws Exception {
      if (tools != null) tools.close(); if (screen != null) { var book = RecipeBooks.book(screen); if (book != null) book.removed(); InventoryPanels.remove(screen); } InventoryTextInput.clear();
      var draft = InventoryToolsConfig.copy(); draft.protectedSlots.clear(); draft.preferredItems.clear(); InventoryToolsConfig.preview(draft);
      mc.player.inventory.setCursorStack(null); Mouse.setGrabbed(false); Keyboard.enableRepeatEvents(false); mc.field_2778 = true; mc.paused = false;
      screen = new class_585(mc.player); screen.container = mc.player.playerContainer; screens.add(screen);
      mc.currentScreen = screen; mc.player.container = screen.container; screen.init(mc, size[0], size[1]);
      tools = (InventoryToolsOverlay)read(screen, "power$inventoryTools");
      check(Bindings.matches("powerbeta.sortingLayout", Keyboard.KEY_F10), "registered sorting-layout binding did not match its temporary F10 code");
      ((ScreenInput)(Object)screen).power$key('\0', Keyboard.KEY_F10);
      check(layer() != null && layer().active(), "native key wrapper did not open the sorting layer");
    }
    InventorySortingLayer layer() throws Exception { return (InventorySortingLayer)read(tools, "sortingLayer"); }
    UiScreen page() throws Exception { return (UiScreen)read(layer(), "page"); }
    SortingPreferencesScreen editor() throws Exception { return (SortingPreferencesScreen)page(); }
    boolean hostOpen() { return mc.currentScreen == screen && mc.player.container == screen.container && screen.container == mc.player.playerContainer; }
    void press(int x, int y, int button) { ((ScreenInput)(Object)screen).power$click(x, y, button); }
    void release(int x, int y, int button) { ((ScreenInput)(Object)screen).power$release(x, y, button); }
    void key(char character, int key) { ((ScreenInput)(Object)screen).power$key(character, key); }
    String inventory() throws Exception { return DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)) + "|output=" + bytes(mc.player.playerContainer.method_2084(0).getStack()); }
    void unchanged(String before) throws Exception { check(before.equals(inventory()) && !NativeInventoryPlan.busy() && !NativeInventoryPlan.failed(), "sorting settings changed native inventory, NBT, cursor, crafting output or pending actions"); }
    void unsaved() throws Exception { check(document.equals(PowerConfig.document()) && Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())) && Objects.equals(cachedPresets, SortingLayoutPresets.snapshot()), "unsaved layer changed persisted settings or the preset cache"); }
    void closed() {
      tools.prepare(); check(hostOpen() && !tools.modal() && !InventoryPanels.modal(screen) && !InventoryTextInput.active(screen), "closing settings did not return input ownership to the native host");
      check(!Keyboard.areRepeatEventsEnabled(), "closing settings did not restore native keyboard-repeat state");
    }
    public void close() throws Exception {
      try {
        if (tools != null) tools.close(); for (var host : screens) { var book = RecipeBooks.book(host); if (book != null) book.removed(); InventoryPanels.remove(host); }
        if (!document.equals(PowerConfig.document())) PowerConfig.write(document);
        var cache = SortingLayoutPresets.class.getDeclaredField("current"); cache.setAccessible(true); cache.set(null, cachedPresets == null ? null : cachedPresets.deepCopy());
        InventoryToolsConfig.preview(settings); RecipeBookConfig.preview(recipes);
      } finally {
        mc.player.inventory.main = originalMain; mc.player.inventory.armor = originalArmor; mc.player.inventory.selectedSlot = originalSelected;
        System.arraycopy(originalGrid, 0, grid, 0, grid.length); mc.player.playerContainer.method_2084(0).setStack(originalOutput);
        mc.player.inventory.setCursorStack(originalCursor); Offhand.set(mc.player.inventory, originalOffhand);
        for (Object entity : List.copyOf(mc.world.field_198))
          if (entity instanceof net.minecraft.class_142 drop && !originalEntities.contains(entity)) drop.markDead();
        GameplayKeys.SORTING_LAYOUT.code = originalKey; Bindings.configure(modifiers);
        mc.currentScreen = originalScreen; mc.player.container = originalContainer; mc.field_2778 = originalFocused; mc.paused = originalPaused;
        Mouse.setGrabbed(originalGrabbed); Keyboard.enableRepeatEvents(originalRepeat); InventoryTextInput.clear(); if (originalTextFocus) InventoryTextInput.focus(originalScreen, true);
      }
    }
  }
}
