package local.luke.power.ui;

import java.util.*;
import local.luke.power.PowerBeta;
import local.luke.power.light.LightSettings;
import local.luke.power.storage.PowerConfig;

/** Menu preferences share the pack configuration, independent of settings drafts. */
public final class MenuPreferences {
  public static final class Values {
    public boolean autoApply, pauseToOptions, blinkingChatCursor;
    public String debugTextColor = "#FFFFFF";
    public String musicBrowserDirectory = "";
    public List<String> colours = new ArrayList<>();

    public void validate() {
      if (colours == null || colours.size() > 16)
        throw new IllegalArgumentException("Save up to 16 colours");
      colours.forEach(LightSettings::rgb);
      LightSettings.rgb(debugTextColor);
      if (musicBrowserDirectory == null || musicBrowserDirectory.length() > 4096 || musicBrowserDirectory.indexOf('\0') >= 0)
        throw new IllegalArgumentException("Invalid music browser location");
    }
  }

  private static Values current = load();
  private static Boolean pausePreview;
  private static Boolean blinkPreview;
  private static String debugColorPreview;

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

  public static boolean blinkingChatCursor() { return blinkPreview == null ? current.blinkingChatCursor : blinkPreview; }
  public static String debugTextColor() { return debugColorPreview == null ? current.debugTextColor : debugColorPreview; }
  public static void previewBlink(boolean enabled) { blinkPreview = enabled; syncPresentation(); }
  public static void previewDebugColor(String color) { LightSettings.rgb(color); debugColorPreview = color; syncPresentation(); }
  public static void syncPresentation() {
    InterfaceState.blinkingChatCursor = blinkingChatCursor();
    InterfaceState.debugTextColor = LightSettings.rgb(debugTextColor());
  }
}
