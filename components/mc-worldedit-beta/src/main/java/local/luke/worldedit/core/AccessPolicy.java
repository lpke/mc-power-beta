package local.luke.worldedit.core;

import local.luke.worldedit.config.WorldOverride;

public final class AccessPolicy {
  private AccessPolicy() {}

  public static boolean allows(
      boolean enabled,
      WorldOverride world,
      boolean integrate,
      boolean installed,
      boolean creative) {
    if (!enabled || world == WorldOverride.DISABLED) return false;
    if (world == WorldOverride.ENABLED) return true;
    return !integrate || !installed || creative;
  }
}
