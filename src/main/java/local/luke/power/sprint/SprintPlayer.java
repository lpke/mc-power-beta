package local.luke.power.sprint;

/** Stamina belongs to the player save, independently of the global enable preference. */
public interface SprintPlayer {
  SprintState power$sprint();
}
