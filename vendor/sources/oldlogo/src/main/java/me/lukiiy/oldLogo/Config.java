package me.lukiiy.oldLogo;

import com.google.gson.*;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Config {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private JsonObject jsonObj = new JsonObject();
    private final File file;

    public Config(String fileName) {
        File confDir = new File(Minecraft.getRunDirectory(), "config");
        if (!confDir.exists()) confDir.mkdirs();

        this.file = new File(confDir, fileName + ".json");
        load();
    }

    public void load() {
        if (!file.exists()) {
            save();
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            jsonObj = gson.fromJson(reader, JsonObject.class);

            if (jsonObj == null) jsonObj = new JsonObject();
        } catch (IOException e) {
            OldLogo.LOGGER.error(e.getMessage());
        }
    }

    public void save() {
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)) {
            gson.toJson(jsonObj, writer);
        } catch (IOException e) {
            OldLogo.LOGGER.error(e.getMessage());
        }
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
