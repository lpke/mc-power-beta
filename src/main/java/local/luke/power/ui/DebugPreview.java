package local.luke.power.ui;

import java.util.*;
import local.luke.power.light.LightSettings;
import net.minecraft.client.Minecraft;

/** Stable provider samples use the live F3 layout and rendering path. */
final class DebugPreview {
  private final SettingsDraft draft;
  DebugPreview(SettingsDraft draft) { this.draft = draft; }
  List<String> order() { return DebugOrder.read(draft.get("interface.debugOrder").value); }
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
    String[] memory = {"Used memory: 35% (350MB) of 1000MB", "Allocated memory: 50% (500MB)"};
    for (int row = 0; row < memory.length; row++) source.add(new DebugOverlay.Line(memory[row], width - mc.textRenderer.getWidth(memory[row]) - 2, 2 + row * 10, mc.textRenderer.getWidth(memory[row]), ""));
    double size = draft.get("interface.debugTextSize").value.getAsDouble();
    var placed = DebugOverlay.arrangeOrdered(source, width, mc.textRenderer::getWidth, order(), this::enabled, size);
    DebugTextRenderer.draw(mc, placed, size, LightSettings.rgb(draft.get("interface.debugTextColor").value.getAsString()));
    return placed;
  }
}
