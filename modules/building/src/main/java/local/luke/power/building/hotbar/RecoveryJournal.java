package local.luke.power.building.hotbar;

import java.io.IOException;
import java.nio.file.Path;
import local.luke.power.storage.AppendJournal;

/** Compatibility wrapper. Existing journal bytes and public limits retain their identity. */
public final class RecoveryJournal {
  public static final int MAGIC = AppendJournal.MAGIC, VERSION = AppendJournal.VERSION,
      HEADER = AppendJournal.HEADER, MAX_RECORD = AppendJournal.MAX_RECORD;
  public static final long MAX_FILE = AppendJournal.MAX_FILE;
  private final AppendJournal delegate;
  public RecoveryJournal(Path path) { delegate = new AppendJournal(path); }
  public RecoveryJournal(Path path, long limit) { delegate = new AppendJournal(path, limit); }
  public void append(byte[] payload) throws IOException { delegate.append(payload); }
  AppendJournal delegate() { return delegate; }
}
