package local.luke.power.input;

import java.util.function.ToIntFunction;
import org.lwjgl.opengl.GL11;

/** Shared full-resolution skybox for the title and the settings preview, with scoped GL state. */
public final class PanoramaRenderer {
  private PanoramaRenderer() {}

  public static void render(ToIntFunction<String> texture, String selection, boolean blur, float ticks, int width, int height) {
    String folder = PanoramaCatalog.folder(selection);
    if (folder.isEmpty()) return;
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT
        | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
    int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glLoadIdentity();
    double aspect = (double)Math.max(1, width) / Math.max(1, height);
    GL11.glFrustum(-.05 * aspect, .05 * aspect, -.05, .05, .05, 10);
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix(); GL11.glLoadIdentity();
    try {
      GL11.glRotatef(180, 1, 0, 0);
      GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND);
      GL11.glDisable(GL11.GL_ALPHA_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDepthMask(false);
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_FOG);
      GL11.glColorMask(true, true, true, true);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      int samples = blur ? 4 : 1;
      for (int sample = 0; sample < samples * samples; sample++) {
        GL11.glPushMatrix();
        try {
          if (blur) GL11.glTranslatef((sample % samples / (float)samples - .5f) / 64,
              (sample / samples / (float)samples - .5f) / 64, 0);
          GL11.glRotatef((float)Math.sin(ticks / 400) * 25 + 20, 1, 0, 0);
          GL11.glRotatef(-ticks * .1f, 0, 1, 0);
          GL11.glColor4f(1, 1, 1, (255 / (sample + 1)) / 255f);
          for (int face = 0; face < 6; face++) {
            GL11.glPushMatrix();
            switch (face) {
              case 1 -> GL11.glRotatef(90, 0, 1, 0);
              case 2 -> GL11.glRotatef(180, 0, 1, 0);
              case 3 -> GL11.glRotatef(-90, 0, 1, 0);
              case 4 -> GL11.glRotatef(90, 1, 0, 0);
              case 5 -> GL11.glRotatef(-90, 1, 0, 0);
            }
            try {
              GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.applyAsInt("/assets/power_controls/textures/" + folder + "/panorama" + face + ".png"));
              GL11.glBegin(GL11.GL_QUADS);
              GL11.glTexCoord2f(0, 0); GL11.glVertex3d(-1, -1, 1);
              GL11.glTexCoord2f(1, 0); GL11.glVertex3d(1, -1, 1);
              GL11.glTexCoord2f(1, 1); GL11.glVertex3d(1, 1, 1);
              GL11.glTexCoord2f(0, 1); GL11.glVertex3d(-1, 1, 1);
              GL11.glEnd();
            } finally { GL11.glPopMatrix(); }
          }
        } finally { GL11.glPopMatrix(); }
        GL11.glColorMask(true, true, true, false);
      }
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glMatrixMode(matrixMode); GL11.glPopAttrib();
    }
  }
}
