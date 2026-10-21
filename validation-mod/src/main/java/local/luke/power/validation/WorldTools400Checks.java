package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import local.luke.power.input.PlayerMode;
import local.luke.power.offhand.Offhand;
import local.luke.power.rules.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import local.luke.power.world.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_180;
import net.minecraft.class_591;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.*;
import net.modificationstation.stationapi.api.client.gui.screen.EditWorldScreen;
import net.modificationstation.stationapi.api.client.gui.widget.ButtonWidgetAttachedContext;
import org.lwjgl.opengl.*;

/** Native title-menu workflows; the only source is a new, token-owned synthetic fixture world. */
public final class WorldTools400Checks {
  private static WorldTools400Files files;
  private static class_180 recreation;
  private static String firstCopyId, secondCopyId;
  private WorldTools400Checks() {}

  public static void run(Minecraft mc, String phase) throws Exception {
    if (phase.equals("native")) { nativeFlows(mc); return; }
    check(files != null, "run world-tools-400 native first in this client session");
    if (phase.equals("create")) create(mc);
    else if (phase.equals("created")) created(mc);
    else if (phase.equals("cleanup")) cleanup(mc);
    else throw new IllegalArgumentException("native, create, created or cleanup");
  }
  private static void nativeFlows(Minecraft mc) throws Exception {
    check(mc.world == null, "leave the disposable world first; this fixture never closes another world");
    check(files == null, "clean up the previous world-tools fixture first");
    check(Display.getWidth() >= 1280 && Display.getHeight() >= 720, "resize the muted disposable client to at least1280x720 for native wide/narrow captures");
    check(!local.luke.power.config.RuleDefaults.review().required(), "review the disposable clone's creation defaults before testing native recreation");
    failures = 0; files = new WorldTools400Files(FabricLoader.getInstance().getGameDir());
    log("WORLD TOOLS400 OWNERSHIP " + files.report.resolve("ownership.json"));
    WorldToolsSettings saved = WorldToolsConfig.copy(); Screen previous = mc.currentScreen;
    try {
      test("saved-world tool buttons follow off/on/mixed preferences in one client session without blank rows", () -> menus(mc));
      test("native backup screen publishes a byte-exact world/map/recovery archive", () -> {
        var screen = operation(mc, "Back up world"); shots(mc, screen, "backup-before");
        WorldSnapshots.Task task = start(screen); shots(mc, screen, "backup-progress"); await(task);
        check(task.error().isEmpty() && task.result() != null, "native backup failed: " + task.error());
        files.backup(task.result()); shots(mc, screen, "backup-complete");
        log("WORLD TOOLS400 BACKUP " + task.result().path());
      });
      test("two native duplicate actions preserve data with independent identities, maps and fenced fallbacks", () -> {
        var screen = operation(mc, "Duplicate world"); shots(mc, screen, "duplicate-before");
        var task = start(screen); shots(mc, screen, "duplicate-progress"); await(task);
        check(task.error().isEmpty() && task.result() != null, "native duplicate failed: " + task.error());
        firstCopyId = files.duplicate(task.result()); shots(mc, screen, "duplicate-complete");
        screen = operation(mc, "Duplicate world"); task = start(screen); await(task);
        check(task.error().isEmpty() && task.result() != null, "second duplicate failed: " + task.error());
        secondCopyId = files.duplicate(task.result()); check(!firstCopyId.equals(secondCopyId), "successive copies reused an identity");
      });
      test("offline access refuses a concurrent lease and leaves fixture bytes unchanged", () -> {
        try (var lease = new SavedWorldAccess(files.game, files.folder, () -> mc.world == null).acquire()) {
          try { WorldSnapshots.start(files.game, files.folder, WorldSnapshots.Action.DUPLICATE, files.folder + "-blocked", () -> mc.world == null); throw new AssertionError("concurrent operation was admitted"); }
          catch (IOException expected) { log("WORLD TOOLS400 EXPECTED lease refusal " + expected.getMessage()); }
        }
        files.unchanged();
      });
      test("symlink source entries are refused without following or deleting their targets", () -> {
        Path link = files.source.resolve("data/refuse-link"), target = files.source.resolve("data/fixture.txt");
        Files.createSymbolicLink(link, target);
        try {
          var task = WorldSnapshots.start(files.game, files.folder, WorldSnapshots.Action.BACKUP, files.folder, () -> mc.world == null); await(task);
          check(task.result() == null && !task.error().isEmpty(), "symlink backup was admitted");
          check(Files.isSymbolicLink(link) && Files.readSymbolicLink(link).equals(target), "backend changed source symlink ownership");
          check(Files.readString(target).contains(files.token), "backend followed or changed symlink target");
        } finally { if (Files.isSymbolicLink(link) && Files.readSymbolicLink(link).equals(target)) Files.delete(link); }
        files.unchanged();
      });
      test("future world rules block native duplication and recreation while a raw backup retains exact bytes", () -> unsupported(mc));
      test("native Cancel keeps original world/recovery bytes and releases the storage lease", () -> {
        var screen = operation(mc, "Back up world"); var task = start(screen);
        if (!task.finished()) { ((ScreenInput)(Object)screen).power$click(screen.width / 2, screen.height - 20, 0); shots(mc, screen, "canceling"); }
        await(task);
        if (task.result() != null) { files.rememberBackup(task.result().path()); log("SKIP native cancel timing window: the copy completed before the cancel press"); }
        else check(!task.error().isEmpty(), "canceled task has no completion or cancellation result");
        files.unchanged(); SavedWorldAccess.requireAvailable(files.game); shots(mc, screen, "cancel-complete");
      });
      test("native Re-create opens a Custom Survival draft containing only seed/rules/difficulty", () -> recreation(mc));
    } finally {
      config(saved); files.manifest();
      if (recreation == null && mc.world == null) mc.setScreen(previous);
      Files.writeString(files.report.resolve("native-result.json"), "{\"failures\":" + failures + ",\"firstCopyId\":\"" + Objects.toString(firstCopyId, "")
          + "\",\"secondCopyId\":\"" + Objects.toString(secondCopyId, "") + "\"}\n");
    }
    log("WORLD TOOLS400 NATIVE failures=" + failures + " reports=" + files.report + "; next create, then created, then cleanup");
  }
  private static void menus(Minecraft mc) throws Exception {
    config(new WorldToolsSettings()); var off = menu(mc); int ordinary = buttons(off).size();
    check(labels(off).stream().noneMatch(WorldTools400Checks::tool), "disabled tool retained a row"); shots(mc, off, "menu-off");
    allOn(); var on = menu(mc); check(buttons(on).size() == ordinary + 3, "enabled tools left duplicates or blank rows");
    for (String label : List.of("Back up world", "Duplicate world", "Re-create from seed")) check(Collections.frequency(labels(on), label) == 1, "tool registration duplicated/missing: " + label);
    shots(mc, on, "menu-on");
    WorldToolsSettings mixed = new WorldToolsSettings(); mixed.duplicate = true; config(mixed); var one = menu(mc);
    check(buttons(one).size() == ordinary + 1 && labels(one).contains("Duplicate world") && !labels(one).contains("Back up world") && !labels(one).contains("Re-create from seed"), "mixed tool preferences did not compact the list");
    shots(mc, one, "menu-mixed"); allOn();
  }
  private static boolean tool(String value) { return Set.of("Back up world", "Duplicate world", "Re-create from seed").contains(value); }
  private static EditWorldScreen menu(Minecraft mc) throws Exception {
    var screen = new EditWorldScreen(new TitleScreen(), new class_591(files.folder, files.folder, 1, 0, false));
    mc.setScreen(screen); screen.init(mc, 640, 360); return screen;
  }
  private static List<ButtonWidgetAttachedContext> buttons(EditWorldScreen screen) throws Exception { return ((WorldEditButtons)UiChecks.field(screen, "buttonMenu")).power$editButtons(); }
  private static List<String> labels(EditWorldScreen screen) throws Exception { return buttons(screen).stream().map(entry -> entry.button().text).toList(); }
  private static void press(EditWorldScreen screen, String label) throws Exception {
    var entry = buttons(screen).stream().filter(button -> button.button().text.equals(label)).findFirst().orElseThrow(() -> new AssertionError("installed world-menu action missing: " + label));
    entry.action().onPress(entry.button());
  }
  private static WorldOperationScreen operation(Minecraft mc, String action) throws Exception {
    allOn(); var menu = menu(mc); press(menu, action);
    check(mc.currentScreen instanceof WorldOperationScreen, "native Edit action did not open its operation screen");
    var operation = (WorldOperationScreen)mc.currentScreen; operation.init(mc, 640, 360); return operation;
  }
  private static WorldSnapshots.Task start(WorldOperationScreen screen) throws Exception {
    ((ScreenInput)(Object)screen).power$click(screen.width / 2 - 75, screen.height - 20, 0);
    var task = (WorldSnapshots.Task)UiChecks.field(screen, "task"); check(task != null, "native Start did not admit a task: " + UiChecks.field(screen, "error")); return task;
  }
  private static void await(WorldSnapshots.Task task) throws Exception {
    long deadline = System.nanoTime() + 25_000_000_000L;
    while (!task.finished() && System.nanoTime() < deadline) Thread.sleep(5);
    if (!task.finished()) { task.cancel(); throw new AssertionError("world operation exceeded the fixture's25-second deadline; preserve its ownership manifest and retry cleanup after it stops"); }
  }
  private static void unsupported(Minecraft mc) throws Exception {
    Path level = files.source.resolve("level.dat"); byte[] before = Files.readAllBytes(level);
    var root = WorldRuleFiles.read(level); root.getCompound("Data").getCompound(WorldRuleData.TAG).putInt("version", 999);
    WorldTools400Files.writeNbt(level, root); byte[] future = Files.readAllBytes(level);
    try {
      var screen = operation(mc, "Duplicate world"); var task = start(screen); await(task);
      check(task.result() == null && !task.error().isEmpty(), "future rules were duplicated"); shots(mc, screen, "unsupported-error");
      try { WorldRecreation.read(files.game, files.folder, () -> mc.world == null); throw new AssertionError("future rules were recreated"); }
      catch (IOException | IllegalArgumentException expected) { log("WORLD TOOLS400 EXPECTED future-rule refusal " + expected.getMessage()); }
      task = WorldSnapshots.start(files.game, files.folder, WorldSnapshots.Action.BACKUP, files.folder + "-future", () -> mc.world == null); await(task);
      check(task.result() != null && task.error().isEmpty(), "raw future-data backup failed: " + task.error()); files.rememberBackup(task.result().path());
      try (ZipFile zip = new ZipFile(task.result().path().toFile())) { check(Arrays.equals(future, zip.getInputStream(zip.getEntry("world/level.dat")).readAllBytes()), "raw future metadata bytes changed"); }
      check(Arrays.equals(future, Files.readAllBytes(level)), "refusal changed unsupported metadata");
    } finally { local.luke.power.config.FileTransaction.atomicWrite(level, before); }
    files.unchanged();
  }
  private static void recreation(Minecraft mc) throws Exception {
    allOn(); var menu = menu(mc); press(menu, "Re-create from seed");
    check(mc.currentScreen instanceof class_180, "recreation was interrupted by a defaults review or failed screen"); recreation = (class_180)mc.currentScreen;
    recreation.init(mc, 640, 360);
    var seed = (TextFieldWidget)UiChecks.field(recreation, "field_633"); check(seed.getText().equals("0"), "recreated numeric zero seed was lost");
    var draft = (CreationRules)UiChecks.field(recreation, "power$draft");
    check(draft.preset() == CreationRules.Preset.CUSTOM && draft.mode().equals("survival") && draft.rules().equals(files.rules), "recreation copied mode or changed the rules draft");
    check((int)UiChecks.field(recreation, "power$difficulty") == 3, "recreation difficulty changed");
    ((TextFieldWidget)UiChecks.field(recreation, "field_632")).setText(files.folder + "-fresh"); UiChecks.call(recreation, "method_608");
    String folder = (String)UiChecks.field(recreation, "field_634"); files.fresh = files.game.resolve("saves").resolve(folder).normalize();
    check(files.fresh.getParent().equals(files.game.resolve("saves")) && folder.startsWith(files.folder) && !Files.exists(files.fresh, LinkOption.NOFOLLOW_LINKS), "fresh creation selected an existing/nonfixture directory");
    shots(mc, recreation, "recreate-draft"); files.unchanged(); files.manifest();
  }
  private static void create(Minecraft mc) throws Exception {
    check(mc.world == null && recreation != null && mc.currentScreen == recreation, "keep the prepared fixture recreation screen open");
    check(!Files.exists(files.fresh, LinkOption.NOFOLLOW_LINKS), "refusing to overwrite an existing recreation directory");
    local.luke.power.map.MapIO.awaitQueued(); files.beforeCreate();
    ButtonWidget create = ((ScreenInput)recreation).power$buttons().stream().filter(button -> button.id == 0).findFirst().orElseThrow();
    ((ScreenInput)recreation).power$click(create.x + 4, create.y + 4, 0);
    check(mc.world != null && WorldRuleChanges.directory(mc).toAbsolutePath().normalize().equals(files.fresh), "native Create did not load the owned fresh directory");
    files.created(files.fresh); created(mc);
  }
  private static void created(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && WorldRuleChanges.directory(mc).toAbsolutePath().normalize().equals(files.fresh), "open only the fixture's freshly recreated world");
    check(Files.readString(files.fresh.resolve(WorldTools400Files.MARKER)).equals(files.token), "fresh world ownership marker missing");
    var properties = mc.world.method_262(); String identity = ((local.luke.power.input.WorldIdentity)properties).power$commandWorldId();
    check(properties.getSeed() == 0 && !identity.equals(files.id) && !identity.equals(firstCopyId) && !identity.equals(secondCopyId), "fresh recreation reused copied identity or lost zero seed");
    check(((WorldDifficulty)properties).power$difficulty() == 3 && WorldRules.of(properties).equals(files.rules), "fresh recreation changed rules/difficulty");
    check(mc.player instanceof PlayerMode mode && !mode.power$creative() && !mode.power$spectator(), "recreation promoted starting game mode");
    check(Offhand.get(mc.player.inventory) == null && Arrays.stream(mc.player.inventory.main).allMatch(Objects::isNull), "recreation copied original player inventory/offhand");
    check(properties.getTime() < 10000 && !properties.asNbt().getCompound("Player").contains("WorldTools400Player"), "recreation copied original simulation/player data");
    files.freshIdentity = identity; files.freshMap(); WorldRuleChanges.save(mc); files.sourceUnchanged(); files.manifest();
    Files.writeString(files.report.resolve("recreated-result.json"), "{\"worldId\":\"" + identity + "\",\"seed\":0,\"difficulty\":3,\"mode\":\"survival\",\"inventoryCopied\":false}\n");
    log("WORLD TOOLS400 RECREATED PASS " + files.fresh + " newWorldId=" + identity + "; cleanup closes only this marked fixture world");
  }
  private static void cleanup(Minecraft mc) throws Exception {
    if (mc.world != null) {
      check(files.fresh != null && WorldRuleChanges.directory(mc).toAbsolutePath().normalize().equals(files.fresh)
          && Files.readString(files.fresh.resolve(WorldTools400Files.MARKER)).equals(files.token), "refusing to close a nonfixture world");
      WorldRuleChanges.save(mc); mc.setWorld(null);
    }
    local.luke.power.map.Maps.leave(); local.luke.power.map.MapIO.awaitQueued(); local.luke.power.storage.JournalQueue.awaitQueued();
    files.freshMap(); SavedWorldAccess.requireAvailable(files.game); mc.setScreen(new TitleScreen()); files.cleanup();
    log("WORLD TOOLS400 CLEANUP PASS retained reports=" + files.report); files = null; recreation = null; firstCopyId = secondCopyId = null;
  }
  private static void allOn() throws Exception { var settings = new WorldToolsSettings(); settings.backup = settings.duplicate = settings.recreate = true; config(settings); }
  private static void config(WorldToolsSettings settings) throws Exception { var field = WorldToolsConfig.class.getDeclaredField("value"); field.setAccessible(true); field.set(null, settings); }
  private static void shots(Minecraft mc, Screen screen, String name) throws Exception {
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try { for (int[] size : new int[][]{{320, 240}, {640, 360}}) { screen.init(mc, size[0], size[1]); SettingsSnapshot.render(mc, screen, size[0], size[1], files.report.resolve(name + "-" + size[0] + ".png")); } }
    finally { GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPopAttrib(); }
  }
}
