package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.util.*;
import local.luke.power.worldedit.WorldEditor;
import net.minecraft.class_66;
import net.minecraft.class_345;
import net.minecraft.class_430;
import net.minecraft.client.Minecraft;

final class WorldEditNavigationChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0; DeathChecks.alive(mc); mc.setScreen(null); mc.field_2807 = mc.player;
    var rules = AccessRules.of(mc.world.method_262()); boolean cheats = rules.allAllowed();
    var settings = WorldEditor.settings().copy();
    int distance = mc.options.viewDistance;
    try {
      rules.allAllowed(true);
      var enabled = settings.copy();  WorldEditor.preview(enabled);
      test("culling reads loaded terrain without generating chunks or canceling unloads", () -> {
        var cache = (local.luke.power.mixin.CullingChunkCacheAccessor)mc.world.method_259();
        var before = new HashMap<>(cache.power$loadedChunks());
        Object provider = Class.forName("dev.tr7zw.entityculling.Provider").getConstructor().newInstance();
        provider.getClass().getMethod("prepareChunk",int.class,int.class).invoke(provider,12345,12345);
        var read = provider.getClass().getMethod("isOpaqueFullCube",int.class,int.class,int.class);
        check(!(boolean)read.invoke(provider,12345*16,100,12345*16), "missing terrain treated as opaque");
        var block = new net.minecraft.block.entity.BlockEntity(); block.world = mc.world; block.x=12345*16; block.y=100; block.z=12345*16;
        Object mod = Class.forName("dev.tr7zw.entityculling.EntityCullingMod").getField("instance").get(null);
        Arrays.stream(mod.getClass().getMethods()).filter(m -> m.getName().equals("setupBox")).findFirst().orElseThrow().invoke(mod,block,null);
        check(before.equals(cache.power$loadedChunks()), "culling loaded or changed chunks");
        Field unloading = net.minecraft.class_326.class.getDeclaredField("field_1225"); unloading.setAccessible(true);
        var queued = (Set<Integer>)unloading.get(mc.world.method_259());
        int key = before.keySet().iterator().next(); var loaded = before.get(key); boolean alreadyQueued = queued.contains(key);
        try {
          queued.add(key); read.invoke(provider,loaded.field_962*16,100,loaded.field_963*16);
          check(queued.contains(key),"culling canceled a scheduled chunk unload");
        } finally { if(!alreadyQueued) queued.remove(key); }
      });
      Validation.backgroundChunkReads.set(0);
      test("through-wall navigation still lands beyond the wall in both directions", () -> {
        for(int direction:new int[]{1,-1}) {
          for(int step=0;step<=8;step++) {
            int x=step*direction;
            mc.world.method_154(x,99,48,1,0);
            for(int y=100;y<=105;y++) mc.world.method_154(x,y,48,0,0);
          }
          mc.world.method_154(4*direction,100,48,1,0);
          mc.world.method_154(4*direction,101,48,1,0);
          mc.player.method_1341(.5,100,48.5,-90*direction,0);
          var output=ChatChecks.submit(mc,"/thru");
          check(output.stream().anyMatch(t->t.contains("Moved to")),"through-wall jump failed: "+output);
          check(Math.abs(mc.player.x-(5*direction+.5))<.001
              && Math.abs(mc.player.boundingBox.minY-100)<.001,"through-wall jump stopped inside or before wall");
        }
      });
      test("jump reaches the top of pillars from their sides and top faces", () -> {
        for(int length:new int[]{4,32,96}) for(int height:new int[]{103,108,116}) {
          for(int x=0;x<=length;x++) for(int y=98;y<=120;y++) mc.world.method_154(x,y,16,0,0);
          for(int y=98;y<=height;y++) mc.world.method_154(length,y,16,1,0);
          for(int feet:new int[]{100,119}) {
            double eye=feet+1.62, target=feet==100?height+.5:height+1;
            float pitch=(float)-Math.toDegrees(Math.atan2(target-eye,length));
            mc.player.method_1341(.5,feet,16.5,-90,pitch);
            var output=ChatChecks.submit(mc,"/j");
            check(output.stream().anyMatch(t->t.contains("Moved to")),"pillar jump failed: "+output);
            check(Math.abs(mc.player.x-(length+.5))<.001
                && Math.abs(mc.player.boundingBox.minY-(height+1))<.001,"jump did not land on pillar top");
          }
        }
      });
      test("jump catches a top-block corner crossed between quarter-block samples", () -> {
        for(int x=0;x<=20;x++) for(int y=98;y<=108;y++) mc.world.method_154(x,y,32,0,0);
        for(int y=98;y<=102;y++) mc.world.method_154(8,y,32,1,0);
        float pitch=(float)Math.toDegrees(Math.atan(.02));
        mc.player.method_1341(.5,100,32.5,-90,pitch);
        var look=mc.player.method_926(1F);
        mc.player.method_1340(.5,103-look.y/look.x*(8.9999-.5),32.5);
        var start=mc.player.method_931(1F);
        var target=mc.world.method_160(start,start.add(look.x*20,look.y*20,look.z*20));
        check(target!=null&&target.field_1984==8&&target.field_1985==102,"fixture crosshair missed top block");
        var output=ChatChecks.submit(mc,"/jumpto");
        check(output.stream().anyMatch(t->t.contains("Moved to"))
            && Math.abs(mc.player.x-8.5)<.001&&Math.abs(mc.player.boundingBox.minY-103)<.001,
            "corner jump failed: "+output);
      });
      test("jump repeatedly crosses chunk boundaries and renders at short and long distances", () -> {
        try {
        for (int view : new int[]{3, 1}) {
          mc.options.viewDistance = view; mc.worldRenderer.method_1537();
          for (int offset : new int[]{-256, 0}) for (int length : new int[]{8, 33, 96, 192, 255}) {
            for (int x = offset; x <= offset + length; x++) {
              mc.world.method_154(x, 99, 0, 1, 0);
              for (int y = 100; y <= 105; y++) mc.world.method_154(x, y, 0, 0, 0);
            }
            mc.world.method_154(offset + length, 101, 0, 1, 0);
            for (int repeat = 0; repeat < 3; repeat++) {
              mc.player.method_1341(offset + .5, 100, .5, -90, 0);
              var output = ChatChecks.submit(mc, repeat % 2 == 0 ? "/j" : "/jumpto");
              check(output.stream().anyMatch(t -> t.contains("Moved to")), "jump failed: " + output);
              check(Math.abs(mc.player.x - (offset + length + .5)) < .001 && Math.abs(mc.player.boundingBox.minY - 102) < .001, "wrong landing position");
              mc.field_2818.method_1841(.5F, 0L);
              mc.world.method_193(mc.player, true); mc.player.tick();
            }
          }
        }
        } catch (Exception e) {
          var trace = new java.io.StringWriter(); e.printStackTrace(new java.io.PrintWriter(trace)); log(trace.toString()); throw e;
        }
      });
      check(Validation.backgroundChunkReads.get()==0,"culling called the world chunk loader during jumps");
      test("unloaded and obstructed targets fail without moving or loading terrain", () -> {
        mc.player.method_1341(.5, 125, .5, 0, -90);
        double x = mc.player.x, y = mc.player.y, z = mc.player.z;
        var output = ChatChecks.submit(mc, "/j");
        check(output.stream().anyMatch(t -> t.contains("No safe destination")), "missing safe cancellation");
        check(mc.player.x == x && mc.player.y == y && mc.player.z == z, "unsafe target moved player");
      });
      test("native chunk sorting keeps near-equal distances consistent", () -> {
        Field field = mc.worldRenderer.getClass().getDeclaredField("field_1808"); field.setAccessible(true);
        var chunks = (class_66[])field.get(mc.worldRenderer); var a = chunks[0]; var b = chunks[1]; var c = chunks[2];
        int[][] positions = {{a.field_245,a.field_246,a.field_247},{b.field_245,b.field_246,b.field_247},{c.field_245,c.field_246,c.field_247}};
        try {
          a.field_245=8; a.field_246=8; a.field_247=8;
          b.field_245=24; b.field_246=8; b.field_247=8;
          c.field_245=8; c.field_246=8; c.field_247=24;
          mc.player.method_1340(16.00002,8,16.00004);
          var comparator = new class_345(mc.player);
          var updates = new class_430(mc.player);
          check(updates.compare(a,a)==0 && Integer.signum(updates.compare(a,b))==-Integer.signum(updates.compare(b,a)),"update sorting violates equality or symmetry");
          check(!(comparator.compare(a,b)==0 && comparator.compare(b,c)==0 && comparator.compare(a,c)!=0), "native distance comparator violates equality transitivity");
          var random = new Random(143);
          for (int attempt=0; attempt<300; attempt++) {
            var list = new ArrayList<class_66>(); for(int i=0;i<1024;i++) list.add(chunks[random.nextInt(3)]);
            list.sort(comparator);
          }
        } finally {
          for(int i=0;i<3;i++) { chunks[i].field_245=positions[i][0]; chunks[i].field_246=positions[i][1]; chunks[i].field_247=positions[i][2]; }
        }
      });
    } finally {
      mc.options.viewDistance = distance; mc.worldRenderer.method_1537(); WorldEditor.leaveWorld(); WorldEditor.preview(settings);
      rules.allAllowed(cheats);  DeathChecks.alive(mc);
    }
    log("WORLDEDIT NAVIGATION FAILURES " + failures);
  }
}
