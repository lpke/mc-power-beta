package local.luke.power.worldedit.config;

public enum WorldOverride {
  INHERIT("Use global setting"),
  ENABLED("Enabled"),
  DISABLED("Disabled");
  public final String label;

  WorldOverride(String label) {
    this.label = label;
  }

  public WorldOverride next() {
    return values()[(ordinal() + 1) % values().length];
  }

  public static WorldOverride decode(int value) {
    return value >= 0 && value < values().length ? values()[value] : INHERIT;
  }
}
