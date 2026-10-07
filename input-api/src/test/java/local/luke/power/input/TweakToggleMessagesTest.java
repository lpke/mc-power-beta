package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import local.luke.power.input.TweakIndicators.Tweak;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TweakToggleMessagesTest {
  private final EnumMap<Tweak, String> states = new EnumMap<>(Tweak.class);
  private final EnumMap<Tweak, Boolean> enabled = new EnumMap<>(Tweak.class);
  private final TweakToggleMessages messages = new TweakToggleMessages();

  @BeforeEach void register() {
    for (Tweak tweak : Tweak.values())
      TweakIndicators.register(tweak, () -> states.get(tweak), () -> enabled.getOrDefault(tweak, false));
  }

  @Test void everyTweakRequiresOptInAndEmitsOncePerTransition() {
    assertTrue(messages.update(true).isEmpty());
    for (Tweak tweak : Tweak.values()) {
      states.put(tweak, ""); assertTrue(messages.update(true).isEmpty());
      enabled.put(tweak, true); assertTrue(messages.update(true).isEmpty());
      states.remove(tweak);
      assertEquals(java.util.List.of(tweak.label + ": \u00a7cOFF\u00a7r"), messages.update(true));
      assertTrue(messages.update(true).isEmpty());
      states.put(tweak, "");
      assertEquals(java.util.List.of(tweak.label + ": \u00a7aON\u00a7r"), messages.update(true));
      assertTrue(messages.update(true).isEmpty());
      enabled.put(tweak, false); states.remove(tweak); assertTrue(messages.update(true).isEmpty());
    }
  }

  @Test void menusFocusChangesAndNewWorldsDoNotQueueMessages() {
    for (Tweak tweak : Tweak.values()) enabled.put(tweak, true);
    messages.update(false);
    states.put(Tweak.FAST_PLACEMENT, "");
    assertTrue(messages.update(false).isEmpty());
    assertTrue(messages.update(true).isEmpty());
    states.remove(Tweak.FAST_PLACEMENT);
    assertTrue(messages.update(false).isEmpty());
    assertTrue(messages.update(true).isEmpty());
    states.put(Tweak.FAST_PLACEMENT, ""); messages.reset();
    assertTrue(messages.update(true).isEmpty());
  }

  @Test void modeDetailsAndHudReadsDoNotGenerateToggleMessages() {
    enabled.put(Tweak.PLACEMENT_RESTRICTION, true);
    states.put(Tweak.PLACEMENT_RESTRICTION, "Face");
    messages.update(true);
    states.put(Tweak.PLACEMENT_RESTRICTION, "Plane");
    assertTrue(messages.update(true).isEmpty());
    states.remove(Tweak.PLACEMENT_RESTRICTION);
    assertNull(TweakIndicators.label(Tweak.PLACEMENT_RESTRICTION));
    assertEquals(1, messages.update(true).size());
  }
}
