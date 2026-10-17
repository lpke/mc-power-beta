package local.luke.power.ui;

import java.util.*;
import local.luke.power.light.LightSettings;
import net.minecraft.client.Minecraft;

/** Stable provider samples use the live F3 layout and rendering path. */
final class DebugPreview {
  private final SettingsDraft draft;
  DebugPreview(SettingsDraft draft) { this.draft = draft; }
  List<String> order() { return DebugOrder.read(draft.get("interface.debugOrder").value); }
  List<String> rightOrder() { return DebugOrder.read(draft.get("interface.debugRightOrder").value); }
  boolean enabled(String id) {
    DebugLines line = DebugLines.find(id);
    if (line == null) return false;
    if (line.culling() && !draft.get("entityculling:config.showF3Info").value.getAsBoolean()) return false;
    if ((line == DebugLines.X || line == DebugLines.Y || line == DebugLines.Z)
        && draft.get("power_hud:config.coordinateDisplay").value.getAsInt() == 1) return false;
    return draft.get(line.setting()).value.getAsBoolean();
  }
  String text(String id) {
    DebugLines line = DebugLines.find(id);
    if (line == null) return "";
    if (line == DebugLines.Y && !draft.get("interface.useFeetYCoordinates").value.getAsBoolean()) return "y: 65.62";
    return line.sample;
  }
  List<DebugOverlay.Line> draw(Minecraft mc, int width) {
    List<DebugOverlay.Line> source = new ArrayList<>();
    for (var line : DebugLines.values()) if (enabled(line.id)) source.add(new DebugOverlay.Line(text(line.id), 2, 2, 0, line.id));
    double size = draft.get("interface.debugTextSize").value.getAsDouble();
    var placed = DebugOverlay.arrangeOrdered(source, width, mc.textRenderer::getWidth, order(), rightOrder(), this::enabled, size);
    DebugTextRenderer.draw(mc, placed, size, LightSettings.rgb(draft.get("interface.debugTextColor").value.getAsString()));
    return placed;
  }
}
