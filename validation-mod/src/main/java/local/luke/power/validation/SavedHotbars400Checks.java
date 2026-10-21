package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.inventory.tools.*;
import local.luke.power.offhand.Offhand;
import local.luke.power.rules.GameRule;
import local.luke.power.storage.PowerConfig;
import local.luke.power.world.WorldCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.Display;

/** Native saved-hotbar publication and user-data isolation, only in a disposable client. */
final class SavedHotbars400Checks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    check(mc.world != null && mc.player != null && Display.isActive()
        && WorldCompatibility.ready(mc.world), "focus a verified disposable world first");
    check(!NativeInventoryPlan.busy(), "finish other inventory plans first");
    var field = SavedCreativeHotbars.class.getDeclaredField("data"); field.setAccessible(true);
    var originalData = field.get(null);
    check(originalData != null && PowerConfig.problem().isEmpty(), "repair unsupported saved hotbars/settings before this fixture");
    var originalDocument = PowerConfig.section("savedCreativeHotbars");
    var originalRules = AccessRules.copy(mc);
    try (var state = new AutoTools400State(mc)) {
      AccessRules.set(mc, GameRule.ALLOW_CREATIVE_MODE, true);
      field.set(null, SavedCreativeHotbarsData.empty());
      test("all nine native saved rows retain counts, damage, NBT and empty slots", () -> {
        for (int row = 0; row < 9; row++) {
          prepare(mc, state);
          mc.player.inventory.main[0] = DeathChecks.tagged(35, 117 + row, row % 16, "saved-row-" + row);
          mc.player.inventory.main[8] = DeathChecks.tagged(267, 1, 71 + row, "saved-tool-" + row);
          String first = DeathItems.encode(mc.player.inventory.main[0]), last = DeathItems.encode(mc.player.inventory.main[8]);
          check(SavedCreativeHotbars.save(mc, row), "native save declined row " + row);
          var snapshot = SavedCreativeHotbars.snapshot(row);
          check(first.equals(snapshot[0]) && last.equals(snapshot[8]) && snapshot[1] == null, "saved row changed native data");
          snapshot[0] = null;
          check(first.equals(SavedCreativeHotbars.snapshot(row)[0]), "caller mutated cached preset");
        }
        PowerConfig.flushPending();
        var read = SavedCreativeHotbarsData.read(PowerConfig.section("savedCreativeHotbars"));
        for (int row = 0; row < 9; row++)
          check(Arrays.equals(read.bar(row), SavedCreativeHotbars.snapshot(row)), "durable saved row differs " + row);
      });
      test("native saved load waits for durable publication and preserves storage, armor and offhand", () -> {
        prepare(mc, state); var inventory = mc.player.inventory;
        field.set(null, ((SavedCreativeHotbarsData)field.get(null)).with(8, preset("loaded-row")));
        inventory.main[0] = DeathChecks.tagged(4, 13, 0, "old-hotbar");
        inventory.main[9] = DeathChecks.tagged(35, 200, 3, "protected-storage");
        inventory.armor[0] = DeathChecks.tagged(313, 1, 17, "protected-armor");
        Offhand.set(inventory, DeathChecks.tagged(35, 123, 7, "protected-hand"));
        String main = DeathItems.encode(inventory.main[0]), storage = DeathItems.encode(inventory.main[9]);
        String armor = DeathItems.encode(inventory.armor[0]), off = DeathItems.encode(Offhand.get(inventory));
        try (var slow = new AutoTools400State.SlowWriter()) {
          check(SavedCreativeHotbars.load(mc, 8), "native load declined");
          Thread.sleep(180); NativeInventoryPlan.tick(mc); slow.checkBlocked();
          check(main.equals(DeathItems.encode(inventory.main[0])), "unflushed load published");
          slow.release(); state.finish();
        }
        for (int i = 0; i < 9; i++)
          check(Objects.equals(SavedCreativeHotbars.snapshot(8)[i], AutoTools400State.bytes(inventory.main[i])), "published row mismatch " + i);
        check(storage.equals(DeathItems.encode(inventory.main[9])) && armor.equals(DeathItems.encode(inventory.armor[0]))
            && off.equals(DeathItems.encode(Offhand.get(inventory))), "load changed protected native data");
      });
      test("a foreign storage edit cancels a prepared saved load without erasing either owner", () -> {
        prepare(mc, state); var inventory = mc.player.inventory;
        field.set(null, ((SavedCreativeHotbarsData)field.get(null)).with(0, preset("foreign-cancel")));
        inventory.main[0] = DeathChecks.tagged(4, 13, 0, "original");
        String before = DeathItems.encode(inventory.main[0]);
        check(SavedCreativeHotbars.load(mc, 0), "load declined");
        inventory.main[12] = DeathChecks.tagged(35, 99, 5, "foreign");
        String foreign = DeathItems.encode(inventory.main[12]); state.finish();
        check(before.equals(DeathItems.encode(inventory.main[0])) && foreign.equals(DeathItems.encode(inventory.main[12])), "foreign edit lost or load partially published");
      });
      test("lost Creative permission cancels prepared loads and keeps saved user data", () -> {
        prepare(mc, state); var inventory = mc.player.inventory;
        field.set(null, ((SavedCreativeHotbarsData)field.get(null)).with(0, preset("permission-cancel")));
        inventory.main[0] = DeathChecks.tagged(4, 13, 0, "permission-before");
        String before = DeathItems.encode(inventory.main[0]);
        String[] preset = SavedCreativeHotbars.snapshot(0);
        check(SavedCreativeHotbars.load(mc, 0), "load declined");
        AccessRules.set(mc, GameRule.ALLOW_CREATIVE_MODE, false);
        try {
          state.finish();
          check(before.equals(DeathItems.encode(inventory.main[0])) && Arrays.equals(preset, SavedCreativeHotbars.snapshot(0)), "revocation changed slots or preset");
        } finally { AccessRules.set(mc, GameRule.ALLOW_CREATIVE_MODE, true); }
      });
      test("disabled saved hotbars and occupied cursor never mutate inventory or presets", () -> {
        prepare(mc, state); String[] preset = SavedCreativeHotbars.snapshot(0);
        mc.player.inventory.setCursorStack(DeathChecks.tagged(35, 2, 0, "cursor"));
        check(!SavedCreativeHotbars.save(mc, 0) && !SavedCreativeHotbars.load(mc, 0), "occupied cursor admitted");
        mc.player.inventory.setCursorStack(null);
        var settings = InventoryToolsConfig.copy(); settings.creativeHotbars = false; InventoryToolsConfig.preview(settings);
        check(!SavedCreativeHotbars.save(mc, 0) && !SavedCreativeHotbars.load(mc, 0), "disabled feature admitted");
        check(Arrays.equals(preset, SavedCreativeHotbars.snapshot(0)), "rejected action changed user data");
      });
      test("Survival preset editing stays separate from inventory and Creative loading", () -> {
        prepare(mc, state); CreativeVehicleChecks.mode(mc, "SURVIVAL");
        var settings = InventoryToolsConfig.copy(); settings.creativeHotbars = false; InventoryToolsConfig.preview(settings);
        var before = SavedCreativeHotbars.editorSnapshot();
        PowerConfig.save("savedCreativeHotbars", before.json());
        String inventory = state.snapshot();
        var next = before.with(3, preset("survival-preset-editor"));
        check(SavedCreativeHotbars.editable(mc) && !SavedCreativeHotbars.eligible(mc), "editor inherited the gameplay mode/toggle gate");
        SavedCreativeHotbars.acceptEdit(mc, before, next);
        check(next.json().equals(SavedCreativeHotbarsData.read(PowerConfig.section("savedCreativeHotbars")).json()), "edited presets were not durably retained");
        check(inventory.equals(state.snapshot()) && !SavedCreativeHotbars.load(mc, 3), "preset editing changed inventory or admitted Survival loading");
        boolean staleRejected = false;
        try { SavedCreativeHotbars.acceptEdit(mc, before, before); } catch (java.io.IOException expected) { staleRejected = true; }
        check(staleRejected && next.json().equals(SavedCreativeHotbars.editorSnapshot().json()), "stale editor overwrote saved presets");
        AccessRules.set(mc, GameRule.ALLOW_CREATIVE_MODE, false);
        try {
          check(!SavedCreativeHotbars.editable(mc), "editor retained revoked Creative access");
          boolean denied = false;
          try { SavedCreativeHotbars.acceptEdit(mc, next, next.with(3, new String[9])); } catch (java.io.IOException expected) { denied = true; }
          check(denied && next.json().equals(SavedCreativeHotbars.editorSnapshot().json()) && inventory.equals(state.snapshot()), "revocation changed presets or inventory");
        } finally { AccessRules.set(mc, GameRule.ALLOW_CREATIVE_MODE, true); }
      });
    } finally {
      NativeInventoryPlan.cancel(); field.set(null, originalData);
      try { PowerConfig.save("savedCreativeHotbars", originalDocument); }
      finally { AccessRules.restore(mc, originalRules); }
    }
    log("SAVED HOTBARS400 failures=" + failures);
  }
  private static String[] preset(String label) throws Exception {
    String[] items = new String[9];
    items[0] = DeathItems.encode(DeathChecks.tagged(35, 125, 8, label));
    items[8] = DeathItems.encode(DeathChecks.tagged(267, 1, 79, label + "-tool"));
    return items;
  }
  private static void prepare(Minecraft mc, AutoTools400State state) throws Exception {
    state.reset(); CreativeVehicleChecks.mode(mc, "CREATIVE");
    var settings = InventoryToolsConfig.copy(); settings.creativeHotbars = true; InventoryToolsConfig.preview(settings);
    check(SavedCreativeHotbars.eligible(mc), "native Creative save/load prerequisites failed");
  }
}
