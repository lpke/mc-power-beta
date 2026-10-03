package local.luke.power.controls.event;

import local.luke.power.controls.util.CompatHelper;
import net.mine_diver.unsafeevents.Event;
import net.mine_diver.unsafeevents.event.EventPhases;
import net.modificationstation.stationapi.api.StationAPI;

import java.util.function.BiFunction;

@SuppressWarnings("UnstableApiUsage")
@EventPhases(StationAPI.INTERNAL_PHASE)
public class RegisterControlFeaturesCompatEvent extends Event {
    public void registerFovCompat(BiFunction<Float, Float, Float> fovFunction) {
        CompatHelper.registerFovCompat(fovFunction);
    }
}
