package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.DeathChecks.*;
import java.nio.file.Path;
import java.util.*;
import com.google.gson.JsonPrimitive;
import local.luke.power.config.*;
import local.luke.power.death.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.world.WorldInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

final class KeepInventoryChecks {
  static void run(Minecraft mc, String action) throws Exception {
    failures = 0; mc.setScreen(null);
    if (action.equals("reload")) {
      check(((WorldInventory)mc.world.method_262()).power$keepInventory(), "world rule did not persist");
      check(mc.player.health <= 0 && KeepInventory.kept(mc.player), "dead-player kept snapshot did not persist");
      var saved = ((KeptPlayer)mc.player).power$keptInventory();
      check(saved.matches(mc.player), "native NBT restart changed stacks");
      mc.method_2122(false, 0); mc.setScreen(null); check(saved.matches(mc.player), "restart respawn lost kept stacks");
      check(DeathTracker.journal(mc).state(latest(mc).id).equals("kept"), "kept claim did not persist");
      log("PASS world rule, dead-player full counts/NBT/cursor/crafting and kept claims survive restart and real respawn");
      ((WorldInventory)mc.world.method_262()).power$keepInventory(false); clear(mc); clearCraft(mc); return;
    }
    alive(mc); clear(mc); clearCraft(mc);
    var properties = (CheatWorld)mc.world.method_262(); var rule = (WorldInventory)properties;
    var settings = DeathConfig.copy(); boolean cheats = properties.power$cheatsEnabled();
    var enabled = new DeathSettings(); enabled.enabled = true; enabled.announceLocation = false; DeathConfig.preview(enabled);
    try {
      test("world rule defaults off, hides without cheats, and waits for Apply", () -> {
        properties.power$cheatsEnabled(false); rule.power$keepInventory(true); check(!rule.power$keepInventory(), "keep rule active without cheats");
        var session = SettingsRegistry.open(mc); var keep = Release110Checks.find(session, "world.keepInventory");
        check(!keep.defaultValue.getAsBoolean() && !SettingAccess.visible(session, keep), "default or hidden rule wrong");
        properties.power$cheatsEnabled(true); session = SettingsRegistry.open(mc); keep = Release110Checks.find(session, "world.keepInventory");
        check(keep.group.equals("World overrides") && SettingAccess.visible(session, keep), "wrong group or rule still hidden");
        keep.value = new JsonPrimitive(true); session.preview(true); check(!rule.power$keepInventory(), "draft applied keep rule"); session.discard();
        session = SettingsRegistry.open(mc); Release110Checks.find(session, "world.keepInventory").value = new JsonPrimitive(true); session.save(Path.of("."));
        check(rule.power$keepInventory(), "Apply ignored keep rule");
        session = SettingsRegistry.open(mc); var gate = Release110Checks.find(session, "world.cheats"); gate.value = new JsonPrimitive(false); session.link(gate);
        check(!Release110Checks.find(session, "world.keepInventory").value.getAsBoolean(), "hidden draft did not reset off"); session.discard(); check(rule.power$keepInventory(), "Cancel changed live rule");
        session = SettingsRegistry.open(mc); gate = Release110Checks.find(session, "world.cheats"); gate.value = new JsonPrimitive(false); session.link(gate); session.save(Path.of("."));
        check(!rule.power$keepInventory(), "Apply disabling cheats kept rule on"); properties.power$cheatsEnabled(true); check(!rule.power$keepInventory(), "re-enabling cheats revived rule");
      });
      test("rule is saved per world, copied across dimensions, and resets on direct cheats disable", () -> {
        rule.power$keepInventory(true); NbtCompound tag = mc.world.method_262().asNbt();
        var restored = new net.minecraft.world.WorldProperties(tag);
        check(((WorldInventory)restored).power$keepInventory(), "NBT rule lost");
        var copy = new net.minecraft.world.WorldProperties(mc.world.method_262()); check(((WorldInventory)copy).power$keepInventory(), "dimension copy lost rule");
        ((CheatWorld)copy).power$cheatsEnabled(false); ((CheatWorld)copy).power$cheatsEnabled(true); check(!((WorldInventory)copy).power$keepInventory(), "direct disable revived rule");
        var fresh = new net.minecraft.world.WorldProperties(1L, "Keep inventory fresh"); check(!((WorldInventory)fresh).power$keepInventory(), "fresh world rule on");
        tag.putBoolean("PowerBetaCheatsEnabled", false); check(!((WorldInventory)new net.minecraft.world.WorldProperties(tag)).power$keepInventory(), "NBT enabled keep without cheats");
      });
      test("real death retains inventory, equipment, cursor and crafting with no item drops", () -> {
        rule.power$keepInventory(true); setup(mc); var saved = KeptInventory.capture(mc.player); int drops = drops(mc);
        mc.player.health = 1; mc.player.damage(null, 10); saved = ((KeptPlayer)mc.player).power$keptInventory(); var death = latest(mc);
        check(mc.player.health <= 0 && saved.matches(mc.player), "death changed kept inventory");
        mc.player.markDead(); check(saved.matches(mc.player), "body removal or container closure changed items");
        check(drops(mc) == drops && death.kept && DeathTracker.journal(mc).state(death.id).equals("kept"), "drops created or record unclaimed");
        check(death.slots.equals(List.of(8, 20, 39, 40)), "kept death snapshot positions changed");
        var old = mc.player; mc.method_2122(false, 0); mc.setScreen(null);
        check(saved.matches(mc.player) && !KeepInventory.kept(mc.player), "real respawn changed stacks or kept stale death state");
        check(Arrays.stream(old.inventory.main).allMatch(Objects::isNull) && Arrays.stream(old.inventory.armor).allMatch(Objects::isNull) && old.inventory.getCursorStack() == null, "old body retained duplicated items");
        mc.player.inventory.setCursorStack(null); var screen = DeathInventoryChecks.open(mc, death, 320, 240);
        String before = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory)); DeathInventoryChecks.bulk(screen); DeathInventoryChecks.click(screen, 8, 0);
        check(before.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory))), "kept viewer allowed duplicate recovery");
        SettingsSnapshot.render(mc, screen, 320, 240, Path.of("power-beta-data/reports/kept-inventory.png")); mc.setScreen(null);
        var output = ChatChecks.submit(mc, "/death log"); check(output.stream().anyMatch(t -> t.contains("Recovered (kept)")), "kept log label missing");
      });
      test("kept snapshots preserve full counts and NBT through native dead-player save/reload", () -> {
        alive(mc); setup(mc); mc.player.inventory.main[8].count = 201; var saved = KeptInventory.capture(mc.player);
        mc.player.health = 1; mc.player.damage(null, 10); saved = ((KeptPlayer)mc.player).power$keptInventory(); var tag = new NbtCompound(); mc.player.writeNbt(tag);
        mc.player.inventory.main[8] = null; mc.player.inventory.setCursorStack(null); mc.player.readtNbt(tag);
        check(saved.matches(mc.player), "native NBT lost full count, damage, tags or position");
        mc.method_2122(false, 0); mc.setScreen(null); check(saved.matches(mc.player), "respawn after NBT load changed stacks");
      });
      test("disabling cheats after death keeps that death's items; the next death drops normally", () -> {
        alive(mc); setup(mc); rule.power$keepInventory(true); var saved = KeptInventory.capture(mc.player);
        mc.player.health = 1; mc.player.damage(null, 10); saved = ((KeptPlayer)mc.player).power$keptInventory(); properties.power$cheatsEnabled(false);
        mc.method_2122(false, 0); mc.setScreen(null); check(saved.matches(mc.player), "cheats disable discarded already kept inventory");
        properties.power$cheatsEnabled(true); check(!rule.power$keepInventory(), "rule revived after cheats on");
        clear(mc); clearCraft(mc); mc.player.inventory.main[0] = tagged(264, 7, 0, "normal-drop"); int drops = drops(mc);
        mc.player.health = 1; mc.player.damage(null, 10);
        check(mc.player.inventory.main[0] == null && drops(mc) > drops && !latest(mc).kept, "off rule suppressed normal drops"); alive(mc);
      });
      test("keep rule works independently of death records and excludes multiplayer", () -> {
        clear(mc); clearCraft(mc); DeathConfig.preview(new DeathSettings()); rule.power$keepInventory(true); setup(mc);
        int records = DeathTracker.journal(mc).records().size(); var saved = KeptInventory.capture(mc.player);
        mc.player.health = 1; mc.player.damage(null, 10); saved = ((KeptPlayer)mc.player).power$keptInventory(); mc.method_2122(false, 0); mc.setScreen(null);
        check(saved.matches(mc.player) && DeathTracker.journal(mc).records().size() == records, "disabled death feature changed keep rule or logged death");
        clear(mc); clearCraft(mc); mc.world.isRemote = true;
        try { KeepInventory.begin(mc.player); check(((KeptPlayer)mc.player).power$keptInventory() == null, "client kept multiplayer inventory"); }
        finally { mc.world.isRemote = false; DeathConfig.preview(enabled); }
      });
      if (action.equals("save")) {
        alive(mc); properties.power$cheatsEnabled(true); rule.power$keepInventory(true); setup(mc); mc.player.inventory.main[8].count = 201;
        DeathConfig.save(enabled); mc.player.health = 1; mc.player.damage(null, 10);
        log("PASS staged dead-player save for restart; run keep-inventory reload after new-world");
      }
      log("KEEP INVENTORY FAILURES " + failures);
    } finally {
      if (!action.equals("save")) { alive(mc); clear(mc); clearCraft(mc); rule.power$keepInventory(false); properties.power$cheatsEnabled(cheats); DeathConfig.preview(settings); }
    }
  }
  private static void setup(Minecraft mc) throws Exception {
    clear(mc); clearCraft(mc); CreativeVehicleChecks.mode(mc, "SURVIVAL");
    mc.player.inventory.main[8] = tagged(35, 7, 5, "kept-main"); mc.player.inventory.main[20] = tagged(267, 1, 83, "kept-tool");
    mc.player.inventory.armor[3] = tagged(310, 1, 27, "kept-helmet"); mc.player.inventory.setCursorStack(tagged(264, 3, 0, "kept-cursor"));
    mc.player.inventory.selectedSlot = 8; mc.player.playerContainer.method_2084(1).setStack(tagged(35, 2, 5, "kept-craft"));
  }
  private static void clearCraft(Minecraft mc) { for (int i = 1; i <= 4; i++) mc.player.playerContainer.method_2084(i).setStack(null); }
  private static int drops(Minecraft mc) { int n = 0; for (Object entity : mc.world.field_198) if (entity instanceof net.minecraft.class_142 item && !item.dead) n++; return n; }
}
