package local.luke.power.world;

import local.luke.power.input.*;
import local.luke.power.mixin.WorldStorageAccessor;
import local.luke.power.rules.*;
import net.minecraft.client.Minecraft;

/** Shared mutation boundary for the rules screen and /gamerule. */
public final class WorldRuleChanges {
  private WorldRuleChanges() {}
  private static boolean revoked(Minecraft mc, RuleSet rules) {
    return mc.player instanceof PlayerMode mode && (mode.power$creative() && !rules.enabled(GameRule.ALLOW_CREATIVE_MODE)
        || mode.power$spectator() && !rules.enabled(GameRule.ALLOW_SPECTATOR_MODE));
  }
  public static void validate(Minecraft mc, RuleSet rules) {
    if (mc.world == null || mc.world.isRemote) throw new IllegalArgumentException("Open a singleplayer world first.");
    if (revoked(mc, rules) && InteractionState.carryingContainer)
      throw new IllegalArgumentException("Place your carried container before disabling the active game mode.");
  }
  public static void apply(Minecraft mc, RuleSet rules) throws Exception {
    validate(mc, rules);
    if (revoked(mc, rules)) Class.forName("local.luke.power.creative.Modes")
        .getMethod("returnToSurvival", Minecraft.class).invoke(null, mc);
    ((WorldRules) mc.world.method_262()).power$rules(rules);
    local.luke.power.sprint.SprintController.rulesChanged(mc);
    if (!rules.enabled(GameRule.ALLOW_SURVIVAL_ASSISTANCE))
      Class.forName("local.luke.power.commands.command.extra.God").getMethod("clear").invoke(null);
  }
  public static void save(Minecraft mc) throws java.io.IOException {
    mc.world.method_251();
    ((WorldStorageAccessor) mc.world).power$storage().method_1733(mc.world.method_262(), mc.world.field_200);
    WorldRuleFiles.verifySaved(directory(mc), WorldRules.of(mc.world.method_262()), ((WorldDifficulty) mc.world.method_262()).power$difficulty());
  }
  public static java.nio.file.Path directory(Minecraft mc) {
    return ((WorldStorageAccessor) mc.world).power$storage().method_1736("power-beta-location").toPath().getParent().getParent();
  }
  public static java.util.List<java.nio.file.Path> files(Minecraft mc) {
    java.nio.file.Path world = directory(mc);
    return java.util.List.of(world.resolve("level.dat"), world.resolve("level.dat_old"), world.resolve("level.dat_new"));
  }
}
