package local.luke.power.ui;

/** Footer actions share measured bounds for rendering and pointer input. */
record OptionsFooterLayout(int right, int buttonWidth, int applyWidth) {
  static OptionsFooterLayout at(int span, int right, int autoApplyTextWidth) {
    int preferred = span < 470 ? 54 : 66;
    int apply = Math.max(preferred, autoApplyTextWidth + 16);
    int width = Math.max(36, Math.min(preferred, (span - apply - 24) / 2));
    return new OptionsFooterLayout(right, width, apply);
  }

  int width(int action) { return action == 1 ? applyWidth : buttonWidth; }

  int x(int action) {
    return switch (action) {
      case 0 -> right - 8 - buttonWidth * 2 - applyWidth - 8;
      case 1 -> right - 8 - buttonWidth - applyWidth - 4;
      case 2 -> right - 8 - buttonWidth;
      default -> throw new IllegalArgumentException("Unknown footer action");
    };
  }
}
