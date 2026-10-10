package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import local.luke.power.building.config.Config;
import local.luke.power.fastplace.FastPlace;
import local.luke.power.fakesneak.FakeSneak;
import local.luke.power.status.*;
import net.minecraft.client.Minecraft;

final class TogglePerformanceChecks {
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null, "load a disposable world first");
    mc.setScreen(null); mc.paused = false; mc.field_2778 = true;
    var before = Config.current().copy();
    var display = StatusConfig.current();
    try {
      check(FastPlace.canOperate(mc), "gameplay toggle unavailable");
      for (boolean hud : new boolean[]{true, false}) {
        var settings = new StatusSettings(); settings.enabled = hud; StatusConfig.preview(settings);
        measure(mc, "fast placement HUD=" + hud, down -> FastPlace.beginTick(mc, false, down, true));
        measure(mc, "edge protection HUD=" + hud, down -> FakeSneak.beginTick(mc, down, true));
      }
    } finally {
      FastPlace.beginTick(mc, false, false, true); FakeSneak.beginTick(mc, false, true);
      Config.apply(before); StatusConfig.preview(display);
    }
  }

  private static void measure(Minecraft mc, String label, java.util.function.Consumer<Boolean> toggle) throws Exception {
    long[] wall = new long[24], cpu = new long[24], render = new long[24];
    var threads = ManagementFactory.getThreadMXBean();
    for (int i = 0; i < wall.length; i++) {
      toggle.accept(false);
      long start = System.nanoTime(), cpuStart = threads.getCurrentThreadCpuTime();
      toggle.accept(true);
      cpu[i] = threads.getCurrentThreadCpuTime() - cpuStart;
      wall[i] = System.nanoTime() - start;
      start = System.nanoTime(); ActiveTweaks.render(mc); render[i] = System.nanoTime() - start;
    }
    Arrays.sort(wall); Arrays.sort(cpu); Arrays.sort(render);
    log("TIMING " + label + " toggle median/max ms=" + ms(wall[12]) + "/" + ms(wall[23])
        + " CPU median ms=" + ms(cpu[12]) + " HUD median/max ms=" + ms(render[12]) + "/" + ms(render[23]));
  }

  private static String ms(long nanos) { return String.format(java.util.Locale.ROOT, "%.3f", nanos / 1_000_000.0); }
}
