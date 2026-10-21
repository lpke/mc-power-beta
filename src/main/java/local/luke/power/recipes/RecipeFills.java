package local.luke.power.recipes;

import java.io.IOException;
import java.util.*;
import java.util.function.BooleanSupplier;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.offhand.Offhand;
import local.luke.power.recipes.planner.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

/** Recipe clicks prepare complete immutable plans; native slots publish only after durable recovery. */
public final class RecipeFills {
  private RecipeFills() {}
  public static boolean craftable(Minecraft mc,RecipeContext context,RecipeDefinition recipe)throws IOException {
    var cells=recipe.layout(context.width,context.height);if(cells==null||recipe.kind!=context.kind)return false;
    return craftable(context,recipe,supply(mc,context));
  }
  public static List<RecipeStack> supply(Minecraft mc,RecipeContext context)throws IOException {
    List<RecipeStack> main=new ArrayList<>(),grid=new ArrayList<>();
    for(ItemStack stack:mc.player.inventory.main)main.add(RecipeNative.stack(stack));
    for(var slot:context.inputs)grid.add(RecipeNative.stack(slot.getStack()));
    RecipeStack output=context.kind==RecipeDefinition.Kind.FURNACE&&!context.inputs.isEmpty()?RecipeNative.stack(context.output.getStack()):null;
    return RecipeFillPlanner.supply(main,RecipeNative.stack(Offhand.get(mc.player.inventory)),grid,
        mc.player.inventory.selectedSlot,Offhand.enabled(mc.player),output);
  }
  public static boolean craftable(RecipeContext context,RecipeDefinition recipe,List<RecipeStack> supply)throws IOException {
    var cells=recipe.layout(context.width,context.height);if(cells==null||recipe.kind!=context.kind)return false;
    var match=RecipeMatcher.match(cells,supply,1,Collections.nCopies(cells.size(),null));
    if(match==null)return false;
    List<RecipeStack> grid=new ArrayList<>();for(int index:match.cells())grid.add(index<0?null:match.supplies().get(index).representative().count(1));
    return valid(recipe,context,grid);
  }
  public static boolean fill(Minecraft mc,RecipeContext context,RecipeDefinition recipe,boolean maximum,
      BooleanSupplier eligible,java.util.function.Consumer<Boolean> selected)throws IOException {
    if(!context.writable(mc)||recipe.kind!=context.kind)return false;
    var cells=recipe.layout(context.width,context.height);if(cells==null)return false;
    List<RecipeStack> main=new ArrayList<>(),grid=new ArrayList<>();
    for(ItemStack item:mc.player.inventory.main)main.add(RecipeNative.stack(item));
    for(var slot:context.inputs)grid.add(RecipeNative.stack(slot.getStack()));
    RecipeStack off=RecipeNative.stack(Offhand.get(mc.player.inventory));
    RecipeStack originalOutput=context.kind==RecipeDefinition.Kind.FURNACE?RecipeNative.stack(context.output.getStack()):null;
    boolean matching=context.crafting!=null&&recipe.nativeRecipe!=null&&recipe.nativeRecipe.method_2072(context.crafting);
    if(context.kind==RecipeDefinition.Kind.FURNACE)matching=grid.get(0)!=null&&cells.get(0).accepts(grid.get(0));
    var plan=RecipeFillPlanner.plan(main,off,grid,cells,mc.player.inventory.selectedSlot,Offhand.enabled(mc.player),matching,maximum,originalOutput);
    if(plan==null)return false;
    if(!plan.ghost()&&!valid(recipe,context,plan.grid()))return false;
    RecipeOutputGuard output=new RecipeOutputGuard(mc,context);
    List<NativeInventoryPlan.Change> changes=new ArrayList<>();
    for(int i=0;i<36;i++)if(!Objects.equals(main.get(i),plan.main().get(i)))
      changes.add(NativeInventoryPlan.Change.main(mc.player.inventory,i,RecipeNative.nativeStack(plan.main().get(i))));
    if(!Objects.equals(off,plan.offhand()))changes.add(NativeInventoryPlan.Change.offhand(mc.player.inventory,RecipeNative.nativeStack(plan.offhand())));
    for(int i=0;i<grid.size();i++)if(!Objects.equals(grid.get(i),plan.grid().get(i)))
      changes.add(output.input(context.inputs.get(i),RecipeNative.nativeStack(plan.grid().get(i))));
    if(!Objects.equals(originalOutput,plan.output()))changes.add(output.returnedOutput());
    Runnable committed=()->{
      try {
        List<RecipeStack> actual=new ArrayList<>();for(var slot:context.inputs)actual.add(RecipeNative.stack(slot.getStack()));
        if(!actual.equals(plan.grid())||!output.matches()||!plan.ghost()&&!valid(recipe,context,actual))
          throw new IOException("Recipe changed during native publication");
        if(context.kind==RecipeDefinition.Kind.FURNACE&&!Objects.equals(RecipeNative.stack(context.output.getStack()),plan.output()))
          throw new IOException("Furnace output changed during native publication");
        if(!plan.ghost()&&context.kind==RecipeDefinition.Kind.CRAFTING
            &&!Objects.equals(encode(context.output.getStack()),encode(recipe.output)))throw new IOException("Native recipe result changed during filling");
        selected.accept(plan.ghost());
      }catch(IOException failure){throw new IllegalStateException("Recipe fill requires recovery",failure);}
    };
    if(changes.isEmpty()){if(eligible.getAsBoolean())committed.run();return true;}
    return NativeInventoryPlan.submit(mc,"recipe_fill",true,changes,()->context.writable(mc)&&eligible.getAsBoolean()&&output.matches(),committed,output::restore);
  }
  private static String encode(ItemStack stack)throws IOException{return stack==null?null:DeathItems.encode(stack);}
  public static boolean valid(RecipeDefinition recipe,RecipeContext context,List<RecipeStack> grid)throws IOException {
    if(context.kind==RecipeDefinition.Kind.FURNACE) {
      if(grid.size()!=1||grid.get(0)==null)return false;
      ItemStack input=RecipeNative.nativeStack(grid.get(0)),result;
      try{result=(ItemStack)Class.forName("net.modificationstation.stationapi.api.recipe.SmeltingRegistry")
          .getMethod("getResultFor",ItemStack.class).invoke(null,input);}
      catch(ReflectiveOperationException failure){throw new IOException("Native smelting adapter unavailable",failure);}
      return Objects.equals(encode(result),encode(recipe.output));
    }
    Container callbacks=new Container(){@Override public boolean method_2094(net.minecraft.entity.player.PlayerEntity player){return false;}};
    class_159 scratch=new class_159(callbacks,context.width,context.height);
    for(int i=0;i<grid.size();i++)scratch.setStack(i,RecipeNative.nativeStack(grid.get(i)));
    for(Object object:class_148.method_538().method_541())if(object instanceof class_160 nativeRecipe&&nativeRecipe.method_2072(scratch))
      return nativeRecipe==recipe.nativeRecipe&&Objects.equals(encode(nativeRecipe.method_2074(scratch)),encode(recipe.output));
    return false;
  }
}
