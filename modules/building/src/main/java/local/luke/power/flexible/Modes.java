package local.luke.power.flexible;

public record Modes(
    boolean offset, boolean adjacent, boolean rotation, boolean reverse, boolean into) {
  public static final Modes NONE = new Modes(false, false, false, false, false);

  public boolean active() {
    return offset || adjacent || rotation || reverse || into;
  }

  public boolean grid() {
    return offset || adjacent || rotation;
  }
}
