package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.FreecamPreviewChecks.*;
import java.nio.file.Files;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.hud.*;
import local.luke.power.input.DetachedCamera;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.DebugOverlay;
import net.minecraft.class_56;
import net.minecraft.class_549;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

final class Release162Checks {
  private static void reach(int blocks, int entities) throws Exception {
    Class<?> api = Class.forName("local.luke.power.creative.config.Config");
    Object settings = api.getMethod("current").invoke(null);
    settings = settings.getClass().getMethod("copy").invoke(settings);
    settings.getClass().getField("blockReach").setInt(settings, blocks);
    settings.getClass().getField("entityReach").setInt(settings, entities);
    api.getMethod("preview", settings.getClass()).invoke(null, settings);
  }
  private static void playerPose(Minecraft mc, double z) {
    mc.player.method_1341(32.5, 110.5 - mc.player.field_1631, z, 180, 0);
    mc.player.prevYaw = mc.player.yaw; mc.player.prevPitch = mc.player.pitch;
  }
  private static List<String> debug(Minecraft mc) {
    mc.options.debugHud = true; mc.options.hideHud = false;
    mc.inGameHud.render(1, false, 0, 0);
    return DebugOverlay.lastLayout().stream().map(DebugOverlay.Line::text).toList();
  }
  private static void sample(Minecraft mc) { for (int i = 0; i < 4; i++) InfoHud.tick(mc); }

  static void run(Minecraft mc) throws Exception {
    failures = 0; scene(mc); PowerConfig.flushPending();
    var before = Files.readAllBytes(PowerConfig.path());
    var session = SettingsRegistry.open(mc);
    boolean hidden = mc.options.hideHud, debug = mc.options.debugHud;
    var rules = (CheatWorld)mc.world.method_262(); boolean cheats = rules.power$cheatsEnabled();
    String mode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    try {
      for (String name : List.of("SURVIVAL", "CREATIVE", "SPECTATOR"))
        for (boolean detached : new boolean[]{false, true}) test("creative preview reach in " + name + " detached=" + detached, () -> {
          active(false); rules.power$cheatsEnabled(true); CreativeVehicleChecks.mode(mc, name);
          if (name.equals("SURVIVAL")) rules.power$cheatsEnabled(false);
          for (int range : new int[]{30, 50, 100}) {
            reach(range, range);
            double z = 33 + range / 10.0 - .01;
            playerPose(mc, z); pose(32.5, 110.5, z, 0, 0); active(detached);
            var originalHit = mc.field_2823;
            blockHit(mc, 32, 110, 32); check(snapshot(mc).size() == 27, "chest contents missing at creative reach");
            check(mc.field_2823 == originalHit, "preview changed interaction target");
            playerPose(mc, z + .02); pose(32.5, 110.5, z + .02, 0, 0);
            check(snapshot(mc).isEmpty(), "preview exceeded configured reach");
          }
          if (name.equals("SURVIVAL")) check(mc.interactionManager.method_1715() < 5, "survival interaction reach changed");
          active(false);
        });
      test("attached previews use frame interpolation", () -> {
        CreativeVehicleChecks.mode(mc, "SURVIVAL"); reach(50, 50); playerPose(mc, 39.75);
        mc.player.prevZ = 36.75;
        var find = Class.forName("local.luke.power.inventory.ContainerTarget").getDeclaredMethod("find", Minecraft.class, float.class);
        find.setAccessible(true);
        check(find.invoke(null, mc, 0F) != null && find.invoke(null, mc, 1F) == null, "preview ignored interpolation");
      });
      for (boolean detached : new boolean[]{false, true}) test("independent entity reach and occlusion detached=" + detached, () -> {
        playerPose(mc, 40.5); pose(32.5, 110.5, 40.5, 0, 0); active(detached);
        var cart = new class_549(mc.world, 32.5, 110, 34, 1); mc.world.method_210(cart);
        try {
          cart.setStack(0, new ItemStack(264, 23, 0)); reach(30, 80);
          check(target(mc).field_1989 == cart && snapshot(mc).get(0).count == 23, "entity reach followed block reach");
          reach(100, 30); check(snapshot(mc).isEmpty(), "cart exceeded entity reach or failed to occlude chest");
          reach(100, 100); mc.world.method_154(32, 110, 36, 1, 0);
          check(snapshot(mc).isEmpty(), "preview passed through solid wall");
        } finally { mc.world.method_154(32,110,36,0,0); cart.setStack(0,null); cart.markDead(); active(false); }
      });
      test("slab completion sits beside Shift placing with no messages or HUD option", () -> {
        var rows = SettingsLayout.ordered(session.settings());
        check(rows.stream().noneMatch(s -> s.id.equals("activeTweaks.slabCompletion") || s.id.equals("tweaks.slabs.announceToggle")), "retired setting shown");
        var slab = rows.stream().filter(s -> s.id.equals("tweaks.slabs.enabled")).findFirst().orElseThrow();
        check(slab.page.equals("Building") && slab.group.equals("Placement rules"), "slab group wrong");
        var previous = rows.get(rows.indexOf(slab) - 1);
        check(previous.id.equals("power_controls:gameplay.shiftPlacingBlacklist"), "slab setting not adjacent to Shift placing");
        check(!SettingsLayout.groups("Building").contains("Slab completion"), "empty slab group retained");
      });
      rules.power$cheatsEnabled(true);
      for (String name : List.of("addFacingToDebugOverlay", "addBiomeToDebugOverlay", "addBlockLightToDebugOverlay", "addSlimeChunkToDebugOverlay"))
        session.settings().stream().filter(s -> s.id.equals("power_compat:config." + name)).findFirst().orElseThrow().parse("true");
      session.preview(true);
      var hud = InfoHudConfig.copy(); hud.enabled = true;
      var cluster = hud.clusters[0]; cluster.coordinates = cluster.direction = cluster.biome = cluster.chunk = true;
      InfoHudConfig.preview(hud);
      test("F3 and custom HUD use detached coordinates, facing, chunk, light and slime chunk", () -> {
        playerPose(mc, 39.75); active(true); pose(32.25, 112.75, 32.5, 0, 0);
        for (int yaw : new int[]{0, 90, 180, 270}) {
          pose(32.25, 112.75, 32.5, 0, yaw); sample(mc);
          var lines = debug(mc);
          check(lines.containsAll(List.of("x: 32.25", "y: " + local.luke.power.input.HeadGeometry.baseY(112.75), "z: 32.5")), "F3 used body coordinates " + lines);
          int facing = Math.floorMod(yaw / 90 + 2, 4);
          check(lines.contains("f: " + facing), "native F3 facing wrong");
          String[] names = {"south (+Z)", "west (-X)", "north (-Z)", "east (+X)"};
          check(lines.contains("Facing: " + names[facing]), "extra F3 facing wrong");
          check(InfoHud.lines(cluster).contains("32, 112, 32 " + HudText.direction(yaw - 180)), "custom coordinates/facing wrong");
        }
        var lines = debug(mc);
        check(InfoHud.lines(cluster).contains("Chunk: 2 / 2"), "camera chunk missing");
        check(lines.contains("Block light: " + mc.world.method_164(class_56.BLOCK,32,112,32)), "body block light used");
        check(lines.contains("Slime Chunk: " + (mc.world.method_199(32,32).method_863(987234911L).nextInt(10) == 0)), "body slime chunk used");
      });
      test("camera biome and negative coordinates work without loading distant chunks", () -> {
        String bodyBiome = mc.world.method_1781().method_1787(32,39).field_888;
        int x = -1_000_000, z = -1_000_000;
        while (x < -990_000 && mc.world.method_1781().method_1787(x,z).field_888.equals(bodyBiome)) x += 64;
        String biome = mc.world.method_1781().method_1787(x,z).field_888;
        check(!biome.equals(bodyBiome) && !mc.world.method_239(x,64,z), "fixture must find a distinct unloaded biome");
        pose(x + .25, 160.5, z + .75, 0, 0); sample(mc); var lines = debug(mc);
        check(lines.contains("Biome: " + biome) && InfoHud.lines(cluster).contains("Biome: " + biome), "body biome used");
        check(InfoHud.lines(cluster).contains(HudText.coordinates(x,160,z) + " (-Z, N)"), "negative coordinate floor wrong");
        check(InfoHud.lines(cluster).contains("Chunk: " + Math.floorDiv(x,16) + " / " + Math.floorDiv(z,16)), "negative chunk floor wrong");
        check(lines.contains("Block light: Unknown") && lines.contains("Slime Chunk: Unknown"), "unloaded terrain reported body data");
        check(!mc.world.method_239(x,64,z), "HUD loaded a chunk");
      });
      test("freecam exit immediately restores player HUD data and preserves player and settings", () -> {
        double x = mc.player.x, y = mc.player.y, z = mc.player.z;
        active(false); InfoHud.tick(mc); var lines = debug(mc);
        check(lines.containsAll(List.of("x: " + x, "y: " + y, "z: " + z)), "F3 failed to restore player");
        String location = HudText.coordinates((int)Math.floor(x),(int)Math.floor(local.luke.power.hud.HudCoordinates.y(mc.player)),(int)Math.floor(z))
            + " " + HudText.direction(mc.player.yaw);
        check(InfoHud.lines(cluster).contains(location), "custom HUD kept stale camera data");
        check(mc.player.x == x && mc.player.y == y && mc.player.z == z, "HUD moved player");
        check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "preview wrote settings");
      });
    } finally {
      active(false); session.discard(); CreativeVehicleChecks.mode(mc, mode); rules.power$cheatsEnabled(cheats);
      mc.options.hideHud = hidden; mc.options.debugHud = debug;
    }
    log("RELEASE 162 FAILURES " + failures);
  }
}
