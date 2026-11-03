package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.ui.UiScreen;
import local.luke.power.validation.mixin.CapeRenderAccess;
import local.luke.power.visual.VisualConfig;
import net.minecraft.class_549;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.*;
import net.minecraft.entity.player.*;
import org.lwjgl.opengl.*;

/** Native model/cape contact sheets; isolated actors never enter the world or replace its player. */
final class CapeGallery473Checks {
  private static final String CAPE = "power-beta-validation:cape-gallery-473";
  private static final Path REPORT = Path.of("power-beta-data/reports/release-473/cape-gallery");
  private static final int WIDTH = 800, HEIGHT = 450;

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null, "load a disposable singleplayer world");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    check(Display.getWidth() >= WIDTH * 2 && Display.getHeight() >= HEIGHT * 2,
        "resize the disposable client to at least 1600x900");
    var visual = VisualConfig.copy();
    var building = local.luke.power.building.config.Config.current().copy();
    var actor = new ClientPlayerEntity(mc, mc.world, mc.session, mc.player.dimensionId);
    actor.field_161 = new net.minecraft.class_41();
    actor.method_1341(mc.player.x, mc.player.y, mc.player.z, 0, 0);
    actor.prevX = actor.x; actor.prevY = actor.y; actor.prevZ = actor.z;
    actor.skinUrl = null; actor.playerCapeUrl = CAPE;
    actor.inventory.main = new net.minecraft.item.ItemStack[actor.inventory.main.length];
    actor.inventory.armor = new net.minecraft.item.ItemStack[actor.inventory.armor.length];
    int matrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL13.glActiveTexture(GL13.GL_TEXTURE0);
    GL11.glMatrixMode(GL11.GL_TEXTURE); GL11.glPushMatrix(); GL11.glLoadIdentity();
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    int texture = -1;
    var panels = new ArrayList<Map<String,Object>>();
    try {
      GL20.glUseProgram(0);
      texture = mc.textureManager.method_1088(capeTexture());
      var renderer = new GalleryRenderer(mc, texture);
      renderer.setDispatcher(EntityRenderDispatcher.field_2489);
      var settings = VisualConfig.copy(); settings.limitCapeRotation = true; VisualConfig.preview(settings);
      Files.createDirectories(REPORT);
      for (boolean minecart : new boolean[]{false, true}) {
        var screen = new Gallery(mc, actor, renderer, building, minecart, panels);
        screen.init(mc, WIDTH, HEIGHT);
        SettingsSnapshot.render(mc, screen, WIDTH, HEIGHT,
            REPORT.resolve(minecart ? "minecart-fast-off-on.png" : "backward-walk-sprint.png"));
      }
      Files.writeString(REPORT.resolve("panels.json"), Catalog.JSON.toJson(panels));
      check(GL11.glGetError() == GL11.GL_NO_ERROR, "cape gallery OpenGL error");
    } finally {
      CapeChecks.recording = false;
      actor.field_1595 = null;
      if (texture >= 0) mc.textureManager.delete(texture);
      VisualConfig.preview(visual);
      local.luke.power.building.config.Config.preview(building);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL13.glActiveTexture(GL13.GL_TEXTURE0);
      GL11.glMatrixMode(GL11.GL_TEXTURE); GL11.glPopMatrix();
      GL13.glActiveTexture(activeTexture);
      GL20.glUseProgram(program);
      GL11.glPopAttrib(); GL11.glMatrixMode(matrix);
    }
    log("CAPE GALLERY473 two native contact sheets and " + panels.size() + " observed poses written");
  }

  private static BufferedImage capeTexture() {
    var image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++) {
      // The native cape's two 10x16 faces occupy x=1..10 and x=12..21, y=1..16.
      int local = x < 12 ? x - 1 : x - 12;
      int color = local == 0 || local == 9 || y == 1 || y == 16 ? 0xffffd34d : 0xffd83242;
      if (local == 4 || local == 5) color = 0xff64d8e8;
      image.setRGB(x, y, color);
    }
    return image;
  }

  private static final class GalleryRenderer extends PlayerEntityRenderer {
    private final Minecraft mc;
    private final int cape;
    GalleryRenderer(Minecraft mc, int cape) { this.mc = mc; this.cape = cape; }
    @Override protected boolean method_2027(String url, String fallback) {
      if (CAPE.equals(url)) mc.textureManager.bindTexture(cape);
      else method_2026("/mob/char.png");
      return true;
    }
    @Override protected void method_821(PlayerEntity player, double x, double y, double z) {}
  }

  private static final class Gallery extends UiScreen {
    private final Minecraft mc;
    private final ClientPlayerEntity actor;
    private final GalleryRenderer renderer;
    private final local.luke.power.building.config.Settings building;
    private final boolean minecart;
    private final List<Map<String,Object>> panels;
    Gallery(Minecraft mc, ClientPlayerEntity actor, GalleryRenderer renderer,
        local.luke.power.building.config.Settings building, boolean minecart, List<Map<String,Object>> panels) {
      this.mc = mc; this.actor = actor; this.renderer = renderer; this.building = building;
      this.minecart = minecart; this.panels = panels;
    }
    @Override public void render(int mx, int my, float delta) {
      fill(0, 0, width, height, 0xff20252d);
      text(minecart ? "Minecart cape / Faster minecarts off and on" : "Backward movement / walking and sprinting", 16, 12, 0xffffff);
      text("Native player model and cape - rear three-quarter view", 16, 28, 0xbfc8d5);
      for (int row = 0; row < 2; row++) for (int column = 0; column < 3; column++) panel(row, column);
      text("Diagnostic cape: gold border, cyan center stripe. Production cape correction enabled.", 16, 433, 0xbfc8d5);
    }
    private void panel(int row, int column) {
      int left = 12 + column * 264, top = 48 + row * 190;
      fill(left, top, left + 256, top + 182, 0xff2b323d);
      float phase = new float[]{0, 1.2f, 2.4f}[column];
      float amplitude = row == 0 ? .35f : 1;
      float yaw = minecart ? new float[]{0, 90, 180}[column] : 0;
      double trail = minecart ? (row == 0 ? .35 : .7) : .2;
      actor.field_1595 = minecart ? new class_549(mc.world, actor.x, actor.y, actor.z, 0) : null;
      actor.yaw = actor.prevYaw = actor.field_1012 = actor.field_1013 = yaw;
      actor.pitch = actor.prevPitch = 0;
      actor.field_1048 = actor.field_1049 = minecart ? 0 : amplitude;
      actor.field_1050 = phase + actor.field_1049 * .5f;
      actor.field_530 = actor.field_533 = minecart ? actor.x - trail : actor.x;
      actor.field_531 = actor.field_534 = actor.y + (minecart ? .05 : -.6);
      actor.field_532 = actor.field_535 = minecart ? actor.z : actor.z + trail;
      actor.field_524 = actor.field_525 = minecart ? 0 : .2f;
      actor.field_1634 = actor.field_1635 = phase / 6;
      if (minecart) {
        var settings = building.copy(); settings.fastMinecarts = row == 1;
        local.luke.power.building.config.Config.preview(settings);
      }
      String title = minecart ? "Fast " + (row == 0 ? "off" : "on") + " / rider yaw " + (int)yaw
          : (row == 0 ? "Walking" : "Sprinting") + " / stride " + (column + 1);
      text(title, left + 10, top + 9, 0xffffff);
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
      List<Float> observed;
      try {
        GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_ALPHA_TEST); GL11.glAlphaFunc(GL11.GL_GREATER, .1f);
        GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_FOG);
        GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glColor4f(1, 1, 1, 1);
        GL11.glTranslatef(left + 130, top + 159, 0);
        GL11.glScalef(69, -69, 69);
        GL11.glRotatef(12, 1, 0, 0);
        // Counter the chosen native body yaw so every panel retains the same view of the back.
        GL11.glRotatef(145 + yaw, 0, 1, 0);
        CapeChecks.angles.clear(); CapeChecks.recording = true;
        // Native PlayerEntityRenderer subtracts the player's eye offset from render Y.
        // This gallery anchors the feet, so supply that offset instead of drawing below the panel.
        renderer.render(actor, 0, actor.field_1631, 0, yaw, .5f);
        observed = List.copyOf(CapeChecks.angles);
        check(observed.size() == 4, "gallery panel did not render the native cape: " + title);
      } finally {
        CapeChecks.recording = false;
        GL11.glPopMatrix(); GL11.glPopAttrib();
      }
      var model = ((CapeRenderAccess)(Object)renderer).power$capeModel();
      if (minecart) check(observed.get(1) == 0 && observed.get(2) == 0, "gallery rider cape tilted laterally");
      var entry = new LinkedHashMap<String,Object>();
      entry.put("panel", title); entry.put("capeAngles", observed);
      entry.put("rightLegPitch", model.rightLeg.pitch); entry.put("leftLegPitch", model.leftLeg.pitch);
      entry.put("controlledTrailBlocks", trail); panels.add(entry);
      text("Cape pitch " + Math.round(observed.get(0)) + " degrees", left + 10, top + 166, 0xbfc8d5);
    }
  }
}
