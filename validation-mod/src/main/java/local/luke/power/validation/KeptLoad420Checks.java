package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.io.*;
import java.util.*;
import local.luke.power.death.*;
import local.luke.power.offhand.Offhand;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;

/** Native byte-count reloads use unregistered scratch players and leave live recovery state alone. */
final class KeptLoad420Checks {
  private KeptLoad420Checks() {}
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null, "load a disposable local world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    failures = 0;
    String live = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
    var liveKept = ((KeptPlayer)mc.player).power$keptInventory();
    test("KEPT420 native reload preserves negative, zero and positive byte-wrap counts in every saved owner", () -> {
      var source = player(mc); seed(source); var expected = KeptInventory.capture(source);
      ((KeptPlayer)source).power$keptInventory(expected); source.health = 0;
      var tag = new NbtCompound(); source.write(tag); byte[] before = nbt(tag);
      var loaded = player(mc); loaded.read(tag);
      check(expected.matches(loaded) && KeepInventory.kept(loaded), "native kept reload lost full counts, slots or item data");
      check(Arrays.equals(before, nbt(tag)) && Arrays.equals(expected.bytes(), ((KeptPlayer)loaded).power$keptInventory().bytes()),
          "native kept reload changed its source tag or attached recovery snapshot");
    });
    test("KEPT420 mismatched later item data rejects all count repairs and keeps source evidence", () -> {
      var source = player(mc); seed(source); var expected = KeptInventory.capture(source);
      ((KeptPlayer)source).power$keptInventory(expected); source.health = 0;
      var tag = new NbtCompound(); source.write(tag);
      entry(tag, 9).putShort("Damage", (short)4); byte[] before = nbt(tag);
      var loaded = player(mc); refused(() -> loaded.read(tag));
      check(loaded.inventory.main[8].count == (byte)128 && loaded.inventory.main[9].count == (byte)201,
          "a failed preflight partially normalized native counts");
      retained(loaded, expected, tag, before);
    });
    test("KEPT420 wrong logical slots reject matching item IDs and counts before any repair", () -> {
      var source = player(mc); seed(source); source.inventory.main[9].count = 128;
      var expected = KeptInventory.capture(source); ((KeptPlayer)source).power$keptInventory(expected); source.health = 0;
      var tag = new NbtCompound(); source.write(tag);
      var first = entry(tag, 8); var second = entry(tag, 9);
      first.putByte("Slot", (byte)9); second.putByte("Slot", (byte)8);
      byte[] before = nbt(tag); var loaded = player(mc); refused(() -> loaded.read(tag));
      check(loaded.inventory.main[8].count == (byte)128 && loaded.inventory.main[9].count == (byte)128,
          "wrong-slot reload partially changed counts");
      retained(loaded, expected, tag, before);
    });
    test("KEPT420 aliased native stacks and ordinary nonpositive restore remain rejected", () -> {
      var source = player(mc); source.inventory.main[8] = DeathChecks.tagged(35, 201, 5, "alias");
      source.inventory.main[9] = source.inventory.main[8].clone(); var expected = KeptInventory.capture(source);
      ItemStack alias = source.inventory.main[8]; alias.count = (byte)201; source.inventory.main[9] = alias;
      refused(() -> expected.restoreLoaded(source));
      check(source.inventory.main[8] == alias && source.inventory.main[9] == alias && alias.count == (byte)201,
          "alias rejection changed the native owner");
      source.inventory.main[9] = null; refused(() -> expected.restore(source));
      check(source.inventory.main[8] == alias && alias.count == (byte)201, "ordinary restore admitted or changed a nonpositive stack");
    });
    check(live.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)))
        && ((KeptPlayer)mc.player).power$keptInventory() == liveKept, "scratch reload changed the live player");
    log("KEPT LOAD420 failures=" + failures); check(failures == 0, "kept-load failures=" + failures);
  }
  private static ClientPlayerEntity player(Minecraft mc) {
    return new ClientPlayerEntity(mc, mc.world, mc.session, mc.player.dimensionId);
  }
  private static void seed(ClientPlayerEntity player) throws Exception {
    int[] counts = {128, 201, 256, 257, 513};
    for (int i = 0; i < counts.length; i++) player.inventory.main[8 + i] = DeathChecks.tagged(35, counts[i], 5, "kept-main-" + i);
    player.inventory.armor[0] = DeathChecks.tagged(313, 201, 71, "kept-armor");
    player.inventory.setCursorStack(DeathChecks.tagged(264, 257, 0, "kept-cursor"));
    player.playerContainer.method_2084(1).setStack(DeathChecks.tagged(35, 256, 4, "kept-grid"));
    Offhand.set(player.inventory, DeathChecks.tagged(35, 201, 3, "kept-offhand")); player.inventory.selectedSlot = 4;
  }
  private static NbtCompound entry(NbtCompound tag, int slot) {
    var list = tag.getList("Inventory");
    for (int i = 0; i < list.size(); i++) {
      var item = (NbtCompound)list.get(i); if ((item.getByte("Slot") & 255) == slot) return item;
    }
    throw new AssertionError("missing native inventory slot " + slot);
  }
  private static void retained(ClientPlayerEntity player, KeptInventory expected, NbtCompound tag, byte[] before) throws Exception {
    check(Arrays.equals(before, nbt(tag)), "rejected reload changed the source NBT");
    var saved = ((KeptPlayer)player).power$keptInventory();
    check(saved != null && Arrays.equals(expected.bytes(), saved.bytes()), "rejected reload detached its verified kept snapshot");
  }
  private static byte[] nbt(NbtCompound tag) throws IOException {
    var bytes = new ByteArrayOutputStream(); NbtIo.write(tag, new DataOutputStream(bytes)); return bytes.toByteArray();
  }
  private static void refused(Check operation) throws Exception {
    try { operation.run(); throw new AssertionError("unsafe native reload was accepted"); }
    catch (IOException | IllegalStateException expected) {}
  }
}
