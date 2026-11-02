package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.MusicPlaylist;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Capture is independent of acceptance and always precedes the native input checks. */
final class ControlsSearch471Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-471/controls-search");
  private static final int[][] SIZES = {{854, 480}, {320, 240}};
  private ControlsSearch471Checks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(Set.of("capture", "run").contains(action), "use controls-search-471 capture|run");
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0 && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1),
        "mute the clone and release mouse buttons");
    check(Display.getWidth() >= 1708 && Display.getHeight() >= 960, "resize the disposable display to at least 1708x960");
    Files.createDirectories(REPORT); failures = 0;
    try (var navigation = new Navigation(); var keys = new Keys(); var state = new Release462Checks.State(mc)) {
      capture(mc, state);
      if (action.equals("run")) for (int[] size : SIZES) {
        test("CONTROLS SEARCH471 exact keyboard/chord/mouse/modifier/Delete " + size[0], () -> exact(mc, state, keys, size));
        test("CONTROLS SEARCH471 literal key/name search and no mutation " + size[0], () -> literal(state, size));
        test("CONTROLS SEARCH471 Back/X/jump and compact bounds " + size[0], () -> navigation(state, size));
        test("CONTROLS SEARCH471 modal cancellation and underlay " + size[0], () -> modal(state, size));
      }
    }
    Files.writeString(REPORT.resolve(action + ".json"), "{\"failures\":" + failures + "}");
    log("CONTROLS SEARCH471 " + action + " failures=" + failures + " reports=" + REPORT.toAbsolutePath());
    check(failures == 0, "controls-search-471 failures=" + failures);
  }

  private static PowerOptionsScreen open(Release462Checks.State state, int[] size) throws Exception {
    if (state.options != null) { state.options.session().discard(); state.options.removed(); state.options = null; }
    field(PowerOptionsScreen.class, "rememberedView").set(null, null);
    field(PowerOptionsScreen.class, "rememberedHistory").set(null, List.of());
    PowerOptionsScreen options = state.open(size, MusicPlaylist.BETA, false);
    check(!(boolean)get(options, "keySearch"), "fresh Controls visit did not default to name search");
    set(options, "page", "Controls"); set(options, "showDisabled", true);
    set(options, "keySearch", false); set(options, "exactKeySearch", null);
    ((Deque<?>)get(options, "history")).clear(); ((Set<?>)get(options, "collapsed")).clear();
    query(options, ""); return options;
  }
  private static List<Setting> seed(PowerOptionsScreen options) throws Exception {
    List<Setting> keys = settings(options).stream().filter(s -> s.kind == Setting.Kind.KEY).toList();
    check(keys.size() >= 6, "need six visible control bindings");
    for (Setting setting : options.session().settings()) if (setting.kind == Setting.Kind.KEY) setting.value = new JsonPrimitive(0);
    int[] codes = {Keyboard.KEY_N, new Chord(Keyboard.KEY_N, Chord.CTRL).encoded(), -98,
        Keyboard.KEY_LCONTROL, Keyboard.KEY_DELETE, new Chord(Keyboard.KEY_N, Chord.CTRL | Chord.SHIFT).encoded()};
    for (int i = 0; i < codes.length; i++) keys.get(i).value = new JsonPrimitive(codes[i]);
    call(options, "layout"); return keys.subList(0, 6);
  }
  private static void capture(Minecraft mc, Release462Checks.State state) throws Exception {
    for (int[] size : SIZES) {
      var options = open(state, size); shot(mc, options, size, "name-default");
      click(options, n(options, "searchModeX") + 5, 33, 0); type(options, "ctrl+");
      shot(mc, options, size, "literal-ctrl");
      options = open(state, size); start(options); shot(mc, options, size, "capture-modal");
      key(options, '\0', Keyboard.KEY_E); shot(mc, options, size, "exact-results");
      jump(options); shot(mc, options, size, "jumped-group");
      click(options, n(options, "left") + 3, 33, 0); // Back to exact results.
      click(options, n(options, "clearSearchX") + 5, 33, 0); shot(mc, options, size, "cleared-with-back");
    }
  }
  private static void exact(Minecraft mc, Release462Checks.State state, Keys physical, int[] size) throws Exception {
    for (int which = 0; which < 5; which++) {
      PowerOptionsScreen options = open(state, size); List<Setting> seeded = seed(options);
      Map<String, String> before = values(options); start(options);
      check(options.capturingBinding() && get(options, "capture") == null, "search claimed a real binding editor");
      switch (which) {
        case 0 -> key(options, 'n', Keyboard.KEY_N);
        case 1 -> {
          // The queued Ctrl event survives a physical release before N is dispatched.
          KeyEventModifiers.event(Keyboard.KEY_LCONTROL, true); KeyEventModifiers.begin();
          try { key(options, 'n', Keyboard.KEY_N); }
          finally { KeyEventModifiers.end(); KeyEventModifiers.event(Keyboard.KEY_LCONTROL, false); }
        }
        case 2 -> click(options, 5, 30, 2);
        case 3 -> {
          physical.buffer.put(Keyboard.KEY_LCONTROL, (byte)1); key(options, '\0', Keyboard.KEY_LCONTROL);
          check(options.capturingBinding(), "modifier committed before release");
          physical.buffer.put(Keyboard.KEY_LCONTROL, (byte)0); options.render(-1, -1, 0);
        }
        case 4 -> key(options, '\0', Keyboard.KEY_DELETE);
      }
      Setting expected = seeded.get(which);
      check(!options.capturingBinding() && Objects.equals(get(options, "exactKeySearch"), expected.value.getAsInt()), "captured the wrong complete binding");
      check(settings(options).stream().map(s -> s.id).toList().equals(List.of(expected.id)), "exact capture also matched another key/chord");
      check(before.equals(values(options)), "hotkey search mutated settings or bindings");
      check(!(boolean)call(options, "searchHotkeyVisible"), "capture button remained visible beside populated search");
    }
  }
  private static void literal(Release462Checks.State state, int[] size) throws Exception {
    var options = open(state, size); List<Setting> seeded = seed(options); Map<String, String> before = values(options);
    String name = seeded.get(0).label; query(options, name);
    check(settings(options).stream().anyMatch(s -> s.id.equals(seeded.get(0).id)), "default name search omitted action label");
    click(options, n(options, "searchModeX") + 5, 33, 0); type(options, "ctrl+");
    Set<String> expected = Set.of(seeded.get(1).id, seeded.get(5).id);
    check(settings(options).stream().map(s -> s.id).collect(java.util.stream.Collectors.toSet()).equals(expected), "literal Ctrl+ key search matched action names, bare Ctrl or other keys");
    type(options, " cOnTrOl + ");
    check(settings(options).stream().map(s -> s.id).collect(java.util.stream.Collectors.toSet()).equals(expected), "key-label aliases or whitespace changed Ctrl+ results");
    query(options, "mouse 3"); check(settings(options).stream().map(s -> s.id).toList().equals(List.of(seeded.get(2).id)), "literal mouse label search failed");
    query(options, "zzzz-no-such-key"); check(settings(options).isEmpty(), "key search fuzzy-matched unrelated keys");
    check(before.equals(values(options)), "typed key/name searches mutated bindings");
  }
  private static void navigation(Release462Checks.State state, int[] size) throws Exception {
    var options = open(state, size); seed(options); Map<String, String> before = values(options);
    start(options); key(options, 'n', Keyboard.KEY_N); String exactQuery = search(options).text();
    bounds(options); jump(options);
    check(search(options).text().isEmpty() && get(options, "exactKeySearch") == null && (boolean)call(options, "searchHotkeyVisible"), "result jump retained its exact filter or hid capture");
    bounds(options);
    click(options, n(options, "left") + 3, 33, 0);
    check(search(options).text().equals(exactQuery) && Objects.equals(get(options, "exactKeySearch"), Keyboard.KEY_N), "Back lost exact search state");
    click(options, n(options, "clearSearchX") + 5, 33, 0);
    check(search(options).text().isEmpty() && get(options, "exactKeySearch") == null && (boolean)call(options, "searchHotkeyVisible"), "X did not clear exact filter and restore capture");
    bounds(options);
    click(options, n(options, "left") + 3, 33, 0);
    check(search(options).text().isEmpty() && !(boolean)get(options, "keySearch"), "Back did not restore original name-search view");
    check(before.equals(values(options)), "search navigation mutated bindings");
  }
  private static void modal(Release462Checks.State state, int[] size) throws Exception {
    var options = open(state, size); seed(options); Map<String, String> before = values(options);
    start(options); key(options, '\0', Keyboard.KEY_NONE);
    check(options.capturingBinding(), "unmapped event ended capture");
    call(options, "panelWheel", new Class<?>[]{int.class}, 120);
    check((double)get(options, "scroll") == 0, "capture wheel scrolled underlay");
    key(options, '\0', Keyboard.KEY_ESCAPE);
    check(!options.capturingBinding() && search(options).text().isEmpty(), "Escape did not cancel capture cleanly");
    start(options); click(options, size[0] / 2, size[1] / 2 + 42, 0);
    check(!options.capturingBinding() && search(options).text().isEmpty(), "Cancel searched its own mouse click");
    start(options); click(options, 5, 30, 0);
    check(get(options, "page").equals("Controls") && Objects.equals(get(options, "exactKeySearch"), -100), "capture click reached sidebar underlay");
    click(options, n(options, "clearSearchX") + 5, 33, 0); start(options); options.removed();
    check(!options.capturingBinding(), "removed screen retained input capture");
    options.init(state.mc, size[0], size[1]); start(options); call(options, "panelCancelGesture");
    check(!options.capturingBinding() && before.equals(values(options)), "gesture cancellation retained capture or changed bindings");
  }
  private static void bounds(PowerOptionsScreen options) throws Exception {
    check(n(options, "searchWidth") >= 24, "Controls search input narrower than 24px");
    int left = n(options, "searchLeft"), end = left + n(options, "searchWidth");
    check(left >= n(options, "left") && end + 4 <= n(options, "searchModeX"), "search field overlaps mode icon");
    if ((boolean)call(options, "searchHotkeyVisible")) check(end + 4 <= n(options, "searchHotkeyX")
        && n(options, "searchHotkeyX") + n(options, "searchHotkeyWidth") + 4 <= n(options, "searchModeX"), "capture button overlaps field or mode icon");
    check(n(options, "searchModeX") + 20 <= n(options, "sectionButtonX") && n(options, "sectionButtonX") + 20 <= options.width,
        "Controls toolbar exceeds its visible width");
  }
  private static void jump(PowerOptionsScreen options) throws Exception {
    Object heading = ((List<?>)get(options, "rows")).stream().filter(row -> {
      try { return (boolean)call(row, "heading"); } catch (Exception e) { throw new IllegalStateException(e); }
    }).findFirst().orElseThrow();
    int y = n(options, "top") + (int)call(heading, "y") - ((Double)get(options, "scroll")).intValue();
    click(options, n(options, "left") + n(options, "groupChevronWidth") + 8, y + 8, 0);
  }
  private static void start(PowerOptionsScreen options) throws Exception { check((boolean)call(options, "searchHotkeyVisible"), "capture button unavailable"); click(options, n(options, "searchHotkeyX") + 4, 33, 0); }
  private static void shot(Minecraft mc, PowerOptionsScreen screen, int[] size, String name) throws Exception { SettingsSnapshot.render(mc, screen, size[0], size[1], REPORT.resolve(name + "-" + size[0] + ".png")); }
  private static TextInput search(PowerOptionsScreen options) throws Exception { return (TextInput)get(options, "search"); }
  private static void query(PowerOptionsScreen options, String query) throws Exception { search(options).setText(query); call(options, "layout"); }
  private static void type(PowerOptionsScreen options, String text) throws Exception {
    query(options, ""); search(options).focused = true;
    for (char character : text.toCharArray()) key(options, character, Keyboard.KEY_NONE);
  }
  private static List<Setting> settings(PowerOptionsScreen options) throws Exception {
    List<Setting> result = new ArrayList<>();
    for (Object row : (List<?>)get(options, "rows")) { Setting setting = (Setting)call(row, "setting"); if (setting != null) result.add(setting); }
    return result;
  }
  private static Map<String, String> values(PowerOptionsScreen options) { Map<String, String> out = new LinkedHashMap<>(); for (Setting s : options.session().settings()) out.put(s.id, s.value.toString()); return out; }
  private static void click(PowerOptionsScreen s, int x, int y, int button) { ((ScreenInput)(Object)s).power$click(x, y, button); }
  private static void key(PowerOptionsScreen s, char c, int key) { ((ScreenInput)(Object)s).power$key(c, key); }
  private static int n(Object o, String name) throws Exception { return (int)call(o, name); }
  private static Field field(Class<?> type, String name) throws Exception { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
  private static Object get(Object o, String name) throws Exception { return field(o.getClass(), name).get(o); }
  private static void set(Object o, String name, Object value) throws Exception { field(o.getClass(), name).set(o, value); }
  private static Object call(Object o, String name) throws Exception { return call(o, name, new Class<?>[0]); }
  private static Object call(Object o, String name, Class<?>[] types, Object... args) throws Exception { Method m = o.getClass().getDeclaredMethod(name, types); m.setAccessible(true); return m.invoke(o, args); }

  private static final class Keys implements AutoCloseable {
    final ByteBuffer buffer; final byte[] previous; final int queued;
    Keys() throws Exception {
      buffer = (ByteBuffer)field(Keyboard.class, "keyDownBuffer").get(null); previous = new byte[buffer.capacity()];
      for (int i = 0; i < previous.length; i++) previous[i] = buffer.get(i);
      for (int key : new int[]{29, 157, 42, 54, 56, 184}) buffer.put(key, (byte)0);
      queued = field(KeyEventModifiers.class, "down").getInt(null); KeyEventModifiers.reconcile(0);
    }
    public void close() throws Exception { for (int i = 0; i < previous.length; i++) buffer.put(i, previous[i]); field(KeyEventModifiers.class, "down").setInt(null, queued); }
  }
  private static final class Navigation implements AutoCloseable {
    final Map<Field, Object> values = new LinkedHashMap<>(); final Map<String, Double> positions; final Set<String> collapsed;
    @SuppressWarnings("unchecked") Navigation() throws Exception {
      for (String name : List.of("rememberedPage", "rememberedView", "rememberedHistory", "rememberedSideScroll", "showDisabled")) { Field f = field(PowerOptionsScreen.class, name); values.put(f, f.get(null)); }
      positions = new HashMap<>((Map<String, Double>)field(PowerOptionsScreen.class, "positions").get(null));
      collapsed = new HashSet<>((Set<String>)field(PowerOptionsScreen.class, "rememberedCollapsed").get(null));
    }
    @SuppressWarnings("unchecked") public void close() throws Exception {
      for (var entry : values.entrySet()) entry.getKey().set(null, entry.getValue());
      var current = (Map<String, Double>)field(PowerOptionsScreen.class, "positions").get(null); current.clear(); current.putAll(positions);
      var groups = (Set<String>)field(PowerOptionsScreen.class, "rememberedCollapsed").get(null); groups.clear(); groups.addAll(collapsed);
    }
  }
}
