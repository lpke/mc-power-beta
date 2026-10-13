package local.luke.power.ui;

import java.util.Collection;
import java.util.Set;

public final class SectionCollapse {
  private SectionCollapse() {}
  public static boolean allCollapsed(Collection<String> groups, Set<String> collapsed) {
    return !groups.isEmpty() && collapsed.containsAll(groups);
  }
  public static void toggleAll(Collection<String> groups, Set<String> collapsed) {
    if (allCollapsed(groups, collapsed)) collapsed.removeAll(groups);
    else collapsed.addAll(groups);
  }
}
