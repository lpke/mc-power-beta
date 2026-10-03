package local.luke.power.validation;

import com.google.gson.JsonPrimitive;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.input.InteractionState;
import local.luke.power.visual.*;
import local.luke.power.worldedit.*;
import local.luke.power.worldedit.carry.*;
import local.luke.power.worldedit.core.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import static local.luke.power.validation.Validation.*;

public final class FeatureChecks {
  private static float cameraBase() throws Exception {Object config=Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);return ((Number)config.getClass().getField("speed").get(config)).floatValue();}
  private static Object camera() throws Exception {return Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);}
  private static Object method(Object value,String name) throws Exception {return value.getClass().getMethod(name).invoke(value);}
  private static void settings(Minecraft mc,Map<String,Object> values) throws Exception {
    ConfigSession session=SettingsRegistry.open(mc);
    for(var e:values.entrySet())find(session,e.getKey()).value=Catalog.JSON.toJsonTree(e.getValue());
    session.save(Path.of(".").toAbsolutePath());
  }
  public static void run(Minecraft mc,String action) throws Exception {
    if(action.equals("light")) {
      settings(mc,Map.of("power_compat:config.addBlockLightToDebugOverlay",true,"power_compat:config.overlayAdditionsYOffset",0));
      Pos p=new Pos((int)Math.floor(mc.player.x),(int)Math.floor(mc.player.boundingBox.minY),(int)Math.floor(mc.player.z));
      new MinecraftWorld(mc.world).set(p.add(1,0,0),new BlockValue(89,0));mc.options.debugHud=true;mc.setScreen(null);
    } else if(action.equals("light-check")) {
      int value=mc.world.method_164(class_56.BLOCK,(int)Math.floor(mc.player.x),(int)Math.floor(mc.player.boundingBox.minY),(int)Math.floor(mc.player.z));
      test("block light uses engine light at player feet",()->check(value==14,"expected 14 beside glowstone, got "+value));
    } else if(action.equals("distance")) {
      for(int distance:new int[]{2,8,16}) {
        ConfigSession session=SettingsRegistry.open(mc);find(session,"native.renderDistance").value=new JsonPrimitive(distance);session.preview();
        mc.worldRenderer.method_1537();
        test("render distance rebuilds actual chunk grid at "+distance,()->{
          for(String name:List.of("field_1810","field_1812")){Field f=mc.worldRenderer.getClass().getDeclaredField(name);f.setAccessible(true);check(f.getInt(mc.worldRenderer)==distance*2+1,"grid did not resize");}
        });session.discard();
      }
    } else if(action.equals("camera-start")) {
      settings(mc,Map.of("power_camera:config.enabled",true,"power_camera:config.sprint",true,"keys.Toggle Freecam",67,"keys.power_creative.sprint",29,"creative.sprintToggle",true));mc.setScreen(null);
    } else if(action.equals("camera-fast")) {
      test("freecam flight sprint boosts speed",()->check((float)method(camera(),"movementSpeed")>cameraBase()+.1f,"not boosted"));
    } else if(action.equals("camera-slow")) {
      test("freecam sprint ends when forward movement stops",()->check((float)method(camera(),"movementSpeed")==cameraBase(),"sprint remained latched"));
    } else if(action.equals("carry-save")) {
      mc.setScreen(null);settings(mc,Map.of("visual.containerCarry",true));
      mc.player.inventory.selectedSlot=0;mc.player.inventory.main[0]=null;mc.player.field_161.field_2536=true;
      Pos p=new Pos((int)Math.floor(mc.player.x)+2,(int)Math.floor(mc.player.y),(int)Math.floor(mc.player.z));
      MinecraftWorld blocks=new MinecraftWorld(mc.world);
      for(Pos n:List.of(p.add(-1,0,0),p.add(1,0,0),p.add(0,0,-1),p.add(0,0,1)))blocks.set(n,BlockValue.AIR);
      blocks.set(p,new BlockValue(54,0));blocks.set(p.add(0,-1,0),new BlockValue(1,0));
      Inventory chest=(Inventory)mc.world.method_1777(p.x(),p.y(),p.z());chest.setStack(0,new ItemStack(264,7,0));chest.markDirty();
      mc.field_2823=new class_27(p.x(),p.y(),p.z(),1,Vec3d.createCached(p.x()+.5,p.y()+.5,p.z()+.5));
      test("pickup before world reload retains inventory journal",()->check(ContainerCarry.click(mc,1)&&ContainerCarry.carrying(),"pickup failed"));
      mc.setWorld(null);mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
    } else if(action.equals("carry-resume")) {
      ContainerCarry.tick(mc);
      test("held container survives world reload",()->check(ContainerCarry.carrying(),"held state lost"));
      Field f=ContainerCarry.class.getDeclaredField("file");f.setAccessible(true);CarryJournal journal=CarryJournal.read((Path)f.get(null));Pos p=journal.source;
      mc.setScreen(null);mc.field_2823=new class_27(p.x(),p.y()-1,p.z(),1,Vec3d.createCached(p.x()+.5,p.y(),p.z()+.5));
      test("recovered container places original items once",()->{
        check(ContainerCarry.click(mc,1)&&!ContainerCarry.carrying(),"placement failed");
        Inventory chest=(Inventory)mc.world.method_1777(p.x(),p.y(),p.z());check(chest.getStack(0).itemId==264&&chest.getStack(0).count==7,"recovered contents differ");
      });
    } else if(action.equals("preview-setup")) {
      settings(mc,Map.of("visual.containerPreview",true,"keys.powerbeta.containerPreview",68));
      Pos p=new Pos((int)Math.floor(mc.player.x)+2,(int)Math.floor(mc.player.y),(int)Math.floor(mc.player.z));
      MinecraftWorld blocks=new MinecraftWorld(mc.world);blocks.set(p,new BlockValue(54,0));
      Inventory chest=(Inventory)mc.world.method_1777(p.x(),p.y(),p.z());chest.setStack(0,new ItemStack(264,7,0));chest.setStack(4,new ItemStack(257,1,37));
      mc.player.method_1340(p.x()-2.5,p.y()+.5,p.z()+.5);mc.player.yaw=-90;mc.player.pitch=0;mc.player.prevYaw=-90;mc.player.prevPitch=0;mc.setScreen(null);
    } else if(action.equals("preview-check")) {
      test("preview targets inventory with held binding",()->{
        check(mc.field_2823!=null && mc.field_2823.field_1983==class_212.TILE,"no block target");
        var h=mc.field_2823;Inventory inventory=(Inventory)mc.world.method_1777(h.field_1984,h.field_1985,h.field_1986);
        check(inventory.getStack(0).count==7&&inventory.getStack(4).getDamage()==37,"preview changed inventory");
        check(local.luke.power.input.Bindings.down(local.luke.power.input.GameplayKeys.PREVIEW),"preview key inactive");
      });
    }
  }
}
