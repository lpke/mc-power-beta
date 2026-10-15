package local.luke.power.camera;

import net.glasslauncher.mods.gcapi3.api.*;

public class FreecamConfig {
    @ConfigRoot(value = "config", visibleName = "Freecam Config")
    public static ConfigFields config = new ConfigFields();

    public static class ConfigFields {
        @ConfigEntry(name = "Hide HUD on entering freecam", description = "Hide the HUD when freecam starts. F1 can change it during freecam; leaving restores the entry state.")
        public Boolean hideHudOnEntry = false;
        @ConfigEntry(name = "Player movement messages", description = "Show a chat message when freecam player movement turns on or off during gameplay. Independent of the Active tweaks HUD.")
        public Boolean playerMovementMessages = false;
        @ConfigEntry(
                name = "Enable Freecam Collisions",
                multiplayerSynced = true
        )
        public Boolean collision = true;
        @ConfigEntry(name = "Freecam door collisions", description = "Collide with doors and trapdoors when camera collisions are enabled.")
        public Boolean doorCollision = false;
        @ConfigEntry(name = "Freecam sprint", description = "Use the flight sprint key, multiplier and Hold/Toggle setting to boost camera speed.")
        public Boolean sprint = true;
        @ConfigEntry(
                name = "Freecam Speed",
                description = "Changing this value will change the speed of the freecam",
                maxValue = 1000,
                minValue = 0
        )
        public Float speed = 10f;

        @ConfigEntry(
                name = "Freecam Drag",
                description = "Changing this value will change the drag of the freecam, The lower the drag, the longer it takes for the camera to stop. This option doesn't do anything for classic movement",
                maxValue = 1000,
                minValue = 0
        )
        public Float drag = 4f;

        @ConfigEntry(
                name = "Classic Freecam Movement",
                description = "When this option is enabled the freecam will move like in older versions of Freecam"
        )
        public Boolean classicMovement = false;
    }
}
