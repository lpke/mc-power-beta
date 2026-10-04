package local.luke.power.audio;

import local.luke.power.PowerBeta;

/** Snapshot of the existing music controls, shared with the unified settings backend. */
public record MusicRules(
    boolean menuEnabled,
    boolean menuOverrides,
    int delayMin,
    int delayRandom,
    float portal,
    float rain,
    float cave,
    float ghast) {
  public static MusicRules read() {
    try {
      Class<?> root = Class.forName("local.luke.power.environment.Config");
      Object config = root.getField("config").get(null);
      Object m = config.getClass().getField("MUSIC_CONFIG").get(config);
      return new MusicRules(
          AudioConfig.current().menuMusic != AudioSettings.MenuMusic.WORLD,
          AudioConfig.current().menuMusic == AudioSettings.MenuMusic.CUSTOM,
          AudioConfig.current().gapMinSeconds * 20,
          (AudioConfig.current().gapMaxSeconds - AudioConfig.current().gapMinSeconds) * 20 + 1,
          number(m, "volumeNetherPortalAmbient"),
          number(m, "volumeRainAmbient"),
          number(m, "volumeCaveAmbient"),
          number(m, "volumeGhastAmbient"));
    } catch (ReflectiveOperationException e) {
      PowerBeta.LOG.error("Could not read music controls", e);
      return new MusicRules(false, false, 12000, 12000, 1, 1, 1, 1);
    }
  }

  private static float number(Object o, String field) throws ReflectiveOperationException {
    return Math.max(0, Math.min(1, ((Number) o.getClass().getField(field).get(o)).floatValue()));
  }

  public float ambient(String id) {
    if (id == null) return 1;
    if (id.startsWith("portal.portal")) return portal;
    if (id.startsWith("ambient.weather.rain")) return rain;
    if (id.startsWith("ambient.cave.cave")) return cave;
    if (id.startsWith("mob.ghast.moan")) return ghast;
    return 1;
  }
}
