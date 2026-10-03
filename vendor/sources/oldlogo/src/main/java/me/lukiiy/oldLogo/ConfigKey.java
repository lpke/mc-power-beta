package me.lukiiy.oldLogo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class ConfigKey<T> {
    public final String key;
    public final T defaultValue;
    private final Function<JsonElement, T> parser;
    private final Function<T, JsonElement> serializer;
    private T stored;

    private ConfigKey(String key, T defaultValue, Function<JsonElement, T> parser, Function<T, JsonElement> serializer) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.parser = parser;
        this.serializer = serializer;

        writeDefault();
        load();
    }

    private void writeDefault() {
        if (defaultValue == null) return;

        OldLogo.CONFIG.setIfAbsent(key, serializer.apply(defaultValue));
    }

    public void load() {
        JsonElement raw = OldLogo.CONFIG.get(key);

        try {
            stored = raw != null ? parser.apply(raw) : defaultValue;
        } catch (Exception e) {
            stored = defaultValue;
        }
    }

    public T get() {
        return stored != null ? stored : defaultValue;
    }

    public void set(T value) {
        stored = value;

        OldLogo.CONFIG.set(key, serializer.apply(value));
    }

    public static ConfigKey<Boolean> bool(String key, boolean def) {
        return new ConfigKey<>(key, def, JsonElement::getAsBoolean, JsonPrimitive::new);
    }

    public static ConfigKey<Double> doubleVal(String key, double def) {
        return new ConfigKey<>(key, def, JsonElement::getAsDouble, JsonPrimitive::new);
    }

    public static ConfigKey<String> string(String key, String def) {
        return new ConfigKey<>(key, def, JsonElement::getAsString, JsonPrimitive::new);
    }

    public static ConfigKey<String[]> stringArray(String key, String[] def) {
        return new ConfigKey<>(key, def,
                e -> {
                    JsonArray array = e.getAsJsonArray();
                    String[] out = new String[array.size()];

                    for (int i = 0; i < array.size(); i++) out[i] = array.get(i).getAsString();
                    return out;
                },
                arr -> {
                    JsonArray array = new JsonArray();

                    for (String s : arr) array.add(s);
                    return array;
                }
        );
    }

    public static ConfigKey<float[]> floatArray(String key, float[] def) {
        return new ConfigKey<>(key, def,
                e -> {
                    JsonArray array = e.getAsJsonArray();
                    float[] out = new float[array.size()];

                    for (int i = 0; i < array.size(); i++) out[i] = array.get(i).getAsFloat();
                    return out;
                },
                arr -> {
                    JsonArray array = new JsonArray();

                    for (float f : arr) array.add(f);
                    return array;
                }
        );
    }

    public static ConfigKey<Map<Character, Integer>> charMap(String key, Map<Character, Integer> def) {
        return new ConfigKey<>(key, def,
                e -> {
                    Map<Character, Integer> out = new LinkedHashMap<>();
                    if (!e.isJsonObject()) return out;

                    JsonObject obj = e.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                        String k = entry.getKey();
                        JsonElement v = entry.getValue();

                        if (k.length() == 1 && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) out.put(k.charAt(0), v.getAsInt());
                    }

                    return out;
                },
                m -> {
                    JsonObject obj = new JsonObject();

                    for (Map.Entry<Character, Integer> entry : m.entrySet()) obj.addProperty(String.valueOf(entry.getKey()), entry.getValue());
                    return obj;
                }
        );
    }
}