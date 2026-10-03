package local.luke.power;

import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;

/** Loader constructs adapters before mixin plugins and config-library preLaunch hooks. */
public final class EarlyDefaults implements LanguageAdapter {
  public EarlyDefaults() {
    Bootstrap.prepare();
  }

  @Override
  public <T> T create(ModContainer mod, String value, Class<T> type)
      throws LanguageAdapterException {
    return LanguageAdapter.getDefault().create(mod, value, type);
  }
}
