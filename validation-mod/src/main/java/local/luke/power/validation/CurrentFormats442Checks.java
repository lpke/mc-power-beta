package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.rules.*;
import local.luke.power.storage.*;
import local.luke.power.world.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Read-only format and native item validation after the game has initialized its registries. */
final class CurrentFormats442Checks {
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null, "open a muted disposable singleplayer clone");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable clone");
    failures = 0;
    Path game = FabricLoader.getInstance().getGameDir();
    byte[] config = Files.readAllBytes(PowerConfig.path());
    JsonObject document = PowerConfig.document();
    test("FORMATS442 current configuration and templates preserve exact stored data", () -> {
      check(ConfigVersions.version(document) == 7, "current schema is not 7");
      check(ConfigVersions.convert(document).equals(document), "current validation changed global data");
      JsonObject templates = document.getAsJsonObject("settings").getAsJsonObject("gameRules");
      for (String name : List.of("survival", "cheats"))
        check(TemplateDocuments.read(templates.get(name), name.equals("cheats")).document().equals(templates.get(name)), "template did not round-trip " + name);
      check(Arrays.equals(config, Files.readAllBytes(PowerConfig.path())), "validation wrote configuration");
    });
    test("FORMATS442 every saved world validates current metadata and full native item snapshots without writes", () -> {
      int files = 0, worlds = 0;
      try (var saves = Files.list(game.resolve("saves"))) {
        for (Path world : saves.filter(Files::isDirectory).sorted().toList()) {
          var plan = WorldRuleFiles.planUpgrade(world);
          check(plan.planned().isEmpty(), "world still requires metadata conversion " + world.getFileName());
          for (var entry : plan.originals().entrySet()) {
            var data = WorldRuleFiles.read(entry.getKey()).getCompound("Data");
            check(data.getCompound(WorldRuleData.TAG).getInt("version") == 4, "metadata is not format 4");
            check(Arrays.equals(entry.getValue(), Files.readAllBytes(entry.getKey())), "metadata validation wrote bytes");
            files++;
          }
          if (!plan.originals().isEmpty()) worlds++;
        }
      }
      check(files > 0, "no saved metadata was checked");
      log("FORMATS442 native validation worlds=" + worlds + " metadataFiles=" + files);
    });
    test("FORMATS442 old and future formats reject with preserved documents", () -> {
      for (int version : new int[]{3, 4, 5, 6, 8, 99}) {
        JsonObject old = document.deepCopy(); old.addProperty("schemaVersion", version); JsonObject before = old.deepCopy();
        boolean blocked = false;
        try { ConfigVersions.convert(old); }
        catch (IllegalArgumentException expected) { blocked = expected.getMessage().contains("4.4.1"); }
        check(blocked && old.equals(before), "unsupported global data admitted or rewritten");
      }
      for (int version : new int[]{0, 1, 2, 3, 5, 99}) {
        var data = new net.minecraft.nbt.NbtCompound();
        if (version != 0) { WorldRuleData.write(data, RuleSet.cheats()); data.getCompound(WorldRuleData.TAG).putInt("version", version); }
        boolean blocked = false;
        try { WorldRuleData.read(data); }
        catch (IllegalArgumentException expected) { blocked = expected.getMessage().contains("4.4.1"); }
        check(blocked, "unsupported world data admitted");
        var properties = new net.minecraft.world.WorldProperties(data);
        check(!((WorldRules)properties).power$rulesProblem().isEmpty(), "native existing-world constructor admitted unsupported rules");
      }
    });
    test("FORMATS442 new native worlds initialize current rules without legacy metadata", () -> {
      var properties = new net.minecraft.world.WorldProperties(42L, "Fresh format fixture");
      check(((WorldRules)properties).power$rulesProblem().isEmpty(), "fresh native construction was blocked");
      var data = properties.asNbt();
      check(data.getCompound(WorldRuleData.TAG).getInt("version") == 4, "fresh rules did not use current format");
      check(WorldRuleData.read(data).equals(WorldRules.of(properties)), "fresh current rules did not round-trip");
    });
    check(Arrays.equals(config, Files.readAllBytes(PowerConfig.path())), "format fixture wrote configuration");
    log("CURRENT FORMATS 442 FAILURES " + failures); check(failures == 0, "current format failures=" + failures);
  }
}
