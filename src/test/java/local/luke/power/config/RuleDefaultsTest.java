package local.luke.power.config;
import com.google.gson.*;
import local.luke.power.rules.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RuleDefaultsTest {
  private JsonObject defaults(DefaultBases.Base base) {
    JsonObject section = new JsonObject();
    section.add("survival", RuleDefaults.latest(base, false).document()); section.add("cheats", RuleDefaults.latest(base, true).document()); return section;
  }
  @Test void sprintDefaultsUseTheCorrectBaseAndTemplate() {
    for (var base : DefaultBases.Base.values()) {
      assertEquals(base == DefaultBases.Base.POWER_BETA, RuleDefaults.latest(base, false).enabled(GameRule.ALLOW_SPRINTING));
      assertTrue(RuleDefaults.latest(base, true).enabled(GameRule.ALLOW_SPRINTING));
    }
  }
  @Test void customTemplatesPromptOncePerDefaultsTargetAndRetainPrivateChanges() {
    var base = DefaultBases.Base.POWER_BETA; var section = defaults(base);
    section.getAsJsonObject("survival").getAsJsonObject("values").addProperty("allow_sprinting", false);
    var review = new RuleDefaults.Review(section, new JsonObject(), base);
    assertTrue(review.compatible()); assertTrue(review.required());
    section.add("review", RuleDefaults.acknowledgement(base));
    assertFalse(new RuleDefaults.Review(section, new JsonObject(), base).required());
    assertTrue(new RuleDefaults.Review(section, new JsonObject(), DefaultBases.Base.BETA).required());
  }
  @Test void unknownRulesCanOnlyBeRemovedFromThePrivateDraftAndFutureFormatsCannotBeKept() {
    var section = defaults(DefaultBases.Base.POWER_BETA);
    section.getAsJsonObject("survival").getAsJsonObject("values").addProperty("retired_rule", true);
    var review = new RuleDefaults.Review(section, new JsonObject(), DefaultBases.Base.POWER_BETA);
    assertFalse(review.compatible());
    var retired = review.differences().stream().filter(d -> d.id().equals("retired_rule")).findFirst().orElseThrow();
    assertTrue(retired.removable()); review.remove(retired); assertTrue(review.compatible());
    assertTrue(section.getAsJsonObject("survival").getAsJsonObject("values").has("retired_rule"));
    section.getAsJsonObject("survival").addProperty("version", 99);
    var future = new RuleDefaults.Review(section, new JsonObject(), DefaultBases.Base.POWER_BETA);
    assertFalse(future.compatible()); assertTrue(future.required()); assertTrue(future.differences().stream().noneMatch(d -> d.template().equals("survival") && d.removable()));
  }
  @Test void reviewDependsOnDefaultsRevisionRatherThanUnrelatedPackVersionText() {
    var base = DefaultBases.Base.POWER_BETA; var section = defaults(base);
    section.getAsJsonObject("survival").getAsJsonObject("values").addProperty("allow_sprinting", false);
    JsonObject review = RuleDefaults.acknowledgement(base); review.addProperty("release", "3.1.1"); section.add("review", review);
    assertFalse(new RuleDefaults.Review(section,new JsonObject(),base).required());
    review.addProperty("defaultsRevision", 99);
    var future = new RuleDefaults.Review(section,new JsonObject(),base);
    assertTrue(future.required()); assertFalse(future.compatible());
  }
}
