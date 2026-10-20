package local.luke.power.ui;

import java.util.*;

/** Visit-local presentation memory never writes gameplay preferences or editor drafts. */
final class HudEditorMemory {
  static String selected = "layout";
  static final Map<String, Double> scrolls = new HashMap<>();
  static final Set<String> collapsed = new HashSet<>(Set.of("minimap/Individual mob colors"));
  private HudEditorMemory() {}
}
