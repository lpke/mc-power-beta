package local.luke.power.ui;

/** Keeps the original palette indices readable while allowing every RGB value. */
public final class ColorValue {
  public static final int CUSTOM = 0x1000000;
  public enum Palette {
    FLEXIBLE(0x487BFF, 0x33DDEE, 0xFFA040, 0x55DD77),
    WORLD_EDIT(0x66DDFF, 0x66FF88, 0xFFA040, 0xDD88FF);

    private final int[] colors;
    Palette(int... colors) { this.colors = colors; }

    public int rgb(int stored) {
      validate(stored);
      return stored < colors.length ? colors[stored] : stored & 0xffffff;
    }

    public int encode(int rgb) {
      if (rgb < 0 || rgb > 0xffffff) throw new IllegalArgumentException("Invalid RGB color");
      for (int i = 0; i < colors.length; i++) if (colors[i] == rgb) return i;
      return CUSTOM | rgb;
    }

    public void validate(int stored) {
      if (stored < 0 || stored >= colors.length && (stored < CUSTOM || stored > 0x1ffffff))
        throw new IllegalArgumentException("Invalid palette or RGB color");
    }
  }

  private ColorValue() {}
}
