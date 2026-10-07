package local.luke.power.config;

import com.google.gson.*;
import java.util.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.option.GameOptions;
import local.luke.power.controls.util.ModOptions;

/** Native settings and custom bindings share the same transaction as every feature. */
public final class NativeStorage {
  private static final Map<String,String> EXTENDED = Map.ofEntries(
      Map.entry("fov","fov"),Map.entry("brightness","brightness"),Map.entry("cloud_height","cloudHeight"),
      Map.entry("clouds","clouds"),Map.entry("fog_density","fogDensity"),
      Map.entry("render_distance","renderDistance"),Map.entry("gui_scale","guiScale"));
  public static void load(GameOptions options) {
    JsonObject data = PowerConfig.section("native");
    try {
      for (var e : data.entrySet()) {
        JsonElement value=e.getValue();
        switch(e.getKey()) {
          case "music" -> options.musicVolume=value.getAsFloat();
          case "sound" -> options.soundVolume=value.getAsFloat();
          case "mouseSensitivity" -> options.mouseSensitivity=value.getAsFloat();
          case "invertYMouse" -> options.invertYMouse=value.getAsBoolean();
          case "viewDistance" -> options.viewDistance=value.getAsInt();
          case "bobView" -> options.bobView=value.getAsBoolean();
          case "anaglyph3d" -> options.anaglyph3d=value.getAsBoolean();
          case "advancedOpengl" -> options.advancedOpengl=value.getAsBoolean();
          case "fpsLimit" -> options.fpsLimit=value.getAsInt();
          case "fancyGraphics" -> options.fancyGraphics=value.getAsBoolean();
          case "ao" -> options.ao=value.getAsBoolean();
          case "skin" -> options.skin=value.getAsString();
          case "lastServer" -> options.lastServer=value.getAsString();
          case "guiScale" -> options.guiScale=value.getAsInt();
        }
      }
      for (var e : EXTENDED.entrySet()) if (data.has(e.getKey())) {
        var field = ModOptions.class.getField(e.getValue()); field.set(null,Catalog.JSON.fromJson(data.get(e.getKey()),field.getType()));
      }
      if (data.has("framerate_limit")) {
        int limit = Math.max(5, Math.min(1005, data.get("framerate_limit").getAsInt()));
        ModOptions.fpsLimit = (limit - 5) / 1000F;
      }
      ModOptions.realGuiScale = ModOptions.guiScale;
      for (var key : options.allKeys) if (data.has("key_"+key.translationKey)) key.code = data.get("key_"+key.translationKey).getAsInt();
    } catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot load game options",e); }
  }
  public static void save(GameOptions options) {
    JsonObject data = PowerConfig.section("native");
    try {
      data.addProperty("music",options.musicVolume); data.addProperty("sound",options.soundVolume);
      data.addProperty("mouseSensitivity",options.mouseSensitivity); data.addProperty("invertYMouse",options.invertYMouse);
      data.addProperty("viewDistance",options.viewDistance); data.addProperty("bobView",options.bobView);
      data.addProperty("anaglyph3d",options.anaglyph3d); data.addProperty("advancedOpengl",options.advancedOpengl);
      data.addProperty("fpsLimit",options.fpsLimit); data.addProperty("fancyGraphics",options.fancyGraphics);
      data.addProperty("ao",options.ao);
      data.addProperty("skin",options.skin); data.addProperty("lastServer",options.lastServer);
      data.addProperty("guiScale",options.guiScale);
      data.addProperty("framerate_limit", ModOptions.getFpsLimitValue());
      for (var e : EXTENDED.entrySet()) data.add(e.getKey(),Catalog.JSON.toJsonTree(ModOptions.class.getField(e.getValue()).get(null)));
      for (var key : options.allKeys) data.addProperty("key_"+key.translationKey,key.code);
      PowerConfig.put("native",data);
    } catch (Exception e) { throw new IllegalStateException("Cannot save game options",e); }
  }
}
