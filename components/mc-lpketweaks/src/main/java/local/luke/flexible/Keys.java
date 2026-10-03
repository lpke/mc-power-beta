package local.luke.flexible;

import net.minecraft.client.option.KeyBinding;

public final class Keys {
  private Keys() {}

  public static final KeyBinding[] ALL = {
    new KeyBinding("Flexible placement (toggle)", 0),
    new KeyBinding("Placement offset (hold)", 0),
    new KeyBinding("Placement adjacent (hold)", 0),
    new KeyBinding("Placement rotation (hold)", 0),
    new KeyBinding("Placement reverse (hold)", 0),
    new KeyBinding("Placement into face (hold)", 0)
  };
}
