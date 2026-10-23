package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import local.luke.power.map.*;
import local.luke.power.input.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

/** Uses the actual inventory and pause hosts, their control bounds and queued settings path. */
final class Menu423Checks {
  static void run(Minecraft mc,String action) throws Exception {
    if(action.equals("pause")) { mc.setScreen(new net.minecraft.class_525()); return; }
    if(action.equals("inventory")) { mc.setScreen(new net.minecraft.class_585(mc.player)); return; }
    check(mc.world!=null&&mc.player!=null,"load a disposable world first");
    check(org.lwjgl.opengl.Display.isActive(),"focus the disposable game window first");
    failures=0;var saved=MapConfig.copy();var previous=mc.currentScreen;boolean hidden=mc.options.hideHud;
    try {
      mc.options.hideHud=false;
      var draft=MapConfig.copy();draft.minimap=draft.showInInventory=true;draft.hideWithDebug=false;draft.size=64;
      for(boolean pause:new boolean[]{false,true})for(boolean circular:new boolean[]{false,true})for(int corner=0;corner<4;corner++) {
        draft.corner=corner;draft.circular=circular;draft.caveMode=2;MapConfig.preview(draft);Maps.tick(mc);
        Screen host=pause?new net.minecraft.class_525():new net.minecraft.class_585(mc.player);
        mc.setScreen(host);host.init(mc,480,270);MinimapQuickMenu.beginFrame();
        final int anchor=corner;
        test("MENU423 "+(pause?"pause":"inventory")+" "+(circular?"circle":"square")+" corner "+corner,()->{
          check(Minimap.inventoryVisible(mc),"minimap hidden in native host");
          check(Minimap.waypointHost(host),"waypoints excluded from native host");
          var layout=MinimapQuickMenuLayout.at(Minimap.layout(MapConfig.current(),480,270,false),draft.circular,anchor,480,270,mc.textRenderer.getWidth("Show in-game waypoints"));
          int x=layout.gear().x()+8,y=layout.gear().y()+8;
          // Render first so pause controls have their final positions.
          host.render(0,0,1);
          check(MinimapQuickMenu.mouse(mc,x,y,0,true),"button did not open");
          check(MinimapQuickMenu.mouse(mc,x,y,0,false),"button release leaked");
          check(MinimapQuickMenu.active(mc)&&InventoryPanels.modal(host),"modal owner missing");
          var row=layout.row(4);
          check(MinimapQuickMenu.mouse(mc,row.x()+20,row.y()+10,0,true),"cave checkbox leaked");
          MinimapQuickMenu.mouse(mc,row.x()+20,row.y()+10,0,false);
          check(MapConfig.current().caveMode==0&&Maps.layer()==-1,"cave disable waited for a tick");
          var sampler=Maps.class.getDeclaredField("sampler");sampler.setAccessible(true);
          check(sampler.get(null)==null,"obsolete cave sampling retained");
          MinimapQuickMenu.mouse(mc,row.x()+20,row.y()+10,0,true);MinimapQuickMenu.mouse(mc,row.x()+20,row.y()+10,0,false);
          check(MapConfig.current().caveMode==2,"checkbox lost selected cave mode");
          var output=Path.of("power-beta-data/reports/menu-423");Files.createDirectories(output);
          Path capture=output.resolve((pause?"pause":"inventory")+(circular?"circle":"square")+anchor+".png");
          SettingsSnapshot.render(mc,new Screen(){@Override public void render(int mx,int my,float delta){
            host.render(mx,my,delta);
            boolean culling=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
            MinimapQuickMenu.renderButton(mc);
            check(org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE),"button leaked face-culling state");
            if(!culling)org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
            MinimapQuickMenu.renderMenu(mc,mx,my);
          }},480,270,capture,x,y);
          var image=javax.imageio.ImageIO.read(capture.toFile());
          int background=image.getRGB((layout.gear().x()+8)*image.getWidth()/480,
              (layout.gear().y()+3)*image.getHeight()/270)&0xffffff;
          check(background==0x65655a||background==0x4d4d4d,"circular button background missing: "+Integer.toHexString(background));
          check(MinimapQuickMenu.key(mc,org.lwjgl.input.Keyboard.KEY_ESCAPE,true),"Escape leaked");
          check(mc.currentScreen==host&&!InventoryPanels.modal(host),"closing menu changed native host");
          check(!Minimap.inventoryWheel(mc,240,140,120)||!((InventoryGui)host).power$containsGuiSpace(240,140),"wheel stole native controls");
        });
      }
      test("MENU423 excludes options and unrelated screens",()->{
        Screen screen=new Screen(){};mc.setScreen(screen);
        check(!Minimap.inventoryVisible(mc)&&!Minimap.waypointHost(screen),"map overlay admitted unrelated screen");
      });
    } finally {
      mc.setScreen(previous);MinimapQuickMenu.active(mc);MapConfig.queued(saved);mc.options.hideHud=hidden;
    }
    log("MENU423 FAILURES "+failures);
  }
}
