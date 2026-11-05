package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.*;
import local.luke.power.config.*;
import local.luke.power.ui.PowerOptionsScreen;
import org.lwjgl.opengl.GL11;
import java.util.*;
import local.luke.power.gameplay.*;
import net.minecraft.class_142;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.Display;

/** Native bucket use, replacement drops and authority boundaries. Disposable worlds only. */
final class Bucket475Checks {
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute disposable client");
    failures = 0;
    var settings = GameplayConfig.copy(); var main = mc.player.inventory.main;
    int selected = mc.player.inventory.selectedSlot;
    double px = mc.player.x, py = mc.player.y, pz = mc.player.z;
    float yaw = mc.player.yaw, pitch = mc.player.pitch;
    boolean hell = mc.world.dimension.field_2176;
    int x = (int)Math.floor(px) + 6, y = 112, z = (int)Math.floor(pz) + 6;
    int[][][] terrain = new int[5][5][5], data = new int[5][5][5];
    Set<Object> prior = new HashSet<>(mc.world.field_198);
    for (int dx=0; dx<5; dx++) for (int dy=0; dy<5; dy++) for (int dz=0; dz<5; dz++) {
      terrain[dx][dy][dz]=mc.world.getBlockId(x+dx-2,y+dy-1,z+dz-2);
      data[dx][dy][dz]=mc.world.method_1778(x+dx-2,y+dy-1,z+dz-2);
    }
    try {
      mc.player.inventory.main = new ItemStack[36]; mc.player.inventory.selectedSlot = 0;
      test("BUCKET475 native water/lava replacement drops exactly once, toggle off retains Beta", () -> {
        for (boolean fix : new boolean[]{false,true}) for (int bucket : new int[]{326,327})
            for (int block : new int[]{50,75,76,55,37,38,6}) {
          reset(mc,x,y,z); mc.world.method_154(x,y,z,block,block==50||block==75||block==76 ? 5 : 0);
          int drop = block==55 ? 331 : block==76 ? 76 : block==75 ? 76 : block;
          int before = drops(mc,drop);
          var choice = GameplayConfig.copy(); choice.bucketBlockDrops=fix; GameplayConfig.preview(choice);
          aim(mc,x,y,z); var held = new ItemStack(bucket,1,0); mc.player.inventory.main[0]=held;
          var returned = held.method_698(mc.world,mc.player);
          int fluid=bucket==326 ? 8 : 10;
          check(mc.world.getBlockId(x,y,z)==fluid || mc.world.getBlockId(x,y,z)==fluid+1,"native bucket target "+block);
          check(returned.itemId==325 && returned.count==1,"native empty bucket exchange");
          check(drops(mc,drop)-before==(fix ? 1 : 0),"drop mismatch fix="+fix+" block="+block+" bucket="+bucket);
        }
      });
      test("BUCKET475 air, existing water, solid obstruction and Nether evaporation never duplicate drops", () -> {
        var choice=GameplayConfig.copy(); choice.bucketBlockDrops=true; GameplayConfig.preview(choice);
        for (int block : new int[]{0,8,1,50}) {
          reset(mc,x,y,z); mc.world.method_154(x,y,z,block,block==50 ? 5 : 0);
          mc.world.dimension.field_2176=block==50;
          int before=drops(mc,50); aim(mc,x,y,z);
          var held=new ItemStack(326,1,0); mc.player.inventory.main[0]=held; held.method_698(mc.world,mc.player);
          check(drops(mc,50)==before,"unrelated or evaporated water dropped a torch");
          if(block==50) check(mc.world.getBlockId(x,y,z)==50,"evaporation deleted torch");
          if(block==1) check(mc.world.getBlockId(x,y,z)==1,"solid obstruction replaced");
        }
        mc.world.dimension.field_2176=hell;
      });
    } finally {
      mc.world.dimension.field_2176=hell;
      for(Object entity : new ArrayList<>(mc.world.field_198)) if(!prior.contains(entity) && entity instanceof class_142 drop) drop.dead=true;
      reset(mc,x,y,z);
      for (int dx=0; dx<5; dx++) for (int dy=0; dy<5; dy++) for (int dz=0; dz<5; dz++)
        mc.world.method_154(x+dx-2,y+dy-1,z+dz-2,terrain[dx][dy][dz],data[dx][dy][dz]);
      GameplayConfig.preview(settings); mc.player.inventory.main=main; mc.player.inventory.selectedSlot=selected;
      mc.player.method_1340(px,py,pz); mc.player.prevX=px; mc.player.prevY=py; mc.player.prevZ=pz;
      mc.player.yaw=mc.player.prevYaw=yaw; mc.player.pitch=mc.player.prevPitch=pitch;
    }
    log("BUCKET475 failures="+failures); check(failures==0,"bucket drop failures");
  }
  private static void aim(Minecraft mc,int x,int y,int z) {
    // Hit a backing block sideways above the replaceable block's outline.
    mc.world.method_154(x-1,y,z,1,0);
    mc.player.method_1340(x+3,y+.8,z+.95);
    mc.player.prevX=mc.player.x; mc.player.prevY=mc.player.y; mc.player.prevZ=mc.player.z;
    mc.player.yaw=mc.player.prevYaw=90; mc.player.pitch=mc.player.prevPitch=0;
  }
  private static int drops(Minecraft mc,int id) {
    int count=0;
    for(Object entity : mc.world.field_198) if(entity instanceof class_142 drop && !drop.dead && drop.field_564.itemId==id) count+=drop.field_564.count;
    return count;
  }
  private static void reset(Minecraft mc,int x,int y,int z) {
    for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) {
      for(int dy=0;dy<=3;dy++) mc.world.method_154(x+dx,y+dy,z+dz,0,0);
      mc.world.method_154(x+dx,y-1,z+dz,1,0);
    }
  }
  static void capture(Minecraft mc) throws Exception {
    var previous = mc.currentScreen; var screen = new PowerOptionsScreen(previous); screen.init(mc, 854, 480);
    Setting setting = screen.session().settings().stream().filter(s -> s.id.equals("gameplay.bucketBlockDrops")).findFirst().orElseThrow();
    field(screen, "page", "Fixes"); ((Set<?>)field(screen, "collapsed")).clear(); field(screen, "scroll", 0d); call(screen, "layout");
    for (Object row : (List<?>)field(screen, "rows")) if (call(row, "setting") == setting) { field(screen, "scroll", Math.max(0, ((Number)call(row, "y")).doubleValue() - 28)); break; }
    call(screen, "layout"); Path report = Path.of("power-beta-data/reports/bucket-475"); Files.createDirectories(report);
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      SettingsSnapshot.render(mc, screen, 854, 480, report.resolve("setting-854.png"));
      screen.init(mc, 320, 240); call(screen, "layout");
      for (Object row : (List<?>)field(screen, "rows")) if (call(row, "setting") == setting) {
        field(screen, "scroll", Math.max(0, ((Number)call(row, "y")).doubleValue() - 28)); break;
      }
      call(screen, "layout");
      SettingsSnapshot.render(mc, screen, 320, 240, report.resolve("setting-320.png"));
      screen.init(mc, 854, 480); call(screen, "layout");
      for (Object row : (List<?>)field(screen, "rows")) if (call(row, "setting") == setting) {
        field(screen, "scroll", Math.max(0, ((Number)call(row, "y")).doubleValue() - 28)); break;
      }
      call(screen, "layout");
      for (Object row : (List<?>)field(screen, "rows")) if (call(row, "setting") == setting) {
        int rowY = (int)call(screen, "top") + ((Number)call(row, "y")).intValue()
            - ((Number)field(screen, "scroll")).intValue();
        SettingsSnapshot.render(mc, screen, 854, 480, report.resolve("setting-help-854.png"),
            (int)call(screen, "left") + 5, rowY + 5);
      }
    }
    finally { GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPopAttrib(); GL11.glMatrixMode(mode); mc.setScreen(previous); }
    for (var base : DefaultBases.Base.values()) check(DefaultBases.value(base, setting.id, setting.powerBetaDefault).getAsBoolean() == (base != DefaultBases.Base.BETA), "bucket drop default for " + base);
    log("BUCKET475 settings/defaults capture PASS");
  }
}
