package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import com.google.gson.JsonObject;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.ContainerPreview;
import local.luke.power.storage.PowerConfig;
import local.luke.power.visual.VisualConfig;
import net.minecraft.class_27;
import net.minecraft.class_549;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Keyboard;

/** Detached picking and one-visit HUD ownership, in a disposable world only. */
final class FreecamPreviewChecks {
  private static Object camera() throws Exception {
    return Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
  }
  static void active(boolean value) throws Exception {
    camera().getClass().getMethod("setActive", boolean.class).invoke(camera(), value);
  }
  static void pose(double x, double y, double z, float pitch, float yaw) throws Exception {
    camera().getClass().getMethod("setCameraPositionAndRotation", double.class, double.class,
        double.class, float.class, float.class, float.class).invoke(camera(), x,y,z,pitch,yaw,0F);
  }
  static class_27 target(Minecraft mc) throws Exception {
    Method method=Class.forName("local.luke.power.inventory.ContainerTarget").getDeclaredMethod("find", Minecraft.class);
    method.setAccessible(true); return (class_27)method.invoke(null,mc);
  }
  @SuppressWarnings("unchecked")
  static List<ItemStack> snapshot(Minecraft mc) throws Exception {
    Method method=ContainerPreview.class.getDeclaredMethod("snapshot", Minecraft.class);
    method.setAccessible(true); return (List<ItemStack>)method.invoke(null,mc);
  }
  static void blockHit(Minecraft mc,int x,int y,int z) throws Exception {
    var hit=target(mc);
    check(hit!=null && hit.field_1983==net.minecraft.class_212.TILE
        && hit.field_1984==x && hit.field_1985==y && hit.field_1986==z,"wrong detached target");
  }
  private static Inventory container(Minecraft mc,int x,int y,int z,int id) {
    mc.world.method_154(x,y,z,id,0);
    return (Inventory)mc.world.method_1777(x,y,z);
  }
  static void scene(Minecraft mc) throws Exception {
    DeathChecks.alive(mc); mc.setScreen(null); active(false);
    Object settings=Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);

    for(int x=24;x<=40;x++) for(int z=24;z<=40;z++) for(int y=108;y<=114;y++)
      mc.world.method_154(x,y,z,y==108?1:0,0);
    mc.player.method_1341(24.5,109,24.5,90,0); mc.field_2807=mc.player;
    var chest=container(mc,32,110,32,54);
    var stack=new ItemStack(267,1,37); DeathChecks.tag(stack).putString("preview-test","kept");
    chest.setStack(0,stack); chest.setStack(26,new ItemStack(264,64,0));
    pose(32.5,110.5,35.5,0,0);
  }
  static void run(Minecraft mc,String action) throws Exception {
    if(action.equals("setup")) {
      scene(mc); var visual=VisualConfig.copy();  VisualConfig.preview(visual);
      GameplayKeys.PREVIEW.code=Keyboard.KEY_P;
      ((KeyBinding)Class.forName("local.luke.power.camera.registry.KeyBindingRegistry")
          .getField("freecamKeybinding").get(null)).code=Keyboard.KEY_C;
      Bindings.configure(Map.of()); mc.options.hideHud=false; active(true); return;
    }
    if(action.equals("state")) {
      var state=new JsonObject(); state.addProperty("active",DetachedCamera.isActive());
      state.addProperty("hideHud",mc.options.hideHud); state.addProperty("previewHeld",Bindings.down(GameplayKeys.PREVIEW));
      state.addProperty("focused",org.lwjgl.opengl.Display.isActive());
      state.addProperty("screen",mc.currentScreen==null?"none":mc.currentScreen.getClass().getSimpleName());
      state.addProperty("slots",mc.world==null?0:snapshot(mc).size());
      Files.writeString(Path.of("power-beta-161-state.json"),state.toString());return;
    }
    if(action.equals("hud-hidden")) {active(false);mc.options.hideHud=true;return;}
    if(action.equals("off")) {active(false);return;}
    if(action.equals("normal")) {
      active(false);mc.options.hideHud=false;
      mc.player.method_1341(32.5,109,35.5,180,0);
      mc.player.prevX=mc.player.x;mc.player.prevY=mc.player.y;mc.player.prevZ=mc.player.z;
      mc.player.prevYaw=mc.player.yaw;mc.player.prevPitch=mc.player.pitch;
      mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;
      return;
    }
    if(action.equals("exit")) {
      mc.options.hideHud=false;active(true);mc.setWorld(null);
      mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
      check(!DetachedCamera.isActive()&&!mc.options.hideHud,"world exit lost HUD snapshot");
      log("PASS world exit restores visible HUD");return;
    }
    if(action.equals("check")) checks(mc);
    else throw new IllegalArgumentException(action);
  }
  private static void checks(Minecraft mc) throws Exception {
    failures=0; scene(mc);
    boolean hud=mc.options.hideHud; var before=Files.readAllBytes(PowerConfig.path());
    var playerContainer=mc.player.container;
    double px=mc.player.x,py=mc.player.y,pz=mc.player.z;
    var nativeHit=new class_27(24,108,24,1,Vec3d.createCached(24.5,109,24.5)); mc.field_2823=nativeHit;
    var chest=(Inventory)mc.world.method_1777(32,110,32);
    String original=DeathItems.encode(chest.getStack(0));
    try {
      for(boolean initiallyHidden:new boolean[]{false,true}) for(boolean manualChange:new boolean[]{false,true})
        test("HUD entry/exit hidden="+initiallyHidden+" manual="+manualChange,()->{
          active(false);mc.options.hideHud=initiallyHidden;active(true);
          check(mc.options.hideHud,"entry did not hide HUD");
          if(manualChange)mc.options.hideHud=false;
          active(true);check(mc.options.hideHud==!manualChange,"duplicate activation overwrote F1");
          active(false);check(mc.options.hideHud==initiallyHidden,"exit lost original state");
          mc.options.hideHud=!initiallyHidden;active(false);
          check(mc.options.hideHud==!initiallyHidden,"duplicate exit restored stale state");
        });
      test("session cleanup restores the freecam HUD immediately",()->{
        Object settings=Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
        mc.options.hideHud=false;active(true);local.luke.power.input.TweakLifecycle.reset();
        check(!DetachedCamera.isActive()&&!mc.options.hideHud,"feature disable left HUD hidden");

      });
      active(true);
      test("preview follows detached camera instead of player, with all horizontal directions",()->{
        double[][] poses={{32.5,110.5,35.5,0,0},{29.5,110.5,32.5,0,90},
            {32.5,110.5,29.5,0,180},{35.5,110.5,32.5,0,270},
            {32.5,113.5,32.5,90,0},{32.5,109.5,32.5,-90,0}};
        for(double[] p:poses){pose(p[0],p[1],p[2],(float)p[3],(float)p[4]);blockHit(mc,32,110,32);}
        pose(32.5,110.5,35.5,0,0);
        camera().getClass().getField("allowPlayerMovement").setBoolean(camera(),true);
        blockHit(mc,32,110,32);check(mc.field_2823==nativeHit,"preview replaced interaction target");
      });
      test("single and double chests preserve order, counts, damage and native NBT",()->{
        var stacks=snapshot(mc);check(stacks.size()==27,"single chest size");
        check(DeathItems.encode(stacks.get(0)).equals(original),"snapshot lost native data");
        stacks.get(0).count=2;DeathChecks.tag(stacks.get(0)).putString("preview-test","changed copy");
        check(DeathItems.encode(chest.getStack(0)).equals(original),"preview mutated original");
        var second=container(mc,31,110,32,54);second.setStack(0,new ItemStack(5,17,0));
        stacks=snapshot(mc);check(stacks.size()==54&&stacks.get(0).count==17&&stacks.get(27).getDamage()==37,"double order");
        second.setStack(0,null);mc.world.method_154(31,110,32,0,0);
      });
      test("solid blocks occlude previews and liquids retain native ray behavior",()->{
        mc.world.method_154(32,110,34,1,0);check(snapshot(mc).isEmpty(),"preview through solid block");
        for(int fluid:new int[]{8,10}) {mc.world.method_154(32,110,34,fluid,0);blockHit(mc,32,110,32);}
        mc.world.method_154(32,110,34,0,0);
      });
      test("furnace and dispenser inventories are read without opening them",()->{
        for(int id:new int[]{61,23}) {
          var source=container(mc,34,110,32,id);source.setStack(0,new ItemStack(264,12,0));
          pose(34.5,110.5,35.5,0,0);var items=snapshot(mc);
          check(items.size()==source.size()&&items.get(0).count==12,"missing inventory "+id);
          source.setStack(0,null);mc.world.method_154(34,110,32,0,0);
        }
      });
      test("storage minecarts are picked before blocks and blocked by solid terrain",()->{
        var cart=new class_549(mc.world,32.5,110,34,1);mc.world.method_210(cart);
        try {
          cart.setStack(0,new ItemStack(264,23,0));pose(32.5,110.4,36.5,0,0);
          check(target(mc).field_1989==cart&&snapshot(mc).get(0).count==23,"cart not picked");
          mc.world.method_154(32,110,35,1,0);check(snapshot(mc).isEmpty(),"cart visible through wall");
          mc.world.method_154(32,110,35,0,0);
        } finally {cart.setStack(0,null);cart.markDead();}
      });
      test("out-of-range, missing terrain and invalid camera poses return no preview",()->{
        pose(32.5,110.5,45.5,0,0);check(snapshot(mc).isEmpty(),"out-of-reach chest");
        int x=1_000_000;check(!mc.world.method_239(x,64,x),"fixture region unexpectedly loaded");
        pose(x+.5,110.5,x+.5,0,0);check(target(mc)==null,"unloaded target");
        check(!mc.world.method_239(x,64,x),"preview loaded terrain");
        pose(Double.NaN,110,32,0,0);check(target(mc)==null,"NaN target accepted");
      });
      test("preview leaves player, containers and saved preferences untouched; exit preserves native targeting",()->{
        check(mc.player.x==px&&mc.player.y==py&&mc.player.z==pz,"player moved");
        check(mc.player.container==playerContainer,"container opened");
        check(DeathItems.encode(chest.getStack(0)).equals(original),"source changed");
        active(false);check(mc.field_2823==nativeHit,"native targeting changed");
        check(Arrays.equals(before,Files.readAllBytes(PowerConfig.path())),"preview wrote settings");
      });
    } finally {active(false);mc.options.hideHud=hud;pose(32.5,110.5,35.5,0,0);}
    log("FREECAM PREVIEW FAILURES "+failures);
  }
}
