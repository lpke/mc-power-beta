package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.gameplay.*;
import local.luke.power.mining.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.SingleplayerInteractionManager;
import net.minecraft.block.Block;
import net.minecraft.class_142;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/** Native mining, settings and persistence checks for a muted disposable client. */
final class Release222Checks {
  private static final String[] RESPONSE={"creative.acceleration","creative.glide","creative.spectatorAcceleration","creative.spectatorGlide","power_camera:config.acceleration","power_camera:config.drag"};
  private static final int X=2,Y=110,Z=2;
  static void run(Minecraft mc,String action) throws Exception {
    if(action.equals("save")||action.equals("saved")) {
      var session=SettingsRegistry.open(mc);
      if(action.equals("saved")) {
        check(GameplayConfig.current().axesSlashLeaves,"leaf setting lost on restart");
        for(String id:RESPONSE)check(Math.abs(find(session,id).value.getAsDouble()-2.375)<1e-7,"fraction lost on restart "+id);
      }
      find(session,"gameplay.axesSlashLeaves").parse(action.equals("save")?"true":"false");
      for(String id:RESPONSE) {
        var setting=find(session,id);
        if(action.equals("save"))setting.parse("2.375");else setting.reset();
      }
      session.save(Path.of(".").toAbsolutePath());log("PASS leaf and fractional flight persistence "+action);return;
    }
    failures=0;FreecamPreviewChecks.active(false);mc.setScreen(null);mc.paused=false;DeathChecks.alive(mc);
    local.luke.power.storage.PowerConfig.flushPending();var document=local.luke.power.storage.PowerConfig.document();
    var gameplay=GameplayConfig.copy();var veins=VeinConfig.copy();var rules=AccessRules.copy(mc);
    var main=mc.player.inventory.main.clone();var manager=mc.interactionManager;
    var saved=new NbtCompound();mc.player.write(saved);boolean auto=MenuPreferences.current().autoApply;
    try {
      AccessRules.restore(mc,local.luke.power.rules.RuleSet.cheats());CreativeVehicleChecks.mode(mc,"SURVIVAL");
      VeinConfig.save(new VeinSettings());mc.player.inventory.selectedSlot=0;
      var options=GameplayConfig.copy();options.axesSlashLeaves=true;GameplayConfig.preview(options);
      test("axes scale across all leaf metadata, diamond matches shovel time and nothing breaks instantly",()->{
        prepare(mc,12,0);mc.player.inventory.main[0]=new ItemStack(277,1,0);float sand=Mining170Checks.progress(mc,12);
        int sandTicks=mine(mc);float previous=0;
        for(int axe:new int[]{271,275,258,279,286}) {
          for(int metadata=0;metadata<16;metadata++) {
            prepare(mc,18,metadata);var tool=new ItemStack(axe,1,7);DeathChecks.tag(tool).putString("slash","keep");mc.player.inventory.main[0]=tool;
            float nativeSpeed=mc.player.method_511(Block.LEAVES);float progress=Mining170Checks.progress(mc,18);
            check(progress>1F/.2F/30 && progress<1,"not fast/non-instant "+axe+" "+progress);
            check(Math.abs(nativeSpeed/.2F/30-progress)<1e-6,"native and flattened speed differ");
            if(metadata==0){check(progress>previous,"tool tiers did not scale");previous=progress;}
            if(axe==279)check(Math.abs(progress-sand)/sand<.1,"diamond differs from sand baseline");
            int ticks=mine(mc);check(ticks>0,"instant leaf break");
            if(axe==279)check(ticks==sandTicks,"diamond mining ticks differ from diamond shovel on sand");
            check(tool.getDamage()==8&&tool.count==1&&mc.player.method_502()==tool,"leaf tool wear/count/identity changed");
            check(DeathChecks.tag(tool).getString("slash").equals("keep"),"leaf tool NBT changed");
          }
        }
        prepare(mc,18,0);var last=new ItemStack(279,1,0);last.setDamage(last.getMaxDamage());mc.player.inventory.main[0]=last;
        mine(mc);check(mc.player.method_502()==null,"broken axe was not removed by native cleanup");
      });
      test("disabled, fist, other blocks/tools, multiplayer and protected modes stay native; penalties compose",()->{
        prepare(mc,18,0);mc.player.inventory.main[0]=new ItemStack(279,1,0);
        float boosted=Mining170Checks.progress(mc,18);mc.player.field_1623=false;
        check(Math.abs(Mining170Checks.progress(mc,18)-boosted/5)<1e-7,"air penalty lost");mc.player.field_1623=true;
        for(int id:new int[]{1,3,12,17,20,47}) {
          options.axesSlashLeaves=false;GameplayConfig.preview(options);float before=Mining170Checks.progress(mc,id);
          options.axesSlashLeaves=true;GameplayConfig.preview(options);check(Mining170Checks.progress(mc,id)==before,"other block changed "+id);
        }
        for(int tool:new int[]{0,278,277,267,359}) {
          mc.player.inventory.main[0]=tool==0?null:new ItemStack(tool,1,0);
          options.axesSlashLeaves=false;GameplayConfig.preview(options);float before=Mining170Checks.progress(mc,18);
          options.axesSlashLeaves=true;GameplayConfig.preview(options);check(Mining170Checks.progress(mc,18)==before,"other tool changed "+tool);
        }
        mc.player.inventory.main[0]=new ItemStack(279,1,0);options.axesSlashLeaves=false;GameplayConfig.preview(options);
        float ordinary=Mining170Checks.progress(mc,18);options.axesSlashLeaves=true;GameplayConfig.preview(options);
        mc.world.isRemote=true;try{check(Mining170Checks.progress(mc,18)==ordinary,"multiplayer speed changed");}finally{mc.world.isRemote=false;}
        for(String mode:List.of("CREATIVE","SPECTATOR")){CreativeVehicleChecks.mode(mc,mode);check(AxesSlashLeaves.speed(mc.player,Block.LEAVES,1)==1,"protected-mode override");}
        CreativeVehicleChecks.mode(mc,"SURVIVAL");
        mc.player.inventory.main[0]=new ItemStack(279,1,0);
        for(int y=110;y<=113;y++)mc.world.method_154(2,y,4,9,0);
        mc.player.method_1340(2.5,111.62,4.5);mc.player.field_1623=true;
        check(Math.abs(Mining170Checks.progress(mc,18)-boosted/5)<1e-6,"water penalty lost");
        for(int y=110;y<=113;y++)mc.world.method_154(2,y,4,0,0);
      });
      test("all axe tiers preserve seeded vanilla sapling drops, including leaf species data",()->{
        for(int metadata=0;metadata<16;metadata++)for(int seed=0;seed<4;seed++) {
          List<String> expected=drops(mc,metadata,seed,0);
          for(int axe:new int[]{271,275,258,279,286})check(drops(mc,metadata,seed,axe).equals(expected),"drops changed "+axe+" metadata="+metadata+" seed="+seed);
        }
      });
      test("response sliders use whole steps; actual right-click editors retain precise values and Cancel restores previews",()->{
        MenuPreferences.update(v->v.autoApply=false);var screen=new PowerOptionsScreen(null);mc.setScreen(screen);screen.init(mc,640,420);
        for(String id:RESPONSE) {
          var setting=find(screen.session(),id);check(setting.kind==Setting.Kind.DECIMAL&&setting.step==1,"response kind/step "+id);
          for(int point=0;point<=100;point++){setting.slide(point/100.0);check(setting.value.getAsDouble()==Math.rint(setting.value.getAsDouble()),"fractional slide "+id);}
          search(screen,setting.label);
          var rows=(List<?>)field(screen,"rows");Object row=rows.stream().filter(r->{try{return call(r,"setting")==setting;}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
          field(screen,"scroll",(double)(int)call(row,"y"));
          int x=(int)call(screen,"right")-90,y=(int)call(screen,"top")+9;
          ((ScreenInput)(Object)screen).power$click(x,y,1);
          Object editor=field(screen,"valueEditor");check((boolean)call(editor,"active"),"right-click editor missing "+id);
          ((TextInput)field(editor,"input")).setText("2.375");((ScreenInput)(Object)screen).power$key('\n',28);
          check(Math.abs(setting.value.getAsDouble()-2.375)<1e-7,"precise UI input lost "+id);
        }
        screen.session().preview(true);var live=SettingsRegistry.open(mc);
        for(String id:RESPONSE)check(Math.abs(find(live,id).value.getAsDouble()-2.375)<1e-7,"backend truncated preview "+id);
        live.discard();screen.session().discard();mc.setScreen(null);
        var restored=SettingsRegistry.open(mc);for(String id:RESPONSE)check(find(restored,id).value.equals(find(restored,id).defaultValue),"Cancel changed response "+id);restored.discard();
      });
      test("leaf setting defaults off, obeys Apply/Cancel and leaves tools untouched",()->{
        var session=SettingsRegistry.open(mc);var setting=find(session,"gameplay.axesSlashLeaves");check(!setting.defaultValue.getAsBoolean(),"leaf default on");
        check(setting.page.equals("Gameplay")&&setting.group.equals("Blocks"),"leaf setting placement");
        var held=mc.player.method_502();String before=local.luke.power.death.DeathItems.encode(held);
        setting.parse("false");session.preview(true);check(GameplayConfig.current().axesSlashLeaves,"manual leaf draft changed live mining");session.discard();
        check(GameplayConfig.current().axesSlashLeaves,"Cancel did not retain leaf setting");
        setting.parse("false");session.save(Path.of(".").toAbsolutePath());check(!GameplayConfig.current().axesSlashLeaves,"Apply did not disable leaf setting");
        setting.parse("true");session.save(Path.of(".").toAbsolutePath());check(GameplayConfig.current().axesSlashLeaves,"Apply did not enable leaf setting");
        check(held==mc.player.method_502()&&before.equals(local.luke.power.death.DeathItems.encode(held)),"menu altered held tool");
      });
    } finally {
      FreecamPreviewChecks.active(false);VeinMining.reset();VeinConfig.save(veins);GameplayConfig.preview(gameplay);AccessRules.restore(mc,rules);
      mc.player.inventory.main=main;mc.player.read(saved);mc.interactionManager=manager;MenuPreferences.update(v->v.autoApply=auto);local.luke.power.storage.PowerConfig.write(document);mc.setScreen(null);
    }
    log("RELEASE 222 FAILURES "+failures);
  }
  private static void prepare(Minecraft mc,int id,int metadata) {
    mc.setScreen(null);mc.paused=false;mc.player.method_1340(2.5,111.62,4.5);mc.player.field_1623=true;
    for(int x=1;x<=3;x++)for(int z=1;z<=5;z++)for(int y=109;y<=114;y++)mc.world.method_154(x,y,z,y==109?1:0,0);
    mc.world.method_154(X,Y,Z,id,metadata);mc.interactionManager=new SingleplayerInteractionManager(mc);
  }
  private static int mine(Minecraft mc) {
    mc.interactionManager.method_1707(X,Y,Z,1);int ticks=0;
    while(mc.world.getBlockId(X,Y,Z)!=0&&ticks++<100)mc.interactionManager.method_1721(X,Y,Z,1);
    check(ticks<100,"native mine stalled");return ticks;
  }
  private static List<String> drops(Minecraft mc,int metadata,int seed,int tool) throws Exception {
    prepare(mc,18,metadata);
    for(Object entity:new ArrayList<>(mc.world.method_291()))if(entity instanceof class_142 item)item.markDead();
    mc.player.inventory.main[0]=tool==0?null:new ItemStack(tool,1,10);mc.world.field_214.setSeed(seed);
    check(mc.interactionManager.method_1716(X,Y,Z,1),"drop fixture failed to break leaves");
    var items=new ArrayList<String>();for(Object entity:mc.world.method_291())if(entity instanceof class_142 item&&!item.dead)
      items.add(item.field_564.itemId+":"+item.field_564.getDamage()+":"+item.field_564.count);
    Collections.sort(items);return items;
  }
}
