package local.luke.power.controls.tweaks.morekeybinds;

import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.client.option.KeyBinding;
import net.modificationstation.stationapi.api.client.event.option.KeyBindingRegisterEvent;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class KeyBindingListener {
    public static KeyBinding panoramaScreenshot;
    public static KeyBinding photoMode;
    public static KeyBinding hotbar1;
    public static KeyBinding hotbar2;
    public static KeyBinding hotbar3;
    public static KeyBinding hotbar4;
    public static KeyBinding hotbar5;
    public static KeyBinding hotbar6;
    public static KeyBinding hotbar7;
    public static KeyBinding hotbar8;
    public static KeyBinding hotbar9;
    public static KeyBinding hideHUD; // F1
    public static KeyBinding takeScreenshot; // F2
    public static KeyBinding debugHud; // F3
    public static KeyBinding thirdPerson; // F5
    public static KeyBinding cinematicCamera; // F6
    public static KeyBinding toggleFullscreen; // F11
    public static KeyBinding dismount;
    public static KeyBinding zoom;
    public static KeyBinding releaseMouse;
    public static KeyBinding rescanMouse;
    public static KeyBinding toggleRawInput;

    @EventListener
    public void stationRegisterKeybindings(KeyBindingRegisterEvent event) {
        registerKeyBindings(event.keyBindings);
    }

    public static void registerKeyBindings(List<KeyBinding> keyBindings) {
        keyBindings.add(dismount = new KeyBinding("key.power_controls.dismount", Keyboard.KEY_LSHIFT));
        keyBindings.add(zoom = new KeyBinding("key.power_controls.zoom", Keyboard.KEY_C));
        keyBindings.add(photoMode = new KeyBinding("key.power_controls.photo_mode", Keyboard.KEY_P));
        keyBindings.add(hideHUD = new KeyBinding("key.power_controls.hide_hud", Keyboard.KEY_F1));
        keyBindings.add(takeScreenshot = new KeyBinding("key.power_controls.take_screenshot", Keyboard.KEY_F2));
        keyBindings.add(debugHud = new KeyBinding("key.power_controls.debug_hud", Keyboard.KEY_F3));
        keyBindings.add(thirdPerson = new KeyBinding("key.power_controls.third_person", Keyboard.KEY_F5));
        keyBindings.add(cinematicCamera = new KeyBinding("key.power_controls.cinematic_camera", Keyboard.KEY_F6));
        keyBindings.add(toggleFullscreen = new KeyBinding("key.power_controls.toggle_fullscreen", Keyboard.KEY_F11));
        keyBindings.add(releaseMouse = new KeyBinding("key.power_controls.release_mouse", Keyboard.KEY_LMENU));
        keyBindings.add(hotbar1 = new KeyBinding("key.power_controls.hotbar_1", Keyboard.KEY_1));
        keyBindings.add(hotbar2 = new KeyBinding("key.power_controls.hotbar_2", Keyboard.KEY_2));
        keyBindings.add(hotbar3 = new KeyBinding("key.power_controls.hotbar_3", Keyboard.KEY_3));
        keyBindings.add(hotbar4 = new KeyBinding("key.power_controls.hotbar_4", Keyboard.KEY_4));
        keyBindings.add(hotbar5 = new KeyBinding("key.power_controls.hotbar_5", Keyboard.KEY_5));
        keyBindings.add(hotbar6 = new KeyBinding("key.power_controls.hotbar_6", Keyboard.KEY_6));
        keyBindings.add(hotbar7 = new KeyBinding("key.power_controls.hotbar_7", Keyboard.KEY_7));
        keyBindings.add(hotbar8 = new KeyBinding("key.power_controls.hotbar_8", Keyboard.KEY_8));
        keyBindings.add(hotbar9 = new KeyBinding("key.power_controls.hotbar_9", Keyboard.KEY_9));
        keyBindings.add(panoramaScreenshot = new KeyBinding("key.power_controls.panorama_screenshot", Keyboard.KEY_NONE));
        keyBindings.add(rescanMouse = new KeyBinding("key.power_controls.rescan", Keyboard.KEY_NONE));
        keyBindings.add(toggleRawInput = new KeyBinding("key.power_controls.toggle_raw_input", Keyboard.KEY_NONE));
    }
}
