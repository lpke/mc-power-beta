package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.concurrent.*;
import local.luke.power.debug.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;

/** Reproduce writer contention without delaying a real player's input. */
final class DebugSave470Checks {
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote, "use a disposable singleplayer world");
    PowerConfig.flushPending();
    var original = PowerConfig.section("power_controls:general");
    Object config = Class.forName("local.luke.power.controls.ControlFeatures").getField("GENERAL_CONFIG").get(null);
    var field = config.getClass().getField("pauseOnLostFocus"); Object before = field.get(config);
    var disk = PowerConfig.class.getDeclaredField("DISK"); disk.setAccessible(true);
    var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
    var worker = Executors.newSingleThreadExecutor(); var watchdog = Executors.newSingleThreadScheduledExecutor();
    try {
      Future<?> blocked = worker.submit(() -> {
        synchronized (disk.get(null)) { entered.countDown(); check(release.await(5, TimeUnit.SECONDS), "writer timed out"); }
        return null;
      });
      check(entered.await(5, TimeUnit.SECONDS), "writer did not acquire disk lock");
      Future<?> timeout = watchdog.schedule(release::countDown, 500, TimeUnit.MILLISECONDS);
      long elapsed;
      try {
        long start = System.nanoTime();
        DebugActions.run(mc, DebugAction.FOCUS);
        local.luke.power.PowerBeta.tick(mc);
        elapsed = System.nanoTime() - start;
        check(release.getCount() == 1, "debug shortcut or following tick waited for the disk writer");
      } finally { timeout.cancel(false); release.countDown(); }
      blocked.get(5, TimeUnit.SECONDS); PowerConfig.flushPending();
      var expected = original.deepCopy(); expected.addProperty("pauseOnLostFocus", !Boolean.TRUE.equals(before));
      check(expected.equals(PowerConfig.section("power_controls:general")), "field edit changed unrelated preferences");
      log("PASS DEBUG470 native debug shortcut and following tick returned with writer blocked; ms=" + elapsed / 1_000_000d);
    } finally {
      release.countDown(); worker.shutdownNow(); watchdog.shutdownNow(); PowerConfig.flushPending();
      field.set(config, before); PowerConfig.put("power_controls:general", original);
    }
  }
}
