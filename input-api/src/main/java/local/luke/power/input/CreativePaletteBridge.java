package local.luke.power.input;

/** The root UI supplies the shared selectable text editor across Minecraft mapping sets. */
public final class CreativePaletteBridge {
  public interface Search {
    String text();
    boolean focused();
    void render(int width, int height, int left, int top, int span, int mouseX, int mouseY);
    boolean press(int mouseX, int mouseY, int button);
    boolean key(char character, int key);
    void focus();
    void close();
  }
  public interface Factory { Search create(Object minecraft); }
  public interface Deletion { boolean destroy(Object minecraft, boolean all); }
  private static Factory factory;
  private static Deletion deletion;
  private CreativePaletteBridge() {}
  public static void register(Factory value) { factory = value; }
  public static void registerDeletion(Deletion value) { deletion = value; }
  public static boolean destroy(Object minecraft, boolean all) {
    if (deletion == null) try { Class.forName("local.luke.power.inventory.CreativeDeletion"); }
    catch (ClassNotFoundException unavailable) { return false; }
    return deletion != null && deletion.destroy(minecraft, all);
  }
  public static Search create(Object minecraft) {
    if (minecraft == null) return null;
    if (factory == null) try { Class.forName("local.luke.power.ui.CreativeSearchInput"); }
    catch (ClassNotFoundException ignored) { return null; }
    return factory == null ? null : factory.create(minecraft);
  }
}
