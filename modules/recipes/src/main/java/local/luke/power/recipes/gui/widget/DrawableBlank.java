package local.luke.power.recipes.gui.widget;

import local.luke.power.recipes.api.gui.AnimatedDrawable;
import local.luke.power.recipes.api.gui.StaticDrawable;
import net.minecraft.client.Minecraft;

import javax.annotation.Nonnull;

public class DrawableBlank implements StaticDrawable, AnimatedDrawable {
    private final int width;
    private final int height;

    public DrawableBlank(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void draw(@Nonnull Minecraft minecraft, int xOffset, int yOffset, int maskTop, int maskBottom, int maskLeft, int maskRight) {

    }

    @Override
    public void draw(@Nonnull Minecraft minecraft) {

    }

    @Override
    public void draw(@Nonnull Minecraft minecraft, int xOffset, int yOffset) {

    }
}
