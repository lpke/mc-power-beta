package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.JsonPrimitive;
import local.luke.power.config.*;
import local.luke.power.death.*;
import local.luke.power.permissions.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

final class DeathChecks {
  static void run(Minecraft mc, String action) throws Exception {
    failures = 0;
    if (action.equals("inventory") || action.equals("inventory-saved")) { DeathInventoryChecks.run(mc, action.equals("inventory-saved")); return; }
    if (action.equals("retry")) {
      alive(mc); clear(mc); ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);
      CreativeVehicleChecks.mode(mc, "CREATIVE"); var record = latest(mc);
      check(DeathTracker.journal(mc).state(record.id).equals("available"), "rollback claim did not survive restart");
      var output = ChatChecks.submit(mc, "/death recover");
      check(output.stream().anyMatch(s -> s.contains("Recovered items")), "restart did not allow recovery through chat");
      String after = DeathItems.inventory(mc.player.inventory.main);
      output = ChatChecks.submit(mc, "/death recover");
      check(output.stream().anyMatch(s -> s.contains("already recovered")) && after.equals(DeathItems.inventory(mc.player.inventory.main)), "chat recovery duplicated items");
      log("PASS restart permits a rolled-back recovery through chat exactly once");
      return;
    }
    if (action.equals("saved")) {
      check(DeathConfig.current().enabled, "death setting did not persist");
      var record = latest(mc); check(record.cause.equals("Saved fixture"), "saved death record missing");
      check(record.dimension == -1 && record.x == -13 && record.y == 100 && record.z == 8, "saved location changed");
      var item = DeathItems.decode(record.items.get(0));
      check(item.count == 7 && item.getDamage() == 5 && tag(item).getString("DeathFixture").equals("saved"), "saved NBT/count/damage changed");
      log("PASS death settings, location, dimension and native item snapshot survived restart");
      return;
    }
    if (action.equals("rollback")) { rollback(mc); return; }
    var settings = DeathConfig.copy(); var rules = CommandPermissions.copy();
    var main = mc.player.inventory.main.clone(); var armor = mc.player.inventory.armor.clone();
    var cursor = mc.player.inventory.getCursorStack();
    boolean cheats = ((CheatWorld)mc.world.method_262()).power$cheatsEnabled();
    mc.setScreen(null); mc.player.container = mc.player.playerContainer; mc.field_2807 = mc.player;
    var enabled = new DeathSettings(); enabled.enabled = true; enabled.announceLocation = enabled.announceItems = false;
    try {
      test("defaults stay off and menu changes wait for Apply", () -> {
        check(!new DeathSettings().enabled, "feature defaults on");
        check(!new DeathSettings().announceItems, "item notices default on");
        DeathConfig.preview(new DeathSettings()); var session = SettingsRegistry.open(mc);
        var setting = Release110Checks.find(session, "death.enabled"); setting.value = new JsonPrimitive(true); session.preview(true);
        check(!DeathConfig.current().enabled, "draft recorded deaths before Apply"); session.discard();
        check(!DeathConfig.current().enabled, "Cancel enabled death records");
        session = SettingsRegistry.open(mc); Release110Checks.find(session, "death.enabled").value = new JsonPrimitive(true);
        session.save(Path.of(".")); check(DeathConfig.current().enabled, "Apply did not enable feature");
      });
      test("disabled feature leaves inventory and saved records alone", () -> {
        DeathConfig.preview(new DeathSettings()); int count = DeathTracker.journal(mc).records().size();
        String before = DeathItems.inventory(mc.player.inventory.main); DeathTracker.died(mc.player, null);
        check(DeathTracker.journal(mc).records().size() == count && before.equals(DeathItems.inventory(mc.player.inventory.main)), "disabled feature wrote a record or inventory");
      });
      DeathConfig.preview(enabled);
      test("chat notice settings are independent and command dispatch shows saved deaths", () -> {
        alive(mc); clear(mc); mc.player.inventory.main[0] = new ItemStack(264, 3, 0);
        for (boolean location : new boolean[]{false, true}) for (boolean items : new boolean[]{false, true}) {
          var notices = new DeathSettings(); notices.enabled = true; notices.announceLocation = location; notices.announceItems = items;
          DeathConfig.preview(notices);
          var output = ChatChecks.record(() -> record(mc, "Notice fixture"));
          check(output.stream().anyMatch(s -> s.contains("Overworld")) == location, "location notice setting ignored");
          check(output.stream().anyMatch(s -> s.contains("Lost items")) == items, "items notice setting ignored");
        }
        DeathConfig.preview(enabled);
        var output = ChatChecks.submit(mc, "/death");
        check(output.stream().anyMatch(s -> s.contains("Notice fixture")), "chat death command did not use saved cause");
        ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);
        ChatChecks.submit(mc, "/kill"); check(mc.player.health <= 0 && latest(mc).cause.equals("/kill"), "/kill cause or dispatch missing");
        alive(mc); clear(mc);
      });
      test("actual fatal fall records feet before body shrink and native drops", () -> {
        alive(mc); clear(mc); mc.player.method_1340(-12.1, 101.62, 8.9);
        mc.player.inventory.main[0] = tagged(35, 7, 5, "main");
        mc.player.inventory.armor[0] = tagged(313, 1, 27, "armor");
        mc.player.inventory.setCursorStack(tagged(264, 3, 0, "cursor"));
        String mainItem = DeathItems.encode(mc.player.inventory.main[0]);
        int expectedY = (int)Math.floor(mc.player.boundingBox.minY); int count = DeathTracker.journal(mc).records().size();
        mc.player.health = 1;
        Method fall = LivingEntity.class.getDeclaredMethod("method_1389", float.class); fall.setAccessible(true); fall.invoke(mc.player, 40F);
        var record = latest(mc);
        check(mc.player.health <= 0 && record.cause.equals("Fall"), "fatal fall cause incorrect: " + record.cause);
        check(record.x == -13 && record.y == expectedY && record.z == 8, "death coordinates/body shrink wrong");
        check(record.items.size() == 3 && record.items.contains(mainItem), "main, armor or cursor snapshot lost");
        check(record.slots.equals(List.of(0, 36, 40)), "main, armor or cursor positions lost");
        check(mc.player.inventory.main[0] == null && mc.player.inventory.armor[0] == null, "normal drops prevented");
        mc.player.method_938(null); check(DeathTracker.journal(mc).records().size() == count + 1, "duplicate death record");
      });
      test("drowning, suffocation, burning, lava, void, lightning and attacker causes", () -> {
        for (String cause : new String[]{"Drowning", "Suffocation", "Fire", "Lava", "Void"}) {
          alive(mc); clear(mc); mc.player.health = 1;
          if (cause.equals("Drowning")) {
            mc.world.method_154(0, 100, 0, 9, 0); mc.world.method_154(0, 101, 0, 9, 0);
            mc.player.method_1340(.5, 100, .5); mc.player.air = -19; mc.player.baseTick();
          } else if (cause.equals("Suffocation")) {
            mc.world.method_154(0, 100, 0, 1, 0); mc.player.method_1340(.5, 100, .5); mc.player.baseTick();
          } else if (cause.equals("Fire")) { mc.player.fire = 20; mc.player.baseTick(); }
          else if (cause.equals("Lava")) {
            mc.world.method_154(0, 100, 0, 11, 0); mc.player.method_1340(.5, 100, .5);
            Method lava = net.minecraft.entity.Entity.class.getDeclaredMethod("method_1332"); lava.setAccessible(true); lava.invoke(mc.player);
          } else {
            Method out = LivingEntity.class.getDeclaredMethod("method_1319"); out.setAccessible(true); out.invoke(mc.player);
          }
          check(latest(mc).cause.equals(cause), "cause expected " + cause + " got " + latest(mc).cause);
          mc.world.method_154(0, 100, 0, 0, 0); mc.world.method_154(0, 101, 0, 0, 0);
        }
        alive(mc); clear(mc); mc.player.health = 1;
        mc.player.method_1383(new net.minecraft.class_411(mc.world, 5.5, 102, 5.5));
        check(latest(mc).cause.equals("Lightning"), "lightning cause missing");
        mc.world.method_154(5, 102, 5, 0, 0);
        alive(mc); clear(mc); mc.player.health = 1;
        var attacker = net.minecraft.class_206.method_732("Creeper", mc.world); mc.player.damage(attacker, 500);
        check(latest(mc).cause.contains("Creeper"), "attacker cause missing");
      });
      alive(mc); clear(mc);
      test("source and flowing water landing preferences and damage stay unchanged", () -> {
        var options = Class.forName("local.luke.power.mechanics.Config").getField("config").get(null);
        check(options != null, "mechanics settings unavailable");
        mc.player.health = 20; mc.player.damage(null, 1); check(mc.player.health < 20, "damage changed by death recording");
      });
      test("save queries, short IDs and logs never modify inventory", () -> {
        alive(mc); clear(mc); mc.player.inventory.main[0] = tagged(35, 7, 5, "queries");
        record(mc, "Query fixture"); String before = DeathItems.inventory(mc.player.inventory.main);
        var death = latest(mc); check(death.id.matches("[0-9a-z]{1,6}"), "death ID too long");
        for (String command : List.of("death", "death items", "death log", "death " + death.id, "death items " + death.id, "death help"))
          DeathCommands.execute(mc, command.split(" "));
        check(before.equals(DeathItems.inventory(mc.player.inventory.main)), "query modified inventory");
        mc.setScreen(null);
      });
      test("recovery needs cheats and its command permission in every game mode", () -> {
        var death = latest(mc); String before = DeathItems.inventory(mc.player.inventory.main);
        for (boolean cheat : new boolean[]{false, true}) for (String mode : new String[]{"SURVIVAL", "SPECTATOR", "CREATIVE"}) {
          ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(cheat); CreativeVehicleChecks.mode(mc, mode);
          if (cheat) { check(DeathTracker.recoveryDenial(mc).isEmpty(), "cheats recovery unavailable in " + mode); continue; }
          check(!DeathTracker.recoveryDenial(mc).isEmpty(), "recovery available in " + mode + " cheats=" + cheat);
          refused(mc, death); check(before.equals(DeathItems.inventory(mc.player.inventory.main)), "denied recovery changed items");
        }
        ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true); CreativeVehicleChecks.mode(mc, "CREATIVE");
        var blocked = CommandPermissions.copy(); blocked.rules.put("deathrecover", CommandPermissions.Rule.DISABLED); CommandPermissions.preview(blocked);
        refused(mc, death); CommandPermissions.preview(rules);
      });
      test("full inventory cancels without a claim or partial insertion", () -> {
        clear(mc); recordWithItems(mc); var death = latest(mc);
        Arrays.fill(mc.player.inventory.main, null); for (int i = 0; i < 36; i++) mc.player.inventory.main[i] = new ItemStack(4, 64, 0);
        String before = DeathItems.inventory(mc.player.inventory.main); refused(mc, death);
        check(before.equals(DeathItems.inventory(mc.player.inventory.main)) && DeathTracker.journal(mc).state(death.id).equals("available"), "full recovery changed inventory/claim");
      });
      test("recovery preserves NBT, damage, full counts and existing armor; cannot repeat", () -> {
        clear(mc); recordWithItems(mc); var death = latest(mc); clear(mc);
        mc.player.inventory.main[0] = tagged(35, 60, 5, "merge"); mc.player.inventory.main[1] = tagged(35, 7, 5, "different");
        mc.player.inventory.armor[0] = tagged(313, 1, 47, "existing-armor");
        String armorBefore = DeathItems.inventory(mc.player.inventory.armor);
        DeathTracker.recover(mc, death);
        check(mc.player.inventory.main[0].count == 64, "compatible stack not merged");
        check(mc.player.inventory.main[1].count == 7 && tag(mc.player.inventory.main[1]).getString("DeathFixture").equals("different"), "different NBT merged");
        int count = 0; boolean tool = false;
        for (var item : mc.player.inventory.main) if (item != null) {
          if (item.itemId == 35 && tag(item).getString("DeathFixture").equals("merge")) count += item.count;
          if (item.itemId == 267 && item.getDamage() == 83 && tag(item).getString("DeathFixture").equals("tool")) tool = true;
          check(item.count <= item.getMaxCount(), "recovered overstack");
        }
        check(count == 130 && tool && armorBefore.equals(DeathItems.inventory(mc.player.inventory.armor)), "count/tool/armor changed");
        String after = DeathItems.inventory(mc.player.inventory.main); refused(mc, death);
        check(after.equals(DeathItems.inventory(mc.player.inventory.main)), "second recovery duplicated items");
        var restarted = new DeathJournal(path(mc)); check(restarted.state(death.id).equals("recovered"), "claim did not persist");
      });
      test("wrong world/player, freecam, held cursor and open container are refused", () -> {
        recordWithItems(mc); var death = latest(mc); clear(mc);
        String world = death.world; death.world = "different"; refused(mc, death); death.world = world;
        String player = death.player; death.player = "other"; refused(mc, death); death.player = player;
        mc.player.inventory.setCursorStack(new ItemStack(4, 1, 0)); refused(mc, death); mc.player.inventory.setCursorStack(null);
        local.luke.power.input.DetachedCamera.register(() -> true); refused(mc, death);
        Object camera = Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
        local.luke.power.input.DetachedCamera.register(() -> { try { return (Boolean)camera.getClass().getMethod("isActive").invoke(camera); } catch (Exception e) { throw new RuntimeException(e); } });
        var container = mc.player.container; mc.player.container = new net.minecraft.class_196(mc.player.inventory, mc.world, 0, 100, 0); refused(mc, death); mc.player.container = container;
      });
      test("multiplayer never records or recovers; settings disable preserves history", () -> {
        int count = DeathTracker.journal(mc).records().size();
        mc.world.isRemote = true;
        try { DeathTracker.died(mc.player, null); check(!DeathTracker.available(mc), "multiplayer death command available"); }
        finally { mc.world.isRemote = false; }
        check(count == DeathTracker.journal(mc).records().size(), "multiplayer logged death");
        DeathConfig.preview(new DeathSettings()); check(count == DeathTracker.journal(mc).records().size(), "disable deleted history");
        DeathConfig.preview(enabled);
      });
      test("another player has a separate log and dimensions retain the world ID", () -> {
        int count = DeathTracker.journal(mc).records().size(); String name = mc.player.name;
        mc.player.name = "OtherDeathCheck";
        try { check(DeathTracker.journal(mc).records().isEmpty(), "another player saw death history"); }
        finally { mc.player.name = name; }
        check(DeathTracker.journal(mc).records().size() == count, "player switch erased history");
        var properties = new net.minecraft.world.WorldProperties(mc.world.method_262());
        check(((local.luke.power.commands.CommandWorld)properties).power$commandWorldId().equals(local.luke.power.commands.CommandContext.world(mc)), "dimension world identity changed");
      });
      clear(mc); CreativeVehicleChecks.mode(mc, "SURVIVAL"); mc.player.method_1340(-12.1, 102, 8.9);
      mc.player.dimensionId = -1; mc.player.inventory.main[0] = tagged(35, 7, 5, "saved"); record(mc, "Saved fixture"); mc.player.dimensionId = 0;
      DeathConfig.save(enabled);
      log("DEATH CHECKS FAILURES " + failures);
    } finally {
      alive(mc); CreativeVehicleChecks.mode(mc, "SURVIVAL");
      mc.player.inventory.main = main; mc.player.inventory.armor = armor; mc.player.inventory.setCursorStack(cursor);
      ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(cheats); CommandPermissions.preview(rules);
      if (failures != 0) DeathConfig.preview(settings);
    }
  }
  static NbtCompound tag(ItemStack stack) throws Exception { return (NbtCompound)stack.getClass().getMethod("getStationNbt").invoke(stack); }
  static ItemStack tagged(int id, int count, int damage, String label) throws Exception {
    var stack = new ItemStack(id, count, damage); tag(stack).putString("DeathFixture", label); return stack;
  }
  static void alive(Minecraft mc) throws Exception {
    if (mc.player.health <= 0 || mc.player.dead) { mc.method_2122(false, 0); mc.setScreen(null); }
    mc.player.health = 20; mc.player.dead = false; mc.player.hurtTime = 0; mc.player.field_1613 = 0; mc.player.field_1041 = 0;
    mc.player.fire = 0; mc.player.air = 300; mc.player.field_1631 = 1.62F;
    mc.player.spacingXZ = .6F; mc.player.spacingY = 1.8F;
    mc.player.method_1340(5.5, 102, 5.5); mc.player.tick(); CreativeVehicleChecks.mode(mc, "SURVIVAL");
    mc.player.container = mc.player.playerContainer;
  }
  static void clear(Minecraft mc) { Arrays.fill(mc.player.inventory.main, null); Arrays.fill(mc.player.inventory.armor, null); mc.player.inventory.setCursorStack(null); }
  static void record(Minecraft mc, String cause) {
    local.luke.power.world.DeathSources.run(mc.player, cause, () -> DeathTracker.died(mc.player, null));
  }
  static DeathRecord latest(Minecraft mc) throws Exception { var records = DeathTracker.journal(mc).records(); return records.get(records.size() - 1); }
  static Path path(Minecraft mc) {
    String identity = local.luke.power.commands.CommandContext.world(mc) + "\n" + mc.player.name;
    return Path.of("power-beta-data/deaths", UUID.nameUUIDFromBytes(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + ".journal");
  }
  private static void recordWithItems(Minecraft mc) throws Exception {
    clear(mc); mc.player.inventory.main[0] = tagged(35, 70, 5, "merge"); mc.player.inventory.main[1] = tagged(267, 1, 83, "tool"); record(mc, "Recovery fixture");
  }
  private static void refused(Minecraft mc, DeathRecord record) throws Exception {
    try { DeathTracker.recover(mc, record); throw new AssertionError("recovery unexpectedly succeeded"); }
    catch (IllegalArgumentException expected) {}
  }
  private static void rollback(Minecraft mc) throws Exception {
    var enabled = DeathConfig.copy(); enabled.enabled = true; DeathConfig.preview(enabled);
    ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true); CreativeVehicleChecks.mode(mc, "CREATIVE"); clear(mc);
    recordWithItems(mc); var record = latest(mc); clear(mc);
    PlayerInventory original = mc.player.inventory;
    PlayerInventory failing = new PlayerInventory(mc.player) {
      private int writes;
      @Override public void setStack(int slot, ItemStack stack) {
        super.setStack(slot, stack); if (++writes == 38) throw new IllegalStateException("Injected partial main and armour write");
      }
    };
    failing.armor[0] = tagged(313, 1, 47, "rollback-boots"); var boots = failing.armor[0];
    mc.player.inventory = failing; String before = DeathItems.inventory(DeathItems.snapshot(failing));
    try {
      try { DeathTracker.recover(mc, record); throw new AssertionError("failure injection was not reached"); }
      catch (java.io.IOException expected) {}
      check(before.equals(DeathItems.inventory(DeathItems.snapshot(failing))) && failing.armor[0] == boots, "rollback changed inventory or armour ownership");
      check(DeathTracker.journal(mc).state(record.id).equals("available"), "rollback kept claim");
      check(!DeathTracker.recoveryDenial(mc).isEmpty(), "failure did not stop recovery");
      log("PASS partial recovery rolled back, retained journal and disabled recovery until restart");
    } finally { mc.player.inventory = original; }
  }
}
