package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.*;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;

final class CreativeBorderChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    var inventory = mc.player.inventory.main.clone();
    var cursor = mc.player.inventory.getCursorStack();
    try {
      CreativeVehicleChecks.mode(mc, "CREATIVE");
      java.util.Arrays.fill(mc.player.inventory.main, null); mc.player.inventory.setCursorStack(null);
      var screen = new class_585(mc.player); mc.setScreen(screen);
      field(screen, "creative_normalGUI", false); field(screen, "creative_items", List.of());
      Path dir = Path.of("power-beta-data/reports/creative-borders"); Files.createDirectories(dir);
      for (int[] size : new int[][] {{320, 240}, {640, 420}}) test("creative border pixels at " + size[0], () -> {
        screen.init(mc, size[0], size[1]);
        Path file = dir.resolve("creative-" + size[0] + ".png");
        SettingsSnapshot.render(mc, screen, size[0], size[1], file);
        var image = ImageIO.read(file.toFile());
        int x = (size[0] - 176) / 2, y = (size[1] - 166) / 2;
        for (int col = 7; col < 151; col++)
          for (int row = 0; row < 18; row++)
            check(image.getRGB((x + col) * 2, (y + 101 + row) * 2)
                == image.getRGB((x + col) * 2, (y + 119 + row) * 2), "final row differs at " + col + "/" + row);
        for (int col = 1; col < 4; col++)
          check((image.getRGB((x + col) * 2, (y + 137) * 2) & 0xffffff) != 0, "black seam at " + col);
        for (int row = 138; row < 140; row++) {
          check((image.getRGB((x + 2) * 2, (y + row) * 2) & 0xffffff) == 0xffffff, "white edge disconnected");
          check((image.getRGB((x + 3) * 2, (y + row) * 2) & 0xffffff) == 0xc6c6c6, "white edge extends too far");
        }
      });
    } finally {
      System.arraycopy(inventory, 0, mc.player.inventory.main, 0, inventory.length);
      mc.player.inventory.setCursorStack(cursor); mc.setScreen(null); CreativeVehicleChecks.mode(mc, mode);
    }
    log("CREATIVE BORDER FAILURES " + failures);
  }
}
