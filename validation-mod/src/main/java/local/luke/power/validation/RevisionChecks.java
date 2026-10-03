package local.luke.power.validation;

import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.visual.*;
import local.luke.power.worldedit.*;
import local.luke.power.worldedit.carry.*;
import local.luke.power.worldedit.core.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.class_27;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;

public final class RevisionChecks {
  private static String track;
  private static Object invoke(Object o,String method,Class<?>[] args,Object...values) throws Exception {
    Method m=o.getClass().getDeclaredMethod(method,args);m.setAccessible(true);return m.invoke(o,values);
  }
  private static List<Setting> visible(PowerOptionsScreen s) throws Exception {
    List<Setting> values=new ArrayList<>();
    for(Object row:(List<?>)field(s,"rows")) { Setting value=(Setting)call(row,"setting");if(value!=null)values.add(value); }
    return values;
  }
  public static void run(Minecraft mc,String action) throws Exception {
    if(action.equals("menu")) menu(mc);
    else if(action.equals("carry")) carry(mc);
    else if(action.equals("next")) {
      mc.options.musicVolume=.7f;AudioController.next();track=AudioController.nowPlaying();
      log("Manual track requested while menu="+(mc.currentScreen!=null));
    } else if(action.equals("next-check")) {
      test("next starts playback without closing Options",()->check(mc.currentScreen instanceof PowerOptionsScreen && SoundManagerAccessor.power$system().playing("BgMusic") && !AudioController.nowPlaying().equals("none"),"not playing in Options"));
      track=AudioController.nowPlaying();AudioController.next();
    } else if(action.equals("previous")) { AudioController.previous(); }
    else if(action.equals("previous-check")) {
      test("previous returns to the last played track in Options",()->check(mc.currentScreen instanceof PowerOptionsScreen && SoundManagerAccessor.power$system().playing("BgMusic") && AudioController.nowPlaying().equals(track),"previous track differs"));
      AudioController.previewSound("ambient.weather.thunder");
    } else if(action.equals("preview-check")) {
      test("speaker preview starts a separate native sound source",()->check(SoundManagerAccessor.power$system().playing("PowerBetaPreview"),"preview not playing"));
    } else if(action.equals("walk-setup")) {
      ConfigSession session=SettingsRegistry.open(mc);
      find(session,"tweaks.autoWalk").value=new JsonPrimitive(true);
      find(session,"keys.Auto-walk (toggle)").value=new JsonPrimitive(org.lwjgl.input.Keyboard.KEY_I);
      session.save(Path.of(".").toAbsolutePath());mc.setScreen(null);
      log("Auto-walk configured on I");
    } else if(action.equals("walk-check")) {
      test("physical auto-walk hotkey starts forward input",()->check(local.luke.power.autowalk.AutoWalk.isWalking() && mc.player.field_161.field_2533>0,"auto-walk inactive"));
    } else if(action.equals("walk-stopped")) {
      test("manual movement or focus/menu stops auto-walk",()->check(!local.luke.power.autowalk.AutoWalk.isWalking(),"walk stayed active"));
    } else if(action.equals("slash-check")) {
      test("slash opens chat with one slash",()->check(mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen && ((local.luke.power.validation.mixin.ChatInput)(Object)mc.currentScreen).power$text().equals("/"),"slash input wrong"));
    } else if(action.equals("chat-closed")) {
      test("slash does not open chat when disabled",()->check(mc.currentScreen==null,"chat opened"));
    } else if(action.equals("unpause")) mc.setScreen(null);
    else if(action.equals("new-world-screen")) mc.setScreen(new net.minecraft.class_180(new net.minecraft.client.gui.screen.TitleScreen()));
  }
  private static void menu(Minecraft mc) throws Exception {
    var parent=mc.currentScreen;
    PowerOptionsScreen screen=new PowerOptionsScreen(parent);mc.setScreen(screen);
    test("menu restores last page and scroll position",()->{
      field(screen,"page","Gameplay");field(screen,"scroll",100d);call(screen,"layout");double position=(double)field(screen,"scroll");
      mc.setScreen(parent);PowerOptionsScreen reopened=new PowerOptionsScreen(parent);mc.setScreen(reopened);
      check(field(reopened,"page").equals("Gameplay") && field(reopened,"scroll").equals(position),"position lost");
      mc.setScreen(screen);
    });
    test("Controls is flat and hides disabled feature bindings",()->{
      field(screen,"page","Controls");field(screen,"scroll",0d);field(screen,"showDisabled",false);find(screen.session(),"power_camera:config.enabled").value=new JsonPrimitive(false);call(screen,"layout");
      check(visible(screen).stream().noneMatch(s->s.id.equals("keys.Toggle Freecam")),"disabled binding visible");
      for(Object row:(List<?>)field(screen,"rows"))check(call(row,"setting")!=null,"section title remains");
      field(screen,"showDisabled",true);call(screen,"layout");check(visible(screen).stream().anyMatch(s->s.id.equals("keys.Toggle Freecam")),"show disabled failed");
    });
    test("conflict filter stays fixed while bindings change",()->{
      Setting first=find(screen.session(),"keys.key.forward"),other=find(screen.session(),"keys.key.back");
      other.value=first.value.deepCopy();call(screen,"layout");Object row=((List<?>)field(screen,"rows")).stream().filter(v->{try{return call(v,"setting")==first;}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
      field(screen,"scroll",(double)(int)call(row,"y"));
      int x=(int)invoke(screen,"controlLeft",new Class[]{row.getClass()},row),top=(int)call(screen,"top"),y=top+(int)call(row,"y")-((Double)field(screen,"scroll")).intValue()+((int)call(row,"height")==40?16:2)+5;
      ((ScreenInput)(Object)screen).power$click(x-5,y,0);
      List<String> ids=(List<String>)field(screen,"conflictIds");check(ids.size()>=2&&ids.get(0).equals(first.id)&&ids.contains(other.id),"click did not open ordered filter");
      other.value=new JsonPrimitive(0);call(screen,"layout");check(visible(screen).stream().anyMatch(s->s.id.equals(other.id)),"resolved row disappeared");
      ((ScreenInput)(Object)screen).power$key('\0',org.lwjgl.input.Keyboard.KEY_ESCAPE);check(((List<?>)field(screen,"conflictIds")).isEmpty(),"filter did not close");
    });
    test("related binding settings link navigates to the setting",()->{
      Setting key=find(screen.session(),"keys.Toggle Freecam");invoke(screen,"related",new Class[]{Setting.class},key);
      check(field(screen,"page").equals("Camera"),"related page wrong");
      check(visible(screen).stream().anyMatch(s->s.id.equals("power_camera:config.enabled")),"related setting absent");
    });
    test("sound controls exclude later-version cached assets",()->{
      check(!AudioController.sounds(mc).stream().anyMatch(s->s.contains("blaze")||s.contains("mob.cat")||s.contains("enderman")),"non-Beta sound shown");
      check(AudioController.sounds(mc).contains("random.click"),"Beta click missing");
    });
    test("every video setting supports live preview",()->{
      for(Setting value:screen.session().settings())if(value.page.equals("Video"))check(!value.restart,"restart-only video setting: "+value.id);
      byte[] disk=Files.readAllBytes(PowerConfig.path());
      for(String id:List.of("native.fancy","native.ao","native.opengl","native.anaglyph")) {
        Setting value=find(screen.session(),id);value.cycle(1);screen.changed(value);
      }
      check(Arrays.equals(disk,Files.readAllBytes(PowerConfig.path())),"preview wrote config");
      screen.session().discard();
    });
    test("render distance reaches every requested integer",()->{
      Class<?> options=Class.forName("local.luke.power.controls.util.ModOptions");Method get=options.getMethod("getRenderDistanceChunks");
      for(int distance=2;distance<=32;distance++) {
        Setting value=find(screen.session(),"native.renderDistance");value.value=new JsonPrimitive(distance);screen.changed(value);
        check((int)get.invoke(null)==distance,"distance differs at "+distance);
      }
      screen.session().discard();
    });
    screen.session().discard();mc.setScreen(parent);log("REVISION MENU CHECKS COMPLETE");
  }
  private static void carry(Minecraft mc) throws Exception {
    check(mc.world!=null && mc.player!=null,"Need test world");mc.setScreen(null);
    VisualSettings original=VisualConfig.copy();VisualSettings enabled=VisualConfig.copy();enabled.containerCarry=true;VisualConfig.preview(enabled);
    double savedX=mc.player.x,savedY=mc.player.y,savedZ=mc.player.z;
    mc.player.method_1340(savedX, Math.max(12,Math.min(118,savedY)), savedZ);
    ItemStack[] originalInventory=mc.player.inventory.main.clone();int oldSlot=mc.player.inventory.selectedSlot;
    var blocks=new MinecraftWorld(mc.world);Pos origin=new Pos((int)Math.floor(mc.player.x),(int)Math.floor(mc.player.y), (int)Math.floor(mc.player.z));
    int y=Math.max(8,Math.min(120,origin.y()));Pos source=new Pos(origin.x()+2,y,origin.z()),target=source.add(0,0,2),support=target.add(0,-1,0);
    Map<Pos,BlockValue> before=new LinkedHashMap<>();
    for(Pos p:List.of(source,source.add(1,0,0),source.add(-1,0,0),source.add(0,0,-1),source.add(0,0,1),target,target.add(1,0,0),target.add(-1,0,0),target.add(0,0,1),support)) { before.put(p,blocks.get(p));blocks.set(p,BlockValue.AIR); }
    try {
      mc.player.inventory.selectedSlot=0;mc.player.inventory.main[0]=null;mc.player.field_161.field_2536=true;
      for(int block:new int[]{54,23,61}) {
        blocks.set(source,new BlockValue(block,2));blocks.set(support,new BlockValue(1,0));
        Inventory inventory=(Inventory)mc.world.method_1777(source.x(),source.y(),source.z());
        inventory.setStack(0,new ItemStack(264,7,0));inventory.setStack(inventory.size()-1,new ItemStack(257,1,37));inventory.markDirty();
        BlockValue snapshot=blocks.get(source);
        test("container "+block+" pickup preserves complete journal",()->{
          mc.field_2823=new class_27(source.x(),source.y(),source.z(),1,Vec3d.createCached(source.x()+.5,source.y()+1,source.z()+.5));
          log("PICKUP player="+mc.player.x+","+mc.player.y+","+mc.player.z+" source="+source+" sneak="+mc.player.method_1373()+" health="+mc.player.health+" freecam="+WorldEditor.freecam()+" feature="+InteractionState.containerCarryEnabled+" loaded="+blocks.loaded(source)+" reach="+mc.interactionManager.method_1715());
          boolean result=ContainerCarry.click(mc,1);
          for(String name:List.of("file","failed","loaded")){Field f=ContainerCarry.class.getDeclaredField(name);f.setAccessible(true);log(name+"="+f.get(null));}
          check(result&&ContainerCarry.carrying(),"pickup did not engage");check(blocks.get(source).id==0,"source remains");
        });
        test("container "+block+" refuses occupied placement",()->{
          blocks.set(target,new BlockValue(1,0));mc.field_2823=new class_27(support.x(),support.y(),support.z(),1,Vec3d.createCached(target.x()+.5,target.y(),target.z()+.5));
          check(ContainerCarry.click(mc,1)&&ContainerCarry.carrying(),"occupied placement lost carry");check(blocks.get(target).id==1,"occupied block overwritten");blocks.set(target,BlockValue.AIR);
        });
        test("container "+block+" placement preserves item identity count damage",()->{
          check(ContainerCarry.click(mc,1)&&!ContainerCarry.carrying(),"placement failed");Inventory restored=(Inventory)mc.world.method_1777(target.x(),target.y(),target.z());
          check(restored.getStack(0).itemId==264&&restored.getStack(0).count==7,"diamond stack changed");
          check(restored.getStack(restored.size()-1).itemId==257&&restored.getStack(restored.size()-1).getDamage()==37,"tool damage changed");
          blocks.set(target,BlockValue.AIR);
        });
      }
    } finally {
      if(!ContainerCarry.carrying()) for(var e:before.entrySet()) blocks.set(e.getKey(),e.getValue());
      mc.player.method_1340(savedX,savedY,savedZ);
      mc.player.field_161.field_2536=false;mc.player.inventory.main=originalInventory;mc.player.inventory.selectedSlot=oldSlot;VisualConfig.preview(original);
    }
    log("CONTAINER CHECKS COMPLETE");
  }
}
