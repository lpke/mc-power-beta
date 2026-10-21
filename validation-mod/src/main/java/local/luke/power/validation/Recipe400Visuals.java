package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.map.*;
import local.luke.power.offhand.Offhand;
import local.luke.power.recipes.*;
import local.luke.power.ui.TextInput;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Fixed logical viewports through the installed native screens; no recipe is placed. */
final class Recipe400Visuals {
  private static final Path REPORT=Path.of("power-beta-data/reports/recipe-400-visual");
  private Recipe400Visuals() {}
  static void run(Minecraft mc)throws Exception {
    check(mc.world!=null&&mc.player!=null&&!mc.world.isRemote,"load a disposable singleplayer world first");
    check(!(mc.player instanceof PlayerMode mode&&(mode.power$creative()||mode.power$spectator())),"use Survival before the visual fixture");
    check(!NativeInventoryPlan.busy(),"finish inventory transactions before the visual fixture");
    failures=0;Files.createDirectories(REPORT);
    var previousScreen=mc.currentScreen;var previousContainer=mc.player.container;
    var main=mc.player.inventory.main;var armor=mc.player.inventory.armor;var cursor=mc.player.inventory.getCursorStack();
    int selected=mc.player.inventory.selectedSlot;boolean grabbed=Mouse.isGrabbed(),textFocused=InventoryTextInput.active(previousScreen);
    String offhand=Offhand.get(mc.player.inventory)==null?null:DeathItems.encode(Offhand.get(mc.player.inventory));
    var settings=RecipeBookConfig.copy();var screens=new ArrayList<Screen>();var manifest=new ArrayList<String>();
    int matrixMode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
    try {
      var enabled=RecipeBookConfig.copy();enabled.enabled=true;enabled.inventory=true;enabled.pauseMenu=true;
      enabled.craftingOpen=true;enabled.furnaceOpen=true;enabled.craftingFilter=false;enabled.furnaceFilter=false;RecipeBookConfig.preview(enabled);
      mc.player.inventory.main=new ItemStack[36];mc.player.inventory.armor=new ItemStack[4];mc.player.inventory.selectedSlot=0;
      mc.player.inventory.setCursorStack(null);Offhand.set(mc.player.inventory,null);Mouse.setGrabbed(false);
      ItemStack[] ingredients={new ItemStack(5,64,0),new ItemStack(280,32,0),new ItemStack(4,32,0),new ItemStack(35,8,0),
          new ItemStack(351,8,1),new ItemStack(351,8,15),new ItemStack(265,8,0),new ItemStack(17,8,0),new ItemStack(17,8,2),new ItemStack(263,8,0)};
      System.arraycopy(ingredients,0,mc.player.inventory.main,0,ingredients.length);

      var wide=player(mc);var wideBook=open(mc,wide,640,360,screens);capture(mc,wide,"player-wide-all-640",manifest);
      carried(mc,wide,wideBook,"player-wide-carried-640",manifest);
      field(wideBook,"filter",true);wideBook.tick();capture(mc,wide,"player-wide-craftable-640",manifest);
      field(wideBook,"filter",false);search(wideBook,"wool");capture(mc,wide,"player-wide-search-640",manifest);
      var search=(TextInput)field(wideBook,"search");search.focused=true;search.selectAll();wideBook.tick();
      capture(mc,wide,"player-wide-search-focused-640",manifest);search.focused=false;wideBook.tick();
      popup(wideBook);capture(mc,wide,"player-wide-variants-640",manifest);
      capture(mc,wide,"player-wide-variant-tooltip-640",manifest,(int)field(wideBook,"popupX")+15,(int)field(wideBook,"popupY")+15);
      field(wideBook,"popup",List.of());search(wideBook,"");ghost(wideBook,259);capture(mc,wide,"player-wide-ghost-640",manifest);
      field(wideBook,"open",false);wideBook.tick();capture(mc,wide,"player-wide-ghost-closed-640",manifest);

      var narrow=player(mc);var narrowBook=open(mc,narrow,320,240,screens);capture(mc,narrow,"player-narrow-all-320",manifest);
      minimap(mc,narrow,narrowBook,manifest);
      search(narrowBook,"wool");popup(narrowBook);capture(mc,narrow,"player-narrow-variants-320",manifest);
      carried(mc,narrow,narrowBook,"player-narrow-carried-320",manifest);
      field(narrowBook,"popup",List.of());search(narrowBook,"");ghost(narrowBook,259);
      field(narrowBook,"open",false);narrowBook.tick();capture(mc,narrow,"player-narrow-ghost-closed-320",manifest);

      var table=new class_516(mc.player.inventory,mc.world,(int)mc.player.x,(int)mc.player.y,(int)mc.player.z);
      var tableBook=open(mc,table,640,360,screens);capture(mc,table,"table-all-640",manifest);
      ghost(tableBook,278);capture(mc,table,"table-ghost-640",manifest);
      field(tableBook,"ghost",null);search(tableBook,"wool");popup(tableBook);capture(mc,table,"table-variants-640",manifest);

      var furnace=new class_138();furnace.setStack(1,new ItemStack(263,8,0));
      var furnaceScreen=new class_524(mc.player.inventory,furnace);var furnaceBook=open(mc,furnaceScreen,640,360,screens);
      capture(mc,furnaceScreen,"furnace-all-640",manifest);furnace.setStack(1,null);
      ghost(furnaceBook,266);capture(mc,furnaceScreen,"furnace-ghost-640",manifest);

      mc.player.container=previousContainer;var viewer=new RecipeBookViewerScreen(previousScreen);
      var viewerBook=open(mc,viewer,640,360,screens);capture(mc,viewer,"viewer-all-640",manifest);
      var detail=recipes(viewerBook).stream().filter(recipe->recipe.kind==RecipeDefinition.Kind.CRAFTING&&recipe.output.itemId==278).findFirst().orElseThrow();
      field(viewerBook,"detail",detail);viewerBook.tick();capture(mc,viewer,"viewer-detail-640",manifest);
      check(!NativeInventoryPlan.busy(),"visual capture unexpectedly queued an inventory transaction");
      check(Arrays.equals(ingredients,Arrays.copyOf(mc.player.inventory.main,ingredients.length)),"visual capture replaced fixture ingredients");
    } finally {
      for(var screen:screens){var book=RecipeBooks.book(screen);if(book!=null)book.removed();InventoryPanels.remove(screen);}
      RecipeBookConfig.preview(settings);mc.currentScreen=previousScreen;mc.player.container=previousContainer;
      mc.player.inventory.main=main;mc.player.inventory.armor=armor;mc.player.inventory.selectedSlot=selected;
      mc.player.inventory.setCursorStack(cursor);Offhand.set(mc.player.inventory,offhand==null?null:DeathItems.decode(offhand));Mouse.setGrabbed(grabbed);
      if(textFocused)InventoryTextInput.focus(previousScreen,true);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glPopAttrib();GL11.glMatrixMode(matrixMode);
    }
    Files.write(REPORT.resolve("captures.txt"),manifest);log("RECIPE400 VISUAL failures="+failures+" captures="+manifest.size()+" directory="+REPORT);
  }
  private static class_585 player(Minecraft mc){var screen=new class_585(mc.player);screen.container=new class_277(mc.player.inventory);return screen;}
  private static RecipeBookOverlay open(Minecraft mc,Screen screen,int width,int height,List<Screen> screens)throws Exception {
    screens.add(screen);mc.currentScreen=screen;if(screen instanceof ContainerScreen container)mc.player.container=container.container;
    screen.init(mc,width,height);var book=RecipeBooks.book(screen);check(book!=null&&book.enabled(),"recipe overlay missing from native screen");
    for(int i=0;i<720;i++){book.tick();var model=field(book,"model");if((int)field(model,"cursor")>=((List<?>)field(model,"recipes")).size())break;}
    field(book,"ticks",0);return book;
  }
  private static void capture(Minecraft mc,Screen screen,String name,List<String> manifest)throws Exception {
    capture(mc,screen,name,manifest,-1,-1);
  }
  private static void capture(Minecraft mc,Screen screen,String name,List<String> manifest,int mx,int my)throws Exception {
    SettingsSnapshot.render(mc,screen,screen.width,screen.height,REPORT.resolve(name+".png"),mx,my);
    manifest.add(name+".png logical="+screen.width+"x"+screen.height+" pixels="+(screen.width*2)+"x"+(screen.height*2));
  }
  private static void carried(Minecraft mc,Screen screen,RecipeBookOverlay book,String name,List<String> manifest)throws Exception {
    var previous=mc.player.inventory.getCursorStack();var carried=DeathChecks.tagged(278,3,375,"recipe-visual-cursor");
    String before=DeathItems.encode(carried);mc.player.inventory.setCursorStack(carried);
    try {
      capture(mc,screen,name,manifest,book.layout().x()+60,book.layout().y()+70);
      check(mc.player.inventory.getCursorStack()==carried&&before.equals(DeathItems.encode(carried)),"recipe rendering changed cursor identity, count, damage or NBT");
    } finally {mc.player.inventory.setCursorStack(previous);}
  }
  private static void minimap(Minecraft mc,Screen screen,RecipeBookOverlay book,List<String> manifest)throws Exception {
    var previous=MapConfig.copy();boolean hidden=mc.options.hideHud,debug=mc.options.debugHud;
    try {
      var shown=MapConfig.copy();shown.minimap=true;shown.showInInventory=true;shown.circular=true;
      shown.size=108;shown.corner=1;shown.offsetX=6;shown.offsetY=6;MapConfig.preview(shown);
      mc.options.hideHud=false;mc.options.debugHud=false;
      check(Maps.cache()!=null&&Minimap.inventoryVisible(mc),"native inventory minimap is not ready");
      capture(mc,screen,"player-narrow-minimap-on-320",manifest);
      var bounds=MinimapGeometry.layout(MapConfig.current(),screen.width,screen.height,false);
      int x=bounds.x()+16,y=bounds.y()+bounds.size()/2;double zoom=MapConfig.current().zoom;
      check(MinimapGeometry.contains(bounds,true,x,y)&&book.layout().background().contains(x,y),"fixture point misses minimap/book overlap");
      check(screen instanceof InventoryGui gui&&gui.power$containsGuiSpace(x,y),"recipe book did not own the minimap overlap");
      check(!Minimap.inventoryWheel(mc,x,y,120)&&MapConfig.current().zoom==zoom,"covered minimap consumed recipe input");
      var off=MapConfig.copy();off.showInInventory=false;MapConfig.preview(off);
      check(!Minimap.inventoryVisible(mc),"inventory minimap ignored its preference");
      capture(mc,screen,"player-narrow-minimap-off-320",manifest);
    } finally {MapConfig.preview(previous);mc.options.hideHud=hidden;mc.options.debugHud=debug;}
  }
  private static void search(RecipeBookOverlay book,String value)throws Exception {((TextInput)field(book,"search")).setText(value);book.tick();field(book,"ticks",0);}
  @SuppressWarnings("unchecked") private static List<RecipeDefinition> recipes(RecipeBookOverlay book)throws Exception {return (List<RecipeDefinition>)field(field(book,"model"),"recipes");}
  @SuppressWarnings("unchecked") private static void popup(RecipeBookOverlay book)throws Exception {
    var groups=(List<List<RecipeDefinition>>)field(book,"groups");int index=-1;
    for(int i=0;i<Math.min(20,groups.size());i++)if(groups.get(i).size()>1){index=i;break;}
    check(index>=0,"grouped wool variants were not available");var cell=book.layout().cell(index);book.mouse(cell.x()+12,cell.y()+12,1);
    check(!((List<?>)field(book,"popup")).isEmpty(),"native right click did not open variants");
  }
  private static void ghost(RecipeBookOverlay book,int output)throws Exception {
    var context=(RecipeContext)field(book,"context");var recipe=recipes(book).stream().filter(value->value.kind==context.kind
        &&value.output.itemId==output&&value.layout(context.width,context.height)!=null).findFirst().orElseThrow();
    field(book,"ghost",recipe);book.tick();field(book,"ticks",0);
  }
}
