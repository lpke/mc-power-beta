package local.luke.power.commands.util;

/** Parsers reject incomplete, non-finite and excessive values before any world mutation. */
public final class CommandNumbers {
  private CommandNumbers() {}

  public static int integer(String text, int min, int max, String label) {
    try {
      int value = Integer.parseInt(text);
      if (value >= min && value <= max) return value;
    } catch (NumberFormatException ignored) {
    }
    throw new IllegalArgumentException(label + " must be from " + min + " to " + max + ".");
  }

  public static double number(String text) {
    try {
      double value = Double.parseDouble(text);
      if (Double.isFinite(value)) return value;
    } catch (NumberFormatException ignored) {
    }
    throw new IllegalArgumentException("Invalid number: " + text);
  }

  public static boolean coordinate(String text) {
    if (text.startsWith("~") || text.startsWith("^")) return true;
    try {
      number(text);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public static double relative(String text, double base) {
    return text.startsWith("~")
        ? base + (text.length() == 1 ? 0 : number(text.substring(1)))
        : number(text);
  }

  public static double[] position(
      double x, double y, double z, float yaw, float pitch, String[] args, int start) {
    if (start + 3 > args.length)
      throw new IllegalArgumentException("Specify all three coordinates: x y z.");
    String a = args[start], b = args[start + 1], c = args[start + 2];
    double[] result;
    if (a.startsWith("^") || b.startsWith("^") || c.startsWith("^")) {
      if (!a.startsWith("^") || !b.startsWith("^") || !c.startsWith("^"))
        throw new IllegalArgumentException("Use ^ for all three local coordinates.");
      double left = local(a), up = local(b), forward = local(c);
      double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
      result =
          new double[] {
            x
                + left * Math.cos(yr)
                - up * Math.sin(yr) * Math.sin(pr)
                - forward * Math.sin(yr) * Math.cos(pr),
            y + up * Math.cos(pr) - forward * Math.sin(pr),
            z
                + left * Math.sin(yr)
                + up * Math.cos(yr) * Math.sin(pr)
                + forward * Math.cos(yr) * Math.cos(pr)
          };
    } else
      result = new double[] {absolute(a, x, true), absolute(b, y, false), absolute(c, z, true)};
    if (!Double.isFinite(result[0])
        || !Double.isFinite(result[1])
        || !Double.isFinite(result[2])
        || Math.abs(result[0]) >= 30000000
        || Math.abs(result[2]) >= 30000000
        || Math.abs(result[1]) > 4096)
      throw new IllegalArgumentException(
          "Position is outside safe Beta bounds: X/Z ±30,000,000; Y ±4,096.");
    return result;
  }

  private static double local(String text) {
    return text.length() == 1 ? 0 : number(text.substring(1));
  }

  private static double absolute(String text, double base, boolean center) {
    return relative(text, base) + (center && text.matches("[+-]?[0-9]+") ? .5 : 0);
  }

  public static int ticks(String text, int defaultMultiplier) {
    int multiplier = defaultMultiplier;
    if (text.endsWith("t") || text.endsWith("s") || text.endsWith("d")) {
      multiplier =
          switch (text.charAt(text.length() - 1)) {
            case 's' -> 20;
            case 'd' -> 24000;
            default -> 1;
          };
      text = text.substring(0, text.length() - 1);
    }
    double result = number(text) * multiplier;
    if (!Double.isFinite(result) || result < 0 || result > Integer.MAX_VALUE)
      throw new IllegalArgumentException("Duration is outside the supported range.");
    return (int) Math.round(result);
  }
}
