package local.luke.power.ui;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/** Shared drawing and hit-test geometry. The final item pushes its heading out of the way. */
public record StickyHeader(int index, int y, int height) {
  public boolean contains(int mouseY, int top) {
    return mouseY >= Math.max(top, y) && mouseY < y + height;
  }

  public static <T> StickyHeader at(List<T> rows, Predicate<T> heading,
      ToIntFunction<T> rowY, ToIntFunction<T> rowHeight, int scroll, int top, int bottom) {
    int candidate = -1;
    for (int i = 0; i < rows.size(); i++) {
      T row = rows.get(i);
      if (rowY.applyAsInt(row) >= scroll) break;
      if (heading.test(row)) candidate = i;
    }
    if (candidate < 0) return null;
    int last = candidate;
    while (last + 1 < rows.size() && !heading.test(rows.get(last + 1))) last++;
    if (last - candidate < 8) return null;
    int height = rowHeight.applyAsInt(rows.get(candidate));
    if (bottom - top < height + rowHeight.applyAsInt(rows.get(candidate + 1))) return null;
    int y = Math.min(top, top + rowY.applyAsInt(rows.get(last)) - scroll - height);
    int naturalY = top + rowY.applyAsInt(rows.get(candidate)) - scroll;
    return y <= naturalY || y + height <= top ? null : new StickyHeader(candidate, y, height);
  }
}
