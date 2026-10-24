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

/** A visit-local inventory modal that leaves uncovered minimap controls available. */
public final class MinimapQuickMenu extends local.luke.power.ui.UiScreen {
  private static final Object OWNER = new Object();
  private static final MinimapQuickMenu VIEW = new MinimapQuickMenu();
  private static Screen host;
  private static Object world, player;
  private static boolean open;
  private static int firstRow;
  private static int ownedButtons;
  private static long ownedKeyEvent = Long.MIN_VALUE;
  private static long frame, checkedFrame = Long.MIN_VALUE;
  private static Screen checkedHost;
  private static MinimapQuickMenuLayout.Rect checkedGear;
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
    int labelWidth = MapTogglePanel.labelWidth(mc);
    layoutHost=screen;layoutSettings=settings;layoutFont=mc.textRenderer;layoutWidth=screen.width;layoutHeight=screen.height;
    return cachedLayout=MinimapQuickMenuLayout.at(Minimap.layout(settings,screen.width,screen.height,false),
        settings.circular,settings.corner,screen.width,screen.height,labelWidth);
  }
  private static boolean available(Minecraft mc) {
    return Minimap.inventoryVisible(mc) && Maps.cache()!=null && !Mouse.isGrabbed()
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
    for(int y=gear.y();y<gear.y()+gear.height();y++)
      for(int x=gear.x();x<gear.x()+gear.width();x++)
        if(gear.containsCircle(x,y)&&gui.power$containsGuiSpace(x,y))return false;
    return gearClear=true;
  }
  public static boolean owns(Minecraft mc,int x,int y) {
    if(active(mc))return true;
    if(!available(mc))return false;
    var layout=layout(mc);return layout.gear().containsCircle(x,y)&&gearAvailable(mc,layout);
  }
  /** The quick-menu modal blocks inventory actions but admits its own uncovered map. */
  public static boolean blocksMapInput(Minecraft mc,int x,int y) {
    if(InventoryPanels.modalOther(mc.currentScreen,OWNER))return true;
    if(!active(mc))return false;
    var layout=layout(mc);
    return layout.menu().contains(x,y)||layout.gear().containsCircle(x,y);
  }
  private static boolean overMap(Minecraft mc,int x,int y) {
    return Minimap.inventoryContains(mc,x,y)&&mc.currentScreen instanceof InventoryGui gui
        &&!gui.power$containsGuiSpace(x,y);
  }
  public static boolean mouse(Minecraft mc,int x,int y,int button,boolean down) {
    boolean wasOpen=active(mc);
    if(host!=mc.currentScreen)ownedButtons=0;
    if(button>=0&&button<16&&!down&&(ownedButtons&(1<<button))!=0) {ownedButtons&=~(1<<button);return true;}
    if(!Display.isActive()) return wasOpen;
    if(wasOpen) {
      var layout=layout(mc);
      // Keep the menu open while wheel/left/right/middle events reach the map.
      // Menu geometry remains an extra GUI panel and wins where they overlap.
      if(!layout.menu().contains(x,y)&&!layout.gear().containsCircle(x,y)&&overMap(mc,x,y))return false;
      int wheel=Mouse.getEventDWheel();
      if(wheel!=0&&layout.menu().contains(x,y))firstRow=layout.panel(firstRow).scroll(wheel);
      if(button>=0&&button<16&&down)ownedButtons|=1<<button;
      if(button>=0&&down) {
        int row=layout.panel(firstRow).rowAt(x,y);
        if(button==0&&row>=0) { toggle(row); click(mc); }
        else if(!layout.menu().contains(x,y)||layout.gear().containsCircle(x,y)) {close();click(mc);}
      }
      return true;
    }
    if(button!=0||!down||!available(mc))return false;
    var layout=layout(mc);
    if(!layout.gear().containsCircle(x,y)||!gearAvailable(mc,layout))return false;
    host=mc.currentScreen;world=mc.world;player=mc.player;open=true;firstRow=0;ownedButtons|=1;
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
    MapToggle.ALL.get(row).toggleMinimap();
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
      GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_CULL_FACE);
      int mx=Display.isActive()?Mouse.getX()*mc.currentScreen.width/mc.displayWidth:-1;
      int my=Display.isActive()?mc.currentScreen.height-Mouse.getY()*mc.currentScreen.height/mc.displayHeight-1:-1;
      boolean hovered=gear.containsCircle(mx,my)||open;
      circle(gear,8,0xff151515);
      circle(gear,7,hovered?0xffa6a693:0xff8b8b8b);
      circle(gear,6,hovered?0xff65655a:0xff4d4d4d);
      int colour=hovered?0xffffe0a0:0xffe0e0e0;
      for(int line=0;line<3;line++) {
        int y=gear.y()+4+line*3, knob=gear.x()+(line==1?9:5);
        VIEW.fill(gear.x()+4,y+1,gear.x()+12,y+2,0xff303030);
        VIEW.fill(gear.x()+4,y,gear.x()+12,y+1,colour);
        VIEW.fill(knob,y-1,knob+2,y+2,colour);
      }
    }finally{GL11.glPopAttrib();}
  }
  private static void circle(MinimapQuickMenuLayout.Rect gear,double radius,int colour) {
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glColor4f((colour>>16&255)/255f,(colour>>8&255)/255f,(colour&255)/255f,(colour>>>24)/255f);
    GL11.glBegin(GL11.GL_TRIANGLE_FAN);
    double x=gear.x()+gear.width()/2d,y=gear.y()+gear.height()/2d;
    GL11.glVertex2d(x,y);
    for(int i=0;i<=24;i++)GL11.glVertex2d(x+radius*CIRCLE_X[i],y+radius*CIRCLE_Y[i]);
    GL11.glEnd();GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glColor4f(1,1,1,1);
  }
  private static final double[] CIRCLE_X=new double[25],CIRCLE_Y=new double[25];
  static {for(int i=0;i<=24;i++){CIRCLE_X[i]=Math.cos(i*Math.PI/12);CIRCLE_Y[i]=Math.sin(i*Math.PI/12);}}
  public static void renderMenu(Minecraft mc,int mouseX,int mouseY) {
    if(!active(mc))return;
    prepare(mc);
    GL11.glPushMatrix();
    try {
      GL11.glTranslatef(0,0,700);
      MapTogglePanel.draw(mc,layout(mc).panel(firstRow),mouseX,mouseY,true);
    }finally{GL11.glPopMatrix();}
  }
}
