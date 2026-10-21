package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.InventoryPanels;
import local.luke.power.recipes.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/** Native registry/NBT/UI probes. These run only in the parent's disposable validation instance. */
public final class Recipe400Checks {
  public static void run(Minecraft mc,String phase)throws Exception {
    if(phase.equals("visual")){Recipe400Visuals.run(mc);return;}
    if(phase.equals("crafting-native")){RecipeCrafting400Checks.run(mc);return;}
    if(phase.equals("furnace-native")||phase.equals("furnace-rollback")){RecipeFurnace400Checks.run(mc,phase.equals("furnace-rollback"));return;}
    failures=0;
    if(phase.equals("registry")) {
      test("native recipe book reads shaped, shapeless and smelting registries",()->{
        var recipes=NativeRecipes.load();check(!recipes.isEmpty(),"empty registry");
        check(recipes.stream().anyMatch(r->r.kind==RecipeDefinition.Kind.FURNACE),"smelting recipes missing");
        check(recipes.stream().anyMatch(r->r.kind==RecipeDefinition.Kind.CRAFTING&&r.shaped),"shaped recipes missing");
        check(recipes.stream().anyMatch(r->r.kind==RecipeDefinition.Kind.CRAFTING&&!r.shaped),"shapeless recipes missing");
        check(recipes.stream().map(r->r.id).distinct().count()==recipes.size(),"unstable duplicate recipe IDs");
        for(var recipe:recipes)check(recipe.output!=null&&recipe.output.count>0&&recipe.ingredients.size()<=9,"unsafe display");
      });
      test("native recipe stack codec retains custom data, metadata and oversized counts",()->{
        ItemStack stack=DeathChecks.tagged(35,117,5,"recipe-native");String before=DeathItems.encode(stack);
        var frozen=RecipeNative.stack(stack);var restored=RecipeNative.nativeStack(frozen);
        check(before.equals(DeathItems.encode(restored)),"full native bytes changed");
        check(stack.count==117&&stack.getDamage()==5,"source mutated");
        var named=DeathChecks.tagged(35,2,5,"named");DeathChecks.tag(named).putString("CustomName","Protected");
        check(!RecipeNative.stack(named).usable(),"named recipe ingredient admitted");
      });
      test("numeric smelting recipes accept every native log metadata variant",()->{
        var context=RecipeContext.viewer(mc,RecipeDefinition.Kind.FURNACE);
        var charcoal=NativeRecipes.load().stream().filter(recipe->recipe.kind==RecipeDefinition.Kind.FURNACE
            &&recipe.output.itemId==263&&recipe.output.getDamage()==1).findFirst().orElseThrow();
        for(int metadata=0;metadata<=2;metadata++) {
          var input=RecipeNative.stack(new ItemStack(17,1,metadata));
          check(charcoal.ingredients.get(0).accepts(input),"log metadata "+metadata+" rejected by adapter");
          check(RecipeFills.valid(charcoal,context,List.of(input)),"log metadata "+metadata+" disagrees with native smelting");
        }
      });
      test("furnace ghost fuel icons follow the native fuel query",()->{
        var furnace=new class_138();var fuels=NativeRecipes.fuels(furnace);
        for(int id:new int[]{5,263,327})check(fuels.stream().anyMatch(stack->stack.itemId==id),"native fuel icon missing: "+id);
        check(fuels.stream().noneMatch(stack->stack.itemId==264),"diamond admitted as fuel");
        check(furnace.getStack(0)==null&&furnace.getStack(1)==null&&furnace.getStack(2)==null,"fuel query changed the furnace");
      });
      test("unknown native recipe subclasses require an explicit display provider",()->{
        var custom=new class_193(1,1,new ItemStack[]{new ItemStack(1,1,0)},new ItemStack(4,1,0)) {
          @Override public boolean method_2072(class_159 grid){return false;}
        };
        var registry=class_148.method_538().method_541();registry.add(custom);
        try {check(NativeRecipes.load().stream().noneMatch(recipe->recipe.nativeRecipe==custom),"custom inputs were inferred from a native superclass");}
        finally {registry.remove(custom);}
      });
      test("additive recipe and saved-hotbar panels retain independent ownership",()->{
        Object owner=new Object(),modal=new Object(),screen=new Object();
        InventoryPanels.update(screen,77,List.of(new InventoryPanels.Rect(1,2,30,40)));
        InventoryPanels.extra(screen,owner,List.of(new InventoryPanels.Rect(200,2,30,40)));
        InventoryPanels.modal(screen,modal,true);check(InventoryPanels.modalOther(screen,owner),"other modal lost");
        InventoryPanels.removeExtra(screen,owner);check(InventoryPanels.offset(screen)==77&&InventoryPanels.contains(screen,2,3),"recipe geometry replaced");
        InventoryPanels.remove(screen);
      });
      log("RECIPE400 REGISTRY failures="+failures);return;
    }
    var state=RecipeBookConfig.copy();state.enabled=true;state.inventory=true;state.pauseMenu=true;
    state.craftingOpen=true;state.furnaceOpen=true;RecipeBookConfig.preview(state);
    if(phase.equals("wide")||phase.equals("narrow")) {
      CreativeVehicleChecks.mode(mc,"SURVIVAL");var screen=new class_585(mc.player);mc.setScreen(screen);
      screen.init(mc,phase.equals("wide")?640:320,360);var book=((RecipeBookHost)screen).power$recipeBook();book.tick();
      check(InventoryPanels.offset(screen)==(phase.equals("wide")?77:0),"native offset mismatch");
      check(book.replacesNative()==phase.equals("narrow"),"narrow replacement mismatch");
    } else if(phase.equals("table")){mc.setScreen(new class_516(mc.player.inventory,mc.world,(int)mc.player.x,(int)mc.player.y,(int)mc.player.z));}
    else if(phase.equals("furnace")){mc.setScreen(new class_524(mc.player.inventory,new class_138()));}
    else if(phase.equals("viewer")){RecipeBooks.openViewer(mc,mc.currentScreen);}
    else throw new IllegalArgumentException("registry, visual, crafting-native, furnace-native, furnace-rollback, wide, narrow, table, furnace or viewer");
    log("RECIPE400 "+phase+" failures="+failures);
  }
}
