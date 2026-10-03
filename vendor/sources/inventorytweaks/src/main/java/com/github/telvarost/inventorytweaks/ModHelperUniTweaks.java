package com.github.telvarost.inventorytweaks;

import net.danygames2014.unitweaks.tweaks.morekeybinds.KeyBindingListener;
import org.lwjgl.input.Keyboard;

public class ModHelperUniTweaks {
    public static int remapKeyCodes(int keyCode) {

        if (keyCode == local.luke.power.input.Bindings.eventCode(KeyBindingListener.hotbar1)) {
            keyCode = Keyboard.KEY_1;
        } else if (keyCode == local.luke.power.input.Bindings.eventCode(KeyBindingListener.hotbar2)) {
            keyCode = Keyboard.KEY_2;
        } else if (keyCode == local.luke.power.input.Bindings.eventCode(KeyBindingListener.hotbar3)) {
            keyCode = Keyboard.KEY_3;
        } else if (keyCode == local.luke.power.input.Bindings.eventCode(KeyBindingListener.hotbar4)) {
            keyCode = Keyboard.KEY_4;
        } else if (keyCode == local.luke.power.input.Bindings.eventCode(KeyBindingListener.hotbar5)) {
            keyCode = Keyboard.KEY_5;
        } else if (keyCode == local.luke.power.input.Bindings.eventCode(KeyBindingListener.hotbar6)) {
            keyCode = Keyboard.KEY_6;
        } else if (keyCode == local.luke.power.input.Bindings.eventCode(KeyBindingListener.hotbar7)) {
            keyCode = Keyboard.KEY_7;
        } else if (keyCode == local.luke.power.input.Bindings.eventCode(KeyBindingListener.hotbar8)) {
            keyCode = Keyboard.KEY_8;
        } else if (keyCode == local.luke.power.input.Bindings.eventCode(KeyBindingListener.hotbar9)) {
            keyCode = Keyboard.KEY_9;
        } else {
            keyCode = Keyboard.KEY_0;
        }

        return keyCode;
    }
}
