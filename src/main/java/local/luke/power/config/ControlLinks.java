package local.luke.power.config;

import java.util.*;

/** One binding-to-feature map drives help, availability and navigation. */
public final class ControlLinks {
  private record Link(String setting, String featureGate, String help) {}
  private static final Map<String, Link> LINKS = new HashMap<>();
  static {
    link("powerbeta.lightOverlay", "lightOverlay.enabled", "", "Toggle light-level numbers on nearby block tops.");
    link("powerbeta.containerPreview", "visual.containerPreview", "visual.containerPreview", "Hold while aiming at a container to preview its contents without opening it.");
    link("key.forward", "", "", "Move forward.");
    link("key.back", "", "", "Move backward.");
    link("key.left", "", "", "Strafe left.");
    link("key.right", "", "", "Strafe right.");
    link("key.jump", "creative.doubleTapTicks", "", "Jump, swim upward or ascend while flying.");
    link("key.sneak", "creative.flight", "", "Sneak at ledges or descend while flying.");
    link("key.drop", "", "", "Drop one item from the selected hotbar slot.");
    link("key.inventory", "", "", "Open or close your inventory.");
    link("key.chat", "", "", "Open chat to write a message or command.");
    link("key.fog", "video.fogCycle", "", "Cycle the configured render distances. Shift reverses the cycle.");
    link("Auto-walk (toggle)", "tweaks.autoWalk", "tweaks.autoWalk", "Start or stop walking forward. Manual forward/back input and focus loss stop it. Inventories keep walking when Inventory while moving is on.");
    // These settings are live toggle states, not feature-availability switches.
    // Keep their toggles and placement modifiers visible while the state is off.
    link("Fast place (toggle)", "tweaks.placement.enabled", "", "Toggle repeated block placement while holding Use.");
    link("Fake sneak (toggle)", "tweaks.sneak.enabled", "", "Toggle ledge protection without slowing movement.");
    link("Slab completion (toggle)", "tweaks.slabs.enabled", "", "Toggle completing matching half slabs from adjacent faces.");
    link("Flexible placement (toggle)", "tweaks.flexible.enabled", "", "Toggle alternate block positions and facing controls.");
    for (String action : List.of("offset", "adjacent", "rotation", "reverse", "into face"))
      link("Placement " + action + " (hold)", "tweaks.flexible.enabled", "", switch (action) {
        case "offset" -> "Hold to move placement outward using the targeted face regions.";
        case "adjacent" -> "Hold to place beside the target, including beyond an edge.";
        case "rotation" -> "Hold to choose block facing from the targeted face.";
        case "reverse" -> "Hold to reverse a directional block's normal placement facing.";
        default -> "Hold to orient a directional block into the targeted face.";
      });
    link("key.powerbeta.free_look", "tweaks.freeLook", "tweaks.freeLook", "Look around independently of your movement direction.");
    link("power_building.hotbar.base", "tweaks.hotbar.swap", "tweaks.hotbar.swap", "Hold to preview inventory rows and switch the active hotbar.");
    link("power_building.hotbar.scroll", "tweaks.hotbar.scroll", "tweaks.hotbar.scroll", "Hold and scroll to cycle inventory rows through the hotbar.");
    for (int i = 1; i <= 3; i++) link("power_building.hotbar.row" + i, "tweaks.hotbar.swap", "tweaks.hotbar.swap", "Swap hotbar contents with inventory row " + i + ".");
    link("power_creative.sprint", "creative.sprintToggle", "creative.sprintFlight", "Boost flight or freecam speed while moving forward. Toggle ends when forward movement stops.");
    link("power_creative.picker", "creative.modePicker", "creative.modePicker", "Cycle game modes while holding the mode-picker modifier; release the modifier to select.");
    link("power_creative.modifier", "creative.modePicker", "creative.modePicker", "Hold with the game-mode cycle key to open the mode picker.");
    link("Toggle Freecam", "power_camera:config.enabled", "power_camera:config.enabled", "Detach the camera from your player. Press again to return.");
    link("Toggle Player Movement", "power_camera:config.enabled", "power_camera:config.enabled", "Allow or stop player movement while the camera is detached.");
    link("Change Freecam Speed", "power_camera:config.speed", "power_camera:config.enabled", "Hold and scroll to adjust detached-camera movement speed.");
    link("Save Or Load Camera Position", "power_camera:config.enabled", "power_camera:config.enabled", "Enter a saved camera position number; hold Ctrl to save the current position instead.");
    link("Open Camera Position Gui", "power_camera:config.enabled", "power_camera:config.enabled", "Manage saved camera positions for the current world.");
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
      {"cinematic_camera", "", "Toggle smoothed mouse movement for camera pans."},
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
    Link l = LINKS.get(key.id);
    return l == null ? null : session.settings().stream().filter(s -> s.id.equals(l.setting)).findFirst().orElse(null);
  }
  public static List<Setting> settings(ConfigSession session, Setting key) {
    Link link = LINKS.get(key.id);
    if (link == null) return List.of();
    return session.settings().stream().filter(s -> relates(link, s))
        .sorted(Comparator.comparingInt(s -> s.id.equals(link.setting) ? 0 : 1)).toList();
  }
  public static boolean hasControls(Setting setting) {
    return LINKS.values().stream().anyMatch(link -> relates(link, setting));
  }
  public static List<Setting> controls(ConfigSession session, Setting setting) {
    return session.settings().stream().filter(s -> {
      Link link = LINKS.get(s.id);
      return link != null && relates(link, setting);
    }).toList();
  }
  private static boolean relates(Link link, Setting setting) {
    if (setting.kind == Setting.Kind.KEY) return false;
    if (setting.id.equals(link.setting) || setting.id.equals(link.featureGate)) return true;
    String family = switch (link.setting) {
      case "lightOverlay.enabled" -> "lightOverlay.";
      case "tweaks.placement.enabled" -> "tweaks.placement.";
      case "tweaks.flexible.enabled" -> "tweaks.flexible.";
      case "tweaks.slabs.enabled" -> "tweaks.slabs.";
      case "tweaks.sneak.enabled" -> "tweaks.sneak.";
      case "tweaks.hotbar.swap", "tweaks.hotbar.scroll" -> "tweaks.hotbar.";
      case "tweaks.freeLook" -> "tweaks.freeLook";
      case "power_camera:config.enabled", "power_camera:config.speed" -> "power_camera:config.";
      case "power_capture:config.customResolutionPhotoWidth" -> "power_capture:config.customResolutionPhoto";
      case "power_capture:config.isometricPhotoScale" -> "power_capture:config.isometricPhoto";
      default -> "";
    };
    if (!family.isEmpty() && setting.id.startsWith(family)) return true;
    return switch (link.setting) {
      case "creative.sprintToggle" -> setting.page.equals("Creative") && setting.group.equals("Flight")
          || setting.id.equals("creative.sprintMultiplier") || setting.id.equals("power_camera:config.sprint");
      case "video.fogCycle" -> setting.id.equals("native.renderDistance") || setting.id.equals("native.fogDensity");
      case "power_controls:userinterface.frontViewThirdPerson" -> setting.id.equals("visual.thirdPersonDistance");
      case "power_capture:config.isometricPhotoScale" -> setting.id.equals("power_capture:config.mirrorIsometricScreenshot")
          || setting.id.equals("power_capture:config.disableRenderingNetherBedrock");
      default -> false;
    };
  }
  public static boolean enabled(ConfigSession session, Setting key) {
    // Flight and freecam share this action; either enabled use keeps it available.
    if (key.id.equals("keys.power_creative.sprint"))
      return featureEnabled(session, "creative.sprintFlight")
          || featureEnabled(session, "power_camera:config.enabled")
              && featureEnabled(session, "power_camera:config.sprint");
    Link l = LINKS.get(key.id);
    return l == null || l.featureGate.isEmpty() || featureEnabled(session, l.featureGate);
  }
  private static boolean featureEnabled(ConfigSession session, String id) {
    return session.settings().stream().filter(s -> s.id.equals(id))
        .allMatch(s -> s.kind != Setting.Kind.BOOLEAN || s.value.getAsBoolean());
  }
}
