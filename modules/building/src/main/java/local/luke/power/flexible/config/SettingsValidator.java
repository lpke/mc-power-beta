package local.luke.power.flexible.config;

public final class SettingsValidator {
  private SettingsValidator() {}

  public static void validate(Settings s) {
    if (s.overlayColor < 0 || s.overlayColor > 3 || s.overlayOpacity < 10 || s.overlayOpacity > 90)
      throw new IllegalArgumentException("Invalid overlay settings");
  }
}
