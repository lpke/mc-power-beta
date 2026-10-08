package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.visual.*;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.class_13;
import net.minecraft.class_519;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.Tessellator;
import net.minecraft.world.BlockView;
import org.lwjgl.opengl.GL11;

final class RedstoneVisualChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    var document = PowerConfig.document(); var original = VisualConfig.copy();
    var pack = mc.field_2768.field_1175;
    var session = SettingsRegistry.open(mc); var setting = find(session, "visual.redstonePowerLevels");
    Path dir = Path.of("power-beta-data/reports/redstone"); Files.createDirectories(dir);
    try {
      test("redstone defaults off, previews without saving, and Cancel restores", () -> {
        check(!setting.defaultValue.getAsBoolean() && setting.group.equals("Textures"), "default or placement");
        byte[] before = Files.readAllBytes(PowerConfig.path());
        setting.parse("true"); session.preview(); check(VisualConfig.current().redstonePowerLevels, "no preview");
        check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "preview saved");
        session.discard(); check(!VisualConfig.current().redstonePowerLevels, "Cancel not restored");
      });
      test("only world settings carry scope notes and world rows identify themselves", () -> {
        for (var s : session.settings()) {
          String tip = Tooltips.setting(s, "");
          if (SettingScope.perWorld(s)) {
            check(tip.contains("Saved separately for each singleplayer world."), "missing world tooltip: " + s.id);
            check((s.label + " " + s.group).toLowerCase(Locale.ROOT).contains("this world"), "missing world name: " + s.id);
          } else check(!tip.contains("Global setting.") && !tip.contains("Global binding."), "global boilerplate: " + s.id);
        }
      });
      for (Object candidate : mc.field_2768.method_1000()) {
        var selected = (net.minecraft.class_285) candidate;
        if (!Set.of("Default", "faithful32pack.zip").contains(selected.field_1137)) continue;
        mc.field_2768.method_999(selected); mc.textureManager.method_1096();
        for (boolean enabled : new boolean[] {false, true}) test("all sixteen power states, pack " + selected.field_1137 + " enabled=" + enabled, () -> {
          checkSprites();
          setting.parse(Boolean.toString(enabled)); session.preview();
          var screen = sheet(mc); screen.init(mc, 640, 420);
          SettingsSnapshot.render(mc, screen, 640, 420, dir.resolve(selected.field_1137 + "-" + enabled + ".png"));
        });
      }
      test("Apply saves redstone choice and opening a new session retains it", () -> {
        setting.parse("true"); session.save(Path.of(".").toAbsolutePath());
        check(find(SettingsRegistry.open(mc), setting.id).value.getAsBoolean(), "Apply lost choice");
        check(PowerConfig.section("visual").get("redstonePowerLevels").getAsBoolean(), "Apply not stored");
      });
    } finally {
      VisualConfig.preview(original); PowerConfig.write(document);
      mc.field_2768.method_999(pack); mc.textureManager.method_1096(); mc.worldRenderer.method_1537();
      mc.setScreen(null);
    }
    log("REDSTONE VISUAL FAILURES " + failures);
  }
  private static void checkSprites() throws Exception {
    var f = RedstonePowerRenderer.class.getDeclaredField("LEVELS"); f.setAccessible(true);
    Object[] levels = (Object[]) f.get(null);
    for (int power = 0; power < levels.length; power++) {
      Object sprite = levels[power].getClass().getMethod("getSprite").invoke(levels[power]);
      Object contents = sprite.getClass().getMethod("getContents").invoke(sprite);
      int source = power == 9 ? 6 : power;
      String expected = "powerbeta:redstone/powerlevel" + (source < 10 ? "0" : "") + source;
      check(contents.getClass().getMethod("getId").invoke(contents).toString().equals(expected), "missing sprite " + power);
      check((int) contents.getClass().getMethod("getWidth").invoke(contents) == 16
          && (int) contents.getClass().getMethod("getHeight").invoke(contents) == 16, "not a 16px sprite " + power);
    }
  }
  private static Screen sheet(Minecraft mc) {
    BlockView view = new BlockView() {
      public int getBlockId(int x, int y, int z) { return y == 0 && x >= 0 && x < 4 && z >= 0 && z < 4 ? 55 : y < 0 ? 1 : 0; }
      public BlockEntity method_1777(int x, int y, int z) { return null; }
      public float method_1784(int x, int y, int z, int light) { return 1; }
      public float method_1782(int x, int y, int z) { return 1; }
      public int method_1778(int x, int y, int z) { return Math.floorMod(x + z * 4, 16); }
      public Material method_1779(int x, int y, int z) { return y < 0 ? Material.STONE : Material.AIR; }
      public boolean method_1783(int x, int y, int z) { return y < 0; }
      public boolean method_1780(int x, int y, int z) { return y < 0; }
      public class_519 method_1781() { return mc.world.method_1781(); }
    };
    return new Screen() {
      public void render(int mx, int my, float delta) {
        try {
          var api = Class.forName("net.modificationstation.stationapi.api.client.texture.StationTextureManager");
          var get = Arrays.stream(api.getMethods()).filter(m -> m.getName().equals("get")).findFirst().orElseThrow();
          var textures = get.invoke(null, mc.textureManager);
          var atlas = Class.forName("net.modificationstation.stationapi.api.client.texture.atlas.Atlases").getField("GAME_ATLAS_TEXTURE").get(null);
          api.getMethod("bindTexture", atlas.getClass()).invoke(textures, atlas);
          GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
          GL11.glPushMatrix(); GL11.glTranslatef(128, 18, 0); GL11.glScalef(96, 96, -96); GL11.glRotatef(-90, 1, 0, 0);
          GL11.glDisable(GL11.GL_CULL_FACE); GL11.glDisable(GL11.GL_LIGHTING);
          var tess = Tessellator.INSTANCE; tess.startQuads();
          try {
            var renderer = new class_13(view);
            for (int power = 0; power < 16; power++) check(renderer.method_71(Block.BLOCKS[55], power % 4, 0, power / 4), "wire did not render");
          } finally { tess.draw(); GL11.glPopMatrix(); GL11.glPopAttrib(); }
        } catch (Exception e) { throw new RuntimeException(e); }
      }
    };
  }
}
