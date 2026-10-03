package local.luke.power.building;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import local.luke.power.building.hotbar.*;
import org.junit.jupiter.api.Test;

class InventoryExchangeTest {
  private final Object[] source = new Object[36];
  private final AtomicReference<Object[]> live = new AtomicReference<>(source);

  private Object[] planned() {
    source[0] = new Object();
    source[9] = new Object();
    return InventoryRows.swapped(source, 0);
  }

  @Test
  void publishesOneArrayOnlyAfterBackupCompletes() throws Exception {
    Object[] next = planned(), before = source.clone();
    InventoryExchange.commit(
        live::get,
        live::set,
        source,
        before,
        next,
        before,
        () -> assertSame(source, live.get()),
        () -> assertSame(next, live.get()));
    assertSame(next, live.get());
    assertArrayEquals(before, source);
    assertSame(before[9], live.get()[0]);
    assertSame(before[0], live.get()[9]);
  }

  @Test
  void diskFailureLeavesOriginalUntouched() {
    Object[] next = planned(), before = source.clone();
    assertThrows(
        IOException.class,
        () ->
            InventoryExchange.commit(
                live::get,
                live::set,
                source,
                before,
                next,
                before,
                () -> {
                  throw new IOException("disk full");
                },
                () -> fail("Must not publish")));
    assertSame(source, live.get());
    assertArrayEquals(before, source);
  }

  @Test
  void refusesStateChangedDuringBackupWithoutOverwritingIt() {
    Object[] next = planned(), before = source.clone(), external = new Object[36];
    assertThrows(
        IllegalStateException.class,
        () ->
            InventoryExchange.commit(
                live::get,
                live::set,
                source,
                before,
                next,
                before,
                () -> live.set(external),
                () -> fail("Must not publish")));
    assertSame(external, live.get());
    assertArrayEquals(before, source);
  }

  @Test
  void detectsInPlaceMutationDuringBackup() {
    Object[] next = planned(), before = source.clone();
    Object external = new Object();
    assertThrows(
        IllegalStateException.class,
        () ->
            InventoryExchange.commit(
                live::get,
                live::set,
                source,
                before,
                next,
                before,
                () -> source[5] = external,
                () -> fail("Must not publish")));
    assertSame(source, live.get());
    assertSame(external, source[5]);
  }

  @Test
  void verificationFailureRestoresPreallocatedRecoveryArray() {
    Object[] next = planned(), before = source.clone(), rollback = source.clone();
    assertThrows(
        IOException.class,
        () ->
            InventoryExchange.commit(
                live::get,
                live::set,
                source,
                before,
                next,
                rollback,
                () -> {},
                () -> {
                  next[0] = null;
                  throw new IOException("injected post-commit failure");
                }));
    assertSame(rollback, live.get());
    assertArrayEquals(before, live.get());
  }

  @Test
  void publicationFailureAndErrorsAlsoRollBack() {
    for (boolean error : new boolean[] {false, true}) {
      live.set(source);
      Object[] next = planned(), before = source.clone();
      assertThrows(
          Throwable.class,
          () ->
              InventoryExchange.commit(
                  live::get,
                  a -> {
                    live.set(a);
                    if (a == next) {
                      if (error) throw new AssertionError("injected error");
                      throw new IllegalStateException("injected failure");
                    }
                  },
                  source,
                  before,
                  next,
                  before,
                  () -> {},
                  () -> {}));
      assertSame(before, live.get());
    }
  }

  @Test
  void rollbackDoesNotOverwriteAnUnrelatedReplacementInventory() {
    Object[] next = planned(), before = source.clone(), other = new Object[36];
    assertThrows(
        IllegalStateException.class,
        () ->
            InventoryExchange.commit(
                live::get,
                live::set,
                source,
                before,
                next,
                before,
                () -> {},
                () -> live.set(other)));
    assertSame(other, live.get());
    assertArrayEquals(before, source);
  }
}
