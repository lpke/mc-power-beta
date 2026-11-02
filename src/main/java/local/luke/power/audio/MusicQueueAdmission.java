package local.luke.power.audio;

import java.util.List;

/** Main-thread queue ownership while the native sound thread opens a requested song. */
final class MusicQueueAdmission {
  enum Result { NONE, WAITING, PLAYING, FAILED }
  private record Request(List<String> queue, int revision, Object previousSource, Object request) {}
  private Request pending, failed;

  boolean blocked(List<String> queue, int revision) {
    refresh(queue, revision);
    return pending != null || failed != null;
  }

  boolean pending(List<String> queue, int revision) {
    refresh(queue, revision);
    return pending != null;
  }

  void begin(List<String> queue, int revision, Object previousSource, Object request) {
    pending = new Request(queue, revision, previousSource, request);
  }

  void reject(List<String> queue, int revision) {
    pending = null;
    failed = new Request(queue, revision, null, "");
  }

  void cancel() { pending = null; }

  Result observe(List<String> queue, int revision, Object source, Object request,
      boolean playing, long elapsed) {
    refresh(queue, revision);
    if (pending == null) return Result.NONE;
    if (source != null && source != pending.previousSource && pending.request == request && playing) {
      pending = null;
      return Result.PLAYING;
    }
    if (elapsed >= 5_000_000_000L) {
      failed = pending;
      pending = null;
      return Result.FAILED;
    }
    return Result.WAITING;
  }

  private void refresh(List<String> queue, int revision) {
    if (pending != null && (pending.queue != queue || pending.revision != revision)) pending = null;
    if (failed != null && (failed.queue != queue || failed.revision != revision)) failed = null;
  }
}
