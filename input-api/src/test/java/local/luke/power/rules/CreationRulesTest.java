package local.luke.power.rules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static local.luke.power.rules.GameRule.*;

class CreationRulesTest {
  @Test void cyclingPreservesCustomDraftAndDoesNotChangeTemplates() {
    RuleSet survival = new RuleSet(), cheats = RuleSet.cheats();
    CreationRules draft = new CreationRules(survival, cheats);
    assertEquals("survival", draft.mode()); assertFalse(draft.rules().enabled(KEEP_INVENTORY));
    draft.cycle(1); assertEquals("creative", draft.mode()); assertTrue(draft.rules().enabled(KEEP_INVENTORY));
    draft.cycle(1); RuleSet custom = draft.custom(); custom.set(FIRE_SPREAD, false); draft.custom(custom);
    draft.cycle(1); draft.cycle(1); draft.cycle(1);
    assertFalse(draft.rules().enabled(FIRE_SPREAD)); assertTrue(survival.enabled(FIRE_SPREAD)); assertTrue(cheats.enabled(FIRE_SPREAD));
    custom.set(FIRE_SPREAD, true); assertFalse(draft.custom().enabled(FIRE_SPREAD));
    survival.set(FALL_DAMAGE, false); assertTrue(draft.template(false).enabled(FALL_DAMAGE));
  }
  @Test void modePermissionMismatchIsVisibleUntilTheDraftIsCorrected() {
    CreationRules draft = new CreationRules(new RuleSet(), RuleSet.cheats());
    draft.cycle(-1); draft.cycleMode(1);
    assertFalse(draft.problem().isEmpty());
    RuleSet rules = draft.custom(); rules.set(ALLOW_CREATIVE_MODE, true); draft.custom(rules);
    assertTrue(draft.problem().isEmpty());
    rules.set(ALLOW_CREATIVE_MODE, false); draft.custom(rules); assertFalse(draft.problem().isEmpty());
    draft.cycleMode(-1); assertTrue(draft.problem().isEmpty());
  }
}
