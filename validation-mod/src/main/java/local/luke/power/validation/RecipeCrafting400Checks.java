package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.offhand.Offhand;
import local.luke.power.recipes.*;
import local.luke.power.storage.JournalQueue;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Isolated native player grid exercises recipe matching and real result callbacks. */
final class RecipeCrafting400Checks {
  private RecipeCrafting400Checks() {}
  static void run(Minecraft mc)throws Exception {
    check(mc.player!=null&&mc.world!=null&&!mc.world.isRemote&&Display.isActive(),"focus a loaded disposable world first");
    check(!NativeInventoryPlan.busy(),"finish inventory transactions first");failures=0;
    var previousScreen=mc.currentScreen;var previousContainer=mc.player.container;
    var main=mc.player.inventory.main;var cursor=mc.player.inventory.getCursorStack();int selected=mc.player.inventory.selectedSlot;
    String offhand=encode(Offhand.get(mc.player.inventory));boolean grabbed=Mouse.isGrabbed();class_585 screen=null;
    try {
      mc.player.inventory.main=new ItemStack[36];mc.player.inventory.selectedSlot=0;mc.player.inventory.setCursorStack(null);
      Offhand.set(mc.player.inventory,null);Mouse.setGrabbed(false);
      screen=new class_585(mc.player);screen.container=new class_277(mc.player.inventory);
      mc.currentScreen=screen;mc.player.container=screen.container;screen.init(mc,640,360);
      var context=RecipeContext.of(screen);check(context!=null&&context.writable(mc),"isolated native grid is not writable");
      var recipes=NativeRecipes.load();
      var dye=recipes.stream().filter(recipe->recipe.kind==RecipeDefinition.Kind.CRAFTING&&!recipe.shaped
          &&recipe.output.itemId==35&&recipe.ingredients.size()==2&&recipe.layout(2,2)!=null).findFirst().orElseThrow();
      var shears=recipe(recipes,359);var sticks=recipe(recipes,280);
      test("native shapeless permutation refills in canonical order",()->{
        var canonical=canonical(dye);refill(mc,context,dye,new ItemStack[]{canonical[1],canonical[0],null,null});
      });
      test("native mirrored shaped recipe refills in canonical order",()->{
        check(shears.shaped&&shears.width==2&&shears.height==2,"shears native shape changed");
        var canonical=canonical(shears);var mirrored=new ItemStack[]{canonical[1],canonical[0],canonical[3],canonical[2]};
        check((canonical[0]==null)!=(mirrored[0]==null),"mirror did not move the native ingredient footprint");
        refill(mc,context,shears,mirrored);
      });
      test("native mirrored distinct ingredients are reassigned to canonical cells",()->{
        var nativeRecipe=new class_193(2,1,new ItemStack[]{new ItemStack(4,1,0),new ItemStack(3,1,0)},new ItemStack(1,1,0));
        var registry=class_148.method_538().method_541();registry.add(0,nativeRecipe);
        try {
          var mirroredRecipe=NativeRecipes.load().stream().filter(recipe->recipe.nativeRecipe==nativeRecipe).findFirst().orElseThrow();
          var canonical=canonical(mirroredRecipe);
          check(canonical[0].itemId!=canonical[1].itemId,"mirror needs distinct native identities");
          refill(mc,context,mirroredRecipe,new ItemStack[]{canonical[1],canonical[0],null,null});
        } finally {registry.remove(nativeRecipe);}
      });
      test("native offset shaped recipe refills in canonical order",()->{
        check(sticks.shaped&&sticks.width==1&&sticks.height==2,"sticks native shape changed");
        var canonical=canonical(sticks);refill(mc,context,sticks,new ItemStack[]{null,canonical[0],null,canonical[2]});
      });
      test("native matching uneven grid cannot shrink through either placement mode",()->{
        Arrays.fill(mc.player.inventory.main,null);var grid=canonical(sticks);grid[0].count=63;grid[2].count=1;setGrid(context,grid);
        String before=snapshot(mc,context);
        for(boolean maximum:new boolean[]{false,true}) {
          boolean[] selectedRecipe={false};
          check(RecipeFills.fill(mc,context,sticks,maximum,()->true,ghost->{check(!ghost,"unchanged recipe became a ghost");selectedRecipe[0]=true;}),"native no-growth selection rejected");
          check(selectedRecipe[0]&&!NativeInventoryPlan.busy(),"native no-growth selection queued a transaction");
          check(before.equals(snapshot(mc,context)),"native no-growth selection changed grid, inventory or output");
        }
      });
    } finally {
      try {NativeInventoryPlan.cancel();JournalQueue.awaitQueued();NativeInventoryPlan.tick(mc);}
      finally {
        if(screen!=null){var book=RecipeBooks.book(screen);if(book!=null)book.removed();InventoryPanels.remove(screen);}
        mc.currentScreen=previousScreen;mc.player.container=previousContainer;mc.player.inventory.main=main;
        mc.player.inventory.selectedSlot=selected;mc.player.inventory.setCursorStack(cursor);
        Offhand.set(mc.player.inventory,offhand==null?null:DeathItems.decode(offhand));Mouse.setGrabbed(grabbed);
      }
    }
    log("RECIPE400 CRAFTING NATIVE failures="+failures);
  }
  private static RecipeDefinition recipe(List<RecipeDefinition> recipes,int output) {
    return recipes.stream().filter(recipe->recipe.kind==RecipeDefinition.Kind.CRAFTING&&recipe.output.itemId==output
        &&recipe.layout(2,2)!=null).findFirst().orElseThrow();
  }
  private static ItemStack[] canonical(RecipeDefinition recipe) {
    ItemStack[] result=new ItemStack[4];var cells=recipe.layout(2,2);
    for(int i=0;i<4;i++)if(cells.get(i)!=null){result[i]=recipe.icons.get(recipe.ingredients.indexOf(cells.get(i))).get(0).clone();result[i].count=1;}
    return result;
  }
  private static void setGrid(RecipeContext context,ItemStack[] grid) {for(int i=0;i<4;i++)context.crafting.setStack(i,grid[i]);}
  private static void refill(Minecraft mc,RecipeContext context,RecipeDefinition recipe,ItemStack[] original)throws Exception {
    Arrays.fill(mc.player.inventory.main,null);var canonical=canonical(recipe);
    for(int i=0;i<4;i++)if(canonical[i]!=null){mc.player.inventory.main[i]=canonical[i].clone();mc.player.inventory.main[i].count=2;}
    setGrid(context,original);check(recipe.nativeRecipe.method_2072(context.crafting),"native recipe did not recognize the original arrangement");
    var totals=totals(mc,context);String before=snapshot(mc,context);boolean[] committed={false};
    check(RecipeFills.fill(mc,context,recipe,false,()->true,ghost->{check(!ghost,"matching recipe became a ghost");committed[0]=true;}),"matching native recipe was not admitted");
    check(before.equals(snapshot(mc,context)),"native slots changed before durable publication");
    JournalQueue.awaitQueued();NativeInventoryPlan.tick(mc);check(committed[0]&&!NativeInventoryPlan.busy(),"native recipe did not publish");
    for(int i=0;i<4;i++){if(canonical[i]!=null)canonical[i].count=2;check(Objects.equals(encode(canonical[i]),encode(context.inputs.get(i).getStack())),"canonical input differs at "+i);}
    check(totals.equals(totals(mc,context)),"native fill changed ingredient identities or total counts");
    check(Objects.equals(encode(recipe.output),encode(context.output.getStack())),"native crafting callback produced another result");
  }
  private static String encode(ItemStack stack)throws java.io.IOException {return stack==null?null:DeathItems.encode(stack);}
  private static List<ItemStack> items(Minecraft mc,RecipeContext context) {
    var items=new ArrayList<>(Arrays.asList(mc.player.inventory.main));for(var slot:context.inputs)items.add(slot.getStack());return items;
  }
  private static String snapshot(Minecraft mc,RecipeContext context)throws Exception {
    var bytes=new ArrayList<String>();for(var stack:items(mc,context))bytes.add(encode(stack));bytes.add(encode(context.output.getStack()));return bytes.toString();
  }
  private static Map<String,Long> totals(Minecraft mc,RecipeContext context)throws Exception {
    Map<String,Long> totals=new HashMap<>();for(var stack:items(mc,context))if(stack!=null){var identity=DeathItems.decode(encode(stack));identity.count=1;totals.merge(encode(identity),(long)stack.count,Long::sum);}return totals;
  }
}
