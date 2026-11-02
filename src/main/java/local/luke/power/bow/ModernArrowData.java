package local.luke.power.bow;

/** Marks only arrows fired by the opted-in local bow. Skeletons and dispensers stay native. */
public interface ModernArrowData {
  void power$modernArrow(boolean critical, boolean creative);
  boolean power$isModernArrow();
}
