package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.field;
import java.nio.file.*;
import local.luke.power.config.*;
import local.luke.power.input.RenderedTerrainBounds;
import local.luke.power.map.*;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Native renderer bounds and shared menu checks, without loading extra terrain. */
final class Map450Checks {
  static void run(Minecraft mc, String action) throws Exception {
    check(mc.world != null && mc.player != null && Maps.cache() != null, "open a mapped disposable world");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    if (action.equals("open")) {
      var style = MapConfig.copy(); style.renderedArea = true; MapConfig.preview(style);
      mc.setScreen(new WorldMapScreen());
      return;
    }
    if (action.equals("capture")) {
      check(mc.currentScreen instanceof WorldMapScreen, "open World Map first");
      Path directory = Path.of("power-beta-data/reports/map-450"); Files.createDirectories(directory);
      int width=mc.currentScreen.width,height=mc.currentScreen.height,mode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
      GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();
      GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
      try {
        mc.currentScreen.init(mc,640,360);
        SettingsSnapshot.render(mc, mc.currentScreen, 640, 360, directory.resolve("rendered-area.png"));
        mc.currentScreen.init(mc,320,240);
        SettingsSnapshot.render(mc, mc.currentScreen, 320, 240, directory.resolve("rendered-area-small.png"));
      } finally {
        mc.currentScreen.init(mc,width,height);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();
        GL11.glPopAttrib();GL11.glMatrixMode(mode);
      }
      return;
    }
    check(action.equals("check"), "use map-450 check|open|capture");
    failures = 0;
    test("MAP450 native renderer exposes exact existing grid bounds", () -> {
      check(mc.worldRenderer instanceof RenderedTerrainBounds, "renderer lacks bounds bridge");
      var bounds = (RenderedTerrainBounds)mc.worldRenderer;
      check(bounds.powerTerrain$minX() == ((Number)field(mc.worldRenderer,"field_1776")).doubleValue(), "min X differs");
      check(bounds.powerTerrain$minZ() == ((Number)field(mc.worldRenderer,"field_1778")).doubleValue(), "min Z differs");
      check(bounds.powerTerrain$maxX() == ((Number)field(mc.worldRenderer,"field_1779")).doubleValue()+16, "max X differs");
      check(bounds.powerTerrain$maxZ() == ((Number)field(mc.worldRenderer,"field_1781")).doubleValue()+16, "max Z differs");
    });
    test("MAP450 shared menus add the border while retaining stored action ordinals", () -> {
      check(MapToggle.RENDERED_AREA.ordinal()==10 && MapToggle.HEADS.ordinal()==5, "stored actions moved");
      check(MapToggle.ALL.contains(MapToggle.RENDERED_AREA) && MapToggle.WORLD_MAP.contains(MapToggle.RENDERED_AREA), "border absent from a menu");
      check(MapToggle.ALL.stream().noneMatch(toggle->toggle.fields.contains("photoWaypoints")), "photo waypoint toggle leaked into maps");
      var style=new MapSettings(); check(!style.renderedArea,"border should default off");
      MapToggle.apply(style,MapToggle.RENDERED_AREA.changes(style)); check(style.renderedArea,"toggle failed");
      style.validate();
    });
    test("MAP450 defaults and settings metadata agree", () -> {
      var session=SettingsRegistry.open(mc);
      var photo=session.settings().stream().filter(setting->setting.id.equals("maps.photoWaypoints")).findFirst().orElseThrow();
      check(DefaultBases.value(DefaultBases.Base.POWER_BETA,photo.id,photo.powerBetaDefault).getAsBoolean(),"Power Beta photo waypoints default off");
      for(var base:DefaultBases.Base.values())
        check(!DefaultBases.value(base,"maps.renderedArea",new com.google.gson.JsonPrimitive(false)).getAsBoolean(),"rendered border default on");
    });
    log("MAP450 failures="+failures); check(failures==0,"map-450 failures="+failures);
  }
}
