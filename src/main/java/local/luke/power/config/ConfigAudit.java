package local.luke.power.config;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import net.glasslauncher.mods.gcapi3.api.ConfigEntry;
import net.glasslauncher.mods.gcapi3.impl.GCCore;
import net.glasslauncher.mods.gcapi3.impl.object.*;
import net.minecraft.client.Minecraft;
public final class ConfigAudit {
  private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
  public static void write(Minecraft mc) throws Exception {
    List<Map<String,Object>> entries=new ArrayList<>();
    GCCore.MOD_CONFIGS.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(root -> visit(root.getKey(),"",root.getValue().configCategoryHandler(),entries));
    Path out=FabricLoader.getInstance().getGameDir().resolve("power-beta-settings-audit.json");
    Files.writeString(out,JSON.toJson(entries));
    local.luke.power.PowerBeta.LOG.info("Audited "+entries.size()+" settings");
    Files.writeString(out.resolveSibling("power-beta-full-catalog.json"), JSON.toJson(SettingsRegistry.open(mc).settings()));
  }
  private static void visit(String root,String group,ConfigCategoryHandler category,List<Map<String,Object>> entries) {
    for (ConfigHandlerBase child:category.values.values()) {
      if (child instanceof ConfigCategoryHandler nested) { visit(root,group+child.id+".",nested,entries);continue; }
      ConfigEntryHandler<?> value=(ConfigEntryHandler<?>)child;
      ConfigEntry meta=child.parentField.getAnnotation(ConfigEntry.class);
      Map<String,Object> entry=new LinkedHashMap<>();
      entry.put("id",root+"."+group+child.id);entry.put("root",root);entry.put("group",group);entry.put("label",child.name);entry.put("description",child.description);
      entry.put("type",child.parentField.getType().getName());entry.put("value",value.value);entry.put("default",value.defaultValue);
      entry.put("min",meta.minValue());entry.put("max",meta.maxValue());entry.put("restart",meta.requiresRestart());entry.put("hidden",meta.hidden());
      entries.add(entry);
    }
  }
}
