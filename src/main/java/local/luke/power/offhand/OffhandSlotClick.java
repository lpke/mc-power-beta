package local.luke.power.offhand;

import local.luke.power.input.InventoryPanels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.screen.slot.Slot;

/** Native outside-click handling admits the extra slot only for its originating press. */
public final class OffhandSlotClick implements AutoCloseable {
  private record Target(ContainerScreen screen, Slot slot, Object player, Object world) {}
  private static final ThreadLocal<Target> CURRENT = new ThreadLocal<>();
  private final Target previous;
  private OffhandSlotClick(Target next) { previous=CURRENT.get(); if(next==null)CURRENT.remove();else CURRENT.set(next); }
  public static OffhandSlotClick begin(Minecraft mc,ContainerScreen screen,Slot slot,int x,int y,int button) {
    boolean valid=slot instanceof OffhandNativeSlot && (button==0||button==1) && mc.currentScreen==screen
        && mc.player!=null && mc.world!=null && !mc.world.isRemote
        && !InventoryPanels.modal(screen) && !InventoryPanels.contains(screen,x,y);
    return new OffhandSlotClick(valid?new Target(screen,slot,mc.player,mc.world):null);
  }
  public static int nativeSlot(Minecraft mc,int slotId) {
    Target target=CURRENT.get();
    return slotId==-999 && target!=null && mc.currentScreen==target.screen && mc.player!=null
        && mc.player==target.player && mc.world==target.world && !mc.world.isRemote
        && mc.player.container==target.screen.container && target.slot.id>=0
        && target.slot.id<target.screen.container.slots.size()
        && target.screen.container.method_2084(target.slot.id)==target.slot ? target.slot.id : slotId;
  }
  @Override public void close() { if(previous==null)CURRENT.remove();else CURRENT.set(previous); }
}
