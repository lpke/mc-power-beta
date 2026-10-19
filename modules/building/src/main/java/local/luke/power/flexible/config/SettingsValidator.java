package local.luke.power.flexible.config;

public final class SettingsValidator {
  private SettingsValidator() {}

  public static void validate(Settings s) {
    local.luke.power.ui.ColorValue.Palette.FLEXIBLE.validate(s.overlayColor);
    if (s.overlayOpacity < 10 || s.overlayOpacity > 90)
      throw new IllegalArgumentException("Invalid overlay settings");
  }
}
