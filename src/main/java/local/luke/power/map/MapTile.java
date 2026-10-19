package local.luke.power.map;

import static local.luke.power.map.MapMath.*;

import java.util.Arrays;

/** Mutable only on the game thread. Workers receive deep copies. Zero alpha means unexplored. */
public final class MapTile {
  public final Key key;
  final int[] accurate = new int[PIXELS], vanilla = new int[PIXELS], blocks = new int[PIXELS];
  final byte[] heights = new byte[PIXELS], lights = new byte[PIXELS], biomes = new byte[PIXELS];
  public long revision, savedRevision, textureRevision;
  public boolean readOnly;

  public MapTile(Key key) {
    this.key = key;
    Arrays.fill(heights, (byte) 255);
  }

  public boolean known(int i) {
    return heights[i] != (byte) 255;
  }

  public int height(int i) {
    return heights[i] & 255;
  }

  public int block(int i) {
    return blocks[i];
  }

  public int light(int i) {
    return lights[i] & 15;
  }

  public int biome(int i) {
    return biomes[i] & 255;
  }

  public int colour(int i, MapSettings s) {
    if (!known(i)) return 0;
    int rgb = s.colourMode == 0 ? accurate[i] : vanilla[i];
    double shade = 1;
    if (s.heightShading) shade *= .82 + height(i) / 350.0;
    if (s.slopeShading) {
      int north = i >= SIZE && known(i - SIZE) ? height(i - SIZE) : height(i);
      shade *= Math.max(.62, Math.min(1.22, 1 + (north - height(i)) * .055));
    }
    if (s.caveLighting && key.layer() != -1) shade *= .35 + light(i) * .65 / 15;
    return 0xff000000 | MapMath.shade(rgb, shade);
  }

  public boolean put(
      int i, int colour, int nativeColour, int height, int light, int block, int biome) {
    if (readOnly) return false;
    if (accurate[i] == colour
        && vanilla[i] == nativeColour
        && height(i) == height
        && light(i) == light
        && blocks[i] == block
        && biome(i) == biome) return false;
    accurate[i] = colour;
    vanilla[i] = nativeColour;
    heights[i] = (byte) height;
    lights[i] = (byte) light;
    blocks[i] = block;
    biomes[i] = (byte) biome;
    revision++;
    return true;
  }

  public boolean dirty() {
    return !readOnly && revision != savedRevision;
  }

  public MapTile copy() {
    MapTile tile = new MapTile(key);
    System.arraycopy(accurate, 0, tile.accurate, 0, PIXELS);
    System.arraycopy(vanilla, 0, tile.vanilla, 0, PIXELS);
    System.arraycopy(blocks, 0, tile.blocks, 0, PIXELS);
    System.arraycopy(heights, 0, tile.heights, 0, PIXELS);
    System.arraycopy(lights, 0, tile.lights, 0, PIXELS);
    System.arraycopy(biomes, 0, tile.biomes, 0, PIXELS);
    tile.revision = revision;
    tile.savedRevision = savedRevision;
    tile.readOnly = readOnly;
    return tile;
  }

  /** Update one child quadrant, preserving the other three quadrants loaded from disk. */
  public boolean merge(MapTile child) {
    if (!key.equals(child.key.parent())) throw new IllegalArgumentException("Wrong map parent");
    int ox = Math.floorMod(child.key.x(), 2) * SIZE / 2,
        oz = Math.floorMod(child.key.z(), 2) * SIZE / 2;
    boolean changed = false;
    for (int z = 0; z < SIZE / 2; z++)
      for (int x = 0; x < SIZE / 2; x++) {
        int first = z * 2 * SIZE + x * 2,
            count = 0,
            ar = 0,
            ag = 0,
            ab = 0,
            vr = 0,
            vg = 0,
            vb = 0,
            h = 0,
            l = 0,
            source = first;
        for (int n = 0; n < 4; n++) {
          int p = first + (n & 1) + (n >> 1) * SIZE;
          if (child.known(p)) {
            int a = child.accurate[p], v = child.vanilla[p];
            count++;
            source = p;
            ar += a >> 16 & 255;
            ag += a >> 8 & 255;
            ab += a & 255;
            vr += v >> 16 & 255;
            vg += v >> 8 & 255;
            vb += v & 255;
            h += child.height(p);
            l += child.light(p);
          }
        }
        if (count == 0) continue;
        changed |=
            put(
                (oz + z) * SIZE + ox + x,
                (ar / count) << 16 | (ag / count) << 8 | ab / count,
                (vr / count) << 16 | (vg / count) << 8 | vb / count,
                h / count,
                l / count,
                child.block(source),
                child.biome(source));
      }
    return changed;
  }
}
