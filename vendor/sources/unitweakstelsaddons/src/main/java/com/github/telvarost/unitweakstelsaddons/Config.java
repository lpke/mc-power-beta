package com.github.telvarost.unitweakstelsaddons;

import net.glasslauncher.mods.gcapi3.api.*;

public class Config {

    @ConfigRoot(value = "config", visibleName = "UniTweaksTelsAddons")
    public static ConfigFields config = new ConfigFields();

    public static class ConfigFields {
        @ConfigCategory(name = "Particles Config")
        public ParticlesConfig PARTICLES_CONFIG = new ParticlesConfig();

        @ConfigCategory(
                name = "Stack Size Config",
                description = "Options here require restart to take effect"
        )
        public StackSizeConfig STACK_SIZE_CONFIG = new StackSizeConfig();

        @ConfigCategory(
                name = "Config: Effectivity List Additions For Shovels",
                description = "Options here require restart to take effect"
        )
        public final ShovelsConfig SHOVELS_CONFIG = new ShovelsConfig();

        @ConfigEntry(name = "Add Biome To Debug Overlay")
        public Boolean addBiomeToDebugOverlay = true;

        @ConfigEntry(name = "Add Day Counter To Debug Overlay")
        public Boolean addDayCounterToDebugOverlay = true;

        @ConfigEntry(name = "Add Hours Played To Debug Overlay")
        public Boolean addTotalPlayTimeToDebugOverlay = true;

        @ConfigEntry(
                name = "Add Light Level To Debug Overlay",
                description = "Calculated at player's eye level"
        )
        public Boolean addLightLevelToDebugOverlay = true;

        @ConfigEntry(name = "Add Slime Chunk To Debug Overlay")
        public Boolean addSlimeChunkToDebugOverlay = true;

        @ConfigEntry(
                name = "Additions To Debug Overlay Y Offset",
                maxLength = 4096,
                minLength = -4096
        )
        public Integer overlayAdditionsYOffset = 0;

        @ConfigEntry(
                name = "Disable Colored Sheep Spawning",
                description = "Makes sheep always spawn with white wool",
                multiplayerSynced = true
        )
        public Boolean disableColoredSheepSpawning = false;

        @ConfigEntry(name = "Milk Squids", multiplayerSynced = true)
        //@ValueOnVanillaServer(booleanValue = TriBoolean.FALSE)
        public Boolean milkSquids = false;

        @ConfigEntry(name = "Pigs Drop Brown Mushrooms", multiplayerSynced = true)
        //@ValueOnVanillaServer(booleanValue = TriBoolean.FALSE)
        public Boolean pigsDropBrownMushrooms = false;

        @ConfigEntry(
                name = "Plant Replacement Fixes Enabled",
                multiplayerSynced = true
        )
        //@ValueOnVanillaServer(booleanValue = TriBoolean.FALSE)
        public Boolean plantReplacementFixesEnabled = true;

        @ConfigEntry(
                name = "Slab Placement Fixes Enabled",
                description = "Restart required for changes to take effect",
                multiplayerSynced = true
        )
        //@ValueOnVanillaServer(booleanValue = TriBoolean.FALSE)
        public Boolean slabPlacementFixesEnabled = true;

        @ConfigEntry(
                name = "Use Player 2x2 Crafting Grid As Inventory",
                multiplayerSynced = true
        )
        //@ValueOnVanillaServer(booleanValue = TriBoolean.FALSE)
        public Boolean allowCraftingInventorySlots = false;

        @ConfigEntry(name = "Vehicle Logout/Login Fix", multiplayerSynced = true)
        public Boolean vehicleLogoutLoginFixesEnabled = true;
    }

    public static class StackSizeConfig {
        @ConfigEntry(
                name = "Enable Modern Wood Door Stack Size",
                multiplayerSynced = true
        )
        public Boolean enableModernWoodDoorStackSize = false;

        @ConfigEntry(
                name = "Enable Modern Iron Door Stack Size",
                multiplayerSynced = true
        )
        public Boolean enableModernIronDoorStackSize = false;

        @ConfigEntry(
                name = "Enable Modern Sign Stack Size",
                multiplayerSynced = true
        )
        public Boolean enableModernSignStackSize = false;
    }

    public static class ShovelsConfig {
        @ConfigEntry(
                name = "Effective On Soul Sand",
                multiplayerSynced = true
        )
        //@ValueOnVanillaServer(booleanValue = TriBoolean.FALSE)
        public Boolean enableShovelsEffectiveOnSoulSand = true;
    }


    public static class ParticlesConfig {
        @ConfigEntry(name = "Disable All Particles")
        public Boolean disableAllParticles = false;

        @ConfigEntry(name = "Disable Water Bubble Particle")
        public Boolean disableWaterBubbleParticle = false;

        @ConfigEntry(name = "Disable Smoke Particle")
        public Boolean disableFireSmokeParticle = false;

        @ConfigEntry(name = "Disable Note Particle")
        public Boolean disableNoteParticle = false;

        @ConfigEntry(name = "Disable Portal Particle")
        public Boolean disablePortalParticle = false;

        @ConfigEntry(name = "Disable Explosion Particle")
        public Boolean disableExplosionParticle = false;

        @ConfigEntry(name = "Disable Flame Particle")
        public Boolean disableFlameParticle = false;

        @ConfigEntry(name = "Disable Lava Ember Particle")
        public Boolean disableLavaEmberParticle = false;

        @ConfigEntry(name = "Disable Footstep Particle")
        public Boolean disableFootstepParticle = false;

        @ConfigEntry(name = "Disable Water Splash Particle")
        public Boolean disableWaterSplashParticle = false;

        @ConfigEntry(name = "Disable Large Smoke Particle")
        public Boolean disableLargeFireSmokeParticle = false;

        @ConfigEntry(name = "Disable Redstone Dust Particle")
        public Boolean disableRedDustParticle = false;

        @ConfigEntry(name = "Disable Snowball Particle")
        public Boolean disableSnowballParticle = false;

        @ConfigEntry(name = "Disable Snow Shovel Particle")
        public Boolean disableSnowShovelParticle = false;

        @ConfigEntry(name = "Disable Slime Particle")
        public Boolean disableSlimeParticle = false;

        @ConfigEntry(name = "Disable Heart Particle")
        public Boolean disableHeartParticle = false;
    }
}
