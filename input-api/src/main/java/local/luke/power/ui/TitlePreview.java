package local.luke.power.ui;

import java.util.Map;
import java.util.function.Supplier;

/** Native title renderer supplied across the pack's independent mapping sets. */
public final class TitlePreview {
  public interface Renderer {
    void tick();
    void draw(int x, int y, int width, int height, int screenWidth, int screenHeight,
        String[] lines, Map<Character, Integer> blocks, float[] rotation,
        boolean inverted, boolean shadow, boolean animation, float speed, float delta);
  }
  public static Supplier<Renderer> factory;
  private TitlePreview() {}
}
