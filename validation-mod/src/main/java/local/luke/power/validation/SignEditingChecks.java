package local.luke.power.validation;

import com.google.gson.JsonPrimitive;
import java.util.Arrays;
import local.luke.power.config.SettingsRegistry;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.block.Block;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.input.Keyboard;

import static local.luke.power.validation.Validation.*;

/** Run only in a disposable world. Checks the real editor and its save/removal hooks. */
public final class SignEditingChecks {
  private static boolean capturePackets;
  private static final java.util.List<net.minecraft.network.packet.play.UpdateSignPacket> packets = new java.util.ArrayList<>();

  public static boolean capture(net.minecraft.network.Packet packet) {
    if (!capturePackets) return false;
    packets.add((net.minecraft.network.packet.play.UpdateSignPacket) packet);
    return true;
  }

  static void run(Minecraft mc) throws Exception {
    failures = 0;
    var session = SettingsRegistry.open(mc);
    var allowEditing = find(session, "power_mechanics:config.INTERACTIVE_BLOCK_CONFIG.allowEditingSigns");
    boolean sneaking = mc.player.field_161.field_2536;
    try {
      mc.player.field_161.field_2536 = false;
      for (int id : new int[] {63, 68}) {
        test("Escape saves new and reopened sign " + id + "; Done still works", () -> {
          int x = (int) mc.player.x, y = 101, z = (int) mc.player.z;
          mc.world.method_200(x, y, z, 0);
          mc.world.method_200(x, y - 1, z, 1);
          mc.world.method_200(x, y, z, id);
          var sign = (SignBlockEntity) mc.world.method_1777(x, y, z);
          allowEditing.value = new JsonPrimitive(false);
          session.preview(true);
          mc.player.method_489(sign);
          var input = (ScreenInput) mc.currentScreen;
          String[] expected = {"first", "second", "third", "fourth"};
          for (int row = 0; row < expected.length; row++) {
            for (char c : expected[row].toCharArray()) input.power$key(c, 0);
            if (row < 3) input.power$key('\r', Keyboard.KEY_RETURN);
            check(mc.currentScreen instanceof SignEditScreen, "typing or Enter closed the editor");
          }
          input.power$key('\0', Keyboard.KEY_ESCAPE);
          checkClosedAndSaved(mc, sign, expected);

          allowEditing.value = new JsonPrimitive(true);
          session.preview(true);
          check(Block.BLOCKS[id].method_1608(mc.world, x, y, z, mc.player), "sign did not reopen");
          input = (ScreenInput) mc.currentScreen;
          input.power$key('!', Keyboard.KEY_1);
          expected[0] += "!";
          input.power$key('\0', Keyboard.KEY_ESCAPE);
          checkClosedAndSaved(mc, sign, expected);

          mc.player.method_489(sign);
          input = (ScreenInput) mc.currentScreen;
          input.power$key('?', Keyboard.KEY_SLASH);
          expected[0] += "?";
          var done = input.power$buttons().stream().filter(button -> button.id == 0).findFirst().orElseThrow();
          input.power$click(done.x + 4, done.y + 4, 0);
          checkClosedAndSaved(mc, sign, expected);

          mc.player.method_489(sign);
          ((ScreenInput) mc.currentScreen).power$key('\0', Keyboard.KEY_ESCAPE);
          checkClosedAndSaved(mc, sign, expected);
        });
      }
      test("Escape and Done each send one multiplayer update with all four lines", () -> {
        var sign = new SignBlockEntity();
        sign.x = 5; sign.y = 102; sign.z = 7;
        sign.world = mc.world;
        for (boolean escape : new boolean[] {true, false}) {
          packets.clear();
          capturePackets = true;
          mc.world.isRemote = true;
          try {
            mc.player.method_489(sign);
            var input = (ScreenInput) mc.currentScreen;
            input.power$key('x', Keyboard.KEY_X);
            if (escape) input.power$key('\0', Keyboard.KEY_ESCAPE);
            else {
              var done = input.power$buttons().stream().filter(button -> button.id == 0).findFirst().orElseThrow();
              input.power$click(done.x + 4, done.y + 4, 0);
            }
            check(mc.currentScreen == null && !Keyboard.areRepeatEventsEnabled(), "remote editor stayed open");
            check(packets.size() == 1, "expected one sign packet, got " + packets.size());
            var packet = packets.get(0);
            check(packet.x == 5 && packet.y == 102 && packet.z == 7, "wrong packet coordinates");
            check(Arrays.equals(packet.text, sign.texts), "packet text differs");
            var bytes = new java.io.ByteArrayOutputStream();
            packet.write(new java.io.DataOutputStream(bytes));
            var received = new net.minecraft.network.packet.play.UpdateSignPacket();
            received.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));
            check(Arrays.equals(received.text, sign.texts), "wire roundtrip changed text");
          } finally {
            mc.setScreen(null);
            mc.world.isRemote = false;
            capturePackets = false;
          }
        }
      });
    } finally {
      mc.setScreen(null);
      mc.player.field_161.field_2536 = sneaking;
      session.discard();
    }
    log("SIGN EDITING FAILURES " + failures);
  }

  private static void checkClosedAndSaved(Minecraft mc, SignBlockEntity sign, String[] expected) {
    check(mc.currentScreen == null, "editor stayed open");
    check(!Keyboard.areRepeatEventsEnabled(), "screen removal left key repeat enabled");
    check(Arrays.equals(sign.texts, expected), "sign text changed while closing");
    var nbt = new NbtCompound();
    sign.writeNbt(nbt);
    var restored = new SignBlockEntity();
    restored.readNbt(nbt);
    check(Arrays.equals(restored.texts, expected), "saved sign text differs");
  }
}
