package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.Arrays;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.PowerOptionsScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Near-final native settings screenshot, without publishing an editor draft. */
final class Release448Visuals {
  static void capture(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && mc.options.musicVolume == 0 && mc.options.soundVolume == 0,
        "use a muted disposable world");
    PowerConfig.flushPending(); byte[] before = Files.readAllBytes(PowerConfig.path());
    var host = mc.currentScreen; var screen = new PowerOptionsScreen(host);
    int nativeScale = mc.options.guiScale;
    Class<?> options = Class.forName("local.luke.power.controls.util.ModOptions");
    var scaleField = options.getField("guiScale"); var realScaleField = options.getField("realGuiScale");
    float scale = scaleField.getFloat(null), realScale = realScaleField.getFloat(null);
    int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      mc.options.guiScale = 2; scaleField.setFloat(null, .25f); realScaleField.setFloat(null, .25f);
      mc.currentScreen = screen; screen.init(mc, 640, 360);
      UiChecks.field(screen, "page", "Audio"); UiChecks.search(screen, "Player fire extinguish");
      Path file = Path.of("power-beta-data/reports/release-448/fire-extinguish.png");
      Files.createDirectories(file.getParent()); SettingsSnapshot.render(mc, screen, 640, 360, file);
      log("RELEASE448 settings capture " + file);
    } finally {
      screen.removed(); mc.currentScreen = host;
      mc.options.guiScale = nativeScale; scaleField.setFloat(null, scale); realScaleField.setFloat(null, realScale);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glMatrixMode(matrixMode); GL11.glPopAttrib();
    }
    check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "capture changed saved preferences");
  }
}
