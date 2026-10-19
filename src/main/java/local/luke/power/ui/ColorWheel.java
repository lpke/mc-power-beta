package local.luke.power.ui;

/** HSV conversion and wheel coordinates shared by drawing and pointer input. */
final class ColorWheel {
  private ColorWheel() {}

  record Hsv(double hue, double saturation, double brightness) {
    Hsv {
      if (!Double.isFinite(hue) || !Double.isFinite(saturation) || !Double.isFinite(brightness))
        throw new IllegalArgumentException("Invalid HSV color");
      hue -= Math.floor(hue);
      saturation = clamp(saturation);
      brightness = clamp(brightness);
    }

    Hsv brightness(double value) { return new Hsv(hue, saturation, value); }

    int rgb() {
      double sector = hue * 6, chroma = brightness * saturation;
      double secondary = chroma * (1 - Math.abs(sector % 2 - 1));
      double base = brightness - chroma;
      double r, g, b;
      switch ((int)sector) {
        case 0 -> { r = chroma; g = secondary; b = 0; }
        case 1 -> { r = secondary; g = chroma; b = 0; }
        case 2 -> { r = 0; g = chroma; b = secondary; }
        case 3 -> { r = 0; g = secondary; b = chroma; }
        case 4 -> { r = secondary; g = 0; b = chroma; }
        default -> { r = chroma; g = 0; b = secondary; }
      }
      return (int)Math.round((r + base) * 255) << 16
          | (int)Math.round((g + base) * 255) << 8
          | (int)Math.round((b + base) * 255);
    }
  }

  static Hsv fromRgb(int rgb, Hsv previous) {
    double r = (rgb >> 16 & 255) / 255d, g = (rgb >> 8 & 255) / 255d, b = (rgb & 255) / 255d;
    double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), delta = max - min;
    double hue = previous == null ? 0 : previous.hue;
    if (delta > 0) hue = (max == r ? (g - b) / delta : max == g ? (b - r) / delta + 2 : (r - g) / delta + 4) / 6;
    // Black has no measurable hue or saturation. Retain them so raising brightness restores the color.
    double saturation = max == 0 ? previous == null ? 0 : previous.saturation : delta / max;
    return new Hsv(hue, saturation, max);
  }

  record Point(double x, double y) {}

  record Bounds(double x, double y, double radius) {
    Bounds {
      if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(radius) || radius <= 0)
        throw new IllegalArgumentException("Invalid wheel bounds");
    }

    boolean contains(double pointerX, double pointerY) {
      double dx = pointerX - x, dy = pointerY - y;
      return dx * dx + dy * dy <= radius * radius;
    }

    Hsv select(Hsv current, double pointerX, double pointerY) {
      double dx = pointerX - x, dy = pointerY - y, distance = Math.hypot(dx, dy);
      double hue = distance == 0 ? current.hue : Math.atan2(dy, dx) / (2 * Math.PI);
      return new Hsv(hue, distance / radius, current.brightness);
    }

    Point marker(Hsv current) {
      double angle = current.hue * 2 * Math.PI, distance = current.saturation * radius;
      return new Point(x + Math.cos(angle) * distance, y + Math.sin(angle) * distance);
    }
  }

  private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
}
