package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.rules.*;

public final class CreationRulesBackend implements Backend {
  private final CreationRules draft;
  private CreationRulesBackend(CreationRules draft) { this.draft = draft; }
  public static ConfigSession open(CreationRules draft) {
    ConfigSession session = new ConfigSession();
    CreationRulesBackend backend = new CreationRulesBackend(draft);
    session.add(backend, RuleSettings.entries(backend.id(), "rules.creation.", draft.custom(), new RuleSet()));
    return session;
  }
  public String id() { return "creationRules"; }
  public List<Path> files() { return List.of(); }
  public void validate(Map<String, JsonElement> values) { RuleSettings.edited(draft.custom(), "rules.creation.", values); }
  public void apply(Map<String, JsonElement> values) { draft.custom(RuleSettings.edited(draft.custom(), "rules.creation.", values)); }
}
