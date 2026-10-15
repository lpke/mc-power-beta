package local.luke.power.validation;

import local.luke.power.rules.*;

/** Fixture setup only. Production actions must check their individual permission. */
final class AccessRules {
  private final WorldRules world;
  private AccessRules(Object properties) { world = (WorldRules) properties; }
  static AccessRules of(Object properties) { return new AccessRules(properties); }
  boolean allAllowed() {
    for (GameRule rule : GameRule.values()) if (rule.access() && !world.power$rules().enabled(rule)) return false;
    return true;
  }
  void allAllowed(boolean allowed) {
    RuleSet rules = world.power$rules().copy();
    for (GameRule rule : GameRule.values()) if (rule.access()) rules.set(rule, allowed);
    world.power$rules(rules);
  }
  static RuleSet copy(net.minecraft.client.Minecraft mc) { return WorldRules.of(mc.world.method_262()).copy(); }
  static void restore(net.minecraft.client.Minecraft mc, RuleSet rules) { ((WorldRules)mc.world.method_262()).power$rules(rules); }
  static void set(net.minecraft.client.Minecraft mc, GameRule rule, boolean value) { WorldRules.of(mc.world.method_262()).set(rule, value); }
}
