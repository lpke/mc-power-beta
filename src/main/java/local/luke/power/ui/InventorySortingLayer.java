package local.luke.power.ui;

import local.luke.power.config.ConfigSession;
import local.luke.power.input.*;
import local.luke.power.inventory.tools.InventoryToolsBackend;
import local.luke.power.offhand.OffhandInput;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Routes settings pages over one native container without closing or replacing it. */
public final class InventorySortingLayer {
  public static final String OWNER = "powerbeta.sortingLayout";
  private final Minecraft mc;
  private final ContainerScreen host;
  private final Object player, world, container;
  private final boolean repeat;
  private UiScreen page;
  private boolean active = true;
  public static boolean available(Minecraft mc) {
    return OffhandInput.container(mc) && !InventoryPanels.modal(mc.currentScreen)
        && !InventoryTextInput.active(mc.currentScreen) && PowerConfig.problem().isEmpty();
  }
  public InventorySortingLayer(Minecraft mc, ContainerScreen host) throws ReflectiveOperationException {
    if (!available(mc)) throw new IllegalStateException("Sorting layout is unavailable");
    this.mc = mc; this.host = host; player = mc.player; world = mc.world; container = host.container;
    repeat = Keyboard.areRepeatEventsEnabled();
    var session = new ConfigSession(); InventoryToolsBackend.register(session);
    var editor = new SortingPreferencesScreen(host, session, () -> {}, true);
    navigate(editor);
    InventoryPanels.extra(host, OWNER, java.util.List.of(new InventoryPanels.Rect(0, 0, host.width, host.height)));
    InventoryPanels.modal(host, OWNER, true); InventoryTextInput.focus(host, true);
  }
  public boolean active() { return active; }
  public void prepare() {
    if (mc.currentScreen != host || mc.player != player || mc.world != world || host.container != container
        || mc.player == null || mc.player.container != container || mc.player.health <= 0 || mc.player.dead
        || DetachedCamera.isActive() || InteractionState.carryingContainer
        || mc.player instanceof PlayerMode mode && !mode.power$inventoryEditable()) close();
  }
  private void navigate(Screen next) {
    TextDrag.cancel();
    if (next == host) { close(); return; }
    if (!(next instanceof UiScreen target)) throw new IllegalArgumentException("Unsupported settings page");
    if (page != null) page.removed();
    page = target; page.layerNavigation(this::navigate); page.init(mc, host.width, host.height);
  }
  public void press(int x, int y, int button) { if (active && org.lwjgl.opengl.Display.isActive()) page.layerPress(x, y, button); }
  public void release(int x, int y, int button) { if (active) page.layerRelease(x, y, button); }
  public void key(char character, int key) { if (active && org.lwjgl.opengl.Display.isActive()) page.layerKey(character, key); }
  public void wheel(int delta) { if (active && org.lwjgl.opengl.Display.isActive()) page.layerWheel(delta); }
  public void render(int x, int y, float delta) {
    if (!active) return;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
    try {
      GL11.glTranslatef(0, 0, 300); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING);
      page.render(x, y, delta);
    } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
  }
  public void close() {
    if (!active) return;
    active = false;
    if (page != null) page.removed();
    TextDrag.cancel(); Keyboard.enableRepeatEvents(repeat);
    InventoryPanels.removeExtra(host, OWNER); InventoryPanels.modal(host, OWNER, false);
    if (!InventoryPanels.modal(host)) InventoryTextInput.focus(host, false);
  }
}
