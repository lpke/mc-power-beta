package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.gameplay.*;
import net.minecraft.class_365;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;

final class TntPerformanceChecks {
  static void run(Minecraft mc,String action) throws Exception {
    var original=GameplayConfig.copy();
    if(action.equals("reload")) {
      check(GameplayConfig.current().limitTnt,"recovery toggle did not load");
      int count=count(mc);check(count>0,"pending TNT missing after reload");
      long start=System.nanoTime();mc.world.method_227();
      check(count(mc)>=count-4,"unbounded TNT after reload");
      log("PASS TNT recovery after restart pending="+count+" tickMs="+(System.nanoTime()-start)/1e6);return;
    }
    (AccessRules.of(mc.world.method_262())).allAllowed(true);CreativeVehicleChecks.mode(mc,"CREATIVE");
    mc.setScreen(null);mc.player.method_1340(0,115,0);
    for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)mc.world.method_214(x,z);
    List<class_365> created=new ArrayList<>();
    if(action.equals("save") || action.equals("save-disabled")) {
      var s=GameplayConfig.copy();s.limitTnt=action.equals("save");s.tntPerTick=4;GameplayConfig.save(s);
      for(int i=0;i<2048;i++) created.add(tnt(mc,i,0));
      mc.setScreen(new local.luke.power.ui.PowerOptionsScreen(null));
      log("PASS staged 2048 primed TNT for recovery restart");return;
    }
    try {
      for(boolean enabled:new boolean[]{false,true}) {
        var s=GameplayConfig.copy();s.limitTnt=enabled;s.tntPerTick=4;s.tntMillis=8;GameplayConfig.preview(s);
        for(int i=0;i<128;i++) created.add(tnt(mc,i,0));
        long start=System.nanoTime();mc.world.method_227();double elapsed=(System.nanoTime()-start)/1e6;
        long dead=created.stream().filter(e->e.dead).count();
        check(enabled ? dead>0 && dead<=4 : dead==128,"detonation limit wrong enabled="+enabled+" dead="+dead);
        log("PASS TNT benchmark enabled="+enabled+" entities=128 detonated="+dead+" tickMs="+elapsed);
        created.forEach(Entity::markDead);mc.world.method_227();created.clear();
      }
      var s=GameplayConfig.copy();s.limitTnt=true;GameplayConfig.preview(s);
      for(int i=0;i<2048;i++)created.add(tnt(mc,i,80));
      int sum=created.stream().mapToInt(e->e.field_1390).sum();
      long start=System.nanoTime();mc.world.method_227();double elapsed=(System.nanoTime()-start)/1e6;
      int advanced=sum-created.stream().mapToInt(e->e.field_1390).sum();
      check(advanced>0 && advanced<=128,"simulation was not bounded");
      for(var e:created) {var tag=new NbtCompound();e.write(tag);var copy=new class_365(mc.world);copy.read(tag);check(copy.field_1390==e.field_1390,"queued fuse not saved");}
      log("PASS TNT simulation entities=2048 advanced="+advanced+" tickMs="+elapsed+" all pending fuses roundtrip");
    } finally {created.forEach(Entity::markDead);GameplayConfig.preview(original);}
  }
  private static class_365 tnt(Minecraft mc,int i,int fuse) {
    var t=new class_365(mc.world,2+i%16,105+(i/256)%8,2+(i/16)%16);t.field_1390=fuse;
    t.velocityX=t.velocityY=t.velocityZ=0;mc.world.method_210(t);return t;
  }
  private static int count(Minecraft mc) {return (int)mc.world.method_291().stream().filter(e->e instanceof class_365 t && !t.dead).count();}
}
