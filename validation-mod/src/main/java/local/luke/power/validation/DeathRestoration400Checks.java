package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import local.luke.power.death.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.offhand.*;
import net.minecraft.class_277;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.Display;

/** Real player/crafting callbacks, with scratch slots and durable claims in a disposable world. */
public final class DeathRestoration400Checks {
  private DeathRestoration400Checks() {}
  public static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && mc.player.health > 0 && !mc.player.dead,
        "load a living player in a disposable local world");
    check(Display.isActive() && mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "focus and mute the disposable client");
    check(!NativeInventoryPlan.busy(), "finish pending inventory work first");
    failures = 0;
    var player = mc.player; var inventory = player.inventory;
    var main = inventory.main; var armor = inventory.armor; var cursor = inventory.getCursorStack();
    var hand = Offhand.get(inventory); int selected = inventory.selectedSlot;
    var container = player.playerContainer; var handler = player.container; var screen = mc.currentScreen; var view = mc.field_2807;
    var rules = AccessRules.copy(mc); var deathSettings = DeathConfig.copy(); var offhandSettings = OffhandConfig.copy();
    Field failed = DeathTracker.class.getDeclaredField("recoveryFailed"); failed.setAccessible(true); boolean wasFailed = failed.getBoolean(null);
    try {
      mc.currentScreen = null; mc.field_2807 = player;
      AccessRules.set(mc, local.luke.power.rules.GameRule.ALLOW_DEATH_RECOVERY, true);
      var settings = DeathConfig.copy(); settings.enabled = true; settings.announceItems = settings.announceLocation = false; DeathConfig.preview(settings);
      var disabled = OffhandConfig.copy(); disabled.enabled = false; OffhandConfig.preview(disabled);
      test("death recovery restores disabled offhand data without enabling use", () -> {
        reset(mc, failed); seed(mc); var record = record(mc, "offhand disabled restoration");
        String expected = bytes(Offhand.get(inventory)); clear(mc);
        DeathTracker.recover(mc, record);
        check(expected.equals(bytes(Offhand.get(inventory))) && !Offhand.enabled(player), "disabled offhand recovery changed ownership or enablement");
        check(DeathTracker.journal(mc).state(record.id).equals("recovered"), "successful recovery claim not completed");
      });
      test("death callback failure restores owned bytes and reopens only the complete claim", () -> {
        reset(mc, failed); seed(mc); var record = record(mc, "owned restoration failure"); clear(mc);
        var grid = new FailingGrid(mc); player.playerContainer = player.container = grid;
        inventory.main[8] = DeathChecks.tagged(267, 1, 71, "retained");
        String before = snapshot(mc); var output = grid.method_2084(0).getStack();
        grid.failure = () -> { throw new IllegalStateException("Owned crafting failure"); }; grid.at = 2;
        refused(() -> DeathTracker.recover(mc, record));
        check(before.equals(snapshot(mc)) && grid.method_2084(0).getStack() == output, "owned rollback changed original bytes or output");
        check(DeathTracker.journal(mc).state(record.id).equals("available") && failed.getBoolean(null), "complete rollback did not preserve retry/shutdown boundary");
      });
      test("death rollback preserves foreign offhand and future input bytes and blocks the claim", () -> {
        reset(mc, failed); seed(mc); var record = record(mc, "foreign restoration failure"); clear(mc);
        var grid = new FailingGrid(mc); player.playerContainer = player.container = grid;
        ItemStack foreignHand = DeathChecks.tagged(35, 201, 6, "foreign-hand");
        ItemStack foreignInput = DeathChecks.tagged(264, 9, 0, "foreign-input");
        ItemStack foreignOutput = DeathChecks.tagged(280, 7, 0, "foreign-output");
        String handBytes = bytes(foreignHand), inputBytes = bytes(foreignInput), outputBytes = bytes(foreignOutput);
        grid.failure = () -> {
          Offhand.set(inventory, foreignHand); raw(grid)[3] = foreignInput;
          grid.field_1125.setStack(0, foreignOutput);
          throw new IllegalStateException("Foreign crafting failure");
        };
        refused(() -> DeathTracker.recover(mc, record));
        check(Offhand.get(inventory) == foreignHand && handBytes.equals(bytes(foreignHand)), "foreign offhand was overwritten");
        check(raw(grid)[3] == foreignInput && inputBytes.equals(bytes(foreignInput)), "not-yet-written foreign input was overwritten");
        check(grid.method_2084(0).getStack() == foreignOutput && outputBytes.equals(bytes(foreignOutput)), "output was restored against foreign inputs");
        check(inventory.main[0] == null && raw(grid)[0] == null, "owned recovery bytes were not rolled back");
        check(DeathTracker.journal(mc).state(record.id).equals("pending"), "incomplete recovery reopened its claim");
        failed.setBoolean(null, false);
        try { DeathTracker.recover(mc, record); throw new AssertionError("pending claim could be retried"); }
        catch (IllegalArgumentException expected) {}
      });
      test("death callback array replacement retains the new owner and pending claim", () -> {
        reset(mc, failed); seed(mc); var record = record(mc, "owner replacement failure"); clear(mc);
        var grid = new FailingGrid(mc); player.playerContainer = player.container = grid;
        ItemStack[] foreign = new ItemStack[36]; foreign[7] = DeathChecks.tagged(35, 119, 4, "foreign-array");
        String expected = bytes(foreign[7]);
        grid.failure = () -> { inventory.main = foreign; throw new IllegalStateException("Array owner changed"); };
        refused(() -> DeathTracker.recover(mc, record));
        check(inventory.main == foreign && expected.equals(bytes(foreign[7])), "rollback reattached the old inventory owner");
        check(DeathTracker.journal(mc).state(record.id).equals("pending"), "lost owner reopened its recovery claim");
      });
      test("kept snapshot callback failure restores owned bytes and retains the saved snapshot", () -> {
        reset(mc, failed); seed(mc); var saved = KeptInventory.capture(player); byte[] savedBytes = saved.bytes(); clear(mc);
        var grid = new FailingGrid(mc); player.playerContainer = player.container = grid;
        inventory.main[8] = DeathChecks.tagged(267, 1, 71, "kept-retained");
        Offhand.set(inventory, DeathChecks.tagged(35, 117, 5, "kept-original-hand"));
        String before = snapshot(mc); var output = grid.method_2084(0).getStack();
        grid.failure = () -> { throw new IllegalStateException("Kept owned callback failure"); }; grid.at = 2;
        refused(() -> saved.restore(player));
        check(before.equals(snapshot(mc)) && grid.method_2084(0).getStack() == output, "kept rollback changed original inventory or output");
        check(Arrays.equals(savedBytes, saved.bytes()), "kept failure changed its durable snapshot");
      });
      test("kept snapshot rollback preserves foreign hand/input/output ownership", () -> {
        reset(mc, failed); seed(mc); var saved = KeptInventory.capture(player); byte[] savedBytes = saved.bytes(); clear(mc);
        var grid = new FailingGrid(mc); player.playerContainer = player.container = grid;
        ItemStack foreignHand = DeathChecks.tagged(35, 117, 8, "kept-foreign-hand");
        ItemStack foreignInput = DeathChecks.tagged(264, 3, 0, "kept-foreign-input");
        ItemStack foreignOutput = DeathChecks.tagged(280, 3, 0, "kept-foreign-output");
        String handBytes = bytes(foreignHand), inputBytes = bytes(foreignInput), outputBytes = bytes(foreignOutput);
        grid.failure = () -> {
          Offhand.set(inventory, foreignHand); raw(grid)[3] = foreignInput; grid.field_1125.setStack(0, foreignOutput);
          throw new IllegalStateException("Kept foreign callback failure");
        };
        refused(() -> saved.restore(player));
        check(Offhand.get(inventory) == foreignHand && handBytes.equals(bytes(foreignHand)), "kept rollback replaced foreign offhand");
        check(raw(grid)[3] == foreignInput && inputBytes.equals(bytes(foreignInput)), "kept rollback replaced foreign input");
        check(grid.method_2084(0).getStack() == foreignOutput && outputBytes.equals(bytes(foreignOutput)), "kept rollback restored stale output");
        check(inventory.main[0] == null && raw(grid)[0] == null && Arrays.equals(savedBytes, saved.bytes()), "kept owned rollback or saved evidence failed");
      });
      test("kept transfer clears the retired player's inputs without another recipe callback", () -> {
        reset(mc, failed);
        var retired = new FailingGrid(mc); player.playerContainer = player.container = retired; seed(mc);
        var saved = KeptInventory.capture(player);
        retired.failure = () -> { throw new IllegalStateException("Retired input callback must not run"); };
        var replacement = (net.minecraft.entity.player.ClientPlayerEntity)mc.interactionManager.method_1717(mc.world);
        replacement.name = player.name;
        try {
          mc.player = replacement; mc.field_2807 = replacement;
          KeepInventory.transfer(mc, player, saved);
          check(saved.matches(replacement), "kept transfer changed the destination bytes");
          check(retired.writes == 0 && Arrays.stream(raw(retired)).allMatch(Objects::isNull)
              && Arrays.stream(inventory.main).allMatch(Objects::isNull) && Offhand.get(inventory) == null,
              "retired owner retained items or ran recipe callbacks");
        } finally { mc.player = player; mc.field_2807 = player; }
      });
      test("kept transfer detects callback changes to the retired player's offhand", () -> {
        reset(mc, failed); seed(mc); var saved = KeptInventory.capture(player);
        ItemStack foreign = DeathChecks.tagged(35, 117, 12, "retired-foreign-hand"); String expected = bytes(foreign);
        var replacement = (net.minecraft.entity.player.ClientPlayerEntity)mc.interactionManager.method_1717(mc.world); replacement.name = player.name;
        try {
          mc.player = replacement; mc.field_2807 = replacement;
          var incoming = new FailingGrid(mc); replacement.playerContainer = replacement.container = incoming;
          incoming.failure = () -> Offhand.set(inventory, foreign);
          try { KeepInventory.transfer(mc, player, saved); throw new AssertionError("foreign source change accepted"); }
          catch (IllegalStateException stopped) {}
          check(Offhand.get(inventory) == foreign && expected.equals(bytes(foreign)), "retired foreign hand was erased");
          check(replacement.inventory.main[0] == null && Offhand.get(replacement.inventory) == null
              && raw(incoming)[0] == null, "failed transfer retained destination copies");
          check(((KeptPlayer)replacement).power$keptInventory() == saved, "failed transfer lost its recovery snapshot");
        } finally { mc.player = player; mc.field_2807 = player; }
      });
    } finally {
      inventory.main = main; inventory.armor = armor; inventory.selectedSlot = selected;
      inventory.setCursorStack(cursor); Offhand.set(inventory, hand);
      player.playerContainer = container; player.container = handler; mc.currentScreen = screen; mc.field_2807 = view;
      failed.setBoolean(null, wasFailed); DeathConfig.preview(deathSettings); OffhandConfig.preview(offhandSettings); AccessRules.restore(mc, rules);
      local.luke.power.inventory.InventoryActions.cancel();
    }
    log("DEATH RESTORATION400 failures=" + failures);
  }
  private static void reset(Minecraft mc, Field failed) throws Exception {
    failed.setBoolean(null, false); mc.player.inventory.main = new ItemStack[36]; mc.player.inventory.armor = new ItemStack[4];
    mc.player.inventory.setCursorStack(null); mc.player.inventory.selectedSlot = 0; Offhand.set(mc.player.inventory, null);
    mc.player.playerContainer = mc.player.container = new class_277(mc.player.inventory);
  }
  private static void seed(Minecraft mc) throws Exception {
    mc.player.inventory.main[0] = DeathChecks.tagged(267, 1, 73, "recovered-main");
    Offhand.set(mc.player.inventory, DeathChecks.tagged(35, 17, 5, "recovered-hand"));
    mc.player.playerContainer.method_2084(1).setStack(DeathChecks.tagged(264, 3, 0, "recovered-input"));
  }
  private static void clear(Minecraft mc) {
    Arrays.fill(mc.player.inventory.main, null); Arrays.fill(mc.player.inventory.armor, null);
    mc.player.inventory.setCursorStack(null); Offhand.set(mc.player.inventory, null);
    for (int i = 1; i <= 4; i++) mc.player.playerContainer.method_2084(i).setStack(null);
  }
  private static DeathRecord record(Minecraft mc, String cause) throws Exception {
    var record = new DeathRecord(); var journal = DeathTracker.journal(mc); record.id = journal.nextId();
    record.world = local.luke.power.commands.CommandContext.world(mc); record.player = mc.player.name; record.cause = cause;
    record.date = System.currentTimeMillis(); DeathItems.capture(mc.player, record); journal.death(record); return record;
  }
  private static String bytes(ItemStack item) throws IOException { return item == null ? null : DeathItems.encode(item); }
  private static String snapshot(Minecraft mc) throws IOException { return DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)); }
  private static ItemStack[] raw(class_277 grid) { return ((local.luke.power.mixin.DeathCraftingAccessor)grid.field_1124).power$deathStacks(); }
  private static void refused(Check operation) throws Exception {
    try { operation.run(); throw new AssertionError("native callback failure was not propagated"); }
    catch (IOException expected) {}
  }
  private static final class FailingGrid extends class_277 {
    Runnable failure;
    int writes, at = 1;
    FailingGrid(Minecraft mc) { super(mc.player.inventory); }
    @Override public void method_2088(Inventory inventory) {
      super.method_2088(inventory);
      if (failure != null && ++writes == at) failure.run();
    }
  }
}
