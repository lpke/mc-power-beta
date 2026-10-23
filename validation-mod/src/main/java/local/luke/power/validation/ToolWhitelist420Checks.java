package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.tools.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

final class ToolWhitelist420Checks {
  private ToolWhitelist420Checks() {}
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && org.lwjgl.opengl.Display.isActive(), "focus a disposable loaded world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    byte[] disk = Files.readAllBytes(PowerConfig.path()); var original = InventoryToolsConfig.copy();
    String inventory = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
    var screen = mc.currentScreen; failures = 0; Files.createDirectories(Sorting403Checks.REPORT);
    try {
      for (int[] size : new int[][]{{320, 240}, {640, 360}}) test("TOOLS420 isolated sword exclusion, Done and Discard " + size[0], () -> {
        var session = new ConfigSession(); InventoryToolsBackend.register(session);
        var row = Release110Checks.find(session, "inventoryTools.toolWhitelist");
        JsonArray all = new JsonArray(); all.add("*"); row.value = all;
        int[] accepted = {0};
        var picker = (ItemPickerScreen)ToolWhitelistEditor.open(screen, row, () -> accepted[0]++);
        mc.currentScreen = picker; picker.init(mc, size[0], size[1]);
        @SuppressWarnings("unchecked") var selected = (Set<String>)UiChecks.field(picker, "selected");
        check(selected.equals(new HashSet<>(ToolWhitelist.registered())), "All tools omitted a registered tool");
        ((TextInput)UiChecks.field(picker, "search")).setText("sword"); UiChecks.call(picker, "filter");
        @SuppressWarnings("unchecked") var swords = (List<ItemCatalogue.Entry>)UiChecks.field(picker, "shown");
        check(swords.size() == 5 && swords.stream().allMatch(entry -> Set.of("267", "268", "272", "276", "283").contains(entry.value())),
            "tool search did not list the five native swords");
        for (int i = 0; i < swords.size(); i++) ((ScreenInput)(Object)picker).power$click((int)UiChecks.call(picker, "left") + 10, 74 + i * 24, 0);
        check(accepted[0] == 0 && row.value.equals(all), "picker published a partial selection");
        check(swords.stream().noneMatch(entry -> selected.contains(entry.value())), "sword exclusion did not change the draft");
        Sorting403Checks.capture(mc, picker, "tool-whitelist-" + size[0], false);
        ((ScreenInput)(Object)picker).power$key('\0', Keyboard.KEY_RETURN);
        check(accepted[0] == 1 && row.value.getAsJsonArray().size() == ToolWhitelist.registered().size() - 5,
            "Done did not accept exactly the sword exclusions");
        session.preview(true);
        check(InventoryToolsConfig.current().toolWhitelist.stream().noneMatch(Set.of("267", "268", "272", "276", "283")::contains),
            "accepted selection retained swords");
        session.discard(); check(Catalog.JSON.toJsonTree(original).equals(Catalog.JSON.toJsonTree(InventoryToolsConfig.current())), "session Discard did not restore tools");

        JsonArray historical = new JsonArray(); historical.add("1"); historical.add("267"); row.value = historical;
        var retained = (ItemPickerScreen)ToolWhitelistEditor.open(screen, row, () -> accepted[0]++);
        mc.currentScreen = retained; retained.init(mc, size[0], size[1]);
        ((ScreenInput)(Object)retained).power$key('\0', Keyboard.KEY_RETURN);
        check(accepted[0] == 2 && row.value.equals(historical), "unchanged historical non-tool blocked saving the picker");
        row.value = all.deepCopy();

        var canceled = (ItemPickerScreen)ToolWhitelistEditor.open(screen, row, () -> accepted[0]++);
        mc.currentScreen = canceled; canceled.init(mc, size[0], size[1]);
        ((ScreenInput)(Object)canceled).power$click((int)UiChecks.call(canceled, "left") + 10, 74, 0);
        var before = row.value.deepCopy(); ((ScreenInput)(Object)canceled).power$key('\0', Keyboard.KEY_ESCAPE);
        check((boolean)UiChecks.call(UiChecks.field(canceled, "exitPrompt"), "active"), "changed picker skipped its exit prompt");
        int width = Math.min(76, Math.max(10, (canceled.width - 32) / 3));
        ((ScreenInput)(Object)canceled).power$click((canceled.width - (width * 3 + 8)) / 2 + width + 4 + width / 2, canceled.height / 2 + 18, 0);
        check(accepted[0] == 2 && row.value.equals(before), "picker Discard published a selection");
        check(inventory.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)))
            && Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "tool editor changed native items or saved settings");
      });
    } finally { InventoryToolsConfig.preview(original); mc.setScreen(screen); }
    log("TOOL WHITELIST420 failures=" + failures + " captures=" + Sorting403Checks.REPORT);
    check(failures == 0, "tool-whitelist failures=" + failures);
  }
}
