package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.JsonPrimitive;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.config.*;
import local.luke.power.config.backend.VisualBackend;
import local.luke.power.recipes.*;
import local.luke.power.rules.GameRule;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.visual.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.*;

/** Rendered Options/search and recipe hints, with preferences and gameplay inventory restored. */
final class Interface442Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/interface-442");
  private static Object invoke(Object owner, String name, Class<?>[] types, Object... values) throws Exception {
    Method method = owner.getClass().getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(owner, values);
  }
  private static Field scopeChoice() throws Exception {
    Field field = Class.forName("local.luke.power.ui.OptionsSearchScope").getDeclaredField("choice"); field.setAccessible(true); return field;
  }
  private static List<?> rows(PowerOptionsScreen screen) throws Exception { return (List<?>)field(screen, "rows"); }
  private static void layout(PowerOptionsScreen screen, String page, String query) throws Exception {
    field(screen, "page", page); field(screen, "showDisabled", true); field(screen, "changedOnly", false);
    field(screen, "conflictIds", List.of()); field(screen, "libraryOpen", false); ((Set<?>)field(screen, "collapsed")).clear();
    ((TextInput)field(screen, "search")).setText(query); call(screen, "layout");
    field(screen, "sideScroll", Math.min(((ScrollBar.Track)call(screen, "sidebarTrack")).maximum(), (double)(screen.visiblePages().indexOf(page) * 22)));
  }
  private static int brightness(int rgb) { return (rgb >> 16 & 255) + (rgb >> 8 & 255) + (rgb & 255); }
  private static int sidebarPixel(PowerOptionsScreen screen, Path path, String page) throws Exception {
    BufferedImage image = ImageIO.read(path.toFile());
    int y = 24 + screen.visiblePages().indexOf(page) * 22 - ((Double)field(screen, "sideScroll")).intValue() + 16;
    return image.getRGB(((int)call(screen, "origin") + (int)call(screen, "sidebar") - 5) * 2, y * 2);
  }
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "Focus a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "Mute the disposable client");
    Screen parent = mc.currentScreen; boolean repeat = Keyboard.areRepeatEventsEnabled(), choice = scopeChoice().getBoolean(null);
    PowerConfig.flushPending();
    VisualSettings visual = VisualConfig.copy(); byte[] disk = Files.readAllBytes(PowerConfig.path());
    failures = 0; Files.createDirectories(REPORT);
    try {
      var screen = new PowerOptionsScreen(parent); screen.init(mc, 640, 420);
      test("INTERFACE442 rendered T search retains sidebar selection at 320 and 640", () -> {
        for (int[] size : new int[][]{{320, 240}, {640, 420}}) {
          screen.init(mc, size[0], size[1]); scopeChoice().setBoolean(null, false); layout(screen, "Camera", "zoom");
          Path all = REPORT.resolve("search-all-" + size[0] + ".png"); SettingsSnapshot.render(mc, screen, size[0], size[1], all);
          scopeChoice().setBoolean(null, true); layout(screen, "Camera", "zoom");
          Path tab = REPORT.resolve("search-tab-" + size[0] + ".png"); SettingsSnapshot.render(mc, screen, size[0], size[1], tab);
          check(brightness(sidebarPixel(screen, tab, "Camera")) < brightness(sidebarPixel(screen, all, "Camera")), "T query did not render selected sidebar background at width " + size[0]);
        }
      });
      test("INTERFACE442 Controls query keeps sidebar selection and plain functional group headings", () -> {
        for (int[] size : new int[][]{{320, 240}, {640, 420}}) {
          screen.init(mc, size[0], size[1]); scopeChoice().setBoolean(null, false); layout(screen, "Controls", "zoom");
          int matches = 0;
          for (Object row : rows(screen)) {
            Setting setting = (Setting)call(row, "setting");
            if (setting == null) { check(!((String)call(row, "group")).startsWith("Controls /"), "Controls heading retained its redundant prefix"); continue; }
            check(setting.kind == Setting.Kind.KEY && call(row, "group").equals(setting.group), "Controls query leaked another page or rewrote the group"); matches++;
          }
          check(matches > 0, "Native Controls Zoom binding disappeared");
          Path controls = REPORT.resolve("controls-search-" + size[0] + ".png"); SettingsSnapshot.render(mc, screen, size[0], size[1], controls);
          double sideScroll = (double)field(screen, "sideScroll");
          layout(screen, "Camera", "zoom"); field(screen, "sideScroll", sideScroll);
          Path all = REPORT.resolve("controls-compare-" + size[0] + ".png"); SettingsSnapshot.render(mc, screen, size[0], size[1], all);
          check(brightness(sidebarPixel(screen, controls, "Controls")) < brightness(sidebarPixel(screen, all, "Controls")), "Controls query lost the selected sidebar fill");
        }
      });
      test("INTERFACE442 disabled Creative permission hides the complete page and its search results", () -> {
        Setting creative = screen.session().settings().stream().filter(s -> s.id.equals("rules.world." + GameRule.ALLOW_CREATIVE_MODE.id())).findFirst().orElseThrow();
        var before = creative.value.deepCopy();
        try {
          creative.value = new JsonPrimitive(false); layout(screen, "General", "reach");
          check(!screen.visiblePages().contains("Creative"), "Creative page remained visible without permission");
          for (Object row : rows(screen)) {
            Setting setting = (Setting)call(row, "setting");
            check(setting == null || !setting.page.equals("Creative"), "All search exposed a hidden Creative page row");
          }
          for (String id : List.of("creative.blockReach", "creative.entityReach")) {
            Setting setting = screen.session().settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow();
            check(SettingAccess.owner(setting) == GameRule.ALLOW_CREATIVE_MODE && !SettingAccess.visible(screen.session(), setting), "Creative reach lacks its permission owner");
          }
          creative.value = new JsonPrimitive(true); layout(screen, "Creative", "");
          check(screen.visiblePages().contains("Creative"), "Creative page did not return when enabled");
        } finally { creative.value = before; call(screen, "layout"); }
      });
      test("INTERFACE442 preview reach has independent five-block defaults and reversible live drafts", () -> {
        Object creative = Class.forName("local.luke.power.creative.config.Config").getMethod("current").invoke(null);
        String creativeBefore = Catalog.JSON.toJson(creative);
        ConfigSession session = new ConfigSession(); VisualBackend.register(session, mc);
        Setting blocks = session.settings().stream().filter(s -> s.id.equals("visual.containerBlockReach")).findFirst().orElseThrow();
        Setting entities = session.settings().stream().filter(s -> s.id.equals("visual.containerEntityReach")).findFirst().orElseThrow();
        check(new VisualSettings().containerBlockReach == 5 && new VisualSettings().containerEntityReach == 5
            && blocks.defaultValue.getAsDouble() == 5 && entities.defaultValue.getAsDouble() == 5, "Preview defaults do not use five blocks");
        check(SettingAccess.owner(blocks) == null && SettingAccess.owner(entities) == null, "Container preview range inherits Creative permissions");
        double blockBefore = VisualConfig.current().containerBlockReach, entityBefore = VisualConfig.current().containerEntityReach;
        blocks.value = new JsonPrimitive(3); entities.value = new JsonPrimitive(10); session.preview();
        check(VisualConfig.current().containerBlockReach == 3 && VisualConfig.current().containerEntityReach == 10
            && creativeBefore.equals(Catalog.JSON.toJson(creative)), "Preview reach draft changed gameplay reach or coupled the two ranges");
        session.discard(); check(VisualConfig.current().containerBlockReach == blockBefore && VisualConfig.current().containerEntityReach == entityBefore, "Cancel did not restore preview distances");
      });
      try (var state = new Recipe402Checks.State(mc)) {
        test("INTERFACE442 complete ghost hint renders only with all matching cells and output empty", () -> {
          state.reset(640); var book = state.book(); var context = state.context();
          @SuppressWarnings("unchecked") var recipes = (List<RecipeDefinition>)field(field(book, "model"), "recipes");
          RecipeDefinition sticks = recipes.stream().filter(recipe -> recipe.output.itemId == 280 && recipe.layout(context.width, context.height) != null).findFirst().orElseThrow();
          var probe = new GhostProbe(book); probe.init(mc, 640, 360); field(book, "ghost", null);
          Path empty = REPORT.resolve("ghost-empty.png"); SettingsSnapshot.render(mc, probe, 640, 360, empty);
          field(book, "ghost", sticks); Path visible = REPORT.resolve("ghost-visible.png"); SettingsSnapshot.render(mc, probe, 640, 360, visible);
          check(!pixels(empty, visible), "Empty matching grid did not render its recipe hint");
          List<?> cells = sticks.layout(context.width, context.height); int matching = -1, unrelated = -1;
          for (int index = 0; index < cells.size(); index++) if (cells.get(index) != null) matching = index; else unrelated = index;
          check(matching >= 0, "Sticks fixture has no ingredient cells");
          context.inputs.get(matching).setStack(new ItemStack(5, 1, 0)); context.output.setStack(null);
          Path occupied = REPORT.resolve("ghost-occupied-input.png"); SettingsSnapshot.render(mc, probe, 640, 360, occupied);
          check(pixels(empty, occupied), "Occupied matching ingredient left a partial ghost hint"); context.inputs.get(matching).setStack(null);
          context.output.setStack(new ItemStack(280, 4, 0)); Path output = REPORT.resolve("ghost-occupied-output.png"); SettingsSnapshot.render(mc, probe, 640, 360, output);
          check(pixels(empty, output), "Occupied output left an ingredient ghost hint"); context.output.setStack(null);
          if (unrelated >= 0) {
            context.inputs.get(unrelated).setStack(new ItemStack(4, 1, 0)); context.output.setStack(null);
            Path other = REPORT.resolve("ghost-unrelated-cell.png"); SettingsSnapshot.render(mc, probe, 640, 360, other);
            check(pixels(visible, other), "Unrelated occupied cell suppressed the matching recipe hint"); context.inputs.get(unrelated).setStack(null);
          }
        });
      }
      check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "Interface drafts changed persistent configuration");
    } finally {
      VisualConfig.preview(visual); scopeChoice().setBoolean(null, choice); mc.currentScreen = parent; Keyboard.enableRepeatEvents(repeat);
    }
    log("INTERFACE442 failures=" + failures + " captures=" + REPORT); check(failures == 0, "Interface442 failures=" + failures);
  }
  private static boolean pixels(Path first, Path second) throws Exception {
    BufferedImage a = ImageIO.read(first.toFile()), b = ImageIO.read(second.toFile());
    return Arrays.equals(a.getRGB(0, 0, a.getWidth(), a.getHeight(), null, 0, a.getWidth()), b.getRGB(0, 0, b.getWidth(), b.getHeight(), null, 0, b.getWidth()));
  }
  private static final class GhostProbe extends Screen {
    private final RecipeBookOverlay book;
    GhostProbe(RecipeBookOverlay book) { this.book = book; }
    @Override public void render(int x, int y, float delta) {
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
      try {
        GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING); fill(0, 0, width, height, 0xff202020);
        invoke(book, "renderGhost", new Class<?>[]{int.class, int.class, RecipeBookLayout.class}, -1, -1, book.layout());
      } catch (Exception error) { throw new IllegalStateException(error); }
      finally { GL11.glPopAttrib(); }
    }
  }
}
