package local.luke.power.worldedit.core;

import java.util.*;

public final class Clipboard {
  private final Map<Pos, BlockValue> relative;

  public Clipboard(Map<Pos, BlockValue> relative) {
    this.relative = Map.copyOf(relative);
  }

  public Map<Pos, BlockValue> blocks() {
    return relative;
  }

  public Clipboard rotate(int quarterTurns) {
    Map<Pos, BlockValue> out = new LinkedHashMap<>();
    int n = Math.floorMod(quarterTurns, 4);
    for (var e : relative.entrySet()) {
      Pos p = e.getKey();
      BlockValue b = e.getValue();
      for (int i = 0; i < n; i++) {
        p = new Pos(-p.z(), p.y(), p.x());
        b = BlockRotation.rotate(b);
      }
      out.put(p, b);
    }
    return new Clipboard(out);
  }

  public Clipboard flip(char axis) {
    Map<Pos, BlockValue> out = new LinkedHashMap<>();
    for (var e : relative.entrySet()) {
      Pos p = e.getKey();
      if (axis == 'y' && BlockRotation.verticalSensitive(e.getValue()))
        throw new IllegalArgumentException(
            "Vertical flip cannot preserve this Beta block's orientation.");
      p =
          new Pos(
              axis == 'x' ? -p.x() : p.x(),
              axis == 'y' ? -p.y() : p.y(),
              axis == 'z' ? -p.z() : p.z());
      out.put(p, BlockRotation.flip(e.getValue(), axis));
    }
    return new Clipboard(out);
  }
}
