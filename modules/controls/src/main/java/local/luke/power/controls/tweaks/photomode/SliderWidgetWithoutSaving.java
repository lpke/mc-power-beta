package local.luke.power.controls.tweaks.photomode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.SliderWidget;
import local.luke.power.input.PhotoModeHooks;
import org.lwjgl.opengl.GL11;

public class SliderWidgetWithoutSaving extends SliderWidget {
    public SliderWidgetWithoutSaving(int id, int x, int y, String text, float value) {
        super(id, x, y, null, text, value);
        this.height = 18;
    }

    @Override
    public void render(Minecraft minecraft, int mouseX, int mouseY) {
        if (!visible) return;
        if (!PhotoModeHooks.tools().renderButton(minecraft, text, x, y, width, height,
            mouseX, mouseY, active, () -> renderBackground(minecraft, mouseX, mouseY))) {
            super.render(minecraft, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBackground(Minecraft minecraft, int mouseX, int mouseY) {
        if (this.visible) {
            if (this.dragging) {
                this.value = (float) (mouseX - (this.x + 4)) / (float) (this.width - 8);
                if (this.value < 0.0F) {
                    this.value = 0.0F;
                }

                if (this.value > 1.0F) {
                    this.value = 1.0F;
                }
            }

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            int handleX = this.x + (int) (this.value * (float) (this.width - 8));
            // Retain both texture edges when using the map controls' compact height.
            this.drawTexture(handleX, this.y, 0, 66, 4, 9);
            this.drawTexture(handleX + 4, this.y, 196, 66, 4, 9);
            this.drawTexture(handleX, this.y + 9, 0, 77, 4, 9);
            this.drawTexture(handleX + 4, this.y + 9, 196, 77, 4, 9);
        }
    }

    @Override
    public boolean isMouseOver(Minecraft minecraft, int mouseX, int mouseY) {
        if (this.active && (mouseX >= this.x) && (mouseY >= this.y) && (mouseX < (this.x + this.width)) && (mouseY < (this.y + this.height))) {
            this.value = (float) (mouseX - (this.x + 4)) / (float) (this.width - 8);
            if (this.value < 0.0F) {
                this.value = 0.0F;
            }

            if (this.value > 1.0F) {
                this.value = 1.0F;
            }

            this.dragging = true;
            return true;
        } else {
            return false;
        }
    }
}
