package local.luke.power.render;

import local.luke.power.input.DetachedCamera;
import local.luke.power.input.HeadGeometry;
import local.luke.power.input.PlayerMode;
import com.matthewperiut.retroauth.skin.data.ModelPartData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPart;
import org.lwjgl.opengl.GL11;

/** A local visual marker only. Never creates an entity or alters the player's model. */
public final class GhostHead {
  private static final ModelPart[][] parts = new ModelPart[2][];
  private static int skinTexture = -1, skinLayout;
  private GhostHead() {}

  public static boolean visible(Minecraft mc) {
    return mc.world != null && mc.player != null && mc.field_2807 != null && mc.options.thirdPerson
        && (DetachedCamera.isActive() || mc.player instanceof PlayerMode mode && mode.power$spectator());
  }

  public static void render(Minecraft mc, float delta) {
    if (!visible(mc)) return;
    var player = mc.player;
    var camera = mc.field_2807;
    boolean detached = DetachedCamera.isActive();
    double x = detached ? DetachedCamera.x() : player.prevX + (player.x - player.prevX) * delta;
    double y = detached ? HeadGeometry.baseY(DetachedCamera.y())
        : player.prevY + (player.y - player.prevY) * delta - player.field_1631 + HeadGeometry.BASE_ABOVE_FEET;
    double z = detached ? DetachedCamera.z() : player.prevZ + (player.z - player.prevZ) * delta;
    float yaw = detached ? DetachedCamera.facingYaw() : player.prevYaw + (player.yaw - player.prevYaw) * delta;
    float pitch = detached ? (float)DetachedCamera.pitch() : player.prevPitch + (player.pitch - player.prevPitch) * delta;
    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw) || !Float.isFinite(pitch)) return;
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
        | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_LIGHTING_BIT);
    GL11.glPushMatrix();
    try {
      GL11.glTranslated(x - (camera.prevX + (camera.x - camera.prevX) * delta),
          y - (camera.prevY + (camera.y - camera.prevY) * delta),
          z - (camera.prevZ + (camera.z - camera.prevZ) * delta));
      GL11.glRotatef(180 - yaw, 0, 1, 0);
      GL11.glScalef(-HeadGeometry.SCALE, -HeadGeometry.SCALE, HeadGeometry.SCALE);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glEnable(GL11.GL_ALPHA_TEST);
      GL11.glAlphaFunc(GL11.GL_GREATER, .01F);
      GL11.glDepthMask(false);
      GL11.glColor4f(1, 1, 1, .65F);
      int texture = mc.textureManager.method_1093(player.skinUrl, player.method_1314());
      if (texture < 0) return;
      mc.textureManager.bindTexture(texture);
      if (skinTexture != texture) {
        // Query only when the skin changes; modern skins use a square texture.
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        skinLayout = height == width ? 1 : 0;
        skinTexture = texture;
      }
      if (parts[skinLayout] == null) {
        ModelPart head = new ModelPart(0, 0), hat = new ModelPart(32, 0);
        ((ModelPartData)(Object)head).setTextureHeight(skinLayout == 1 ? 64 : 32);
        ((ModelPartData)(Object)hat).setTextureHeight(skinLayout == 1 ? 64 : 32);
        head.addCuboid(-4, -8, -4, 8, 8, 8, 0);
        hat.addCuboid(-4, -8, -4, 8, 8, 8, .5F);
        parts[skinLayout] = new ModelPart[]{head, hat};
      }
      ModelPart head = parts[skinLayout][0], hat = parts[skinLayout][1];
      head.pitch = hat.pitch = pitch * (float)Math.PI / 180;
      head.render(1F / 16); hat.render(1F / 16);
    } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
  }
}
