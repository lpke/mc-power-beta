package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import local.luke.power.validation.mixin.Client253Access;
import local.luke.power.inventory.tools.InventoryToolsConfig;
import local.luke.power.offhand.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;

/** Actual native main/offhand use, attachment survival and support removal. */
final class OffhandTorch451Checks {
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive()
        && mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "focus a muted disposable world");
    failures = 0;
    try (var state = new AutoTools400State(mc)) {
      for (int torch : new int[]{50, 76}) for (boolean off : new boolean[]{false, true}) {
        test((off ? "offhand" : "main-hand") + " torch " + torch + " attaches to a spawner top", () -> {
          state.reset(); state.scene(52, 1); state.attack.set(false);
          var tools = InventoryToolsConfig.copy(); tools.restock = tools.toolSwitch = false; InventoryToolsConfig.preview(tools);
          var pickaxe = AutoTools400State.tagged(278, 1, 19, "pickaxe");
          var torches = AutoTools400State.tagged(torch, 4, 0, "torch");
          state.inventory.main[0] = off ? pickaxe : torches;
          Offhand.set(state.inventory, off ? torches : pickaxe);
          check(Offhand.playable(mc), "fixture did not enable native offhand use");
          String toolBytes = AutoTools400State.bytes(pickaxe);
          var expected = torches.clone(); expected.count--;
          OffhandUse.newPress(mc); ((Client253Access)(Object)mc).power$use(1); state.finish();
          check(mc.world.getBlockId(state.x, state.y + 1, state.z) == torch, "torch placement failed");
          check(mc.world.method_1778(state.x, state.y + 1, state.z) == 5, "torch is not upright");
          check(AutoTools400State.bytes(torches).equals(AutoTools400State.bytes(expected)), "torch consumed wrong native item data/count");
          check(toolBytes.equals(AutoTools400State.bytes(pickaxe)), "torch placement changed the pickaxe");
          check(Offhand.stack(mc.player, off ? local.luke.power.inventory.HandUse.Hand.OFF : local.luke.power.inventory.HandUse.Hand.MAIN) == torches,
              "torch placement changed hand ownership");
          Block.BLOCKS[torch].method_1609(mc.world, state.x, state.y + 1, state.z, 1);
          check(mc.world.getBlockId(state.x, state.y + 1, state.z) == torch, "neighbor update detached a supported torch");
          mc.world.method_229(state.x, state.y, state.z, 0);
          Block.BLOCKS[torch].method_1609(mc.world, state.x, state.y + 1, state.z, 0);
          check(mc.world.getBlockId(state.x, state.y + 1, state.z) == 0, "torch survived removal of its support");
          check(!OffhandUse.active(), "native hand scope leaked");
        });
      }
      test("spawners retain native cage transparency and full-face support", () -> {
        state.reset(); state.scene(52, 1);
        check(!Block.BLOCKS[52].method_1620() && mc.world.method_1780(state.x, state.y, state.z), "spawner support/transparency changed");
      });
    }
    log("OFFHAND TORCH451 failures=" + failures);
  }
}
