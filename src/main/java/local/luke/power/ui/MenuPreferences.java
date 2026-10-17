package local.luke.power.ui;

import java.util.*;
import local.luke.power.PowerBeta;
import local.luke.power.light.LightSettings;
import local.luke.power.storage.PowerConfig;

/** Menu preferences share the pack configuration, independent of settings drafts. */
public final class MenuPreferences {
  public static final class Values {
    public boolean autoApply = true;
    public boolean pauseToOptions, blinkingChatCursor = true;
    public boolean blinkingUiCursor = true;
    public boolean hidePowerBetaVersion;
    public boolean debugTimeOfDay, useFeetYCoordinates = true;
    public String debugTextColor = "#FFFFFF";
    public double debugTextSize = 1;
    public List<String> debugOrder = DebugOrder.defaults();
    public Map<String, Boolean> debugLines = new LinkedHashMap<>();
    public String musicBrowserDirectory = "";
    public List<String> colours = new ArrayList<>();

    public void validate() {
      if (colours == null || colours.size() > 16)
        throw new IllegalArgumentException("Save up to 16 colours");
      colours.forEach(LightSettings::rgb);
      LightSettings.rgb(debugTextColor);
      DebugOrder.validate(debugOrder);
      if (!Double.isFinite(debugTextSize) || debugTextSize < .5 || debugTextSize > 3) throw new IllegalArgumentException("F3 text size must be between 0.5 and 3.");
      if (debugLines == null || debugLines.size() > 256 || debugLines.entrySet().stream().anyMatch(e -> e.getKey() == null
          || !e.getKey().matches("[A-Za-z0-9_.-]{1,96}") || e.getValue() == null)) throw new IllegalArgumentException("Invalid F3 line visibility.");
      if (musicBrowserDirectory == null || musicBrowserDirectory.length() > 4096 || musicBrowserDirectory.indexOf('\0') >= 0)
        throw new IllegalArgumentException("Invalid music browser location");
    }
  }

  private static Values current = load();
  private static Boolean pausePreview;
  private static Boolean blinkPreview, uiBlinkPreview;
  private static String debugColorPreview;
  private static Boolean debugTimePreview;
  private static Boolean feetYPreview;
  private static Boolean powerVersionPreview;
  private static Double debugSizePreview;
  private static List<String> debugOrderPreview;
  private static final Map<String, Boolean> debugLinePreviews = new HashMap<>();

  private static Values load() {
    try {
      Values v = PowerConfig.read("interface", Values.class);
      v.validate();
      return v;
    } catch (Exception e) {
      PowerBeta.LOG.error("Could not read interface preferences", e);
      return new Values();
    }
  }

  public static Values current() {
    return current;
  }

  public static void update(java.util.function.Consumer<Values> edit) throws java.io.IOException {
    Values v =
        local.luke.power.config.Catalog.JSON.fromJson(
            local.luke.power.config.Catalog.JSON.toJson(current), Values.class);
    edit.accept(v);
    v.validate();
    PowerConfig.save("interface", v);
    current = v;
    syncPresentation();
  }

  public static boolean pauseToOptions() {
    return pausePreview == null ? current.pauseToOptions : pausePreview;
  }

  public static void previewPause(boolean enabled) {
    pausePreview = enabled;
  }

  public static boolean blinkingUiCursor() { return uiBlinkPreview == null ? current.blinkingUiCursor : uiBlinkPreview; }
  public static void previewUiBlink(boolean enabled) { uiBlinkPreview = enabled; syncPresentation(); }
  public static boolean blinkingChatCursor() { return blinkPreview == null ? current.blinkingChatCursor : blinkPreview; }
  public static boolean debugTimeOfDay() { return debugTimePreview == null ? current.debugTimeOfDay : debugTimePreview; }
  public static void previewDebugTime(boolean enabled) { debugTimePreview = enabled; }
  public static boolean useFeetYCoordinates() { return feetYPreview == null ? current.useFeetYCoordinates : feetYPreview; }
  public static boolean hidePowerBetaVersion() { return powerVersionPreview == null ? current.hidePowerBetaVersion : powerVersionPreview; }
  public static void previewPowerVersion(boolean hidden) { powerVersionPreview = hidden; }
  public static void previewFeetY(boolean enabled) { feetYPreview = enabled; }
  public static String debugTextColor() { return debugColorPreview == null ? current.debugTextColor : debugColorPreview; }
  public static double debugTextSize() { return debugSizePreview == null ? current.debugTextSize : debugSizePreview; }
  public static List<String> debugOrder() { return debugOrderPreview == null ? current.debugOrder : debugOrderPreview; }
  public static boolean debugLine(String id) { return debugLinePreviews.getOrDefault(id, current.debugLines.getOrDefault(id, true)); }
  public static void previewDebugSize(double size) { debugSizePreview = size; }
  public static void previewDebugOrder(List<String> order) { debugOrderPreview = DebugOrder.validate(order); }
  public static void previewDebugLine(String id, boolean enabled) { debugLinePreviews.put(id, enabled); }
  public static void previewBlink(boolean enabled) { blinkPreview = enabled; syncPresentation(); }
  public static void previewDebugColor(String color) { LightSettings.rgb(color); debugColorPreview = color; syncPresentation(); }
  public static void syncPresentation() {
    InterfaceState.blinkingChatCursor = blinkingChatCursor();
    InterfaceState.blinkingUiCursor = blinkingUiCursor();
    InterfaceState.debugTextColor = LightSettings.rgb(debugTextColor());
  }
}
