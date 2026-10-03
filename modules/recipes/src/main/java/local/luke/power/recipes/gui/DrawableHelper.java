package local.luke.power.recipes.gui;

import local.luke.power.recipes.api.gui.AMIDrawable;
import local.luke.power.recipes.api.gui.AnimatedDrawable;
import local.luke.power.recipes.api.gui.StaticDrawable;
import local.luke.power.recipes.api.gui.TickTimer;
import local.luke.power.recipes.gui.widget.DrawableBlank;
import local.luke.power.recipes.gui.widget.DrawableResource;
import local.luke.power.recipes.util.RecipeBrowser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DrawableHelper {

    @Nonnull
    public static AnimatedDrawable createAnimatedDrawable(@Nullable StaticDrawable drawable, int ticksPerCycle, @Nullable AnimatedDrawable.StartDirection startDirection, boolean inverted) {
        if (drawable == null) {
            RecipeBrowser.LOGGER.error("Null drawable, returning blank drawable", new NullPointerException());
            return new DrawableBlank(0, 0);
        }
        if (startDirection == null) {
            RecipeBrowser.LOGGER.error("Null startDirection, defaulting to Top", new NullPointerException());
            startDirection = AnimatedDrawable.StartDirection.TOP;
        }

        if (inverted) {
            if (startDirection == AnimatedDrawable.StartDirection.TOP) {
                startDirection = AnimatedDrawable.StartDirection.BOTTOM;
            } else if (startDirection == AnimatedDrawable.StartDirection.BOTTOM) {
                startDirection = AnimatedDrawable.StartDirection.TOP;
            } else if (startDirection == AnimatedDrawable.StartDirection.LEFT) {
                startDirection = AnimatedDrawable.StartDirection.RIGHT;
            } else {
                startDirection = AnimatedDrawable.StartDirection.LEFT;
            }
        }

        int tickTimerMaxValue;
        if (startDirection == AnimatedDrawable.StartDirection.TOP || startDirection == AnimatedDrawable.StartDirection.BOTTOM) {
            tickTimerMaxValue = drawable.getHeight();
        } else {
            tickTimerMaxValue = drawable.getWidth();
        }
        TickTimer tickTimer = new local.luke.power.recipes.util.TickTimer(ticksPerCycle, tickTimerMaxValue, !inverted);
        return new local.luke.power.recipes.gui.widget.DrawableAnimated(drawable, tickTimer, startDirection);
    }

    @Nonnull
    public static StaticDrawable createDrawable(@Nullable String resourceLocation, int u, int v, int width, int height, int paddingTop, int paddingBottom, int paddingLeft, int paddingRight) {
        if (resourceLocation == null) {
            RecipeBrowser.LOGGER.error("Null resourceLocation, returning blank drawable", new NullPointerException());
            return new DrawableBlank(width, height);
        }
        return new DrawableResource(resourceLocation, u, v, width, height, paddingTop, paddingBottom, paddingLeft, paddingRight);
    }

    @Nonnull
    public static StaticDrawable createDrawable(@Nullable String resourceLocation, int u, int v, int width, int height) {
        if (resourceLocation == null) {
            RecipeBrowser.LOGGER.error("Null resourceLocation, returning blank drawable", new NullPointerException());
            return new DrawableBlank(width, height);
        }
        return new DrawableResource(resourceLocation, u, v, width, height);
    }

    public static AMIDrawable createBlankDrawable(int width, int height) {
        return new DrawableBlank(width, height);
    }
}
