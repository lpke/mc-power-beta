package local.luke.power.storage;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

public final class PowerConfigShutdownProbe {
  public static void main(String[] args) throws Exception {
    PowerConfig.configure(Path.of(args[0]));
    var disk = PowerConfig.class.getDeclaredField("DISK"); disk.setAccessible(true);
    var entered = new CountDownLatch(1);
    Thread blocker = new Thread(() -> {
      try {
        synchronized (disk.get(null)) { entered.countDown(); Thread.sleep(300); }
      } catch (Exception e) { throw new RuntimeException(e); }
    });
    blocker.setDaemon(true); blocker.start(); entered.await();
    JsonObject value = new JsonObject(); value.addProperty("enabled", true);
    PowerConfig.saveDeferred("shutdown", value);
    System.exit(0); // The daemon writer is still blocked; the shutdown hook must wait for it.
  }
}
