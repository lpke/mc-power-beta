package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import net.minecraft.client.Minecraft;

public final class WorldBackend implements Backend {
  private final Minecraft mc;
  private final Object world;
  private final Class<?> type, api;

  private WorldBackend(Minecraft mc) throws Exception {
    this.mc = mc;
    world = mc.world;
    api = Class.forName("local.luke.power.worldedit.WorldEditor");
    type = Class.forName("local.luke.power.worldedit.config.WorldOverride");
  }

  public static void register(ConfigSession s, Minecraft mc) throws Exception {
    if (mc.world == null || mc.world.isRemote) return;
    WorldBackend b = new WorldBackend(mc);
    Enum<?> value = (Enum<?>) b.api.getMethod("worldOverride", Minecraft.class).invoke(null, mc);
    s.add(
        b,
        List.of(
            new Setting(
                "worldedit.worldOverride",
                b.id(),
                "World editing",
                "Access",
                "This world",
                "The master switch takes priority. This override takes priority over the"
                    + " creative-mode requirement.",
                Setting.Kind.CHOICE,
                new JsonPrimitive(value.ordinal()),
                new JsonPrimitive(0),
                0,
                2,
                1,
                List.of("Use global setting", "Enabled", "Disabled"),
                false)));
  }

  public String id() {
    return "world";
  }

  public List<Path> files() {
    return List.of();
  }

  public void validate(Map<String, JsonElement> values) {
    if (mc.world != world || mc.world == null || mc.world.isRemote)
      throw new IllegalArgumentException("The open world changed. Reopen Options.");
  }

  public void apply(Map<String, JsonElement> values) throws Exception {
    validate(values);
    api.getMethod("worldOverride", Minecraft.class, type)
        .invoke(
            null, mc, type.getEnumConstants()[values.get("worldedit.worldOverride").getAsInt()]);
  }
}
