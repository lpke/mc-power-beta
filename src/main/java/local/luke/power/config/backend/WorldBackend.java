package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.rules.*;
import local.luke.power.world.*;
import net.minecraft.client.Minecraft;

/** Loaded-world edits. The registry also supplies the creation and preset editors. */
public final class WorldBackend implements Backend {
  private final Minecraft mc;
  private final Object world;
  private WorldBackend(Minecraft mc) { this.mc = mc; world = mc.world; }

  public static void register(ConfigSession session, Minecraft mc) {
    if (mc.world == null) { RulePresetBackend.register(session); return; }
    if (mc.world.isRemote) return;
    WorldBackend backend = new WorldBackend(mc);
    List<Setting> entries = new ArrayList<>(RuleSettings.entries(backend.id(), "rules.world.", WorldRules.of(mc.world.method_262()), new RuleSet()));
    entries.add(new Setting("world.difficulty", backend.id(), "General", "Game", "Difficulty",
        "Set combat difficulty and hostile mob spawning. Peaceful removes hostile mobs.", Setting.Kind.CHOICE,
        new JsonPrimitive(((WorldDifficulty) mc.world.method_262()).power$difficulty()), new JsonPrimitive(WorldDifficulty.DEFAULT),
        0, 3, 1, List.of("Peaceful", "Easy", "Normal", "Hard"), false));
    session.add(backend, entries);
  }
  public String id() { return "world"; }
  public List<Path> files() { checkWorld(); return WorldRuleChanges.files(mc); }
  private void checkWorld() {
    if (mc.world != world || mc.world == null || mc.world.isRemote)
      throw new IllegalArgumentException("The open world changed. Reopen Options.");
  }
  public void validate(Map<String, JsonElement> values) {
    checkWorld();
    if (values.containsKey("world.difficulty")) WorldDifficulty.checked(values.get("world.difficulty").getAsBigDecimal().intValueExact());
    WorldRuleChanges.validate(mc, RuleSettings.edited(WorldRules.of(mc.world.method_262()), "rules.world.", values));
  }
  public void apply(Map<String, JsonElement> values) throws Exception {
    validate(values);
    WorldRuleChanges.apply(mc, RuleSettings.edited(WorldRules.of(mc.world.method_262()), "rules.world.", values));
    if (values.containsKey("world.difficulty")) {
      int difficulty = values.get("world.difficulty").getAsInt();
      ((WorldDifficulty) mc.world.method_262()).power$difficulty(difficulty);
      mc.world.field_213 = difficulty;
    }
    WorldRuleChanges.save(mc);
  }
}
