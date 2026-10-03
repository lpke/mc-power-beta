package local.luke.power.camera.listener;

import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ToolMaterial;
import net.modificationstation.stationapi.api.client.event.keyboard.KeyStateChangedEvent;
import org.lwjgl.input.Keyboard;
import local.luke.power.camera.Freecam;
import local.luke.power.camera.client.gui.GuiSavedCameraLocations;
import local.luke.power.camera.registry.KeyBindingRegistry;

import java.util.Arrays;

public class KeyPressedListener {
    public KeyPressedListener() {
        local.luke.power.input.Bindings.onMousePress("free-camera", this::press);
    }
    private String[] validCharacters = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"}; // For alphabetic character support add '"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "Q", "Y", "Z", '
    @EventListener
    public void keyPressed(KeyStateChangedEvent event) {
        if(event.environment == KeyStateChangedEvent.Environment.IN_GAME) {
            Freecam.freecamController.updateSpeed = local.luke.power.input.Bindings.down(KeyBindingRegistry.changeSpeedKeybinding);
            if (!Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()) return;

            // Input cameraposition name
            if(Freecam.freecamController.savePosition || Freecam.freecamController.loadPosition){
                if(Arrays.stream(validCharacters).anyMatch(Keyboard.getKeyName(Keyboard.getEventKey())::equals) && Keyboard.isKeyDown(Keyboard.getEventKey())){
                    Freecam.freecamController.cameraPositionName += Keyboard.getKeyName(Keyboard.getEventKey());
                }
                if(Keyboard.isKeyDown(Keyboard.KEY_BACK)){
                    if(Freecam.freecamController.cameraPositionName.length() > 0){
                        Freecam.freecamController.cameraPositionName = Freecam.freecamController.cameraPositionName.substring(0, Freecam.freecamController.cameraPositionName.length() - 1);
                    }
                }
                if(Keyboard.isKeyDown(Keyboard.KEY_RETURN)){
                    if(Freecam.freecamController.savePosition){
                        if(!Freecam.freecamController.cameraPositionName.isEmpty()){
                            Freecam.freecamController.saveCameraPosition(Freecam.freecamController.cameraPositionName);
                        }
                        Freecam.freecamController.savePosition = false;
                    }
                    else {
                        Freecam.freecamController.loadCameraPosition(Freecam.freecamController.cameraPositionName);
                        Freecam.freecamController.loadPosition = false;
                    }
                    Freecam.freecamController.cameraPositionName = "";
                }
            }

            press(Keyboard.getEventKey());
        }
    }

    private void press(int keyCode) {
            if (!local.luke.power.camera.FreecamConfig.config.enabled) { Freecam.freecamController.setActive(false); return; }
            // Toggle freecam
            if(local.luke.power.input.Bindings.matches(KeyBindingRegistry.freecamKeybinding, keyCode)) {
                ClientPlayerEntity player = Minecraft.class.cast(FabricLoader.getInstance().getGameInstance()).player;
                if(!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && !Freecam.freecamController.isActive() || !Freecam.freecamController.cameraPositionSet){
                    Freecam.freecamController.setCameraPositionAndRotation(player.x, player.y, player.z, player.pitch, player.yaw + 180, 0);
                    Freecam.freecamController.cameraPositionSet = true;
                }
                if(!Freecam.freecamController.isActive()){
                    Freecam.freecamController.velocityX = 0;
                    Freecam.freecamController.velocityY = 0;
                    Freecam.freecamController.velocityZ = 0;
                }
                Freecam.freecamController.setActive(!Freecam.freecamController.isActive());
            }

            // Toggle player movement
            if(local.luke.power.input.Bindings.matches(KeyBindingRegistry.playerMovementKeybinding, keyCode)) {
                Freecam.freecamController.allowPlayerMovement = !Freecam.freecamController.allowPlayerMovement;
            }

            // Change speed
            if(local.luke.power.input.Bindings.down(KeyBindingRegistry.changeSpeedKeybinding)) {
                Freecam.freecamController.updateSpeed = true;
            } else {
                Freecam.freecamController.updateSpeed = false;
            }

            // Save/load cameraposition
            if(local.luke.power.input.Bindings.matches(KeyBindingRegistry.cameraPositionKeybinding, keyCode)) {
                if(!Freecam.freecamController.loadPosition && !Freecam.freecamController.savePosition && Freecam.freecamController.isActive()){
                    if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)){
                        Freecam.freecamController.savePosition = true;
                    } else {
                        Freecam.freecamController.loadPosition = true;
                    }
                }
            }

            // Open cameraposition gui
            if(local.luke.power.input.Bindings.matches(KeyBindingRegistry.cameraPositionGuiKeybinding, keyCode)) {
                if(Freecam.freecamController.isActive()){
                    ((Minecraft) FabricLoader.getInstance().getGameInstance()).setScreen(new GuiSavedCameraLocations());
                }
            }
    }
}
