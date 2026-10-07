package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.config.*;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Captures real menu rendering and its complete row order in a disposable client. */
final class SettingsSnapshot {
  static void write(Minecraft mc, String name) throws Exception {
    if (!name.matches("[a-z0-9-]+")) throw new IllegalArgumentException("Invalid snapshot name");
    Path dir = Path.of("power-beta-data/reports/settings-review", name);
    Files.createDirectories(dir);
    ConfigAudit.write(mc);
    var screen = new PowerOptionsScreen(mc.currentScreen);
    screen.init(mc, 640, 420);
    field(screen, "showDisabled", true);
    ((Set<?>)field(screen, "collapsed")).clear();
    ((TextInput)field(screen, "search")).setText("");
    field(screen, "relatedIds", List.of());
    field(screen, "conflictIds", List.of());
    field(screen, "changedOnly", false);
    field(screen, "libraryOpen", false);
    Files.writeString(dir.resolve("catalog.json"), Catalog.JSON.toJson(screen.session().settings()));
    var pages = new ArrayList<Map<String, Object>>();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      for (String page : PowerOptionsScreen.PAGES) {
        field(screen, "page", page);
        var record = new LinkedHashMap<String, Object>();
        record.put("page", page);
        var shots = new ArrayList<Map<String, Object>>();
        for (int[] size : new int[][] {{640, 420}, {320, 240}}) {
          screen.init(mc, size[0], size[1]);
          field(screen, "scroll", 0d); call(screen, "layout");
          List<?> rows = (List<?>)field(screen, "rows");
          if (size[0] == 640) {
            var order = new ArrayList<Map<String, Object>>();
            for (Object row : rows) {
              Setting setting = (Setting)call(row, "setting");
              var entry = new LinkedHashMap<String, Object>();
              entry.put("group", call(row, "group"));
              entry.put("id", setting == null ? null : setting.id);
              order.add(entry);
            }
            record.put("rows", order);
          }
          int viewport = (int)call(screen, "bottom") - (int)call(screen, "top");
          int content = (int)call(screen, "contentHeight");
          int maxScroll = Math.max(0, content - viewport);
          int index = 0;
          for (int offset = 0;; offset = Math.min(maxScroll, offset + Math.max(24, viewport - 48))) {
            field(screen, "scroll", (double)offset); call(screen, "layout");
            String filename = page.toLowerCase(Locale.ROOT).replace(' ', '-') + "-" + size[0] + "-" + index++ + ".png";
            render(mc, screen, size[0], size[1], dir.resolve(filename));
            shots.add(Map.of("file", filename, "width", size[0], "height", size[1], "scroll", field(screen, "scroll")));
            if (offset == maxScroll || size[0] == 320) break;
          }
        }
        record.put("screenshots", shots); pages.add(record);
      }
      field(screen, "page", "Audio"); field(screen, "libraryOpen", true);
      var library = (MusicLibraryScreen)field(screen, "library");
      library.restore(new MusicLibraryScreen.State(false, "", "", 0, 0));
      library.showTracks();
      ((Set<?>)field(library, "collapsed")).clear(); call(library, "rebuildRows");
      var libraryShots = new ArrayList<Map<String, Object>>();
      for (int[] size : new int[][] {{640, 420}, {320, 240}}) {
        screen.init(mc, size[0], size[1]);
        int viewport = (int)call(library, "bottom") - (int)call(library, "listTop");
        int maxScroll = Math.max(0, (int)field(library, "contentHeight") - viewport);
        int index = 0;
        for (int offset = 0;; offset = Math.min(maxScroll, offset + Math.max(24, viewport - 48))) {
          field(library, "trackScroll", offset);
          String filename = "audio-library-" + size[0] + "-" + index++ + ".png";
          render(mc, screen, size[0], size[1], dir.resolve(filename));
          libraryShots.add(Map.of("file", filename, "width", size[0], "height", size[1], "scroll", offset));
          if (offset == maxScroll || size[0] == 320) break;
        }
      }
      pages.stream().filter(p -> p.get("page").equals("Audio")).findFirst().orElseThrow().put("libraryScreenshots", libraryShots);
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib();
      mc.setScreen(mc.currentScreen);
    }
    Files.writeString(dir.resolve("pages.json"), Catalog.JSON.toJson(pages));
    Validation.log("SETTINGS SNAPSHOT " + name + " " + screen.session().settings().size() + " settings, " + pages.size() + " tabs");
  }

  static void render(Minecraft mc, net.minecraft.client.gui.screen.Screen screen, int width, int height, Path file) throws Exception {
    int pixelsWide = width * 2, pixelsHigh = height * 2;
    Validation.check(org.lwjgl.opengl.Display.getWidth() >= pixelsWide
        && org.lwjgl.opengl.Display.getHeight() >= pixelsHigh, "Resize the disposable client before capture");
    int oldWidth = mc.displayWidth, oldHeight = mc.displayHeight;
    mc.displayWidth = pixelsWide; mc.displayHeight = pixelsHigh;
    try {
    GL11.glViewport(0, 0, pixelsWide, pixelsHigh);
    GL11.glDisable(GL11.GL_SCISSOR_TEST);
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity();
    GL11.glOrtho(0, width, height, 0, 1000, 3000);
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
    GL11.glTranslatef(0, 0, -2000);
    GL11.glColor4f(1,1,1,1);
    GL11.glDisable(GL11.GL_LIGHTING);
    screen.render(-1, -1, 0);
    GL11.glFinish();
    ByteBuffer pixels = BufferUtils.createByteBuffer(pixelsWide * pixelsHigh * 4);
    GL11.glReadPixels(0, 0, pixelsWide, pixelsHigh, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
    var image = new BufferedImage(pixelsWide, pixelsHigh, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < pixelsHigh; y++) for (int x = 0; x < pixelsWide; x++) {
      int at = ((pixelsHigh - y - 1) * pixelsWide + x) * 4;
      image.setRGB(x, y, (pixels.get(at) & 255) << 16 | (pixels.get(at + 1) & 255) << 8 | pixels.get(at + 2) & 255);
    }
    ImageIO.write(image, "png", file.toFile());
    Validation.check(GL11.glGetError() == GL11.GL_NO_ERROR, "Screenshot OpenGL error: " + file);
    } finally {
      mc.displayWidth = oldWidth; mc.displayHeight = oldHeight;
    }
  }
}
