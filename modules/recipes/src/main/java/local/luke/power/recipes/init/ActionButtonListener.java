package local.luke.power.recipes.init;

import local.luke.power.recipes.action.HealActionButton;
import local.luke.power.recipes.action.SetTimeActionButton;
import local.luke.power.recipes.action.ToggleWeatherActionButton;
import local.luke.power.recipes.action.TrashActionButton;
import local.luke.power.recipes.api.event.ActionButtonRegisterEvent;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.util.Namespace;

public class ActionButtonListener {
    @SuppressWarnings("UnstableApiUsage")
    public static final Namespace NAMESPACE = Namespace.resolve();

    @EventListener
    public void registerActionButtons(ActionButtonRegisterEvent event) {
        // 23000 = Dusk, 6000 = Day, 13000 = Dusk, 18000 = Night
        event.add(NAMESPACE.id("set_time_day"), new SetTimeActionButton(6000, "/assets/power_recipes/stationapi/textures/gui/day.png"));
        event.add(NAMESPACE.id("set_time_night"), new SetTimeActionButton(18000, "/assets/power_recipes/stationapi/textures/gui/night.png"));
        event.add(NAMESPACE.id("toggle_weather"), new ToggleWeatherActionButton());
        event.add(NAMESPACE.id("heal"), new HealActionButton());
        event.add(NAMESPACE.id("trash"), new TrashActionButton());
    }
}
