package local.luke.power.map;

import java.util.List;
import local.luke.power.input.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** A visit-local modal over the unchanged native inventory. Writes only queued preferences. */
public final class MinimapQuickMenu extends Screen {
  private static final Object OWNER = new Object();
  private static final MinimapQuickMenu VIEW = new MinimapQuickMenu();
  private static final String[] LABELS = {"Show items", "Show mobs", "Show waypoints", "Show in-game waypoints"};
  private static Screen host;
  private static Object world, player;
  private static boolean open;
  private static int ownedButtons;
  private static long ownedKeyEvent = Long.MIN_VALUE;
  private static long frame, checkedFrame = Long.MIN_VALUE;
  private static Screen checkedHost;
  private static MinimapQuickMenuLayout.Circle checkedGear;
  private static boolean gearClear;
  private static Screen layoutHost;
  private static MapSettings layoutSettings;
  private static Object layoutFont;
  private static int layoutWidth,layoutHeight;
  private static MinimapQuickMenuLayout.Layout cachedLayout;
  private static MinimapQuickMenuLayout.Rect publishedMenu;
  private MinimapQuickMenu() {}
  public static void beginFrame() { frame++; }
  private static MinimapQuickMenuLayout.Layout layout(Minecraft mc) {
    var screen=mc.currentScreen;var settings=MapConfig.current();
    if(cachedLayout!=null&&layoutHost==screen&&layoutSettings==settings&&layoutFont==mc.textRenderer
        &&layoutWidth==screen.width&&layoutHeight==screen.height)return cachedLayout;
    int labelWidth = java.util.Arrays.stream(LABELS).mapToInt(mc.textRenderer::getWidth).max().orElse(120);
    layoutHost=screen;layoutSettings=settings;layoutFont=mc.textRenderer;layoutWidth=screen.width;layoutHeight=screen.height;
    return cachedLayout=MinimapQuickMenuLayout.at(Minimap.layout(settings,screen.width,screen.height,false),
        settings.circular,screen.width,screen.height,labelWidth);
  }
  private static boolean available(Minecraft mc) {
    return Minimap.inventoryVisible(mc) && Maps.cache()!=null && Display.isActive() && !Mouse.isGrabbed()
        && !InventoryPanels.modalOther(mc.currentScreen,OWNER);
  }
  public static boolean active(Minecraft mc) {
    if (open && (host!=mc.currentScreen || world!=mc.world || player!=mc.player || !available(mc))) close();
    if(open) {
      var menu=layout(mc).menu();
      publish(menu);
    }
    return open;
  }
  private static void close() {
    if(host!=null) { InventoryPanels.modal(host,OWNER,false); InventoryPanels.removeExtra(host,OWNER); }
    open=false;
    publishedMenu=null;
  }
  private static void publish(MinimapQuickMenuLayout.Rect menu) {
    if(menu.equals(publishedMenu))return;
    InventoryPanels.extra(host,OWNER,List.of(new InventoryPanels.Rect(menu.x(),menu.y(),menu.width(),menu.height())));
    publishedMenu=menu;
  }
  private static boolean gearAvailable(Minecraft mc, MinimapQuickMenuLayout.Layout layout) {
    if (!available(mc) || !(mc.currentScreen instanceof InventoryGui gui)) return false;
    var gear=layout.gear();
    if(checkedFrame==frame&&checkedHost==mc.currentScreen&&gear.equals(checkedGear))return gearClear;
    checkedFrame=frame;checkedHost=mc.currentScreen;checkedGear=gear;gearClear=false;
    for(int y=gear.y()-gear.radius();y<=gear.y()+gear.radius();y++)
      for(int x=gear.x()-gear.radius();x<=gear.x()+gear.radius();x++)
        if(gear.contains(x,y)&&gui.power$containsGuiSpace(x,y))return false;
    return gearClear=true;
  }
  public static boolean owns(Minecraft mc,int x,int y) {
    if(active(mc))return true;
    if(!available(mc))return false;
    var layout=layout(mc);return layout.gear().contains(x,y)&&gearAvailable(mc,layout);
  }
  public static boolean mouse(Minecraft mc,int x,int y,int button,boolean down) {
    boolean wasOpen=active(mc);
    if(host!=mc.currentScreen)ownedButtons=0;
    if(button>=0&&button<16&&!down&&(ownedButtons&(1<<button))!=0) {ownedButtons&=~(1<<button);return true;}
    if(wasOpen) {
      if(button>=0&&button<16&&down)ownedButtons|=1<<button;
      if(button>=0&&down) {
        var layout=layout(mc);int row=layout.rowAt(x,y);
        if(button==0&&row>=0) { toggle(row); click(mc); }
        else if(!layout.menu().contains(x,y)||layout.gear().contains(x,y)) {close();click(mc);}
      }
      return true;
    }
    if(button!=0||!down||!available(mc))return false;
    var layout=layout(mc);
    if(!layout.gear().contains(x,y)||!gearAvailable(mc,layout))return false;
    host=mc.currentScreen;world=mc.world;player=mc.player;open=true;ownedButtons|=1;
    if(host instanceof InventoryDrag drag)drag.power$cancelItemDrag();
    if(host instanceof CreativePaint paint)paint.power$cancelCreativePaint();
    InventoryPanels.modal(host,OWNER,true);
    publish(layout.menu());
    click(mc);return true;
  }
  public static boolean key(Minecraft mc,int key,boolean down) {
    if(!active(mc))return false;
    ownedKeyEvent=Keyboard.getEventNanoseconds();
    if(down&&key==Keyboard.KEY_ESCAPE){close();click(mc);}
    return true;
  }
  public static boolean ownsKeyEvent() { return ownedKeyEvent==Keyboard.getEventNanoseconds(); }
  static void toggle(int row) {
    var copy=MapConfig.copy();
    switch(row) {
      case 0 -> copy.items=!copy.items;
      case 1 -> {boolean enabled=!(copy.hostile&&copy.animals);copy.hostile=copy.animals=enabled;}
      case 2 -> copy.waypoints=!copy.waypoints;
      case 3 -> copy.worldWaypoints=!copy.worldWaypoints;
      default -> throw new IllegalArgumentException("Unknown minimap toggle");
    }
    MapConfig.queued(copy);
  }
  private static void click(Minecraft mc){mc.soundManager.method_2009("random.click",1,1);}
  private static void prepare(Minecraft mc){VIEW.init(mc,mc.currentScreen.width,mc.currentScreen.height);}
  public static void renderButton(Minecraft mc) {
    active(mc);
    if(!available(mc))return;
    var layout=layout(mc);if(!open&&!gearAvailable(mc,layout))return;
    prepare(mc);var gear=layout.gear();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_LIGHTING);
      for(int dy=-gear.radius();dy<=gear.radius();dy++) {
        int half=(int)Math.sqrt(gear.radius()*gear.radius()-dy*dy);
        VIEW.fill(gear.x()-half,gear.y()+dy,gear.x()+half+1,gear.y()+dy+1,0xe0202020);
      }
      // Eight teeth and the hollow center stay legible at the native GUI scale.
      for(int index=0;index<8;index++) {
        double angle=index*Math.PI/4;int x=gear.x()+(int)Math.round(Math.cos(angle)*4),y=gear.y()+(int)Math.round(Math.sin(angle)*4);
        VIEW.fill(x-1,y-1,x+2,y+2,0xffcccccc);
      }
      VIEW.fill(gear.x()-2,gear.y()-2,gear.x()+3,gear.y()+3,0xffcccccc);
      VIEW.fill(gear.x()-1,gear.y()-1,gear.x()+2,gear.y()+2,0xff202020);
    }finally{GL11.glPopAttrib();}
  }
  public static void renderMenu(Minecraft mc,int mouseX,int mouseY) {
    if(!active(mc))return;
    prepare(mc);var layout=layout(mc);var menu=layout.menu();var settings=MapConfig.current();
    boolean[] values={settings.items,settings.hostile&&settings.animals,settings.waypoints,settings.worldWaypoints};
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_LIGHTING);GL11.glTranslatef(0,0,700);
      VIEW.fill(menu.x()-1,menu.y()-1,menu.x()+menu.width()+1,menu.y()+menu.height()+1,0xff808080);
      VIEW.fill(menu.x(),menu.y(),menu.x()+menu.width(),menu.y()+menu.height(),0xf0181818);
      for(int index=0;index<4;index++) {
        var row=layout.row(index);boolean hovered=row.contains(mouseX,mouseY);
        if(hovered)VIEW.fill(row.x(),row.y(),row.x()+row.width(),row.y()+row.height(),0xff383838);
        VIEW.fill(row.x()+3,row.y()+5,row.x()+13,row.y()+15,0xffa0a0a0);
        VIEW.fill(row.x()+4,row.y()+6,row.x()+12,row.y()+14,0xff101010);
        if(values[index])VIEW.drawStringWithShadow(mc.textRenderer,"x",row.x()+5,row.y()+6,0xffffff);
        VIEW.drawStringWithShadow(mc.textRenderer,LABELS[index],row.x()+19,row.y()+6,hovered?0xffffff:0xdddddd);
      }
    }finally{GL11.glPopMatrix();GL11.glPopAttrib();}
  }
}
