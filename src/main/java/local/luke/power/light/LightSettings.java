package local.luke.power.light;

/** Block light predicts darkness at night; combined light also includes the current sky. */
public final class LightSettings {
  public boolean enabled;
  public int radius = 12;
  public int verticalRange = 6;
  public int lightSource;
  public boolean spawnableOnly;
  public int greenFrom = 8;
  public String lowColor = "#FF5555";
  public String highColor = "#55FF55";
  public double textSize = .6;

  public void validate() {
    if (radius < 2 || radius > 24 || verticalRange < 1 || verticalRange > 16)
      throw new IllegalArgumentException("Overlay range must be 2–24 horizontally and 1–16 vertically");
    if (lightSource < 0 || lightSource > 1 || greenFrom < 0 || greenFrom > 16)
      throw new IllegalArgumentException("Invalid light source or threshold");
    if (!Double.isFinite(textSize) || textSize < .2 || textSize > .9)
      throw new IllegalArgumentException("Text size must be between 0.2 and 0.9 blocks");
    rgb(lowColor);
    rgb(highColor);
  }

  public static int rgb(String value) {
    if (value == null || !value.matches("#?[0-9a-fA-F]{6}"))
      throw new IllegalArgumentException("Use a six-digit RGB colour, such as #FF5555");
    return Integer.parseInt(value.startsWith("#") ? value.substring(1) : value, 16);
  }

  public int color(int level) { return rgb(level < greenFrom ? lowColor : highColor); }
}
