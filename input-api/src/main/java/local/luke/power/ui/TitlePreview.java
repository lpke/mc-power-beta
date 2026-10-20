package local.luke.power.ui;

import java.util.Map;
import java.util.function.Supplier;

/** Native title renderer supplied across the pack's independent mapping sets. */
public final class TitlePreview {
  public interface Renderer {
    void tick();
    default void replay() {}
    default boolean settled() { return false; }
    void draw(int x, int y, int width, int height, int screenWidth, int screenHeight,
        String[] lines, Map<Character, Integer> blocks, float[] rotation,
        boolean inverted, boolean shadow, boolean animation, float speed, float delta);
  }
  public static Supplier<Renderer> factory;
  public static java.util.function.IntPredicate renderableBlock = id -> false;
  public interface HitTest { boolean contains(int x, int y, int width); }
  public static HitTest hitTest = (x, y, width) -> false;
  private static long selectionRevision;
  public static long selectionRevision() { return selectionRevision; }
  public static void selectionChanged() { selectionRevision++; }
  private TitlePreview() {}
}
