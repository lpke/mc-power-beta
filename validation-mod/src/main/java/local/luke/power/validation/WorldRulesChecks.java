package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.commands.GameRuleCommand;
import local.luke.power.config.*;
import local.luke.power.death.DeathItems;
import local.luke.power.permissions.CommandPermissions;
import local.luke.power.rules.*;
import local.luke.power.ui.*;
import local.luke.power.world.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.WorldProperties;

final class WorldRulesChecks {
  static Setting find(ConfigSession session, String id) {
    return session.settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow(() -> new AssertionError("Missing " + id));
  }
  static void permissions(Minecraft mc) throws Exception {
    failures = 0;
    check(mc.world != null && !mc.world.isRemote, "requires a disposable singleplayer world");
    var before = AccessRules.copy(mc);
    var inventory = mc.player.inventory.main.clone();
    var cursor = mc.player.inventory.getCursorStack();
    int selected = mc.player.inventory.selectedSlot;
    String mode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    try {
      mc.setScreen(null); DeathChecks.alive(mc); CreativeVehicleChecks.mode(mc, "SURVIVAL");
      test("all 39 rules retain types, aliases, defaults, dimension copies and native NBT", () -> {
        check(GameRule.values().length == 39, "rule inventory differs from the decision record");
        RuleSet values = new RuleSet();
        for (GameRule rule : GameRule.values()) {
          check(GameRule.find(rule.id()) == rule && GameRule.find(rule.alias()) == rule, "alias mismatch " + rule);
          if (rule.integer()) values.set(rule, new com.google.gson.JsonPrimitive(73));
          else values.set(rule, !values.enabled(rule));
        }
        var properties = new WorldProperties(173L, "Rules fixture");
        ((WorldRules) properties).power$rules(values);
        ((WorldDifficulty) properties).power$difficulty(3);
        var saved = new WorldProperties(properties.asNbt());
        var dimension = new WorldProperties(saved);
        check(WorldRules.of(saved).equals(values) && WorldRules.of(dimension).equals(values), "native rules lost");
        check(((WorldDifficulty) dimension).power$difficulty() == 3, "dimension difficulty lost");
        WorldRules.of(dimension).set(GameRule.KEEP_INVENTORY, false);
        check(WorldRules.of(saved).enabled(GameRule.KEEP_INVENTORY), "dimension copy aliases rule memory");
      });
      test("future and malformed native rules remain blocked rather than silently resetting", () -> {
        var properties = new WorldProperties(173L, "Invalid rules");
        var tag = properties.asNbt(); tag.getCompound(WorldRuleData.TAG).putInt("version", 99);
        var future = new WorldProperties(tag);
        check(!((WorldRules) future).power$rulesProblem().isEmpty(), "future rules not blocked");
        try { future.asNbt(); throw new AssertionError("future rules overwritten"); }
        catch (IllegalStateException expected) {}
        tag.getCompound(WorldRuleData.TAG).putInt("version", RuleSet.VERSION);
        tag.getCompound(WorldRuleData.TAG).putString("fire_spread", "false");
        check(!((WorldRules)new WorldProperties(tag)).power$rulesProblem().isEmpty(), "wrong native type accepted");
        var legacy = new net.minecraft.nbt.NbtCompound();
        legacy.putByte("PowerBetaCheatsEnabled", (byte) 2);
        check(!((WorldRules)new WorldProperties(legacy)).power$rulesProblem().isEmpty(), "invalid old boolean widened access");
        legacy.putBoolean("PowerBetaCheatsEnabled", true);
        legacy.putInt("PowerBetaWorldEditAccess", 3);
        check(!((WorldRules)new WorldProperties(legacy)).power$rulesProblem().isEmpty(), "invalid old editing access accepted");
      });
      test("each access permission grants only its own command family", () -> {
        List<String> commands = List.of("give", "summon", "kill", "killall", "ride", "remove", "butcher",
            "heal", "god", "clear", "repair", "tp", "teleport", "spawn", "warp", "up", "thru",
            "spawnpoint", "setworldspawn", "time", "weather");
        for (GameRule allowed : GameRule.values()) if (allowed.access()) {
          var rules = new RuleSet(); rules.set(allowed, true); AccessRules.restore(mc, rules);
          for (String command : commands)
            check(CommandPermissions.allowed(command) == (CommandPermissions.owner(command) == allowed), allowed + " leaked to " + command);
          check(CommandPermissions.modeDenial("creative").isEmpty() == (allowed == GameRule.ALLOW_CREATIVE_MODE), "Creative permission mismatch");
          check(CommandPermissions.modeDenial("spectator").isEmpty() == (allowed == GameRule.ALLOW_SPECTATOR_MODE), "Spectator permission mismatch");
          check(CommandPermissions.modeDenial("survival").isEmpty(), "Survival unavailable");
          for (String command : List.of("seed", "difficulty", "hat", "help")) check(CommandPermissions.allowed(command), "safe command gated " + command);
        }
      });
      test("safe subcommands retain help and data while their mutations are blocked", () -> {
        AccessRules.restore(mc, new RuleSet());
        for (String[] args : List.of(new String[]{"time","query"}, new String[]{"warp","list"},
            new String[]{"warp","set","fixture"}, new String[]{"clear","264","0","0"}))
          check(CommandPermissions.denial(args[0], args).isEmpty(), "safe subcommand denied " + Arrays.toString(args));
        check(!CommandPermissions.denial("warp", new String[]{"warp","fixture"}).isEmpty(), "warp travel allowed");
        check(!CommandPermissions.denial("time", new String[]{"time","set","day"}).isEmpty(), "time edit allowed");
        check(ChatChecks.submit(mc, "/difficulty normal").stream().noneMatch(s -> s.startsWith("§c")), "difficulty gated");
      });
      test("repair changes only durability, and help and denied calls preserve the held item", () -> {
        var held = DeathChecks.tagged(267, 1, 83, "repair-native-data");
        mc.player.inventory.selectedSlot = 0; mc.player.inventory.main[0] = held;
        String original = DeathItems.encode(held);
        ChatChecks.submit(mc, "/repair"); check(original.equals(DeathItems.encode(held)), "denied repair changed item");
        AccessRules.set(mc, GameRule.ALLOW_SURVIVAL_ASSISTANCE, true);
        ChatChecks.submit(mc, "/repair help"); check(original.equals(DeathItems.encode(held)), "repair help changed item");
        var expected = DeathItems.decode(original); expected.setDamage(0);
        var output = ChatChecks.submit(mc, "/repair");
        check(mc.player.inventory.main[0] == held && DeathItems.encode(expected).equals(DeathItems.encode(held)), "repair changed identity/count/native NBT");
        check(output.stream().anyMatch(s -> s.contains("Repaired")), "missing repair feedback " + output);
        check(ChatChecks.submit(mc, "/repair").stream().anyMatch(s -> s.contains("already")), "already repaired feedback missing");
        mc.player.inventory.main[0] = null;
        check(ChatChecks.submit(mc, "/repair").stream().anyMatch(s -> s.contains("Hold")), "empty hand feedback missing");
      });
      test("mode revocation is independent and keeps the player's position", () -> {
        AccessRules.restore(mc, RuleSet.cheats());
        for (String active : List.of("CREATIVE", "SPECTATOR")) {
          AccessRules.of(mc.world.method_262()).allAllowed(true); CreativeVehicleChecks.mode(mc, active);
          double x = mc.player.x, y = mc.player.y, z = mc.player.z;
          var rules = AccessRules.copy(mc);
          rules.set(active.equals("CREATIVE") ? GameRule.ALLOW_CREATIVE_MODE : GameRule.ALLOW_SPECTATOR_MODE, false);
          WorldRuleChanges.apply(mc, rules);
          check(mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString().equals("SURVIVAL"), "revocation kept " + active);
          check(mc.player.x == x && mc.player.y == y && mc.player.z == z, "revocation moved the player");
          check(WorldRules.of(mc.world.method_262()).enabled(GameRule.KEEP_INVENTORY), "mode revocation erased keep inventory");
        }
      });
      test("gamerule query, validation, aliases and durable saves use the same registry", () -> {
        AccessRules.restore(mc, new RuleSet());
        WorldRuleChanges.save(mc);
        Path metadata = WorldRuleChanges.directory(mc).resolve("level.dat");
        for (GameRule rule : GameRule.values()) {
          byte[] saved = Files.readAllBytes(metadata);
          GameRuleCommand.execute(mc, new String[]{"gamerule", rule.id()});
          check(Arrays.equals(saved, Files.readAllBytes(metadata)), "query wrote world metadata");
          try { GameRuleCommand.execute(mc, new String[]{"gamerule", rule.id(), "invalid"}); throw new AssertionError("invalid value accepted " + rule); }
          catch (IllegalArgumentException expected) {}
          check(Arrays.equals(saved, Files.readAllBytes(metadata)), "invalid command wrote world metadata");
          GameRuleCommand.execute(mc, new String[]{"gamerule", rule.alias(), rule.integer() ? "73" : "false"});
          var persisted = WorldRuleData.read(WorldRuleFiles.read(metadata).getCompound("Data"));
          check(persisted.equals(AccessRules.copy(mc)), "persisted rule differs " + rule);
        }
      });
      ui(mc);
    } finally {
      AccessRules.restore(mc, before); CreativeVehicleChecks.mode(mc, mode);
      System.arraycopy(inventory, 0, mc.player.inventory.main, 0, inventory.length);
      mc.player.inventory.selectedSlot = selected; mc.player.inventory.setCursorStack(cursor);
      mc.setScreen(null); WorldRuleChanges.save(mc);
    }
    log("WORLD RULES FAILURES " + failures);
  }

  static void ui(Minecraft mc) throws Exception {
    test("rule editor exposes 39 searchable rules and independently hides unavailable tools", () -> {
      AccessRules.restore(mc, new RuleSet());
      var screen = new PowerOptionsScreen(null); mc.setScreen(screen);
      check(screen.visiblePages().contains("Game rules"), "missing rules tab");
      check(!screen.visiblePages().contains("World editing"), "unavailable editing tab visible");
      var session = screen.session();
      check(session.settings().stream().filter(s -> s.id.startsWith("rules.world.")).count() == 39, "missing rules");
      check(session.settings().stream().noneMatch(s -> s.id.equals("world.cheats") || s.backend.equals("commandAccess")), "obsolete access controls");
      for (var rule : GameRule.values()) check(SettingAccess.visible(session, find(session, "rules.world." + rule.id())), "rule hidden " + rule);
      find(session, "rules.world.allow_spectator_mode").parse("true"); call(screen, "layout");
      check(SettingAccess.visible(session, find(session, "creative.spectatorSpeed")), "Spectator settings hidden");
      check(!SettingAccess.visible(session, find(session, "creative.flight")), "Spectator exposed Creative controls");
      check(SettingAccess.visible(session, find(session, "keys.power_creative.sprint")), "shared sprint hidden");
      check(!WorldRules.of(mc.world.method_262()).enabled(GameRule.ALLOW_SPECTATOR_MODE), "manual draft changed world");
      session.discard();
      for (int width : new int[]{320, 640}) {
        screen.init(mc, width, width == 320 ? 240 : 420);
        field(screen, "page", "Game rules"); search(screen, "explosionsDestroyItems");
        check(((List<?>) field(screen, "rows")).size() > 0, "alias search empty");
        SettingsSnapshot.render(mc, screen, width, width == 320 ? 240 : 420,
            Path.of("power-beta-data/reports/rules-" + width + ".png"));
      }
      mc.setScreen(null);
    });
  }
}
