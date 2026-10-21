package local.luke.power.recipes;

import java.util.*;
import local.luke.power.input.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.inventory.Container;
import net.minecraft.screen.slot.Slot;

/** Explicit native layouts. Handler slot45 is never assumed to mean offhand. */
public final class RecipeContext {
  public final Container handler;
  public final RecipeDefinition.Kind kind;
  public final int width,height;
  public final List<Slot> inputs;
  public final Slot output;
  public final class_159 crafting;
  private boolean readOnly;
  private RecipeContext(Container handler,RecipeDefinition.Kind kind,int width,int height,int first,int count,int output,class_159 crafting) {
    this.handler=handler;this.kind=kind;this.width=width;this.height=height;this.crafting=crafting;
    List<Slot> slots=new ArrayList<>();for(int i=0;i<count;i++)slots.add(handler.method_2084(first+i));inputs=List.copyOf(slots);
    this.output=handler.method_2084(output);
  }
  public static RecipeContext of(ContainerScreen screen) {
    Container handler=screen.container;
    if(screen.getClass()==class_585.class&&handler.getClass()==class_277.class)
      return new RecipeContext(handler,RecipeDefinition.Kind.CRAFTING,2,2,1,4,0,((class_277)handler).field_1124);
    if(screen.getClass()==class_516.class&&handler.getClass()==class_196.class)
      return new RecipeContext(handler,RecipeDefinition.Kind.CRAFTING,3,3,1,9,0,((class_196)handler).field_708);
    if(screen.getClass()==class_524.class&&handler.getClass()==class_337.class)
      return new RecipeContext(handler,RecipeDefinition.Kind.FURNACE,1,1,0,1,2,null);
    return null;
  }
  public static RecipeContext viewer(Minecraft mc,RecipeDefinition.Kind kind) {
    RecipeContext view=new RecipeContext(mc.player.playerContainer,kind,kind==RecipeDefinition.Kind.CRAFTING?3:1,
        kind==RecipeDefinition.Kind.CRAFTING?3:1,1,kind==RecipeDefinition.Kind.CRAFTING?4:0,0,null);view.readOnly=true;return view;
  }
  public boolean writable(Minecraft mc) {
    boolean slots=inputs.stream().allMatch(slot->slot.id>=0&&slot.id<handler.slots.size()&&handler.method_2084(slot.id)==slot);
    return !readOnly&&slots&&mc.currentScreen instanceof ContainerScreen screen&&screen.container==handler
        &&mc.world!=null&&!mc.world.isRemote&&mc.player!=null&&mc.player.world==mc.world&&mc.player.container==handler
        &&mc.player.health>0&&!mc.player.dead&&mc.field_2807==mc.player&&!DetachedCamera.isActive()
        &&!InteractionState.carryingContainer&&!(mc.player instanceof PlayerMode mode&&mode.power$spectator())
        &&mc.player.inventory.getCursorStack()==null&&handler.method_2094(mc.player)
        &&local.luke.power.world.WorldCompatibility.ready(mc.world);
  }
}
