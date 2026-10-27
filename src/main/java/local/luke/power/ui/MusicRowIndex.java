package local.luke.power.ui;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/** Immutable row geometry prepared when a music list changes, not while it renders. */
final class MusicRowIndex {
  private final int[] starts, ends, headings;

  private MusicRowIndex(int[] starts, int[] ends, int[] headings) {
    this.starts = starts; this.ends = ends; this.headings = headings;
  }

  static <T> MusicRowIndex of(List<T> rows, Predicate<T> heading,
      ToIntFunction<T> rowY, ToIntFunction<T> rowHeight) {
    int[] starts = new int[rows.size()], ends = new int[rows.size()], headings = new int[rows.size()];
    int count = 0;
    for (int i = 0; i < rows.size(); i++) {
      T row = rows.get(i);
      starts[i] = rowY.applyAsInt(row);
      int height = rowHeight.applyAsInt(row);
      if (height <= 0 || i > 0 && starts[i] < ends[i - 1])
        throw new IllegalArgumentException("Music rows must have positive height and ordered, separate bounds");
      ends[i] = Math.addExact(starts[i], height);
      if (heading.test(row)) headings[count++] = i;
    }
    return new MusicRowIndex(starts, ends, Arrays.copyOf(headings, count));
  }

  /** First row whose lower edge is below the viewport's document-space upper edge. */
  int firstVisible(int documentTop) {
    int low = 0, high = ends.length;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (ends[middle] <= documentTop) low = middle + 1; else high = middle;
    }
    return low;
  }

  /** Exclusive row limit at the viewport's document-space lower edge. */
  int limitVisible(int documentBottom) {
    int low = 0, high = starts.length;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (starts[middle] < documentBottom) low = middle + 1; else high = middle;
    }
    return low;
  }

  int at(int documentY) {
    int row = firstVisible(documentY);
    return row < starts.length && starts[row] <= documentY ? row : -1;
  }

  StickyHeader sticky(int scroll, int top, int bottom) {
    int low = 0, high = headings.length;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (starts[headings[middle]] < scroll) low = middle + 1; else high = middle;
    }
    if (low == 0) return null;
    int candidate = headings[low - 1];
    int last = low < headings.length ? headings[low] - 1 : starts.length - 1;
    if (last - candidate < 8) return null;
    int height = ends[candidate] - starts[candidate];
    if (bottom - top < height + ends[candidate + 1] - starts[candidate + 1]) return null;
    int y = Math.min(top, top + starts[last] - scroll - height);
    int naturalY = top + starts[candidate] - scroll;
    return y <= naturalY || y + height <= top ? null : new StickyHeader(candidate, y, height);
  }
}
