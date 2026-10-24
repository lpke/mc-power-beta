package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;
import local.luke.power.ui.GuiItems;
import net.minecraft.class_583;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Compare the custom GUI path with native inventory pixels after unrelated drawing. */
final class GuiItems444Checks {
  private static final int SIZE = 48;
  static void run(Minecraft mc) throws Exception {
    test("GUI444 native item lighting, tint, decorations and caller GL state", () -> {
      check(GLContext.getCapabilities().GL_EXT_framebuffer_object, "Item fixture requires framebuffers");
      int framebuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
      boolean blit = GLContext.getCapabilities().GL_EXT_framebuffer_blit;
      int read = blit ? GL11.glGetInteger(EXTFramebufferBlit.GL_READ_FRAMEBUFFER_BINDING_EXT) : framebuffer;
      int draw = blit ? GL11.glGetInteger(EXTFramebufferBlit.GL_DRAW_FRAMEBUFFER_BINDING_EXT) : framebuffer;
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
      Release300MapRefinement.Canvas canvas = null;
      try {
        canvas = new Release300MapRefinement.Canvas(SIZE, SIZE, SIZE, SIZE);
        ItemRenderer renderer = new ItemRenderer();
        List<ItemStack> stacks = List.of(new ItemStack(2, 1, 0), new ItemStack(1, 1, 0),
            new ItemStack(18, 1, 0), new ItemStack(35, 3, 14), new ItemStack(331, 12, 0), new ItemStack(267, 2, 37));
        var iconClass = Class.forName("local.luke.power.ui.ItemIcons");
        var constructor = iconClass.getDeclaredConstructor(); constructor.setAccessible(true);
        Object icons = constructor.newInstance();
        var iconDraw = iconClass.getDeclaredMethod("draw", local.luke.power.ui.UiScreen.class, Minecraft.class,
            local.luke.power.ui.ItemCatalogue.Entry.class, int.class, int.class); iconDraw.setAccessible(true);
        for (ItemStack stack : stacks) {
          for (float scale : new float[]{1, .8f}) {
            setup(canvas); nativeLighting();
            GL11.glTranslatef(12, 12, 0); GL11.glScalef(scale, scale, 1);
            renderer.method_1487(mc.textRenderer, mc.textureManager, stack, 0, 0);
            renderer.method_1488(mc.textRenderer, mc.textureManager, stack, 0, 0);
            byte[] reference = pixels();
            setup(canvas); poison(mc); Map<String, String> before = state();
            GuiItems.draw(renderer, mc, stack, 12, 12, scale, true);
            check(before.equals(state()), "GUI item leaked GL state for " + stack.itemId + ": " + differences(before, state()));
            equal(reference, pixels(), "native GUI item " + stack.itemId + " scale=" + scale);
          }
          setup(canvas); nativeLighting();
          renderer.method_1487(mc.textRenderer, mc.textureManager, stack, 12, 12);
          byte[] reference = pixels();
          setup(canvas); poison(mc); Map<String, String> before = state();
          var entry = new local.luke.power.ui.ItemCatalogue.Entry("fixture", stack.itemId, stack.getDamage(), "Fixture", "", stack);
          iconDraw.invoke(icons, new local.luke.power.ui.UiScreen() {}, mc, entry, 12, 12);
          check(before.equals(state()), "Shared picker icon leaked GL state for " + stack.itemId);
          equal(reference, pixels(), "shared picker/editor icon " + stack.itemId);
        }
        // Count/durability draws must also retain state without drawing a second item.
        setup(canvas); poison(mc); var before = state();
        GuiItems.decorations(renderer, mc, stacks.get(stacks.size() - 1), 12, 12);
        check(before.equals(state()), "Standalone decorations leaked GL state");
        log("GUI444 native pixels matched for grass, stone, leaves, metadata wool, redstone and damaged/count sword; all GL snapshots restored");
      } finally {
        if (canvas != null) canvas.close();
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
        if (blit) {
          EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_READ_FRAMEBUFFER_EXT, read);
          EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_DRAW_FRAMEBUFFER_EXT, draw);
        }
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
        GL11.glPopClientAttrib(); GL11.glPopAttrib();
      }
    });
  }
  private static void setup(Release300MapRefinement.Canvas canvas) {
    canvas.bind(); GL13.glActiveTexture(GL13.GL_TEXTURE0);
    GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_BLEND); GL11.glDisable(GL11.GL_FOG);
    GL11.glEnable(GL11.GL_ALPHA_TEST); GL11.glAlphaFunc(GL11.GL_GREATER, .1f);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GL11.glOrtho(0, SIZE, SIZE, 0, 1000, 3000);
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity(); GL11.glTranslatef(0, 0, -2000);
  }
  private static void nativeLighting() {
    GL11.glPushMatrix();
    try { GL11.glRotatef(120, 1, 0, 0); class_583.method_1930(); }
    finally { GL11.glPopMatrix(); }
    GL11.glColor4f(1, 1, 1, 1); GL11.glEnable(GL12.GL_RESCALE_NORMAL); GL11.glEnable(GL11.GL_DEPTH_TEST);
  }
  private static void poison(Minecraft mc) {
    class_583.method_1930();
    FloatBuffer dark = BufferUtils.createFloatBuffer(4).put(new float[]{0, 0, 0, 1}); dark.flip();
    GL11.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, dark); GL11.glLight(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, dark);
    GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, dark);
    GL11.glDisable(GL11.GL_COLOR_MATERIAL); GL11.glDisable(GL12.GL_RESCALE_NORMAL); GL11.glDisable(GL11.GL_DEPTH_TEST);
    GL11.glEnable(GL11.GL_FOG); GL11.glColor4f(.2f, .3f, .4f, .7f);
    mc.textureManager.bindTexture(mc.textureManager.getTextureId("/gui/gui.png"));
    GL11.glMatrixMode(GL11.GL_PROJECTION);
  }
  private static Map<String, String> state() {
    Map<String, String> result = new LinkedHashMap<>();
    for (int property : new int[]{GL11.GL_CURRENT_COLOR, GL11.GL_CURRENT_NORMAL, GL11.GL_MODELVIEW_MATRIX,
        GL11.GL_PROJECTION_MATRIX, GL11.GL_LIGHT_MODEL_AMBIENT}) result.put("float " + property, floats(property));
    for (int property : new int[]{GL11.GL_MATRIX_MODE, GL11.GL_TEXTURE_BINDING_2D, GL11.GL_SHADE_MODEL,
        GL11.GL_COLOR_MATERIAL_FACE, GL11.GL_COLOR_MATERIAL_PARAMETER, GL11.GL_DEPTH_FUNC,
        GL11.GL_MODELVIEW_STACK_DEPTH, GL11.GL_PROJECTION_STACK_DEPTH, GL11.GL_ATTRIB_STACK_DEPTH})
      result.put("integer " + property, Integer.toString(GL11.glGetInteger(property)));
    for (int property : new int[]{GL11.GL_LIGHTING, GL11.GL_LIGHT0, GL11.GL_LIGHT1, GL11.GL_COLOR_MATERIAL,
        GL11.GL_DEPTH_TEST, GL12.GL_RESCALE_NORMAL, GL11.GL_FOG, GL11.GL_CULL_FACE, GL11.GL_TEXTURE_2D})
      result.put("enabled " + property, Boolean.toString(GL11.glIsEnabled(property)));
    for (int light : new int[]{GL11.GL_LIGHT0, GL11.GL_LIGHT1})
      for (int property : new int[]{GL11.GL_POSITION, GL11.GL_DIFFUSE, GL11.GL_AMBIENT, GL11.GL_SPECULAR}) {
        FloatBuffer values = BufferUtils.createFloatBuffer(16); GL11.glGetLight(light, property, values);
        float[] data = new float[4]; values.get(data); result.put("light " + light + ":" + property, Arrays.toString(data));
      }
    for (int property : new int[]{GL11.GL_AMBIENT, GL11.GL_DIFFUSE, GL11.GL_SPECULAR, GL11.GL_EMISSION}) {
      FloatBuffer values = BufferUtils.createFloatBuffer(16); GL11.glGetMaterial(GL11.GL_FRONT, property, values);
      float[] data = new float[4]; values.get(data); result.put("material " + property, Arrays.toString(data));
    }
    return result;
  }
  private static String floats(int property) {
    FloatBuffer values = BufferUtils.createFloatBuffer(16); GL11.glGetFloat(property, values);
    float[] data = new float[16]; values.get(data); return Arrays.toString(data);
  }
  private static String differences(Map<String, String> before, Map<String, String> after) {
    return before.keySet().stream().filter(key -> !Objects.equals(before.get(key), after.get(key))).toList().toString();
  }
  private static byte[] pixels() {
    ByteBuffer pixels = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
    GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1); GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
    GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0); GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
    GL11.glReadPixels(0, 0, SIZE, SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
    byte[] result = new byte[pixels.capacity()]; pixels.get(result); return result;
  }
  private static void equal(byte[] reference, byte[] actual, String label) {
    int differences = 0, visible = 0;
    for (int i = 0; i < reference.length; i++) {
      if (Math.abs((reference[i] & 255) - (actual[i] & 255)) > 1) differences++;
      if (i % 4 != 3 && reference[i] != reference[i % 4]) visible++;
    }
    check(visible > 0 && differences == 0, label + " pixel differences=" + differences);
  }
}
