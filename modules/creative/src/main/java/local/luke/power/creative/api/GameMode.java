package local.luke.power.creative.api;

import java.util.Locale;

public enum GameMode {
  SURVIVAL("Survival"),
  CREATIVE("Creative"),
  SPECTATOR("Spectator");
  public final String label;

  GameMode(String label) {
    this.label = label;
  }

  public GameMode next() {
    return values()[(ordinal() + 1) % values().length];
  }

  public static GameMode parse(String value) {
    return switch (value.toLowerCase(Locale.ROOT)) {
      case "survival", "s", "0" -> SURVIVAL;
      case "creative", "c", "1" -> CREATIVE;
      case "spectator", "sp", "3" -> SPECTATOR;
      default -> throw new IllegalArgumentException("Expected survival, creative or spectator.");
    };
  }
}
