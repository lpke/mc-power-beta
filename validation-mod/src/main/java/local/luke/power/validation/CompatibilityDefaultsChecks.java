package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.Files;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.storage.PowerConfig;
import net.glasslauncher.mods.gcapi3.impl.GCCore;
import net.glasslauncher.mods.gcapi3.impl.object.*;
import net.minecraft.Item;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;

final class CompatibilityDefaultsChecks {
  static void run(Minecraft mc)throws Exception {
    failures=0;mc.player.closeScreen();mc.setScreen(null);DeathChecks.alive(mc);
    var root=GCCore.MOD_CONFIGS.get("power_compat:config").configCategoryHandler();
    var sizes=(ConfigCategoryHandler)root.values.values().stream().filter(v->v.id.equals("STACK_SIZE_CONFIG")).findFirst().orElseThrow();
    test("live door/sign limits obey vanilla server authority and retain saved preferences and oversized stacks",()->{
      byte[] before=Files.readAllBytes(PowerConfig.path());
      for(var spec:List.of(new Object[]{"enableModernWoodDoorStackSize",324,64},new Object[]{"enableModernIronDoorStackSize",330,64},new Object[]{"enableModernSignStackSize",323,16})) {
        var entry=(ConfigEntryHandler<?>)sizes.values.values().stream().filter(v->v.id.equals(spec[0])).findFirst().orElseThrow();
        Object prior=entry.value;int id=(int)spec[1],limit=(int)spec[2];var held=DeathChecks.tagged(id,23,0,"oversized");String bytes=DeathItems.encode(held);
        try {
          entry.reset(true);check(Item.ITEMS[id].getMaxCount()==limit,"live limit missing "+id);
          entry.vanillaServerBehavior();check(Item.ITEMS[id].getMaxCount()==1&&entry.multiplayerLoaded,"vanilla limit missing "+id);
          entry.undoVanillaServerBehaviour();check(Item.ITEMS[id].getMaxCount()==limit&&!entry.multiplayerLoaded,"local limit not restored "+id);
          entry.reset(false);check(Item.ITEMS[id].getMaxCount()==1&&bytes.equals(DeathItems.encode(held)),"disabling changed stack "+id);
        } finally {entry.undoVanillaServerBehaviour();entry.reset(prior);}
      }
      check(Arrays.equals(before,Files.readAllBytes(PowerConfig.path())),"server overrides persisted");
    });
    test("crafting inputs save full native data and counts, including disabled preference and legacy reads",()->{
      var entry=(ConfigEntryHandler<?>)root.values.values().stream().filter(v->v.id.equals("allowCraftingInventorySlots")).findFirst().orElseThrow();
      Object preference=entry.value;var original=new NbtCompound();mc.player.writeNbt(original);
      try {
        for(boolean enabled:new boolean[]{true,false}) {
          entry.reset(enabled);var expected=new ArrayList<String>();
          for(int i=0;i<4;i++) {
            var stack=DeathChecks.tagged(35,201+i,i,"crafting-save-"+i);
            stack.getClass().getMethod("spc$setStr",String.class).invoke(stack,"Kept name "+i);
            mc.player.playerContainer.method_2084(i+1).setStack(stack);expected.add(DeathItems.encode(stack));
          }
          var saved=new NbtCompound();mc.player.writeNbt(saved);
          check(saved.getCompound("power_crafting_inputs").values().size()==4,"recipe output saved or missing inputs");
          for(int i=1;i<=4;i++)mc.player.playerContainer.method_2084(i).setStack(null);
          mc.player.readtNbt(saved);
          for(int i=0;i<4;i++)check(expected.get(i).equals(DeathItems.encode(mc.player.playerContainer.method_2084(i+1).getStack())),"input data lost "+i+" enabled="+enabled);
          var legacy=new NbtCompound();
          for(Object value:saved.values()) {var tag=(NbtElement)value;if(!tag.getKey().equals("power_crafting_inputs"))legacy.put(tag.getKey(),tag);}
          legacy.putInt("ItemID_Slot0",35);legacy.putInt("ItemAmount_Slot0",7);legacy.putInt("ItemDamage_Slot0",5);
          mc.player.readtNbt(legacy);var recovered=mc.player.playerContainer.method_2084(1).getStack();
          check(recovered.itemId==35&&recovered.count==7&&recovered.getDamage()==5,"legacy input lost");
          for(int i=2;i<=4;i++)check(mc.player.playerContainer.method_2084(i).getStack()==null,"stale input remained");
        }
        entry.vanillaServerBehavior();check(Boolean.FALSE.equals(entry.value)&&entry.multiplayerLoaded,"vanilla grid preference not blocked");entry.undoVanillaServerBehaviour();
      } finally {entry.undoVanillaServerBehaviour();entry.reset(preference);mc.player.readtNbt(original);}
    });
    log("COMPATIBILITY DEFAULTS FAILURES "+failures);
  }
}
