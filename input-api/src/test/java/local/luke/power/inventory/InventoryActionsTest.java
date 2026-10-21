package local.luke.power.inventory;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class InventoryActionsTest {
  private final Object world = new Object(), player = new Object(), handler = new Object(), inventory = new Object();
  @BeforeEach void initialize() { InventoryActions.initializeGameThread(); }
  @AfterEach void clear() { InventoryActions.cancel(); }

  @Test void automaticWorkDoesNotQueueOrInvalidateAnExistingLease() {
    var first = begin(false);
    assertNull(begin(false));
    assertTrue(first.valid(world, player, handler, inventory));
  }
  @Test void manualActionSupersedesPendingWorkAndOldCompletionCannotReleaseTheNewOwner() {
    var first = begin(false); var second = begin(true);
    InventoryActions.finish(first);
    assertFalse(first.valid(world, player, handler, inventory));
    assertTrue(second.valid(world, player, handler, inventory));
    assertTrue(InventoryActions.busy());
  }
  @Test void everyNativeOwnerIdentityIsRequired() {
    var lease = begin(true);
    assertFalse(lease.valid(new Object(), player, handler, inventory));
    assertFalse(lease.valid(world, new Object(), handler, inventory));
    assertFalse(lease.valid(world, player, new Object(), inventory));
    assertFalse(lease.valid(world, player, handler, new Object()));
    assertTrue(lease.valid(world, player, handler, inventory));
  }
  @Test void cancellationAndCompletionPermanentlyRetireLeases() {
    var lease = begin(true); InventoryActions.cancel();
    assertFalse(lease.valid(world, player, handler, inventory));
    var next = begin(false); InventoryActions.finish(next);
    assertFalse(next.valid(world, player, handler, inventory));
    assertFalse(InventoryActions.busy());
  }
  @Test void missingOwnerCannotCancelAValidPlan() {
    var lease = begin(true);
    assertNull(InventoryActions.begin(null, player, handler, inventory, "invalid", true));
    assertTrue(lease.valid(world, player, handler, inventory));
  }
  @Test void workersCannotAcquireInventoryOwnership() throws Exception {
    begin(true);
    var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
    Thread worker = new Thread(() -> { try { begin(true); } catch (Throwable error) { failure.set(error); } });
    worker.start(); worker.join();
    assertInstanceOf(IllegalStateException.class, failure.get());
  }
  private InventoryActions.Lease begin(boolean manual) {
    return InventoryActions.begin(world, player, handler, inventory, "test", manual);
  }
}
