package local.luke.power.config.backend;
import local.luke.power.config.*;
import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.*;
import net.minecraft.client.resource.language.TranslationStorage;
import org.lwjgl.input.Keyboard;

public final class NativeBackend implements Backend {
  private final Minecraft mc;
  private final List<Setting> entries=new ArrayList<>();
  private final Map<String,Consumer<JsonElement>> setters=new LinkedHashMap<>();
  public NativeBackend(Minecraft mc)throws Exception{
    this.mc=mc;GameOptions o=mc.options;
    slider("music","Audio","Volume","Music",Option.MUSIC,0,100,1,100);
    slider("sound","Audio","Volume","Sound effects",Option.SOUND,0,100,1,100);
    slider("sensitivity","Controls","Mouse","Sensitivity",Option.SENSITIVITY,0,200,1,100);
    toggle("invert","Controls","Mouse","Invert mouse",Option.INVERT_MOUSE,o.invertYMouse,false);
    toggle("bobbing","Camera","View","View bobbing",Option.VIEW_BOBBING,o.bobView,true);
    toggle("anaglyph","Video","Rendering","3D anaglyph",Option.ANAGLYPH,o.anaglyph3d,false);
    toggle("opengl","Video","Rendering","Advanced OpenGL",Option.ADVANCED_OPENGL,o.advancedOpengl,false);
    toggle("ao","Video","Rendering","Smooth lighting",Option.AMBIENT_OCCLUSION,o.ao,true);
    toggle("fancy","Video","Rendering","Fancy graphics",Option.GRAPHICS,o.fancyGraphics,true);
    choice("difficulty","General","World","Difficulty",o.difficulty,2,List.of("Peaceful","Easy","Normal","Hard"),v->o.setInt(Option.DIFFICULTY,v-o.difficulty));
    Class<?> mod=Class.forName("net.danygames2014.unitweaks.util.ModOptions");
    for(String[] spec:new String[][]{
      {"fov","fovOption","Camera","View","Field of view","70","110","70"},
      {"brightness","brightnessOption","Video","Lighting","Brightness","0","100","0"},
      {"cloudHeight","cloudHeightOption","Video","Sky","Cloud height","108","256","108"},
      {"fogDensity","fogDensityOption","Video","Rendering","Fog density","0","200","100"},
      {"renderDistance","renderDistanceOption","Video","Rendering","Render distance, chunks","2","32","8"},
      {"fpsLimit","fpsLimitOption","Video","Performance","Frame limit, 300 is unlimited","5","300","125"},
      {"guiScale","guiScaleOption","Interface","Scale","GUI scale, 0 is automatic","0","8","0"}}){
      Option option=(Option)mod.getField(spec[1]).get(null);if(option==null)continue;
      slider(spec[0],spec[2],spec[3],spec[4],option,Integer.parseInt(spec[5]),Integer.parseInt(spec[6]),spec[0].equals("fpsLimit")?5:1,Integer.parseInt(spec[7]));
    }
    Option clouds=(Option)mod.getField("cloudsOption").get(null);
    if(clouds!=null)toggle("clouds","Video","Sky","Clouds",clouds,mod.getField("clouds").getBoolean(null),true);
    if(mod.getField("renderDistanceOption").get(null)==null)choice("renderDistance","Video","Rendering","Render distance",o.viewDistance,0,List.of("Far","Normal","Short","Tiny"),v->o.setInt(Option.RENDER_DISTANCE,v-o.viewDistance));
    if(mod.getField("fpsLimitOption").get(null)==null)choice("fpsLimit","Video","Performance","Frame limit",o.fpsLimit,1,List.of("Unlimited","Balanced","Power saver"),v->o.setInt(Option.FRAMERATE_LIMIT,v-o.fpsLimit));
    Map<String,Integer> used=new HashMap<>();
    for(KeyBinding k:o.allKeys){
      int index=used.merge(k.translationKey,1,Integer::sum);
      String key="keys."+k.translationKey+(index==1?"":"."+index);
      String label=TranslationStorage.getInstance().get(k.translationKey);
      if(label.equals(k.translationKey)){label=Catalog.words(k.translationKey.replaceAll("^(key\\.)?","").replaceAll("(lpketweaks|lpkecreative|omnilook|unitweaks|freecam|betterscreenshots|mojangfixstationapi)\\.","").replace('.',' '));}
      if(k.translationKey.toLowerCase(Locale.ROOT).contains("omnilook"))label="Free look";
      entries.add(new Setting(key,id(),"Controls","Key bindings",label,"Click to bind a key or mouse button. Escape cancels; Delete clears.",Setting.Kind.KEY,new JsonPrimitive(k.code),new JsonPrimitive(defaultKey(k)), -100,255,1,List.of(),false));
      setters.put(key,v->k.code=v.getAsInt());
    }
  }
  private static int defaultKey(KeyBinding k){return switch(k.translationKey){case "key.forward"->Keyboard.KEY_W;case "key.back"->Keyboard.KEY_S;case "key.left"->Keyboard.KEY_A;case "key.right"->Keyboard.KEY_D;case "key.jump"->Keyboard.KEY_SPACE;case "key.sneak"->Keyboard.KEY_LSHIFT;case "key.inventory"->Keyboard.KEY_E;case "key.drop"->Keyboard.KEY_Q;case "key.chat"->Keyboard.KEY_T;case "key.fog"->Keyboard.KEY_F;case "key.unitweaks.hide_hud"->Keyboard.KEY_F1;case "key.unitweaks.take_screenshot"->Keyboard.KEY_F2;case "key.unitweaks.debug_hud","lpkecreative.modifier"->Keyboard.KEY_F3;case "lpkecreative.picker"->Keyboard.KEY_F4;case "key.unitweaks.third_person"->Keyboard.KEY_F5;case "key.unitweaks.toggle_fullscreen"->Keyboard.KEY_F11;case "key.unitweaks.dismount"->Keyboard.KEY_LSHIFT;case "lpkecreative.sprint"->Keyboard.KEY_LCONTROL;case "playerList"->Keyboard.KEY_TAB;default->k.translationKey.matches("key.unitweaks.hotbar_[1-9]")?Integer.parseInt(k.translationKey.substring(k.translationKey.length()-1))+1:0;};}
  private void slider(String key,String page,String group,String label,Option option,double min,double max,double step,double defaults){
    float raw=mc.options.getFloat(option);double current=min+raw*(max-min);if(step>=1)current=Math.round(current/step)*step;
    entries.add(new Setting("native."+key,id(),page,group,label,"",Setting.Kind.INTEGER,new JsonPrimitive(current),new JsonPrimitive(defaults),min,max,step,List.of(),false));
    setters.put("native."+key,v->mc.options.setFloat(option,(float)Math.max(0,Math.min(1,(v.getAsDouble()-min)/(max-min)+(min==0&&max==100?0:0.000001)))));
  }
  private void toggle(String key,String page,String group,String label,Option option,boolean current,boolean defaults){
    entries.add(new Setting("native."+key,id(),page,group,label,"",Setting.Kind.BOOLEAN,new JsonPrimitive(current),new JsonPrimitive(defaults),0,1,1,List.of(),false));
    setters.put("native."+key,v->{boolean now=option==Option.GRAPHICS?mc.options.fancyGraphics:mc.options.getBoolean(option);if(now!=v.getAsBoolean())mc.options.setInt(option,1);});
  }
  private void choice(String key,String page,String group,String label,int current,int defaults,List<String> choices,IntConsumer setter){
    entries.add(new Setting("native."+key,id(),page,group,label,"",Setting.Kind.CHOICE,new JsonPrimitive(current),new JsonPrimitive(defaults),0,choices.size()-1,1,choices,false));setters.put("native."+key,v->setter.accept(v.getAsInt()));
  }
  public static void register(ConfigSession session,Minecraft mc)throws Exception{NativeBackend b=new NativeBackend(mc);session.add(b,b.entries);}
  public String id(){return "native";}
  public List<Path> files(){return List.of(FabricLoader.getInstance().getGameDir().resolve("options.txt"));}
  public void validate(Map<String,JsonElement> values){for(String key:values.keySet())if(!setters.containsKey(key))throw new IllegalArgumentException("Unknown game option");}
  public void apply(Map<String,JsonElement> values){values.forEach((key,value)->setters.get(key).accept(value));mc.options.save();local.luke.power.audio.AudioController.refresh();}
}
