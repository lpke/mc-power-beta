package local.luke.power.rules;

/** Implemented by native world properties; shared between mapping sets. */
public interface WorldRules {
  RuleSet power$rules();
  void power$rules(RuleSet rules);
  default String power$rulesProblem() { return ""; }
  static RuleSet of(Object properties) { return ((WorldRules) properties).power$rules(); }
  static boolean enabled(Object properties, GameRule rule) { return of(properties).enabled(rule); }
}
