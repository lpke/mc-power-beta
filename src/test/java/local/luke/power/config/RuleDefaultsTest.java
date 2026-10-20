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
  @Test void legacyUneditedTemplatesFollowEachBaseWithoutMutatingTheSource() {
    for (var base : DefaultBases.Base.values()) for (var next : DefaultBases.Base.values()) {
      var source = defaults(base); var original = source.deepCopy();
      var replacement = RuleDefaults.followBase(source, new JsonObject(), base, next);
      assertEquals(original, source);
      if (base == next) { assertNull(replacement); continue; }
      assertEquals(RuleDefaults.latest(next, false).document(), replacement.get("survival"));
      assertEquals(RuleDefaults.latest(next, true).document(), replacement.get("cheats"));
      assertFalse(new RuleDefaults.Review(replacement, new JsonObject(), next).required());
    }
  }
  @Test void signaturesRecognizeUnchangedOldDefaultsButNeverAUserEdit() {
    var base = DefaultBases.Base.POWER_BETA; var source = defaults(base);
    source.add("review", RuleDefaults.acknowledgement(base));
    source.getAsJsonObject("review").addProperty("defaultsRevision", 1);
    source.getAsJsonObject("review").addProperty("release", "3.0.0");
    assertNotNull(RuleDefaults.followBase(source, new JsonObject(), base, DefaultBases.Base.BETA));
    // Key ordering is not an edit, including after Gson 2.8.9 reads a saved document.
    JsonObject values = source.getAsJsonObject("survival").getAsJsonObject("values");
    var sprint = values.remove("allow_sprinting"); values.add("allow_sprinting", sprint);
    source = JsonParser.parseString(source.toString()).getAsJsonObject();
    assertNotNull(RuleDefaults.followBase(source, new JsonObject(), base, DefaultBases.Base.BETA));
    source.getAsJsonObject("survival").getAsJsonObject("values").addProperty("allow_sprinting", false);
    assertNull(RuleDefaults.followBase(source, new JsonObject(), base, DefaultBases.Base.BETA));
  }
  @Test void customOrUnverifiableTemplatesAndFutureMetadataNeverFollowSilently() {
    var base = DefaultBases.Base.POWER_BETA;
    for (String fault : java.util.List.of("custom", "future", "unknown", "badSignature", "missingSignature", "futureReview", "unknownBase")) {
      var source = defaults(base); source.add("review", RuleDefaults.acknowledgement(base));
      var meta = source.getAsJsonObject("review");
      switch (fault) {
        case "custom" -> source.getAsJsonObject("cheats").getAsJsonObject("values").addProperty("allow_sleep", false);
        case "future" -> source.getAsJsonObject("survival").addProperty("version", 99);
        case "unknown" -> source.getAsJsonObject("survival").getAsJsonObject("values").addProperty("retired_rule", true);
        case "badSignature" -> meta.addProperty("defaultsSignature", false);
        case "missingSignature" -> meta.getAsJsonObject("defaultsSignature").remove("cheats");
        case "futureReview" -> meta.addProperty("format", 99);
        case "unknownBase" -> meta.addProperty("base", "future_base");
      }
      var original = source.deepCopy();
      assertNull(RuleDefaults.followBase(source, new JsonObject(), base, DefaultBases.Base.BETA), fault);
      assertEquals(original, source);
    }
  }
}
