package local.luke.power.recipes.config;

public enum OverlayMode {
    RECIPE("gui.config.power_recipes.overlaymode.recipe"),
    CHEAT("gui.config.power_recipes.overlaymode.cheat"),
    UTILITY("gui.config.power_recipes.overlaymode.utility");

    public final String translationKey;

    OverlayMode(String translationKey) {
        this.translationKey = translationKey;
    }

    @Override
    public String toString() {
        return translationKey;
    }
}
