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
  public enum Result { ACCEPTED, NO_SPACE, UNAVAILABLE, BUSY, RECOVERY_REQUIRED }
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
    return fillResult(mc,context,recipe,maximum,eligible,selected)==Result.ACCEPTED;
  }
  public static Result fillResult(Minecraft mc,RecipeContext context,RecipeDefinition recipe,boolean maximum,
      BooleanSupplier eligible,java.util.function.Consumer<Boolean> selected)throws IOException {
    return fillResult(mc,context,recipe,maximum,eligible,selected,1);
  }
  public static Result fillResult(Minecraft mc,RecipeContext context,RecipeDefinition recipe,boolean maximum,
      BooleanSupplier eligible,java.util.function.Consumer<Boolean> selected,int steps)throws IOException {
    if(NativeInventoryPlan.failed())return Result.RECOVERY_REQUIRED;
    if(!context.writable(mc)||recipe.kind!=context.kind||!eligible.getAsBoolean())return Result.UNAVAILABLE;
    var cells=recipe.layout(context.width,context.height);if(cells==null)return Result.UNAVAILABLE;
    List<RecipeStack> main=new ArrayList<>(),grid=new ArrayList<>();
    for(ItemStack item:mc.player.inventory.main)main.add(RecipeNative.stack(item));
    for(var slot:context.inputs)grid.add(RecipeNative.stack(slot.getStack()));
    RecipeStack off=RecipeNative.stack(Offhand.get(mc.player.inventory));
    RecipeStack originalOutput=context.kind==RecipeDefinition.Kind.FURNACE?RecipeNative.stack(context.output.getStack()):null;
    boolean matching=context.crafting!=null&&recipe.nativeRecipe!=null&&recipe.nativeRecipe.method_2072(context.crafting);
    if(context.kind==RecipeDefinition.Kind.FURNACE)matching=grid.get(0)!=null&&cells.get(0).accepts(grid.get(0));
    var plan=RecipeFillPlanner.placeSteps(main,off,grid,cells,mc.player.inventory.selectedSlot,Offhand.enabled(mc.player),matching,maximum,originalOutput,steps);
    if(plan==null)return Result.NO_SPACE;
    if(!plan.ghost()&&!valid(recipe,context,plan.grid()))return Result.UNAVAILABLE;
    return publish(mc,context,recipe,main,off,grid,originalOutput,plan,false,eligible,selected);
  }
  /** Only the displayed, already placed crafting recipe may return one batch of inputs. */
  public static Result removeResult(Minecraft mc,RecipeContext context,RecipeDefinition recipe,
      BooleanSupplier eligible,java.util.function.Consumer<Boolean> selected)throws IOException {
    return removeResult(mc,context,recipe,eligible,selected,1);
  }
  public static Result removeResult(Minecraft mc,RecipeContext context,RecipeDefinition recipe,
      BooleanSupplier eligible,java.util.function.Consumer<Boolean> selected,int steps)throws IOException {
    if(NativeInventoryPlan.failed())return Result.RECOVERY_REQUIRED;
    if(context.kind!=RecipeDefinition.Kind.CRAFTING||recipe.kind!=context.kind||!context.writable(mc)
        ||!eligible.getAsBoolean())return Result.UNAVAILABLE;
    List<RecipeStack> main=new ArrayList<>(),grid=new ArrayList<>();
    for(ItemStack item:mc.player.inventory.main)main.add(RecipeNative.stack(item));
    for(var slot:context.inputs)grid.add(RecipeNative.stack(slot.getStack()));
    if(!valid(recipe,context,grid)||!Objects.equals(encode(context.output.getStack()),encode(recipe.output)))return Result.UNAVAILABLE;
    RecipeStack off=RecipeNative.stack(Offhand.get(mc.player.inventory));
    var plan=RecipeFillPlanner.removeSteps(main,off,grid,mc.player.inventory.selectedSlot,Offhand.enabled(mc.player),steps);
    if(plan==null)return Result.NO_SPACE;
    return publish(mc,context,recipe,main,off,grid,null,plan,true,eligible,selected);
  }
  private static Result publish(Minecraft mc,RecipeContext context,RecipeDefinition recipe,List<RecipeStack> main,
      RecipeStack off,List<RecipeStack> grid,RecipeStack originalOutput,RecipeFillPlanner.Plan plan,boolean removal,
      BooleanSupplier eligible,java.util.function.Consumer<Boolean> selected)throws IOException {
    String remainingOutput=removal?craftingOutput(context,plan.grid()):null;
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
        if(!actual.equals(plan.grid())||!output.matches()||!removal&&!plan.ghost()&&!valid(recipe,context,actual))
          throw new IOException("Recipe changed during native publication");
        if(context.kind==RecipeDefinition.Kind.FURNACE&&!Objects.equals(RecipeNative.stack(context.output.getStack()),plan.output()))
          throw new IOException("Furnace output changed during native publication");
        if(removal&&!Objects.equals(encode(context.output.getStack()),remainingOutput))
          throw new IOException("Native recipe result changed during removal");
        if(!removal&&!plan.ghost()&&context.kind==RecipeDefinition.Kind.CRAFTING
            &&!Objects.equals(encode(context.output.getStack()),encode(recipe.output)))throw new IOException("Native recipe result changed during filling");
        selected.accept(plan.ghost());
      }catch(IOException failure){throw new IllegalStateException("Recipe fill requires recovery",failure);}
    };
    if(changes.isEmpty()){if(!eligible.getAsBoolean())return Result.UNAVAILABLE;committed.run();return Result.ACCEPTED;}
    if(NativeInventoryPlan.submit(mc,removal?"recipe_remove":"recipe_fill",true,changes,()->context.writable(mc)&&eligible.getAsBoolean()&&output.matches(),committed,output::restore))return Result.ACCEPTED;
    if(NativeInventoryPlan.failed())return Result.RECOVERY_REQUIRED;
    return NativeInventoryPlan.busy()||local.luke.power.inventory.InventoryActions.busy()?Result.BUSY:Result.UNAVAILABLE;
  }
  private static String encode(ItemStack stack)throws IOException{return stack==null?null:DeathItems.encode(stack);}
  static String craftingOutput(RecipeContext context,List<RecipeStack> grid)throws IOException {
    class_159 scratch=scratch(context,grid);
    for(Object object:class_148.method_538().method_541())if(object instanceof class_160 nativeRecipe&&nativeRecipe.method_2072(scratch))
      return encode(nativeRecipe.method_2074(scratch));
    return null;
  }
  private static class_159 scratch(RecipeContext context,List<RecipeStack> grid)throws IOException {
    Container callbacks=new Container(){@Override public boolean method_2094(net.minecraft.entity.player.PlayerEntity player){return false;}};
    class_159 scratch=new class_159(callbacks,context.width,context.height);
    for(int i=0;i<grid.size();i++)scratch.setStack(i,RecipeNative.nativeStack(grid.get(i)));
    return scratch;
  }
  public static boolean valid(RecipeDefinition recipe,RecipeContext context,List<RecipeStack> grid)throws IOException {
    if(context.kind==RecipeDefinition.Kind.FURNACE) {
      if(grid.size()!=1||grid.get(0)==null)return false;
      ItemStack input=RecipeNative.nativeStack(grid.get(0)),result;
      try{result=(ItemStack)Class.forName("net.modificationstation.stationapi.api.recipe.SmeltingRegistry")
          .getMethod("getResultFor",ItemStack.class).invoke(null,input);}
      catch(ReflectiveOperationException failure){throw new IOException("Native smelting adapter unavailable",failure);}
      return Objects.equals(encode(result),encode(recipe.output));
    }
    class_159 scratch=scratch(context,grid);
    for(Object object:class_148.method_538().method_541())if(object instanceof class_160 nativeRecipe&&nativeRecipe.method_2072(scratch))
      return nativeRecipe==recipe.nativeRecipe&&Objects.equals(encode(nativeRecipe.method_2074(scratch)),encode(recipe.output));
    return false;
  }
}
