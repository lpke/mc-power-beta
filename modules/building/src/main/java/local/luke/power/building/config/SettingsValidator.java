package local.luke.power.building.config;

public final class SettingsValidator {
  private SettingsValidator() {}

  public static void validate(Settings s) {
    s.hotbar.validate();
    if (s.autoWalkHoldMillis < 100 || s.autoWalkHoldMillis > 3000)
      throw new IllegalArgumentException("Auto-walk hold time must be 100 to 3000 ms");
    if (s.freeLookPerspective == null)
      throw new IllegalArgumentException("Choose a free look perspective");
    if (s.placement.attemptsPerTick < 1
        || s.placement.attemptsPerTick > 16
        || s.placement.slabMode == null)
      throw new IllegalArgumentException("Placement rate must be 1 to 16");
    local.luke.power.flexible.config.SettingsValidator.validate(s.flexible);
    local.luke.power.fastplace.config.Settings.parseFilters(
        local.luke.power.fastplace.config.Settings.formatFilters(s.placement.blacklist));
    local.luke.power.fastplace.config.Settings.parseFilters(
        local.luke.power.fastplace.config.Settings.formatFilters(s.placement.whitelist));
  }
}
