package local.luke.power.controls.tweaks.photomode;

import local.luke.power.input.PhotoModeHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.ButtonWidget;

/** Native input with the pack's shared compact button drawing. */
final class PhotoButtonWidget extends ButtonWidget {
    PhotoButtonWidget(int id, int x, int y, int width, int height, String label) {
        super(id, x, y, width, height, label);
    }

    @Override
    public void render(Minecraft minecraft, int mouseX, int mouseY) {
        if (!visible) return;
        if (!PhotoModeHooks.tools().renderButton(minecraft, text, x, y, width, height,
            mouseX, mouseY, active, () -> renderBackground(minecraft, mouseX, mouseY))) {
            super.render(minecraft, mouseX, mouseY);
        }
    }
}
