package local.luke.power.rules;

/** A creation screen owns this draft. Neither presets nor existing worlds are edited. */
public final class CreationRules {
  public enum Preset { SURVIVAL, CHEATS, CUSTOM }
  private final RuleSet survival, cheats;
  private RuleSet custom;
  private Preset preset = Preset.SURVIVAL;
  public CreationRules(RuleSet survival, RuleSet cheats) {
    this.survival = survival.copy(); this.cheats = cheats.copy(); custom = survival.copy();
  }
  public Preset preset() { return preset; }
  public void cycle(int direction) { preset = Preset.values()[Math.floorMod(preset.ordinal() + direction, 3)]; }
  public RuleSet rules() { return switch (preset) { case SURVIVAL -> survival.copy(); case CHEATS -> cheats.copy(); case CUSTOM -> custom.copy(); }; }
  public RuleSet custom() { return custom.copy(); }
  public void custom(RuleSet rules) { custom = rules.copy(); }
  public RuleSet template(boolean cheat) { return (cheat ? cheats : survival).copy(); }
  public String mode() { return preset == Preset.CHEATS ? "creative" : "survival"; }
  public String problem() {
    if (mode().equals("survival")) return "";
    GameRule rule = mode().equals("creative") ? GameRule.ALLOW_CREATIVE_MODE : GameRule.ALLOW_SPECTATOR_MODE;
    return rules().enabled(rule) ? "" : "Turn on " + rule.label() + " or choose Survival rules.";
  }
}
