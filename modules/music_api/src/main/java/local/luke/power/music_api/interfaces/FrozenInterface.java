package local.luke.power.music_api.interfaces;

import net.modificationstation.stationapi.api.util.Util;

public interface FrozenInterface {
    default int powerMusic_getFrozenTicks() {
        return Util.assertImpl();
    }

    default void powerMusic_setFrozenTicks(int frozenTicks) {
        Util.assertImpl();
    }
}
