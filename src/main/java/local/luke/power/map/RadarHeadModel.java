package local.luke.power.map;

import java.util.List;

/** Native Beta head cuboids and UVs, detached from every live entity renderer and pose. */
final class RadarHeadModel {
  record Cuboid(int u, int v, double x, double y, double z,
      int width, int height, int depth, double dilation) {}
  record Head(List<Cuboid> base, List<Cuboid> overlay, double centerY, double centerZ,
      String overlayTexture, int overlayColor) {}
  private RadarHeadModel() {}
  private static Cuboid box(int u, int v, double x, double y, double z, int w, int h, int d) {
    return new Cuboid(u, v, x, y, z, w, h, d, 0);
  }
  private static Head head(Cuboid box, double y, double z) {
    return new Head(List.of(box), List.of(), y, z, "", 0xffffff);
  }
  static Head create(RadarIconSource source) {
    return switch (source.type()) {
      case "Player", "Skeleton", "Zombie", "Giant", "PigZombie" -> new Head(
          List.of(box(0, 0, -4, -8, -4, 8, 8, 8)),
          List.of(new Cuboid(32, 0, -4, -8, -4, 8, 8, 8, .5)), -4, 0, "", 0xffffff);
      case "Creeper" -> head(box(0, 0, -4, -8, -4, 8, 8, 8), -4, 0);
      case "Spider" -> head(box(32, 4, -4, -4, -8, 8, 8, 8), 0, -4);
      // Beta pigs have the original quadruped head, without a separate modern snout.
      case "Pig" -> head(box(0, 0, -4, -4, -8, 8, 8, 8), 0, -4);
      case "Cow" -> new Head(List.of(box(0, 0, -4, -4, -6, 8, 8, 6),
          box(22, 0, -4, -6, -3, 1, 3, 1), box(22, 0, 3, -6, -3, 1, 3, 1)),
          List.of(), 0, -3, "", 0xffffff);
      case "Chicken" -> new Head(List.of(box(0, 0, -2, -6, -2, 4, 6, 3),
          box(14, 0, -2, -4, -4, 4, 2, 2), box(14, 4, -1, -2, -3, 2, 2, 2)),
          List.of(), -3, -.5, "", 0xffffff);
      case "Wolf" -> new Head(List.of(box(0, 0, -3, -3, -2, 6, 6, 4),
          box(16, 14, -3, -5, 0, 2, 2, 1), box(16, 14, 1, -5, 0, 2, 2, 1),
          box(0, 10, -1.5, 0, -5, 3, 3, 4)), List.of(), 0, 0, "", 0xffffff);
      case "Sheep" -> new Head(List.of(box(0, 0, -3, -4, -6, 6, 6, 8)),
          source.wool() ? List.of(new Cuboid(0, 0, -3, -4, -4, 6, 6, 6, .6)) : List.of(),
          -1, -2, "/mob/sheep_fur.png", woolColor(source.woolColor()));
      case "Ghast" -> head(box(0, 0, -8, -8, -8, 16, 16, 16), 0, 0);
      case "Squid" -> head(box(0, 0, -6, -8, -6, 12, 16, 12), 0, 0);
      case "Slime" -> new Head(List.of(box(0, 16, -3, 17, -3, 6, 6, 6),
          box(32, 0, -3.25, 18, -3.5, 2, 2, 2), box(32, 4, 1.25, 18, -3.5, 2, 2, 2),
          box(32, 8, 0, 21, -3.5, 1, 1, 1)),
          List.of(box(0, 0, -4, 16, -4, 8, 8, 8)), 20, 0, "", 0xffffff);
      default -> null;
    };
  }
  private static int woolColor(int color) {
    int[] colors = {0xffffff, 0xf2b233, 0xe580d9, 0x99b2f2, 0xe5e533, 0x80cc1a,
        0xf2b2cc, 0x4d4d4d, 0x999999, 0x4d99b2, 0xb266e5, 0x3366cc,
        0x80664d, 0x668033, 0xcc4d4d, 0x1a1a1a};
    return colors[Math.max(0, Math.min(15, color))];
  }
}
