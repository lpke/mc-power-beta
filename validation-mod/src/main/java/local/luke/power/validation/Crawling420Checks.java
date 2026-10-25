package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.Files;
import java.util.*;
import local.luke.power.gameplay.*;
import local.luke.power.rules.*;
import local.luke.power.sprint.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.class_41;
import net.minecraft.class_413;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.opengl.*;

/** Native dry-pose, movement, collision, saved-position and model checks. */
final class Crawling420Checks {
  private static void near(double actual, double expected, double tolerance, String label) {
    check(Math.abs(actual - expected) <= tolerance, label + ": " + actual + " expected " + expected);
  }

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive(), "focus a muted disposable singleplayer world");
    failures = 0;
    var player = mc.player;
    var saved = new NbtCompound(); player.write(saved);
    var gameplay = GameplayConfig.copy(); var sprint = SprintConfig.copy(); var rules = AccessRules.copy(mc);
    var input = player.field_161; var screen = mc.currentScreen;
    boolean paused = mc.paused;
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(player).toString();
    byte[] disk = Files.readAllBytes(PowerConfig.path());
    int x = ((int)Math.floor(player.x) >> 4) * 16 + 8, z = ((int)Math.floor(player.z) >> 4) * 16 + 8, y = 112;
    var blocks = new ArrayList<int[]>();
    try {
      check(mc.world.method_155(x - 6, 0, z - 6, x + 6, 127, z + 29), "crawl scene requires already loaded neighboring chunks");
      for (int bx = x - 2; bx <= x + 2; bx++) for (int bz = z - 2; bz <= z + 25; bz++) for (int by = y - 1; by <= y + 3; by++)
        blocks.add(new int[]{bx, by, bz, mc.world.getBlockId(bx, by, bz), mc.world.method_1778(bx, by, bz)});
      mc.setScreen(null); mc.paused = false; AccessRules.restore(mc, RuleSet.cheats()); CreativeVehicleChecks.mode(mc, "SURVIVAL");
      var speeds = SprintConfig.copy(); speeds.stamina = false; speeds.noSprintInCombat = false; SprintConfig.preview(speeds);
      floor(mc, x, y, z);
      test("native trapdoor crawling is independent of modern water and sprint permission", () -> {
        for (boolean water : new boolean[]{false, true}) for (boolean sprinting : new boolean[]{false, true}) {
          settings(true, water); permission(mc, sprinting); reset(mc, x, y, z);
          mc.world.method_154(x, y + 1, z, 96, 4);
          ModernSwimming.tick(player); check(!ModernSwimming.lowPose(player), "open trapdoor started crawling");
          mc.world.method_154(x, y + 1, z, 96, 0);
          step(mc, 0, false);
          check(ModernSwimming.compact(player) && !ModernSwimming.swimming(player), "closed trapdoor did not start dry crawl");
          check(!((SprintPlayer)player).power$sprint().sprinting, "crawl retained ground sprint");
          near(player.spacingY, .6, 1e-6, "crawl body"); near(player.field_1631, .4, 1e-6, "crawl eye");
          near(player.boundingBox.minY, y, 1e-6, "trapdoor entry moved feet");
          mc.world.method_154(x, y + 1, z, 0, 0);
        }
      });
      test("overhead trapdoors retain standing clearance across ladder positions", () -> {
        settings(true, false); permission(mc, false);
        for (int direction = 0; direction < 4; direction++) for (double offset : new double[]{.31, .5, .69}) {
          floor(mc, x, y, z); reset(mc, x, y, z);
          mc.world.method_154(x, y, z, 65, 5);
          mc.world.method_154(x, y + 1, z, 65, 5);
          mc.world.method_154(x, y + 2, z, 96, direction | 4);
          player.method_1341(x + offset, y, z + .5, 0, 0);
          ModernSwimming.tick(player);
          mc.world.method_154(x, y + 2, z, 96, direction);
          ModernSwimming.tick(player);
          check(!ModernSwimming.lowPose(player), "clear closed trapdoor forced a low pose");
          near(player.boundingBox.minY, y, 1e-6, "overhead trapdoor moved feet");
        }
        floor(mc, x, y, z);
      });
      test("forced crouching on a ladder allows descent but real Sneak still holds", () -> {
        for (boolean sneak : new boolean[]{false, true}) {
          floor(mc, x, y, z); settings(true, false); reset(mc, x, y, z);
          mc.world.method_154(x, y, z, 65, 5);
          mc.world.method_154(x, y + 1, z, 65, 5);
          mc.world.method_154(x, y + 2, z, 96, 0);
          player.method_1341(x + .5, y + .4, z + .5, 0, 0);
          player.field_161.field_2536 = sneak;
          ModernSwimming.tick(player);
          check(ModernSwimming.crouched(player) && player.method_932(), "fixture did not crouch on a ladder");
          double feet = player.boundingBox.minY;
          player.velocityY = -.1; player.field_1623 = false; player.method_945(0, 0);
          near(player.boundingBox.minY, feet - (sneak ? 0 : .1), 1e-6, "ladder descent follows actual Sneak input");
          if (!sneak) {
            for (int i = 0; i < 8; i++) { ModernSwimming.tick(player); player.method_945(0, 0); }
            ModernSwimming.tick(player);
            check(!ModernSwimming.lowPose(player), "ladder descent retained crouch after clearing the trapdoor");
          }
        }
        floor(mc, x, y, z);
      });
      test("the independent off preference and solid lower-body obstruction cannot initiate crawling", () -> {
        settings(false, true); permission(mc, true); reset(mc, x, y, z);
        mc.world.method_154(x, y + 1, z, 96, 0); ModernSwimming.tick(player);
        check(!ModernSwimming.lowPose(player), "disabled crawling shrank the player");
        settings(true, false); mc.world.method_154(x, y, z, 1, 0); ModernSwimming.tick(player);
        check(!ModernSwimming.lowPose(player), "solid lower-body collision manufactured a crawl");
        mc.world.method_154(x, y, z, 0, 0); mc.world.method_154(x, y + 1, z, 0, 0);
      });
      test("native dry crawling travels at the 0.3 input speed and retains walls", () -> {
        settings(true, false); permission(mc, false); reset(mc, x, y, z);
        for (int bz = z - 1; bz <= z + 24; bz++) {
          mc.world.method_154(x, y + 1, bz, 1, 0);
          mc.world.method_154(x - 1, y, bz, 1, 0); mc.world.method_154(x + 1, y, bz, 1, 0);
        }
        double velocity = 0, distance = 0, start = player.z;
        float friction = .6f * .91f;
        float acceleration = .1f * (.16277136f / (friction * friction * friction));
        for (int tick = 0; tick < 40; tick++) {
          velocity += acceleration * (.3f * .98f); distance += velocity; velocity *= friction;
          step(mc, 1, false);
        }
        near(player.z - start, distance, 3e-5, "native crawl displacement");
        near(player.velocityZ, velocity, 2e-6, "native crawl velocity");
        check(player.z > start + 2 && player.boundingBox.maxY < y + 1, "crawl failed to traverse the tunnel");
        player.move(1, 0, 0); check(player.x <= x + .70001, "crawl bypassed the native wall");
        floor(mc, x, y, z);
      });
      test("keyboard sneak and synthetic input apply the dry speed exactly once", () -> {
        settings(true, false); reset(mc, x, y, z); mc.world.method_154(x, y + 1, z, 1, 0);
        player.field_161 = new class_413(mc.options); player.field_161.field_2536 = true; player.field_161.field_2533 = .3f;
        ModernSwimming.preparePose(player); ModernSwimming.prepareInput(player);
        near(player.field_161.field_2533, .3, 1e-6, "keyboard crawl was scaled twice"); ModernSwimming.tick(player);
        player.field_161 = new class_41(); player.field_161.field_2536 = true; player.field_161.field_2533 = 1;
        ModernSwimming.preparePose(player); ModernSwimming.prepareInput(player);
        near(player.field_161.field_2533, .3, 1e-6, "synthetic crawl bypassed slow input"); ModernSwimming.tick(player);
        floor(mc, x, y, z);
      });
      test("saved crawl, live disable, 1.5 crouch and clear standing exits preserve native feet", () -> {
        settings(true, false); reset(mc, x, y, z); mc.world.method_154(x, y + 1, z, 1, 0); step(mc, 0, false);
        var tag = new NbtCompound(); player.write(tag); player.read(tag);
        check(ModernSwimming.compact(player) && !ModernSwimming.swimming(player), "reload lost safe dry pose");
        near(player.boundingBox.minY, y, 1e-6, "reload moved feet");
        settings(false, false); ModernSwimming.tick(player); check(ModernSwimming.compact(player), "disable stood into a roof");
        mc.world.method_154(x, y + 1, z, 0, 0); mc.world.method_154(x, y + 2, z, 1, 0);
        player.method_1341(x + .5, y + .5, z + .5, 0, 0); ModernSwimming.tick(player);
        check(ModernSwimming.crouched(player) && !ModernSwimming.compact(player), "1.5 gap failed to select crouch");
        near(player.field_1631, 1.27, 1e-6, "crouched eye"); near(player.boundingBox.minY, y + .5, 1e-6, "crouch moved feet");
        mc.world.method_154(x, y + 2, z, 0, 0); ModernSwimming.tick(player);
        check(!ModernSwimming.lowPose(player), "clear exit retained low pose"); near(player.spacingY, 1.8, 1e-6, "standing body");
      });
      test("default crouch camera matches native Beta through presses, releases and frame interpolation", () -> {
        boolean third=mc.options.thirdPerson;
        try {
          for(boolean perspective:new boolean[]{false,true}) {
            mc.options.thirdPerson=perspective;
            double[][] nativeHeights=new double[72][3];
            for(int variant=0;variant<3;variant++) {
              boolean crawling=variant==2;
              floor(mc,x,y,z);settings(crawling,false);reset(mc,x,y,z);
              var options=GameplayConfig.copy();options.crouchCameraHeight=variant==1;GameplayConfig.preview(options);
              for(int tick=0;tick<72;tick++) {
                boolean held=tick%24<12;
                step(mc,0,held);
                near(player.boundingBox.minY,y,1e-6,"native-height crouch moved feet");
                for(int frame=0;frame<3;frame++) {
                  double height=cameraY(mc,frame*.5f);
                  if(variant==0)nativeHeights[tick][frame]=height;
                  else near(height,nativeHeights[tick][frame],2e-4,"Beta camera trajectory tick "+tick+" frame "+frame);
                }
                if(tick%24==11)near(cameraY(mc),y+1.54,2e-4,"Beta's settled crouch height");
                if(tick==11)log("CROUCH CAMERA "+(perspective?"third":"first")+" crawling="+crawling
                    +" option="+options.crouchCameraHeight+" height="+(cameraY(mc)-y));
              }
            }
          }
        } finally {mc.options.thirdPerson=third;floor(mc,x,y,z);}
      });
      test("crouching saves native-normalized positions and retains collision feet after reload", () -> {
        for(boolean enabled:new boolean[]{false,true}) {
          floor(mc,x,y,z);settings(true,false);reset(mc,x,y,z);
          var options=GameplayConfig.copy();options.crouchCameraHeight=enabled;GameplayConfig.preview(options);
          for(int tick=0;tick<25;tick++)step(mc,0,true);
          var tag=new NbtCompound();player.write(tag);
          near(((net.minecraft.nbt.NbtDouble)tag.getList("Pos").get(1)).value,y+1.62,1e-6,"saved native eye origin");
          player.read(tag);
          near(player.boundingBox.minY,y,1e-6,"crouch reload moved feet");
          for(int tick=0;tick<12;tick++) {
            step(mc,0,tick<6);
            near(player.boundingBox.minY,y,1e-6,"reloaded crouch moved feet tick "+tick);
          }
        }
      });
      test("deeper crouch camera is optional and both perspectives retain safe low poses", () -> {
        boolean third=mc.options.thirdPerson;
        try {
          for(boolean enabled:new boolean[]{false,true}) for(boolean perspective:new boolean[]{false,true}) {
            floor(mc,x,y,z); settings(true,false); reset(mc,x,y,z);
            var options=GameplayConfig.copy();options.crouchCameraHeight=enabled;GameplayConfig.preview(options);
            mc.options.thirdPerson=perspective;
            for(int tick=0;tick<25;tick++)step(mc,0,true);
            var state=((SwimPlayer)player).power$swim();
            check(state.crouched&&!state.compact,"sneak lost physical crouch");
            near(player.boundingBox.minY,y,1e-6,"sneak moved feet");
            near(state.cameraEye(1),enabled?1.27:1.62,1e-5,"camera preference");
            near(cameraY(mc),y+(enabled?1.27:1.54),2e-4,"native perspective height");
            log("CROUCH CAMERA "+(perspective?"third":"first")+" option="+enabled+" height="+(cameraY(mc)-y));
            // Changing only the camera preference must not change collision or saved feet.
            options.crouchCameraHeight=!enabled;GameplayConfig.preview(options);
            for(int tick=0;tick<25;tick++)step(mc,0,true);
            near(cameraY(mc),y+(!enabled?1.27:1.54),2e-4,"live preference change");
            near(player.boundingBox.minY,y,1e-6,"live preference moved feet");
          }
          var options=GameplayConfig.copy();options.crouchCameraHeight=false;GameplayConfig.preview(options);
          mc.world.method_154(x,y+1,z,1,0);
          for(int tick=0;tick<25;tick++)step(mc,0,false);
          near(((SwimPlayer)player).power$swim().cameraEye(1),.4,1e-5,"disabled sneak camera blocked crawling camera");
        } finally {mc.options.thirdPerson=third;floor(mc,x,y,z);}
      });
      test("dry models and armor use the modern stroke without leaking it to another render", () -> {
        settings(true, false); reset(mc, x, y, z); mc.world.method_154(x, y + 1, z, 1, 0);
        for (int i = 0; i < 12; i++) step(mc, 0, false);
        check(!player.method_1334() && ModernSwimming.amount(player, 1) == 1, "dry animation never reached its pose");
        var renderer = new PlayerEntityRenderer() {
          @Override protected boolean method_2027(String url, String fallback) { method_2026("/mob/char.png"); return true; }
        };
        renderer.setDispatcher(EntityRenderDispatcher.field_2489);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
        try { renderer.render(player, 0, 0, -5, 0, 1); }
        finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
        for (var field : PlayerEntityRenderer.class.getDeclaredFields()) if (field.getType() == BipedEntityModel.class) {
          field.setAccessible(true); var model = (BipedEntityModel)field.get(renderer);
          model.setAngles(0, 0, 0, 0, 30, .0625f);
          near(model.head.pitch, Math.PI / 6, 1e-6, "renderer leaked dry head pose");
        }
        var model = new BipedEntityModel(); ((SwimmingModel)model).power$swimPose(1, true);
        model.setAngles(22, 1, 0, 0, 30, .0625f);
        near(model.leftArm.pitch, Math.PI / 2, 1e-6, "dry stroke arm");
        near(model.head.pitch, -Math.PI / 4, 1e-6, "dry stroke head");
        check(GL11.glGetError() == GL11.GL_NO_ERROR, "crawl renderer left a GL error");
        floor(mc, x, y, z);
      });
      test("flight, multiplayer, other players and unloaded terrain cannot initiate crawling", () -> {
        settings(true, false); reset(mc, x, y, z); mc.world.method_154(x, y + 1, z, 1, 0);
        mc.world.isRemote = true;
        try { ModernSwimming.tick(player); check(!ModernSwimming.lowPose(player), "multiplayer initiated local crawling"); }
        finally { mc.world.isRemote = false; }
        CreativeVehicleChecks.mode(mc, "SPECTATOR"); ModernSwimming.tick(player);
        check(!ModernSwimming.lowPose(player), "Spectator initiated crawling"); CreativeVehicleChecks.mode(mc, "SURVIVAL");
        var other = new net.minecraft.entity.player.ClientPlayerEntity(mc, mc.world, mc.session, player.dimensionId);
        other.method_1341(x + .5, y, z + .5, 0, 0); ModernSwimming.tick(other);
        check(!ModernSwimming.lowPose(other), "another player initiated local crawling");
        reset(mc, x, y, z); player.method_1340(31_999_900, y, 31_999_900);
        Release252Checks.chunkRequests = 0; Release252Checks.checkingUnloaded = true;
        try { ModernSwimming.tick(player); }
        finally { Release252Checks.checkingUnloaded = false; }
        check(Release252Checks.chunkRequests == 0 && !ModernSwimming.lowPose(player), "pose check loaded terrain or shrank at an unknown boundary");
        floor(mc,x,y,z);reset(mc,x,y,z);mc.world.method_154(x,y+2,z,1,0);
        player.method_1341(x+.5,y+.5,z+.5,0,0);ModernSwimming.tick(player);
        var state=((SwimPlayer)player).power$swim();
        check(state.crouched&&state.lowCeiling,"low-ceiling fixture did not crouch");state.resetCamera(1.27f);
        player.method_1340(31_999_900,y,31_999_900);
        Release252Checks.chunkRequests=0;Release252Checks.checkingUnloaded=true;
        try {ModernSwimming.tick(player);}
        finally {Release252Checks.checkingUnloaded=false;}
        check(Release252Checks.chunkRequests==0&&state.crouched&&state.lowCeiling,"unknown terrain discarded the safe low camera");
        near(state.cameraEye(1),1.27,1e-6,"unknown terrain raised a blocked camera");
      });
      test("crawl previews and movement do not write global preferences", () -> check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "crawl wrote preferences"));
    } finally {
      mc.world.isRemote = false; Release252Checks.checkingUnloaded = false;
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[3], b[4]);
      AccessRules.restore(mc, RuleSet.cheats()); CreativeVehicleChecks.mode(mc, mode);
      GameplayConfig.preview(gameplay); SprintConfig.preview(sprint); AccessRules.restore(mc, rules);
      player.read(saved); player.field_161 = input; mc.paused = paused; mc.setScreen(screen); SprintController.reset();
    }
    log("CRAWLING 420 FAILURES " + failures);
  }

  private static double cameraY(Minecraft mc) throws Exception {return cameraY(mc,1f);}

  private static double cameraY(Minecraft mc,float delta) throws Exception {
    GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
    try {
      GL11.glLoadIdentity();var method=net.minecraft.class_555.class.getDeclaredMethod("method_1851",float.class);
      method.setAccessible(true);method.invoke(mc.field_2818,delta);
      var p=mc.player;
      GL11.glTranslated(-(p.prevX+(p.x-p.prevX)*delta),-(p.prevY+(p.y-p.prevY)*delta),-(p.prevZ+(p.z-p.prevZ)*delta));
      var matrix=org.lwjgl.BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,matrix);
      return -(matrix.get(4)*matrix.get(12)+matrix.get(5)*matrix.get(13)+matrix.get(6)*matrix.get(14));
    } finally {GL11.glPopMatrix();}
  }

  private static void settings(boolean crawl, boolean water) {
    var settings = GameplayConfig.copy(); settings.modernCrawling = crawl; settings.modernWaterMovement = water; GameplayConfig.preview(settings);
  }
  private static void permission(Minecraft mc, boolean enabled) {
    var rules = AccessRules.copy(mc); rules.set(GameRule.ALLOW_SPRINTING, enabled); AccessRules.restore(mc, rules);
  }
  private static void floor(Minecraft mc, int x, int y, int z) {
    for (int bx = x - 2; bx <= x + 2; bx++) for (int bz = z - 2; bz <= z + 25; bz++) for (int by = y - 1; by <= y + 3; by++)
      mc.world.method_154(bx, by, bz, by == y - 1 ? 1 : 0, 0);
  }
  private static void reset(Minecraft mc, int x, int y, int z) {
    var p = mc.player; var state = ((SwimPlayer)p).power$swim();
    state.swimming = state.compact = state.crouched = false; state.amount = state.previousAmount = 0; state.resetCamera(1.62f);
    p.spacingXZ = .6f; p.spacingY = 1.8f; p.field_1631 = 1.62f; p.field_1640 = 0;
    p.method_1341(x + .5, y, z + .5, 0, 0); p.prevX = p.x; p.prevY = p.y; p.prevZ = p.z;
    p.velocityX = p.velocityZ = 0; p.velocityY = -.08 * (double).98f; p.field_1623 = true;
    p.field_161 = new class_41(); ((SprintPlayer)p).power$sprint().cancel(false); SprintController.reset();
  }
  private static void step(Minecraft mc, float forward, boolean sneak) {
    mc.player.field_161.field_2532 = 0; mc.player.field_161.field_2533 = forward;
    mc.player.field_161.field_2536 = sneak; mc.player.field_161.field_2535 = false; mc.player.tick();
  }
}
