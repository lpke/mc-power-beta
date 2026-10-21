package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import local.luke.power.death.DeathItems;
import local.luke.power.gameplay.*;
import local.luke.power.input.*;
import local.luke.power.inventory.*;
import local.luke.power.inventory.tools.*;
import local.luke.power.mining.*;
import local.luke.power.offhand.*;
import local.luke.power.storage.JournalQueue;
import net.minecraft.SingleplayerInteractionManager;
import net.minecraft.class_27;
import net.minecraft.class_142;
import net.minecraft.client.Minecraft;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Mouse;

/** Owns temporary native state and a nine-cell region which must already be loaded air. */
final class AutoTools400State implements AutoCloseable {
  final Minecraft mc;
  final PlayerInventory inventory;
  final AtomicBoolean attack = new AtomicBoolean(true), detached = new AtomicBoolean();
  final int x, y, z;
  private final ItemStack[] main, armor;
  private final ItemStack cursor, off;
  private final int selected, health;
  private final int fire, air;
  private final double velocityX, velocityY, velocityZ;
  private final float fallDistance;
  private final float yaw, pitch, prevYaw, prevPitch;
  private final Field fallField;
  private final boolean dead, grounded, focus, paused, grabbed;
  private final Screen screen;
  private final Container handler;
  private final LivingEntity view;
  private final InteractionManager manager;
  private final class_27 hit;
  private final BooleanSupplier oldAttack, oldDetached;
  private final String mode;
  private final String originalInventory;
  private final InventoryToolsSettings tools;
  private final GameplaySettings gameplay;
  private final OffhandSettings offhand;
  private final VeinSettings veins;
  private final Field veinCurrent;
  private final Field creativeCurrent;
  private final Object creativeSettings;
  private final Class<?> creativePlayer;
  private final boolean flying;
  private final Map<Field, Object> modeState = new LinkedHashMap<>();
  private final Set<Entity> existing = Collections.newSetFromMap(new IdentityHashMap<>());

  AutoTools400State(Minecraft mc) throws Exception {
    this.mc = mc; inventory = mc.player.inventory;
    main = inventory.main; armor = inventory.armor; cursor = inventory.getCursorStack(); off = Offhand.get(inventory);
    selected = inventory.selectedSlot; health = mc.player.health; dead = mc.player.dead;
    fire = mc.player.fire; air = mc.player.air;
    fallField = field(Entity.class, "field_1636"); fallDistance = fallField.getFloat(mc.player);
    yaw = mc.player.yaw; pitch = mc.player.pitch; prevYaw = mc.player.prevYaw; prevPitch = mc.player.prevPitch;
    velocityX = mc.player.velocityX; velocityY = mc.player.velocityY; velocityZ = mc.player.velocityZ;
    grounded = mc.player.field_1623; focus = mc.field_2778; paused = mc.paused; grabbed = Mouse.isGrabbed();
    screen = mc.currentScreen; handler = mc.player.container; view = mc.field_2807;
    manager = mc.interactionManager; hit = mc.field_2823;
    originalInventory = DeathItems.inventory(DeathItems.snapshot(inventory, mc.player.playerContainer));
    mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    creativePlayer = Class.forName("local.luke.power.creative.inventory.interfaces.CreativePlayer");
    flying = (Boolean)creativePlayer.getMethod("creative_isFlying").invoke(mc.player);
    for (String name : new String[]{"power$previous", "power$flightOverride", "power$spectatorSpeed", "power$restoringFlight"}) {
      Field field = playerField(mc, name); modeState.put(field, field.get(mc.player));
    }
    creativeCurrent = field(Class.forName("local.luke.power.creative.config.Config"), "settings");
    creativeSettings = creativeCurrent.get(null);
    oldAttack = (BooleanSupplier)field(AttackInput.class, "held").get(null);
    oldDetached = (BooleanSupplier)field(DetachedCamera.class, "active").get(null);
    veinCurrent = field(VeinConfig.class, "current"); veins = VeinConfig.current();
    tools = InventoryToolsConfig.copy(); gameplay = GameplayConfig.copy(); offhand = OffhandConfig.copy();
    for (Object entity : mc.world.field_198) existing.add((Entity)entity);
    int[] cells = airRegion(mc); x = cells[0]; y = cells[1] + 1; z = cells[2];
    log("AUTOTOOLS400 owned air x=" + x + ".." + (x + 2) + " y=" + (y - 1) + ".." + (y + 1) + " z=" + z);
  }

  void reset() throws Exception {
    cancel();
    attack.set(true); detached.set(false);
    AttackInput.register(attack::get); DetachedCamera.register(detached::get);
    // Entering Creative normally enables and queues its saved flight preference. Keep that
    // prerequisite in a temporary config object so mode exclusion tests never queue a save.
    Object creative = creativeSettings.getClass().getMethod("copy").invoke(creativeSettings);
    creative.getClass().getField("flight").setBoolean(creative, true); creativeCurrent.set(null, creative);
    mc.currentScreen = null; mc.player.container = mc.player.playerContainer; mc.field_2807 = mc.player;
    mc.field_2778 = true; mc.paused = false; mc.player.health = 20; mc.player.dead = false;
    CreativeVehicleChecks.mode(mc, "SURVIVAL"); mc.interactionManager = new SingleplayerInteractionManager(mc);
    mc.player.field_1623 = true; Mouse.setGrabbed(true);
    mc.player.yaw = yaw; mc.player.pitch = pitch; mc.player.prevYaw = prevYaw; mc.player.prevPitch = prevPitch;
    inventory.main = new ItemStack[36]; inventory.armor = new ItemStack[4];
    inventory.setCursorStack(null); inventory.selectedSlot = 0; Offhand.set(inventory, null);
    var enabled = new OffhandSettings(); enabled.enabled = true; OffhandConfig.preview(enabled);
    var settings = new InventoryToolsSettings(); settings.toolSwitch = settings.restock = true;
    settings.restockThreshold = 1; InventoryToolsConfig.preview(settings);
    var game = GameplayConfig.copy(); game.torchShovel = false; GameplayConfig.preview(game);
    veins(new VeinSettings()); clearBlocks(); aim(0);
    check(InventoryAutomation.survival(mc), "fixture did not establish actual survival eligibility");
  }

  void cancel() throws Exception {
    NativeInventoryPlan.cancel(); JournalQueue.awaitQueued(); NativeInventoryPlan.tick(mc);
    check(!NativeInventoryPlan.busy(), "fixture left a pending inventory plan");
    InventoryAutomation.clear(); VeinMining.reset();
  }

  void veins(VeinSettings settings) throws Exception {
    settings.validate(); VeinMining.reset(); veinCurrent.set(null, settings);
  }

  void scene(int id, int count) {
    VeinMining.reset(); clearBlocks();
    for (int i = 0; i < count; i++) {
      mc.world.method_200(x + i, y - 1, z, 1);
      mc.world.method_200(x + i, y, z, id);
      check(mc.world.getBlockId(x + i, y, z) == id, "fixture block did not install");
    }
    aim(0);
  }

  void aim(int offset) {
    mc.field_2823 = new class_27(x + offset, y, z, 1, Vec3d.createCached(x + offset + .5, y + 1, z + .5));
  }
  void lookAtTarget() {
    double eyeY = mc.player.y + 1.62 - mc.player.field_1631;
    double dx = x + .5 - mc.player.x, dy = y + .5 - eyeY, dz = z + .5 - mc.player.z;
    mc.player.yaw = mc.player.prevYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
    mc.player.pitch = mc.player.prevPitch = (float)-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
    var ray = mc.world.method_161(Vec3d.createCached(mc.player.x, eyeY, mc.player.z), Vec3d.createCached(x + .5, y + .5, z + .5), false);
    check(ray != null && ray.field_1984 == x && ray.field_1985 == y && ray.field_1986 == z,
        "native bucket ray does not reach the owned target");
  }

  void begin() { mc.interactionManager.method_1707(x, y, z, 1); }
  boolean breakBlock() { return mc.interactionManager.method_1716(x, y, z, 1); }
  void finish() throws IOException {
    JournalQueue.awaitQueued(); NativeInventoryPlan.tick(mc);
    check(!NativeInventoryPlan.busy(), "durable plan did not finish");
  }

  boolean use(HandUse.Hand hand) {
    if (hand == HandUse.Hand.MAIN) {
      try (HandUse ignored = HandUse.begin(inventory, hand, false)) {
        return mc.interactionManager.method_1712(mc.player, mc.world, inventory.getSelectedItem());
      }
    }
    // Beta's InteractionManager writes main[] directly. Production offers OFF through its own
    // native item callback and replacement normalization after the non-consuming MAIN pass.
    ItemStack original = Offhand.get(inventory); int count = original == null ? 0 : original.count;
    class_27 target = mc.field_2823; mc.field_2823 = null;
    OffhandUse.newPress(mc);
    try (OffhandUse use = OffhandUse.begin(mc, 1)) {
      check(use != null, "native hand-use admission failed"); OffhandUse.nativePath();
      ItemStack main = inventory.getSelectedItem();
      if (main != null) OffhandUse.air(mc, main, () -> mc.interactionManager.method_1712(mc.player, mc.world, main));
      use.offerOffhand();
      return Offhand.get(inventory) != original || original != null && original.count != count;
    } finally {
      mc.field_2823 = target;
    }
  }

  void place(HandUse.Hand hand) {
    ItemStack stack = Offhand.stack(mc.player, hand);
    try (HandUse ignored = HandUse.begin(inventory, hand, hand == HandUse.Hand.OFF)) {
      check(mc.interactionManager.method_1713(mc.player, mc.world, stack, x, y, z, 1), "native placement failed");
    }
    check(mc.world.getBlockId(x, y + 1, z) == 4, "native placement did not reach the owned air cell");
  }

  String snapshot() throws IOException { return DeathItems.inventory(DeathItems.snapshot(inventory, mc.player.playerContainer)); }
  static String bytes(ItemStack item) throws IOException { return item == null ? null : DeathItems.encode(item); }
  static ItemStack tagged(int id, int count, int damage, String label) throws Exception {
    return DeathChecks.tagged(id, count, damage, "auto-tools-400:" + label);
  }

  private void clearBlocks() {
    for (int dx = 0; dx < 3; dx++) for (int dy = -1; dy <= 1; dy++) mc.world.method_200(x + dx, y + dy, z, 0);
  }

  @Override public void close() throws Exception {
    try { cancel(); }
    finally {
      try {
        clearBlocks();
        // Only item entities created by these synchronous native callbacks belong to this fixture.
        for (Object value : new ArrayList<>(mc.world.field_198)) if (value instanceof class_142 item && !existing.contains(item)) item.markDead();
      }
      finally {
        inventory.main = main; inventory.armor = armor; inventory.selectedSlot = selected;
        inventory.setCursorStack(cursor); Offhand.set(inventory, off);
        InventoryToolsConfig.preview(tools); GameplayConfig.preview(gameplay); OffhandConfig.preview(offhand);
        VeinMining.reset(); veinCurrent.set(null, veins);
        AttackInput.register(oldAttack); DetachedCamera.register(oldDetached);
        try {
          CreativeVehicleChecks.mode(mc, mode);
          for (var entry : modeState.entrySet()) entry.getKey().set(mc.player, entry.getValue());
          playerField(mc, "power$restoringFlight").setBoolean(mc.player, true);
          creativePlayer.getMethod("creative_setFlying", boolean.class).invoke(mc.player, flying);
        }
        finally {
          creativeCurrent.set(null, creativeSettings);
          for (var entry : modeState.entrySet()) entry.getKey().set(mc.player, entry.getValue());
          mc.player.health = health; mc.player.dead = dead; mc.player.field_1623 = grounded;
          mc.player.fire = fire; mc.player.air = air; fallField.setFloat(mc.player, fallDistance);
          mc.player.velocityX = velocityX; mc.player.velocityY = velocityY; mc.player.velocityZ = velocityZ;
          mc.player.yaw = yaw; mc.player.pitch = pitch; mc.player.prevYaw = prevYaw; mc.player.prevPitch = prevPitch;
          mc.interactionManager = manager; mc.currentScreen = screen; mc.player.container = handler;
          mc.field_2807 = view; mc.field_2823 = hit; mc.field_2778 = focus; mc.paused = paused; Mouse.setGrabbed(grabbed);
        }
      }
      for (int dx = 0; dx < 3; dx++) for (int dy = -1; dy <= 1; dy++)
        check(mc.world.getBlockId(x + dx, y + dy, z) == 0, "owned fixture cell was not restored to air");
      check(inventory.main == main && inventory.armor == armor && originalInventory.equals(snapshot())
          && field(AttackInput.class, "held").get(null) == oldAttack && field(DetachedCamera.class, "active").get(null) == oldDetached,
          "fixture native data or exact input/listener provider was not restored");
    }
  }

  private static int[] airRegion(Minecraft mc) {
    int px = (int)Math.floor(mc.player.x), py = (int)Math.floor(mc.player.boundingBox.minY), pz = (int)Math.floor(mc.player.z);
    for (int dy = 0; dy <= 2; dy++) for (int dz = -3; dz <= 3; dz++) for (int dx = -3; dx <= 1; dx++) {
      int x = px + dx, y = py + dy, z = pz + dz;
      if (y < 1 || y + 2 > 126 || Math.abs(dz) <= 1 && dx >= -2 && dx <= 0) continue;
      double distance = square(mc.player.x - x - .5) + square(mc.player.y - y - 1.5) + square(mc.player.z - z - .5);
      if (distance > 16) continue;
      boolean air = true;
      // The untouched air margin also prevents the bounded two-block vein from reaching terrain.
      for (int a = -1; a <= 3; a++) for (int b = 0; b < 3; b++) for (int c = -1; c <= 1; c++)
        if (!mc.world.method_239(x + a, y + b, z + c) || mc.world.getBlockId(x + a, y + b, z + c) != 0
            || mc.world.method_1777(x + a, y + b, z + c) != null) air = false;
      if (air) return new int[]{x, y, z};
    }
    throw new AssertionError("need nine loaded air cells near the player; no existing terrain was changed");
  }
  private static double square(double value) { return value * value; }
  private static Field field(Class<?> owner, String name) throws Exception {
    Field field = owner.getDeclaredField(name); field.setAccessible(true); return field;
  }
  private static Field playerField(Minecraft mc, String name) throws Exception {
    for (Class<?> type = mc.player.getClass(); type != null; type = type.getSuperclass()) {
      try { return field(type, name); } catch (NoSuchFieldException absent) { }
    }
    throw new NoSuchFieldException(name);
  }

  static final class SlowWriter implements AutoCloseable {
    private final CountDownLatch release = new CountDownLatch(1), exited = new CountDownLatch(1);
    SlowWriter() throws Exception {
      CountDownLatch entered = new CountDownLatch(1);
      ((ThreadPoolExecutor)field(JournalQueue.class, "IO").get(null)).execute(() -> {
        entered.countDown();
        try { release.await(); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        finally { exited.countDown(); }
      });
      if (!entered.await(2, TimeUnit.SECONDS)) { release(); throw new AssertionError("journal blocker was not installed"); }
    }
    void checkBlocked() { check(release.getCount() == 1 && exited.getCount() == 1, "journal worker escaped before explicit release"); }
    void release() { release.countDown(); }
    @Override public void close() { release(); }
  }
}
