package local.luke.power.config;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class Catalog {
  public static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
  private static final JsonObject DATA=load();
  private static JsonObject load(){try(var in=Catalog.class.getResourceAsStream("/assets/powerbeta/settings-catalog.json")){if(in==null)throw new IOException("Missing settings catalog");return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException e){throw new IllegalStateException(e);}}
  public static JsonObject metadata(String id){return DATA.has(id)?DATA.getAsJsonObject(id):new JsonObject();}
  public static String text(JsonObject o,String key,String fallback){return o.has(key)?o.get(key).getAsString():fallback;}
  public static double number(JsonObject o,String key,double fallback){return o.has(key)?o.get(key).getAsDouble():fallback;}
  public static String words(String s){String v=s.replaceAll("([a-z0-9])([A-Z])","$1 $2").replace('_',' ').toLowerCase(Locale.ROOT);return v.isEmpty()?v:Character.toUpperCase(v.charAt(0))+v.substring(1);}
  public static Setting setting(String id,String backend,String page,String group,String label,String description,Setting.Kind kind,JsonElement current,JsonElement defaults,double min,double max,double step,List<String> choices,boolean restart){
    JsonObject m=metadata(id);
    return new Setting(id,backend,text(m,"page",page),text(m,"group",group),text(m,"label",label),text(m,"description",description),kind,current,m.has("default")?m.get("default"):defaults,number(m,"min",min),number(m,"max",max),number(m,"step",step),choices,m.has("restart")?m.get("restart").getAsBoolean():restart);
  }
}
