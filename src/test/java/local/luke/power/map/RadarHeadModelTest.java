package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class RadarHeadModelTest {
  private RadarHeadModel.Head model(String type) {
    return RadarHeadModel.create(new RadarIconSource(type, "/mob/test.png", null, 0, false));
  }

  @Test void everyNativeMobAndPlayerHasDetachedHeadGeometryAndUnknownTypesKeepDots() {
    assertEquals(14, RadarIconSource.MOBS.size());
    for (String type : RadarIconSource.MOBS) assertNotNull(model(type), type);
    assertNotNull(model("Player")); assertNull(model("ModdedMob"));
    var player = model("Player");
    assertEquals(8, player.base().get(0).width()); assertEquals(8, player.base().get(0).height());
    assertEquals(-4, player.centerY()); assertEquals(0, player.centerZ());
    assertEquals(.5, player.overlay().get(0).dilation());
  }

  @Test void cowHornsChickenBillAndWolfEarsAndMuzzleAreActualSeparateCuboids() {
    var cow = model("Cow"); assertEquals(3, cow.base().size());
    assertEquals(8, cow.base().get(0).width());
    assertTrue(cow.base().subList(1,3).stream().allMatch(box -> box.width() == 1 && box.height() == 3));
    var chicken = model("Chicken"); assertEquals(3, chicken.base().size());
    assertEquals(List.of(0, 14, 14), chicken.base().stream().map(RadarHeadModel.Cuboid::u).toList());
    assertEquals(4, chicken.base().get(0).width());
    var wolf = model("Wolf"); assertEquals(4, wolf.base().size());
    assertEquals(6, wolf.base().get(0).width());
    assertEquals(2, wolf.base().get(1).width()); assertEquals(2, wolf.base().get(2).width());
    assertEquals(-5, wolf.base().get(3).z());
    assertEquals(1, model("Pig").base().size());
  }

  @Test void sheepFleeceUsesOnlyItsOwnTintAndDisappearsWhenSheared() {
    var bare = RadarHeadModel.create(new RadarIconSource("Sheep", "/mob/sheep.png", null, 14, false));
    var wool = RadarHeadModel.create(new RadarIconSource("Sheep", "/mob/sheep.png", null, 14, true));
    assertEquals(bare.base(), wool.base()); assertTrue(bare.overlay().isEmpty());
    assertEquals(1, wool.overlay().size()); assertEquals("/mob/sheep_fur.png", wool.overlayTexture());
    assertEquals(.6, wool.overlay().get(0).dilation());
    assertNotEquals(0xffffff, wool.overlayColor());
  }

  @Test void silhouetteRadiusIncludesHornsEarsWoolAndTheHalfPixelOutline() {
    for (var sample : java.util.Map.of("Creeper", 4.5, "Cow", 6.5, "Wolf", 5.5,
        "Chicken", 3.5, "Ghast", 8.5, "Squid", 8.5).entrySet()) {
      var source = new RadarIconSource(sample.getKey(), "/mob/test.png", null, 0, false);
      assertEquals(sample.getValue(), RadarIcons.radius(source), sample.getKey());
    }
  }

  @Test void snapshotsResolveCurrentCategoryAndMobDraftColorsWithoutWaitingForAnotherTick() {
    var settings = new MapSettings();
    var snapshot = new Maps.Radar(0,0,0,"Zombie",0xff6655,false,0,0,0,0,
        RadarKind.HOSTILE, "Zombie", null);
    assertEquals(0xff6655, snapshot.color(settings));
    settings.radarHostileColor = 0x123456; assertEquals(0x123456, snapshot.color(settings));
    settings.radarZombieColor = 0; assertEquals(0, snapshot.color(settings));
    settings.radarHostileColor = 0xabcdef; assertEquals(0, snapshot.color(settings));
    settings.radarZombieColor = -1; assertEquals(0xabcdef, snapshot.color(settings));
    assertEquals(0xff6655, snapshot.color());
    assertTrue(RadarKind.VEHICLES.visible(settings));
    settings.otherEntities = false;
    assertFalse(RadarKind.VEHICLES.visible(settings)); assertFalse(RadarKind.PROJECTILES.visible(settings));
    assertFalse(RadarKind.OTHER.visible(settings)); assertTrue(RadarKind.ANIMALS.visible(settings));
  }
}
