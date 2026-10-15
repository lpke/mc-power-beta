package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.WebsiteGallery.*;
import java.nio.file.*;
import java.lang.reflect.Method;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.flexible.*;
import local.luke.power.gameplay.*;
import local.luke.power.ui.*;
import local.luke.power.visual.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;

/** Real feature states and renderer captures for the public gallery. Disposable clone only. */
final class WebsiteDetails {
  private static final List<class_142> drops=new ArrayList<>();
  private static int conflictX,conflictY;
  static void run(Minecraft mc,String action) throws Exception {
    if(action.startsWith("iso ")) { isometric(mc,Integer.parseInt(action.substring(4)));return; }
    switch(action) {
      case "textures-on", "textures-off" -> {
        setting(mc,"visual.oldCobble",String.valueOf(action.endsWith("on")));setting(mc,"visual.oldBricks",String.valueOf(action.endsWith("on")));
        local.luke.power.visual.TextureOverrides.clear();mc.textureManager.method_1096();mc.worldRenderer.method_1537();
      }
      case "placement" -> placement(mc);
      case "placement-check" -> {
        var h=mc.field_2823;
        check(h!=null,"no raycast");
        var p=FlexiblePlacement.plan(mc,mc.player.inventory.getSelectedItem(),h.field_1984,h.field_1985,h.field_1986,h.field_1987);
        log("PLACEMENT hit="+h.field_1984+","+h.field_1985+","+h.field_1986+" side="+h.field_1987+" plan="+p);
        check(p!=null&&p.valid(),"no valid preview");
      }
      case "placement-overview" -> {camera(mc,11,12,-13,39,34);mc.options.hideHud=true;}
      case "fence", "slabs" -> recipe(mc,action);
      case "conflicts" -> conflicts(mc);
      case "conflicts-filter" -> {click((PowerOptionsScreen)mc.currentScreen,conflictX,conflictY);Mouse.setCursorPosition(0,0);}
      case "chat-select", "chat-cursor" -> {
        mc.setScreen(new ChatScreen());
        var access=Class.forName("local.luke.power.commands.integration.chat.ChatWidgetAccess");
        access.getMethod("setText",String.class).invoke(null,"Meet at the northern bridge, then follow the river.");
        var editor=(local.luke.power.chat.ChatBuffer)access.getMethod("editor").invoke(null);
        editor.position(12,false);editor.position(action.equals("chat-select")?27:12,action.equals("chat-select"));
        Mouse.setCursorPosition(0,0);
      }
      case "pause-music" -> {
        WebsiteGallery.run(mc,"menu-scene");
        var audio=AudioConfig.copy();audio.menuControls=audio.menuControlsScrub=true;audio.master=60;AudioConfig.preview(audio);
        mc.options.musicVolume=.3f;mc.soundManager.method_2008();AudioController.next();mc.setScreen(new class_525());
      }
      case "music-seek" -> AudioController.seek(.42);
      case "music-queue" -> {
        var tracks=AudioController.music(mc).stream().sorted().limit(4).toList();
        MusicRequests.edit(queue->{queue.tracks.clear();tracks.forEach(queue::add);});
        PowerOptionsScreen.openMusic(mc,null,true,null);Mouse.setCursorPosition(0,0);
      }
      case "items" -> items(mc);
      case "items-hidden", "items-visible" -> {var s=VisualConfig.copy();s.hideEntities=action.endsWith("hidden");VisualConfig.preview(s);}
      case "items-merge" -> {var s=GameplayConfig.copy();s.mergeItems=true;s.mergeThreshold=16;GameplayConfig.preview(s);}
      case "tnt-settings" -> menu(mc,"Gameplay","TNT");
      case "placement-settings" -> menu(mc,"Building","flexible");
      case "buckets" -> {
        WebsiteGallery.run(mc,"crafting-scene");setting(mc,"power_compat:config.STACK_SIZE_CONFIG.stackableEmptyBuckets","true");CreativeVehicleChecks.mode(mc,"SURVIVAL");mc.setScreen(new class_585(mc.player));
        mc.player.inventory.main[0]=new ItemStack(325,16,0);mc.player.inventory.main[1]=new ItemStack(326,1,0);mc.player.inventory.main[2]=new ItemStack(327,1,0);mc.player.inventory.main[3]=new ItemStack(335,1,0);
        mc.player.inventory.main[9]=new ItemStack(325,16,0);mc.player.inventory.main[10]=new ItemStack(325,16,0);Mouse.setCursorPosition(0,0);
      }
      case "iso-scene" -> {WebsiteGallery.run(mc,"castle");setting(mc,"native.renderDistance","6");camera(mc,0,40,0,135,25);mc.options.hideHud=true;}
      default -> throw new IllegalArgumentException(action);
    }
  }
  private static void placement(Minecraft mc) throws Exception {
    WebsiteGallery.run(mc,"building");setting(mc,"tweaks.placement.enabled","false");
    for(int dx=-10;dx<=10;dx++)for(int dz=-10;dz<=10;dz++)for(int dy=0;dy<7;dy++)block(mc,dx,dy,dz,0,0);
    for(int dx=-7;dx<=7;dx++)for(int dz=-7;dz<=7;dz++) {
      double radius=Math.sqrt(dx*dx+dz*dz);
      if(radius>=6.5&&radius<7.5&&!(dx==4&&dz==-6))block(mc,dx,1,dz,5,0);
    }
    var s=local.luke.power.building.config.Config.current().copy();s.flexible.showOverlay=s.flexible.showPreview=true;
    local.luke.power.building.config.Config.preview(s);for(int i=0;i<Keys.ALL.length;i++)Keys.ALL[i].code=0;Keys.ALL[0].code=34;
    mc.player.inventory.main[0]=new ItemStack(5,64,0);mc.player.inventory.selectedSlot=0;
    // Aim at the left edge of the north face. Offset places the missing diagonal block.
    double cx=4.3,cy=3.4,cz=-8,tx=5.1,ty=1.65,tz=-5.001;
    float yaw=(float)Math.toDegrees(Math.atan2(-(tx-cx),tz-cz));
    float pitch=(float)Math.toDegrees(Math.atan2(cy-ty,Math.hypot(tx-cx,tz-cz)));
    camera(mc,cx,cy,cz,yaw,pitch);mc.options.hideHud=false;
  }
  private static void recipe(Minecraft mc,String kind) throws Exception {
    WebsiteGallery.run(mc,"crafting-scene");
    setting(mc,"power_controls:recipes.modern.modernFenceRecipe","true");
    setting(mc,"power_controls:recipes.modern.sixSlabsPerCraft","true");
    Class.forName("local.luke.power.controls.tweaks.recipes.RecipeListener").getMethod("registerShapedRecipes").invoke(null);
    block(mc,0,0,0,58,0);camera(mc,.5,2,-2,0,15);
    setting(mc,"native.guiScale","3");mc.player.method_484(x,y,z);var c=(class_196)mc.player.container;
    if(kind.equals("fence"))for(int row=0;row<2;row++)for(int col=0;col<3;col++)c.field_708.setStack(row*3+col,new ItemStack(col==1?280:5,1,0));
    else for(int col=0;col<3;col++)c.field_708.setStack(3+col,new ItemStack(1,1,0));
    check(c.method_2084(0).getStack()!=null,"recipe produced no output");Mouse.setCursorPosition(0,0);
  }
  private static void conflicts(Minecraft mc) throws Exception {
    WebsiteGallery.run(mc,"controls-scene");setting(mc,"keys.power_beta.fullBright","36");setting(mc,"keys.power_beta.entities","36");
    menu(mc,"Controls","full bright");var screen=(PowerOptionsScreen)mc.currentScreen;
    for(Object row:(List<?>)field(screen,"rows")) {
      Setting s=(Setting)call(row,"setting");if(s==null||s.kind!=Setting.Kind.KEY)continue;
      Method left=screen.getClass().getDeclaredMethod("controlLeft",row.getClass());left.setAccessible(true);
      conflictX=(int)left.invoke(screen,row)-6;conflictY=(int)call(screen,"top")+(int)call(row,"y")+10;
      Mouse.setCursorPosition(conflictX*mc.displayWidth/screen.width,mc.displayHeight-conflictY*mc.displayHeight/screen.height-1);return;
    }
    throw new IllegalStateException("conflicting binding not visible");
  }
  private static void items(Minecraft mc) throws Exception {
    WebsiteGallery.run(mc,"textures");var s=GameplayConfig.copy();s.mergeItems=false;GameplayConfig.preview(s);
    var v=VisualConfig.copy();v.oldCobble=v.oldBricks=false;VisualConfig.preview(v);mc.worldRenderer.method_1537();
    for(var d:drops)d.markDead();drops.clear();
    for(int n=0;n<96;n++) {
      var item=new class_142(mc.world,x-3+(n%12)*.5,y+.5,z-3+(n/12)*.5,new ItemStack(n%2==0?4:3,1,0));
      item.velocityX=item.velocityY=item.velocityZ=0;item.field_567=32767;mc.world.method_210(item);drops.add(item);
    }
    camera(mc,1,4,-7,0,30);mc.options.hideHud=true;
  }
  private static void isometric(Minecraft mc,int angle) throws Exception {
    mc.setScreen(null);
    for(int pass=0;pass<4096&&!mc.worldRenderer.method_1549(mc.field_2807,false);pass++){}
    Object config=Class.forName("local.luke.power.capture.Config").getField("config").get(null);
    Class<?> type=config.getClass();type.getField("isometricPhotoScale").set(config,3);
    var rotation=type.getField("isometricPhotoRotation");rotation.set(config,rotation.getType().getEnumConstants()[(angle/90)%4]);
    type.getField("isometricPhotoRotationOffset").set(config,angle%90);
    Path root=Path.of("power-beta-data/reports/gallery-orbit");Files.createDirectories(root);
    var renderer=Class.forName("local.luke.power.capture.IsometricScreenshotRenderer");
    renderer.getMethod("doRender").invoke(renderer.getConstructor(Minecraft.class,java.io.File.class).newInstance(mc,root.toFile()));
    Path latest;try(var files=Files.list(root.resolve("screenshots"))) {latest=files.max(Comparator.comparing(p->p.getFileName().toString())).orElseThrow();}
    Files.move(latest,root.resolve(String.format("%03d.png",angle)),StandardCopyOption.REPLACE_EXISTING);
    log("ISOMETRIC ANGLE "+angle);
  }
}
