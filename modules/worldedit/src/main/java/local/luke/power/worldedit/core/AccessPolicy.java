package local.luke.power.worldedit.core;

import local.luke.power.worldedit.config.WorldOverride;

public final class AccessPolicy {
  private AccessPolicy() {}
  public static boolean allows(boolean cheats, boolean enabled, WorldOverride world) {
    return cheats && enabled && world != WorldOverride.DISABLED;
  }
}
