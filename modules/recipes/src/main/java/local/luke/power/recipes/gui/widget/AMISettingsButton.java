package local.luke.power.recipes.gui.widget;

import local.luke.power.recipes.config.AMIConfig;
import local.luke.power.recipes.gui.RenderHelper;
import net.minecraft.client.Minecraft;

public class AMISettingsButton extends ActionButtonWidget {
    public AMISettingsButton(int id, int x, int y) {
        super(id, x, y, 22, 22, "");
        iconType = ButtonIconType.OTHER;
    }

    @Override
    public void render(Minecraft minecraft, int mouseX, int mouseY) {
        if(!visible) {
            return;
        }
        
        switch (AMIConfig.getOverlayMode()){
            case CHEAT -> texture = "/assets/power_recipes/stationapi/textures/gui/settings_cheat.png";
            case UTILITY -> texture = "/assets/power_recipes/stationapi/textures/gui/settings_utility.png";
            default -> texture = "/assets/power_recipes/stationapi/textures/gui/settings.png";
        }
        
        super.render(minecraft, mouseX, mouseY);
        RenderHelper.bindTexture(texture);
        RenderHelper.drawTexture(x + 1, y + 1, width - 2, height - 2);
    }
}
