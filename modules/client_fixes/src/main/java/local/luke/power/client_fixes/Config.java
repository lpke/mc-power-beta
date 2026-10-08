package local.luke.power.client_fixes;

import blue.endless.jankson.Comment;
import net.glasslauncher.mods.gcapi3.api.*;

public class Config {

    @ConfigRoot(value = "config", visibleName = "ClientFixes")
    public static ConfigFields config = new ConfigFields();

    public static class ConfigFields {

        @ConfigEntry(
                name = "Disable Server List IP Addresses",
                description = "Hides IP addresses for the server list only"
        )
        public Boolean disableServerListIpAddresses = false;

        @ConfigEntry(
                name = "Disable Stats Checksum Verification",
                description = "Allows stats to be reorganized by mods"
        )
        public Boolean disableStatsChecksumVerification = true;

        @ConfigEntry(
                name = "Enable Authentication Changes",
                description = "Restart required for changes to take effect"
        )
        public Boolean enableAuthenticationChanges = true;

        @ConfigEntry(
                name = "Enable Controls Changes",
                description = "Restart required for changes to take effect"
        )
        public Boolean enableControlsChanges = true;

        @ConfigEntry(
                name = "Enable Chat Changes",
                description = "Restart required for changes to take effect"
        )
        public Boolean enableChatChanges = true;

        @ConfigEntry(
                name = "Enable Debug Graph Changes",
                description = "Restart required for changes to take effect"
        )
        public Boolean enableDebugGraphChanges = true;

        @ConfigEntry(
                name = "Enable Debug Graph Toggle As KEYBIND+F3",
                description = "False=Toggle debug graph with just KEYBIND"
        )
        public Boolean enableDebugGraphModernToggle = true;

        @ConfigEntry(
                name = "Enable Debug Menu World Seed",
                description = "Restart required for changes to take effect"
        )
        public Boolean enableDebugMenuWorldSeed = true;

        @ConfigEntry(
                name = "Enable Multiplayer Server Changes",
                description = "Restart required for changes to take effect"
        )
        public Boolean enableMultiplayerServerChanges = true;



        @ConfigEntry(
                name = "Enable Wooden Sign Changes",
                description = "Restart required for changes to take effect"
        )
        public Boolean enableWoodenSignChanges = true;










    }
}
