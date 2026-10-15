package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.rules.*;
import local.luke.power.world.*;
import net.minecraft.class_591;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.nbt.*;
import net.minecraft.world.WorldProperties;

final class Release202Checks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    test("camera and leaf defaults agree with their reset values", () -> {
      var session = SettingsRegistry.open(mc);
      for (var entry : Map.of("power_camera:config.speed", "12.0", "power_camera:config.drag", "10.0",
          "power_controls:tweaks.beta18LeavesRendering", "true").entrySet()) {
        var row = WorldRulesChecks.find(session, entry.getKey());
        check(row.defaultValue.equals(com.google.gson.JsonParser.parseString(entry.getValue())), "wrong reset default " + entry.getKey());
      }
      session.discard();
    });
    mc.setWorld(null);
    test("world badges use Creative permission, preserve old worlds and reject invalid rules", () -> {
      Map<String, byte[]> originals = new LinkedHashMap<>();
      for (String kind : List.of("Survival", "Creative allowed", "Spectator only", "Old cheats", "Invalid rules")) {
        String name = "202 " + kind;
        NbtCompound data = new WorldProperties(202, name).asNbt();
        RuleSet rules = new RuleSet();
        rules.set(GameRule.ALLOW_CREATIVE_MODE, kind.equals("Creative allowed"));
        rules.set(GameRule.ALLOW_SPECTATOR_MODE, kind.equals("Spectator only"));
        WorldRuleData.write(data, rules);
        if (kind.equals("Old cheats")) {
          data.values().removeIf(value -> ((NbtElement)value).getKey().equals(WorldRuleData.TAG));
          data.putBoolean("PowerBetaCheatsEnabled", true);
        }
        if (kind.equals("Invalid rules")) data.getCompound(WorldRuleData.TAG).putInt("version", 999);
        var root = new NbtCompound(); root.put("Data", data);
        Path file = Path.of("saves", name, "level.dat"); Files.createDirectories(file.getParent());
        try (var stream = Files.newOutputStream(file)) { NbtIo.writeCompressed(root, stream); }
        originals.put(name, Files.readAllBytes(file));
      }
      int seen = 0;
      for (Object value : mc.method_2127().method_1002()) {
        class_591 metadata = (class_591)value;
        String name = metadata.method_1956();
        if (!originals.containsKey(name)) continue;
        boolean badge = (Boolean)metadata.getClass().getMethod("creative_isCreative").invoke(metadata);
        check(badge == (name.equals("202 Creative allowed") || name.equals("202 Old cheats")), "wrong badge " + name);
        check(Arrays.equals(originals.get(name), Files.readAllBytes(Path.of("saves", name, "level.dat"))), "listing changed " + name);
        seen++;
      }
      check(seen == originals.size(), "fixture worlds missing from native list");
      var screen = new SelectWorldScreen(null); mc.setScreen(screen);
      for (int width : new int[]{320, 640}) {
        screen.init(mc, width, width == 320 ? 240 : 420);
        SettingsSnapshot.render(mc, screen, width, width == 320 ? 240 : 420,
            Path.of("power-beta-data/reports/world-list-202-" + width + ".png"));
      }
      mc.setScreen(null);
    });
    log("RELEASE 202 FAILURES " + failures);
  }
}
