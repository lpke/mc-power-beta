package local.luke.power.audio;

/** Native item health is needed because Beta reports false even when item damage succeeds. */
public interface ItemLavaSoundState {
  int power$itemSoundHealth();
}
