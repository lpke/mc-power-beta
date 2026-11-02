package local.luke.power.bow;

import local.luke.power.gameplay.GameplayConfig;
import local.luke.power.input.*;
import local.luke.power.inventory.*;
import local.luke.power.offhand.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** A held native use owns one exact hand stack until release or cancellation. */
public final class ModernBow {
  private static PlayerEntity owner;
  private static ItemStack bow;
  private static HandUse.Hand hand;
  private static int slot, ticks;
  private static long swap;
  private static final BowFov fov = new BowFov();
  static {
    TweakLifecycle.register("modern_bow", () -> { cancel(); fov.reset(); });
    BowView.register(() -> fov.active() && enabled() && GameplayConfig.current().modernBowFov,
        delta -> fov.factor((float)delta, SprintView.fov((float)delta)));
  }
  private ModernBow() {}
  public static boolean enabled() { return GameplayConfig.current().modernBowHandling; }
  private static Minecraft client() { return (Minecraft)FabricLoader.getInstance().getGameInstance(); }
  public static boolean creative(PlayerEntity player) { return player instanceof PlayerMode mode && mode.power$creative(); }
  private static boolean playable(Minecraft mc) {
    return mc != null && mc.player != null && mc.world != null && mc.player.world == mc.world && !mc.world.isRemote
        && local.luke.power.world.WorldCompatibility.ready(mc.world)
        && mc.player.health > 0 && !mc.player.dead && mc.field_2807 == mc.player && mc.interactionManager != null
        && !(mc.player instanceof PlayerMode mode && mode.power$spectator()) && !DetachedCamera.isActive()
        && !InteractionState.carryingContainer && mc.currentScreen == null && mc.field_2778
        && mc.player.container == mc.player.playerContainer && mc.player.inventory.getCursorStack() == null;
  }
  public static boolean using(PlayerEntity player, ItemStack stack) {
    return enabled() && owner == player && bow == stack && stack != null;
  }
  public static int ticks(PlayerEntity player, ItemStack stack) { return using(player, stack) ? ticks : 0; }
  public static HandUse.Hand hand(PlayerEntity player) { return enabled() && owner == player ? hand : null; }
  /** A live local draw owns Attack as well as Use, independently of the drawing hand. */
  public static boolean ownsAttack(Minecraft mc) {
    return owner != null && enabled() && mc != null && mc.player == owner
        && mc.world != null && mc.world == owner.world && !mc.world.isRemote;
  }
  /** Native Camera.tick runs after input, once per unpaused gameplay tick. */
  public static void tickFov(Minecraft mc) {
    if (!enabled() || !GameplayConfig.current().modernBowFov) { if (fov.active()) fov.reset(); return; }
    if (mc.world == null || mc.player == null) { if (fov.active()) fov.reset(); return; }
    if (mc.paused) return;
    int elapsed = owner == mc.player ? Math.max(0, ticks - 1) : -1;
    if (elapsed <= 0 && !fov.active()) return;
    fov.tick(elapsed, local.luke.power.sprint.SprintController.targetFov(), SprintView.fov(0), SprintView.fov(1));
  }
  /** Called only after the native block/entity use path passes to this bow. */
  public static void begin(PlayerEntity player, ItemStack stack) {
    Minecraft mc = client();
    if (!playable(mc) || mc.player != player || !Display.isActive()) return;
    if (owner != null || NativeInventoryPlan.busy()) return;
    if (!creative(player) && ammo(player) == null) return;
    owner = player; bow = stack; hand = HandUse.hand(player.inventory); slot = player.inventory.selectedSlot;
    ticks = 1; swap = HandUse.manualRevision();
  }
  public static void cancel() { owner = null; bow = null; hand = null; ticks = 0; }
  public static void tick(Minecraft mc) {
    // With the preference off, do not poll input, inspect inventories or prepare a draw.
    if (!enabled()) { if (owner != null) cancel(); return; }
    if (owner == null) return;
    if (!playable(mc) || mc.player != owner || !Display.isActive() || owner.inventory.selectedSlot != slot
        || HandUse.manualRevision() != swap || hand == HandUse.Hand.OFF && !Offhand.enabled(owner)
        || Offhand.stack(owner, hand) != bow || bow.count <= 0) { cancel(); return; }
    if (UseInput.held() || Mouse.isCreated() && Mouse.isButtonDown(1)) {
      if (++ticks >= 72000) release(mc);
    } else release(mc);
  }
  private static ItemStack ammo(PlayerEntity player) {
    // Official projectile lookup prefers the offhand, then main hand, then storage.
    if (Offhand.enabled(player)) {
      ItemStack off = Offhand.get(player.inventory);
      if (arrow(off)) return off;
    }
    ItemStack main = player.inventory.main[player.inventory.selectedSlot];
    if (arrow(main)) return main;
    for (ItemStack stack : player.inventory.main) if (arrow(stack)) return stack;
    return null;
  }
  private static boolean arrow(ItemStack stack) { return stack != null && stack.itemId == Item.field_475.id && stack.count > 0; }
  /** Native release, also used by the muted disposable fixture after advancing draw ticks. */
  public static void release(Minecraft mc) {
    if (!enabled() || owner == null) { cancel(); return; }
    PlayerEntity player = owner; ItemStack weapon = bow; HandUse.Hand used = hand; int heldTicks = ticks;
    cancel();
    if (!playable(mc) || mc.player != player || Offhand.stack(player, used) != weapon) return;
    float power = BowPhysics.power(heldTicks); ItemStack ammunition = ammo(player); boolean creative = creative(player);
    if (power < .1f || !creative && ammunition == null) return;
    final Object world = mc.world;
    final int selected = player.inventory.selectedSlot;
    final long revision = HandUse.manualRevision();
    // Beta stores an eye-based native position. Its render eye also accounts for
    // the selected crouch camera and pose easing, independently of physical height.
    float cameraOffset = local.luke.power.gameplay.ModernSwimming.cameraOffset(player, 1, player.field_1631) - 1.62f;
    final double x = player.x, y = player.y - cameraOffset - (double).1f, z = player.z;
    final float yaw = player.yaw, pitch = player.pitch;
    final double vx = player.velocityX, vy = player.field_1623 ? 0 : player.velocityY, vz = player.velocityZ;
    final boolean breaks = !creative && weapon.getDamage() + 1 >= BowPhysics.DURABILITY;
    class_410[] fired = {null};
    Runnable spawn = () -> {
      var arrow = new class_410(mc.world);fired[0] = arrow;
      arrow.field_1576 = player; arrow.field_1574 = !creative;
      ((ModernArrowData)(Object)arrow).power$modernArrow(power == 1, creative);
      arrow.method_1340(x, y, z);
      float yawRadians = yaw * ((float)Math.PI / 180), pitchRadians = pitch * ((float)Math.PI / 180);
      double dx = -Math.sin(yawRadians) * Math.cos(pitchRadians), dy = -Math.sin(pitchRadians), dz = Math.cos(yawRadians) * Math.cos(pitchRadians);
      double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
      var random = java.util.concurrent.ThreadLocalRandom.current();
      arrow.velocityX = (dx / length + BowPhysics.triangle(random.nextDouble(), random.nextDouble())) * power * 3 + vx;
      arrow.velocityY = (dy / length + BowPhysics.triangle(random.nextDouble(), random.nextDouble())) * power * 3 + vy;
      arrow.velocityZ = (dz / length + BowPhysics.triangle(random.nextDouble(), random.nextDouble())) * power * 3 + vz;
      arrow.yaw = arrow.prevYaw = (float)Math.toDegrees(Math.atan2(arrow.velocityX, arrow.velocityZ));
      arrow.pitch = arrow.prevPitch = (float)Math.toDegrees(Math.atan2(arrow.velocityY, Math.hypot(arrow.velocityX, arrow.velocityZ)));
      if (!mc.world.method_210(arrow)) throw new IllegalStateException("The world rejected the released arrow");
      if (breaks) {
        local.luke.power.audio.SoundCategoryScope.run("players", () -> mc.world.method_150(x,y,z,"power_beta:entity.item.break",.8f,.8f+random.nextFloat()*.4f));
        if (used == HandUse.Hand.OFF) OffhandRenderer.changed();
      }
      local.luke.power.audio.SoundCategoryScope.run("players", () -> mc.world.method_150(x,y,z,"power_beta:entity.arrow.shoot",1,BowPhysics.shotPitch(random.nextFloat(),power)));
    };
    java.util.function.BooleanSupplier valid = () -> enabled() && playable(mc) && mc.player == player && mc.world == world
        && Display.isActive() && player.inventory.selectedSlot == selected && HandUse.manualRevision() == revision
        && creative(player) == creative && (used != HandUse.Hand.OFF || Offhand.enabled(player));
    if (creative) {
      // Creative releases have no inventory writes. Retain the native use lease around the world callback.
      var lease = InventoryActions.begin(mc.world,player,player.container,player.inventory,"release_bow",true);
      if (lease == null) return;
      try { if (valid.getAsBoolean() && lease.valid(mc.world,player,player.container,player.inventory)) spawn.run(); }
      finally { InventoryActions.finish(lease); }
    } else NativeInventoryPlan.submitBowShot(mc,used,weapon,ammunition,valid,spawn,() -> { if (fired[0] != null) fired[0].markDead(); });
  }
}
