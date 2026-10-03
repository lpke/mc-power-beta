package local.luke.power.worldedit;

import net.minecraft.world.World;

/** Scoped to a single adapter write on the game thread. */
public final class EditScope {
  private static World current;

  public static boolean active(World world) {
    return world == current;
  }

  public static void run(World world, Runnable action) {
    World old = current;
    current = world;
    try {
      action.run();
    } finally {
      current = old;
    }
  }
}
