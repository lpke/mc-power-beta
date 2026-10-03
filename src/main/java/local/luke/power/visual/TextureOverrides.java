package local.luke.power.visual;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.PowerBeta;

/** Apply only selected tiles, preserving the selected pack's resolution and all other pixels. */
public final class TextureOverrides {
  private static final Map<Object, byte[]> TERRAIN = new WeakHashMap<>();
  public static synchronized void clear() { TERRAIN.clear(); }

  public static synchronized InputStream replace(Object pack, String path, InputStream original) {
    VisualSettings s = VisualConfig.current();
    String weather = path.equals("/environment/rain.png") && s.softRain ? "rain"
        : path.equals("/environment/snow.png") && s.softSnow ? "snow" : null;
    if (weather != null) {
      InputStream replacement = TextureOverrides.class.getResourceAsStream("/assets/powerbeta/textures/" + weather + ".png");
      if (replacement != null) { close(original); return replacement; }
    }
    if (!path.equals("/terrain.png") || !(s.oldCobble || s.oldBricks) || original == null) return original;
    byte[] bytes = null;
    try {
      byte[] cached = TERRAIN.get(pack);
      if (cached != null) { close(original); return new ByteArrayInputStream(cached); }
      try (original) { bytes = original.readAllBytes(); }
      BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
      if (image == null || image.getWidth() % 16 != 0 || image.getHeight() % 16 != 0)
        return new ByteArrayInputStream(bytes);
      BufferedImage old;
      try (var in = TextureOverrides.class.getResourceAsStream("/assets/powerbeta/textures/old-terrain.png")) {
        old = ImageIO.read(in);
      }
      Graphics2D g = image.createGraphics();
      try {
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (s.oldCobble) tile(g, image, old, 0, 1);
        if (s.oldBricks) tile(g, image, old, 7, 0);
      } finally { g.dispose(); }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(image, "png", out);
      byte[] result = out.toByteArray();
      TERRAIN.put(pack, result);
      return new ByteArrayInputStream(result);
    } catch (Exception e) {
      PowerBeta.LOG.warn("Could not apply texture override; keeping selected pack", e);
      return bytes == null ? original : new ByteArrayInputStream(bytes);
    }
  }

  private static void tile(Graphics2D g, BufferedImage to, BufferedImage from, int x, int y) {
    int w = to.getWidth() / 16, h = to.getHeight() / 16, a = from.getWidth() / 16, b = from.getHeight() / 16;
    g.drawImage(from, x * w, y * h, (x + 1) * w, (y + 1) * h,
        x * a, y * b, (x + 1) * a, (y + 1) * b, null);
  }
  private static void close(InputStream in) { if (in != null) try { in.close(); } catch (IOException ignored) {} }
}
