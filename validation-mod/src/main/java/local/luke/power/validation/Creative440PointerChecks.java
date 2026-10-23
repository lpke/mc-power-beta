package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.creative.CreativeTabsData;
import local.luke.power.creativeui.CreativeTabsBackend;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/** Native pointer events operate on an isolated draft. No fixture-generated inventory writes. */
final class Creative440PointerChecks {
  private static CreativeTabsScreen screen;
  private static Screen parent;
  private static Setting setting;
  private static JsonElement settingBefore;
  private static byte[] configBefore;
  private static List<String> inventoryBefore, expected;
  private static int accepted;
  private static List<String> inventory(Minecraft mc) {
    List<String> result = new ArrayList<>();
    for (ItemStack stack : mc.player.inventory.main) result.add(stack == null ? "" : stack.count + ":" + stack.writeNbt(new NbtCompound()));
    ItemStack cursor = mc.player.inventory.getCursorStack(); result.add(cursor == null ? "" : cursor.count + ":" + cursor.writeNbt(new NbtCompound()));
    return result;
  }
  private static CreativeTabsData data() throws Exception { return (CreativeTabsData)field(screen, "data"); }
  private static List<String> cells() throws Exception {
    CreativeTabsData data = data();
    for (int i = 0; i < data.size(); i++) if (data.tab(i).get("id").getAsString().equals("first")) return CreativeTabsData.slots(data.tab(i));
    throw new AssertionError("First tab was removed");
  }
  private static void unchanged(Minecraft mc) throws Exception {
    check(Arrays.equals(configBefore, Files.readAllBytes(PowerConfig.path())), "Pointer editor wrote configuration");
    check(inventoryBefore.equals(inventory(mc)), "Pointer editor changed native inventory/cursor");
    check(accepted == 0 && setting.value.equals(settingBefore), "Pointer editor published an unfinished draft");
  }
  private static void clearSelection() throws Exception {
    ((Set<?>)field(screen, "cells")).clear(); field(screen, "anchor", -1);
  }
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("editor")) {
      check(screen == null, "Run creative-440 stop before opening another pointer draft");
      ConfigSession session = new ConfigSession(); CreativeTabsBackend.register(session); setting = session.settings().get(0);
      CreativeTabsData data = CreativeTabsData.read(CreativeTabsData.defaults()); data.customized(true);
      List<String> values = new ArrayList<>(List.of("1", "2", "3", "4", "5", "6", "7", "12"));
      while (values.size() < 64) values.add(""); expected = List.copyOf(values);
      data.append(CreativeTabsData.tab("first", "First", "1", values));
      data.append(CreativeTabsData.tab("second", "Second", "2", Collections.nCopies(64, "")));
      data.append(CreativeTabsData.tab("third", "Third", "3", Collections.nCopies(64, "")));
      setting.value = data.json(); settingBefore = setting.value.deepCopy(); accepted = 0;
      parent = mc.currentScreen; configBefore = Files.readAllBytes(PowerConfig.path()); inventoryBefore = inventory(mc);
      screen = new CreativeTabsScreen(parent, setting, () -> accepted++); mc.setScreen(screen);
      int x = (int)call(screen, "gridLeft"), y = (int)call(screen, "gridTop"), left = (int)call(screen, "left");
      log("CREATIVE440 POINTER ready GUI=" + screen.width + "x" + screen.height + " display=" + mc.displayWidth + "x" + mc.displayHeight
          + " grid=" + x + "," + y + " cell=18 center=" + (x + 9) + "," + (y + 9) + " tabs=" + (left + 25) + ",70,93,116");
      log("CREATIVE440 POINTER single: drag cell0 to cell3, then creative-440 pointer-check single"); return;
    }
    check(screen != null, "Run creative-440 editor first");
    if (action.equals("stop")) {
      try { unchanged(mc); } finally { mc.setScreen(parent); screen = null; }
      log("CREATIVE440 POINTER stopped; native inventory and configuration unchanged"); return;
    }
    check(action.startsWith("pointer-check ") && mc.currentScreen == screen, "Keep the pointer editor open");
    String stage = action.substring("pointer-check ".length()).trim();
    List<String> next = new ArrayList<>(expected); Set<Integer> selection;
    switch (stage) {
      case "single" -> { Collections.swap(next, 0, 3); selection = Set.of(3); }
      case "multi" -> { next = CreativeTabsData.moveCells(expected, Set.of(1, 3), 2); selection = Set.of(3, 5); }
      case "range" -> { next = CreativeTabsData.moveCells(expected, Set.of(0, 1, 2), 8); selection = Set.of(8, 9, 10); }
      case "tabs" -> {
        List<String> order = new ArrayList<>(); for (int i = 0; i < data().size(); i++) order.add(data().tab(i).get("id").getAsString());
        check(order.equals(List.of("second", "third", "first")), "Tab drag order=" + order);
        check((int)field(screen, "selected") == 2, "Tab drag lost the selected tab"); selection = Set.of();
      }
      case "focus" -> selection = Set.copyOf((Set<Integer>)field(screen, "cells"));
      default -> throw new IllegalArgumentException("Use single, multi, range, tabs or focus");
    }
    check(cells().equals(next), "Pointer " + stage + " cells=" + cells().subList(0, 16) + " expected=" + next.subList(0, 16));
    if (!stage.equals("tabs")) check(((Set<?>)field(screen, "cells")).equals(selection), "Pointer " + stage + " selection=" + field(screen, "cells"));
    check((int)field(screen, "dragCell") == -1 && (int)field(screen, "dragTab") == -1, "Native release did not relinquish drag ownership");
    unchanged(mc); expected = List.copyOf(next); clearSelection();
    log("PASS CREATIVE440 POINTER " + stage + " native event selection/drag, isolated draft and unchanged inventory/configuration");
    switch (stage) {
      case "single" -> log("CREATIVE440 POINTER multi: Ctrl-click cells1 and3, release Ctrl, drag cell3 to cell5, then pointer-check multi");
      case "multi" -> log("CREATIVE440 POINTER range: Ctrl-click cell0, release Ctrl; Shift-click cell2, release Shift; drag cell0 to cell8, then pointer-check range");
      case "range" -> log("CREATIVE440 POINTER tabs: drag first tab row to third row, then pointer-check tabs");
      default -> log("CREATIVE440 POINTER run creative-440 stop when finished");
    }
  }
}
