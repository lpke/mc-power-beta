package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import local.luke.power.config.ConfigSession;
import local.luke.power.config.SettingsRegistry;
import net.minecraft.client.Minecraft;

/** Explicit controls for disposable, real-world performance comparisons. Never saves preferences. */
public final class Performance421Controls {
  private static ConfigSession preview;
  private static double[] pose;
  private static net.minecraft.item.ItemStack held;
  private static int heldSlot = -1;
  private Performance421Controls() {}

  public static void run(Minecraft mc, String command) throws Exception {
    String[] args = command.split(" ", 3);
    switch (args[0]) {
      case "load" -> {
        check(mc.world == null, "return to title before loading a benchmark save");
        String folder = command.substring(5);
        check(!folder.contains("/") && !folder.contains("\\") && !folder.equals("..")
            && Files.isRegularFile(Path.of("saves", folder, "level.dat")), "existing cloned save required");
        mc.interactionManager = new net.minecraft.SingleplayerInteractionManager(mc);
        mc.method_2120(folder, folder, 0L);
        mc.setScreen(null);
        net.minecraft.client.render.entity.EntityRenderDispatcher.field_2489.method_1917(
            mc.world, mc.textureManager, mc.textRenderer, mc.player, mc.options, 0F);
      }
      case "preview" -> {
        if (preview == null) preview = SettingsRegistry.open(mc);
        var setting = find(preview, args[1]);
        setting.value = JsonParser.parseString(args[2]);
        preview.link(setting);
        preview.preview();
        log("PERFORMANCE 421 preview " + setting.id + "=" + setting.value);
      }
      case "restore" -> {
        if (preview != null) preview.discard();
        preview = null;
      }
      case "map" -> mc.setScreen(new local.luke.power.ui.WorldMapScreen());
      case "resume" -> mc.setScreen(null);
      case "console" -> ChatChecks.submit(mc, command.substring(8));
      case "held" -> {
        int id = Integer.parseInt(args[1]);
        if (id < 0) {
          check(heldSlot >= 0, "no held-item preview");
          mc.player.inventory.main[heldSlot] = held;
          held = null; heldSlot = -1;
        } else {
          check(id < net.minecraft.Item.ITEMS.length && net.minecraft.Item.ITEMS[id] != null, "unknown item");
          if (heldSlot < 0) { heldSlot = mc.player.inventory.selectedSlot; held = mc.player.inventory.main[heldSlot]; }
          check(heldSlot == mc.player.inventory.selectedSlot, "held preview slot changed");
          mc.player.inventory.main[heldSlot] = new net.minecraft.item.ItemStack(id, 1, 0);
        }
        mc.player.inventory.markDirty();
      }
      case "remember" -> pose = new double[]{mc.player.x, mc.player.y, mc.player.z, mc.player.yaw, mc.player.pitch};
      case "return" -> { check(pose != null, "remember a pose first"); position(mc, pose); }
      case "position" -> {
        String[] fields = command.substring(9).split(" ");
        check(fields.length == 5, "position X Y Z YAW PITCH");
        double[] values = new double[5];
        for (int i = 0; i < 5; i++) { values[i] = Double.parseDouble(fields[i]); check(Double.isFinite(values[i]), "finite pose required"); }
        position(mc, values);
      }
      case "autosave" -> {
        // The production option is applied at world construction. Explicit live test override.
        var field = net.minecraft.world.World.class.getDeclaredField("field_212");
        field.setAccessible(true); field.setInt(mc.world, Integer.parseInt(args[1]) * 20);
      }
      case "vbo" -> {
        Object pool = mc.worldRenderer.getClass().getMethod("smoothbeta_getTerrainVboPool").invoke(mc.worldRenderer);
        log("PERFORMANCE 421 VBO capacity=" + UiChecks.field(pool, "capacity") + " next=" + UiChecks.field(pool, "nextPos")
            + " live=" + UiChecks.field(pool, "size") + " stride=" + UiChecks.field(pool, "vertexBytes")
            + " commandBytes=" + ((java.nio.IntBuffer)UiChecks.field(pool, "bufferIndirect")).capacity() * 4);
      }
      case "state" -> {
        log("PERFORMANCE 421 fullBright=" + local.luke.power.render.RenderControls.fullBright()
            + " heldEmission=" + local.luke.power.render.HeldLighting.emission());
        log("PERFORMANCE 421 window=" + mc.displayWidth + "x" + mc.displayHeight
            + " screen=" + mc.currentScreen + " active=" + org.lwjgl.opengl.Display.isActive());
        if (mc.player != null) log("PERFORMANCE 421 pose=" + mc.player.x + "," + mc.player.y
            + "," + mc.player.z + " yaw=" + mc.player.yaw + " pitch=" + mc.player.pitch);
      }
      default -> throw new IllegalArgumentException("load FOLDER | preview ID JSON | restore | map | resume | held ID|-1 | state | vbo | remember | return | position X Y Z YAW PITCH | autosave SECONDS | console COMMAND");
    }
  }
  private static void position(Minecraft mc, double[] values) {
    check(mc.player != null, "open a cloned world");
    mc.player.method_1340(values[0], values[1], values[2]);
    mc.player.prevX = values[0]; mc.player.prevY = values[1]; mc.player.prevZ = values[2];
    mc.player.prevYaw = mc.player.yaw = (float)values[3];
    mc.player.prevPitch = mc.player.pitch = (float)values[4];
    mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
  }
}
