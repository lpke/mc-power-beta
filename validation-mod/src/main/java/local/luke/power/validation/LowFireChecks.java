package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.Arrays;
import javax.imageio.ImageIO;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.visual.*;
import net.minecraft.class_556;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

final class LowFireChecks {
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("saved")) {
      check(VisualConfig.current().lowFire, "low fire preference lost on restart");
      log("PASS low fire survived restart"); return;
    }
    failures = 0;
    var old = VisualConfig.copy(); old.lowFire = false; VisualConfig.preview(old);
    test("low fire defaults off; Cancel restores the rendered overlay without writing", () -> {
      var session = SettingsRegistry.open(mc); var row = Release110Checks.find(session, "visual.lowFire");
      check(!row.defaultValue.getAsBoolean() && row.page.equals("Video") && row.group.equals("Textures"), "wrong low fire default/location");
      byte[] before = Files.readAllBytes(PowerConfig.path());
      row.parse("true"); session.preview(); check(VisualConfig.current().lowFire, "preview missing");
      session.discard(); check(!VisualConfig.current().lowFire, "Cancel failed");
      check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "Cancel saved settings");
    });
    test("real fire renderer keeps identical texture pixels and lowers both quads", () -> {
      Path dir = Path.of("power-beta-data/reports/low-fire"); Files.createDirectories(dir);
      int health = mc.player.health, fire = mc.player.fire;
      // Populate procedural fire's initially empty frames before comparing in one tick.
      for (int i = 0; i < 40; i++) mc.textureManager.method_1084();
      var normal = render(mc, false, dir.resolve("normal.png"));
      var low = render(mc, true, dir.resolve("low.png"));
      int topNormal = top(normal), topLow = top(low), changed = 0, coloured = 0;
      check(topNormal < 360 && topLow - topNormal == 54, "fire not lowered: " + topNormal + " -> " + topLow);
      for (int y = 54; y < 360; y++) for (int x = 0; x < 640; x++) {
        int a = normal.getRGB(x, y - 54), b = low.getRGB(x, y);
        if ((a & 0xffffff) != 0) coloured++;
        if (a != b) changed++;
      }
      check(coloured > 1000 && changed < 100, "texture changed: pixels=" + coloured + " mismatch=" + changed);
      check(mc.player.health == health && mc.player.fire == fire, "rendering changed fire damage");
      log("LOW FIRE pixel comparison coloured=" + coloured + " mismatches=" + changed);
    });
    test("Apply persists low fire", () -> {
      var current = VisualConfig.copy(); current.lowFire = false; VisualConfig.preview(current);
      var session = SettingsRegistry.open(mc); Release110Checks.find(session, "visual.lowFire").parse("true"); session.save(Path.of("."));
      check(PowerConfig.section("visual").get("lowFire").getAsBoolean(), "Apply lost low fire");
    });
    log("LOW FIRE FAILURES " + failures);
  }
  private static int top(BufferedImage image) {
    for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
      if ((image.getRGB(x, y) & 0xffffff) != 0) return y;
    return image.getHeight();
  }
  private static BufferedImage render(Minecraft mc, boolean low, Path path) throws Exception {
    var settings = VisualConfig.copy(); settings.lowFire = low; VisualConfig.preview(settings);
    int program = GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM);
    var tessellator = net.minecraft.client.render.Tessellator.INSTANCE;
    var offsets = new java.util.LinkedHashMap<java.lang.reflect.Field,Double>();
    for (var field : net.minecraft.client.render.Tessellator.class.getDeclaredFields()) if(field.getType()==double.class) {
      field.setAccessible(true); offsets.put(field,field.getDouble(tessellator));
    }
    tessellator.translate(0D,0D,0D);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    org.lwjgl.opengl.GL20.glUseProgram(0); org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
    GL11.glMatrixMode(GL11.GL_TEXTURE); GL11.glPushMatrix(); GL11.glLoadIdentity();
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      GL11.glViewport(0, 0, 640, 360); GL11.glDisable(GL11.GL_SCISSOR_TEST);
      GL11.glClearColor(0, 0, 0, 1); GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GL11.glOrtho(-1, 1, -1, 1, .1, 10);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_FOG); GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_CULL_FACE); GL11.glColorMask(true, true, true, true);
      GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_ALPHA_TEST);
      // The normal overlay call binds StationAPI's game atlas, not the legacy terrain texture.
      Object manager = Class.forName("net.modificationstation.stationapi.api.client.StationRenderAPI")
          .getMethod("getBakedModelManager").invoke(null);
      Object atlasId = Class.forName("net.modificationstation.stationapi.api.client.texture.atlas.Atlases")
          .getField("GAME_ATLAS_TEXTURE").get(null);
      Object atlas = manager.getClass().getMethod("getAtlas", atlasId.getClass()).invoke(manager, atlasId);
      var renderer = mc.field_2818.field_2342;
      atlas.getClass().getMethod("bindTexture").invoke(atlas);
      var method = class_556.class.getDeclaredMethod("method_1867", float.class); method.setAccessible(true);
      method.invoke(renderer, 0F); GL11.glFinish();
      ByteBuffer pixels = BufferUtils.createByteBuffer(640 * 360 * 4);
      GL11.glReadPixels(0, 0, 640, 360, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
      var image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
      for (int y = 0; y < 360; y++) for (int x = 0; x < 640; x++) {
        int at = ((359 - y) * 640 + x) * 4;
        image.setRGB(x, y, (pixels.get(at) & 255) << 16 | (pixels.get(at + 1) & 255) << 8 | pixels.get(at + 2) & 255);
      }
      ImageIO.write(image, "png", path.toFile());
      check(GL11.glGetError() == GL11.GL_NO_ERROR, "fire renderer GL error"); return image;
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_TEXTURE); GL11.glPopMatrix(); GL11.glPopAttrib();
      org.lwjgl.opengl.GL20.glUseProgram(program);
      for (var entry:offsets.entrySet()) entry.getKey().setDouble(tessellator,entry.getValue());
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
  }
}
