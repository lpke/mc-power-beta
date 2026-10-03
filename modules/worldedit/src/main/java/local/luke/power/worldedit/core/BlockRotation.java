package local.luke.power.worldedit.core;

public final class BlockRotation {
  private static int facing(int m) {
    return switch (m) {
      case 2 -> 5;
      case 5 -> 3;
      case 3 -> 4;
      case 4 -> 2;
      default -> m;
    };
  }

  public static BlockValue rotate(BlockValue b) {
    int m = b.meta;
    switch (b.id) {
      case 23, 54, 61, 62, 65, 68 -> m = facing(m);
      case 29, 33 -> m = (m & 8) | facing(m & 7);
      case 53, 67 ->
          m =
              switch (m) {
                case 0 -> 2;
                case 2 -> 1;
                case 1 -> 3;
                case 3 -> 0;
                default -> m;
              };
      case 26, 86, 91, 93, 94 -> m = (m & 12) | ((m + 1) & 3);
      case 63 -> m = (m + 4) & 15;
      case 50, 75, 76, 77 ->
          m =
              (m & 8)
                  | switch (m & 7) {
                    case 1 -> 3;
                    case 3 -> 2;
                    case 2 -> 4;
                    case 4 -> 1;
                    default -> m & 7;
                  };
      case 64, 71 -> {
        if ((m & 8) == 0) m = (m & 12) | ((m + 1) & 3);
      }
      case 96 ->
          m =
              (m & 12)
                  | switch (m & 3) {
                    case 0 -> 3;
                    case 3 -> 1;
                    case 1 -> 2;
                    default -> 0;
                  };
      case 66 ->
          m =
              switch (m) {
                case 0 -> 1;
                case 1 -> 0;
                case 2 -> 5;
                case 5 -> 3;
                case 3 -> 4;
                case 4 -> 2;
                case 6 -> 7;
                case 7 -> 8;
                case 8 -> 9;
                case 9 -> 6;
                default -> m;
              };
      case 27, 28 ->
          m =
              (m & 8)
                  | switch (m & 7) {
                    case 0 -> 1;
                    case 1 -> 0;
                    case 2 -> 5;
                    case 5 -> 3;
                    case 3 -> 4;
                    case 4 -> 2;
                    default -> m & 7;
                  };
      case 69 ->
          m =
              (m & 8)
                  | switch (m & 7) {
                    case 1 -> 3;
                    case 3 -> 2;
                    case 2 -> 4;
                    case 4 -> 1;
                    case 5 -> 6;
                    case 6 -> 5;
                    case 0 -> 7;
                    case 7 -> 0;
                    default -> m & 7;
                  };
      default -> {}
    }
    return b.withMeta(m);
  }

  public static boolean verticalSensitive(BlockValue b) {
    return switch (b.id) {
      case 26, 44, 53, 67, 64, 71, 96, 66, 27, 28, 50, 75, 76, 77, 69, 63, 68, 65 -> true;
      default -> false;
    };
  }

  public static BlockValue flip(BlockValue b, char axis) {
    // Mirror metadata by exchanging the two opposing directions on the requested axis.
    int m = b.meta;
    int low = m & 7;
    if (b.id == 29
        || b.id == 33
        || b.id == 23
        || b.id == 54
        || b.id == 61
        || b.id == 62
        || b.id == 65
        || b.id == 68) {
      int a = axis == 'x' ? 4 : axis == 'z' ? 2 : 0, other = a + 1;
      if (low == a) m = (m & 8) | other;
      else if (low == other) m = (m & 8) | a;
    } else if (axis == 'y') return b;
    else if (b.id == 53 || b.id == 67) {
      int a = axis == 'x' ? 0 : 2;
      if (m == a) m = a + 1;
      else if (m == a + 1) m = a;
    } else if (b.id == 86 || b.id == 91 || b.id == 93 || b.id == 94) {
      int d = m & 3;
      if (axis == 'z' && d % 2 == 0 || axis == 'x' && d % 2 == 1) m = (m & 12) | ((d + 2) & 3);
    } else if (b.id == 63) m = (axis == 'x' ? -m : 8 - m) & 15;
    else if (verticalSensitive(b) && b.id != 44)
      throw new IllegalArgumentException(
          "Flip cannot preserve block " + b.id + " metadata; use //rotate instead.");
    return b.withMeta(m);
  }
}
