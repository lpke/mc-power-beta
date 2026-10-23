package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.tools.*;
import local.luke.power.storage.PowerConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Preferences-only world switches. Native items, world metadata and other preferences stay unchanged. */
final class Sorting440Checks {
  private Sorting440Checks() {}
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && WorldSortingLayouts.available(), "load a verified disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    PowerConfig.flushPending(); JsonObject document = PowerConfig.document(); var tools = InventoryToolsConfig.copy();
    String items = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
    Map<Field, Object> cache = new LinkedHashMap<>();
    for (String name : List.of("data", "world", "problem")) {
      Field field = WorldSortingLayouts.class.getDeclaredField(name); field.setAccessible(true); cache.put(field, field.get(null));
    }
    Path game = FabricLoader.getInstance().getGameDir(); failures = 0;
    String first = UUID.randomUUID().toString(), second = UUID.randomUUID().toString();
    try {
      var data = WorldSortingLayoutsData.read(document.getAsJsonObject("settings").getAsJsonObject(WorldSortingLayoutsData.SECTION)).seed(List.of(first, second));
      PowerConfig.replaceSection(WorldSortingLayoutsData.SECTION, document.getAsJsonObject("settings").get(WorldSortingLayoutsData.SECTION), data.json());
      cache.keySet().stream().filter(f -> f.getName().equals("data")).findFirst().orElseThrow().set(null, data);
      test("SORTING440 queued layouts and named presets belong only to their world", () -> {
        WorldSortingLayouts.activate(first, game.resolve("saves"));
        InventoryToolsConfig.queuedLayout(Set.of(35), Map.of(34, "35:4"));
        var presets = SortingLayoutsData.read(new JsonObject()).save("First world", Catalog.JSON.toJsonTree(Set.of(35)), Catalog.JSON.toJsonTree(Map.of(34, "35:4")));
        SortingLayoutPresets.queued(SortingLayoutPresets.snapshot(), presets.json()); PowerConfig.flushPending();
        WorldSortingLayouts.activate(second, game.resolve("saves"));
        var expected = data.layout(second);
        check(Catalog.JSON.toJsonTree(InventoryToolsConfig.current().protectedSlots).equals(expected.get("protectedSlots")), "second world inherited the first world's protected slots");
        check(Catalog.JSON.toJsonTree(InventoryToolsConfig.current().preferredItems).equals(expected.get("preferredItems")), "second world inherited the first world's item preferences");
        WorldSortingLayouts.activate(first, game.resolve("saves"));
        check(InventoryToolsConfig.current().protectedSlots.equals(List.of(35)) && InventoryToolsConfig.current().preferredItems.equals(Map.of(34, "35:4")), "first world layout did not survive switching");
        check(presets.json().equals(SortingLayoutPresets.snapshot()), "first world named presets did not survive switching");
        JsonObject before = document.getAsJsonObject("settings").deepCopy(), after = PowerConfig.document().getAsJsonObject("settings").deepCopy();
        before.remove(WorldSortingLayoutsData.SECTION); after.remove(WorldSortingLayoutsData.SECTION);
        check(before.equals(after), "world layout edit changed global preferences or legacy layouts");
      });
      check(items.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer))), "sorting preference migration changed native item data");
    } finally {
      PowerConfig.flushPending(); PowerConfig.write(document);
      for (var saved : cache.entrySet()) saved.getKey().set(null, saved.getValue());
      InventoryToolsConfig.preview(tools);
    }
    log("SORTING440 failures=" + failures); check(failures == 0, "sorting-worlds-440 failures=" + failures);
  }
}
