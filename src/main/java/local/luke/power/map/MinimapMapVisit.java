package local.luke.power.map;

import local.luke.power.input.*;
import local.luke.power.ui.WorldMapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;

/** Retain the native container during a map visit, including its cursor and crafting inputs. */
public final class MinimapMapVisit {
  private static Screen host;
  private static Object world,player,container;
  private static boolean opening;
  private MinimapMapVisit() {}
  public static boolean open(Minecraft mc,int x,int y) {
    if(!MapConfig.current().worldMap||!Minimap.inventoryContains(mc,x,y)
        ||!(mc.currentScreen instanceof InventoryGui gui)||gui.power$containsGuiSpace(x,y)
        ||InventoryPanels.modal(mc.currentScreen)||!org.lwjgl.opengl.Display.isActive()
        ||org.lwjgl.input.Mouse.isGrabbed())return false;
    host=mc.currentScreen;world=mc.world;player=mc.player;
    container=host instanceof ContainerScreen screen?screen.container:null;
    if(host instanceof InventoryDrag drag)drag.power$cancelItemDrag();
    if(host instanceof CreativePaint paint)paint.power$cancelCreativePaint();
    TweakLifecycle.register("minimap-map-visit",()->abandon(mc));
    opening=true;
    try {mc.setScreen(new WorldMapScreen(host));}
    finally {opening=false;}
    return true;
  }
  public static boolean retains(Screen screen) {return opening&&host==screen;}
  /** Map children may finish at gameplay or open a different map. Keep one native owner. */
  public static Screen transition(Minecraft mc,Screen next) {
    if(host==null||opening)return next;
    if(next==null||next==host)return returning(mc,host);
    if(!(next instanceof local.luke.power.ui.UiScreen))abandon(mc);
    return next;
  }
  public static Screen returning(Minecraft mc,Screen parent) {
    if(parent!=host)return parent;
    boolean valid=mc.world==world&&mc.player==player&&mc.player!=null
        &&(container==null||mc.player.container==container)&&!mc.player.dead&&mc.player.health>0;
    Screen result=valid?host:null;
    if(valid)clear();else abandon(mc);
    return result;
  }
  private static void clear(){host=null;world=player=container=null;}
  private static void abandon(Minecraft mc) {
    Screen previous=host;
    boolean owned=mc.world==world&&mc.player==player&&mc.player!=null
        &&container!=null&&mc.player.container==container;
    clear();
    if(owned)previous.removed();
  }
}
