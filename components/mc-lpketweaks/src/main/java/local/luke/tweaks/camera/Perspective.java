package local.luke.tweaks.camera;

public enum Perspective {
  THIRD_PERSON("Third person"),
  FIRST_PERSON("First person");
  public final String label;

  Perspective(String label) {
    this.label = label;
  }

  public Perspective next() {
    return values()[(ordinal() + 1) % values().length];
  }
}
