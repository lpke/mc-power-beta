package com.github.telvarost.zastavkaapi.interfaces;

import net.modificationstation.stationapi.api.util.Util;

public interface FrozenInterface {
    default int zastavkaApi_getFrozenTicks() {
        return Util.assertImpl();
    }

    default void zastavkaApi_setFrozenTicks(int frozenTicks) {
        Util.assertImpl();
    }
}
