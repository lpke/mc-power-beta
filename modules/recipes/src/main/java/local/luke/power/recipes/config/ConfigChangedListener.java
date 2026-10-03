package local.luke.power.recipes.config;

import local.luke.power.recipes.util.RecipeBrowser;
import net.glasslauncher.mods.gcapi3.api.PreConfigSavedListener;
import net.glasslauncher.mods.gcapi3.impl.GlassYamlFile;

public class ConfigChangedListener implements PreConfigSavedListener {
    @Override
    public void onPreConfigSaved(int source, GlassYamlFile oldValues, GlassYamlFile newValues) {

        if (oldValues.getBoolean("showRedundantItems", false) != newValues.getBoolean("showRedundantItems", false)) {
            RecipeBrowser.reloadBlacklist();
        }
        else if (oldValues.getBoolean("editMode", false) != newValues.getBoolean("editMode", false)) {
            RecipeBrowser.reloadBlacklist();
        }
    }
}
