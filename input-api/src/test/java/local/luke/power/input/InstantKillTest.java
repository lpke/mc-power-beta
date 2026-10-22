package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;
import local.luke.power.permissions.CommandPermissions;
import local.luke.power.rules.*;
import org.junit.jupiter.api.Test;

class InstantKillTest {
  @Test void toggleStaysWithTheExactOwnerAndRevocationCannotReviveIt() {
    var rules = new RuleSet();
    rules.set(GameRule.ALLOW_SURVIVAL_ASSISTANCE, true);
    CommandPermissions.context(() -> new CommandPermissions.Context("test", false, rules));
    Object player = new Object(), world = new Object();
    try {
      assertTrue(InstantKill.toggle(player, world));
      assertTrue(InstantKill.active(player, world));
      assertFalse(InstantKill.active(new Object(), world));
      assertFalse(InstantKill.active(player, new Object()));
      rules.set(GameRule.ALLOW_SURVIVAL_ASSISTANCE, false);
      assertFalse(InstantKill.active(player, world));
      rules.set(GameRule.ALLOW_SURVIVAL_ASSISTANCE, true);
      assertFalse(InstantKill.active(player, world));
      assertTrue(InstantKill.toggle(player, world));
      TweakLifecycle.register("instant-kill-test", InstantKill::clear);
      TweakLifecycle.reset();
      assertFalse(InstantKill.active(player, world));
      assertEquals(GameRule.ALLOW_SURVIVAL_ASSISTANCE, CommandPermissions.owner("ik"));
    } finally {
      InstantKill.clear();
      CommandPermissions.context(() -> new CommandPermissions.Context("", false, new RuleSet()));
    }
  }
}
