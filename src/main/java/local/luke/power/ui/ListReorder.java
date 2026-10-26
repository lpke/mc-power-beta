package local.luke.power.ui;

import java.util.List;

/** A drop targets the gap before an item, including the gap after the final item. */
final class ListReorder {
  private ListReorder() {}

  static int slot(int pointerY, int top, int scroll, int rowHeight, int size) {
    return Math.max(0, Math.min(size, Math.floorDiv(pointerY - top + scroll + rowHeight / 2, rowHeight)));
  }

  static <T> boolean move(List<T> items, int from, int slot) {
    if (from < 0 || from >= items.size() || slot < 0 || slot > items.size())
      throw new IllegalArgumentException("Invalid reorder position");
    int target = slot > from ? slot - 1 : slot;
    if (target == from) return false;
    T item = items.remove(from);
    items.add(target, item);
    return true;
  }
}
