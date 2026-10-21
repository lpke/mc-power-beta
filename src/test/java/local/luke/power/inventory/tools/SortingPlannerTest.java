package local.luke.power.inventory.tools;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SortingPlannerTest {
  private SortingPlanner.Cell item(String identity, int id, int count) { return item(identity, id, count, 64, count > 64); }
  private SortingPlanner.Cell item(String identity, int id, int count, int limit, boolean pinned) {
    return new SortingPlanner.Cell(new SortingPlanner.Item(identity, "full:" + identity, id, 0, count, 64, true), limit, pinned);
  }
  private SortingPlanner.Cell empty() { return new SortingPlanner.Cell(null, 64, false); }
  private Map<String, Long> totals(List<SortingPlanner.Cell> cells) {
    Map<String, Long> counts = new HashMap<>();
    for (var cell : cells) if (cell.item() != null) counts.merge(cell.item().identity(), (long)cell.item().count(), Long::sum);
    return counts;
  }
  @Test void stableOrderingCompactsAndMergesOnlyEqualCompleteIdentities() {
    var before = List.of(item("id2:nameA", 2, 7), empty(), item("id1:tagB", 1, 20), item("id1:tagA", 1, 30), item("id1:tagB", 1, 50));
    var after = SortingPlanner.sort(before);
    assertEquals("id1:tagA", after.get(0).item().identity()); assertEquals(30, after.get(0).item().count());
    assertEquals("id1:tagB", after.get(1).item().identity()); assertEquals(64, after.get(1).item().count());
    assertEquals(6, after.get(2).item().count()); assertEquals("id2:nameA", after.get(3).item().identity()); assertNull(after.get(4).item());
    assertEquals(totals(before), totals(after)); assertEquals(after, SortingPlanner.sort(after));
  }
  @Test void originalOversizedStacksStayPinnedWithoutSplittingOrMerging() {
    var oversize = item("protected", 99, 1000); var before = List.of(oversize, item("b", 2, 3), item("a", 1, 2), empty());
    var after = SortingPlanner.sort(before); assertSame(oversize, after.get(0)); assertEquals("a", after.get(1).item().identity());
    assertEquals(totals(before), totals(after));
  }
  @Test void perSlotLimitsAreAppliedBeforePublication() {
    var before = List.of(new SortingPlanner.Cell(null, 4, false), item("same", 1, 60));
    var after = SortingPlanner.sort(before); assertEquals(4, after.get(0).item().count()); assertEquals(56, after.get(1).item().count());
  }
  @Test void unstackableNativeDataIsNeverMerged() {
    var tool = new SortingPlanner.Item("damaged:tool", "nativeData", 257, 150, 1, 1, false);
    var after = SortingPlanner.sort(List.of(new SortingPlanner.Cell(tool, 64, false), empty(), new SortingPlanner.Cell(tool, 64, false)));
    assertEquals(1, after.get(0).item().count()); assertEquals(1, after.get(1).item().count()); assertNull(after.get(2).item());
  }
  @Test void depositFreezesOriginalDestinationTypesAndPreservesUnmatchedSourceData() {
    var source = List.of(item("a:fullNBT", 1, 10), item("a:otherNBT", 1, 11), item("b", 2, 12));
    var destination = List.of(item("a:fullNBT", 1, 63), empty(), empty());
    var plan = SortingPlanner.deposit(source, destination);
    assertNull(plan.source().get(0).item()); assertEquals(source.get(1), plan.source().get(1)); assertEquals(source.get(2), plan.source().get(2));
    assertEquals(64, plan.destination().get(0).item().count()); assertEquals(9, plan.destination().get(1).item().count()); assertNull(plan.destination().get(2).item());
    assertEquals("a:fullNBT", plan.destination().get(1).item().identity());
  }
  @Test void emptyDestinationAcceptsNoInventedMatchingTypes() {
    var source = List.of(item("a", 1, 3), item("b", 2, 4)); var empty = List.of(empty(), empty());
    var plan = SortingPlanner.deposit(source, empty); assertEquals(source, plan.source()); assertEquals(empty, plan.destination());
  }
  @Test void fullDestinationLeavesRemaindersAndOversizedSourceUntouched() {
    var source = List.of(item("a", 1, 10), item("a", 1, 1000)); var destination = List.of(item("a", 1, 63));
    var plan = SortingPlanner.deposit(source, destination); assertEquals(9, plan.source().get(0).item().count());
    assertEquals(source.get(1), plan.source().get(1)); assertEquals(64, plan.destination().get(0).item().count());
  }
  @Test void pinnedDestinationRemainsIntactButOriginalIdentityCanFillAnEmptyCell() {
    var pinned = item("a", 1, 1000); var source = List.of(item("a", 1, 17));
    var plan = SortingPlanner.deposit(source, List.of(pinned, empty())); assertSame(pinned, plan.destination().get(0));
    assertEquals(17, plan.destination().get(1).item().count()); assertNull(plan.source().get(0).item());
  }
  @Test void randomizedSortsConserveNativeIdentitiesLimitsAndAreIdempotent() {
    Random random = new Random(400);
    for (int attempt = 0; attempt < 256; attempt++) {
      List<SortingPlanner.Cell> before = new ArrayList<>();
      for (int i = 0; i < 54; i++) { int id = random.nextInt(12); before.add(id == 0 ? empty() : item("id" + id + ":tag" + random.nextInt(3), id, 1 + random.nextInt(64))); }
      var after = SortingPlanner.sort(before); assertEquals(totals(before), totals(after)); assertEquals(after, SortingPlanner.sort(after));
      for (var cell : after) if (cell.item() != null) assertTrue(cell.item().count() <= Math.min(cell.limit(), cell.item().limit()));
    }
  }
  @Test void randomizedDepositsConserveBothInventoriesAndUseOnlyOriginalTypes() {
    Random random = new Random(401);
    for (int attempt = 0; attempt < 128; attempt++) {
      List<SortingPlanner.Cell> source = new ArrayList<>(), destination = new ArrayList<>(); Set<String> allowed = new HashSet<>();
      for (int i = 0; i < 27; i++) source.add(item("id" + random.nextInt(8), 1, 1 + random.nextInt(64)));
      for (int i = 0; i < 54; i++) { var cell = random.nextBoolean() ? empty() : item("id" + random.nextInt(4), 1, 1 + random.nextInt(64)); destination.add(cell); if (cell.item() != null) allowed.add(cell.item().identity()); }
      var plan = SortingPlanner.deposit(source, destination); List<SortingPlanner.Cell> before = new ArrayList<>(source); before.addAll(destination);
      List<SortingPlanner.Cell> after = new ArrayList<>(plan.source()); after.addAll(plan.destination()); assertEquals(totals(before), totals(after));
      for (var cell : plan.destination()) if (cell.item() != null) assertTrue(allowed.contains(cell.item().identity()));
    }
  }
  @Test void protectedSlotsAndPreferredItemsKeepTheirPositionsAndAllNativeData() {
    var pinned = new SortingPlanner.Cell(item("protected", 9, 4).item(), 64, true);
    var before = List.of(item("low:tag", 1, 12), pinned, empty(), item("preferred:tag", 7, 50), item("preferred:tag", 7, 30));
    var preferences = Map.of(0, new SortingPlanner.Preference(7, -1));
    var after = SortingPlanner.sort(before, false, preferences);
    assertEquals("preferred:tag", after.get(0).item().identity()); assertEquals(64, after.get(0).item().count());
    assertSame(pinned, after.get(1)); assertEquals("low:tag", after.get(2).item().identity());
    assertEquals(16, after.get(3).item().count()); assertEquals(totals(before), totals(after));
    assertEquals(after, SortingPlanner.sort(after, false, preferences));
  }
  @Test void preferredMetadataMatchesExactlyAndMissingPreferencesLeaveCapacityAvailable() {
    var variant = new SortingPlanner.Item("variant:5:nbt", "native:5", 35, 5, 12, 64, true);
    var before = List.of(item("any:nbt", 35, 6), empty(), new SortingPlanner.Cell(variant, 64, false));
    var preferences = Map.of(0, new SortingPlanner.Preference(35, 5), 2, new SortingPlanner.Preference(999, -1));
    var after = SortingPlanner.sort(before, false, preferences);
    assertEquals("variant:5:nbt", after.get(0).item().identity()); assertEquals("any:nbt", after.get(1).item().identity());
    assertNull(after.get(2).item()); assertEquals(totals(before), totals(after));
  }
  @Test void reverseOrderingKeepsPreferredAndProtectedSlotsIndependent() {
    var pinned = new SortingPlanner.Cell(item("pinned", 99, 7).item(), 64, true);
    var before = List.of(item("a", 1, 4), item("c", 3, 5), pinned, item("b", 2, 6));
    var after = SortingPlanner.sort(before, true, Map.of(0, new SortingPlanner.Preference(1, -1)));
    assertEquals(1, after.get(0).item().id()); assertEquals(3, after.get(1).item().id());
    assertSame(pinned, after.get(2)); assertEquals(2, after.get(3).item().id());
    assertEquals(after, SortingPlanner.sort(after, true, Map.of(0, new SortingPlanner.Preference(1, -1))));
  }
  @Test void preferredSlotsDoNotCombineDifferentNativeIdentities() {
    var before = List.of(empty(), item("a:nbt", 1, 40), item("b:nbt", 1, 50));
    var after = SortingPlanner.sort(before, false, Map.of(0, new SortingPlanner.Preference(1, -1)));
    assertEquals(40, after.get(0).item().count()); assertEquals("a:nbt", after.get(0).item().identity());
    assertEquals(50, after.get(1).item().count()); assertEquals("b:nbt", after.get(1).item().identity());
    assertEquals(totals(before), totals(after));
  }
  @Test void unsupportedOrUnpinnedOversizedInputRefusesWithoutMutatingTheCaller() {
    var before = List.of(item("oversize", 1, 65, 64, false)); assertThrows(IllegalArgumentException.class, () -> SortingPlanner.sort(before));
    assertEquals(65, before.get(0).item().count()); assertThrows(IllegalArgumentException.class, () -> SortingPlanner.sort(Collections.nCopies(129, empty())));
  }
}
