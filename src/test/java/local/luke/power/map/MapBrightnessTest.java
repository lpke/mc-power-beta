package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import local.luke.power.render.FullBrightState;
import org.junit.jupiter.api.Test;

class MapBrightnessTest {
  @Test void effectiveBrightnessChangesCaveColorsAndStyleWithoutChangingPreferenceOrTerrain() throws Exception {
    var settings=new MapSettings();
    var cave=new MapTile(new MapMath.Key(0,8,0,0,0,CaveWindow.at(64,10,10).context()));
    var surface=new MapTile(new MapMath.Key(0,-1,0,0,0));
    for(var tile:new MapTile[]{cave,surface})tile.put(0,0x809060,0x809060,63,0,1,0);
    byte[] saved=MapCodec.encode(cave);int dark=cave.color(0,settings),style=settings.style();
    assertNotEquals(dark,cave.color(0,settings,true));
    assertNotEquals(style,settings.style(true));
    assertEquals(surface.color(0,settings),surface.color(0,settings,true));
    assertTrue(settings.caveLighting);
    assertEquals(dark,cave.color(0,settings,false));
    assertEquals(style,settings.style(false));
    assertArrayEquals(saved,MapCodec.encode(cave));
    settings.caveLighting=false;
    assertEquals(cave.color(0,settings,false),cave.color(0,settings,true));
    assertEquals(settings.style(false),settings.style(true));
    assertFalse(settings.caveLighting);
  }

  @Test void spectatorVisitOverridesAndSettingPreviewCancellationRetainTheirSeparateOwners() {
    var light=new FullBrightState();var settings=new MapSettings();Object world=new Object();
    light.update(world,true,false,true);int normal=settings.style(light.active());
    light.update(world,true,true,true);
    assertFalse(settings.caveLighting(light.active()));assertNotEquals(normal,settings.style(light.active()));
    light.toggle();
    assertTrue(settings.caveLighting(light.active()),"manual off during the spectator visit restores cave shading");
    light.update(world,true,false,true);
    assertEquals(normal,settings.style(light.active()));
    light.toggle();light.update(world,true,true,true);light.toggle();light.update(world,true,false,true);
    assertTrue(light.active(),"leaving restores the original manual full bright");
    assertFalse(settings.caveLighting(light.active()));
    settings.caveLighting=false;int preview=settings.style(light.active());
    settings.caveLighting=true;
    assertEquals(preview,settings.style(light.active()),"canceling the preference preview retains full bright");
    light.toggle();assertTrue(settings.caveLighting(light.active()));
    light.update(world,true,true,true);light.update(world,false,true,true);
    assertTrue(settings.caveLighting(light.active()),"revoking spectator access releases only its override");
  }
}
