package local.luke.power.input;

import local.luke.power.permissions.CommandPermissions;

/** A visit-local damage assist, scoped to the exact player and world. */
public final class InstantKill {
  private static Object player, world;

  private InstantKill() {}

  public static boolean toggle(Object owner, Object level) {
    boolean enabled = !active(owner, level);
    player = enabled ? owner : null;
    world = enabled ? level : null;
    return enabled;
  }

  public static boolean active(Object owner, Object level) {
    if (!CommandPermissions.allowed("instakill")) clear();
    return owner != null && level != null && player == owner && world == level;
  }

  public static void clear() { player = world = null; }
}
