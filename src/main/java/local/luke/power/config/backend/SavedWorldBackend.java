package local.luke.power.config.backend;

import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.rules.RuleSet;
import local.luke.power.world.SavedWorldRules;

/** Uses the same rule rows as creation and loaded worlds, with no global settings. */
public final class SavedWorldBackend implements Backend {
  private final SavedWorldRules world;
  private SavedWorldBackend(SavedWorldRules world) { this.world = world; }
  public static ConfigSession open(SavedWorldRules world) {
    ConfigSession session = new ConfigSession();
    SavedWorldBackend backend = new SavedWorldBackend(world);
    session.add(backend, RuleSettings.entries(backend.id(), "rules.world.", world.rules(), new RuleSet()));
    return session;
  }
  public String id() { return "savedWorld"; }
  // SavedWorldRules owns its verified, recoverable transaction, including rollback saves.
  public List<Path> files() { return List.of(); }
  public void validate(Map<String, JsonElement> values) throws Exception {
    world.validate(); RuleSettings.edited(world.rules(), "rules.world.", values);
  }
  public void apply(Map<String, JsonElement> values) throws Exception {
    world.save(RuleSettings.edited(world.rules(), "rules.world.", values));
  }
}
