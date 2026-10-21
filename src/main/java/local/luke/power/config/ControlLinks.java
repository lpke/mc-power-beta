package local.luke.power.config;

import java.util.*;

/** Exact counterparts drive navigation; feature gates only control availability. */
public final class ControlLinks {
  private record Link(String setting, String featureGate, String help) {}
  private static final Map<String, Link> LINKS = new LinkedHashMap<>();
  static {
    link("power_beta.entities", "visual.hideEntities", "", "Toggle rendering for the selected entity types.");
    link("power_beta.fullBright", "", "", "Toggle full bright. Changes rendering only.");
    link("powerbeta.lightOverlay", "lightOverlay.enabled", "", "Toggle light-level numbers on nearby block tops.");
    link("powerbeta.containerPreview", "", "", "Hold while aiming at a container to preview its contents without opening it.");
    link("key.forward", "", "", "Move forward.");
    link("key.back", "", "", "Move backward.");
    link("key.left", "", "", "Strafe left.");
    link("key.right", "", "", "Strafe right.");
    link("key.jump", "", "", "Jump, swim upward or ascend while flying.");
    link("key.sneak", "", "", "Sneak at ledges or descend while flying.");
    link("key.drop", "", "", "Drop one item from the selected hotbar slot.");
    link("powerbeta.recipeBook", "recipeBook.enabled", "", "Open the recipe book or toggle it in an eligible inventory. Search fields and modal windows keep keyboard ownership.");
    link("powerbeta.sortingLayout", "inventoryTools.preferredItems", "", "Edit your sorting layout over the open inventory. Keeps the inventory open and changes settings only.");
    link("powerbeta.sortInventory", "inventoryTools.sortingButton", "", "Sort player storage when pointing over your inventory; otherwise sort the open chest or dispenser. Excludes hotbar, equipment and crafting slots.");
    link("powerbeta.depositMatching", "inventoryTools.depositMatchingButton", "", "Deposit matching main-storage items into the supported open chest or dispenser.");
    link("powerbeta.saveCreativeHotbar", "inventoryTools.creativeHotbars", "", "Hold with a hotbar number to save one of nine Creative hotbars. Saved presets stay outside defaults resets.");
    link("powerbeta.loadCreativeHotbar", "inventoryTools.creativeHotbars", "", "Hold with a hotbar number to load one of nine Creative hotbars. Requires singleplayer Creative access.");
    link("powerbeta.toolSwitch", "inventoryTools.toolSwitch", "", "Toggle automatic tool switching in singleplayer Survival.");
    link("powerbeta.restock", "inventoryTools.restock", "", "Toggle automatic item refilling in singleplayer Survival.");
    link("powerbeta.swapHands", "offhand.enabled", "", "Swap the selected hotbar item with the offhand in singleplayer. In your inventory, swap the hovered writable slot instead.");
    link("key.inventory", "", "", "Open or close your inventory.");
    link("key.chat", "", "", "Open chat to write a message or command.");
    link("powerbeta.openCommands", "", "", "Open chat with / ready to enter a command.");
    link("powerbeta.debugGraph", "", "", "Toggle the performance graph and show the debug overlay.");
    link("key.fog", "video.fogCycle", "", "Cycle the configured render distances. Shift reverses the cycle.");
    link("Auto-walk (toggle)", "tweaks.autoWalkHoldToWalk", "", "Start or stop walking forward. Player forward/back input stops it. Focus loss stops it only when configured. Freecam controls the camera while your player keeps walking. Inventories keep walking when Inventory while moving is on.");
    link("powerbeta.map", "maps.worldMap", "maps.worldMap", "Open the world map. Drag to pan, scroll to zoom, and right-click to manage a waypoint.");
    link("powerbeta.waypoint", "", "", "Create a waypoint at the reported camera position.");
    link("powerbeta.waypoints", "", "", "Search and manage waypoint sets.");
    link("powerbeta.enlargeMap", "", "maps.minimap", "Hold to enlarge the minimap.");
    link("powerbeta.radarIcons", "maps.radar", "", "Hold to replace nearby mob radar dots with upright model-head icons on either map. Dots return on release. Shares Tab with the player list by default.");
    link("powerbeta.mapZoomIn", "maps.zoom", "maps.minimap", "Zoom the minimap in.");
    link("powerbeta.mapZoomOut", "maps.zoom", "maps.minimap", "Zoom the minimap out.");
    link("powerbeta.minimap", "maps.minimap", "", "Show or hide the minimap.");
    link("powerbeta.toggleWorldWaypoints", "maps.worldWaypoints", "", "Show or hide waypoint markers in the world.");
    link("powerbeta.toggleMapWaypoints", "maps.waypoints", "", "Show or hide waypoint markers on maps.");
    link("powerbeta.toggleMapRadar", "maps.radar", "", "Show or hide entities on maps.");
    link("powerbeta.toggleMapCaves", "maps.caveMode", "", "Cycle surface, automatic cave and manual cave mapping.");
    link("powerbeta.toggleMapGrid", "maps.chunkGrid", "", "Show or hide chunk borders on maps.");
    link("powerbeta.clearMapDestination", "", "", "Clear the current map destination.");
    link("powerbeta.mapSettings", "", "", "Open Map settings.");
    link("powerbeta.mapZoomReset", "maps.zoom", "maps.minimap", "Reset the minimap zoom.");
    link("powerbeta.sprint", "sprint.activation", "rules.world.allow_sprinting", "Sprint forward when Allow sprinting is enabled in this world's Game rules, using the global activation and stamina preferences.");
    link("powerbeta.infoHud", "infoHud.enabled", "", "Toggle all custom HUD clusters together. Each cluster keeps its contents, appearance and F3 visibility preference.");
    // These settings are live toggle states, not feature-availability switches.
    link("power_beta.clickMining", "tweaks.clickMining", "", "Temporarily invert Continuous click mining until the next press or world exit.");
    link("Auto-mine (toggle)", "", "", "Toggle holding Attack / Break. Menus, focus loss, world changes and detached-camera control stop it.");
    // Keep their toggles and placement modifiers visible while the state is off.
    link("Fast place (toggle)", "tweaks.placement.enabled", "", "Toggle repeated block placement while holding Use.");
    link("Fake sneak (toggle)", "tweaks.sneak.enabled", "", "Toggle fake sneak to stop at ledges without slowing movement.");
    for (String action : List.of("offset", "adjacent", "rotation", "reverse", "into face"))
      link("Placement " + action + " (hold)", "", "", switch (action) {
        case "offset" -> "Hold to move placement outward using the targeted face regions.";
        case "adjacent" -> "Hold to place beside the target, including beyond an edge.";
        case "rotation" -> "Hold to choose block facing from the targeted face.";
        case "reverse" -> "Hold to reverse a directional block's normal placement facing.";
        default -> "Hold to orient a directional block into the targeted face.";
      });
    link("key.powerbeta.free_look", "tweaks.freeLookToggle", "", "Look around independently of your movement direction.");
    link("power_building.hotbar.base", "tweaks.hotbar.overlay", "", "Hold to preview inventory rows and switch the active hotbar.");
    link("power_building.hotbar.scroll", "tweaks.hotbar.reverseScroll", "", "Hold and scroll to cycle inventory rows through the hotbar.");
    for (int i = 1; i <= 3; i++) link("power_building.hotbar.row" + i, "", "", "Swap hotbar contents with inventory row " + i + ".");
    link("power_creative.sprint", "creative.sprintToggle", "", "Boost creative, spectator or freecam flight in any direction. Toggle ends when movement stops.");
    link("power_creative.picker", "", "", "Cycle game modes while holding the mode-picker modifier; release the modifier to select.");
    link("power_creative.modifier", "", "", "Hold with the game-mode cycle key to open the mode picker.");
    link("Toggle Freecam", "", "", "Detach the camera. The HUD follows Hide HUD on entering freecam; F1 can change it during the visit. Press again to return and restore the previous HUD visibility.");
    link("Toggle Player Movement", "", "", "Allow or stop player movement while the camera is detached.");
    link("Change Freecam Speed", "power_camera:config.speed", "", "Hold and scroll to adjust detached-camera movement speed.");
    link("Save Or Load Camera Position", "", "", "Enter a saved camera position number; hold Ctrl to save the current position instead.");
    link("Open Camera Position Gui", "", "", "Manage saved camera positions for the current world.");
    link("Custom Photo", "power_capture:config.customResolutionPhotoWidth", "", "Capture the view at the configured screenshot resolution.");
    link("Isometric Photo", "power_capture:config.isometricPhotoScale", "", "Capture an isometric view with the configured size and rotation.");
    link("playerList", "", "", "Hold to display the multiplayer player list.");
    String[][] actions = {
      {"dismount", "", "Leave the boat, minecart or other vehicle you are riding."},
      {"zoom", "", "Hold to zoom the camera; scroll to adjust magnification."},
      {"photo_mode", "", "Open photo controls for camera position, framing and capture."},
      {"hide_hud", "", "Toggle the crosshair, hotbar and other HUD elements."},
      {"take_screenshot", "", "Save a screenshot of the current view."},
      {"debug_hud", "", "Toggle the debug overlay with coordinates and performance information."},
      {"third_person", "power_controls:userinterface.frontViewThirdPerson", "Cycle between first-person and configured third-person views."},
      {"cinematic_camera", "", "Toggle smoothed mouse movement for camera pans."},
      {"toggle_fullscreen", "", "Switch between windowed and fullscreen display."},
      {"release_mouse", "", "Release the mouse pointer from the game window."},
      {"panorama_screenshot", "", "Capture the six faces of a panorama at your current position."},
      {"rescan", "", "Rescan connected pointing devices for raw mouse input."},
      {"toggle_raw_input", "power_controls:general.rawInput", "Switch between raw-device and normal mouse input."}
    };
    for (String[] a : actions) link("key.power_controls." + a[0], a[1], "", a[2]);
    for (int i = 1; i <= 9; i++) link("key.power_controls.hotbar_" + i, "", "", "Select hotbar slot " + i + ".");
  }
  private static void link(String key, String setting, String featureGate, String help) { LINKS.put("keys." + key, new Link(setting, featureGate, help)); }
  public static String description(String id) { Link l = LINKS.get(id); return l == null ? "Activate this action using the assigned key or mouse button." : l.help; }
  public static Setting related(ConfigSession session, Setting key) {
    Link link = LINKS.get(key.id);
    if (link == null || link.setting.isEmpty()) return null;
    return session.settings().stream().filter(s -> relates(link, s) && SettingAccess.visible(session, s))
        .findFirst().orElse(null);
  }
  public static List<Setting> settings(ConfigSession session, Setting key) {
    Setting target = related(session, key);
    return target == null ? List.of() : List.of(target);
  }
  public static boolean hasControls(Setting setting) {
    return LINKS.values().stream().anyMatch(link -> relates(link, setting));
  }
  /** Prefer registration order when two actions share a setting, such as zoom in/out. */
  public static List<Setting> controls(ConfigSession session, Setting setting) {
    for (var entry : LINKS.entrySet()) {
      if (!relates(entry.getValue(), setting)) continue;
      Setting target = session.settings().stream()
          .filter(s -> s.id.equals(entry.getKey()) && SettingAccess.visible(session, s))
          .findFirst().orElse(null);
      if (target != null) return List.of(target);
    }
    return List.of();
  }
  private static boolean relates(Link link, Setting setting) {
    return setting.kind != Setting.Kind.KEY && !link.setting.isEmpty() && setting.id.equals(link.setting);
  }
  public static boolean enabled(ConfigSession session, Setting key) {
    Link l = LINKS.get(key.id);
    return l == null || l.featureGate.isEmpty() || featureEnabled(session, l.featureGate);
  }
  private static boolean featureEnabled(ConfigSession session, String id) {
    return session.settings().stream().filter(s -> s.id.equals(id))
        .allMatch(s -> s.kind != Setting.Kind.BOOLEAN || s.value.getAsBoolean());
  }
}
