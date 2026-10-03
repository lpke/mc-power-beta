package local.luke.power.title;

import com.google.gson.*;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Config {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private JsonObject jsonObj = new JsonObject();
    public Config(String ignored) { load(); }
    public void load() { jsonObj = local.luke.power.storage.PowerConfig.section("title"); }
    public void save() {
        try { local.luke.power.storage.PowerConfig.put("title", jsonObj); }
        catch (IOException e) { throw new IllegalStateException("Could not save title settings", e); }
    }

    public boolean has(String key) {
        return get(key) != null;
    }

    public JsonElement get(String key) {
        String[] parts = key.split("\\.");
        JsonElement current = jsonObj;

        for (String part : parts) {
            if (!current.isJsonObject()) return null;

            current = current.getAsJsonObject().get(part);
            if (current == null) return null;
        }

        return current;
    }

    public void set(String key, JsonElement value) {
        JsonObject parent = getObjectPath(key);
        String leaf = key.substring(key.lastIndexOf('.') + 1);

        parent.add(leaf, value);
        save();
    }

    public void setIfAbsent(String key, JsonElement value) {
        if (has(key)) return;

        set(key, value);
    }

    private JsonObject getObjectPath(String key) {
        String[] parts = key.split("\\.");
        JsonObject current = jsonObj;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];

            JsonElement next = current.get(part);
            if (next == null || !next.isJsonObject()) {
                JsonObject created = new JsonObject();

                current.add(part, created);
                current = created;
            } else {
                current = next.getAsJsonObject();
            }
        }

        return current;
    }
}
