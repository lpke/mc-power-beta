package local.luke.power.ui;

import java.util.function.ToIntFunction;

/** A single measured row shared by zoom-editor rendering and clicks. */
record ZoomStepsFooter(int[] x, int[] width) {
  static final String[] LABELS = {"Add level", "Defaults", "Undo", "Done", "Cancel"};

  static ZoomStepsFooter at(int left, int span, ToIntFunction<String> measure) {
    int[] widths = java.util.Arrays.stream(LABELS).mapToInt(measure).toArray();
    int padding = Math.max(8, Math.min(16, (span - java.util.Arrays.stream(widths).sum() - 16) / 5));
    for (int i = 0; i < widths.length; i++) widths[i] += padding;
    int[] x = new int[5];
    x[0] = left;
    for (int i = 1; i < 3; i++) x[i] = x[i - 1] + widths[i - 1] + 4;
    x[4] = left + span - widths[4];
    x[3] = x[4] - widths[3] - 4;
    return new ZoomStepsFooter(x, widths);
  }
}
