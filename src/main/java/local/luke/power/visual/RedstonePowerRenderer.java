package local.luke.power.visual;

import net.minecraft.block.Block;
import net.minecraft.class_408;
import net.minecraft.client.render.Tessellator;
import net.minecraft.world.BlockView;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.client.event.texture.TextureRegisterEvent;
import net.modificationstation.stationapi.api.client.texture.Sprite;
import net.modificationstation.stationapi.api.client.texture.atlas.Atlas;
import net.modificationstation.stationapi.api.client.texture.atlas.Atlases;
import net.modificationstation.stationapi.api.util.Identifier;

/** Chunk geometry using the normal wire connections; all textures are loaded with the atlas. */
public final class RedstonePowerRenderer {
  private static final Atlas.Sprite[] LEVELS = new Atlas.Sprite[16];
  private static Atlas.Sprite wire, line;

  @EventListener
  public void textures(TextureRegisterEvent event) {
    wire = Atlases.getTerrain().addTexture(Identifier.of("powerbeta:redstone/redstone_dust_dot"));
    line = Atlases.getTerrain().addTexture(Identifier.of("powerbeta:redstone/redstone_dust_line0"));
    for (int power = 0; power < LEVELS.length; power++) {
      // The supplied pack replaces 9 with a joke image; rotate its 6 glyph instead.
      int texture = power == 9 ? 6 : power;
      LEVELS[power] = Atlases.getTerrain().addTexture(Identifier.of(
          "powerbeta:redstone/powerlevel" + (texture < 10 ? "0" : "") + texture));
    }
  }

  public static boolean render(BlockView world, Block block, int x, int y, int z) {
    if (!VisualConfig.current().redstonePowerLevels || wire == null) return false;
    int power = world.method_1778(x, y, z) & 15;
    boolean openAbove = !world.method_1780(x, y + 1, z);
    boolean west = connects(world, x - 1, y, z, 1, openAbove);
    boolean east = connects(world, x + 1, y, z, 3, openAbove);
    boolean north = connects(world, x, y, z - 1, 2, openAbove);
    boolean south = connects(world, x, y, z + 1, 0, openAbove);
    boolean across = west || east, along = north || south;
    // Beta draws a full line for one axis and a cross when isolated.
    if (!along) west = east = true;
    if (!across) north = south = true;
    float light = block.method_1604(world, x, y, z), strength = power / 15f;
    Tessellator t = Tessellator.INSTANCE;
    t.color(light * (power == 0 ? .3f : strength * .6f + .4f),
        light * Math.max(0, strength * strength * .7f - .5f), 0);
    Sprite dot = wire.getSprite(), strip = line.getSprite();
    double height = y + .015625;
    // Keep the reference's shaded edges: four-pixel lines and six-pixel junctions.
    if ((west || east) && (north || south))
      flat(t, x, height, z, x + 1, z + 1, dot.getMinU(), dot.getMinV(), dot.getMaxU(), dot.getMaxV());
    if (west || east) horizontal(t, strip, x, height, z, west ? 0 : .5, east ? 1 : .5);
    if (north || south)
      flat(t, x, height, z + (north ? 0 : .5), x + 1, z + (south ? 1 : .5),
          strip.getMinU(), strip.getFrameV(north ? 0 : 8), strip.getMaxU(), strip.getFrameV(south ? 16 : 8));
    if (openAbove) {
      if (climbs(world, x - 1, y, z)) side(t, strip, x + .015625, y, z, x + .015625, z + 1);
      if (climbs(world, x + 1, y, z)) side(t, strip, x + .984375, y, z + 1, x + .984375, z);
      if (climbs(world, x, y, z - 1)) side(t, strip, x + 1, y, z + .015625, x, z + .015625);
      if (climbs(world, x, y, z + 1)) side(t, strip, x, y, z + .984375, x + 1, z + .984375);
    }
    Sprite number = LEVELS[power].getSprite();
    t.color(light, light, light);
    // Match the reference pack's quarter-block panel, slightly above the wire.
    flat(t, x + .375, y + .025, z + .375, x + .625, z + .625,
        power == 9 ? number.getMaxU() : number.getMinU(), power == 9 ? number.getMaxV() : number.getMinV(),
        power == 9 ? number.getMinU() : number.getMaxU(), power == 9 ? number.getMinV() : number.getMaxV());
    return true;
  }

  private static boolean connects(BlockView w, int x, int y, int z, int side, boolean openAbove) {
    return class_408.method_1287(w, x, y, z, side)
        || (!w.method_1780(x, y, z) && class_408.method_1287(w, x, y - 1, z, -1))
        || (openAbove && climbs(w, x, y, z));
  }

  private static boolean climbs(BlockView w, int x, int y, int z) {
    return w.method_1780(x, y, z) && class_408.method_1287(w, x, y + 1, z, -1);
  }

  private static void flat(Tessellator t, double x0, double y, double z0, double x1, double z1,
      double u0, double v0, double u1, double v1) {
    t.vertex(x1, y, z1, u1, v1); t.vertex(x1, y, z0, u1, v0);
    t.vertex(x0, y, z0, u0, v0); t.vertex(x0, y, z1, u0, v1);
  }

  private static void horizontal(Tessellator t, Sprite s, double x, double y, double z, double from, double to) {
    t.vertex(x + to, y, z + 1, s.getMaxU(), s.getFrameV(to * 16));
    t.vertex(x + to, y, z, s.getMinU(), s.getFrameV(to * 16));
    t.vertex(x + from, y, z, s.getMinU(), s.getFrameV(from * 16));
    t.vertex(x + from, y, z + 1, s.getMaxU(), s.getFrameV(from * 16));
  }

  private static void side(Tessellator t, Sprite s, double x0, double y, double z0, double x1, double z1) {
    t.vertex(x0, y + 1.015625, z0, s.getMinU(), s.getMinV());
    t.vertex(x1, y + 1.015625, z1, s.getMaxU(), s.getMinV());
    t.vertex(x1, y, z1, s.getMaxU(), s.getMaxV());
    t.vertex(x0, y, z0, s.getMinU(), s.getMaxV());
  }
}
