package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.gameplay.*;
import local.luke.power.mining.*;
import local.luke.power.input.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.SingleplayerInteractionManager;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

final class Mining170Checks {
  private static final int X = 2, Y = 110, Z = 2;
  static float progress(Minecraft mc, int id) throws Exception {
    Object state = Block.class.getMethod("getDefaultState").invoke(Block.BLOCKS[id]);
    var method = Arrays.stream(state.getClass().getMethods()).filter(m -> m.getName().equals("calcBlockBreakingDelta")).findFirst().orElseThrow();
    Object pos = method.getParameterTypes()[2].getConstructor(int.class,int.class,int.class).newInstance(X,Y,Z);
    return (float)method.invoke(state,mc.player,mc.world,pos);
  }
  private static void scene(Minecraft mc, int id, int count) {
    VeinMining.reset(); mc.setScreen(null); mc.paused = false;
    mc.player.method_1340(X + .5,Y + mc.player.field_1631,Z + 2.5); mc.player.field_1623 = true;
    mc.player.field_161.field_2536 = true;
    for(int x=X-1;x<X+10;x++) for(int y=Y-1;y<Y+3;y++) for(int z=Z-1;z<Z+3;z++) mc.world.method_200(x,y,z,0);
    for(int i=0;i<count;i++) mc.world.method_200(X+i,Y,Z,id);
    mc.interactionManager = new SingleplayerInteractionManager(mc);
  }
  private static int mine(Minecraft mc) {
    mc.interactionManager.method_1707(X,Y,Z,1);
    int ticks=0;
    while(mc.world.getBlockId(X,Y,Z)!=0 && ticks++<4000) {
      VeinMining.tick(mc); mc.interactionManager.method_1721(X,Y,Z,1);
    }
    check(ticks<4000,"first block did not finish");
    for(int i=0;i<600 && VeinMining.busy();i++) VeinMining.tick(mc);
    check(!VeinMining.busy(),"vein did not finish");
    return ticks;
  }
  static void run(Minecraft mc) throws Exception {
    failures=0; PowerConfig.flushPending(); var doc=PowerConfig.document();
    var gameplay=GameplayConfig.copy(); var veins=VeinConfig.copy();
    var main=mc.player.inventory.main.clone(); var manager=mc.interactionManager;
    var cheats=AccessRules.of(mc.world.method_262()); boolean allowed=cheats.allAllowed();
    String mode=mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    double x=mc.player.x,y=mc.player.y,z=mc.player.z;
    try {
      cheats.allAllowed(true);CreativeVehicleChecks.mode(mc,"SURVIVAL");cheats.allAllowed(false);
      mc.player.inventory.selectedSlot=0;Arrays.fill(mc.player.inventory.main,null);
      var options=GameplayConfig.copy();options.torchShovel=true;GameplayConfig.preview(options);
      VeinConfig.save(new VeinSettings());
      test("torch shovel matches diamond speed and keeps air penalties",()->{
        scene(mc,12,1);mc.player.inventory.main[0]=new ItemStack(277,1,0);
        float diamond=progress(mc,12);mc.player.inventory.main[1]=mc.player.inventory.main[0];mc.player.inventory.main[0]=new ItemStack(50,37,0);
        check(Math.abs(progress(mc,12)-diamond)<1e-7,"diamond speed mismatch");
        mc.player.field_1623=false;check(Math.abs(progress(mc,12)-diamond/5)<1e-7,"air penalty changed");mc.player.field_1623=true;
        for(int id:new int[]{1,3,24}) { options.torchShovel=false;GameplayConfig.preview(options);float before=progress(mc,id);options.torchShovel=true;GameplayConfig.preview(options);check(before==progress(mc,id),"other block sped up"); }
      });
      test("torch shovel chooses most worn diamond and charges successful breaks once",()->{
        scene(mc,13,1);Arrays.fill(mc.player.inventory.main,null);
        ItemStack torch=new ItemStack(50,37,0),fresh=new ItemStack(277,1,8),worn=new ItemStack(277,1,120);
        mc.player.inventory.main[0]=torch;mc.player.inventory.main[1]=fresh;mc.player.inventory.main[35]=worn;mc.player.inventory.main[2]=new ItemStack(273,1,130);
        check(TorchShovel.slot(mc.player,Block.GRAVEL)==35,"wrong shovel selected");
        mine(mc);check(worn.getDamage()==121 && fresh.getDamage()==8,"durability was not charged once to most worn");
        check(mc.player.inventory.selectedSlot==0 && mc.player.method_502()==torch && torch.count==37,"held torch or selection changed");
        check(!mc.interactionManager.method_1716(X,Y,Z,1),"air reported broken");check(worn.getDamage()==121,"failed break consumed shovel");
        scene(mc,12,1);worn.setDamage(worn.getMaxDamage());mine(mc);
        check(mc.player.inventory.main[35]==null && fresh.getDamage()==8,"broken shovel cleanup");
      });
      test("torch exclusions preserve creative, multiplayer, disabled and non-diamond mining",()->{
        scene(mc,12,1);Arrays.fill(mc.player.inventory.main,null);mc.player.inventory.main[0]=new ItemStack(50,1,0);
        for(int tool:new int[]{269,273,256,284}) {mc.player.inventory.main[1]=new ItemStack(tool,1,0);check(TorchShovel.slot(mc.player,Block.SAND)<0,"non-diamond shovel accepted");}
        mc.player.inventory.main[1]=new ItemStack(277,1,0);
        options.torchShovel=false;GameplayConfig.preview(options);check(TorchShovel.slot(mc.player,Block.SAND)<0,"disabled accepted");options.torchShovel=true;GameplayConfig.preview(options);
        mc.world.isRemote=true;try {check(TorchShovel.slot(mc.player,Block.SAND)<0,"multiplayer override");}finally{mc.world.isRemote=false;}
        cheats.allAllowed(true);CreativeVehicleChecks.mode(mc,"CREATIVE");check(TorchShovel.slot(mc.player,Block.SAND)<0,"creative override");CreativeVehicleChecks.mode(mc,"SURVIVAL");cheats.allAllowed(false);
      });
      test("veins scale first-block time, mine connected ores and wear original tool",()->{
        var v=new VeinSettings();v.enabled=true;v.timePercent=100;v.blocksPerTick=1;VeinConfig.save(v);
        scene(mc,16,4);Arrays.fill(mc.player.inventory.main,null);ItemStack pick=new ItemStack(278,1,12);mc.player.inventory.main[0]=pick;
        float normal=progress(mc,16);mc.interactionManager.method_1707(X,Y,Z,1);
        for(int i=0;i<100;i++)VeinMining.tick(mc);
        check(Math.abs(progress(mc,16)-normal/4)<1e-7,"vein time multiplier");
        mine(mc);for(int i=0;i<4;i++)check(mc.world.getBlockId(X+i,Y,Z)==0,"ore left in vein");
        check(pick.getDamage()==16,"per-block durability "+pick.getDamage());
      });
      test("default timing never beats a single block and respects obsidian speed",()->{
        var v=new VeinSettings();v.enabled=true;VeinConfig.save(v);
        for(int count:new int[]{1,3,8}) {
          scene(mc,16,count);mc.player.inventory.main[0]=new ItemStack(278,1,0);
          float normal=progress(mc,16);mc.interactionManager.method_1707(X,Y,Z,1);
          for(int i=0;i<100;i++)VeinMining.tick(mc);
          check(Math.abs(progress(mc,16)-normal/Math.max(1,count*.25))<1e-7,"minimum/percentage time wrong for "+count);
        }
        var before=local.luke.power.building.config.Config.current().copy();
        try {
          var speed=before.copy();speed.obsidianBreakingSpeed=100;local.luke.power.building.config.Config.preview(speed);
          v.tools.put("49",List.of(278));v.timePercent=100;VeinConfig.save(v);scene(mc,49,2);mc.player.inventory.main[0]=new ItemStack(278,1,0);
          float normal=progress(mc,49);check(normal>.03F,"obsidian speed bypassed");mc.interactionManager.method_1707(X,Y,Z,1);
          for(int i=0;i<100;i++)VeinMining.tick(mc);
          check(Math.abs(progress(mc,49)-normal/2)<1e-7,"obsidian and vein speed conflict");
        } finally {local.luke.power.building.config.Config.preview(before);}
      });
      test("vein tool break stops, no-wear option preserves extra durability, no slot swapping",()->{
        var v=new VeinSettings();v.enabled=true;v.timePercent=25;VeinConfig.save(v);
        scene(mc,16,4);var pick=new ItemStack(278,1,0);pick.setDamage(pick.getMaxDamage()-1);mc.player.inventory.main[0]=pick;
        mine(mc);check(mc.world.getBlockId(X,Y,Z)==0 && mc.world.getBlockId(X+1,Y,Z)==0,"tool should mine two blocks");
        check(mc.world.getBlockId(X+2,Y,Z)==16,"continued after tool broke");
        v.durabilityPerBlock=false;VeinConfig.save(v);scene(mc,16,4);pick=new ItemStack(278,1,15);mc.player.inventory.main[0]=pick;mine(mc);
        check(pick.getDamage()==16 && mc.player.inventory.selectedSlot==0,"free extra wear damaged or swapped tool");
      });
      test("vein limits, crouch, tool rules, cancellation and torch integration",()->{
        var v=new VeinSettings();v.enabled=true;v.maxBlocks=2;VeinConfig.save(v);scene(mc,16,4);mc.player.inventory.main[0]=new ItemStack(278,1,0);mine(mc);
        check(mc.world.getBlockId(X+1,Y,Z)==0 && mc.world.getBlockId(X+2,Y,Z)==16,"vein limit ignored");
        scene(mc,16,4);mc.player.field_161.field_2536=false;mine(mc);check(mc.world.getBlockId(X+1,Y,Z)==16,"crouch gate ignored");
        scene(mc,56,4);mc.player.inventory.main[0]=new ItemStack(270,1,0);mine(mc);check(mc.world.getBlockId(X+1,Y,Z)==56,"wrong tool chained");
        v.tools.put("12",List.of(277));v.maxBlocks=8;VeinConfig.save(v);scene(mc,12,3);mc.player.inventory.main[0]=new ItemStack(50,32,0);var shovel=new ItemStack(277,1,10);mc.player.inventory.main[1]=shovel;mine(mc);
        check(shovel.getDamage()==13 && mc.player.method_502().itemId==50,"torch vein did not preserve wear/held item");
        scene(mc,16,4);mc.player.inventory.main[0]=new ItemStack(278,1,0);mc.interactionManager.method_1707(X,Y,Z,1);VeinMining.tick(mc);
        mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen());VeinMining.tick(mc);mc.setScreen(null);
        check(mc.world.getBlockId(X,Y,Z)==16 && !VeinMining.busy(),"menu changed world during search");
      });
      test("fire rule is per world, independent of access permissions, and stops spread while fire ages",()->{
        var session=SettingsRegistry.open(mc);var setting=find(session,"rules.world.fire_spread");
        check(setting.defaultValue.getAsBoolean() && SettingScope.perWorld(setting) && SettingAccess.visible(session,setting),"fire scope/default/access");
        VeinConfig.save(new VeinSettings());scene(mc,0,0);
        mc.world.method_200(X,Y-1,Z,87);mc.world.method_200(X,Y,Z,51);mc.world.method_200(X+1,Y,Z,5);
        AccessRules.set(mc, local.luke.power.rules.GameRule.FIRE_SPREAD, false);GameplayConfig.preview(options);
        for(int i=0;i<100;i++)Block.BLOCKS[51].method_1602(mc.world,X,Y,Z,new Random(i));
        check(mc.world.getBlockId(X+1,Y,Z)==5,"disabled fire consumed neighbor");check(mc.world.method_1778(X,Y,Z)>0,"fire age stopped");
        AccessRules.set(mc, local.luke.power.rules.GameRule.FIRE_SPREAD, true);GameplayConfig.preview(options);
        for(int i=0;i<500 && mc.world.getBlockId(X+1,Y,Z)==5;i++)Block.BLOCKS[51].method_1602(mc.world,X,Y,Z,new Random(i));
        check(mc.world.getBlockId(X+1,Y,Z)!=5,"enabled fire never consumed neighbor");
      });
      test("temporary click mining leaves saved preference alone and resets with lifecycle",()->{
        var saved=local.luke.power.building.config.Config.current().copy();
        try {
          for(boolean preference:new boolean[]{false,true}) {
            var next=saved.copy();next.clickMining=preference;local.luke.power.building.config.Config.preview(next);
            ClickMining.reset();ClickMining.toggle();
            check(ClickMining.enabled(preference)!=preference,"override did not invert");
            check(local.luke.power.building.config.Config.current().clickMining==preference,"saved preference changed");
            check(TweakIndicators.label(TweakIndicators.Tweak.CLICK_MINING).equals(preference?"Click mining disabled":"Click mining enabled"),"wrong HUD text");
            TweakLifecycle.reset();check(!ClickMining.inverted(),"lifecycle did not reset");
          }
        }finally{local.luke.power.building.config.Config.preview(saved);ClickMining.reset();}
      });
    } finally {
      VeinMining.reset();VeinConfig.save(veins);GameplayConfig.preview(gameplay);PowerConfig.write(doc);
      mc.world.isRemote=false;mc.setScreen(null);mc.interactionManager=manager;
      System.arraycopy(main,0,mc.player.inventory.main,0,main.length);mc.player.method_1340(x,y,z);mc.player.field_161.field_2536=false;
      cheats.allAllowed(true);CreativeVehicleChecks.mode(mc,mode);cheats.allAllowed(allowed);
    }
    log("MINING 170 FAILURES "+failures);
  }
}
