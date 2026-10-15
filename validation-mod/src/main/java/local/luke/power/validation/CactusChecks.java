package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import local.luke.power.config.*;
import local.luke.power.rules.*;
import local.luke.power.mixin.ItemHealthAccessor;
import net.minecraft.block.Block;
import net.minecraft.class_142;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

final class CactusChecks {
  static void run(Minecraft mc) throws Exception {
    failures=0;
    var before=AccessRules.copy(mc);
    boolean remote=mc.world.isRemote;
    try {
      test("cactus protection defaults off and only commits on Apply",()->{
        check(new RuleSet().enabled(GameRule.CACTUS_DESTROYS_ITEMS),"default is on");
        var session=SettingsRegistry.open(mc);
        var setting=find(session,"rules.world.cactus_destroys_items");
        setting.value=new com.google.gson.JsonPrimitive(!before.enabled(GameRule.CACTUS_DESTROYS_ITEMS));
        session.preview();check(AccessRules.copy(mc).enabled(GameRule.CACTUS_DESTROYS_ITEMS)==before.enabled(GameRule.CACTUS_DESTROYS_ITEMS),"preview changed mechanics");
        session.discard();check(AccessRules.copy(mc).enabled(GameRule.CACTUS_DESTROYS_ITEMS)==before.enabled(GameRule.CACTUS_DESTROYS_ITEMS),"Cancel changed mechanics");
      });
      for(boolean enabled:new boolean[]{false,true}) for(boolean multiplayer:new boolean[]{false,true})
        test("cactus contact protection="+enabled+" multiplayer="+multiplayer,()->{
          AccessRules.set(mc, GameRule.CACTUS_DESTROYS_ITEMS, !enabled);
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
            AccessRules.set(mc, GameRule.CACTUS_DESTROYS_ITEMS, true);
            Block.BLOCKS[81].method_1615(mc.world,4,110,4,item);
            check(((ItemHealthAccessor)item).power$health()==2,"toggle did not apply live");
          }
          item.markDead();
        });
      test("cactus still damages living entities",()->{
        mc.world.isRemote=false;AccessRules.set(mc, GameRule.CACTUS_DESTROYS_ITEMS, false);
        var pig=new net.minecraft.class_443(mc.world);
        int health=pig.health;Block.BLOCKS[81].method_1615(mc.world,4,110,4,pig);
        check(pig.health<health,"living entity protected");pig.markDead();
      });
    } finally {mc.world.isRemote=remote;AccessRules.restore(mc, before);}
    log("CACTUS FAILURES "+failures);
  }
}
