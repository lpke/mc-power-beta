package local.luke.power.building.hotbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Receives detached bytes only; inventory access and publication stay on the game thread. */
final class JournalWriter {
  private static final ExecutorService IO = Executors.newSingleThreadExecutor(task -> {
    Thread thread = new Thread(task, "Power Beta hotbar journal");
    thread.setDaemon(true);
    return thread;
  });

  private JournalWriter() {}

  static Future<?> append(RecoveryJournal journal, byte[] bytes) {
    return IO.submit(() -> {
      journal.append(bytes);
      return null;
    });
  }
}
