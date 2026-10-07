package local.luke.power.input;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import local.luke.power.input.TweakIndicators.Tweak;

/** Tick-only transition tracking. Suppressed ticks still absorb state changes silently. */
public final class TweakToggleMessages {
  private final EnumMap<Tweak, Boolean> previous = new EnumMap<>(Tweak.class);

  public void reset() { previous.clear(); }

  public List<String> update(boolean announce) {
    List<String> result = new ArrayList<>();
    for (Tweak tweak : Tweak.values()) {
      boolean active = TweakIndicators.label(tweak) != null;
      Boolean before = previous.put(tweak, active);
      if (announce && before != null && before != active && TweakIndicators.announces(tweak))
        result.add(tweak.label + ": " + (active ? "\u00a7aON\u00a7r" : "\u00a7cOFF\u00a7r"));
    }
    return result;
  }
}
