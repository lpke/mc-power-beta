package com.github.telvarost.quickadditions;

import net.glasslauncher.mods.gcapi3.api.*;

public class Config {

    @ConfigRoot(value = "config", visibleName = "QuickAdditions")
    public static ConfigFields config = new ConfigFields();

    public static class ConfigFields {
        @ConfigCategory(
                name = "Add Missing Achievements Config",
                description = "Must be in a world when marking achievements"
        )
        public MissingAchievementsConfig MISSING_ACHIEVEMENTS_CONFIG = new MissingAchievementsConfig();

        @ConfigCategory(
                name = "Entity Spawn Mechanics Config"
        )
        public EntitySpawnMechanicsConfig ENTITY_SPAWN_MECHANICS_CONFIG = new EntitySpawnMechanicsConfig();

        @ConfigCategory(
                name = "Music Config"
        )
        public MusicConfig MUSIC_CONFIG = new MusicConfig();

        @ConfigCategory(
                name = "Sleep Config"
        )
        public SleepConfig SLEEP_CONFIG = new SleepConfig();

        @ConfigCategory(
                name = "Weather Config"
        )
        public WeatherConfig WEATHER_CONFIG = new WeatherConfig();

        @ConfigEntry(
                name = "Force Display Active",
                description = "Game will not pause when tabbing away"
        )
        public Boolean forceDisplayActive = false;

        @ConfigEntry(
                name = "Spawn Protection Radius",
                description = "Restart required; Overridden By UniTweaks",
                multiplayerSynced = true,
                maxLength = 256
        )
        public Integer SPAWN_PROTECTION_RADIUS = 16;

        @ConfigEntry(
                name = "Soul Sand Crafting Recipe Enable",
                description = "Restart required for changes to take effect",
                multiplayerSynced = true
        )
        public Boolean ENABLE_SOUL_SAND_RECIPE = false;

        @ConfigEntry(
                name = "Soul Sand Recipe Output: 1-16",
                description = "Restart required for changes to take effect",
                multiplayerSynced = true,
                maxLength = 16
        )
        public Integer SOUL_SAND_OUTPUT = 4;
    }

    public static class MissingAchievementsConfig {
        @ConfigEntry(name = "Mark Achieved: Open Inventory")
        public Boolean OPEN_INVENTORY = false;

        @ConfigEntry(name = "Mark Achieved: Mine Wood")
        public Boolean MINE_WOOD = false;

        @ConfigEntry(name = "Mark Achieved: Craft Workbench")
        public Boolean CRAFT_WORKBENCH = false;

        @ConfigEntry(name = "Mark Achieved: Craft Pickaxe")
        public Boolean CRAFT_PICKAXE = false;

        @ConfigEntry(name = "Mark Achieved: Craft Furnace")
        public Boolean CRAFT_FURNACE = false;

        @ConfigEntry(name = "Mark Achieved: Aquire Iron")
        public Boolean AQUIRE_IRON = false;

        @ConfigEntry(name = "Mark Achieved: Craft Hoe")
        public Boolean CRAFT_HOE = false;

        @ConfigEntry(name = "Mark Achieved: Make Bread")
        public Boolean MAKE_BREAD = false;

        @ConfigEntry(name = "Mark Achieved: Bake Cake")
        public Boolean BAKE_CAKE = false;

        @ConfigEntry(name = "Mark Achieved: Craft Better Pickaxe")
        public Boolean CRAFT_BETTER_PICKAXE = false;

        @ConfigEntry(name = "Mark Achieved: Cook Fish")
        public Boolean COOK_FISH = false;

        @ConfigEntry(name = "Mark Achieved: On A Rail")
        public Boolean ON_A_RAIL = false;

        @ConfigEntry(name = "Mark Achieved: Craft Sword")
        public Boolean CRAFT_SWORD = false;

        @ConfigEntry(name = "Mark Achieved: Kill Enemy")
        public Boolean KILL_ENEMY = false;

        @ConfigEntry(name = "Mark Achieved: Kill Cow")
        public Boolean KILL_COW = false;

        @ConfigEntry(name = "Mark Achieved: Fly Pig")
        public Boolean FLY_PIG = false;
    }

    public static class EntitySpawnMechanicsConfig {
        @ConfigCategory(
                name = "Entity Spawn List Config",
                description = "Restart required for changes to take effect"
        )
        public SpawnListConfig SPAWN_LIST_CONFIG = new SpawnListConfig();

        @ConfigEntry(
                name = "Disable Living Entities Despawning",
                description = "Living entities will no longer despawn",
                multiplayerSynced = true
        )
        public Boolean disableLivingEntitiesDespawning = false;

        @ConfigEntry(
                name = "Disable Saddled Pigs Despawning",
                description = "Saddled pigs will no longer despawn",
                multiplayerSynced = true
        )
        public Boolean disableSaddledPigsDespawning = false;

        @ConfigEntry(
                name = "Do Not Count Saddled Pigs",
                description = "Saddled pigs are not in passive count",
                multiplayerSynced = true
        )
        public Boolean doNotCountSaddledPigs = false;

        @ConfigEntry(
                name = "Monsters Spawn At Or Below Light Level",
                description = "4 = Nighttime Light Level",
                multiplayerSynced = true,
                maxLength = 7
        )
        public Integer monstersSpawnAtOrBelowLightLevel = 7;

        @ConfigEntry(
                name = "Spawn Group Size",
                description = "Does not effect ghasts, wolves cap at 8",
                multiplayerSynced = true,
                maxLength = 16
        )
        public Integer SPAWN_GROUP_SIZE = 4;

        @ConfigEntry(
                name = "Spawn Capacity Max: Monster",
                multiplayerSynced = true,
                maxLength = 255
        )
        public Integer MAX_SPAWN_CAPACITY_MONSTER = 70;

        @ConfigEntry(
                name = "Spawn Capacity Max: Passive",
                multiplayerSynced = true,
                maxLength = 255
        )
        public Integer MAX_SPAWN_CAPACITY_PASSIVE = 15;

        @ConfigEntry(
                name = "Spawn Capacity Max: Squid",
                multiplayerSynced = true,
                maxLength = 255
        )
        public Integer MAX_SPAWN_CAPACITY_SQUID = 5;
    }

    public static class SpawnListConfig {
        @ConfigEntry(
                name = "Enable Chicken Entity Spawning",
                multiplayerSynced = true
        )
        public Boolean enableChickenEntitySpawning = true;

        @ConfigEntry(
                name = "Enable Cow Entity Spawning",
                multiplayerSynced = true
        )
        public Boolean enableCowEntitySpawning = true;

        @ConfigEntry(
                name = "Enable Creeper Entity Spawning",
                multiplayerSynced = true
        )
        public Boolean enableCreeperEntitySpawning = true;

        @ConfigEntry(
                name = "Enable Ghast Entity Spawning",
                multiplayerSynced = true
        )
        public Boolean enableGhastEntitySpawning = true;

        @ConfigEntry(
                name = "Enable Pig Entity Spawning",
                multiplayerSynced = true
        )
        public Boolean enablePigEntitySpawning = true;

        @ConfigEntry(
                name = "Enable Sheep Entity Spawning",
                multiplayerSynced = true
        )
        public Boolean enableSheepEntitySpawning = true;

        @ConfigEntry(
                name = "Enable Skeleton Entity Spawning",
                multiplayerSynced = true
        )
        public Boolean enableSkeletonEntitySpawning = true;

        @ConfigEntry(
                name = "Enable Slime Entity Spawning",
                multiplayerSynced = true
        )
        public Boolean enableSlimeEntitySpawning = true;

        @ConfigEntry(
                name = "Enable Spider Entity Spawning",
                multiplayerSynced = true
        )
        public Boolean enableSpiderEntitySpawning = true;

        @ConfigEntry(
                name = "Enable Squid Entity Spawning",
                multiplayerSynced = true
        )
        public Boolean enableSquidEntitySpawning = true;

        @ConfigEntry(
                name = "Enable Wolf Entity Spawning",
                multiplayerSynced = true
        )
        public Boolean enableWolfEntitySpawning = true;

        @ConfigEntry(
                name = "Enable Zombie Entity Spawning",
                multiplayerSynced = true
        )
        public Boolean enableZombieEntitySpawning = true;

        @ConfigEntry(
                name = "Enable Zombie Pigman Entity Spawning",
                multiplayerSynced = true
        )
        public Boolean enableZombiePigmanEntitySpawning = true;
    }

    public static class MusicConfig {
        @ConfigEntry(
                name = "Dimension Change Stops Background Music",
                description = "Using portals stops any active BGM"
        )
        public Boolean stopCurrentBgmOnPortalUse = true;

        @ConfigEntry(
                name = "Dimension Change Stops Level Specific Songs",
                description = "Using portals stops dimension specific songs"
        )
        public Boolean stopDimensionSpecificSongOnPortalUse = true;

        @ConfigEntry(
                name = "Disable Background Music",
                description = "Disables randomly playing background music"
        )
        public Boolean disableBackgroundMusic = false;

        @ConfigEntry(
                name = "Main Menu Theme Enabled",
                description = "Plays 'mainmenu' song file if one is found"
        )
        public Boolean mainMenuThemeEnabled = true;

        @ConfigEntry(name = "Main Menu Theme Overrides Background Music")
        public Boolean mainMenuThemeOverridesBGM = false;

        @ConfigEntry(
                name = "Minecraft Default Background Music Disabled",
                description = "Restart required for changes to take effect"
        )
        public Boolean disableDefaultMinecraftBGM = false;

        @ConfigEntry(
                name = "Music Countdown Random Interval Min",
                description = "Default Value: 12000",
                maxLength = Integer.MAX_VALUE
        )
        public Integer musicCoundownRandomIntervalMin = 12000;

        @ConfigEntry(
                name = "Music Countdown Random Interval Max",
                description = "Default Value: 12000",
                maxLength = Integer.MAX_VALUE
        )
        public Integer musicCoundownRandomIntervalMax = 12000;

        @ConfigEntry(
                name = "Overlay For Music In Debug Enabled",
                description = "If using Better F3 use its module config"
        )
        public Boolean overlayForMusicInDebug = true;

        @ConfigEntry(
                name = "Overlay For Music In Debug Y Offset",
                maxLength = 4096,
                minLength = -4096
        )
        public Integer overlayForMusicInDebugYOffset = 0;

        @ConfigEntry(
                name = "Volume: Nether Portal Ambient",
                description = "Float value between 0.0 (0%) and 1.0 (100%)",
                maxLength = 1
        )
        public Float volumeNetherPortalAmbient = 1.0F;

        @ConfigEntry(
                name = "Volume: Rain Ambient",
                description = "Float value between 0.0 (0%) and 1.0 (100%)",
                maxLength = 1
        )
        public Float volumeRainAmbient = 1.0F;

        @ConfigEntry(
                name = "Volume: Ghast Ambient",
                description = "Float value between 0.0 (0%) and 1.0 (100%)",
                maxLength = 1
        )
        public Float volumeGhastAmbient = 1.0F;

        @ConfigEntry(
                name = "Volume: Cave Ambient",
                description = "Float value between 0.0 (0%) and 1.0 (100%)",
                maxLength = 1
        )
        public Float volumeCaveAmbient = 1.0F;
    }

    public static class SleepConfig {
        @ConfigEntry(
                name = "Asleep Player Percentage For Skipping Night",
                description = "Float value between 0.0 (0%) and 1.0 (100%)",
                multiplayerSynced = true
        )
        public Float asleepPlayerPercentageForSkippingNight = 1.0f;

        @ConfigEntry(
                name = "Beds Speed Up Night Rather Than Skip It",
                multiplayerSynced = true
        )
        public Boolean bedsSpeedUpNightRatherThanSkipIt = false;

        @ConfigEntry(
                name = "Beds Speed Up Night Tick Rate",
                description = "1 = normal speed",
                multiplayerSynced = true,
                minLength = 1,
                maxLength = 100
        )
        public Integer bedsSpeedUpNightTickRate = 20;

        @ConfigEntry(
                name = "Beds Speed Up Night Tick World",
                description = "Night speed up ticks world as well",
                multiplayerSynced = true
        )
        public Boolean bedsSpeedUpNightTickWorld = true;

        @ConfigEntry(
                name = "Click Or Swing Hand To Exit Beds Enabled",
                multiplayerSynced = true
        )
        public Boolean handSwingClickToExitBedsEnabled = true;
    }

    public static class WeatherConfig {
        @ConfigEntry(
                name = "Always Snow Above Set Y Level Enabled",
                multiplayerSynced = true
        )
        public Boolean enableAlwaysSnowAboveSetYLevel = false;

        @ConfigEntry(
                name = "Always Snow Above This Y Level",
                multiplayerSynced = true,
                maxLength = 256
        )
        public Integer alwaysSnowAboveThisYLevel = 100;

        @ConfigEntry(
                name = "Disable Rain",
                multiplayerSynced = true
        )
        public Boolean disableRain = false;

        @ConfigEntry(
                name = "Disable Thunder",
                multiplayerSynced = true
        )
        public Boolean disableThunder = false;

        @ConfigEntry(
                name = "Disable Rain In Savanna Biomes",
                description = "Restart required for changes to take effect",
                multiplayerSynced = true
        )
        public Boolean disableRainInSavannaBiomes = false;

        @ConfigEntry(
                name = "Sleep Only Resets Weather When Raining",
                multiplayerSynced = true
        )
        public Boolean sleepOnlyResetsWeatherWhenRaining = false;

        @ConfigEntry(
                name = "Time Until Rain: Random Limit",
                description = "Added to minimum to find rain time",
                multiplayerSynced = true,
                maxLength = Integer.MAX_VALUE
        )
        public Integer timeUntilRainRandomLimit = 168000;

        @ConfigEntry(
                name = "Time Until Rain: Minimum",
                description = "Minimum time in ticks until rain occurs",
                multiplayerSynced = true,
                maxLength = Integer.MAX_VALUE
        )
        public Integer timeUntilRainMinimum = 12000;

        @ConfigEntry(
                name = "Time Rain Duration: Random Limit",
                description = "Added to minimum to find rain duration",
                multiplayerSynced = true,
                maxLength = Integer.MAX_VALUE
        )
        public Integer rainDurationRandomLimit = 12000;

        @ConfigEntry(
                name = "Time Rain Duration: Minimum",
                description = "Minimum time in ticks until rain ends",
                multiplayerSynced = true,
                maxLength = Integer.MAX_VALUE
        )
        public Integer rainDurationMinimum = 12000;

        @ConfigEntry(
                name = "Time Until Thunder: Random Limit",
                description = "Added to minimum to find thunder time",
                multiplayerSynced = true,
                maxLength = Integer.MAX_VALUE
        )
        public Integer timeUntilThunderRandomLimit = 168000;

        @ConfigEntry(
                name = "Time Until Thunder: Minimum",
                description = "Minimum time in ticks until thunder occurs",
                multiplayerSynced = true,
                maxLength = Integer.MAX_VALUE
        )
        public Integer timeUntilThunderMinimum = 12000;

        @ConfigEntry(
                name = "Time Thunder Duration: Random Limit",
                description = "Added to minimum to find thunder duration",
                multiplayerSynced = true,
                maxLength = Integer.MAX_VALUE
        )
        public Integer thunderDurationRandomLimit = 12000;

        @ConfigEntry(
                name = "Time Thunder Duration: Minimum",
                description = "Minimum time in ticks until thunder ends",
                multiplayerSynced = true,
                maxLength = Integer.MAX_VALUE
        )
        public Integer thunderDurationMinimum = 3600;
    }
}
