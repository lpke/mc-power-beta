package local.luke.power.building.hotbar;

import java.util.concurrent.Future;
import local.luke.power.storage.JournalQueue;

/** The shared writer accepts detached bytes only. */
final class JournalWriter {
  private JournalWriter() {}
  static Future<?> append(RecoveryJournal journal, byte[] bytes) {
    return JournalQueue.append(journal.delegate(), bytes);
  }
}
