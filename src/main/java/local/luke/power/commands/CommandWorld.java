package local.luke.power.commands;

/** A stable identifier copied between dimensions and saved in world metadata. */
public interface CommandWorld extends local.luke.power.input.WorldIdentity {
  String power$commandWorldId();
}
