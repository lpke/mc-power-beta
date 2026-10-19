package local.luke.power.map;

import static local.luke.power.map.MapMath.*;

import java.util.Arrays;
import java.util.BitSet;

/** Mutable only on the game thread. Workers receive deep copies. Zero alpha means unexplored. */
public final class MapTile {
  public final Key key;
  final int[] accurate = new int[PIXELS], vanilla = new int[PIXELS], blocks = new int[PIXELS];
  final int[] reliefAccurate = new int[PIXELS], reliefVanilla = new int[PIXELS];
  private final BitSet dirtyRelief = new BitSet(PIXELS);
  final byte[] heights = new byte[PIXELS], lights = new byte[PIXELS], biomes = new byte[PIXELS];
  public long revision, savedRevision, textureRevision;
  public boolean readOnly;
  boolean propagateRelief;

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

  public int color(int i, MapSettings s) {
    if (!known(i)) return 0;
    int rgb =
        s.colourMode == 0 ? reliefAccurate[i] : reliefVanilla[i];
    double shade = Math.max(.9, Math.min(1, height(i) / 63d));
    if (s.caveLighting && key.layer() != -1) shade *= .35 + light(i) * .65 / 15;
    return 0xff000000 | MapMath.shade(rgb, shade);
  }

  public boolean put(
      int i, int color, int nativeColor, int height, int light, int block, int biome) {
    if (readOnly) return false;
    if (accurate[i] == color
        && vanilla[i] == nativeColor
        && height(i) == height
        && light(i) == light
        && blocks[i] == block
        && biome(i) == biome) return false;
    accurate[i] = color;
    vanilla[i] = nativeColor;
    heights[i] = (byte) height;
    lights[i] = (byte) light;
    blocks[i] = block;
    biomes[i] = (byte) biome;
    reliefAccurate[i] = color;
    reliefVanilla[i] = nativeColor;
    invalidateRelief(i);
    revision++;
    return true;
  }

  @FunctionalInterface
  interface Heights {
    int at(int x, int z);
  }

  void invalidateRelief(int i) {
    if (key.level() != 0 || readOnly) return;
    dirtyRelief.set(i);
    if (i + SIZE < PIXELS) dirtyRelief.set(i + SIZE);
    if (i % SIZE < SIZE - 1 && i + SIZE + 1 < PIXELS) dirtyRelief.set(i + SIZE + 1);
  }

  void invalidateReliefPixel(int i) {
    if (key.level() == 0 && !readOnly) dirtyRelief.set(i);
  }

  void invalidateAllRelief() {
    if (key.level() == 0 && !readOnly) dirtyRelief.set(0, PIXELS);
  }

  /** Reads neighboring cached heights only, never native terrain or disk. */
  boolean refreshRelief(Heights neighbors) {
    if (readOnly || key.level() != 0) return false;
    boolean changed = false;
    for (int i = dirtyRelief.nextSetBit(0); i >= 0; i = dirtyRelief.nextSetBit(i + 1)) {
      if (!known(i)) continue;
      int x = i % SIZE, z = i / SIZE, h = height(i);
      int north = adjacent(x, z - 1, h, neighbors);
      int northwest = adjacent(x - 1, z - 1, h, neighbors);
      int a = MapRelief.color(accurate[i], h, north, northwest, key.dimension());
      int v = MapRelief.color(vanilla[i], h, north, northwest, key.dimension());
      if (reliefAccurate[i] != a || reliefVanilla[i] != v) {
        reliefAccurate[i] = a;
        reliefVanilla[i] = v;
        changed = true;
      }
    }
    dirtyRelief.clear();
    if (changed) revision++;
    return changed;
  }

  private int adjacent(int x, int z, int fallback, Heights neighbors) {
    if (x >= 0 && z >= 0) {
      int i = z * SIZE + x;
      return known(i) ? height(i) : fallback;
    }
    int value = neighbors == null ? -1 : neighbors.at((int) key.minX() + x, (int) key.minZ() + z);
    return value < 0 ? fallback : value;
  }

  public boolean dirty() {
    return !readOnly && revision != savedRevision;
  }

  public MapTile copy() {
    MapTile tile = new MapTile(key);
    System.arraycopy(accurate, 0, tile.accurate, 0, PIXELS);
    System.arraycopy(vanilla, 0, tile.vanilla, 0, PIXELS);
    System.arraycopy(reliefAccurate, 0, tile.reliefAccurate, 0, PIXELS);
    System.arraycopy(reliefVanilla, 0, tile.reliefVanilla, 0, PIXELS);
    tile.dirtyRelief.or(dirtyRelief);
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
    if (readOnly) return false;
    child.refreshRelief(null);
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
            ra = 0,
            ga = 0,
            ba = 0,
            rv = 0,
            gv = 0,
            bv = 0,
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
            int sa = child.reliefAccurate[p], sv = child.reliefVanilla[p];
            ra += sa >> 16 & 255;
            ga += sa >> 8 & 255;
            ba += sa & 255;
            rv += sv >> 16 & 255;
            gv += sv >> 8 & 255;
            bv += sv & 255;
            h += child.height(p);
            l += child.light(p);
          }
        }
        if (count == 0) continue;
        int destination = (oz + z) * SIZE + ox + x;
        int shadedA = (ra / count) << 16 | (ga / count) << 8 | ba / count;
        int shadedV = (rv / count) << 16 | (gv / count) << 8 | bv / count;
        boolean reliefChanged =
            reliefAccurate[destination] != shadedA || reliefVanilla[destination] != shadedV;
        changed |=
            put(
                destination,
                (ar / count) << 16 | (ag / count) << 8 | ab / count,
                (vr / count) << 16 | (vg / count) << 8 | vb / count,
                h / count,
                l / count,
                child.block(source),
                child.biome(source));
        reliefAccurate[destination] = shadedA;
        reliefVanilla[destination] = shadedV;
        if (reliefChanged) {
          revision++;
          changed = true;
        }
      }
    return changed;
  }
}
