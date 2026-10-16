package local.luke.power.config;

import java.util.*;

/** One binding-to-feature map drives help, availability and navigation. */
public final class ControlLinks {
  private record Link(String setting, String featureGate, String help) {}
  private static final Map<String, Link> LINKS = new HashMap<>();
  static {
    link("power_beta.entities", "visual.hideEntities", "", "Toggle rendering for the selected entity types.");
    link("power_beta.fullBright", "visual.fullBrightMessages", "", "Toggle full bright. Changes rendering only.");
    link("powerbeta.lightOverlay", "lightOverlay.enabled", "", "Toggle light-level numbers on nearby block tops.");
    link("powerbeta.containerPreview", "creative.blockReach", "", "Hold while aiming at a container to preview its contents without opening it.");
    link("key.forward", "", "", "Move forward.");
    link("key.back", "", "", "Move backward.");
    link("key.left", "", "", "Strafe left.");
    link("key.right", "", "", "Strafe right.");
    link("key.jump", "creative.doubleTapTicks", "", "Jump, swim upward or ascend while flying.");
    link("key.sneak", "creative.flight", "", "Sneak at ledges or descend while flying.");
    link("key.drop", "", "", "Drop one item from the selected hotbar slot.");
    link("key.inventory", "", "", "Open or close your inventory.");
    link("key.chat", "", "", "Open chat to write a message or command.");
    link("powerbeta.openCommands", "", "", "Open chat with / ready to enter a command.");
    link("powerbeta.debugGraph", "", "", "Toggle the performance graph and show the debug overlay.");
    link("key.fog", "video.fogCycle", "", "Cycle the configured render distances. Shift reverses the cycle.");
    link("Auto-walk (toggle)", "tweaks.autoWalkHoldToWalk", "", "Start or stop walking forward. Player forward/back input stops it. Focus loss stops it only when configured. Freecam controls the camera while your player keeps walking. Inventories keep walking when Inventory while moving is on.");
    link("powerbeta.infoHud", "infoHud.enabled", "", "Toggle all custom HUD clusters together. Each cluster keeps its contents, appearance and F3 visibility preference.");
    // These settings are live toggle states, not feature-availability switches.
    link("power_beta.clickMining", "tweaks.clickMining", "", "Temporarily invert Continuous click mining until the next press or world exit.");
    link("Auto-mine (toggle)", "tweaks.autoMineAnnounceToggle", "", "Toggle holding Attack / Break. Menus, focus loss, world changes and detached-camera control stop it.");
    // Keep their toggles and placement modifiers visible while the state is off.
    link("Fast place (toggle)", "tweaks.placement.enabled", "", "Toggle repeated block placement while holding Use.");
    link("Fake sneak (toggle)", "tweaks.sneak.enabled", "", "Toggle fake sneak to stop at ledges without slowing movement.");
    for (String action : List.of("offset", "adjacent", "rotation", "reverse", "into face"))
      link("Placement " + action + " (hold)", "tweaks.flexible.showOverlay", "", switch (action) {
        case "offset" -> "Hold to move placement outward using the targeted face regions.";
        case "adjacent" -> "Hold to place beside the target, including beyond an edge.";
        case "rotation" -> "Hold to choose block facing from the targeted face.";
        case "reverse" -> "Hold to reverse a directional block's normal placement facing.";
        default -> "Hold to orient a directional block into the targeted face.";
      });
    link("key.powerbeta.free_look", "tweaks.freeLookToggle", "", "Look around independently of your movement direction.");
    link("power_building.hotbar.base", "tweaks.hotbar.overlay", "", "Hold to preview inventory rows and switch the active hotbar.");
    link("power_building.hotbar.scroll", "tweaks.hotbar.reverseScroll", "", "Hold and scroll to cycle inventory rows through the hotbar.");
    for (int i = 1; i <= 3; i++) link("power_building.hotbar.row" + i, "tweaks.hotbar.overlay", "", "Swap hotbar contents with inventory row " + i + ".");
    link("power_creative.sprint", "creative.sprintToggle", "", "Boost creative, spectator or freecam flight in any direction. Toggle ends when movement stops.");
    link("power_creative.picker", "", "", "Cycle game modes while holding the mode-picker modifier; release the modifier to select.");
    link("power_creative.modifier", "", "", "Hold with the game-mode cycle key to open the mode picker.");
    link("Toggle Freecam", "power_camera:config.speed", "", "Detach the camera. The HUD follows Hide HUD on entering freecam; F1 can change it during the visit. Press again to return and restore the previous HUD visibility.");
    link("Toggle Player Movement", "power_camera:config.speed", "", "Allow or stop player movement while the camera is detached.");
    link("Change Freecam Speed", "power_camera:config.speed", "", "Hold and scroll to adjust detached-camera movement speed.");
    link("Save Or Load Camera Position", "power_camera:config.speed", "", "Enter a saved camera position number; hold Ctrl to save the current position instead.");
    link("Open Camera Position Gui", "power_camera:config.speed", "", "Manage saved camera positions for the current world.");
    link("Custom Photo", "power_capture:config.customResolutionPhotoWidth", "", "Capture the view at the configured screenshot resolution.");
    link("Isometric Photo", "power_capture:config.isometricPhotoScale", "", "Capture an isometric view with the configured size and rotation.");
    link("playerList", "", "", "Hold to display the multiplayer player list.");
    String[][] actions = {
      {"dismount", "", "Leave the boat, minecart or other vehicle you are riding."},
      {"zoom", "native.fov", "Hold to zoom the camera; scroll to adjust magnification."},
      {"photo_mode", "power_controls:userinterface.photoModeConfig.enablePhotoModeButton", "Open photo controls for camera position, framing and capture."},
      {"hide_hud", "", "Toggle the crosshair, hotbar and other HUD elements."},
      {"take_screenshot", "power_capture:config.addBasicScreenshotsToClipboard", "Save a screenshot of the current view."},
      {"debug_hud", "", "Toggle the debug overlay with coordinates and performance information."},
      {"third_person", "power_controls:userinterface.frontViewThirdPerson", "Cycle between first-person and configured third-person views."},
      {"cinematic_camera", "activeTweaks.cinematicCameraMessages", "Toggle smoothed mouse movement for camera pans."},
      {"toggle_fullscreen", "", "Switch between windowed and fullscreen display."},
      {"release_mouse", "", "Release the mouse pointer from the game window."},
      {"panorama_screenshot", "", "Capture the six faces of a panorama at your current position."},
      {"rescan", "power_controls:general.rawInput", "Rescan connected pointing devices for raw mouse input."},
      {"toggle_raw_input", "power_controls:general.rawInput", "Switch between raw-device and normal mouse input."}
    };
    for (String[] a : actions) link("key.power_controls." + a[0], a[1], "", a[2]);
    for (int i = 1; i <= 9; i++) link("key.power_controls.hotbar_" + i, "", "", "Select hotbar slot " + i + ".");
  }
  private static void link(String key, String setting, String featureGate, String help) { LINKS.put("keys." + key, new Link(setting, featureGate, help)); }
  public static String description(String id) { Link l = LINKS.get(id); return l == null ? "Activate this action using the assigned key or mouse button." : l.help; }
  public static Setting related(ConfigSession session, Setting key) {
    return settings(session, key).stream().findFirst().orElse(null);
  }
  public static List<Setting> settings(ConfigSession session, Setting key) {
    Link link = LINKS.get(key.id);
    if (link == null) return List.of();
    return session.settings().stream().filter(s -> relates(link, s) && SettingAccess.visible(session, s))
        .sorted(Comparator.comparingInt(s -> s.id.equals(link.setting) ? 0 : 1)).toList();
  }
  public static boolean hasControls(Setting setting) {
    return LINKS.values().stream().anyMatch(link -> relates(link, setting));
  }
  public static List<Setting> controls(ConfigSession session, Setting setting) {
    return session.settings().stream().filter(s -> {
      Link link = LINKS.get(s.id);
      return link != null && relates(link, setting) && SettingAccess.visible(session, s);
    }).toList();
  }
  private static boolean relates(Link link, Setting setting) {
    if (setting.kind == Setting.Kind.KEY) return false;
    if (setting.id.equals(link.setting) || setting.id.equals(link.featureGate)) return true;
    String family = switch (link.setting) {
      case "lightOverlay.enabled" -> "lightOverlay.";
      case "infoHud.enabled" -> "infoHud.";
      case "tweaks.placement.enabled" -> "tweaks.placement.";
      case "tweaks.flexible.showOverlay" -> "tweaks.flexible.";
      case "tweaks.sneak.enabled" -> "tweaks.sneak.";
      case "tweaks.hotbar.overlay", "tweaks.hotbar.reverseScroll" -> "tweaks.hotbar.";
      case "tweaks.freeLookToggle" -> "tweaks.freeLook";
      case "tweaks.autoWalkHoldToWalk" -> "tweaks.autoWalk";
      case "tweaks.autoMineAnnounceToggle" -> "tweaks.autoMine";
      case "power_camera:config.speed" -> "power_camera:config.";
      case "power_capture:config.customResolutionPhotoWidth" -> "power_capture:config.customResolutionPhoto";
      case "power_capture:config.isometricPhotoScale" -> "power_capture:config.isometricPhoto";
      default -> "";
    };
    if (!family.isEmpty() && setting.id.startsWith(family)) return true;
    return switch (link.setting) {
      case "creative.blockReach" -> setting.id.equals("creative.entityReach");
      case "visual.hideEntities" -> setting.group.equals("Entity visibility");
      case "visual.fullBrightMessages" -> setting.id.equals("visual.spectatorFullBright");
      case "creative.sprintToggle" -> setting.page.equals("Creative") && Set.of("Flight", "Spectator").contains(setting.group)
          || setting.id.startsWith("power_camera:config.sprint");
      case "video.fogCycle" -> setting.id.equals("native.renderDistance") || setting.id.equals("native.fogDensity");
      case "power_controls:userinterface.frontViewThirdPerson" -> setting.id.equals("visual.thirdPersonDistance");
      case "power_capture:config.isometricPhotoScale" -> setting.id.equals("power_capture:config.mirrorIsometricScreenshot")
          || setting.id.equals("power_capture:config.disableRenderingNetherBedrock");
      default -> false;
    };
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
