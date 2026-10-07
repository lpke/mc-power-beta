package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import local.luke.power.permissions.CheatWorld;
import local.luke.power.worldedit.WorldEditor;
import local.luke.power.worldedit.config.WorldOverride;
import local.luke.power.worldedit.core.Pos;
import local.luke.power.worldedit.core.Region;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

final class WorldEditHandChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    check(mc.world != null && !mc.world.isRemote, "requires a disposable singleplayer world");
    var rules = (CheatWorld) mc.world.method_262();
    boolean cheats = rules.power$cheatsEnabled();
    var override = WorldEditor.worldOverride(mc);
    var settings = WorldEditor.settings().copy();
    var inventory = mc.player.inventory.main.clone();
    int slot = mc.player.inventory.selectedSlot;
    try {
      mc.setScreen(null); rules.power$cheatsEnabled(true);
      WorldEditor.worldOverride(mc, WorldOverride.ENABLED);
      var enabled = settings.copy(); enabled.enabled = true; WorldEditor.preview(enabled);
      mc.player.inventory.selectedSlot = 0;
      var editor = WorldEditor.editor(mc);
      editor.select(Region.between(new Pos(0, 101, 0), new Pos(1, 101, 0)));
      test("set hand snapshots wool metadata and preserves the stack; undo and redo work", () -> {
        mc.world.method_201(0, 101, 0, 1, 0); mc.world.method_201(1, 101, 0, 1, 0);
        ItemStack stack = new ItemStack(35, 17, 14); mc.player.inventory.main[0] = stack;
        ChatChecks.submit(mc, "//set hand");
        mc.player.inventory.main[0] = new ItemStack(44, 9, 2);
        drain(); expect(mc, 0, 35, 14); expect(mc, 1, 35, 14);
        check(stack.count == 17 && stack.getDamage() == 14, "held wool changed");
        ChatChecks.submit(mc, "//undo"); drain(); expect(mc, 0, 1, 0); expect(mc, 1, 1, 0);
        ChatChecks.submit(mc, "//redo"); drain(); expect(mc, 0, 35, 14); expect(mc, 1, 35, 14);
        ChatChecks.submit(mc, "//set hand"); drain(); expect(mc, 0, 44, 2); expect(mc, 1, 44, 2);
        check(mc.player.inventory.main[0].count == 9, "held slabs consumed");
      });
      test("hand replacement masks match only the held variant", () -> {
        mc.world.method_201(0, 101, 0, 35, 14); mc.world.method_201(1, 101, 0, 35, 0);
        mc.player.inventory.main[0] = new ItemStack(35, 8, 14);
        ChatChecks.submit(mc, "//replace hand stone"); drain(); expect(mc, 0, 1, 0); expect(mc, 1, 35, 0);
        ChatChecks.submit(mc, "//gmask hand");
        mc.player.inventory.main[0] = new ItemStack(35, 8, 0);
        ChatChecks.submit(mc, "//set dirt"); drain(); expect(mc, 0, 1, 0); expect(mc, 1, 35, 0);
        ChatChecks.submit(mc, "//gmask");
      });
      test("empty hands, tools and empty stacks fail without world edits", () -> {
        int undo = editor.engine.undoSize();
        for (ItemStack stack : new ItemStack[] {null, new ItemStack(278, 1, 0), new ItemStack(1, 0, 0)}) {
          mc.player.inventory.main[0] = stack;
          var messages = ChatChecks.submit(mc, "//set hand");
          check(messages.stream().anyMatch(s -> s.contains("Hold a block to use hand")), "missing invalid-hand message " + messages);
          check(!editor.engine.busy() && editor.engine.undoSize() == undo, "invalid hand queued an edit");
          expect(mc, 0, 1, 0); expect(mc, 1, 35, 0);
        }
      });
      test("hand does not bypass cheats", () -> {
        mc.player.inventory.main[0] = new ItemStack(1, 64, 0); rules.power$cheatsEnabled(false);
        var messages = ChatChecks.submit(mc, "//set hand");
        check(messages.stream().anyMatch(s -> s.contains("Cheats are disabled")), "cheats gate bypassed");
        check(!editor.engine.busy(), "disabled cheats queued an edit"); expect(mc, 1, 35, 0);
      });
    } finally {
      WorldEditor.leaveWorld(); WorldEditor.preview(settings);
      rules.power$cheatsEnabled(cheats); WorldEditor.worldOverride(mc, override);
      System.arraycopy(inventory, 0, mc.player.inventory.main, 0, inventory.length);
      mc.player.inventory.selectedSlot = slot; mc.setScreen(null);
    }
    log("WORLDEDIT HAND FAILURES " + failures);
  }

  private static void drain() {
    int ticks = 0;
    while (WorldEditor.current().engine.busy() && ticks++ < 100) WorldEditor.current().engine.tick(1000);
    check(!WorldEditor.current().engine.busy(), "edit did not finish");
  }

  private static void expect(Minecraft mc, int x, int id, int meta) {
    check(mc.world.getBlockId(x, 101, 0) == id && mc.world.method_1778(x, 101, 0) == meta,
        "wrong block at " + x + ": " + mc.world.getBlockId(x, 101, 0) + ":" + mc.world.method_1778(x, 101, 0));
  }
}
