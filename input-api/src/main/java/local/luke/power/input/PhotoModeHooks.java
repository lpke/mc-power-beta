package local.luke.power.input;

/** Mapping-independent bridge from the controls camera to the pack's shared map tools. */
public final class PhotoModeHooks {
  public interface Camera {
    boolean powerPhoto$ownsPointer(int x, int y);
    boolean powerPhoto$dragging();
    void powerPhoto$screenshot();
  }
  public interface Tools {
    default void init(Object screen, Object client) {}
    default void removed(Object screen) {}
    default boolean key(Object screen, char character, int key) { return false; }
    default boolean mouse(Object screen, int x, int y, int button, boolean down, int wheel) { return false; }
    default boolean blocked(Object screen) { return false; }
    default boolean controlsHidden(Object screen) { return false; }
    default void renderScene(Object screen, int x, int y) {}
    default void render(Object screen, int x, int y, float delta) {}
  }
  private static Tools tools = new Tools() {};
  private PhotoModeHooks() {}
  public static Tools tools() { return tools; }
  public static void register(Tools value) { tools = java.util.Objects.requireNonNull(value); }
}
