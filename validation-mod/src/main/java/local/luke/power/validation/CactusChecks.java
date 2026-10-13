package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import local.luke.power.config.*;
import local.luke.power.gameplay.*;
import local.luke.power.mixin.ItemHealthAccessor;
import net.minecraft.block.Block;
import net.minecraft.class_142;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

final class CactusChecks {
  static void run(Minecraft mc) throws Exception {
    failures=0;
    var before=GameplayConfig.copy();
    boolean remote=mc.world.isRemote;
    try {
      test("cactus protection defaults off and only commits on Apply",()->{
        check(!new GameplaySettings().cactusProofItems,"default is on");
        var session=SettingsRegistry.open(mc);
        var setting=find(session,"gameplay.cactusProofItems");
        setting.value=new com.google.gson.JsonPrimitive(!before.cactusProofItems);
        session.preview();check(GameplayConfig.current().cactusProofItems==before.cactusProofItems,"preview changed mechanics");
        session.discard();check(GameplayConfig.current().cactusProofItems==before.cactusProofItems,"Cancel changed mechanics");
      });
      for(boolean enabled:new boolean[]{false,true}) for(boolean multiplayer:new boolean[]{false,true})
        test("cactus contact protection="+enabled+" multiplayer="+multiplayer,()->{
          var s=GameplayConfig.copy();s.cactusProofItems=enabled;GameplayConfig.preview(s);
          mc.world.isRemote=multiplayer;
          var stack=new ItemStack(267,1,37);DeathChecks.tag(stack).putString("cactus-test","preserved");
          var item=new class_142(mc.world,4.5,110.3,4.5,stack);
          for(int i=0;i<20&&!item.dead;i++) Block.BLOCKS[81].method_1615(mc.world,4,110,4,item);
          boolean protectedItem=enabled&&!multiplayer;
          check(protectedItem?!item.dead&&((ItemHealthAccessor)item).power$health()==5:item.dead,"unexpected survival");
          check(item.field_564==stack&&stack.count==1&&stack.getDamage()==37
              &&DeathChecks.tag(stack).getString("cactus-test").equals("preserved"),"stack changed");
          if(protectedItem){
            item.damage(null,2);check(((ItemHealthAccessor)item).power$health()==3,"other damage was suppressed");
            s.cactusProofItems=false;GameplayConfig.preview(s);
            Block.BLOCKS[81].method_1615(mc.world,4,110,4,item);
            check(((ItemHealthAccessor)item).power$health()==2,"toggle did not apply live");
          }
          item.markDead();
        });
      test("cactus still damages living entities",()->{
        mc.world.isRemote=false;var s=GameplayConfig.copy();s.cactusProofItems=true;GameplayConfig.preview(s);
        var pig=new net.minecraft.class_443(mc.world);
        int health=pig.health;Block.BLOCKS[81].method_1615(mc.world,4,110,4,pig);
        check(pig.health<health,"living entity protected");pig.markDead();
      });
    } finally {mc.world.isRemote=remote;GameplayConfig.preview(before);}
    log("CACTUS FAILURES "+failures);
  }
}
