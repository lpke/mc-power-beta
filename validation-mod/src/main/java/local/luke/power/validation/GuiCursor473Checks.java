package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Adds pending rendered frames to the older cursor event-order regression. */
public final class GuiCursor473Checks {
  private static int[] chestSite;
  private GuiCursor473Checks() {}

  static void run(Minecraft mc, String command) throws Exception {
    if (command.startsWith("prepare ")) {
      GuiCursor472Checks.run(mc, "helper " + command.substring(8));
      restoreChest(mc);
      sample(mc); return;
    }
    if (command.equals("chest-prepare")) {
      prepareChest(mc);
      double dy = chestSite[1] + .5 - (mc.player.y + local.luke.power.input.InteractionEye.offset(mc.player, 1, 0));
      double dx = chestSite[0] + .5 - mc.player.x, dz = chestSite[2] + .5 - mc.player.z;
      mc.player.yaw = mc.player.prevYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
      mc.player.pitch = mc.player.prevPitch = (float)-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
      sample(mc); return;
    }
    if (command.equals("state")) { sample(mc); return; }
    if (command.equals("finish")) { GuiCursor472Checks.run(mc, "finish"); restoreChest(mc); return; }
    check(command.startsWith("load "), "use prepare native|raw, load LAYERS inventory|pause|chat, state or finish");
    String[] args = command.substring(5).split(" ");
    boolean gate = false, finish = false;
    int offset = 0;
    while (offset < args.length && !args[offset].matches("[0-9]+")) {
      switch (args[offset++]) {
        case "gate" -> gate = true;
        case "finish" -> finish = true;
        default -> throw new IllegalArgumentException("unknown load option");
      }
    }
    int layers = Integer.parseInt(args[offset]);
    check(layers >= 0 && layers <= 4096 && mc.currentScreen == null, "use 0..4096 layers from gameplay");
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glLoadIdentity();
    GL11.glOrtho(0, 1, 0, 1, -1, 1);
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix(); GL11.glLoadIdentity();
    try {
      GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
      GL11.glBegin(GL11.GL_QUADS);
      for (int i = 0; i < layers; i++) {
        GL11.glColor4f((i & 1) == 0 ? .2f : .8f, .25f, .45f, .3f);
        GL11.glVertex2f(0, 0); GL11.glVertex2f(1, 0); GL11.glVertex2f(1, 1); GL11.glVertex2f(0, 1);
      }
      GL11.glEnd(); GL11.glFlush(); Display.swapBuffers();
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(mode);
    }
    if (gate) {
      Path ack = Path.of("power-beta-gui-cursor-473.release");
      Files.deleteIfExists(ack);
      Files.writeString(Path.of("power-beta-gui-cursor-473.gate"), "submitted");
      long deadline = System.nanoTime() + 5_000_000_000L;
      while (!Files.exists(ack) && System.nanoTime() < deadline) Thread.sleep(1);
      check(Files.exists(ack), "compositor gate acknowledgement missing");
      Files.delete(ack);
    }
    if (finish) GL11.glFinish();
    if (args[offset + 1].equals("chest")) {
      prepareChest(mc);
      check(net.minecraft.block.Block.BLOCKS[54].method_1608(mc.world, chestSite[0], chestSite[1], chestSite[2], mc.player), "native chest activation failed");
      check(mc.currentScreen instanceof net.minecraft.class_264, "native chest screen did not open");
    } else mc.setScreen(switch (args[offset + 1]) {
      case "inventory" -> new net.minecraft.class_585(mc.player);
      case "pause" -> new net.minecraft.class_525();
      case "chat" -> new net.minecraft.client.gui.screen.ChatScreen();
      default -> throw new IllegalArgumentException(args[1]);
    });
    if (gate) Files.writeString(Path.of("power-beta-gui-cursor-473.gate"), "released");
    sample(mc);
  }

  private static void sample(Minecraft mc) throws Exception {
    GuiCursor472Checks.run(mc, "state");
    JsonObject value = new JsonParser().parse(Files.readString(Path.of("power-beta-gui-cursor-472.json"))).getAsJsonObject();
    value.addProperty("vendor", GL11.glGetString(GL11.GL_VENDOR));
    value.addProperty("renderer", GL11.glGetString(GL11.GL_RENDERER));
    value.addProperty("nanoTime", System.nanoTime());
    Files.writeString(Path.of("power-beta-gui-cursor-473.json"), value.toString());
    log("CURSOR473 " + value);
  }

  private static void prepareChest(Minecraft mc) throws Exception {
    check(chestSite == null && mc.world != null && mc.player != null && mc.currentScreen == null, "prepare one chest from gameplay");
    GuiCursor472Checks.run(mc, "state");
    int x = (int)Math.floor(mc.player.x) + 2, y = (int)Math.floor(mc.player.boundingBox.minY), z = (int)Math.floor(mc.player.z);
    check(y > 0 && y < 126 && mc.world.method_155(x - 1, y, z - 1, x + 1, y + 2, z + 1), "keep the chest fixture in loaded terrain");
    check(mc.world.method_1777(x, y, z) == null && mc.world.method_1777(x, y + 1, z) == null, "chest fixture overlaps a block entity");
    chestSite = new int[] {x, y, z, mc.world.getBlockId(x, y, z), mc.world.method_1778(x, y, z),
        mc.world.getBlockId(x, y + 1, z), mc.world.method_1778(x, y + 1, z)};
    mc.world.method_154(x, y + 1, z, 0, 0);
    mc.world.method_154(x, y, z, 54, 0);
    var settings = local.luke.power.building.config.Config.current().copy();
    settings.flexible.placeAgainstContainers = false;
    local.luke.power.building.config.Config.preview(settings);
  }

  private static void restoreChest(Minecraft mc) {
    if (chestSite == null) return;
    var site = chestSite; chestSite = null;
    mc.world.method_154(site[0], site[1], site[2], site[3], site[4]);
    mc.world.method_154(site[0], site[1] + 1, site[2], site[5], site[6]);
  }
}
