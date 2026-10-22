package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.InventoryPanels;
import local.luke.power.inventory.SpectatorContainerScreen;
import local.luke.power.rules.GameRule;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.class_137;
import net.minecraft.class_138;

final class SpectatorContainer404Checks {
  static void run(Minecraft mc)throws Exception {
    check(mc.player!=null&&mc.world!=null&&!mc.world.isRemote,"disposable world required");
    failures=0;var rules=AccessRules.copy(mc);var previous=mc.currentScreen;var handler=mc.player.container;
    String mode=Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    String before=DeathItems.inventory(DeathItems.snapshot(mc.player.inventory,mc.player.playerContainer));
    var constructor=SpectatorContainerScreen.class.getDeclaredConstructor(Minecraft.class,List.class);constructor.setAccessible(true);
    var reports=Path.of("power-beta-data/reports/spectator-container-404");Files.createDirectories(reports);
    try {
      AccessRules.set(mc,GameRule.ALLOW_SPECTATOR_MODE,true);CreativeVehicleChecks.mode(mc,"SPECTATOR");
      for(Inventory inventory:List.of(new DisplaySource(27),new DisplaySource(54),new class_138(),new class_137())) {
        test("SPECTATOR404 read-only "+inventory.getClass().getSimpleName()+" size="+inventory.size(),()->{
          var screen=constructor.newInstance(mc,List.of(inventory));mc.currentScreen=screen;screen.init(mc,640,360);
          check(mc.player.container==handler,"viewer installed its display handler on the player");
          int count=screen.container.slots.size();var snapshot=new ArrayList<String>();
          for(int i=0;i<inventory.size();i++)snapshot.add(inventory.getStack(i)==null?null:DeathItems.encode(inventory.getStack(i)));
          for(int i=0;i<count;i++) {
            var slot=screen.container.method_2084(i);var copy=slot.getStack();if(copy!=null)copy.count=1;
            for(int button:new int[]{0,1,2}) {
              ((ScreenInput)(Object)screen).power$click(232+slot.x,97+slot.y,button);
              screen.container.onSlotClick(i,button,true,mc.player);
            }
            screen.container.method_2086(i);
          }
          screen.container.onSlotClick(-999,0,false,mc.player);screen.container.method_2087(mc.player);
          for(int i=0;i<inventory.size();i++)check(Objects.equals(snapshot.get(i),inventory.getStack(i)==null?null:DeathItems.encode(inventory.getStack(i))),"source item changed");
          check(before.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory,mc.player.playerContainer))),"viewer changed personal inventory");
          SettingsSnapshot.render(mc,screen,640,360,reports.resolve("container-"+inventory.size()+".png"));
          screen.removed();InventoryPanels.remove(screen);
          check(mc.player.container==handler,"viewer closure changed the native owner");
        });
      }
    }finally{mc.currentScreen=previous;mc.player.container=handler;AccessRules.restore(mc,rules);CreativeVehicleChecks.mode(mc,mode);}
    check(failures==0,"Spectator container failures="+failures);
  }
  private static final class DisplaySource implements Inventory {
    private final ItemStack[] items;
    DisplaySource(int size)throws Exception{items=new ItemStack[size];items[0]=DeathChecks.tagged(35,23,5,"viewer-native-data");}
    public int size(){return items.length;}
    public ItemStack getStack(int slot){return items[slot];}
    public void setStack(int slot,ItemStack value){items[slot]=value;}
    public ItemStack removeStack(int slot,int amount){throw new AssertionError("viewer removed a real stack");}
    public String getName(){return "Chest";}
    public int getMaxCountPerStack(){return 64;}
    public void markDirty(){throw new AssertionError("viewer dirtied the source");}
    public boolean canPlayerUse(PlayerEntity player){return true;}
  }
}
