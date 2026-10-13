package local.luke.power.slabplacement;

import java.io.IOException;
import local.luke.power.slabplacement.config.*;

public final class SlabPlacement {
  private SlabPlacement() {}

  public static Settings settings() {
    return local.luke.power.building.config.Config.current().slabs.copy();
  }

  public static void apply(Settings value) throws IOException {
    local.luke.power.building.config.Config.update(s -> s.slabs = value.copy());
  }

  public static boolean enabled() {
    return local.luke.power.building.config.Config.current().slabs.enabled;
  }
}
