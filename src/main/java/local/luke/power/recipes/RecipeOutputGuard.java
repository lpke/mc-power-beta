package local.luke.power.recipes;

import java.io.IOException;
import java.util.Objects;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.mixin.DeathSlotAccessor;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/** Guard derived crafting results and journal owned furnace outputs when returning them. */
public final class RecipeOutputGuard {
  private final Minecraft mc;
  private final Object world,player,inventory;
  private final RecipeContext context;
  private final Inventory owner;
  private final int index;
  private final String before;
  private ItemStack latest;
  private String latestData;
  public RecipeOutputGuard(Minecraft mc,RecipeContext context)throws IOException {
    this.mc=mc;this.context=context;world=mc.world;player=mc.player;inventory=mc.player.inventory;
    var access=(DeathSlotAccessor)context.output;owner=access.power$deathInventory();index=access.power$deathIndex();
    if(context.kind==RecipeDefinition.Kind.CRAFTING?(owner.getClass()!=class_507.class||index!=0)
        :(owner.getClass()!=class_138.class||index!=2))throw new IOException("Unsupported recipe result inventory");
    latest=context.output.getStack();before=latestData=encode(latest);
  }
  private static String encode(ItemStack stack)throws IOException{return stack==null?null:DeathItems.encode(stack);}
  public boolean matches() {
    try{return owned()&&context.output.getStack()==latest&&Objects.equals(latestData,encode(latest));}
    catch(IOException failure){return false;}
  }
  private boolean owned() {
    return mc.world==world&&mc.player==player&&mc.player!=null&&mc.player.inventory==inventory
        &&mc.player.container==context.handler&&context.output.id<context.handler.slots.size()
        &&context.handler.method_2084(context.output.id)==context.output&&((DeathSlotAccessor)context.output).power$deathInventory()==owner;
  }
  private void observe() {
    try{latest=context.output.getStack();latestData=encode(latest);}
    catch(IOException failure){throw new IllegalStateException("Recipe result changed to unsafe native data",failure);}
  }
  public NativeInventoryPlan.Change input(Slot slot,ItemStack next) {
    var base=NativeInventoryPlan.Change.slot(slot,next);
    return new NativeInventoryPlan.Change(base.label(),base.owner(),base.index(),base.get(),stack->{
      try{base.set().accept(stack);}finally{observe();}
    },base.accepts(),base.limit(),base.wholeOversize(),base.next());
  }
  public NativeInventoryPlan.Change returnedOutput() {
    if(context.kind!=RecipeDefinition.Kind.FURNACE)throw new IllegalStateException("Crafting results are not owned items");
    return new NativeInventoryPlan.Change("furnace.output",owner,index,context.output::getStack,stack->{
      try{owner.setStack(index,stack);owner.markDirty();}finally{observe();}
    },Objects::isNull,owner::getMaxCountPerStack,false,null);
  }
  public void restore() {
    try {
      // A returned furnace output is an owned item and the shared transaction restores it first.
      if(context.kind==RecipeDefinition.Kind.FURNACE&&owned()&&Objects.equals(before,encode(context.output.getStack())))return;
      if(!matches())throw new IllegalStateException("Recipe result ownership changed; recovery evidence retained");
      ItemStack original=before==null?null:DeathItems.decode(before);
      // Both exact native owners have callback-free result setters.
      owner.setStack(index,original);latest=original;latestData=before;
    }catch(IOException failure){throw new IllegalStateException("Recipe result snapshot could not be restored",failure);}
  }
}
