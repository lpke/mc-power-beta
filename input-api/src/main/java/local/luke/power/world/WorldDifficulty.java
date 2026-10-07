package local.luke.power.world;

/** Difficulty belongs to the save, including all its dimensions. */
public interface WorldDifficulty {
  String TAG = "PowerBetaDifficulty";
  int DEFAULT = 2;
  int power$difficulty();
  void power$difficulty(int difficulty);

  static int checked(int value) {
    if (value < 0 || value > 3) throw new IllegalArgumentException("Difficulty must be between 0 and 3.");
    return value;
  }
}
