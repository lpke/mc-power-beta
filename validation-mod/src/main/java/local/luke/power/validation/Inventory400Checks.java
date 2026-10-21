package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.*;
import local.luke.power.inventory.tools.*;
import local.luke.power.offhand.*;
import local.luke.power.storage.JournalQueue;
import local.luke.power.world.WorldCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Native full-data, slow-write and callback-failure tests. Never install outside a disposable client. */
public final class Inventory400Checks {
  private Inventory400Checks() {}
  public static void run(Minecraft mc, String phase) throws Exception {
    failures = 0;
    if (phase.startsWith("persist-")) { persistence(mc, phase); return; }
    check(phase.equals("native"), "expected native");
    check(mc.world != null && mc.player != null && Display.isActive(), "focus a disposable loaded world");
    check(mc.player.health > 0 && !mc.player.dead, "use a living player for native inventory checks");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute this disposable clone before native use checks");
    log("INVENTORY400 ready=" + WorldCompatibility.ready(mc.world) + " view=" + (mc.field_2807 == mc.player)
        + " focus=" + mc.field_2778 + " paused=" + mc.paused + " grabbed=" + Mouse.isGrabbed());
    check(WorldCompatibility.ready(mc.world), "real world load did not establish the durable format fence");
    check(!NativeInventoryPlan.busy(), "another native transaction is still pending");
    var player = mc.player; var inventory = player.inventory;
    ItemStack[] main = inventory.main, armor = inventory.armor;
    ItemStack cursor = inventory.getCursorStack(), off = Offhand.get(inventory);
    int selected = inventory.selectedSlot, health = player.health;
    var screen = mc.currentScreen; var handler = player.container; var view = mc.field_2807;
    boolean focus = mc.field_2778, paused = mc.paused, grabbed = Mouse.isGrabbed();
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(player).toString();
    var offhandSettings = OffhandConfig.copy(); var toolSettings = InventoryToolsConfig.copy();
    try {
      mc.setScreen(null); player.container = player.playerContainer; mc.field_2807 = player;
      mc.field_2778 = true; mc.paused = false; Mouse.setGrabbed(true); CreativeVehicleChecks.mode(mc, "SURVIVAL");
      var enabled = OffhandConfig.copy(); enabled.enabled = true; OffhandConfig.preview(enabled);
      inventory.main = new ItemStack[36]; inventory.armor = new ItemStack[4]; inventory.selectedSlot = 0;
      inventory.setCursorStack(null); Offhand.set(inventory, null);
      test("offhand preserves native inventory dimensions and player-container slot identity", () -> {
        check(inventory.main.length == 36 && inventory.armor.length == 4 && inventory.size() == 40, "native arrays or size changed");
        check(player.playerContainer.slots.size() == 46, "offhand was not appended after native slots");
        var access = (local.luke.power.mixin.DeathSlotAccessor)(Object)player.playerContainer.method_2084(45);
        check(access.power$deathInventory() instanceof OffhandSlotInventory hand && hand.owner() == inventory, "wrong offhand backing owner");
      });
      test("native hand callback getter and setter route only inside their scope", () -> {
        inventory.main[0] = DeathChecks.tagged(4, 8, 0, "main");
        ItemStack held = DeathChecks.tagged(35, 13, 5, "off"); Offhand.set(inventory, held);
        check(inventory.getSelectedItem() == inventory.main[0], "ordinary getter changed");
        try (HandUse ignored = HandUse.begin(inventory, HandUse.Hand.OFF, true)) {
          check(inventory.getSelectedItem() == held, "offhand getter did not route");
          ItemStack replacement = DeathChecks.tagged(35, 12, 5, "off"); inventory.setStack(0, replacement);
          check(Offhand.get(inventory) == replacement && inventory.main[0].count == 8, "offhand native setter changed main");
        }
        check(inventory.getSelectedItem() == inventory.main[0] && HandUse.hand(inventory) == HandUse.Hand.MAIN, "hand scope leaked");
      });
      test("offhand native persistence retains oversized counts, damage and unknown data", () -> {
        var previous = new NbtCompound();
        previous.putString("future-note", "preserve"); var document = new NbtCompound();
        for (int count : new int[]{1, 64, 117, 200, 1000}) {
          ItemStack held = DeathChecks.tagged(35, count, 5, "persistent"); OffhandData.write(document, held, previous);
          check(bytes(held).equals(bytes(OffhandData.read(document))), "offhand round trip changed native bytes at count " + count);
        }
        check(document.getCompound(OffhandData.TAG).getString("future-note").equals("preserve"), "unknown field discarded");
        document.getCompound(OffhandData.TAG).putInt("format", 999);
        try { OffhandData.read(document); throw new AssertionError("future offhand format accepted"); }
        catch (IOException expected) { check(document.getCompound(OffhandData.TAG).getInt("format") == 999, "future document overwritten"); }
      });
      test("native player saves and kept-death snapshots include full offhand ownership", () -> {
        reset(inventory); inventory.main[0] = DeathChecks.tagged(267, 1, 71, "kept-main");
        Offhand.set(inventory, DeathChecks.tagged(35, 200, 5, "kept-offhand"));
        String held = bytes(Offhand.get(inventory)); var nativeTag = new NbtCompound(); player.writeNbt(nativeTag);
        check(held.equals(bytes(OffhandData.read(nativeTag))), "native player write hook omitted or changed the offhand");
        var saved = local.luke.power.death.KeptInventory.read(local.luke.power.death.KeptInventory.capture(player).bytes());
        reset(inventory); saved.restore(player);
        check(saved.matches(player) && held.equals(bytes(Offhand.get(inventory))), "kept snapshot restoration changed offhand data");
      });
      test("death records reserve offhand slot 45 and reject occupied original ownership", () -> {
        reset(inventory); Offhand.set(inventory, DeathChecks.tagged(35, 17, 5, "death-hand"));
        var record = new local.luke.power.death.DeathRecord(); DeathItems.capture(player, record);
        int entry = record.slots.indexOf(45); check(record.slotLayout == 2 && entry >= 0, "death snapshot omitted offhand");
        try { DeathItems.plan(inventory, player.playerContainer, record, List.of(entry)); throw new AssertionError("occupied offhand recovery admitted"); }
        catch (IllegalArgumentException expected) {}
        String held = bytes(Offhand.get(inventory)); Offhand.set(inventory, null);
        var planned = DeathItems.plan(inventory, player.playerContainer, record, List.of(entry));
        check(held.equals(bytes(planned[45])) && Offhand.get(inventory) == null, "offhand recovery planning wrote inventory or changed data");
      });
      test("hand swap publishes only after durable completion and preserves full native data", () -> {
        reset(inventory); inventory.main[0] = DeathChecks.tagged(267, 1, 73, "main-tool");
        Offhand.set(inventory, DeathChecks.tagged(35, 41, 5, "off-wool"));
        String before = bytes(inventory.main[0]), other = bytes(Offhand.get(inventory));
        check(Offhand.swap(mc), "swap declined");
        check(before.equals(bytes(inventory.main[0])) && other.equals(bytes(Offhand.get(inventory))), "swap changed slots before flush");
        finish(mc); check(other.equals(bytes(inventory.main[0])) && before.equals(bytes(Offhand.get(inventory))), "swap changed identity/count");
      });
      test("two rapid hand swaps cancel an unpublished inverse without touching inventory", () -> {
        String before = bytes(inventory.main[0]), other = bytes(Offhand.get(inventory));
        check(Offhand.swap(mc) && Offhand.swap(mc), "rapid swap was not accepted"); finish(mc);
        check(before.equals(bytes(inventory.main[0])) && other.equals(bytes(Offhand.get(inventory))), "inverse cancellation wrote native slots");
      });
      test("a rejected plan does not disable subsequent valid inventory actions", () -> {
        reset(inventory); inventory.main[0] = new ItemStack(4, 1, 0);
        check(!NativeInventoryPlan.submit(mc, "invalid-count", true,
            List.of(NativeInventoryPlan.Change.main(inventory, 0, new ItemStack(4, 2, 0))), () -> true, () -> {}), "nonconserving plan admitted");
        check(Offhand.swap(mc), "preparation decline poisoned later actions"); finish(mc);
        check(inventory.main[0] == null && Offhand.get(inventory).count == 1, "valid next swap did not publish");
      });
      test("foreign changes after preparation cancel the entire plan without overwriting them", () -> {
        reset(inventory); inventory.main[0] = DeathChecks.tagged(4, 8, 0, "source");
        String original = bytes(inventory.main[0]); check(Offhand.swap(mc), "swap declined");
        inventory.main[9] = DeathChecks.tagged(35, 3, 5, "foreign"); finish(mc);
        check(original.equals(bytes(inventory.main[0])) && Offhand.get(inventory) == null
            && inventory.main[9].count == 3, "foreign change was overwritten or partial publication occurred");
      });
      test("a real journal lock failure preserves native slots and existing recovery bytes", () -> {
        reset(inventory); inventory.main[0] = DeathChecks.tagged(267, 1, 73, "locked-journal");
        String before = bytes(inventory.main[0]);
        Path journal = (Path)privateField("journalPath").get(null);
        check(journal != null && Files.isRegularFile(journal), "a prior durable action must create the journal first");
        byte[] durable = Files.readAllBytes(journal);
        try {
          try (FileChannel channel = FileChannel.open(journal, StandardOpenOption.WRITE); var lock = channel.lock()) {
            check(Offhand.swap(mc), "locked-journal plan was not queued"); finish(mc);
            check(before.equals(bytes(inventory.main[0])) && Offhand.get(inventory) == null, "failed flush published inventory");
            check(Arrays.equals(durable, Files.readAllBytes(journal)), "failed append changed prior recovery evidence");
            check(!Offhand.swap(mc), "durable failure did not stop subsequent actions");
            clearExpectedFailure();
          }
        } finally { privateField("failed").setBoolean(null, false); }
      });
      test("native food consumption refills the main hand after a slow durable write", () -> {
        reset(inventory); var settings = InventoryToolsConfig.copy(); settings.restock = true; InventoryToolsConfig.preview(settings);
        inventory.main[0] = DeathChecks.tagged(260, 1, 0, "food"); inventory.main[9] = DeathChecks.tagged(260, 7, 0, "food");
        player.health = 10;
        check(mc.interactionManager.method_1712(player, mc.world, inventory.main[0]), "native food use failed");
        check(inventory.main[0] == null && player.health > 10, "native use was not applied");
        try (SlowWriter slow = new SlowWriter()) {
          Restock.tick(mc); check(NativeInventoryPlan.busy(), "native use did not request restock");
          Thread.sleep(320); NativeInventoryPlan.tick(mc);
          slow.checkBlocked();
          check(inventory.main[0] == null && inventory.main[9].count == 7, "unflushed refill published");
          check(Restock.waitingForUse(mc), "same held-use press cannot wait for its own slow refill");
          slow.release(); finish(mc);
        }
        check(inventory.main[0].count == 7 && inventory.main[9] == null, "food refill failed");
      });
      test("manual hand change cancels a queued automatic refill", () -> {
        reset(inventory); inventory.main[0] = DeathChecks.tagged(4, 2, 0, "place"); inventory.main[9] = DeathChecks.tagged(4, 7, 0, "place");
        var action = Restock.before(mc, inventory.main[0]); inventory.main[0].count--; Restock.after(mc, action);
        try (SlowWriter slow = new SlowWriter()) {
          Restock.tick(mc); check(NativeInventoryPlan.busy(), "refill missing"); HandUse.swapping();
          NativeInventoryPlan.tick(mc); slow.checkBlocked();
          check(inventory.main[0].count == 1 && inventory.main[9].count == 7, "canceled unflushed refill touched slots");
          slow.release(); finish(mc);
        }
        check(inventory.main[0].count == 1 && inventory.main[9].count == 7, "manual gesture did not cancel refill");
      });
      test("creative and menu states never run automatic survival refills", () -> {
        CreativeVehicleChecks.mode(mc, "CREATIVE"); check(Restock.before(mc, inventory.main[0]) == null, "Creative requested restock");
        CreativeVehicleChecks.mode(mc, "SURVIVAL"); mc.currentScreen = new net.minecraft.class_525();
        try { check(Restock.before(mc, inventory.main[0]) == null, "menu requested restock"); } finally { mc.currentScreen = null; }
      });
      test("callback failure restores owned native changes before derived output rollback", () -> {
        reset(inventory); inventory.main[0] = DeathChecks.tagged(4, 8, 0, "rollback");
        String before = bytes(inventory.main[0]); AtomicBoolean restored = new AtomicBoolean();
        check(NativeInventoryPlan.submit(mc, "owned-failure", true, List.of(
            NativeInventoryPlan.Change.main(inventory, 0, null), NativeInventoryPlan.Change.offhand(inventory, inventory.main[0])),
            () -> true, () -> { throw new IllegalStateException("fixture callback failure"); }, () -> restored.set(true)), "failure plan declined");
        try {
          finish(mc); check(restored.get() && before.equals(bytes(inventory.main[0])) && Offhand.get(inventory) == null, "owned rollback failed");
          clearExpectedFailure();
        } finally { privateField("failed").setBoolean(null, false); }
      });
      test("foreign callback changes never restore derived output after ownership is lost", () -> {
        reset(inventory); inventory.main[0] = DeathChecks.tagged(4, 8, 0, "source"); AtomicBoolean restored = new AtomicBoolean();
        ItemStack foreign = DeathChecks.tagged(35, 3, 5, "foreign"); String foreignBytes = bytes(foreign);
        check(NativeInventoryPlan.submit(mc, "foreign-failure", true, List.of(
            NativeInventoryPlan.Change.main(inventory, 0, null), NativeInventoryPlan.Change.offhand(inventory, inventory.main[0])),
            () -> true, () -> { inventory.main[0] = foreign; }, () -> restored.set(true)), "failure plan declined");
        try {
          finish(mc); check(!restored.get() && inventory.main[0] == foreign && foreignBytes.equals(bytes(inventory.main[0]))
              && Offhand.get(inventory) == null, "rollback overwrote a foreign change or restored derived output");
          clearExpectedFailure();
        } finally { privateField("failed").setBoolean(null, false); }
      });
    } finally {
      try { NativeInventoryPlan.cancel(); JournalQueue.awaitQueued(); NativeInventoryPlan.tick(mc); }
      finally {
        InventoryAutomation.clear();
        inventory.main = main; inventory.armor = armor; inventory.selectedSlot = selected;
        inventory.setCursorStack(cursor); Offhand.set(inventory, off); player.health = health;
        OffhandConfig.preview(offhandSettings); InventoryToolsConfig.preview(toolSettings);
        try { CreativeVehicleChecks.mode(mc, mode); }
        finally {
          mc.currentScreen = screen; player.container = handler;
          mc.field_2807 = view; mc.field_2778 = focus; mc.paused = paused; Mouse.setGrabbed(grabbed);
        }
      }
    }
    log("INVENTORY400 NATIVE failures=" + failures);
  }
  private static void persistence(Minecraft mc, String phase) throws Exception {
    check(mc.world != null && mc.player != null && WorldCompatibility.ready(mc.world), "load a verified disposable world first");
    Path report = Path.of("power-beta-data/reports/inventory-400-persistence.json");
    if (phase.equals("persist-start")) {
      mc.setScreen(null); var enabled = OffhandConfig.copy(); enabled.enabled = true; OffhandConfig.preview(enabled);
      ItemStack main = DeathChecks.tagged(267, 1, 73, "restart-main");
      ItemStack hand = DeathChecks.tagged(35, 200, 5, "restart-offhand");
      mc.player.inventory.main[0] = main; mc.player.inventory.selectedSlot = 0; Offhand.set(mc.player.inventory, hand);
      var expected = new com.google.gson.JsonObject(); expected.addProperty("main", bytes(main)); expected.addProperty("offhand", bytes(hand));
      expected.addProperty("world", ((WorldIdentity)mc.world.method_262()).power$commandWorldId());
      Files.createDirectories(report.getParent()); Files.writeString(report, expected.toString());
      local.luke.power.world.WorldRuleChanges.save(mc);
      log("INVENTORY400 PERSIST START saved full native offhand count200; quit and restart before persist-finish");
    } else if (phase.equals("persist-finish")) {
      var expected = com.google.gson.JsonParser.parseString(Files.readString(report)).getAsJsonObject();
      check(expected.get("world").getAsString().equals(((WorldIdentity)mc.world.method_262()).power$commandWorldId()), "loaded a different world");
      check(expected.get("main").getAsString().equals(bytes(mc.player.inventory.main[0]))
          && expected.get("offhand").getAsString().equals(bytes(Offhand.get(mc.player.inventory))), "restart changed native hand data");
      log("INVENTORY400 PERSIST FINISH PASS main damage/NBT and offhand count200/NBT survived a process restart");
    } else throw new IllegalArgumentException("persist-start or persist-finish");
  }
  private static void reset(net.minecraft.entity.player.PlayerInventory inventory) {
    InventoryAutomation.clear(); Arrays.fill(inventory.main, null); Arrays.fill(inventory.armor, null);
    Offhand.set(inventory, null); inventory.setCursorStack(null); inventory.selectedSlot = 0;
  }
  private static String bytes(ItemStack item) throws IOException { return item == null ? null : DeathItems.encode(item); }
  private static void finish(Minecraft mc) throws IOException {
    JournalQueue.awaitQueued(); NativeInventoryPlan.tick(mc); check(!NativeInventoryPlan.busy(), "plan did not finish");
  }
  private static void clearExpectedFailure() throws Exception {
    var field = privateField("failed");
    check(field.getBoolean(null), "injected publication failure did not stop the action family"); field.setBoolean(null, false);
  }
  private static java.lang.reflect.Field privateField(String name) throws Exception {
    var field = NativeInventoryPlan.class.getDeclaredField(name); field.setAccessible(true); return field;
  }
  private static final class SlowWriter implements AutoCloseable {
    private final CountDownLatch release = new CountDownLatch(1);
    private final CountDownLatch exited = new CountDownLatch(1);
    SlowWriter() throws Exception {
      var field = JournalQueue.class.getDeclaredField("IO"); field.setAccessible(true);
      CountDownLatch entered = new CountDownLatch(1);
      ((ThreadPoolExecutor)field.get(null)).execute(() -> {
        entered.countDown();
        try { release.await(); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        finally { exited.countDown(); }
      });
      if (!entered.await(2, TimeUnit.SECONDS)) { release(); throw new AssertionError("slow writer was not installed"); }
    }
    void checkBlocked() { check(release.getCount() == 1 && exited.getCount() == 1, "slow writer stopped blocking before explicit release"); }
    void release() { release.countDown(); }
    public void close() { release(); }
  }
}
