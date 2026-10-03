package local.luke.power.storage;

/** Read-only names used when opening worlds made before the unified pack. */
public final class LegacyKeys {
  public static String original(String current) {
    return switch(current) {
      case "PowerBetaGameMode" -> "LpkeGameMode";
      case "PowerBetaPreviousGameMode" -> "LpkePreviousGameMode";
      case "PowerBetaSpectatorSpeed" -> "LpkeSpectatorSpeed";
      case "PowerBetaWorldEditAccess" -> "LpkeWorldEditAccess";
      default -> throw new IllegalArgumentException("Unknown legacy world field");
    };
  }
  private LegacyKeys() {}
}
