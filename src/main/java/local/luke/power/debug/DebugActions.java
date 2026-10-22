package local.luke.power.debug;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.Locale;
import local.luke.power.input.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

public final class DebugActions {
  public static boolean hitboxes, borders, tooltips, profiler, frames, network, light;
  private static String previousMode="SURVIVAL";
  private DebugActions() {}
  public static void clear() { hitboxes=borders=tooltips=profiler=frames=network=light=false; previousMode="SURVIVAL"; }
  public static void message(Minecraft mc,String text) { mc.inGameHud.addChatMessage("\u00a7e[Debug]\u00a7r "+text); }
  private static void toggle(Minecraft mc,String name,boolean value) { message(mc,name+": "+(value?"on":"off")); }
  public static void run(Minecraft mc,DebugAction action) throws Exception {
    switch(action) {
      case RELOAD -> { mc.worldRenderer.method_1537(); message(mc,"Reloading chunks"); }
      case HITBOXES -> toggle(mc,"Entity hitboxes",hitboxes=!hitboxes);
      case BORDERS -> toggle(mc,"Chunk borders",borders=!borders);
      case TOOLTIPS -> toggle(mc,"Advanced tooltips",tooltips=!tooltips);
      case CLEAR_CHAT -> mc.inGameHud.clearChat();
      case DISTANCE -> {
        var type=Class.forName("local.luke.power.controls.util.ModOptions");
        type.getMethod("cycleRenderDistance").invoke(null);
        int chunks=(int)type.getMethod("getRenderDistanceChunks").invoke(null);
        var section=PowerConfig.section("native");
        // Native option storage keeps the slider's normalized text representation.
        section.addProperty("render_distance",type.getField("renderDistance").getFloat(null));
        PowerConfig.saveDeferred("native",section); mc.worldRenderer.method_1537();
        message(mc,"Render distance: "+chunks+" chunks");
      }
      case LOCATION -> {
        var p=ReportedPosition.current(mc.player,mc.player.x,mc.player.y,mc.player.z,mc.player.yaw,mc.player.pitch);
        copy(mc,String.format(Locale.ROOT,"/tp @s %.2f %.2f %.2f %.2f %.2f",p.x(),p.y(),p.z(),p.yaw(),p.pitch()));
      }
      case TARGET -> DebugTarget.copy(mc);
      case PROFILE -> DebugPerformance.record(mc);
      case TEXTURE_DUMP -> DebugTextures.dump(mc);
      case RESOURCES -> {
        local.luke.power.visual.TextureOverrides.clear(); mc.textureManager.method_1096();
        net.modificationstation.stationapi.api.StationAPI.EVENT_BUS.post(
            net.modificationstation.stationapi.api.client.event.resource.AssetsReloadEvent.builder().build());
        mc.worldRenderer.method_1537(); message(mc,"Reloaded textures and resources");
      }
      case FOCUS -> {
        Object config=Class.forName("local.luke.power.controls.ControlFeatures").getField("GENERAL_CONFIG").get(null);
        var field=config.getClass().getField("pauseOnLostFocus"); boolean next=!Boolean.TRUE.equals(field.get(config)); field.set(config,next);
        var section=PowerConfig.section("power_controls:general"); section.addProperty("pauseOnLostFocus",next); PowerConfig.saveDeferred("power_controls:general",section);
        toggle(mc,"Pause on lost focus",next);
      }
      case HELP -> help(mc);
      case VERSION -> {
        message(mc,"Power Beta "+net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("power_beta").orElseThrow().getMetadata().getVersion().getFriendlyString()+" / Minecraft Beta 1.7.3");
        message(mc,"Java "+System.getProperty("java.version")+" / "+org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER));
      }
      case SPECTATE -> spectate(mc);
      case OPTIONS -> local.luke.power.ui.PowerOptionsScreen.openDebug(mc);
      case PAUSE -> mc.setScreen(new SilentPause());
      case PROFILER -> toggle(mc,"Profiler chart",profiler=!profiler);
      case FRAMES -> toggle(mc,"Frame / tick charts",frames=!frames);
      case NETWORK -> toggle(mc,"Network chart",network=!network);
      case LIGHT -> toggle(mc,"Light brightness table",light=!light);
      case CRASH -> {} // Deliberate hold, never an immediate key-down action.
    }
  }
  public static void copy(Minecraft mc,String text) {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text),null);
    message(mc,"Copied to clipboard");
  }
  private static void spectate(Minecraft mc) throws Exception {
    if(mc.world.isRemote || DetachedCamera.isActive()) { message(mc,"Switch modes using the server or leave freecam first."); return; }
    var mode=Class.forName("local.luke.power.creative.api.GameMode");
    String current=mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    String next=current.equals("SPECTATOR")?previousMode:"SPECTATOR";
    if(!current.equals("SPECTATOR")) previousMode=current;
    Object value=mode.getMethod("valueOf",String.class).invoke(null,next);
    Class.forName("local.luke.power.creative.Modes").getMethod("change",Minecraft.class,mode).invoke(null,mc,value);
  }
  private static void help(Minecraft mc) {
    String prefix=org.lwjgl.input.Keyboard.getKeyName(DebugKeys.MODIFIER.code);
    message(mc,"Debug shortcuts. Edit keys in Controls > Debug.");
    for(var action:DebugAction.values()) {
      int key=DebugKeys.binding(action).code;
      if(key!=0) message(mc,prefix+" + "+org.lwjgl.input.Keyboard.getKeyName(key)+": "+action.label);
    }
    message(mc,"Mode picker uses its configured modifier and cycle key in Controls.");
  }
  private static final class SilentPause extends Screen {
    @Override public void render(int x,int y,float delta) {}
    @Override protected void keyPressed(char c,int key) { if(key==org.lwjgl.input.Keyboard.KEY_ESCAPE) minecraft.setScreen(null); }
  }
}
