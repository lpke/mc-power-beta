package local.luke.power.map;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** One explicit readback of the visible map; PNG encoding and durable writes use the map worker. */
public final class MapScreenshot {
  private static CompletableFuture<Path> writing;
  private static String status = "";
  private static long expires;
  private static boolean announce;
  private MapScreenshot() {}

  public static void clearStatus() { status = ""; expires = 0; announce = false; }
  private static void message(String text) { status = text; expires = System.nanoTime() + 4_000_000_000L; }
  public static String status() {
    if (writing != null && writing.isDone()) {
      try { Path file = writing.join(); if (announce) message("Screenshot saved: " + file.getFileName()); }
      catch (RuntimeException failure) { if (announce) message("Screenshot failed: " + MapCache.reason(failure)); }
      writing = null;
    }
    return System.nanoTime() < expires ? status : "";
  }

  public static void capture(Minecraft mc) {
    if (writing != null) { message("A screenshot is already being saved"); return; }
    int width = mc.displayWidth, height = mc.displayHeight;
    if (width < 1 || height < 1 || (long) width * height > 16_777_216) {
      message("Screenshot exceeds the 64 MiB capture limit"); return;
    }
    ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
    GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
    try {
      GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
      GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
      GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
      GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
      GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
    } finally { GL11.glPopClientAttrib(); }
    Path output = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir()
        .resolve("screenshots/map-" + System.currentTimeMillis() + "-" + UUID.randomUUID() + ".png");
    writing = MapIO.submit(() -> {
      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
        int offset = ((height - y - 1) * width + x) * 4;
        image.setRGB(x, y, (pixels.get(offset) & 255) << 16
            | (pixels.get(offset + 1) & 255) << 8 | pixels.get(offset + 2) & 255);
      }
      var bytes = new ByteArrayOutputStream();
      if (!ImageIO.write(image, "PNG", bytes)) throw new java.io.IOException("PNG encoder unavailable");
      MapFiles.replace(output, bytes.toByteArray());
      return output;
    });
    announce = true;
    message(writing == null ? "Map storage is busy; try again" : "Saving screenshot...");
  }
}
