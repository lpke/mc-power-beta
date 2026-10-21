package local.luke.power.offhand;

import java.io.IOException;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.PlayerMode;
import local.luke.power.inventory.*;
import net.minecraft.class_500;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/** Beta armor has no use callback. Native equipment slots still own acceptance and limits. */
public final class OffhandEquipment {
  private static Object pendingWorld,pendingPlayer,pendingStack;
  private static HandUse.Hand pendingHand;
  private static long pendingPress,pendingHands;
  private OffhandEquipment() {}
  /** A held repeat cannot invalidate the lease of its own slow durable equip. */
  public static boolean pending(Minecraft mc,int button) {
    if(button!=1||pendingStack==null||!NativeInventoryPlan.busy()||!Offhand.playable(mc)||mc.currentScreen!=null
        ||pendingPress!=OffhandUse.pressRevision()||pendingHands!=HandUse.manualRevision()
        ||mc.world!=pendingWorld||mc.player!=pendingPlayer)return false;
    return Offhand.stack(mc.player,pendingHand)==pendingStack;
  }
  public static Boolean use(Minecraft mc,HandUse.Hand hand,ItemStack stack) {
    if(stack.getItem().getClass()!=class_500.class||!(stack.getItem() instanceof class_500 armor)
        ||!Boolean.TRUE.equals(local.luke.power.controls.ControlFeatures.GAMEPLAY_CONFIG.rightClickEquipArmor))return null;
    int equipment=armor.field_2083;
    if(equipment<0||equipment>3||!Offhand.playable(mc)||mc.currentScreen!=null)return false;
    if(!NativeInventoryPlan.busy())pendingStack=null;
    if(pendingStack==stack&&pendingHand==hand&&pendingWorld==mc.world&&pendingPlayer==mc.player
        &&pendingPress==OffhandUse.pressRevision()&&pendingHands==HandUse.manualRevision())return true;
    var player=mc.player;var inventory=player.inventory;
    var slot=player.playerContainer.method_2084(5+equipment);
    if(!slot.canInsert(stack))return false;
    ItemStack worn=slot.getStack();
    try {
      if(worn!=null&&identity(worn).equals(identity(stack)))return false;
      boolean creative=player instanceof PlayerMode mode&&mode.power$creative();
      ItemStack piece=DeathItems.decode(DeathItems.encode(stack));piece.count=1;
      ItemStack held;
      List<NativeInventoryPlan.Change> changes=new ArrayList<>();
      if(stack.count==1)held=creative&&worn==null?stack:worn;
      else {
        held=DeathItems.decode(DeathItems.encode(stack));if(!creative)held.count--;
        if(worn!=null) {
          // Plan the old equipment completely before touching either hand.
          ItemStack remainder=DeathItems.decode(DeathItems.encode(worn));
          for(int i=0;i<36&&remainder.count>0;i++) {
            if(hand==HandUse.Hand.MAIN&&i==inventory.selectedSlot)continue;
            ItemStack current=inventory.main[i];
            if(current!=null&&(!current.isStackable()||!identity(current).equals(identity(remainder))))continue;
            int room=Math.max(0,Math.min(remainder.getMaxCount(),inventory.getMaxCountPerStack())-(current==null?0:current.count));
            int amount=Math.min(room,remainder.count);if(amount==0)continue;
            ItemStack next=DeathItems.decode(DeathItems.encode(current==null?remainder:current));next.count=current==null?amount:current.count+amount;
            changes.add(NativeInventoryPlan.Change.main(inventory,i,next));remainder.count-=amount;
          }
          // Existing oversized equipment is preserved if its normal return slots are full.
          if(remainder.count>0)return false;
        }
      }
      changes.add(hand==HandUse.Hand.OFF?NativeInventoryPlan.Change.offhand(inventory,held)
          :NativeInventoryPlan.Change.main(inventory,inventory.selectedSlot,held));
      changes.add(NativeInventoryPlan.Change.slot(slot,piece));
      boolean accepted=NativeInventoryPlan.submitEquipment(mc,"equip_"+hand.name().toLowerCase(Locale.ROOT),changes,
          ()->Offhand.playable(mc)&&mc.currentScreen==null&&mc.player.inventory.getCursorStack()==null,
          ()->{pendingStack=null;if(hand==HandUse.Hand.OFF)OffhandRenderer.changed();},creative&& (stack.count>1||worn==null));
      if(accepted){pendingWorld=mc.world;pendingPlayer=player;pendingStack=stack;pendingHand=hand;
        pendingPress=OffhandUse.pressRevision();pendingHands=HandUse.manualRevision();}
      return accepted;
    } catch(IOException failure) { throw new IllegalStateException("Equipment could not preserve native item data",failure); }
  }
  private static String identity(ItemStack stack)throws IOException { ItemStack copy=stack.clone();copy.count=1;return DeathItems.encode(copy); }
}
