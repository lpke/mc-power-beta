package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.validation.mixin.ScreenInput;
import local.luke.power.visual.*;
import local.luke.power.permissions.CheatWorld;
import net.minecraft.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/** Runs only in a disposable world. Uses real portal callbacks and container cleanup. */
final class PortalInventoryChecks {
  private static final String ID = "visual.inventoryInPortals";

  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("saved")) {
      check(VisualConfig.current().inventoryInPortals, "portal setting lost on restart");
      check(Release110Checks.find(SettingsRegistry.open(mc), ID).value.getAsBoolean(), "UI lost saved value");
      check(mc.world != null, "run new-world to reload the test save first");
      assertStack(mc.player.inventory.main[8], 1, 83, "travel-carried");
      log("PASS portal setting and carried item count/damage/NBT survived restart");
      return;
    }
    if (action.equals("open")) {
      enable(true);
      mc.player.field_504 = 0;
      mc.player.field_511 = 0; mc.player.method_1388();
      mc.setScreen(new class_585(mc.player));
      return;
    }
    failures = 0;
    enable(false);
    check(mc.world != null, "run new-world first");
    ((CheatWorld) mc.world.method_262()).power$cheatsEnabled(true);
    CreativeVehicleChecks.mode(mc, "SURVIVAL");
    mc.setScreen(null);
    ((local.luke.power.world.WorldDifficulty) mc.world.method_262()).power$difficulty(0);
    mc.world.field_213 = 0;
    for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
      mc.world.method_200(x, 100, z, 1);
      for (int y = 101; y < 106; y++) mc.world.method_200(x, y, z, 0);
    }
    mc.player.method_1340(.5, 103, .5);
    ((CheatWorld) mc.world.method_262()).power$cheatsEnabled(false);
    test("default off, visible without cheats, preview Cancel and Apply", () -> {
      check(!new VisualSettings().inventoryInPortals, "Java default enabled");
      var session = SettingsRegistry.open(mc);
      var row = Release110Checks.find(session, ID);
      check(!row.defaultValue.getAsBoolean() && row.page.equals("Inventory"), "wrong default/page");
      check(SettingAccess.visible(session, row) && !row.description.isBlank(), "QoL setting hidden/help missing");
      byte[] before = Files.readAllBytes(PowerConfig.path());
      row.parse("true"); session.preview();
      check(VisualConfig.current().inventoryInPortals, "preview missing");
      session.discard();
      check(!VisualConfig.current().inventoryInPortals, "Cancel failed");
      check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "Cancel wrote settings");
      var old = PowerConfig.document().getAsJsonObject("settings");
      session = SettingsRegistry.open(mc);
      Release110Checks.find(session, ID).parse("true"); session.save(Path.of("."));
      var saved = PowerConfig.document().getAsJsonObject("settings");
      check(saved.getAsJsonObject("visual").get("inventoryInPortals").getAsBoolean(), "Apply not saved");
      for (var entry : old.entrySet()) if (!entry.getKey().equals("visual"))
        check(saved.get(entry.getKey()).equals(entry.getValue()), "unrelated section changed: " + entry.getKey());
    });
    test("disabled option retains vanilla portal closure and warm-up", () -> {
      enable(false); mc.player.field_504 = 0;
      mc.setScreen(new class_585(mc.player)); pulse(mc);
      check(mc.currentScreen == null, "vanilla closure blocked");
      check(Math.abs(mc.player.field_504 - .0125F) < .00001, "warm-up changed");
    });
    test("enabled inventory stays open with cursor, crafting, damage and NBT intact", () -> {
      enable(true); mc.player.field_504 = 0;
      mc.setScreen(new class_585(mc.player)); Screen inventory = mc.currentScreen;
      ItemStack cursor = tagged(267, 1, 37, "cursor"), craft = tagged(4, 11, 0, "craft");
      mc.player.inventory.setCursorStack(cursor); grid(mc).setStack(0, craft);
      for (int i = 0; i < 8; i++) pulse(mc);
      check(mc.currentScreen == inventory, "inventory closed during warm-up");
      check(Math.abs(mc.player.field_504 - .1F) < .00001, "portal clock stopped");
      check(mc.player.inventory.getCursorStack() == cursor && grid(mc).getStack(0) == craft, "items moved");
      assertStack(cursor, 1, 37, "cursor"); assertStack(craft, 11, 0, "craft");
      ((ScreenInput) inventory).power$key('\0', 1);
      check(mc.currentScreen == null && mc.player.inventory.getCursorStack() == null && grid(mc).getStack(0) == null, "Escape cleanup failed");
      check(count(mc, mc.world, cursor) == 1 && count(mc, mc.world, craft) == 1, "Escape lost/duplicated items");
      pulse(mc);
      check(count(mc, mc.world, cursor) == 1 && count(mc, mc.world, craft) == 1, "later tick duplicated items");
    });
    test("other screens and unknown inventory subclasses still close", () -> {
      for (Screen screen : List.of(new ChatScreen(), new class_585(mc.player) {})) {
        mc.setScreen(screen); mc.player.field_504 = 0; pulse(mc);
        check(mc.currentScreen == null, "unsupported screen retained");
      }
      mc.world.method_200(2, 101, 0, 54);
      var chest = (net.minecraft.inventory.Inventory) mc.world.method_1777(2, 101, 0);
      ItemStack stored = tagged(267, 1, 53, "chest"); chest.setStack(0, stored);
      mc.player.method_486(chest); pulse(mc);
      check(mc.currentScreen == null && chest.getStack(0) == stored, "chest altered or retained");
      assertStack(stored, 1, 53, "chest");
    });
    test("switching off closes the open inventory with one cleanup", () -> {
      mc.setScreen(new class_585(mc.player)); ItemStack cursor = tagged(264, 17, 0, "disable");
      mc.player.inventory.setCursorStack(cursor); enable(false); pulse(mc); pulse(mc);
      check(mc.currentScreen == null && count(mc, mc.world, cursor) == 1, "disable lost/duplicated cursor");
      assertStack(cursor, 17, 0, "disable"); enable(true);
    });
    test("remote portal timing stays server-owned", () -> {
      boolean remote = mc.world.isRemote;
      try {
        mc.world.isRemote = true; mc.player.field_504 = .99F;
        int dimension = mc.player.dimensionId;
        mc.setScreen(new class_585(mc.player)); Screen screen = mc.currentScreen; pulse(mc);
        check(mc.player.dimensionId == dimension && mc.currentScreen == screen, "client initiated remote travel");
      } finally { mc.world.isRemote = remote; mc.player.closeScreen(); mc.player.field_504 = 0; }
    });
    for (String mode : List.of("survival", "creative", "spectator"))
      test(mode + " portal transfer cleans items before dimension change", () -> travel(mc, mode));
    log("PORTAL INVENTORY FAILURES " + failures);
  }

  private static void travel(Minecraft mc, String mode) throws Exception {
    ((CheatWorld) mc.world.method_262()).power$cheatsEnabled(true);
    ChatChecks.submit(mc, "/gamemode " + mode);
    mc.player.closeScreen(); mc.player.field_511 = 0; mc.player.field_504 = 0;
    World source = mc.world; int dimension = mc.player.dimensionId;
    int x = (int) Math.floor(mc.player.x), y = (int) Math.floor(mc.player.boundingBox.minY), z = (int) Math.floor(mc.player.z);
    for (int dx = -1; dx <= 2; dx++) for (int dy = -1; dy <= 3; dy++)
      source.method_200(x + dx, y + dy, z, dx == -1 || dx == 2 || dy == -1 || dy == 3 ? 49 : 90);
    mc.setScreen(new class_585(mc.player));
    String suffix = mode + "-" + java.util.UUID.randomUUID();
    String cursorTag = "travel-cursor-" + suffix, craftTag = "travel-craft-" + suffix;
    ItemStack cursor = tagged(267, 1, 71, cursorTag), craft = tagged(4, 13, 0, craftTag);
    ItemStack carried = tagged(267, 1, 83, "travel-carried"); mc.player.inventory.main[8] = carried;
    mc.player.inventory.setCursorStack(cursor); grid(mc).setStack(0, craft);
    double oldX = mc.player.x, oldZ = mc.player.z;
    int ticks = 0;
    do {
      Block.BLOCKS[90].method_1615(source, x, y, z, mc.player);
      mc.player.method_937(); ticks++;
      if (mc.world == source) check(mc.currentScreen instanceof class_585, "inventory closed before travel");
    } while (mc.world == source && ticks < 85);
    check(mc.player.dimensionId != dimension, "portal did not transfer");
    check(mode.equals("survival") ? ticks >= 80 && ticks <= 81 : ticks == 1, "portal delay changed: " + ticks);
    check(mc.player.field_511 > 0, "cooldown removed");
    check(mc.currentScreen == null && mc.player.container == mc.player.playerContainer, "inventory not closed/reset");
    check(mc.player.inventory.getCursorStack() == null && grid(mc).getStack(0) == null, "temporary items retained");
    for (ItemStack item : List.of(cursor, craft)) {
      check(count(mc, source, item) == 1, "travel lost/duplicated item");
      var drops = source.field_198.stream().filter(e -> e instanceof class_142 drop && !drop.dead && drop.field_564 == item).toList();
      check(drops.size() == 1, "cleanup did not run in source world");
      var drop = (class_142) drops.get(0);
      check(Math.abs(drop.x - oldX) < 2 && Math.abs(drop.z - oldZ) < 2, "dropped at destination coordinates");
    }
    check(mc.player.inventory.main[8] == carried, "carried inventory changed");
    assertStack(cursor, 1, 71, cursorTag); assertStack(craft, 13, 0, craftTag);
    assertStack(carried, 1, 83, "travel-carried");
    // Read the source dimension back from disk after travel saved it. Reference checks
    // alone cannot detect a dropped item omitted from the chunk save.
    World reloaded = new World(mc.world, class_50.method_1767(dimension));
    for (Object e : source.field_198) if (e instanceof class_142 drop
        && (drop.field_564 == cursor || drop.field_564 == craft))
      reloaded.method_199((int) Math.floor(drop.x), (int) Math.floor(drop.z));
    for (String name : List.of(cursorTag, craftTag)) {
      var matches = new ArrayList<ItemStack>();
      for (Object e : reloaded.field_198) if (e instanceof class_142 drop
          && tag(drop.field_564).getString("portal-check").equals(name)) matches.add(drop.field_564);
      check(matches.size() == 1, "source chunk save lost/duplicated " + name);
      assertStack(matches.get(0), name.equals(cursorTag) ? 1 : 13, name.equals(cursorTag) ? 71 : 0, name);
    }
    log("PORTAL " + mode + " ticks=" + ticks + " from=" + dimension + " to=" + mc.player.dimensionId);
    mc.player.field_504 = 0;
  }

  private static void enable(boolean value) {
    var s = VisualConfig.copy(); s.inventoryInPortals = value; VisualConfig.preview(s);
  }
  private static void pulse(Minecraft mc) { mc.player.field_511 = 0; mc.player.method_1388(); mc.player.method_937(); }
  private static net.minecraft.inventory.Inventory grid(Minecraft mc) { return ((class_277) mc.player.playerContainer).field_1124; }
  private static NbtCompound tag(ItemStack item) throws Exception {
    return (NbtCompound) item.getClass().getMethod("getStationNbt").invoke(item);
  }
  private static ItemStack tagged(int id, int count, int damage, String name) throws Exception {
    ItemStack item = new ItemStack(id, count, damage); tag(item).putString("portal-check", name); return item;
  }
  private static void assertStack(ItemStack item, int count, int damage, String name) throws Exception {
    check(item.count == count && item.getDamage() == damage && tag(item).getString("portal-check").equals(name), "count/damage/NBT changed: " + name);
  }
  private static long count(Minecraft mc, World source, ItemStack item) {
    long n = mc.player.inventory.getCursorStack() == item ? 1 : 0;
    for (ItemStack stack : mc.player.inventory.main) if (stack == item) n++;
    for (Object e : source.field_198) if (e instanceof class_142 drop && !drop.dead && drop.field_564 == item) n++;
    if (source != mc.world) for (Object e : mc.world.field_198) if (e instanceof class_142 drop && !drop.dead && drop.field_564 == item) n++;
    return n;
  }
}
