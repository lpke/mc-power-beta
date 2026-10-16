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
        @ConfigEntry(name = "Sprint activation", description = "Hold requires the sprint key; Toggle stays active until movement stops.")
        public Boolean sprintToggle = true;
        @ConfigEntry(name = "Sprint speed, %", minValue = 100, maxValue = 400)
        public Integer sprintMultiplier = 200;
        @ConfigEntry(
                name = "Freecam Speed",
                description = "Changing this value will change the speed of the freecam",
                maxValue = 1000,
                minValue = 0
        )
        public Float speed = (float)(local.luke.power.input.FlightMotion.HORIZONTAL_SPEED * 1.1);

        @ConfigEntry(
                name = "Freecam deceleration",
                description = "Coasting when movement is released: 0 stops immediately; 5 uses modern flight drag.",
                maxValue = 5,
                minValue = 0
        )
        public Float drag = 3f;

        @ConfigEntry(name = "Freecam acceleration", minValue = 0, maxValue = 5,
                description = "0 reaches flight speed immediately; 5 uses modern Creative acceleration. Lower values respond faster without changing the flight speed.")
        public Float acceleration = 3.5f;

    }
}
