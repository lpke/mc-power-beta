package local.luke.power.map;

/** Spreads any rate from one to twenty updates over the native twenty-Hz clock. */
public final class MapCadence {
  private int phase;
  private boolean first = true;

  public boolean tick(int rate) {
    if (first) {
      first = false;
      return true;
    }
    phase += rate;
    if (phase < 20) return false;
    phase -= 20;
    return true;
  }
}
