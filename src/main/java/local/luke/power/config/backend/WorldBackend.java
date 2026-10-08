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
            new Setting("world.difficulty", b.id(), "General", "Game", "Difficulty",
                "Set combat difficulty and hostile mob spawning. Peaceful removes hostile mobs. Apply to confirm.",
                Setting.Kind.CHOICE, new JsonPrimitive(((local.luke.power.world.WorldDifficulty) b.properties()).power$difficulty()),
                new JsonPrimitive(local.luke.power.world.WorldDifficulty.DEFAULT), 0, 3, 1,
                List.of("Peaceful", "Easy", "Normal", "Hard"), false),
            new Setting(
                "world.cheats",
                b.id(),
                "General",
                "Game",
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
            cycle(b, "daylightCycle", "Daylight cycle", "Advance the sun, moon and day counter. Off freezes the current daylight time; gameplay and play time continue. Sleeping still sets your spawn and wakes you without advancing daylight.", ((local.luke.power.world.WorldCycles) b.properties()).power$daylightCycle()),
            cycle(b, "weatherCycle", "Weather cycle", "Allow weather to change naturally. Off freezes the current weather and pauses its timers. Sleeping cannot clear frozen weather. Turning on resumes the timers.", ((local.luke.power.world.WorldCycles) b.properties()).power$weatherCycle()),
            cycle(b, "hostileSpawning", "Hostile mob spawning", "Allow new hostile mobs to spawn naturally, including sleep ambushes. Existing mobs and /summon are unaffected. Requires cheats.", ((local.luke.power.world.WorldSpawning) b.properties()).power$hostileSpawning()),
            cycle(b, "passiveSpawning", "Passive mob spawning", "Allow animals and squid to spawn naturally. Existing mobs and /summon are unaffected. Requires cheats.", ((local.luke.power.world.WorldSpawning) b.properties()).power$passiveSpawning()),
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

  private static Setting cycle(WorldBackend b, String id, String label, String help, boolean value) {
    return new Setting("world." + id, b.id(), "General", "World overrides", label, help,
        Setting.Kind.BOOLEAN, new JsonPrimitive(value), new JsonPrimitive(true), 0, 1, 1, List.of(), false);
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
    checkWorld();
    if (values.containsKey("world.difficulty")) local.luke.power.world.WorldDifficulty.checked(values.get("world.difficulty").getAsBigDecimal().intValueExact());
    boolean cheats = values.containsKey("world.cheats") ? values.get("world.cheats").getAsBoolean() : properties().power$cheatsEnabled();
    if (!cheats && (values.containsKey("world.daylightCycle") || values.containsKey("world.weatherCycle") || values.containsKey("world.hostileSpawning") || values.containsKey("world.passiveSpawning")))
      throw new IllegalArgumentException("Enable cheats to change world rules.");
  }

  private void checkWorld() {
    if (mc.world != world || mc.world == null || mc.world.isRemote)
      throw new IllegalArgumentException("The open world changed. Reopen Options.");
  }

  public void apply(Map<String, JsonElement> values) throws Exception {
    validate(values);
    restore(values);
  }

  public void restore(Map<String, JsonElement> values) throws Exception {
    checkWorld();
    if (values.containsKey("world.difficulty")) {
      int difficulty = values.get("world.difficulty").getAsInt();
      ((local.luke.power.world.WorldDifficulty) properties()).power$difficulty(difficulty);
      mc.world.field_213 = difficulty;
    }
    if (values.containsKey("world.cheats")) {
      boolean enabled = values.get("world.cheats").getAsBoolean();
      if (!enabled && properties().power$cheatsEnabled()) {
        Class.forName("local.luke.power.creative.Modes")
            .getMethod("disableCheats", Minecraft.class)
            .invoke(null, mc);
      }
      properties().power$cheatsEnabled(enabled);
    }
    var spawning = (local.luke.power.world.WorldSpawning) properties();
    if (values.containsKey("world.hostileSpawning")) spawning.power$hostileSpawning(values.get("world.hostileSpawning").getAsBoolean());
    if (values.containsKey("world.passiveSpawning")) spawning.power$passiveSpawning(values.get("world.passiveSpawning").getAsBoolean());
    var cycles = (local.luke.power.world.WorldCycles) properties();
    if (values.containsKey("world.daylightCycle")) cycles.power$daylightCycle(values.get("world.daylightCycle").getAsBoolean());
    if (values.containsKey("world.weatherCycle")) cycles.power$weatherCycle(values.get("world.weatherCycle").getAsBoolean());
    if (values.containsKey("worldedit.worldOverride"))
      api.getMethod("worldOverride", Minecraft.class, type)
          .invoke(
              null, mc, type.getEnumConstants()[values.get("worldedit.worldOverride").getAsInt()]);
  }
}
