package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import local.luke.power.autowalk.*;
import local.luke.power.building.config.Config;
import local.luke.power.input.*;
import local.luke.power.inventory.tools.*;
import local.luke.power.map.*;
import local.luke.power.recipes.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.*;
import org.lwjgl.opengl.Display;

/** The check phase uses real held W and Tab in an already-open disposable inventory. */
final class Controls403Checks {
  private static SortingLayer402Checks.State state;
  private static RecipeBookOverlay book;
  private static local.luke.power.building.config.Settings savedBuilding;
  private static MapSettings savedMap;
  private static int forward,radar,sort;
  private Controls403Checks() {}
  static void run(Minecraft mc,String action)throws Exception {
    if(action.equals("open")) {
      check(state==null&&Display.isActive(),"focus the disposable client and finish earlier checks");
      savedBuilding=Config.current().copy();savedMap=MapConfig.copy();forward=mc.options.forwardKey.code;radar=MapKeys.RADAR_ICONS.code;sort=GameplayKeys.SORT_INVENTORY.code;
      state=new SortingLayer402Checks.State(mc);state.open(new int[]{640,360});state.tools.close();state.closed();
      Files.createDirectories(Sorting403Checks.REPORT);
      var moving=savedBuilding.copy();moving.inventoryWhileMoving=true;Config.preview(moving);mc.options.forwardKey.code=Keyboard.KEY_W;MapKeys.RADAR_ICONS.code=Keyboard.KEY_TAB;
      var map=MapConfig.copy();map.radar=map.minimap=true;MapConfig.preview(map);
      var recipes=RecipeBookConfig.copy();recipes.enabled=recipes.inventory=recipes.inventoryButton=true;RecipeBookConfig.preview(recipes);
      book=RecipeBooks.book(state.screen);UiChecks.field(book,"open",true);book.tick();
      var search=book.layout().search();state.press(search.x()+4,search.y()+4,0);
      check(InventoryTextInput.active(state.screen),"search did not claim the inventory");
      log("CONTROLS403 ready for held W and Tab");return;
    }
    check(state!=null,"run controls-403 open first");
    try {
      if(action.equals("cancel"))return;
      check(action.equals("check")&&Keyboard.isKeyDown(Keyboard.KEY_W)&&Keyboard.isKeyDown(Keyboard.KEY_TAB),"hold physical W and Tab in the disposable window");
      failures=0;
      test("CONTROLS403 search consumes W and suppresses native movement",()->{
        String before=state.inventory();var search=(TextInput)UiChecks.field(book,"search");String text=search.text();state.key('w',Keyboard.KEY_W);mc.player.field_161.method_1942(mc.player);
        check(search.text().equals(text+"w"),"typing did not reach recipe search");
        check(!InventoryMovement.allows(mc,state.screen)&&!MovementScreens.allows(state.screen)&&mc.player.field_161.field_2533==0,"search W drove player movement");
        check(!MapKeys.iconsHeld(mc),"search allowed the mob-heads hold key");state.unchanged(before);
      });
      test("CONTROLS403 ending search restores held movement and Tab mob heads",()->{
        ((TextInput)UiChecks.field(book,"search")).focused=false;book.tick();mc.player.field_161.method_1942(mc.player);
        check(InventoryMovement.allows(mc,state.screen)&&mc.player.field_161.field_2533>0,"positive control did not restore held W");
        check(MapKeys.iconsHeld(mc),"unconflicted inventory Tab did not show mob heads");
      });
      test("CONTROLS403 inventory sorting takes priority over the same hold key",()->{
        GameplayKeys.SORT_INVENTORY.code=Keyboard.KEY_TAB;
        try{check(Bindings.down(GameplayKeys.SORT_INVENTORY),"inventory sort positive control was unavailable");check(!MapKeys.iconsHeld(mc),"mob heads ignored inventory key precedence");}
        finally{GameplayKeys.SORT_INVENTORY.code=sort;}
        check(MapKeys.iconsHeld(mc),"removing the conflict did not restore Tab");
        InventoryPanels.modal(state.screen,"403fixture",true);
        try{check(!MapKeys.iconsHeld(mc),"modal layer admitted the mob-heads hold key");}finally{InventoryPanels.modal(state.screen,"403fixture",false);}
      });
      test("CONTROLS403 compact S starts one pixel lower with an empty bottom inset",()->{
        UiChecks.field(book,"open",false);book.tick();state.tools.prepare();
        Sorting403Checks.capture(mc,state.screen,"letters-S-640",false);
        Object header=UiChecks.field(state.tools,"sort");letter((InventoryToolsLayout.Rect)UiChecks.field(header,"rect"),"letters-S-640");
      });
      test("CONTROLS403 Creative Survival view supports both recipe button styles",()->{
        var rules=AccessRules.copy(mc);String mode=mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
        try {
          AccessRules.set(mc,local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE,true);CreativeVehicleChecks.mode(mc,"CREATIVE");
          UiChecks.field(state.screen,"creative_normalGUI",true);
          for(int style:new int[]{0,1}) {
            var recipes=RecipeBookConfig.copy();recipes.buttonStyle=style;RecipeBookConfig.preview(recipes);UiChecks.field(book,"open",false);book.tick();
            check(book.enabled()&&!((InventoryGui)state.screen).power$usesCatalogWheel(),"Creative Survival tab disabled the recipe book");
            var bounds=(InventoryPanels.Rect)UiChecks.call(book,"toggleBounds");
            Sorting403Checks.capture(mc,state.screen,"creative-recipe-"+style+"-640",false);
            if(style==1)letter(new InventoryToolsLayout.Rect(bounds.x(),bounds.y(),10,10),"creative-recipe-1-640");
            state.press(bounds.x()+3,bounds.y()+3,0);state.release(bounds.x()+3,bounds.y()+3,0);book.tick();
            check((boolean)UiChecks.field(book,"open")&&InventoryPanels.offset(state.screen)>0,"native click did not open and shift the Creative Survival recipe book");
            Sorting403Checks.capture(mc,state.screen,"creative-recipe-open-"+style+"-640",false);
          }
          UiChecks.field(state.screen,"creative_normalGUI",false);book.tick();check(!book.enabled(),"recipe controls appeared over Creative catalog slots");
        }finally{CreativeVehicleChecks.mode(mc,mode);AccessRules.restore(mc,rules);UiChecks.field(state.screen,"creative_normalGUI",true);}
      });
      test("CONTROLS403 Creative hotbar layer hides native inventory and Escape restores it",()->{
        var rules=AccessRules.copy(mc);String mode=mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
        try {
          AccessRules.set(mc,local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE,true);CreativeVehicleChecks.mode(mc,"CREATIVE");
          var enabled=InventoryToolsConfig.copy();enabled.creativeHotbars=enabled.creativeHotbarsButton=true;InventoryToolsConfig.preview(enabled);
          UiChecks.field(state.screen,"creative_normalGUI",true);UiChecks.field(book,"open",false);book.tick();state.tools.prepare();
          Object header=UiChecks.field(state.tools,"second");var bounds=(InventoryToolsLayout.Rect)UiChecks.field(header,"rect");
          String before=state.inventory();state.press(bounds.x()+3,bounds.y()+3,0);state.release(bounds.x()+3,bounds.y()+3,0);
          check(state.tools.modal(),"H did not open the native hotbar layer");check(!MapKeys.iconsHeld(mc),"hotbar modal admitted Tab");
          Sorting403Checks.capture(mc,state.screen,"creative-hotbars-hidden-640",true);state.key('\0',Keyboard.KEY_ESCAPE);
          check(!state.tools.modal()&&state.hostOpen(),"hotbar Escape closed the inventory");state.unchanged(before);
          Sorting403Checks.capture(mc,state.screen,"creative-hotbars-restored-640",false);letter((InventoryToolsLayout.Rect)UiChecks.field(header,"rect"),"creative-hotbars-restored-640");
        }finally{CreativeVehicleChecks.mode(mc,mode);AccessRules.restore(mc,rules);}
      });
      test("CONTROLS403 pause-menu map and photo Escape return to the same pause screen",()->{
        UiChecks.field(book,"open",false);book.tick();var pause=new net.minecraft.class_525();mc.currentScreen=pause;pause.init(mc,640,360);
        var map=new WorldMapScreen(pause);mc.setScreen(map);((ScreenInput)(Object)map).power$key('\0',Keyboard.KEY_ESCAPE);
        check(mc.currentScreen==pause&&!Mouse.isGrabbed(),"map Escape resumed gameplay instead of the pause menu");
        var photo=(Screen)Class.forName("local.luke.power.controls.tweaks.photomode.PhotoModeScreen").getConstructor(Screen.class).newInstance(pause);
        mc.setScreen(photo);((ScreenInput)(Object)photo).power$key('\0',Keyboard.KEY_ESCAPE);
        check(mc.currentScreen==pause&&!Mouse.isGrabbed(),"photo Escape resumed gameplay or recaptured the mouse");
        mc.currentScreen=state.screen;mc.player.container=state.screen.container;
      });
      log("CONTROLS403 failures="+failures);check(failures==0,"Controls403 failures="+failures);
    } finally {
      try{state.close();}finally{state=null;book=null;Config.preview(savedBuilding);MapConfig.preview(savedMap);mc.options.forwardKey.code=forward;MapKeys.RADAR_ICONS.code=radar;GameplayKeys.SORT_INVENTORY.code=sort;}
    }
  }
  private static void letter(InventoryToolsLayout.Rect rect,String image)throws Exception {
    var pixels=javax.imageio.ImageIO.read(Sorting403Checks.REPORT.resolve(image+".png").toFile());int min=10,max=-1;
    for(int y=1;y<9;y++)for(int x=1;x<9;x++)if((pixels.getRGB((rect.x()+x)*2,(rect.y()+y)*2)&0xffffff)==0x606060){min=Math.min(min,y);max=Math.max(max,y);}
    check(min==2&&max==7,"compact glyph bounds were "+min+".."+max+", expected 2..7");
  }
}
