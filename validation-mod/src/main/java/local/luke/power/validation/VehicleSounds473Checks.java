package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.input.PlayerMode;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.sprint.*;
import net.minecraft.SingleplayerInteractionManager;
import net.minecraft.class_27;
import local.luke.power.validation.mixin.Client253Access;
import net.minecraft.class_113;
import net.minecraft.class_142;
import net.minecraft.class_549;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;

/** Muted native Creative removal and Survival damage across every Beta vehicle type. */
public final class VehicleSounds473Checks {
  private record Sound(String id, float volume, float pitch) {}
  private static final List<Sound> SOUNDS = new ArrayList<>();
  private static boolean recording;
  public static void capture(String id, float volume, float pitch) {
    if (recording && id.startsWith("power_beta:entity.player.attack.")) SOUNDS.add(new Sound(id, volume, pitch));
  }
  private static Field field(Class<?> type, String name) throws Exception {
    for (Class<?> owner = type; owner != null; owner = owner.getSuperclass())
      try { var f = owner.getDeclaredField(name); f.setAccessible(true); return f; }
      catch (NoSuchFieldException next) {}
    throw new NoSuchFieldException(name);
  }
  private static void charge(Minecraft mc, int ticks) throws Exception {
    field(mc.player.getClass(), "power$vehicleSoundCharge").setInt(mc.player, ticks);
    var held = mc.player.method_502();
    field(mc.player.getClass(), "power$vehicleSoundItem").setInt(mc.player, held == null ? 0 : held.itemId);
  }
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && mc.currentScreen == null,
        "open a muted disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable clone");
    SilentAudioChecks.ready(mc); failures = 0;
    var player = mc.player; var saved = new NbtCompound(); player.write(saved);
    var inventory = player.inventory.main.clone(); int slot = player.inventory.selectedSlot;
    var audio = AudioConfig.copy(); var rules = AccessRules.copy(mc); var sprint = SprintConfig.copy();
    SprintState sprintState = ((SprintPlayer)player).power$sprint();
    var sprintFields = new LinkedHashMap<Field, Object>();
    for (Field value : SprintState.class.getDeclaredFields()) { value.setAccessible(true); sprintFields.put(value, value.get(sprintState)); }
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(player).toString();
    byte[] disk = Files.readAllBytes(PowerConfig.path());
    var created = new ArrayList<Entity>();
    Box area = Box.getOrCreate(player.x - 8, player.y - 8, player.z - 8, player.x + 8, player.y + 8, player.z + 8);
    var priorDrops = new HashSet<>(mc.world.method_175(class_142.class, area));
    var priorHit = mc.field_2823; var priorInteraction = mc.interactionManager;
    int priorCooldown = field(mc.getClass(), "field_2787").getInt(mc);
    boolean priorSwing = player.handSwinging; int priorSwingTicks = player.handSwingTicks;
    int priorCharge = field(player.getClass(), "power$vehicleSoundCharge").getInt(player);
    int priorItem = field(player.getClass(), "power$vehicleSoundItem").getInt(player);
    try {
      AccessRules.restore(mc, RuleSet.cheats()); player.inventory.selectedSlot = 0; player.inventory.main[0] = null;
      var enabled = AudioConfig.copy(); enabled.vehicleAttackSounds = true; AudioConfig.preview(enabled);
      for (int type = -1; type < 3; type++) {
        final int kind = type;
        test("AUDIO473 creative one native deletion emits exactly one thunk for vehicle " + type, () -> {
          CreativeVehicleChecks.mode(mc, "CREATIVE"); charge(mc, 25);
          Entity vehicle = vehicle(mc, kind); created.add(vehicle);
          if (vehicle instanceof class_549 cart && kind == 1) cart.setStack(0, new ItemStack(264, 64, 0));
          int drops = mc.world.method_175(class_142.class, area).size();
          SOUNDS.clear(); recording = true; mc.interactionManager.attackEntity(player, vehicle);
          check(vehicle.dead && SOUNDS.size() == 1 && SOUNDS.get(0).id.equals("power_beta:entity.player.attack.strong"), "missing or duplicate Creative thunk " + SOUNDS);
          check(mc.world.method_175(class_142.class, area).size() == drops, "Creative contents/drops changed");
          charge(mc, 25); mc.interactionManager.attackEntity(player, vehicle);
          check(SOUNDS.size() == 1 && ((VehicleAttackSoundPlayer)player).power$vehicleSoundCharge() == 0, "dead vehicle repeated a thunk or retained charge");
        });
        test("AUDIO473 survival successful hits retain native damage and no added final break cue for vehicle " + type, () -> {
          CreativeVehicleChecks.mode(mc, "SURVIVAL"); charge(mc, 25);
          Entity vehicle = vehicle(mc, kind); created.add(vehicle);
          SOUNDS.clear(); recording = true; mc.interactionManager.attackEntity(player, vehicle);
          check(!vehicle.dead && SOUNDS.size() == 1 && SOUNDS.get(0).id.equals("power_beta:entity.player.attack.strong"), "charged Survival hit changed " + SOUNDS);
          mc.interactionManager.attackEntity(player, vehicle);
          check(SOUNDS.size() == 2 && SOUNDS.get(1).id.equals("power_beta:entity.player.attack.weak"), "rapid Survival hit lacks weak cue " + SOUNDS);
          int hits = 2;
          while (!vehicle.dead && hits < 20) { mc.interactionManager.attackEntity(player, vehicle); hits++; }
          check(vehicle.dead && SOUNDS.size() == hits, "final hit lost or added a separate break cue " + SOUNDS);
          for (Sound sound : SOUNDS) check(sound.volume == 1 && sound.pitch == 1, "native event gain/pitch changed");
        });
        test("AUDIO473 disabled vehicle sounds preserve native removal for vehicle " + type, () -> {
          var off = AudioConfig.copy(); off.vehicleAttackSounds = false; AudioConfig.preview(off);
          CreativeVehicleChecks.mode(mc, "CREATIVE"); Entity vehicle = vehicle(mc, kind); created.add(vehicle);
          SOUNDS.clear(); recording = true; mc.interactionManager.attackEntity(player, vehicle);
          check(vehicle.dead && SOUNDS.isEmpty(), "disabled sound changed removal or emitted audio");
          AudioConfig.preview(enabled);
        });
      }
      test("AUDIO473 rejected native vehicle damage emits no success cue and resets attack charge", () -> {
        CreativeVehicleChecks.mode(mc, "SURVIVAL"); player.inventory.main[0] = null; charge(mc, 25);
        var rejected = new RejectingBoat(mc); created.add(rejected); mc.world.method_210(rejected);
        SOUNDS.clear(); recording = true; mc.interactionManager.attackEntity(player, rejected);
        check(rejected.attempts == 1 && !rejected.dead && SOUNDS.isEmpty(), "rejected damage emitted success audio or changed native result " + SOUNDS);
        check(((VehicleAttackSoundPlayer)player).power$vehicleSoundCharge() == 0, "rejected attack retained charge");
      });
      test("AUDIO473 full sprint hit emits knockback then strong; rapid sprint hit emits only weak", () -> {
        CreativeVehicleChecks.mode(mc, "SURVIVAL"); player.inventory.main[0] = null;
        AccessRules.set(mc, GameRule.ALLOW_SPRINTING, true);
        var settings = SprintConfig.copy(); settings.stamina = settings.noSprintInCombat = false; SprintConfig.preview(settings);
        sprintState.land(settings, true, 1, false, false, false);
        check(SprintController.active(player), "fixture did not activate native sprint state");
        Entity vehicle = vehicle(mc, -1); created.add(vehicle); charge(mc, 25);
        SOUNDS.clear(); recording = true; mc.interactionManager.attackEntity(player, vehicle);
        check(SOUNDS.stream().map(Sound::id).toList().equals(List.of("power_beta:entity.player.attack.knockback", "power_beta:entity.player.attack.strong")), "sprint cue order differs from modern " + SOUNDS);
        for (Sound sound : SOUNDS) check(sound.volume == 1 && sound.pitch == 1, "sprint cue input gain/pitch changed");
        SOUNDS.clear(); mc.interactionManager.attackEntity(player, vehicle);
        check(SOUNDS.size() == 1 && SOUNDS.get(0).id.equals("power_beta:entity.player.attack.weak"), "rapid sprint hit retained full-strength cues " + SOUNDS);
        sprintState.cancel(false); SprintConfig.preview(sprint);
      });
      test("AUDIO473 native displacement selects sword sweep at the exact modern speed boundary", () -> {
        CreativeVehicleChecks.mode(mc, "SURVIVAL"); player.inventory.main[0] = new ItemStack(267, 1, 0);
        sprintState.cancel(false); player.field_1623 = true;
        double threshold = (double).1f * 2.5;
        // Integral positions within the loaded fixture area retain the float-derived boundary exactly.
        double px = player.x, pz = player.z, py = player.y;
        double baseX = Math.floor(px), baseZ = Math.floor(pz);
        double oldPrevX = player.prevX, oldPrevZ = player.prevZ;
        double oldVx = player.velocityX, oldVz = player.velocityZ;
        try {
          for (double distance : new double[]{.25, threshold, threshold + .0001}) {
            player.method_1340(baseX, py, baseZ); player.prevX = baseX - distance; player.prevZ = baseZ;
            check(player.x - player.prevX == distance || distance > threshold, "fixture lost exact threshold displacement");
            player.velocityX = player.velocityZ = 0; charge(mc, 25);
            Entity vehicle = vehicle(mc, -1); created.add(vehicle); SOUNDS.clear(); recording = true;
            mc.interactionManager.attackEntity(player, vehicle);
            String expected = distance < threshold ? "power_beta:entity.player.attack.sweep" : "power_beta:entity.player.attack.strong";
            check(SOUNDS.size() == 1 && SOUNDS.get(0).id.equals(expected), "displacement boundary=" + distance + " expected=" + expected + " sounds=" + SOUNDS);
          }
          player.method_1340(baseX, py, baseZ); player.prevX = baseX; player.prevZ = baseZ;
          player.velocityX = player.velocityZ = 1; charge(mc, 25);
          Entity stationary = vehicle(mc, -1); created.add(stationary); SOUNDS.clear(); mc.interactionManager.attackEntity(player, stationary);
          check(SOUNDS.size() == 1 && SOUNDS.get(0).id.equals("power_beta:entity.player.attack.sweep"), "velocity overrode stationary actual displacement " + SOUNDS);
          player.field_1623 = false; charge(mc, 25); SOUNDS.clear();
          Entity airborne = vehicle(mc, -1); created.add(airborne); mc.interactionManager.attackEntity(player, airborne);
          check(SOUNDS.size() == 1 && SOUNDS.get(0).id.equals("power_beta:entity.player.attack.strong"), "airborne sword emitted a sweep " + SOUNDS);
        } finally {
          player.method_1340(px, py, pz); player.prevX = oldPrevX; player.prevZ = oldPrevZ;
          player.velocityX = oldVx; player.velocityZ = oldVz;
        }
      });
      test("AUDIO473 accepted misses reset charge; suppressed presses and right use retain it", () -> {
        CreativeVehicleChecks.mode(mc, "SURVIVAL"); player.inventory.main[0] = null;
        var input = (Client253Access)mc; mc.field_2823 = null; charge(mc, 25);
        field(mc.getClass(), "field_2787").setInt(mc, 7); input.power$use(0);
        check(((VehicleAttackSoundPlayer)player).power$vehicleSoundCharge() == 25, "suppressed press reset charge");
        field(mc.getClass(), "field_2787").setInt(mc, 0); input.power$use(1);
        check(((VehicleAttackSoundPlayer)player).power$vehicleSoundCharge() == 25, "right use reset charge");
        SOUNDS.clear(); recording = true; input.power$use(0);
        check(((VehicleAttackSoundPlayer)player).power$vehicleSoundCharge() == 0 && SOUNDS.isEmpty(), "accepted miss did not reset silently");
        Entity vehicle = vehicle(mc, -1); created.add(vehicle); mc.interactionManager.attackEntity(player, vehicle);
        check(SOUNDS.size() == 1 && SOUNDS.get(0).id.equals("power_beta:entity.player.attack.weak"), "miss followed by vehicle hit was charged " + SOUNDS);
        var off = AudioConfig.copy(); off.vehicleAttackSounds = false; AudioConfig.preview(off); charge(mc, 25);
        field(mc.getClass(), "field_2787").setInt(mc, 0); mc.field_2823 = null; input.power$use(0);
        check(((VehicleAttackSoundPlayer)player).power$vehicleSoundCharge() == 25, "disabled miss changed charge");
        AudioConfig.preview(enabled); mc.field_2823 = priorHit;
      });
      test("AUDIO473 native mining abort resets charge once; idle cancellation and completed mining retain it", () -> {
        CreativeVehicleChecks.mode(mc, "SURVIVAL"); player.inventory.main[0] = null;
        var nativeManager = new SingleplayerInteractionManager(mc); mc.interactionManager = nativeManager;
        int x = (int)Math.floor(player.x) + 3, y = Math.min(124, (int)Math.floor(player.boundingBox.minY) + 2), z = (int)Math.floor(player.z);
        int block = mc.world.getBlockId(x, y, z), metadata = mc.world.method_1778(x, y, z);
        try {
          charge(mc, 25); nativeManager.method_1705();
          check(((VehicleAttackSoundPlayer)player).power$vehicleSoundCharge() == 25, "idle cancellation reset charge");
          mc.world.method_154(x, y, z, 1, 0); nativeManager.method_1707(x, y, z, 1);
          check(mc.world.getBlockId(x, y, z) == 1, "fixture stone was instantly mined");
          charge(mc, 25); nativeManager.method_1705();
          check(((VehicleAttackSoundPlayer)player).power$vehicleSoundCharge() == 0, "native noninstant mining abort did not reset charge");
          SOUNDS.clear(); recording = true; Entity vehicle = vehicle(mc, -1); created.add(vehicle);
          nativeManager.attackEntity(player, vehicle);
          check(SOUNDS.size() == 1 && SOUNDS.get(0).id.equals("power_beta:entity.player.attack.weak"), "mining abort followed by vehicle hit was charged " + SOUNDS);
          charge(mc, 25); nativeManager.method_1705();
          check(((VehicleAttackSoundPlayer)player).power$vehicleSoundCharge() == 25, "repeated idle abort reset charge");
          mc.world.method_154(x, y, z, 1, 0); nativeManager.method_1707(x, y, z, 1);
          nativeManager.method_1716(x, y, z, 1); charge(mc, 25); nativeManager.method_1705();
          check(((VehicleAttackSoundPlayer)player).power$vehicleSoundCharge() == 25, "completed mining left an active abort");
          mc.world.method_154(x, y, z, 1, 0); nativeManager.method_1707(x, y, z, 1);
          var off = AudioConfig.copy(); off.vehicleAttackSounds = false; AudioConfig.preview(off); charge(mc, 25); nativeManager.method_1705();
          check(((VehicleAttackSoundPlayer)player).power$vehicleSoundCharge() == 25, "disabled mining abort changed charge");
          AudioConfig.preview(enabled);
          mc.world.method_154(x, y, z, 0, 0); mc.field_2823 = new class_27(x, y, z, 1, net.minecraft.util.math.Vec3d.createCached(x + .5, y + 1, z + .5));
          charge(mc, 25); field(mc.getClass(), "field_2787").setInt(mc, 0); ((Client253Access)mc).power$use(0);
          check(((VehicleAttackSoundPlayer)player).power$vehicleSoundCharge() == 0, "now-air tile did not use miss charge reset");
        } finally {
          mc.world.method_154(x, y, z, block, metadata); AudioConfig.preview(enabled);
          mc.interactionManager = priorInteraction; mc.field_2823 = priorHit;
        }
      });
      test("AUDIO473 tool swap resets only sound charge and charges with native player ticks", () -> {
        CreativeVehicleChecks.mode(mc, "SURVIVAL"); charge(mc, 25);
        player.inventory.main[0] = new ItemStack(267, 1, 0); player.tick();
        check(((VehicleAttackSoundPlayer)player).power$vehicleSoundCharge() == 0, "sword swap did not reset charge");
        for (int tick = 0; tick < 12; tick++) player.tick();
        check(((VehicleAttackSoundPlayer)player).power$vehicleSoundCharge() == 12, "native ticks did not charge sword sound");
        Entity vehicle = vehicle(mc, -1); created.add(vehicle); player.field_1623 = true; player.velocityX = player.velocityZ = 0;
        SOUNDS.clear(); recording = true; mc.interactionManager.attackEntity(player, vehicle);
        check(SOUNDS.size() == 1 && SOUNDS.get(0).id.equals("power_beta:entity.player.attack.sweep"), "charged stationary sword lacks modern sweep " + SOUNDS);
        player.inventory.main[0] = null;
      });
      test("AUDIO473 spectator and remote worlds retain ownership and no sound", () -> {
        Entity vehicle = vehicle(mc, -1); created.add(vehicle); SOUNDS.clear(); recording = true;
        CreativeVehicleChecks.mode(mc, "SPECTATOR"); mc.interactionManager.attackEntity(player, vehicle);
        check(!vehicle.dead && SOUNDS.isEmpty(), "spectator attack emitted sound");
        CreativeVehicleChecks.mode(mc, "CREATIVE"); mc.world.isRemote = true;
        try { mc.interactionManager.attackEntity(player, vehicle); check(!vehicle.dead && SOUNDS.isEmpty(), "remote world emitted local sound"); }
        finally { mc.world.isRemote = false; }
        check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "vehicle audio fixture wrote settings");
      });
    } finally {
      recording = false; mc.world.isRemote = false; mc.interactionManager = priorInteraction; mc.field_2823 = priorHit;
      field(mc.getClass(), "field_2787").setInt(mc, priorCooldown);
      player.handSwinging = priorSwing; player.handSwingTicks = priorSwingTicks;
      for (Entity vehicle : created) {
        if (vehicle instanceof class_549 cart) for (int i = 0; i < cart.size(); i++) cart.setStack(i, null);
        vehicle.markDead();
      }
      for (Object object : mc.world.method_175(class_142.class, area))
        if (!priorDrops.contains(object)) ((Entity)object).markDead();
      AccessRules.restore(mc, RuleSet.cheats()); CreativeVehicleChecks.mode(mc, mode);
      player.read(saved); System.arraycopy(inventory, 0, player.inventory.main, 0, inventory.length); player.inventory.selectedSlot = slot;
      field(player.getClass(), "power$vehicleSoundCharge").setInt(player, priorCharge);
      field(player.getClass(), "power$vehicleSoundItem").setInt(player, priorItem);
      AudioConfig.preview(audio); AccessRules.restore(mc, rules); SprintConfig.preview(sprint);
      for (var value : sprintFields.entrySet()) value.getKey().set(sprintState, value.getValue());
    }
    log("AUDIO473 FAILURES " + failures);
  }
  private static final class RejectingBoat extends class_113 {
    int attempts;
    RejectingBoat(Minecraft mc) { super(mc.world, mc.player.x + 2, mc.player.y, mc.player.z); }
    @Override public boolean damage(Entity attacker, int amount) { attempts++; return false; }
  }
  private static Entity vehicle(Minecraft mc, int type) {
    Entity vehicle = type < 0 ? new class_113(mc.world, mc.player.x + 2, mc.player.y, mc.player.z)
        : new class_549(mc.world, mc.player.x + 2, mc.player.y, mc.player.z, type);
    mc.world.method_210(vehicle); return vehicle;
  }
}
