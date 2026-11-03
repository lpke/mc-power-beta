package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.gameplay.GameplayConfig;
import local.luke.power.input.*;
import local.luke.power.inventory.tools.*;
import local.luke.power.mining.*;
import local.luke.power.rules.RuleSet;
import local.luke.power.storage.PowerConfig;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.Display;

/** Owns real native cumulative progress. No synthetic break-progress writes or bypassed breaks. */
final class VeinProgress473Checks {
  private VeinProgress473Checks() {}
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive()
        && !MovementOwnership.cameraControlsMovement() && !InteractionState.carryingContainer, "focus a disposable Survival world outside detached/carry operations");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    check(!VeinMining.busy() && !local.luke.power.inventory.NativeInventoryPlan.busy(), "finish existing mining/inventory work first");
    PowerConfig.flushPending(); byte[] preferences = Files.readAllBytes(PowerConfig.path());
    try (var fixture = new Fixture(mc)) {
      fixture.scene(false, false, true);
      var nativeManager = new SingleplayerInteractionManager(mc); mc.interactionManager = nativeManager;
      nativeManager.method_1707(fixture.x, fixture.y, fixture.z, 1);
      nativeManager.method_1721(fixture.x, fixture.y, fixture.z, 1);
      for (int tick = 0; tick < 5; tick++) nativeManager.method_1721(fixture.x, fixture.y, fixture.z, 1);
      float actual = field(SingleplayerInteractionManager.class, "field_2184").getFloat(nativeManager);
      check(actual > 0 && actual < 1, "native accessor fixture did not earn partial progress");
      check(nativeManager instanceof VeinMining.ProgressOwner, "native manager is missing the injected progress bridge");
      var bridge = (VeinMining.ProgressOwner)(Object)nativeManager;
      check(bridge.power$veinProgress(fixture.x, fixture.y, fixture.z) == actual, "injected bridge differs from actual native progress");
      check(bridge.power$veinProgress(fixture.x + 1, fixture.y, fixture.z) == 0
          && bridge.power$veinProgress(fixture.x, fixture.y + 1, fixture.z) == 0
          && bridge.power$veinProgress(fixture.x, fixture.y, fixture.z + 1) == 0,
          "injected bridge attributed progress to a different target");
      nativeManager.method_1705();
      check(field(SingleplayerInteractionManager.class, "field_2184").getFloat(nativeManager) == 0
          && bridge.power$veinProgress(fixture.x, fixture.y, fixture.z) == 0, "native cancel did not clear injected bridge progress");
      fixture.record("injected native bridge partial progress", actual);

      fixture.scene(false, false, true); int normal = fixture.finish(); fixture.single("native timing baseline");
      fixture.record("normal single-block ticks", normal);
      fixture.scene(false, true, true); fixture.charge(.9);
      double ordinary = fixture.manager.progress(); mc.player.field_161.field_2536 = true;
      fixture.finish(); fixture.single("normal 90%, then Shift");
      fixture.record("late Shift", ordinary);

      fixture.scene(true, true, true); int full = fixture.finish(); fixture.whole("Shift from the start");
      check(full > normal && full >= normal * 3, "full vein did not pay its whole-vein timing cost"); fixture.record("full Shift ticks", full);

      fixture.scene(true, true, true); fixture.charge(.45);
      mc.player.field_161.field_2536 = false; fixture.step(); mc.player.field_161.field_2536 = true;
      fixture.finish(); fixture.single("release/reacquire Shift");

      fixture.scene(true, true, true); fixture.charge(.45);
      ItemStack original = mc.player.inventory.main[0];
      mc.player.inventory.main[1] = DeathChecks.tagged(270, 1, 0, "vein-progress-new-tool"); mc.player.inventory.selectedSlot = 1;
      fixture.finish(); fixture.single("held tool changed"); check(original.getDamage() == 0, "old tool was charged after its ownership ended");

      fixture.scene(true, true, true); fixture.charge(.45);
      fixture.settings(false, true); fixture.step(); fixture.settings(true, true);
      fixture.finish(); fixture.single("feature disabled/re-enabled on the same block");

      fixture.scene(true, true, true); fixture.charge(.45);
      mc.paused = true; VeinMining.tick(mc); mc.paused = false;
      fixture.finish(); fixture.single("pause invalidated the plan");

      fixture.scene(true, true, true); fixture.charge(.45);
      VeinMining.reset(); fixture.finish(); fixture.single("lifecycle reset invalidated the plan");

      fixture.scene(true, true, true); fixture.charge(.45);
      fixture.manager.method_1705(); int afterCancel = fixture.finish(); fixture.whole("native cancel and restart");
      check(afterCancel >= full - 4, "cancel reused progress instead of paying a complete new slow break");

      fixture.scene(true, true, true); fixture.charge(.45);
      fixture.target = 1; fixture.manager.method_1721(fixture.x + 1, fixture.y, fixture.z, 1);
      check(fixture.manager.progress() == 0, "native target switch retained another block's progress");
      int afterTarget = fixture.finish(); fixture.whole("native target switch");
      check(afterTarget >= full - 4, "new target skipped its whole-vein timing cost");

      fixture.scene(false, true, false); fixture.finish(); fixture.whole("Crouching only off");

      fixture.scene(false, false, true); fixture.finish(); fixture.single("feature disabled");
      check(fixture.manager.reads == 0, "disabled vein mining read qualification progress");
      check(field(VeinMining.class, "candidate").get(null) == null, "disabled vein mining prepared a search");
      fixture.record("disabled qualification reads", fixture.manager.reads);
      log("VEINPROGRESS473 native progress, Shift/tool/config/lifecycle ownership, target/cancel and disabled guards PASS");
    }
    check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "vein fixture changed saved settings");
  }

  private static Field field(Class<?> type, String name) throws Exception { Field result = type.getDeclaredField(name); result.setAccessible(true); return result; }

  private static final class CountingManager extends SingleplayerInteractionManager implements VeinMining.ProgressOwner {
    private final Field bx, by, bz, amount;
    int reads;
    CountingManager(Minecraft mc) throws Exception {
      super(mc); bx = field(SingleplayerInteractionManager.class, "field_2181"); by = field(SingleplayerInteractionManager.class, "field_2182");
      bz = field(SingleplayerInteractionManager.class, "field_2183"); amount = field(SingleplayerInteractionManager.class, "field_2184");
    }
    @Override public float power$veinProgress(int x, int y, int z) {
      reads++;
      try { return x == bx.getInt(this) && y == by.getInt(this) && z == bz.getInt(this) ? amount.getFloat(this) : 0; }
      catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
    }
    float progress() throws Exception { return amount.getFloat(this); }
  }

  private static final class Fixture implements AutoCloseable {
    final Minecraft mc;
    final int x, y = 112, z;
    final ClientPlayerEntity owner, probe;
    final net.minecraft.entity.LivingEntity view;
    final net.minecraft.client.InteractionManager originalManager;
    final net.minecraft.client.gui.screen.Screen screen;
    final class_27 hit;
    final boolean paused;
    final Field current;
    final Object originalVeins;
    final local.luke.power.gameplay.GameplaySettings gameplay;
    final InventoryToolsSettings tools;
    final RuleSet rules;
    final List<int[]> blocks = new ArrayList<>();
    final Set<Entity> existing = Collections.newSetFromMap(new IdentityHashMap<>());
    final List<Map<String, Object>> report = new ArrayList<>();
    CountingManager manager;
    int target;
    Fixture(Minecraft mc) throws Exception {
      this.mc = mc; owner = mc.player; view = mc.field_2807; originalManager = mc.interactionManager; screen = mc.currentScreen; hit = mc.field_2823; paused = mc.paused;
      current = field(VeinConfig.class, "current"); originalVeins = current.get(null); gameplay = GameplayConfig.copy(); tools = InventoryToolsConfig.copy(); rules = AccessRules.copy(mc);
      x = (int)Math.floor(owner.x) + 5; z = (int)Math.floor(owner.z) + 5;
      check(mc.world.method_155(x - 2, y - 2, z - 2, x + 6, y + 3, z + 4), "load the mining fixture area first");
      for (int bx = x - 1; bx <= x + 4; bx++) for (int by = y - 1; by <= y + 2; by++) for (int bz = z - 1; bz <= z + 3; bz++) {
        check(mc.world.method_1777(bx, by, bz) == null, "mining fixture overlaps a block entity");
        blocks.add(new int[]{bx, by, bz, mc.world.getBlockId(bx, by, bz), mc.world.method_1778(bx, by, bz)});
      }
      for (Object entity : mc.world.method_291()) existing.add((Entity)entity);
      probe = new ClientPlayerEntity(mc, mc.world, mc.session, owner.dimensionId); probe.field_161 = new class_41();
      mc.player = probe; mc.field_2807 = probe; mc.setScreen(null); mc.paused = false; AccessRules.restore(mc, RuleSet.cheats()); CreativeVehicleChecks.mode(mc, "SURVIVAL");
      var disabled = InventoryToolsConfig.copy(); disabled.toolSwitch = disabled.restock = false; InventoryToolsConfig.preview(disabled);
      var ordinary = GameplayConfig.copy(); ordinary.torchShovel = false; GameplayConfig.preview(ordinary);
    }
    void settings(boolean enabled, boolean crouch) throws Exception {
      var settings = new VeinSettings(); settings.enabled = enabled; settings.crouchingOnly = crouch; settings.timePercent = 100;
      settings.maxBlocks = 4; settings.maxChecks = 128; settings.checksPerTick = 128; settings.blocksPerTick = 1; settings.radius = 4;
      VeinMining.reset(); current.set(null, settings);
    }
    void scene(boolean crouch, boolean enabled, boolean crouchingOnly) throws Exception {
      retireDrops(); settings(enabled, crouchingOnly); target = 0; manager = new CountingManager(mc); mc.interactionManager = manager;
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], 0, 0);
      for (int n = 0; n < 4; n++) { mc.world.method_154(x + n, y - 1, z, 1, 0); mc.world.method_154(x + n, y, z, 16, 0); }
      probe.method_1341(x + .5, y, z + 2.5, 0, 0); probe.field_1623 = true; probe.field_161.field_2536 = crouch;
      probe.inventory.main = new ItemStack[36]; probe.inventory.main[0] = DeathChecks.tagged(270, 1, 0, "vein-progress-original-tool"); probe.inventory.selectedSlot = 0;
      mc.field_2823 = new class_27(x, y, z, 1, Vec3d.createCached(x + .5, y + 1, z + .5));
      manager.method_1707(x, y, z, 1); manager.method_1721(x, y, z, 1);
    }
    void step() { VeinMining.tick(mc); manager.method_1721(x + target, y, z, 1); }
    void charge(double amount) throws Exception {
      int ticks = 0;
      while (manager.progress() < amount && mc.world.getBlockId(x + target, y, z) != 0 && ticks++ < 2000) step();
      check(ticks < 2000 && manager.progress() >= amount && mc.world.getBlockId(x + target, y, z) == 16, "could not accumulate real native partial progress");
    }
    int finish() {
      int ticks = 0;
      while (mc.world.getBlockId(x + target, y, z) != 0 && ticks++ < 2000) step();
      check(ticks < 2000, "native first block did not finish");
      for (int n = 0; n < 256 && VeinMining.busy(); n++) VeinMining.tick(mc);
      check(!VeinMining.busy(), "native vein did not finish"); return ticks;
    }
    void single(String name) {
      check(mc.world.getBlockId(x, y, z) == 0, name + " did not retain the native first break");
      for (int n = 1; n < 4; n++) check(mc.world.getBlockId(x + n, y, z) == 16, name + " claimed normal progress for a vein");
      check(probe.inventory.getSelectedItem().getDamage() == 1 && drops() == 1, name + " changed native wear/drops");
    }
    void whole(String name) {
      for (int n = 0; n < 4; n++) check(mc.world.getBlockId(x + n, y, z) == 0, name + " left ore in the qualified vein");
      check(probe.inventory.getSelectedItem().getDamage() == 4 && drops() == 4, name + " changed native wear/drops");
    }
    int drops() {
      int count = 0;
      for (Object entity : mc.world.method_291()) if (entity instanceof class_142 item && !item.dead && !existing.contains(item)) {
        check(item.field_564.itemId == 263, "native coal drop identity changed"); count += item.field_564.count;
      }
      return count;
    }
    void record(String name, double value) { report.add(Map.of("case", name, "value", value)); }
    void retireDrops() { for (Object entity : mc.world.method_291()) if (entity instanceof class_142 item && !existing.contains(item)) item.markDead(); }
    @Override public void close() throws Exception {
      VeinMining.reset(); current.set(null, originalVeins); GameplayConfig.preview(gameplay); InventoryToolsConfig.preview(tools); retireDrops();
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[3], b[4]);
      mc.player = owner; mc.field_2807 = view; mc.interactionManager = originalManager; mc.field_2823 = hit; mc.paused = paused; AccessRules.restore(mc, rules); mc.setScreen(screen);
      Path path = Path.of("power-beta-data/reports/vein-progress-473"); Files.createDirectories(path); Files.writeString(path.resolve("summary.json"), Catalog.JSON.toJson(report));
    }
  }
}
