package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.permissions.CheatWorld;
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
                "world.cheats",
                b.id(),
                "General",
                "World",
                "Cheats enabled",
                "Allow game mode changes, item spawning, teleporting and world editing in this"
                    + " world. Turning off returns you to survival. Saved warps, inventories and"
                    + " access rules are kept. Apply to confirm.",
                Setting.Kind.BOOLEAN,
                new JsonPrimitive(b.properties().power$cheatsEnabled()),
                new JsonPrimitive(false),
                0,
                1,
                1,
                List.of(),
                false),
            new Setting(
                "worldedit.worldOverride",
                b.id(),
                "World editing",
                "Access",
                "This world",
                "Further restrict editing in this world. Cheats enabled and the global editing"
                    + " switch must both be on.",
                Setting.Kind.CHOICE,
                new JsonPrimitive(value.ordinal()),
                new JsonPrimitive(0),
                0,
                2,
                1,
                List.of("Use global setting", "Enabled", "Disabled"),
                false)));
  }

  private CheatWorld properties() {
    return (CheatWorld) mc.world.method_262();
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
    if (values.containsKey("world.cheats")) {
      boolean enabled = values.get("world.cheats").getAsBoolean();
      if (!enabled && properties().power$cheatsEnabled()) {
        Class.forName("local.luke.power.creative.Modes")
            .getMethod("disableCheats", Minecraft.class)
            .invoke(null, mc);
      }
      properties().power$cheatsEnabled(enabled);
    }
    if (values.containsKey("worldedit.worldOverride"))
      api.getMethod("worldOverride", Minecraft.class, type)
          .invoke(
              null, mc, type.getEnumConstants()[values.get("worldedit.worldOverride").getAsInt()]);
  }
}
