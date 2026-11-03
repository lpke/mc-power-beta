package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.building.camera.FreeLook;
import local.luke.power.building.config.Config;
import local.luke.power.gameplay.GameplayConfig;
import local.luke.power.input.DetachedCamera;
import local.luke.power.storage.PowerConfig;
import net.minecraft.class_41;
import net.minecraft.class_113;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Sustained full-lock native driving and actual boat/player render angles. Disposable worlds only. */
public final class BoatTurn473Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/boat-turn-473");
  public static boolean capture;
  public static float renderedBoatYaw;
  private BoatTurn473Checks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(action.equals("native") || action.equals("observe"), "use boat-turn-473 native|observe");
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable singleplayer world");
    check(!DetachedCamera.isActive() && !FreeLook.active() && mc.player.field_1595 == null, "leave freecam, free look and vehicles first");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    PowerConfig.flushPending(); byte[] config = Files.readAllBytes(PowerConfig.path()); Files.createDirectories(REPORT);
    var p = mc.player; var saved = new net.minecraft.nbt.NbtCompound(); p.write(saved);
    var building = Config.current().copy(); var gameplay = GameplayConfig.copy();
    var input = p.field_161; var screen = mc.currentScreen;
    boolean third = mc.options.thirdPerson, bob = mc.options.bobView;
    var frontField = Class.forName("local.luke.power.controls.util.ModOptions").getField("frontView");
    boolean front = frontField.getBoolean(null);
    String mode = ((local.luke.power.input.PlayerMode)p).power$spectator() ? "SPECTATOR"
        : ((local.luke.power.input.PlayerMode)p).power$creative() ? "CREATIVE" : "SURVIVAL";
    float body = p.field_1012, previousBody = p.field_1013;
    int cx = (int)Math.floor(p.x), cz = (int)Math.floor(p.z), water = 112;
    check(mc.world.method_155(cx - 10, water - 1, cz - 10, cx + 10, water + 5, cz + 10), "load the fixture area first");
    List<int[]> blocks = new ArrayList<>();
    for (int x = cx - 10; x <= cx + 10; x++) for (int z = cz - 10; z <= cz + 10; z++) for (int y = water - 1; y <= water + 5; y++) {
      check(mc.world.method_1777(x, y, z) == null, "fixture overlaps a block entity");
      blocks.add(new int[]{x, y, z, mc.world.getBlockId(x, y, z), mc.world.method_1778(x, y, z)});
    }
    List<String> frames = new ArrayList<>();
    frames.add("scenario,tick,perspective,delta,boatYaw,boatPrevYaw,renderedBoatYaw,playerYaw,playerPrevYaw,bodyYaw,prevBodyYaw,renderedBodyYaw,renderedHeadYaw,cameraYaw");
    List<Map<String, Object>> reports = new ArrayList<>(); List<String> errors = new ArrayList<>();
    List<Entity> created = new ArrayList<>();
    int matrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      mc.setScreen(null); CreativeVehicleChecks.mode(mc, "SURVIVAL"); mc.options.bobView = false;
      p.field_161 = new class_41();
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[1] < water ? 1 : b[1] == water ? 9 : 0, 0);
      var renderer = new RiderRenderer(); renderer.setDispatcher(EntityRenderDispatcher.field_2489);
      for (boolean steering : new boolean[]{false, true}) for (boolean fast : new boolean[]{false, true})
        for (boolean protection : new boolean[]{false, true}) for (boolean placement : new boolean[]{false, true}) for (int direction : new int[]{-1, 1}) {
          String scenario = "steering=" + steering + " speed=" + fast + " protection=" + protection + " placement=" + placement + " direction=" + direction;
          var settings = building.copy(); settings.boatSteering = steering; settings.boatSpeed = fast; settings.boatProtection = protection; Config.preview(settings);
          var rules = local.luke.power.config.Catalog.JSON.fromJson(local.luke.power.config.Catalog.JSON.toJson(gameplay), local.luke.power.gameplay.GameplaySettings.class); rules.boatPlacementFacing = placement; GameplayConfig.preview(rules);
          var boat = new class_113(mc.world, cx + .5, water + 1, cz + .5); created.add(boat); mc.world.method_210(boat);
          boat.yaw = boat.prevYaw = direction * 350; p.yaw = p.prevYaw = boat.yaw + 90; p.pitch = p.prevPitch = 0;
          p.field_1012 = p.field_1013 = p.yaw;
          p.method_1376(boat); boat.method_1382();
          double speed = fast ? .75 : .35; boat.velocityX = -Math.cos(Math.toRadians(boat.yaw)) * speed; boat.velocityZ = -Math.sin(Math.toRadians(boat.yaw)) * speed;
          double maxBody = 0, maxHead = 0, maxCamera = 0, maxBoat = 0; int wraps = 0, recenters = 0;
          float[] lastBody = new float[3], lastHead = new float[3], lastCamera = new float[3], lastBoat = new float[3];
          boolean[] sampled = new boolean[3];
          for (int tick = 0; tick < 180; tick++) {
            // Translate the whole rig between ticks on a uniform pool. Motion and angles stay native.
            if (Math.hypot(boat.x - cx - .5, boat.z - cz - .5) > 5) {
              double dx = cx + .5 - boat.x, dz = cz + .5 - boat.z;
              boat.method_1340(boat.x + dx, boat.y, boat.z + dz); p.method_1340(p.x + dx, p.y, p.z + dz); recenters++;
            }
            boat.field_1637 = boat.x; boat.field_1638 = boat.y; boat.field_1639 = boat.z;
            p.field_1637 = p.x; p.field_1638 = p.y; p.field_1639 = p.z;
            p.field_161.field_2533 = 1; p.field_161.field_2532 = -direction;
            boat.tick(); p.method_1384();
            if (Math.abs(boat.yaw - boat.prevYaw) > 180) wraps++;
            if (boat.dead || p.field_1595 != boat) { errors.add(scenario + " lost its boat at tick " + tick); break; }
            float[] angles = {boat.yaw, boat.prevYaw, p.yaw, p.prevYaw, p.field_1012, p.field_1013};
            for (int perspective = 0; perspective < 3; perspective++) {
              mc.options.thirdPerson = perspective != 0; frontField.setBoolean(null, perspective == 2);
              for (float delta : new float[]{0, .25f, .5f, .75f, 1}) {
                var dispatcher = EntityRenderDispatcher.field_2489;
                dispatcher.method_1917(mc.world, mc.textureManager, mc.textRenderer, p, mc.options, delta);
                renderedBoatYaw = Float.NaN; capture = true;
                try { dispatcher.method_1921(boat, delta); } finally { capture = false; }
                renderer.render(p, 0, 0, 0, p.yaw, delta);
                float head = renderer.headYaw(), camera = dispatcher.field_2497;
                check(Float.isFinite(renderedBoatYaw) && Float.isFinite(renderer.bodyYaw) && Float.isFinite(head), "native render capture missing");
                if (sampled[perspective] && tick > 12) {
                  maxBoat = Math.max(maxBoat, Math.abs(wrap(renderedBoatYaw - lastBoat[perspective])));
                  maxBody = Math.max(maxBody, Math.abs(wrap(renderer.bodyYaw - lastBody[perspective])));
                  maxHead = Math.max(maxHead, Math.abs(wrap(head - lastHead[perspective])));
                  maxCamera = Math.max(maxCamera, Math.abs(wrap(camera - lastCamera[perspective])));
                }
                lastBoat[perspective] = renderedBoatYaw; lastBody[perspective] = renderer.bodyYaw; lastHead[perspective] = head; lastCamera[perspective] = camera; sampled[perspective] = true;
                if (steering && fast && protection && placement) frames.add(scenario.replace(' ', ';') + "," + tick + "," + perspective + "," + delta + "," + boat.yaw + "," + boat.prevYaw + "," + renderedBoatYaw + "," + p.yaw + "," + p.prevYaw + "," + p.field_1012 + "," + p.field_1013 + "," + renderer.bodyYaw + "," + head + "," + camera);
              }
            }
            check(Arrays.equals(angles, new float[]{boat.yaw, boat.prevYaw, p.yaw, p.prevYaw, p.field_1012, p.field_1013}), "render changed boat/rider angles");
          }
          var report = new LinkedHashMap<String, Object>(); report.put("scenario", scenario); report.put("wraps", wraps); report.put("recenters", recenters);
          report.put("maxBoatFrameDegrees", maxBoat); report.put("maxBodyFrameDegrees", maxBody); report.put("maxHeadFrameDegrees", maxHead); report.put("maxCameraFrameDegrees", maxCamera); reports.add(report);
          if (steering && wraps == 0) errors.add(scenario + " never crossed a full-turn boundary");
          if (maxBoat > 5.1 || maxBody > 10 || maxHead > 10 || maxCamera > 5.1) errors.add(scenario + " frame jumps: " + report);
          p.method_1376(null); boat.markDead(); boat.method_1340(cx + 40, water + 8, cz + 40);
          log("BOAT473 " + report);
        }
      check(GL11.glGetError() == 0, "boat/player renderer OpenGL error");
    } finally {
      capture = false; if (p.field_1595 != null) p.method_1376(null);
      for (Entity boat : created) { boat.markDead(); boat.method_1340(cx + 40, water + 8, cz + 40); }
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[3], b[4]);
      Config.preview(building); GameplayConfig.preview(gameplay); p.field_161 = input; p.read(saved); CreativeVehicleChecks.mode(mc, mode);
      p.field_1012 = body; p.field_1013 = previousBody; mc.options.thirdPerson = third; mc.options.bobView = bob; frontField.setBoolean(null, front);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPopAttrib(); GL11.glMatrixMode(matrix);
      mc.setScreen(screen);
      Files.write(REPORT.resolve("frames.csv"), frames); Files.writeString(REPORT.resolve("summary.json"), local.luke.power.config.Catalog.JSON.toJson(Map.of("scenarios", reports, "errors", errors)));
    }
    check(Arrays.equals(config, Files.readAllBytes(PowerConfig.path())), "boat fixture changed saved settings");
    log("BOAT473 errors=" + errors.size());
    if (action.equals("native")) check(errors.isEmpty(), "boat turn failures: " + errors);
  }

  private static float wrap(float angle) { angle %= 360; if (angle < -180) angle += 360; if (angle >= 180) angle -= 360; return angle; }

  private static final class RiderRenderer extends PlayerEntityRenderer {
    float bodyYaw;
    @Override protected void method_824(PlayerEntity player, float age, float body, float delta) { bodyYaw = body; super.method_824(player, age, body, delta); }
    @Override protected boolean method_2027(String url, String fallback) { method_2026("/mob/char.png"); return true; }
    float headYaw() { return (float)Math.toDegrees(((BipedEntityModel)model).head.yaw); }
  }
}
