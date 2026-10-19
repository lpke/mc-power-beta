package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.ByteBuffer;
import java.util.*;
import local.luke.power.config.SettingsRegistry;
import local.luke.power.gameplay.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

final class Release150InteractionChecks {
  static void run(Minecraft mc) throws Exception {
    failures=0;
    var properties=AccessRules.of(mc.world.method_262());boolean cheats=properties.allAllowed();
    String mode=mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    var inventory=Arrays.stream(mc.player.inventory.main).map(s->ItemStack.clone(s)).toArray(ItemStack[]::new);
    var cursor=ItemStack.clone(mc.player.inventory.getCursorStack());
    var keysField=Keyboard.class.getDeclaredField("keyDownBuffer");keysField.setAccessible(true);
    var keys=(ByteBuffer)keysField.get(null);byte shift=keys.get(42);
    var gameplay=GameplayConfig.copy();var session=SettingsRegistry.open(mc);List<Entity> created=new ArrayList<>();
    try {
      properties.allAllowed(true);CreativeVehicleChecks.mode(mc,"CREATIVE");
      test("actual catalog Shift-click obeys item and live bucket limits with both mouse buttons",()->{
        keys.put(42,(byte)1);
        for(int width:new int[]{320,640})for(boolean buckets:new boolean[]{false,true}) {
          find(session,"power_compat:config.STACK_SIZE_CONFIG.stackableEmptyBuckets").parse(Boolean.toString(buckets));session.preview(true);
          for(int[] item:new int[][]{{1,64},{257,1},{310,1},{325,buckets?16:1},{326,1}})for(int button:new int[]{0,1}) {
            Arrays.fill(mc.player.inventory.main,null);mc.player.inventory.setCursorStack(null);mc.player.closeScreen();
            var screen=new class_585(mc.player);mc.setScreen(screen);screen.init(mc,width,420);
            field(screen,"creative_normalGUI",false);field(screen,"creative_rowIndex",0);
            var template=new ItemStack(item[0],1,item[0]==257?37:0);DeathChecks.tag(template).putString("catalogue","preserve");
            field(screen,"creative_items",List.of(template));
            var held=new ItemStack(264,3,0);mc.player.inventory.setCursorStack(held);
            ((ScreenInput)(Object)screen).power$click((width-176)/2+16,(420-166)/2-20+22,button);
            int total=Arrays.stream(mc.player.inventory.main).filter(Objects::nonNull).mapToInt(s->s.count).sum();
            check(total==item[1],"catalog count id="+item[0]+" count="+total);
            var result=Arrays.stream(mc.player.inventory.main).filter(Objects::nonNull).findFirst().orElseThrow();
            check(DeathChecks.tag(result).getString("catalogue").equals("preserve") && result.getDamage()==template.getDamage(),"catalog item data changed");
            check(mc.player.inventory.getCursorStack()==held && held.count==3 && template.count==1,"cursor/template changed");
          }
        }
      });
      test("crowded item merging has bounded passes and conserves 4096 items",()->{
        mc.player.inventory.setCursorStack(null);mc.player.closeScreen();mc.setScreen(null);mc.player.method_1340(0,110,0);mc.world.method_214(0,0);
        for(int i=0;i<4096;i++)created.add(Release150Checks.item(mc,4,1,0,2+(i%4)*.1,110,2));
        var s=GameplayConfig.copy();s.mergeItems=false;GameplayConfig.preview(s);
        long start=System.nanoTime();for(int i=0;i<100;i++)check(ItemMerging.merge(mc.world,mc)==0,"off state merged");
        double disabled=(System.nanoTime()-start)/1e6;
        s.mergeItems=true;s.mergeThreshold=256;GameplayConfig.preview(s);double max=0;int passes=0;
        while(Release150Checks.alive(created)>=256 && passes++<256) {
          start=System.nanoTime();ItemMerging.merge(mc.world,mc);max=Math.max(max,(System.nanoTime()-start)/1e6);
        }
        check(Release150Checks.count(created)==4096 && Release150Checks.alive(created)<256,"merge count/performance regression");
        for(var entity:created)if(!entity.dead) {
          var item=(net.minecraft.class_142)entity;
          check(item.field_564.count<=64,"overfull merged stack");
          var tag=new net.minecraft.nbt.NbtCompound();item.write(tag);var copy=new net.minecraft.class_142(mc.world);copy.read(tag);
          check(copy.field_564.count==item.field_564.count,"merged stack did not survive save");
        }
        log("ITEM BENCHMARK off100PassMs="+disabled+" enabledPasses="+passes+" maxPassMs="+max+" remaining="+Release150Checks.alive(created));
      });
    } finally {
      keys.put(42,shift);created.forEach(Entity::markDead);GameplayConfig.preview(gameplay);session.discard();
      mc.player.inventory.setCursorStack(null);mc.player.closeScreen();
      System.arraycopy(inventory,0,mc.player.inventory.main,0,inventory.length);mc.player.inventory.setCursorStack(cursor);
      properties.allAllowed(cheats);CreativeVehicleChecks.mode(mc,mode);
    }
    log("RELEASE 150 INTERACTION FAILURES "+failures);
  }
}
