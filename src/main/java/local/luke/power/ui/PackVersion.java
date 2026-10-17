package local.luke.power.ui;

import net.fabricmc.loader.api.FabricLoader;

/** The displayed pack version always comes from the built mod metadata. */
public final class PackVersion {
  private static final String LABEL = "Power Beta " + FabricLoader.getInstance().getModContainer("power_beta")
      .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("development");
  private PackVersion() {}
  public static String label() { return LABEL; }
}
