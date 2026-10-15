package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.*;
import java.util.*;
import local.luke.power.rules.*;
import local.luke.power.world.*;
import net.minecraft.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/** Native behavior, separate from registry and UI tests. Uses a disposable world. */
final class RuleBehaviorChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    var rules = AccessRules.copy(mc);
    String mode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    int difficulty = ((WorldDifficulty)mc.world.method_262()).power$difficulty();
    int health = mc.player.health;
    double x = mc.player.x, y = mc.player.y, z = mc.player.z;
    var entities = new HashSet<>(mc.world.method_291());
    try {
      mc.setScreen(null); DeathChecks.alive(mc); AccessRules.restore(mc, new RuleSet());
      CreativeVehicleChecks.mode(mc, "SURVIVAL");
      for (int bx=38;bx<=46;bx++) for(int bz=38;bz<=46;bz++) {
        mc.world.method_200(bx,108,bz,1);
        for(int by=109;by<=116;by++) mc.world.method_200(bx,by,bz,0);
      }
      mc.player.method_1340(42.5,110.62,42.5);
      test("player fall damage is live and leaves mob falls unchanged", () -> {
        var fall = LivingEntity.class.getDeclaredMethod("method_1389",float.class); fall.setAccessible(true);
        for(boolean enabled:new boolean[]{false,true}) {
          AccessRules.set(mc,GameRule.FALL_DAMAGE,enabled); reset(mc.player);
          fall.invoke(mc.player,7F); check(mc.player.health==(enabled?16:20),"player fall rule "+enabled+": "+mc.player.health);
          var pig=new class_443(mc.world);reset(pig);fall.invoke(pig,7F);
          check(pig.health==16,"player rule protected a mob");
        }
      });
      test("fire rule covers burning and lava while lightning and mob damage remain native", () -> {
        var lava=Entity.class.getDeclaredMethod("method_1332");lava.setAccessible(true);
        var lightning=Entity.class.getDeclaredMethod("method_1392",int.class);lightning.setAccessible(true);
        for(boolean enabled:new boolean[]{false,true}) {
          AccessRules.set(mc,GameRule.FIRE_DAMAGE,enabled);reset(mc.player);mc.player.fire=20;mc.player.baseTick();
          check(mc.player.health==(enabled?19:20),"burning rule "+enabled);
          reset(mc.player);lava.invoke(mc.player);check(mc.player.health==(enabled?16:20),"lava rule "+enabled);
          reset(mc.player);lightning.invoke(mc.player,5);check(mc.player.health==15,"lightning impact suppressed");
          var pig=new class_443(mc.world);reset(pig);lava.invoke(pig);check(pig.health==16,"mob lava suppressed");
        }
      });
      test("drowning rule changes player health only and keeps the native oxygen cycle", () -> {
        for(int bx=41;bx<=43;bx++)for(int bz=41;bz<=43;bz++)for(int by=109;by<=113;by++)mc.world.method_154(bx,by,bz,9,0);
        try {
          for(boolean enabled:new boolean[]{false,true}) {
            AccessRules.set(mc,GameRule.DROWNING_DAMAGE,enabled); reset(mc.player);mc.player.air=-19;mc.player.baseTick();
            check(mc.player.health==(enabled?18:20)&&mc.player.air==0,"drowning rule "+enabled+": "+mc.player.health+" / "+mc.player.air);
            var pig=new class_443(mc.world);pig.method_1340(42.5,110,42.5);reset(pig);pig.air=-19;pig.baseTick();
            check(pig.health==18,"mob drowning suppressed");
          }
        } finally { for(int bx=41;bx<=43;bx++)for(int bz=41;bz<=43;bz++)for(int by=109;by<=113;by++)mc.world.method_200(bx,by,bz,0); }
      });
      test("Peaceful regeneration is independently switchable and direct healing stays available", () -> {
        ((WorldDifficulty)mc.world.method_262()).power$difficulty(0);mc.world.field_213=0;
        for(boolean enabled:new boolean[]{false,true}) {
          AccessRules.set(mc,GameRule.PEACEFUL_HEALTH_REGENERATION,enabled);
          reset(mc.player);mc.player.health=10;mc.player.field_1645=0;mc.player.method_937();
          check(mc.player.health==(enabled?11:10),"Peaceful rule "+enabled+": "+mc.player.health);
          mc.player.method_939(3);check(mc.player.health==(enabled?14:13),"direct healing blocked");
        }
      });
      test("TNT pauses without losing position, fuse or native saved data, then resumes", () -> {
        var tnt=new class_365(mc.world,44,112,44);tnt.field_1390=43;
        AccessRules.set(mc,GameRule.TNT_EXPLODES,false);
        double tx=tnt.x,ty=tnt.y,tz=tnt.z;
        for(int i=0;i<250;i++)tnt.tick();
        check(!tnt.dead&&tnt.field_1390==43&&tnt.x==tx&&tnt.y==ty&&tnt.z==tz,"paused TNT changed");
        var tag=new NbtCompound();tnt.write(tag);var loaded=new class_365(mc.world);loaded.read(tag);
        check(loaded.field_1390==43,"saved fuse lost");
        AccessRules.set(mc,GameRule.TNT_EXPLODES,true);loaded.tick();
        check(loaded.field_1390==42&&!loaded.dead,"paused TNT did not resume");
      });
      test("TNT-off blocks flint, break ignition and explosion chains without deleting TNT blocks", () -> {
        AccessRules.set(mc,GameRule.TNT_EXPLODES,false);
        int before=tntCount(mc);mc.world.method_200(44,109,44,Block.field_1887.id);
        var held=mc.player.inventory.main[mc.player.inventory.selectedSlot];
        try {
          mc.player.inventory.main[mc.player.inventory.selectedSlot]=new ItemStack(Item.field_472,1);
          Block.field_1887.method_1608(mc.world,44,109,44,mc.player);
          Block.field_1887.method_1612(mc.world,44,109,44,1);
          Block.field_1887.method_1613(mc.world,44,109,44);
          var blast=new class_60(mc.world,null,44.5,109.5,44.5,2);
          blast.field_1397.add(new class_339(44,109,44));blast.method_1196(false);
          check(tntCount(mc)==before&&mc.world.getBlockId(44,109,44)==Block.field_1887.id,"disabled TNT primed or vanished");
        } finally {mc.player.inventory.main[mc.player.inventory.selectedSlot]=held;mc.world.method_200(44,109,44,0);}
      });
      test("dropped-item explosion protection covers TNT, creepers and fireballs and preserves native data", () -> {
        AccessRules.set(mc,GameRule.TNT_EXPLODES,true);
        for(int source=0;source<3;source++) for(boolean damage:new boolean[]{false,true}) {
          AccessRules.set(mc,GameRule.EXPLOSIONS_DESTROY_ITEMS,damage);
          var stack=DeathChecks.tagged(4,31,2,"explosion-data");String encoded=local.luke.power.death.DeathItems.encode(stack);
          var item=new class_142(mc.world,44.5,112,44.5,stack);mc.world.method_210(item);
          Entity attacker=source==0?new class_365(mc.world):source==1?new class_114(mc.world):null;
          var blast=source==2?ExplosionSource.fireball(()->new class_60(mc.world,null,44.5,112,44.5,2))
              :new class_60(mc.world,attacker,44.5,112,44.5,2);
          blast.method_1195();
          check(item.dead==damage,"item explosion source "+source+" / "+damage);
          check(encoded.equals(local.luke.power.death.DeathItems.encode(item.field_564)),"item data changed");item.markDead();
        }
      });
      test("creeper and ghast block rules are independent of TNT block destruction", () -> {
        for(int source=0;source<3;source++)for(boolean grief:new boolean[]{false,true}) {
          AccessRules.set(mc,GameRule.CREEPER_GRIEFING,grief);AccessRules.set(mc,GameRule.GHAST_GRIEFING,grief);
          mc.world.method_200(44,109,44,1);
          var blast=source==2?ExplosionSource.fireball(()->new class_60(mc.world,null,44.5,109.5,44.5,2))
              :new class_60(mc.world,source==1?new class_114(mc.world):new class_365(mc.world),44.5,109.5,44.5,2);
          blast.field_1397.add(new class_339(44,109,44));blast.method_1196(false);
          check(mc.world.getBlockId(44,109,44)==(source==0||grief?0:1),"block destruction source "+source+" / "+grief);
        }
      });
      test("dungeon spawners pause independently of natural mob spawning", () -> {
        var spawner=new class_104();spawner.world=mc.world;spawner.x=42;spawner.y=109;spawner.z=42;spawner.field_2681=50;
        AccessRules.set(mc,GameRule.SPAWN_ANIMALS,false);AccessRules.set(mc,GameRule.SPAWN_MONSTERS,false);
        AccessRules.set(mc,GameRule.SPAWNER_BLOCKS_WORK,false);for(int i=0;i<20;i++)spawner.method_1076();
        check(spawner.field_2681==50,"disabled spawner advanced");
        AccessRules.set(mc,GameRule.SPAWNER_BLOCKS_WORK,true);spawner.method_1076();
        check(spawner.field_2681==49,"natural-spawn rules blocked dungeon spawner");
      });
      test("mob loot blocks death drops while live mobs and player drops remain available", () -> {
        for(boolean loot:new boolean[]{false,true}) {
          AccessRules.set(mc,GameRule.MOB_DROPS,loot);
          var pig=new class_443(mc.world);pig.method_1340(44,110,44);pig.health=0;
          var drop=pig.method_1327(new ItemStack(319,1,0),0);
          check(drop.dead!=loot,"death loot rule "+loot);
          drop.markDead();pig.health=20;drop=pig.method_1327(new ItemStack(319,1,0),0);
          check(!drop.dead,"live-mob drop blocked");drop.markDead();
          drop=mc.player.method_1327(new ItemStack(4,1,0),0);check(!drop.dead,"player drop blocked");drop.markDead();
        }
      });
      test("sleep threshold uses actual sleepers, rejects over 100 and requires one sleeper at zero", () -> {
        var sleep=(local.luke.power.validation.mixin.PlayerSleepAccess)mc.player;
        try {
          for(int percentage:new int[]{0,1,50,100,101,Integer.MAX_VALUE}) {
            WorldRules.of(mc.world.method_262()).set(GameRule.PLAYERS_SLEEPING_PERCENTAGE,new JsonPrimitive(percentage));
            sleep.power$sleeping(false);sleep.power$sleepTimer(0);
            check(!mc.world.method_268(),"night skipped without sleepers");
            sleep.power$sleeping(true);sleep.power$sleepTimer(100);
            check(mc.world.method_268()==(percentage<=100),"sleep threshold "+percentage);
          }
        } finally {sleep.power$sleeping(false);sleep.power$sleepTimer(0);}
      });
      test("portal entry can be blocked without blocking return travel or retaining an old contact", () -> {
        var contact=PlayerEntity.class.getDeclaredField("field_512");contact.setAccessible(true);
        int dimension=mc.player.dimensionId;
        try {
          AccessRules.set(mc,GameRule.ALLOW_ENTERING_NETHER_USING_PORTALS,false);
          mc.player.dimensionId=0;mc.player.field_504=0;contact.setBoolean(mc.player,true);mc.player.method_937();
          check(mc.player.field_504==0&&!contact.getBoolean(mc.player),"blocked entry advanced or retained portal contact");
          AccessRules.set(mc,GameRule.ALLOW_ENTERING_NETHER_USING_PORTALS,true);mc.player.method_937();
          check(mc.player.field_504==0,"enabling portals reused an old contact");
          AccessRules.set(mc,GameRule.ALLOW_ENTERING_NETHER_USING_PORTALS,false);
          mc.player.dimensionId=-1;contact.setBoolean(mc.player,true);mc.player.method_937();
          check(mc.player.field_504>0&&!contact.getBoolean(mc.player),"Nether return portal blocked");
        } finally {mc.player.dimensionId=dimension;mc.player.field_504=0;contact.setBoolean(mc.player,false);}
      });
    } finally {
      mc.world.isRemote=false;AccessRules.restore(mc,rules);CreativeVehicleChecks.mode(mc,mode);
      ((WorldDifficulty)mc.world.method_262()).power$difficulty(difficulty);mc.world.field_213=difficulty;
      reset(mc.player);mc.player.health=health;mc.player.method_1340(x,y,z);
      for(Object entry:new ArrayList<>(mc.world.method_291()))if(entry instanceof Entity entity&&!entities.contains(entity))entity.markDead();
    }
    log("RULE BEHAVIOR FAILURES "+failures);
  }
  private static void reset(LivingEntity entity) {entity.health=20;entity.dead=false;entity.fire=0;entity.field_1613=0;entity.hurtTime=0;entity.field_1041=0;}
  private static int tntCount(Minecraft mc) {return (int)mc.world.method_291().stream().filter(e->e instanceof class_365 t&&!t.dead).count();}
}
