package local.luke.power.fastplace;

public enum SlabMode {
  CONTINUOUS("Continuous layers"),
  DOUBLE("Double slabs"),
  MATCH_FIRST("Match first placement");
  public final String label;

  SlabMode(String label) {
    this.label = label;
  }
}
