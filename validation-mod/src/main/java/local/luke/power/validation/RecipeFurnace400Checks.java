package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.io.IOException;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.InventoryPanels;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.offhand.Offhand;
import local.luke.power.recipes.*;
import local.luke.power.recipes.mixin.RecipeFurnaceAccessor;
import local.luke.power.storage.JournalQueue;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Native furnace placement and a separate, deliberately failing recovery probe. */
final class RecipeFurnace400Checks {
  private RecipeFurnace400Checks() {}
  static void run(Minecraft mc,boolean rollback)throws Exception {
    check(mc.world!=null&&!mc.world.isRemote&&mc.player!=null&&Display.isActive(),"focus a loaded disposable world first");
    check(!NativeInventoryPlan.busy(),"finish the previous inventory plan first");
    failures=0;
    var previousScreen=mc.currentScreen;var previousContainer=mc.player.container;
    var main=mc.player.inventory.main;var cursor=mc.player.inventory.getCursorStack();
    int selected=mc.player.inventory.selectedSlot;boolean grabbed=Mouse.isGrabbed();
    String offhand=encode(Offhand.get(mc.player.inventory));
    int x=(int)Math.floor(mc.player.x)+1,y=Math.max(1,Math.min(126,(int)Math.floor(mc.player.y))),z=(int)Math.floor(mc.player.z)+1;
    check(mc.world.getBlockId(x,y,z)==0,"fixture needs one nearby air block");
    class_524 screen=null;class_138 furnace=null;
    try {
      mc.world.method_200(x,y,z,61);furnace=(class_138)mc.world.method_1777(x,y,z);
      check(furnace!=null,"native furnace block entity missing");
      mc.player.inventory.main=new ItemStack[36];mc.player.inventory.selectedSlot=0;
      mc.player.inventory.setCursorStack(null);Offhand.set(mc.player.inventory,null);Mouse.setGrabbed(false);
      screen=new class_524(mc.player.inventory,furnace);mc.currentScreen=screen;mc.player.container=screen.container;screen.init(mc,640,360);
      var context=RecipeContext.of(screen);var target=furnace;
      check(context!=null&&context.writable(mc),"native furnace is not writable");
      var charcoal=NativeRecipes.load().stream().filter(recipe->recipe.kind==RecipeDefinition.Kind.FURNACE
          &&recipe.output.itemId==263&&recipe.output.getDamage()==1).findFirst().orElseThrow();
      if(rollback) {
        test("furnace callback failure restores full output bytes and all owned inputs",()->{
          prepare(mc,target);String before=snapshot(mc,target);
          check(RecipeFills.fill(mc,context,charcoal,false,()->true,ghost->{throw new IllegalStateException("intentional furnace publication probe");}),"rollback plan was not admitted");
          finish(mc);check(before.equals(snapshot(mc,target)),"callback rollback changed original native inventory bytes");
          check(target.getStack(2).count==117,"rollback clamped oversized furnace output");
        });
      } else {
        test("furnace recipe returns finished output and preserves fuel and native counts",()->{
          prepare(mc,target);String before=snapshot(mc,target),fuel=encode(target.getStack(1));var totals=totals(mc,target);boolean[] committed={false};
          check(RecipeFills.fill(mc,context,charcoal,false,()->true,ghost->{check(!ghost,"available recipe became a ghost");committed[0]=true;}),"furnace plan was not admitted");
          check(before.equals(snapshot(mc,target)),"queued recipe changed native slots before durable publication");finish(mc);
          check(committed[0],"furnace plan did not publish");check(totals.equals(totals(mc,target)),"furnace return changed full native identity/count totals");
          check(target.getStack(0).itemId==17&&target.getStack(0).getDamage()==2&&target.getStack(0).count==1,"birch input was not placed");
          check(target.getStack(2)==null&&fuel.equals(encode(target.getStack(1))),"finished output or fuel handling differs");
        });
        test("furnace output capacity is checked before consuming the new ingredient",()->{
          for(int i=0;i<36;i++)mc.player.inventory.main[i]=new ItemStack(4,64,0);
          mc.player.inventory.main[0]=new ItemStack(17,1,2);raw(target)[0]=null;raw(target)[2]=DeathChecks.tagged(35,1,5,"blocked-output");
          String before=snapshot(mc,target);
          check(!RecipeFills.fill(mc,context,charcoal,false,()->true,ghost->{}),"full output return unexpectedly fit");
          check(before.equals(snapshot(mc,target)),"rejected capacity plan changed native slots");
        });
        test("canceling a pending furnace fill keeps finished output and inputs intact",()->{
          prepare(mc,target);String before=snapshot(mc,target);
          check(RecipeFills.fill(mc,context,charcoal,false,()->true,ghost->{throw new AssertionError("canceled recipe published");}),"cancel probe was not admitted");
          NativeInventoryPlan.cancel();finish(mc);check(before.equals(snapshot(mc,target)),"canceled furnace plan changed native bytes");
        });
        test("matching full furnace input leaves finished output in place",()->{
          Arrays.fill(mc.player.inventory.main,null);raw(target)[0]=new ItemStack(17,64,2);raw(target)[2]=DeathChecks.tagged(35,3,5,"no-growth");
          String before=snapshot(mc,target);
          check(RecipeFills.fill(mc,context,charcoal,true,()->true,ghost->check(!ghost,"matching full input became a ghost")),"full matching input rejected");
          check(before.equals(snapshot(mc,target))&&!NativeInventoryPlan.busy(),"no-growth selection changed or queued native slots");
        });
        test("returned furnace output can supply an explicitly registered smelting chain",()->{
          var registry=class_44.method_144().method_147();Object previous=registry.put(1,new ItemStack(20,1,0));
          try {
            Arrays.fill(mc.player.inventory.main,null);raw(target)[0]=null;raw(target)[1]=new ItemStack(263,2,0);raw(target)[2]=new ItemStack(1,3,0);
            var stone=RecipeNative.stack(target.getStack(2));var recipe=NativeRecipes.load().stream().filter(value->value.kind==RecipeDefinition.Kind.FURNACE
                &&value.output.itemId==20&&value.ingredients.get(0).accepts(stone)).findFirst().orElseThrow();
            var totals=totals(mc,target);String fuel=encode(target.getStack(1));boolean[] committed={false};
            check(RecipeFills.craftable(mc,context,recipe),"returned output was omitted from available ingredients");
            check(RecipeFills.fill(mc,context,recipe,true,()->true,ghost->{check(!ghost,"output chain became a ghost");committed[0]=true;}),"output chain was not admitted");finish(mc);
            check(committed[0]&&target.getStack(0).itemId==1&&target.getStack(0).count==3&&target.getStack(2)==null,"returned output was not placed as input");
            check(totals.equals(totals(mc,target))&&fuel.equals(encode(target.getStack(1))),"output chain changed counts or fuel");
          } finally {if(previous==null)registry.remove(1);else registry.put(1,previous);}
        });
      }
    } finally {
      try {NativeInventoryPlan.cancel();JournalQueue.awaitQueued();NativeInventoryPlan.tick(mc);}
      finally {
        if(screen!=null)InventoryPanels.remove(screen);
        mc.currentScreen=previousScreen;mc.player.container=previousContainer;mc.player.inventory.main=main;
        mc.player.inventory.selectedSlot=selected;mc.player.inventory.setCursorStack(cursor);Offhand.set(mc.player.inventory,offhand==null?null:DeathItems.decode(offhand));
        if(furnace!=null)Arrays.fill(raw(furnace),null);mc.world.method_200(x,y,z,0);Mouse.setGrabbed(grabbed);
      }
    }
    log("RECIPE400 FURNACE "+(rollback?"ROLLBACK":"NATIVE")+" failures="+failures);
  }
  private static ItemStack[] raw(class_138 furnace){return ((RecipeFurnaceAccessor)furnace).power$recipeStacks();}
  private static void prepare(Minecraft mc,class_138 furnace)throws Exception {
    Arrays.fill(mc.player.inventory.main,null);mc.player.inventory.main[0]=new ItemStack(17,2,2);
    raw(furnace)[0]=new ItemStack(15,1,0);raw(furnace)[1]=DeathChecks.tagged(263,3,0,"fuel");raw(furnace)[2]=DeathChecks.tagged(35,117,5,"finished-output");
  }
  private static void finish(Minecraft mc)throws IOException {JournalQueue.awaitQueued();NativeInventoryPlan.tick(mc);check(!NativeInventoryPlan.busy(),"durable furnace plan did not finish");}
  private static String encode(ItemStack stack)throws IOException{return stack==null?null:DeathItems.encode(stack);}
  private static List<ItemStack> items(Minecraft mc,class_138 furnace){var items=new ArrayList<>(Arrays.asList(mc.player.inventory.main));items.addAll(Arrays.asList(raw(furnace)));items.add(Offhand.get(mc.player.inventory));return items;}
  private static String snapshot(Minecraft mc,class_138 furnace)throws IOException {var bytes=new ArrayList<String>();for(var item:items(mc,furnace))bytes.add(encode(item));return bytes.toString();}
  private static Map<String,Long> totals(Minecraft mc,class_138 furnace)throws IOException {
    Map<String,Long> result=new HashMap<>();for(var item:items(mc,furnace))if(item!=null){var identity=DeathItems.decode(DeathItems.encode(item));identity.count=1;result.merge(DeathItems.encode(identity),(long)item.count,Long::sum);}return result;
  }
}
