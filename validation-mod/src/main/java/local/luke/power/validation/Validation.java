package local.luke.power.validation;

import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.mixin.*;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.opengl.GL11;

public final class Validation {
  public static int inventoryMouseX, inventoryMouseY;
  public static final java.util.concurrent.atomic.AtomicInteger backgroundChunkReads = new java.util.concurrent.atomic.AtomicInteger();
  private static final Path COMMAND = Path.of("power-beta-validation.command"),
      REPORT = Path.of("power-beta-validation.log");
  private static int ticks;
  static int failures;

  interface Check {
    void run() throws Exception;
  }

  static void log(String s) throws Exception {
    Files.writeString(REPORT, s + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    System.out.println("POWER VALIDATION " + s);
  }

  static void test(String name, Check c) throws Exception {
    try {
      c.run();
      log("PASS " + name);
    } catch (Throwable e) {
      failures++;
      log("FAIL " + name + " " + e);
      e.printStackTrace();
    }
  }

  static void check(boolean okay, String message) {
    if (!okay) throw new AssertionError(message);
  }

  public static void tick(Minecraft mc) {
    Minimap441Checks.observe(mc);
    AutoMineChecks.observe(mc);
    Performance420Checks.tick(mc);
    HeldLight400Checks.tick(mc);
    Forest241Capture.tick(mc);
    Release300Checks.observe(mc);
    try { Release241Checks.observe(mc); } catch (Throwable failure) { try { log("FAIL native 2.4.1 observer "+failure); } catch (Exception ignored) {} }
    Release151SphereChecks.observe(mc);
    WebsiteMotion.tick(mc);
    if (++ticks % 5 != 0 || !Files.exists(COMMAND)) return;
    try {
      String command = Files.readString(COMMAND).trim();
      Files.delete(COMMAND);
      if (command.startsWith("music-playlists-460 ")) MusicPlaylists460Checks.run(mc, command.substring("music-playlists-460 ".length()));
      else if (command.equals("world-metadata-450")) WorldMetadata450Checks.run(mc);
      else if (command.equals("offhand-torch-451")) OffhandTorch451Checks.run(mc);
      else if (command.equals("sprint-stairs-451")) SprintStairs451Checks.run(mc);
      else if (command.equals("inventory-451") || command.startsWith("inventory-451 ")) Inventory451Checks.run(mc,command.length()==13 ? "test" : command.substring(14));
      else if (command.equals("photo-paused-450")) PhotoPausedTeleport450Checks.run(mc);
      else if (command.startsWith("photo-450 ")) Photo450Checks.run(mc,command.substring(10));
      else if (command.startsWith("map-450 ")) Map450Checks.run(mc,command.substring(8));
      else if (command.equals("dropdown-450 screens")) Dropdown450Screens.capture(mc);
      else if (command.equals("dropdown-450 capture")) Dropdown450Checks.capture(mc);
      else if (command.equals("dropdown-450")) Dropdown450Checks.run(mc);
      else if (command.equals("light-450")) Light450Checks.run(mc);
      else if (command.startsWith("map-449 ")) Map449Checks.run(mc,command.substring(8));
      else if (command.startsWith("photo-449 ")) Photo449Checks.run(mc,command.substring(10));
      else if (command.equals("waypoint-449")) Waypoint449Checks.run(mc);
      else if (command.equals("tweak-lifecycle-exit-448")) TweakLifecycle448Checks.boundary(mc);
      else if (command.equals("native-y-reporting-448")) NativeYReporting448Checks.run(mc);
      else if (command.equals("y-commands-448")) YCommand448Checks.run(mc);
      else if (command.equals("tweak-lifecycle-448")) TweakLifecycle448Checks.run(mc);
      else if (command.equals("camera-target-448")) CameraTarget448Checks.run(mc);
      else if (command.equals("placement-pose-448")) PlacementPose448Checks.run(mc);
      else if (command.equals("physical-pose-448")) PhysicalPose448Checks.run(mc);
      else if (command.startsWith("map-toggles-448")) MapToggles448Checks.run(mc, command.substring("map-toggles-448".length()).trim());
      else if (command.startsWith("mining-overlay-448 ")) MiningOverlay448Checks.run(mc, command.substring(19).trim());
      else if (command.equals("release-448 capture")) Release448Visuals.capture(mc);
      else if (command.equals("waypoint-448")) Waypoint448Checks.run(mc);
      else if (command.startsWith("recipe-hotkey-448 ")) RecipeHotkey448Checks.run(mc, command.substring(18));
      else if (command.startsWith("menu-423 ")) Menu423Checks.run(mc,command.substring(9));
      else if (command.startsWith("gallery-scene ")) WebsiteScenes.run(mc,command.substring(14));
      else if (command.startsWith("gallery-action ")) WebsiteActions.run(mc,command.substring(15));
      else if (command.startsWith("gallery-motion ")) WebsiteMotion.run(mc,command.substring(15));
      else if (command.startsWith("release-152")) Release152Checks.run(mc, command.substring(11).trim());
      else if (command.startsWith("release-160")) Release160Checks.run(mc, command.substring(11).trim());
      else if (command.startsWith("placement-lifecycle ")) PlacementLifecycleChecks.run(mc, command.substring(20));
      else if (command.startsWith("interface-170 ")) Interface170Checks.run(mc,command.substring(14));
      else if (command.equals("light-performance-230")) LightPerformance230.run(mc);
      else if (command.startsWith("release-230 ")) Editor230Checks.run(mc,command.substring(12));
      else if (command.startsWith("hud-responsive-230 ")) HudResponsive230Checks.run(mc,command.substring(19));
      else if (command.startsWith("release-250 ")) Release250Checks.run(mc,command.substring(12));
      else if (command.startsWith("release-251 ")) Release251BoatChecks.run(mc,command.substring(12));
      else if (command.equals("water-253")) Water253Checks.run(mc);
      else if (command.equals("release-253")) Release253Checks.run(mc);
      else if (command.startsWith("release-260 ")) Release260Checks.run(mc,command.substring(12));
      else if (command.startsWith("release-321 ")) Release321Checks.run(mc,command.substring(12));
      else if (command.startsWith("inventory-tools-400 ")) InventoryTools400Checks.run(mc,command.substring(20));
      else if (command.startsWith("inventory-400 ")) Inventory400Checks.run(mc,command.substring(14));
      else if (command.startsWith("subtitles-400 ")) Subtitles400Checks.run(mc,command.substring(14));
      else if (command.startsWith("held-light-400 ")) HeldLight400Checks.run(mc,command.substring(15));
      else if (command.startsWith("world-tools-400 ")) WorldTools400Checks.run(mc,command.substring(16));
      else if (command.startsWith("auto-tools-400 ")) AutoTools400Checks.run(mc,command.substring(15));
      else if (command.equals("saved-hotbars-400")) SavedHotbars400Checks.run(mc);
      else if (command.equals("death-restoration-400")) DeathRestoration400Checks.run(mc);
      else if (command.equals("kept-load-420")) KeptLoad420Checks.run(mc);
      else if (command.equals("tool-whitelist-420")) ToolWhitelist420Checks.run(mc);
      else if (command.equals("sorting-worlds-440")) Sorting440Checks.run(mc);
      else if (command.startsWith("settings-400 ")) HudSettings400Checks.run(mc,command.substring(13));
      else if (command.startsWith("input-400 ")) Input400Checks.run(mc,command.substring(10));
      else if (command.startsWith("offhand-actions-400 ")) OffhandActions400Checks.run(mc,command.substring(20));
      else if (command.equals("polish-400")) Polish400Checks.run(mc);
      else if ((command.equals("spectator-inventory-401") || command.equals("spectator-inventory-404"))) SpectatorInventory401Checks.run(mc);
      else if (command.startsWith("debug-410")) Debug410Checks.run(mc,command.substring(9).trim());
      else if (command.startsWith("release-420 ")) Release420Checks.run(mc,command.substring(12));
      else if (command.startsWith("performance-420 ")) Performance420Checks.run(mc, command.substring(16));
      else if (command.equals("vbo-421")) Vbo421Checks.run(mc);
      else if (command.equals("map-hot-421")) MapHot421Checks.run(mc);
      else if (command.equals("culling-421")) Culling421Checks.run(mc);
      else if (command.startsWith("performance-421 ")) Performance421Controls.run(mc, command.substring(16));
      else if (command.equals("terrain-cache-420")) TerrainCache420Checks.run(mc);
      else if (command.startsWith("mesh-water-430 ")) MeshWater430Checks.run(mc, command.substring(15).trim());
      else if (command.equals("water-columns-430")) WaterColumns430Checks.run(mc);
      else if (command.equals("leaf-lighting-430")) LeafLighting430Checks.run();
      else if (command.startsWith("mesh-water-421 ")) MeshWater421Checks.run(mc, command.substring(15).trim());
      else if (command.startsWith("mesh-cache-420 ")) MeshCache420Checks.run(mc, command.substring(15).trim());
      else if (command.equals("spectator-containers-404")) SpectatorContainer404Checks.run(mc);
      else if (command.equals("sorting-401")) Sorting401Checks.run(mc);
      else if (command.equals("recipe-401")) Recipe401Checks.run(mc);
      else if (command.equals("recipe-402")) Recipe402Checks.run(mc);
      else if (command.equals("commands-403")) Commands403Checks.run(mc);
      else if (command.equals("sorting-403")) Sorting403Checks.run(mc);
      else if (command.startsWith("controls-403 ")) Controls403Checks.run(mc,command.substring(13));
      else if (command.equals("sorting-layer-402")) SortingLayer402Checks.run(mc);
      else if (command.equals("container-offhand-402")) ContainerOffhand402Checks.run(mc);
      else if (command.equals("release-440")) Release440Checks.run(mc);
      else if (command.equals("current-formats-442")) CurrentFormats442Checks.run(mc);
      else if (command.equals("interface-442")) Interface442Checks.run(mc);
      else if (command.equals("palette-442")) Palette442Checks.run(mc);
      else if (command.equals("text-inputs-444")) TextInputs444Checks.run(mc);
      else if (command.startsWith("creative-443-containers")) Creative443ContainerChecks.run(mc, command.substring(23).trim());
      else if (command.startsWith("inventory-442")) Inventory442Checks.run(mc, command.substring(13).trim());
      else if (command.equals("player-fire-extinguish-448")) PlayerFireExtinguish448Checks.run(mc);
      else if (command.equals("fluid-sounds-440")) FluidSounds440Checks.run(mc);
      else if (command.equals("fluid-sounds-420")) FluidSounds420Checks.run(mc);
      else if (command.equals("water-sounds-432")) WaterSounds432Checks.run(mc);
      else if (command.equals("extra-sound-decoders-432")) ExtraSoundDecoder432Checks.run(mc);
      else if (command.equals("command-names-402")) CommandNames402Checks.run(mc);
      else if (command.startsWith("recipe-400 ")) Recipe400Checks.run(mc,command.substring(11));
      else if (command.startsWith("release-330 ")) Release330Checks.run(mc,command.substring(12));
      else if (command.startsWith("release-322 ")) Release322Checks.run(mc,command.substring(12));
      else if (command.startsWith("release-320 ")) Release320Checks.run(mc,command.substring(12));
      else if (command.startsWith("release-310 ")) Release310Checks.run(mc,command.substring(12));
      else if (command.startsWith("release-261 ")) Release261Checks.run(mc,command.substring(12));
      else if (command.equals("release-300-freecam-commands")) Release300FreecamCommandsChecks.run(mc);
      else if (command.equals("release-300-rule-editors")) RuleEditorRenderChecks.run(mc);
      else if (command.equals("release-300-map-light")) MapLightShadingChecks.run(mc);
      else if (command.equals("release-300-mining-air")) Release300MiningAirChecks.run(mc);
      else if (command.equals("release-300-exact-frames")) Release300FrameChecks.run(mc);
      else if (command.equals("release-300-sprint-polish")) Release300SprintPolishChecks.run(mc);
      else if (command.equals("release-300-ui-polish")) Release300UiPolishChecks.check(mc);
      else if (command.equals("release-300-waypoint-polish")) Release300WaypointPolishChecks.run(mc);
      else if (command.equals("release-300-extra-sounds")) Release300ExtraSoundsChecks.run(mc);
      else if (command.equals("release-300-rerelease") || command.startsWith("release-300-rerelease "))
        Release300RereleaseChecks.run(mc, command.substring("release-300-rerelease".length()).trim());
      else if (command.startsWith("release-300 ")) Release300Checks.run(mc,command.substring(12));
      else if (command.equals("release-252")) Release252Checks.run(mc);
      else if (command.startsWith("release-242 ")) Release242Checks.run(mc,command.substring(12));
      else if (command.startsWith("release-241 ")) Release241Checks.run(mc,command.substring(12));
      else if (command.startsWith("release-240 ")) Release240Checks.run(mc,command.substring(12));
      else if (command.startsWith("release-222 ")) Release222Checks.run(mc,command.substring(12));
      else if (command.startsWith("release-221 ")) Release221Checks.run(mc,command.substring(12));
      else if (command.equals("release-220")) Release220Checks.run(mc);
      else if (command.equals("release-211")) Release211Checks.run(mc);
      else if (command.equals("title-alignment")) TitleAlignmentChecks.run(mc);
      else if (command.equals("release-211-editor")) Release211Checks.editor(mc);
      else if (command.equals("release-210")) Release210Checks.run(mc);
      else if (command.equals("release-202")) Release202Checks.run(mc);
      else if (command.startsWith("release-201 ")) Release201Checks.run(mc,command.substring(12));
      else if (command.startsWith("release-175 ")) Release175Checks.run(mc,command.substring(12));
      else if (command.equals("water-terrain")) WaterTerrainChecks.run(mc);
      else if (command.startsWith("release-176 ")) Release176Checks.run(mc,command.substring(12));
      else if (command.startsWith("release-174 ")) Release174Checks.run(mc,command.substring(12));
      else if (command.startsWith("release-173 ")) Release173Checks.run(mc,command.substring(12));
      else if (command.equals("release-172")) Release172Checks.run(mc);
      else if (command.startsWith("release-171 ")) Release171Checks.run(mc,command.substring(12));
      else if (command.equals("mining-170")) Mining170Checks.run(mc);
      else if (command.equals("release-162")) Release162Checks.run(mc);
      else if (command.startsWith("freecam-motion ")) FreecamMotionChecks.run(mc, command.substring(15));
      else if (command.startsWith("freecam-preview ")) FreecamPreviewChecks.run(mc, command.substring(16));
      else if (command.equals("cape-checks")) CapeChecks.run(mc);
      else if (command.equals("music-collapse")) MusicCollapseChecks.run(mc);
      else if (command.equals("cactus-items")) CactusChecks.run(mc);
      else if (command.equals("menu-scrub")) MenuScrubChecks.run(mc);
      else if (command.startsWith("photo-controls ")) PhotoControlsChecks.run(mc, command.substring(15));
      else if (command.startsWith("skin ")) SkinChecks.run(mc, command.substring(5).trim());
      else if (command.startsWith("release-151")) Release151Checks.run(mc, command.substring(11).trim());
      else if (command.equals("interaction-150")) Release150InteractionChecks.run(mc);
      else if (command.startsWith("gallery-web ")) WebsiteGallery.run(mc,command.substring(12));
      else if (command.startsWith("gallery-detail ")) WebsiteDetails.run(mc,command.substring(15));
      else if (command.startsWith("gallery-150 ")) Release150Gallery.run(mc,command.substring(12));
      else if (command.startsWith("render-150 ")) Render150Checks.run(mc,command.substring(11));
      else if (command.startsWith("audio-150 ")) Release150AudioChecks.run(mc,command.substring(10));
      else if (command.startsWith("audio-previous ")) AudioPreviousChecks.run(mc,command.substring(15));
      else if (command.startsWith("tnt-performance")) TntPerformanceChecks.run(mc,command.length()>15 ? command.substring(15).trim() : "check");
      else if (command.startsWith("release-150")) Release150Checks.run(mc, command.length() > 11 ? command.substring(11).trim() : "check");
      else if (command.startsWith("gallery ")) GalleryChecks.run(mc, command.substring(8));
      else if (command.startsWith("world-settings-")) WorldSettingsChecks.run(mc,command.substring(15));
      else if (command.equals("cleanup")) CleanupChecks.run(mc);
      else if (command.equals("sign-editing")) SignEditingChecks.run(mc);
      else if (command.equals("obsidian-mining")) ObsidianMiningChecks.run(mc);
      else if (command.startsWith("water-landings")) WaterLandingChecks.run(mc, command.length() > 15 ? command.substring(15) : "check");
      else if (command.startsWith("bucket-stacks")) BucketStackChecks.run(mc, command.length() > 13 ? command.substring(13).trim() : "check");
      else if (command.equals("toggle-performance")) TogglePerformanceChecks.run(mc);
      else if (command.startsWith("toggle-saves")) ToggleSaveChecks.run(mc, command.length() > 13 ? command.substring(13) : "check");
      else if (command.equals("worldedit-navigation")) WorldEditNavigationChecks.run(mc);
      else if (command.startsWith("death-crafting")) DeathCraftingChecks.run(mc, command.length() > 14 ? command.substring(14).trim() : "check");
      else if (command.equals("worldedit-hand")) WorldEditHandChecks.run(mc);
      else if (command.startsWith("toggle-messages")) ToggleMessageChecks.run(mc, command.length() > 16 ? command.substring(16) : "check");
      else if (command.equals("settings-review")) SettingsReviewChecks.run(mc);
      else if (command.startsWith("settings-snapshot ")) SettingsSnapshot.write(mc, command.substring(18));
      else if (command.equals("cheats-creation")) CheatsChecks.creation(mc);
      else if (command.startsWith("indicators-")) IndicatorsMenuChecks.run(mc,command.substring(11));
      else if (command.startsWith("movement-")) InventoryMovementChecks.run(mc,command.substring(9));
      else if (command.equals("world-rules")) WorldRulesChecks.permissions(mc);
      else if (command.startsWith("rules-restart ")) RuleRestartChecks.run(mc, command.substring(14));
      else if (command.equals("rule-behavior")) RuleBehaviorChecks.run(mc);
      else if (command.startsWith("quick-transfer")) QuickTransferChecks.run(mc, command.substring(14).trim());
      else if (command.equals("creation-rules")) CreationRuleChecks.run(mc);
      else if (command.equals("compatibility-defaults")) CompatibilityDefaultsChecks.run(mc);
      else if (command.equals("cheats")) CheatsChecks.run(mc);
      else if (command.equals("modern-commands")) ModernCommandChecks.run(mc);
      else if (command.startsWith("audio-presets-")) AudioPresetChecks.run(mc,command.substring(14));
      else if (command.startsWith("audio-start-")) AudioStartChecks.run(command.substring(12));
      else if (command.startsWith("audio-groups-")) AudioGroupChecks.run(mc,command.substring(13));
      else if (command.equals("audio-maintenance")) AudioMaintenanceChecks.run(mc);
      else if (command.equals("inline-values")) InlineValueChecks.run(mc);
      else if (command.equals("sticky-groups")) StickyGroupChecks.run(mc);
      else if (command.equals("audio-refinements")) AudioRefinementChecks.run(mc);
      else if (command.equals("audio-exclusions")) AudioExclusionChecks.run(mc);
      else if (command.startsWith("music-eras-")) MusicErasChecks.run(mc,command.substring(11));
      else if (command.startsWith("audio-polish-")) AudioPolishChecks.run(mc,command.substring(13));
      else if (command.startsWith("audio-layout-")) AudioLayoutChecks.run(mc,command.substring(13));
      else if (command.equals("fog-array")) FogArrayChecks.run(mc);
      else if (command.startsWith("library-check-")) CommandsLibraryChecks.run(mc,command.substring(14));
      else if (command.equals("menu-persisted")) MenuUpdateChecks.persisted(mc);
      else if (command.startsWith("music-update-")) MusicUpdateChecks.run(mc,command.substring(13));
      else if (command.equals("menu-update")) MenuUpdateChecks.run(mc);
      else if (command.equals("equipment-swap")) LightInventoryChecks.inventory(mc);
      else if (command.startsWith("overlay-")) LightInventoryChecks.light(mc,command.substring(8));
      else if (command.equals("split-restart-start") || command.equals("split-restart-finish")) ChestJoinChecks.restart(mc,command.endsWith("finish"));
      else if (command.startsWith("preview-audio-")) PreviewPickerChecks.audio(mc,command.substring(14));
      else if (command.equals("pack-picker")) PreviewPickerChecks.picker(mc);
      else if (command.equals("filter-navigation")) FilteredNavigationChecks.run(mc);
      else if (command.equals("chest-join")) ChestJoinChecks.run(mc);
      else if (command.equals("carry-hold") || command.equals("carry-release")) NavigationCarryChecks.hold(mc,command.equals("carry-release"));
      else if (command.equals("carry-resume-check")) NavigationCarryChecks.resumed(mc);
      else if (command.equals("navigation-check")) NavigationCarryChecks.menu(mc);
      else if (command.equals("double-carry") || command.equals("carry-demo")) NavigationCarryChecks.carry(mc,command.equals("carry-demo"));
      else if (command.startsWith("feature-")) FeatureChecks.run(mc,command.substring(8));
      else if (command.startsWith("revision-")) RevisionChecks.run(mc,command.substring(9));
      else if (command.startsWith("keys-")) InputChecks.run(mc, command.substring(5));
      else if (command.equals("creative-vehicles")) CreativeVehicleChecks.run(mc);
      else if (command.startsWith("release-110-")) Release110InputChecks.run(mc,command.substring(12));
      else if (command.startsWith("release-111")) Release111Checks.run(mc,command.length()>12 ? command.substring(12) : "check");
      else if (command.startsWith("portal-inventory")) PortalInventoryChecks.run(mc,command.length()>17 ? command.substring(17) : "check");
      else if (command.startsWith("crafting-grid")) CraftingGridChecks.run(mc,command.length()>14 ? command.substring(14) : "check");
      else if (command.startsWith("freecam-walk")) FreecamWalkChecks.run(mc,command.length()>13 ? command.substring(13) : "check");
      else if (command.startsWith("low-fire")) LowFireChecks.run(mc,command.length()>9 ? command.substring(9) : "check");
      else if (command.startsWith("damage-camera")) DamageCameraChecks.run(mc,command.length()>14 ? command.substring(14) : "check");
      else if (command.startsWith("auto-mine")) AutoMineChecks.run(mc,command.length()>10 ? command.substring(10) : "check");
      else if (command.equals("release-110")) Release110Checks.run(mc);
      else if (command.equals("command-completion")) CommandCompletionChecks.run(mc);
      else if (command.equals("release-300-chat-break") || command.startsWith("release-300-chat-break "))
        Release300ChatBreakChecks.run(mc, command.substring("release-300-chat-break".length()).trim());
      else if (command.equals("swimming-300")) Swimming300Checks.run(mc);
      else if (command.equals("crawling-420")) Crawling420Checks.run(mc);
      else if (command.startsWith("release-300-stamina ")) Release300StaminaHudChecks.visual(mc, command.substring("release-300-stamina ".length()).trim());
      else if (command.equals("release-300-marker-refinements")) Release300MarkerRefinementsChecks.run(mc);
      else if (command.equals("redstone-sounds")) RedstoneSoundChecks.run(mc);
      else if (command.equals("redstone-visuals")) RedstoneVisualChecks.run(mc);
      else if (command.startsWith("creative-443-movement ")) Creative443MovementChecks.run(mc, command.substring("creative-443-movement ".length()).trim());
      else if (command.equals("creative-440")) Creative440Checks.run(mc);
      else if (command.startsWith("creative-440 ")) Creative440PointerChecks.run(mc, command.substring("creative-440 ".length()).trim());
      else if (command.equals("recipe-441")) Recipe441Checks.run(mc);
      else if (command.equals("recipe-availability-444")) RecipeAvailability444Checks.run(mc);
      else if (command.equals("current-formats-442")) CurrentFormats442Checks.run(mc);
      else if (command.startsWith("creative-442")) Creative442Checks.run(mc, command.substring(12).trim());
      else if (command.startsWith("minimap-446")) Minimap446Checks.run(mc, command.substring("minimap-446".length()).trim());
      else if (command.startsWith("map-ui-461")) MapUi461Checks.run(mc, command.substring("map-ui-461".length()).trim());
      else if (command.startsWith("audio-ui-461")) AudioUi461Checks.run(mc, command.substring("audio-ui-461".length()).trim());
      else if (command.startsWith("release-462")) Release462Checks.run(mc, command.substring("release-462".length()).trim());
      else if (command.startsWith("release-463")) Release463Checks.run(mc, command.substring("release-463".length()).trim());
      else if (command.startsWith("release-464-audio")) Release464AudioChecks.run(mc, command.substring("release-464-audio".length()).trim());
      else if (command.startsWith("release-464-map-logo")) Release464MapLogoChecks.run(mc, command.substring("release-464-map-logo".length()).trim());
      else if (command.startsWith("release-464-music-ui")) Release464MusicUiChecks.run(mc, command.substring("release-464-music-ui".length()).trim());
      else if (command.startsWith("release-464-shared-ui")) Release464SharedUiChecks.run(mc, command.substring("release-464-shared-ui".length()).trim());
      else if (command.startsWith("audio-transition-461")) AudioTransition461Checks.run(mc, command.substring("audio-transition-461".length()).trim());
      else if (command.startsWith("minimap-444")) Minimap444Checks.run(mc,command.substring("minimap-444".length()).trim());
      else if (command.startsWith("inventory-look-445")) InventoryLook445Checks.run(mc, command.substring("inventory-look-445".length()).trim());
      else if (command.startsWith("ui-447")) Ui447Checks.run(mc, command.substring("ui-447".length()).trim());
      else if (command.startsWith("ui-446")) Ui445Checks.run446(mc, command.substring("ui-446".length()).trim());
      else if (command.startsWith("ui-445")) Ui445Checks.run(mc, command.substring("ui-445".length()).trim());
      else if (command.equals("gui-items-444")) GuiItems444Checks.run(mc);
      else if (command.equals("creative-444")) Creative444Checks.run(mc);
      else if (command.startsWith("creative-443-revision")) Creative443RevisionChecks.run(mc, command.substring("creative-443-revision".length()).trim());
      else if (command.equals("creative-443") || command.startsWith("creative-443 ")) Creative443Checks.run(mc, command.substring("creative-443".length()).trim());
      else if (command.startsWith("recipe-441 ")) Recipe441Checks.physical(mc, command.substring(11));
      else if (command.startsWith("minimap-441 ")) Minimap441Checks.run(mc, command.substring(12));
      else if (command.equals("creative-borders")) CreativeBorderChecks.run(mc);
      else if (command.equals("creative-drops")) CreativeDropChecks.run(mc);
      else if (command.startsWith("creative-paint")) CreativePaintChecks.run(mc, command.length() > 14 ? command.substring(14).trim() : "check");
      else if (command.equals("chat-check")) ChatChecks.run(mc);
      else if (command.equals("chat-wrap")) ChatWrapChecks.run(mc);
      else if (command.equals("freecam-culling")) FreecamCullingChecks.run(mc);
      else if (command.equals("freecam-distance")) FreecamDistanceChecks.run(mc);
      else if (command.startsWith("death-checks")) DeathChecks.run(mc, command.length() > 12 ? command.substring(12).trim() : "check");
      else if (command.startsWith("keep-inventory")) KeepInventoryChecks.run(mc, command.length() > 14 ? command.substring(14).trim() : "check");
      else if (command.equals("help-audit")) HelpAuditChecks.run(mc);
      else if (command.equals("freecam-handoff")) FreecamHandoffChecks.run(mc);
      else if (command.startsWith("chat ")) ChatChecks.submit(mc, command.substring(5));
      else if (command.equals("audit")) audit(mc);
      else if (command.equals("ui-check")) UiChecks.run(mc);
      else if (command.equals("texture-check")) TextureChecks.run(mc);
      else if (command.equals("roundtrip")) roundtrip(mc);
      else if (command.equals("screens")) screens(mc);
      else if (command.startsWith("options")) {
        PowerOptionsScreen s = new PowerOptionsScreen(mc.currentScreen);
        if (command.length() > 8) {
          Field p = PowerOptionsScreen.class.getDeclaredField("page");
          p.setAccessible(true);
          p.set(s, command.substring(8));
        }
        mc.setScreen(s);
      } else if (command.equals("audio")) audio(mc);
      else if (command.equals("input")) input(mc);
      else if (command.startsWith("set ")) {
        String[] parts = command.split(" ", 3);
        ConfigSession s = SettingsRegistry.open(mc);
        find(s, parts[1]).value = JsonParser.parseString(parts[2]);
        s.save(Path.of(".").toAbsolutePath());
      } else if ((command.equals("defaults-check") || command.equals("profile-defaults"))) {
        failures = 0;
        ConfigSession s = SettingsRegistry.open(mc);
        for (Setting setting : s.settings())
          test(
              "pack default " + setting.id,
              () ->
                  check(
                      (command.equals("profile-defaults") && setting.id.equals("native.guiScale") && setting.value.getAsInt()==4)
                          || (command.equals("profile-defaults") && setting.id.equals("native.sensitivity") && Math.abs(setting.value.getAsDouble()-80)<.001)
                          || setting.value.equals(setting.defaultValue)
                          || setting.value.isJsonPrimitive()
                              && setting.value.getAsJsonPrimitive().isNumber()
                              && Math.abs(
                                      setting.value.getAsDouble()
                                          - setting.defaultValue.getAsDouble())
                                  < .000001,
                      "Expected " + setting.defaultValue + " got " + setting.value));
        log("DEFAULT CHECK TOTAL " + s.settings().size() + " FAILURES " + failures);
      } else if (command.equals("new-world")) {
        mc.interactionManager = new net.minecraft.SingleplayerInteractionManager(mc);
        mc.method_2120("Power Beta release check", "Power Beta release check", 17320261003L);
        if (mc.player.dimensionId == -1) mc.method_2139();
        mc.setScreen(null);
        // Direct fixture loading can render weather entities before the first world frame.
        net.minecraft.client.render.entity.EntityRenderDispatcher.field_2489.method_1917(
            mc.world, mc.textureManager, mc.textRenderer, mc.player, mc.options, 0F);
      } else if (command.equals("quit")) {
        mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
        mc.setWorld(null);
        mc.scheduleStop();
      } else if (command.startsWith("music-")) music(mc, command.substring(6));
      else if (command.equals("pause")) mc.setScreen(new net.minecraft.class_525());
      else if (command.equals("defaults")) {
        ConfigSession s = SettingsRegistry.open(mc);
        s.settings().forEach(Setting::reset);
        s.save(Path.of(".").toAbsolutePath());
        log("DEFAULTS SAVED " + s.settings().size());
      } else if (command.equals("dump"))
        Files.writeString(
            Path.of("power-beta-full-catalog.json"),
            Catalog.JSON.toJson(SettingsRegistry.open(mc).settings()));
      else if (command.equals("title")) {
        mc.setWorld(null);
        mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
      }
      log("COMMAND DONE " + command);
    } catch (Throwable e) {
      try {
        Throwable cause = e;
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (cause.getCause() != null && seen.add(cause)) cause = cause.getCause();
        log("COMMAND FAILED " + cause);
        var details = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(details));
        log(details.toString());
      } catch (Exception ignored) {
      }
      e.printStackTrace();
    }
  }

  static void audit(Minecraft mc) throws Exception {
    failures = 0;
    ConfigSession session = SettingsRegistry.open(mc);
    Set<String> ids = new HashSet<>();
    int n = 0;
    for (Setting s : session.settings()) {
      test(
          "schema " + s.id,
          () -> {
            check(ids.add(s.id), "duplicate");
            s.validate(s.defaultValue);
            s.validate(s.value);
            check(PowerOptionsScreen.PAGES.contains(s.page), "unknown page");
            check(
                !s.label.matches("(?i).*(ControlFeatures|MojangFix|Lpke|key\\.).*"),
                "untranslated label");
          });
      n++;
      if (s.kind == Setting.Kind.BOOLEAN
          || s.kind == Setting.Kind.CHOICE
          || s.kind == Setting.Kind.INTEGER
          || s.kind == Setting.Kind.DECIMAL) {
        JsonElement old = s.value.deepCopy();
        s.cycle(1);
        s.cycle(-1);
        check(
            s.value.equals(old) || Math.abs(s.value.getAsDouble() - old.getAsDouble()) < 0.000001,
            "inverse cycle " + s.id);
        s.value = old;
      }
    }
    log("AUDIT TOTAL " + n + " FAILURES " + failures);
  }

  static Setting find(ConfigSession s, String id) {
    return s.settings().stream().filter(e -> e.id.equals(id)).findFirst().orElseThrow();
  }

  static void roundtrip(Minecraft mc) throws Exception {
    failures = 0;
    ConfigSession s = SettingsRegistry.open(mc);
    Map<String, JsonElement> old = new LinkedHashMap<>();
    Map<String, JsonElement> edits = new LinkedHashMap<>();
    edits.put("native.music", new JsonPrimitive(43));
    edits.put("audio.category.blocks", new JsonPrimitive(35));
    edits.put("creative.glide", new JsonPrimitive(2));
    edits.put("worldedit.opacity", new JsonPrimitive(70));
    edits.put("power_hud:config.chatHistorySize", new JsonPrimitive(101));
    edits.put("logo.logo.animation.enabled", new JsonPrimitive(false));
    Object[] inventory = mc.player == null ? null : mc.player.inventory.main.clone();
    for (var e : edits.entrySet()) {
      Setting a = find(s, e.getKey());
      old.put(e.getKey(), a.value.deepCopy());
      a.value = e.getValue();
    }
    try {
      s.save(Path.of(".").toAbsolutePath());
      ConfigSession reread = SettingsRegistry.open(mc);
      for (var e : edits.entrySet())
        test(
            "save and reload " + e.getKey(),
            () -> {
              JsonElement actual = find(reread, e.getKey()).value;
              check(
                  actual.equals(e.getValue())
                      || actual.isJsonPrimitive()
                          && actual.getAsJsonPrimitive().isNumber()
                          && actual.getAsDouble() == e.getValue().getAsDouble(),
                  "Saved value differs: " + actual);
            });
      if (inventory != null)
        test(
            "menu edits preserve inventory object identity",
            () -> {
              for (int i = 0; i < inventory.length; i++)
                check(inventory[i] == mc.player.inventory.main[i], "Inventory changed");
            });
    } finally {
      ConfigSession restore = SettingsRegistry.open(mc);
      old.forEach((id, value) -> find(restore, id).value = value);
      restore.save(Path.of(".").toAbsolutePath());
    }
    log("ROUNDTRIP FAILURES " + failures);
  }

  static void screens(Minecraft mc) throws Exception {
    failures = 0;
    Screen old = mc.currentScreen;
    for (int[] size : new int[][] {{320, 240}, {427, 240}, {550, 380}, {854, 480}})
      for (String page : PowerOptionsScreen.PAGES)
        test(
            "render " + page + " " + size[0] + "x" + size[1],
            () -> {
              PowerOptionsScreen screen = new PowerOptionsScreen(old);
              Field p = PowerOptionsScreen.class.getDeclaredField("page");
              p.setAccessible(true);
              p.set(screen, page);
              screen.init(mc, size[0], size[1]);
              while (GL11.glGetError() != GL11.GL_NO_ERROR) {}
              screen.render(-1, -1, 0);
              check(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL error");
              screen.removed();
            });
    mc.setScreen(old);
    log("SCREEN FAILURES " + failures);
  }

  static void audio(Minecraft mc) throws Exception {
    failures = 0;
    AudioSettings old = AudioConfig.copy();
    test(
        "cached sound resources loaded",
        () ->
            check(
                AudioController.sounds(mc).size() > 50,
                "Only " + AudioController.sounds(mc).size() + " sounds"));
    test(
        "cached music resources loaded",
        () -> check(AudioController.music(mc).size() >= 3, "No vanilla playlist"));
    test(
        "empty saved playlists keep an empty automatic pool",
        () -> {
          var empty=AudioConfig.copy();MusicFixtureSupport.selection(empty,List.of());
          try { AudioConfig.preview(empty);check(AudioController.activeMusic(mc).isEmpty(),"Empty saved playlist expanded"); }
          finally {AudioConfig.preview(old);}
        });
    try {
      AudioSettings draft = AudioConfig.copy();
      draft.master = 50;
      draft.categories.put("hostile", 40);
      draft.sounds.put("mob.zombie", 25);
      AudioConfig.save(draft);
      test(
          "runtime category and individual mixing",
          () ->
              check(
                  Math.abs(AudioController.mix("validation", "mob.zombie", 1, false) - .05)
                      < .00001,
                  "Incorrect gain"));
      draft.master = 0;
      AudioConfig.save(draft);
      test(
          "master mute is exact",
          () -> check(AudioController.mix("validation", "mob.zombie", 1, false) == 0, "Not muted"));
    } finally {
      AudioConfig.save(old);
    }
    log("AUDIO FAILURES " + failures);
  }

  private static AudioSettings musicBefore;
  private static float volumeBefore;

  static void music(Minecraft mc, String action) throws Exception {
    var system = SoundManagerAccessor.power$system();
    if (action.equals("setup")) {
      musicBefore = AudioConfig.copy();
      volumeBefore = mc.options.musicVolume;
      AudioSettings s = AudioConfig.copy();
      s.musicDirectories = List.of("validation-music");
      MusicFixtureSupport.imported(s, false);
      s.master = 100;
      s.shuffle = false;
      s.sounds.put("music:Test song été.wav", 50);
      s.sounds.put("music:Test song two.wav", 50);
      AudioConfig.save(s);
      mc.options.musicVolume = .7f;
    } else if (action.equals("next")) AudioController.next();
    else if (action.equals("check")) {
      failures = 0;
      test(
          "custom library loads Unicode filenames",
          () ->
              check(
                  AudioController.customTracks().size() == 2,
                  "tracks=" + AudioController.customTracks().size()));
      test(
          "custom WAV plays through native sound engine",
          () ->
              check(
                  system.playing("BgMusic") && AudioController.nowPlaying().startsWith("Test song"),
                  "Playing " + AudioController.nowPlaying()));
      test(
          "custom track volume multiplies native music",
          () ->
              check(
                  Math.abs(system.getVolume("BgMusic") - .35) < .001,
                  "Volume=" + system.getVolume("BgMusic")));
      test(
          "music debug HUD follows custom playback",
          () ->
              check(
                  Class.forName("local.luke.power.environment.ModHelper$ModHelperFields")
                      .getField("currentBGM")
                      .get(null)
                      .equals(AudioController.nowPlaying()),
                  "Debug track stale"));
      AudioController.togglePause();
      log("MUSIC FAILURES " + failures);
    } else if (action.equals("paused")) {
      test(
          "music pause keeps stream stopped",
          () ->
              check(
                  !system.playing("BgMusic") && AudioController.status().matches("Music paused|Paused: .+"),
                  "Pause ignored"));
      AudioController.togglePause();
    } else if (action.equals("resumed")) {
      test(
          "music resumes without replacing stream",
          () -> check(system.playing("BgMusic"), "Resume ignored"));
      AudioSettings s = AudioConfig.copy();
      MusicFixtureSupport.imported(s, true);
      AudioConfig.save(s);
    } else if (action.equals("add")) {
      var pool = ((SoundManagerAccessor) mc.soundManager).power$music();
      List<net.minecraft.class_267> nativeTracks;
      synchronized (pool) {
        nativeTracks = List.copyOf(((SoundPoolAccessor) pool).power$tracks());
      }
      Set<String> found = new HashSet<>();
      for (int i = 0; i < nativeTracks.size() + 2; i++)
        found.add(AudioController.choose().field_2127.toExternalForm());
      test(
          "saved playlist retains every Beta and imported track",
          () -> check(found.size() == BuiltinMusic.VANILLA.size() + 2, "Only " + found.size()));
      AudioSettings s = AudioConfig.copy();
      MusicFixtureSupport.imported(s, false);
      s.musicDirectories = List.of("missing-validation-folder");
      AudioConfig.save(s);
    } else if (action.equals("fallback")) {
      var pool = ((SoundManagerAccessor) mc.soundManager).power$music();
      List<net.minecraft.class_267> nativeTracks;
      synchronized (pool) {
        nativeTracks = List.copyOf(((SoundPoolAccessor) pool).power$tracks());
      }
      test(
          "missing imported references do not expand saved playlist",
          () ->
              check(
                  AudioController.choose() == null, "Missing references admitted unrelated tracks"));
      test(
          "scanner never creates missing folders",
          () -> check(!Files.exists(Path.of("missing-validation-folder")), "Folder was created"));
      log("MUSIC FINAL FAILURES " + failures);
    } else if (action.equals("restore")) {
      AudioConfig.save(musicBefore);
      mc.options.musicVolume = volumeBefore;
      AudioController.next();
    } else if (action.equals("menu-check")) {
      failures = 0;
      test(
          "custom menu music plays",
          () -> check(system.playing("PowerBetaMenu"), "No menu stream"));
      test(
          "menu override prevents overlapping background music",
          () -> check(!system.playing("BgMusic"), "Overlapping music"));
      log("MENU MUSIC FAILURES " + failures);
    } else if (action.equals("world-check")) {
      test(
          "world entry stops menu music",
          () -> check(!system.playing("PowerBetaMenu"), "Menu music continued in world"));
      log("TRANSITION MUSIC FAILURES " + failures);
    }
  }

  static void input(Minecraft mc) throws Exception {
    failures = 0;
    Screen previous = mc.currentScreen;
    PowerOptionsScreen screen = new PowerOptionsScreen(previous);
    mc.setScreen(screen);
    var input = (local.luke.power.validation.mixin.ScreenInput) (Object) screen;
    test(
        "search filters the shared settings menu",
        () -> {
          int left = Math.max(90, Math.min(155, screen.width / 4)) + 16;
          input.power$click(left + 8, 34, 0);
          for (char c : "Master volume".toCharArray()) input.power$key(c, 0);
          Field rows = PowerOptionsScreen.class.getDeclaredField("rows");
          rows.setAccessible(true);
          int count = ((List<?>) rows.get(screen)).size();
          check(count >= 2 && count < 20, "Search did not filter volume settings: " + count);
        });
    Setting master = find(screen.session(), "audio.master");
    JsonElement original = master.value.deepCopy();
    int controlX = screen.width - 100,
        controlY =
            screen.width - (Math.max(90, Math.min(155, screen.width / 4)) + 16) - 14 < 340
                ? 95
                : 81;
    test(
        "left then right click restores a setting",
        () -> {
          input.power$click(controlX, controlY, 0);
          check(!master.value.equals(original), "Left click ignored");
          input.power$click(controlX, controlY, 1);
          check(master.value.equals(original), "Right click ignored");
        });
    test(
        "Escape cancels key capture",
        () -> {
          Field capture = PowerOptionsScreen.class.getDeclaredField("capture");
          capture.setAccessible(true);
          Setting key =
              screen.session().settings().stream()
                  .filter(s -> s.kind == Setting.Kind.KEY)
                  .findFirst()
                  .orElseThrow();
          JsonElement old = key.value.deepCopy();
          capture.set(screen, key);
          input.power$key('\0', org.lwjgl.input.Keyboard.KEY_ESCAPE);
          check(key.value.equals(old) && capture.get(screen) == null, "Escape changed binding");
        });
    test(
        "Delete clears and mouse input binds keys",
        () -> {
          Field capture = PowerOptionsScreen.class.getDeclaredField("capture");
          capture.setAccessible(true);
          Setting key =
              screen.session().settings().stream()
                  .filter(s -> s.kind == Setting.Kind.KEY)
                  .findFirst()
                  .orElseThrow();
          JsonElement old = key.value.deepCopy();
          capture.set(screen, key);
          input.power$key('\0', org.lwjgl.input.Keyboard.KEY_DELETE);
          check(key.value.getAsInt() == 0, "Delete ignored");
          capture.set(screen, key);
          input.power$click(0, 0, 3);
          check(key.value.getAsInt() == -97, "Mouse binding ignored");
          key.value = old;
        });
    test(
        "Cancel and discard leave live settings untouched",
        () -> {
          master.value = new JsonPrimitive(35);
          input.power$click(screen.width - 120, screen.height - 20, 0);
          input.power$click(screen.width / 2 + 10, screen.height / 2 + 20, 0);
          check(mc.currentScreen == previous, "Discard did not close");
          check(
              find(SettingsRegistry.open(mc), "audio.master").value.equals(original),
              "Draft leaked");
        });
    mc.setScreen(previous);
    log("INPUT FAILURES " + failures);
  }
}
