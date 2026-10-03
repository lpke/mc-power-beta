package local.luke.power.input;

/** Implemented by native key bindings, independent of any Minecraft mapping names. */
public interface Binding {
  String power$id();
  int power$code();
}
