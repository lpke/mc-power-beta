package local.luke.power.recipes.testmod;

import local.luke.power.recipes.api.action.ActionButton;
import local.luke.power.recipes.config.OverlayMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CheatyDummyActionButton implements ActionButton {
    private final List<OverlayMode> allowedOverlayModes = List.of(OverlayMode.CHEAT);
    
    @Override
    public boolean perform(Object minecraft, World world, PlayerEntity player, boolean isOperator, int mouseButton, boolean holdingShift) {
        return false;
    }

    @Override
    public String getTexture() {
        return "/assets/power_recipes/stationapi/textures/gui/bin.png";
    }

    @Override
    public String getClickSound() {
        return "random.drr";
    }

    @Override
    public @Nullable List<OverlayMode> allowedOverlayModes() {
        return allowedOverlayModes;
    }
}
