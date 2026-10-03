package local.luke.power.config.api;

import com.google.gson.*;
import java.io.IOException;
import local.luke.power.storage.PowerConfig;
import net.glasslauncher.mods.gcapi3.impl.GlassYamlFile;
import org.simpleyaml.configuration.ConfigurationSection;

/** Keeps the annotation API while replacing per-module YAML files with one JSON document. */
public final class ConfigDocuments extends GlassYamlFile {
  private final String section;
  private ConfigDocuments(String section) { this.section = section; }
  public static GlassYamlFile open(String module, String category) { return new ConfigDocuments(module + ":" + category); }
  @Override public void createOrLoad() throws IOException { loadFromString(PowerConfig.section(section).toString()); }
  @Override public void createNewFile() { }
  @Override public void save() throws IOException { PowerConfig.put(section, values(this)); }
  private static JsonObject values(ConfigurationSection source) {
    JsonObject result = new JsonObject();
    for (String key : source.getKeys(false)) {
      Object value = source.get(key);
      result.add(key, value instanceof ConfigurationSection nested ? values(nested) : new Gson().toJsonTree(value));
    }
    return result;
  }
}
