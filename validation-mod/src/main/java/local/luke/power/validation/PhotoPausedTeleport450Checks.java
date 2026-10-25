package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;

import java.lang.reflect.*;
import java.util.*;
import local.luke.power.input.DetachedCamera;
import local.luke.power.input.PhotoModeHooks;
import local.luke.power.map.*;
import local.luke.power.mixin.CullingChunkCacheAccessor;
import local.luke.power.rules.GameRule;
import local.luke.power.ui.PhotoModeTools;
import net.minecraft.class_43;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** A real photo-context teleport must reconcile a paused player without ticking the world. */
final class PhotoPausedTeleport450Checks {
  private PhotoPausedTeleport450Checks() {}

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && !mc.player.dead && mc.player.health > 0,
        "open a disposable singleplayer world with a living player");
    check(!DetachedCamera.isActive() && mc.player.field_1595 == null, "leave freecam and dismount before the paused teleport check");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    check(mc.world.method_259() instanceof CullingChunkCacheAccessor, "use the native loaded-chunk cache");
    var world = mc.world; var player = mc.player; var previous = mc.currentScreen;
    var cache = (CullingChunkCacheAccessor)world.method_259();
    Map<Integer, class_43> loaded = new HashMap<>(cache.power$loadedChunks());
    var chunks = new HashSet<>(loaded.values());
    var memberships = memberships(chunks, player);
    check(player.field_1618 && memberships.size() == 1, "player must begin in exactly one native chunk section");
    var dirty = new IdentityHashMap<class_43, Boolean>(); chunks.forEach(chunk -> dirty.put(chunk, chunk.field_969));
    var pose = new PlayerSnapshot(player); var rules = AccessRules.copy(mc);
    long time = world.getTime(); int age = player.field_1645;
    boolean paused = mc.paused, grabbed = Mouse.isGrabbed(), repeat = Keyboard.areRepeatEventsEnabled(), focused = mc.field_2778;
    var camera = mc.field_2807;
    var pause = new net.minecraft.class_525();
    var photo = (Screen)Class.forName("local.luke.power.controls.tweaks.photomode.PhotoModeScreen")
        .getConstructor(Screen.class).newInstance(pause);
    try {
      AccessRules.set(mc, GameRule.ALLOW_TELEPORTATION, true);
      PhotoModeTools.tick(mc);
      mc.setScreen(pause); mc.paused = true; mc.setScreen(photo);
      Object tools = PhotoModeHooks.tools();
      Waypoint target = safeTarget(mc, tools, chunks, player.field_1619, player.field_1621);
      check(target != null, "no safe target found in the nearest eight already-loaded neighboring chunks");
      check((target.x >> 4) != player.field_1619 || (target.z >> 4) != player.field_1621,
          "fixture must cross a native chunk boundary");
      check(world.getTime() == time && player.field_1645 == age && loaded.equals(cache.power$loadedChunks()),
          "target preparation advanced simulation or loaded chunks");
      // These are intentionally stale before the production action. The fixture never reconciles them.
      player.prevX = player.x - 7; player.prevY = player.y - 5; player.prevZ = player.z - 9;
      player.field_1637 = player.x + 17; player.field_1638 = player.y + 13; player.field_1639 = player.z + 19;
      field(tools, "target", target.copy());
      Method action = PhotoModeTools.class.getDeclaredMethod("action", int.class); action.setAccessible(true);
      action.invoke(tools, 2);
      check(mc.world == world && mc.player == player && mc.currentScreen == pause && mc.paused && pause.shouldPause(),
          "photo teleport did not return to the same paused world and pause-screen instance");
      check(close(player.x, target.x + .5) && close(player.z, target.z + .5) && close(player.boundingBox.minY, target.y),
          "photo context action did not move the physical player to the target feet position");
      check(close(player.prevX, player.x) && close(player.prevY, player.y) && close(player.prevZ, player.z),
          "native previous-position history stayed at the old player position");
      check(close(player.field_1637, player.x) && close(player.field_1638, player.y) && close(player.field_1639, player.z),
          "native render interpolation history stayed at the old player position while paused");
      int section = Math.max(0, Math.min(7, (int)Math.floor(player.y / 16)));
      check(player.field_1618 && player.field_1619 == (target.x >> 4) && player.field_1621 == (target.z >> 4) && player.field_1620 == section,
          "native entity chunk coordinates were not reconciled");
      class_43 destination = MapSampler.loaded(world, target.x >> 4, target.z >> 4);
      var after = memberships(chunks, player);
      check(destination != null && after.size() == 1 && after.get(0).entities == destination.field_965[section],
          "player was duplicated or remained in the old native chunk entity list");
      check(world.getTime() == time && player.field_1645 == age, "paused photo teleport simulated a world or player tick");
      check(loaded.equals(cache.power$loadedChunks()), "paused photo teleport loaded or replaced a terrain chunk");
      log("PASS PHOTO450 paused context teleport updates physical feet, both native position histories and native chunk membership without ticks or extra chunks");
    } finally {
      // Restore exact membership and primitive state directly, without invoking the production repair or ticking.
      removeMembership(new HashSet<>(cache.power$loadedChunks().values()), player);
      pose.restore(player);
      for (Membership slot : memberships) slot.entities.add(Math.min(slot.index, slot.entities.size()), player);
      dirty.forEach((chunk, value) -> chunk.field_969 = value);
      mc.world = world; mc.player = player; AccessRules.restore(mc, rules);
      mc.setScreen(previous); mc.paused = paused; mc.field_2807 = camera; mc.field_2778 = focused;
      Mouse.setGrabbed(grabbed); Keyboard.enableRepeatEvents(repeat);
    }
  }

  private static Waypoint safeTarget(Minecraft mc, Object tools, Set<class_43> loaded, int oldX, int oldZ) throws Exception {
    var candidates = loaded.stream().filter(chunk -> chunk.field_962 != oldX || chunk.field_963 != oldZ)
        .filter(chunk -> Math.abs((long)chunk.field_962 - oldX) <= 3 && Math.abs((long)chunk.field_963 - oldZ) <= 3)
        .sorted(Comparator.comparingLong(chunk -> Math.abs((long)chunk.field_962 - oldX) + Math.abs((long)chunk.field_963 - oldZ)))
        .limit(8).toList();
    for (class_43 chunk : candidates) for (int localX : new int[]{3, 7, 11}) for (int localZ : new int[]{3, 7, 11}) {
      if (MapSampler.loaded(mc.world, chunk.field_962, chunk.field_963) != chunk) continue;
      for (int y = 125; y >= 2; y--) {
        if (chunk.method_859(localX, y - 1, localZ) == 0 || chunk.method_859(localX, y, localZ) != 0
            || chunk.method_859(localX, y + 1, localZ) != 0) continue;
        var target = new Waypoint(); target.dimension = mc.world.dimension.id;
        target.x = chunk.field_962 * 16 + localX; target.y = y; target.z = chunk.field_963 * 16 + localZ;
        field(tools, "target", target.copy());
        if ((boolean)call(tools, "safeTeleport")) return target;
      }
    }
    return null;
  }

  private record Membership(List<Entity> entities, int index) {}
  @SuppressWarnings("unchecked")
  private static List<Membership> memberships(Set<class_43> chunks, Entity player) {
    var result = new ArrayList<Membership>();
    for (class_43 chunk : chunks) for (List<?> entities : chunk.field_965) for (int i = 0; i < entities.size(); i++)
      if (entities.get(i) == player) result.add(new Membership((List<Entity>)entities, i));
    return result;
  }
  private static void removeMembership(Set<class_43> chunks, Entity player) {
    for (class_43 chunk : chunks) for (List<?> entities : chunk.field_965) entities.removeIf(entity -> entity == player);
  }
  private static boolean close(double left, double right) { return Math.abs(left - right) < .000001; }

  private static final class PlayerSnapshot {
    private final Map<Field, Object> primitives = new LinkedHashMap<>();
    private final double[] box;
    PlayerSnapshot(Entity player) throws IllegalAccessException {
      for (Field field : player.getClass().getFields()) if (!Modifier.isStatic(field.getModifiers())
          && !Modifier.isFinal(field.getModifiers()) && field.getType().isPrimitive()) primitives.put(field, field.get(player));
      var b = player.boundingBox; box = new double[]{b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ};
    }
    void restore(Entity player) throws IllegalAccessException {
      for (var entry : primitives.entrySet()) entry.getKey().set(player, entry.getValue());
      player.boundingBox.set(box[0], box[1], box[2], box[3], box[4], box[5]);
    }
  }
}
