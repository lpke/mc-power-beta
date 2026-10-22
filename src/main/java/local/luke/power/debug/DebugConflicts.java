package local.luke.power.debug;

public final class DebugConflicts {
  private DebugConflicts() {}
  public static boolean overlap(String first,String second) {
    boolean a=first.startsWith("keys.powerbeta.debug."), b=second.startsWith("keys.powerbeta.debug.");
    if(a!=b) return false; // The debug layer owns its chord; ordinary actions own plain presses.
    return !(first.endsWith(".location") && second.endsWith(".crash") || first.endsWith(".crash") && second.endsWith(".location"));
  }
}
