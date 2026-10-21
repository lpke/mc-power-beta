package local.luke.power.inventory;

import java.util.Objects;

/** Game-thread coordination only. A lease never replaces native stack/owner revalidation. */
public final class InventoryActions {
  private static Thread thread;
  private static long generation;
  private static Lease active;

  private InventoryActions() {}

  /** Invoke explicitly at the native game-loop entry, before exposing inventory callbacks. */
  public static void initializeGameThread() {
    if (thread == null) thread = Thread.currentThread();
    gameThread();
  }

  /** Manual actions supersede pending automation; automatic work never queues behind another plan. */
  public static Lease begin(Object world, Object player, Object handler, Object inventory,
      String action, boolean manual) {
    gameThread();
    Objects.requireNonNull(action);
    if (world == null || player == null || handler == null || inventory == null) return null;
    if (active != null && !manual) return null;
    cancel();
    return active = new Lease(world, player, handler, inventory, action, generation);
  }

  /** Call on focus, screen, world or explicit inventory-action ownership changes. */
  public static void cancel() {
    gameThread();
    active = null;
    generation++;
  }

  public static boolean busy() {
    gameThread();
    return active != null;
  }

  public static void finish(Lease lease) {
    gameThread();
    if (lease != null && active == lease) active = null;
  }

  private static void gameThread() {
    if (thread == null) throw new IllegalStateException("Inventory action thread is not initialized");
    if (thread != Thread.currentThread())
      throw new IllegalStateException("Inventory actions belong to the game thread");
  }

  public static final class Lease {
    private final Object world, player, handler, inventory;
    private final long generation;
    public final String action;

    private Lease(Object world, Object player, Object handler, Object inventory, String action, long generation) {
      this.world = world; this.player = player; this.handler = handler; this.inventory = inventory;
      this.action = action; this.generation = generation;
    }

    public boolean valid(Object world, Object player, Object handler, Object inventory) {
      gameThread();
      return active == this && InventoryActions.generation == generation && this.world == world
          && this.player == player && this.handler == handler && this.inventory == inventory;
    }
  }
}
