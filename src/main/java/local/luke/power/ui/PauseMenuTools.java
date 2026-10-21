package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;
import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.tweaks.photomode.PhotoModeScreen;
import local.luke.power.map.MapKeys;
import local.luke.power.map.Maps;
import local.luke.power.mixin.ButtonBoundsAccessor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/** Pause-only item shortcuts share native UI sounds and the final relocated menu geometry. */
public final class PauseMenuTools extends UiScreen {
  private enum Tool { PHOTO, MAP, WAYPOINTS, RECIPES }
  private final ItemRenderer renderer = new ItemRenderer();
  private final ItemStack[] icons = {new ItemStack(321, 1, 0), new ItemStack(358, 1, 0), new ItemStack(345, 1, 0), new ItemStack(340, 1, 0)};
  private final List<Tool> included = new ArrayList<>();
  private Screen parent;
  private PauseToolsLayout.Box bounds;

  public void configure(Screen parent, List<?> buttons, boolean pause) {
    this.parent = pause ? parent : null;
    if (pause) buttons.removeIf(value -> ((ButtonWidget)value).id == 20);
  }
  private boolean available() {
    return parent != null && minecraft.world != null && minecraft.player != null
        && !minecraft.player.dead && minecraft.player.health > 0;
  }
  private void include() {
    included.clear();
    if (!available()) return;
    if (local.luke.power.recipes.RecipeBooks.pauseAvailable(minecraft)) included.add(Tool.RECIPES);
    if (ControlFeatures.USER_INTERFACE_CONFIG.photoModeConfig.enablePhotoModeButton) included.add(Tool.PHOTO);
    if (MenuPreferences.pauseMapButton() && MapKeys.available("powerbeta.map")) included.add(Tool.MAP);
    if (MenuPreferences.pauseWaypointsButton()) included.add(Tool.WAYPOINTS);
  }
  public int reserveAboveBack() {
    include();
    return !included.isEmpty() && MenuPreferences.pauseToolsPosition() == 0 ? PauseToolsLayout.STEP : 0;
  }
  public void layout(List<?> buttons, MenuMusicLayout music) {
    include(); bounds = null;
    if (included.isEmpty()) return;
    var obstacles = new ArrayList<PauseToolsLayout.Box>();
    obstacles.add(new PauseToolsLayout.Box(width / 2 - 100, 36, 200, 16));
    PauseToolsLayout.Box back = null;
    for (Object value : buttons) {
      ButtonWidget button = (ButtonWidget)value;
      if (!button.visible) continue;
      var dimensions = (ButtonBoundsAccessor)(Object)button;
      var box = new PauseToolsLayout.Box(button.x, button.y, dimensions.power$width(), dimensions.power$height());
      obstacles.add(box);
      if (button.id == 4) back = box;
    }
    if (back == null) return;
    if (music != null) obstacles.add(new PauseToolsLayout.Box(music.x(), music.y(), music.width(), music.height()));
    bounds = PauseToolsLayout.at(width, height, MenuPreferences.pauseToolsPosition(), included.size(), back, obstacles);
  }
  public boolean press(int x, int y, int button) {
    if (button != 0 || bounds == null || !available()) return false;
    for (int i = 0; i < included.size(); i++) {
      if (!inside(x, y, bounds.x() + i * PauseToolsLayout.STEP, bounds.y(), 20, 20)) continue;
      click();
      switch (included.get(i)) {
        case RECIPES -> local.luke.power.recipes.RecipeBooks.openViewer(minecraft, parent);
        case PHOTO -> minecraft.setScreen(new PhotoModeScreen(parent));
        case MAP -> minecraft.setScreen(new WorldMapScreen());
        case WAYPOINTS -> { Maps.ensureWaypoints(minecraft); minecraft.setScreen(new WaypointsScreen(parent)); }
      }
      return true;
    }
    return false;
  }
  public void render(int mx, int my, float delta) {
    if (bounds == null) return;
    String hover = "";
    for (int i = 0; i < included.size(); i++) {
      Tool tool = included.get(i); int x = bounds.x() + i * PauseToolsLayout.STEP, y = bounds.y();
      button("", x, y, 20, 20, mx, my, true);
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
      try {
        GL11.glPushMatrix();
        try { GL11.glRotatef(120, 1, 0, 0); net.minecraft.class_583.method_1930(); }
        finally { GL11.glPopMatrix(); }
        GL11.glColor4f(1, 1, 1, 1); GL11.glEnable(32826);
        renderer.method_1487(minecraft.textRenderer, minecraft.textureManager, icons[tool.ordinal()], x + 2, y + 2);
      } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
      if (inside(mx, my, x, y, 20, 20)) hover = switch (tool) { case PHOTO -> "Photo mode"; case MAP -> "Map"; case WAYPOINTS -> "Waypoints"; case RECIPES -> "Recipe book"; };
    }
    if (!hover.isEmpty()) tooltip(hover, mx, my);
  }
}
