package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.death.DeathItems;
import local.luke.power.gameplay.*;
import local.luke.power.input.*;
import local.luke.power.rules.RuleSet;
import local.luke.power.storage.PowerConfig;
import local.luke.power.validation.mixin.Client253Access;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.StationAPI;
import net.modificationstation.stationapi.api.event.entity.player.PlayerEvent;
import org.lwjgl.opengl.Display;

/** Real crouch ticks, native picking and a native right click three blocks down a wall. */
final class CrouchReach473Checks {
  private CrouchReach473Checks() {}
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive()
        && !DetachedCamera.isActive() && !InteractionState.carryingContainer, "focus a disposable singleplayer world outside freecam/carrying");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    check(mc.interactionManager instanceof SingleplayerInteractionManager, "use the native Survival interaction manager");
    check(!local.luke.power.flexible.FlexiblePlacement.modifiersActive(), "release flexible placement modifiers first");
    PowerConfig.flushPending(); byte[] preferences = Files.readAllBytes(PowerConfig.path());
    var owner = mc.player; var camera = mc.field_2807; var screen = mc.currentScreen; var hit = mc.field_2823;
    var gameplay = GameplayConfig.copy(); var building = local.luke.power.building.config.Config.current().copy(); var rules = AccessRules.copy(mc);
    Class<?> creative = Class.forName("local.luke.power.creative.config.Config"); Object before = creative.getMethod("current").invoke(null);
    before = before.getClass().getMethod("copy").invoke(before); var preview = creative.getMethod("preview", before.getClass());
    int x = (int)Math.floor(owner.x) + 5, z = (int)Math.floor(owner.z) + 5, y = 112;
    check(mc.world.method_155(x - 3, y - 4, z - 3, x + 3, y + 4, z + 3), "load the fixture area first");
    List<int[]> blocks = new ArrayList<>(); List<Map<String, Object>> results = new ArrayList<>();
    for (int bx = x - 3; bx <= x + 3; bx++) for (int bz = z - 3; bz <= z + 3; bz++) for (int by = y - 4; by <= y + 4; by++) {
      check(mc.world.method_1777(bx, by, bz) == null, "wall fixture overlaps a block entity");
      blocks.add(new int[]{bx, by, bz, mc.world.getBlockId(bx, by, bz), mc.world.method_1778(bx, by, bz)});
    }
    var player = new ClientPlayerEntity(mc, mc.world, mc.session, owner.dimensionId);
    player.field_161 = new class_41();
    try {
      mc.player = player; mc.field_2807 = player; mc.setScreen(null); AccessRules.restore(mc, RuleSet.cheats()); CreativeVehicleChecks.mode(mc, "SURVIVAL");
      var placement = building.copy(); placement.flexible.placeAgainstContainers = false;
      local.luke.power.building.config.Config.preview(placement);
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], 0, 0);
      for (int by = y - 3; by <= y; by++) mc.world.method_154(x, by, z, 1, 0);
      for (int[] normal : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
        configure(true, true); pose(mc, x, y, z, normal, true);
        check(ModernSwimming.crouched(player) && !ModernSwimming.compact(player), "native ticks did not crouch the player");
        double feet = player.boundingBox.minY, eye = player.y + InteractionEye.offset(player, 1, 0);
        double distance = aim(mc, x, y - 3, z, normal);
        check(distance > 4 && distance < 4.5, "wall example must distinguish Beta and modern reach: " + distance);
        check(player.method_929(4, 1) == null, "the old four-block ray already reaches this wall example");
        check(Math.abs(mc.interactionManager.method_1715() - 4.5) < 1e-6, "crouched native manager did not expose modern block reach");
        player.inventory.selectedSlot = 0; player.inventory.main[0] = DeathChecks.tagged(4, 8, 0, "crouch-wall-reach");
        String identity = DeathItems.encode(player.inventory.main[0]);
        mc.field_2818.method_1838(1);
        check(target(mc.field_2823, x, y - 3, z), "native picking missed the wall three blocks below the supporting top block");
        ((Client253Access)mc).power$use(1);
        check(mc.world.getBlockId(x + normal[0], y - 3, z + normal[1]) == 4, "native right click did not place beside the wall");
        check(player.inventory.main[0] != null && player.inventory.main[0].count == 7, "Survival placement did not consume one block");
        player.inventory.main[0].count = 8;
        check(identity.equals(DeathItems.encode(player.inventory.main[0])), "placement changed native item data");
        check(Math.abs(player.boundingBox.minY - feet) < 1e-7 && Math.abs(eye - feet - 1.27) < 1e-5, "reach fix changed physical feet or crouched eye");
        mc.world.method_154(x + normal[0], y - 3, z + normal[1], 0, 0);
        results.add(Map.of("normalX", normal[0], "normalZ", normal[1], "feetY", feet, "eyeY", eye, "distance", distance, "reach", 4.5, "placed", true));
      }
      for (boolean crawling : new boolean[]{false, true}) for (boolean lower : new boolean[]{false, true}) for (boolean sneak : new boolean[]{false, true}) {
        if (crawling && lower && sneak) continue;
        configure(crawling, lower); pose(mc, x, y, z, new int[]{1,0}, sneak); aim(mc, x, y - 3, z, new int[]{1,0});
        check(Math.abs(mc.interactionManager.method_1715() - 4) < 1e-6, "ordinary/Beta/off reach changed");
        mc.field_2818.method_1838(1); check(!target(mc.field_2823, x, y - 3, z), "off or standing state acquired modern wall reach");
      }
      configure(true, true); pose(mc, x, y, z, new int[]{1,0}, true);
      check(range(player, class_212.ENTITY, 3) == 3, "entity interaction reach changed");
      check(range(player, class_212.TILE, 6) == 6, "custom block reach changed");
      var foreign = new ClientPlayerEntity(mc, mc.world, mc.session, owner.dimensionId);
      ((SwimPlayer)foreign).power$swim().crouched = true;
      check(range(foreign, class_212.TILE, 4) == 4, "foreign player acquired local reach");
      mc.world.isRemote = true; try { check(range(player, class_212.TILE, 4) == 4, "multiplayer acquired local reach"); } finally { mc.world.isRemote = false; }
      FreecamPreviewChecks.active(true); try { check(range(player, class_212.TILE, 4) == 4, "detached camera changed physical reach"); } finally { FreecamPreviewChecks.active(false); }
      var state = ((SwimPlayer)player).power$swim(); state.crouched = false; state.compact = true;
      check(range(player, class_212.TILE, 4) == 4, "compact pose acquired the crouching reach adjustment");
      state.compact = false; state.crouched = true;
      for (int configured : new int[]{40, 100}) {
        Object settings = before.getClass().getMethod("copy").invoke(before); settings.getClass().getField("blockReach").setInt(settings, configured); preview.invoke(null, settings);
        CreativeVehicleChecks.mode(mc, "CREATIVE");
        check(Math.abs(mc.interactionManager.method_1715() - configured / 10d) < 1e-6, "Creative configured reach changed");
        CreativeVehicleChecks.mode(mc, "SURVIVAL");
      }
      log("CROUCHREACH473 wall placement in four directions and scope guards PASS");
    } finally {
      FreecamPreviewChecks.active(false); mc.world.isRemote = false;
      GameplayConfig.preview(gameplay); local.luke.power.building.config.Config.preview(building); preview.invoke(null, before);
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[3], b[4]);
      mc.player = owner; mc.field_2807 = camera; mc.field_2823 = hit; AccessRules.restore(mc, rules); mc.setScreen(screen);
      Path report = Path.of("power-beta-data/reports/crouch-reach-473"); Files.createDirectories(report);
      Files.writeString(report.resolve("wall-placement.json"), Catalog.JSON.toJson(results));
    }
    check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "reach fixture changed saved settings");
  }
  private static void configure(boolean crawling, boolean lower) {
    var settings = GameplayConfig.copy(); settings.modernCrawling = crawling; settings.crouchCameraHeight = lower; GameplayConfig.preview(settings);
  }
  private static void pose(Minecraft mc, int x, int y, int z, int[] normal, boolean sneak) {
    var player = mc.player; var state = ((SwimPlayer)player).power$swim();
    state.swimming = state.compact = state.crouched = state.lowCeiling = false; state.resetCamera(1.62f);
    player.spacingXZ = .6f; player.spacingY = 1.8f; player.field_1631 = 1.62f; player.field_1640 = 0;
    player.method_1341(x + .5 + normal[0] * .7, y + 1, z + .5 + normal[1] * .7, 0, 0);
    player.velocityX = player.velocityY = player.velocityZ = 0; player.field_161.field_2536 = sneak;
    for (int tick = 0; tick < 24; tick++) player.tick();
  }
  private static double aim(Minecraft mc, int x, int y, int z, int[] normal) {
    var player = mc.player; double eye = player.y + InteractionEye.offset(player, 1, 0);
    double dx = x + .5 + normal[0] * .5 - player.x, dz = z + .5 + normal[1] * .5 - player.z, dy = y + .95 - eye;
    player.yaw = player.prevYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
    player.pitch = player.prevPitch = (float)-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
    player.prevX = player.x; player.prevY = player.y; player.prevZ = player.z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }
  private static boolean target(class_27 hit, int x, int y, int z) { return hit != null && hit.field_1983 == class_212.TILE && hit.field_1984 == x && hit.field_1985 == y && hit.field_1986 == z; }
  private static double range(ClientPlayerEntity player, class_212 type, double ordinary) { return StationAPI.EVENT_BUS.post(PlayerEvent.Reach.builder().player(player).type(type).currentReach(ordinary).build()).currentReach; }
}
