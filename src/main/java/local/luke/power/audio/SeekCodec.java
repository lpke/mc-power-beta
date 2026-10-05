package local.luke.power.audio;

import java.net.URL;
import javax.sound.sampled.AudioFormat;
import paulscode.sound.*;

/** Hands an already-positioned decoder to SoundSystem exactly once. */
public final class SeekCodec implements ICodec {
  private ICodec delegate;
  private String resource;
  private SoundBuffer first;

  public SeekCodec() {}

  public void reverseByteOrder(boolean reverse) {
    if (delegate != null) delegate.reverseByteOrder(reverse);
  }

  public boolean initialize(URL url) {
    // OpenAL calls initialize to inspect the format, then again in preLoad.
    // The handoff must survive both calls without losing the positioned decoder.
    if (delegate != null && url.toExternalForm().equals(resource)) return initialized();
    cleanup();
    MusicSeeking.Prepared prepared = MusicSeeking.claim(url);
    if (prepared == null) return false;
    delegate = prepared.codec();
    resource = url.toExternalForm();
    first = prepared.first();
    return true;
  }

  public boolean initialized() {
    return delegate != null && delegate.initialized();
  }

  public SoundBuffer read() {
    if (first != null) {
      SoundBuffer next = first;
      first = null;
      return next;
    }
    return delegate == null ? null : delegate.read();
  }

  public SoundBuffer readAll() {
    throw new UnsupportedOperationException("Seek streams must remain streamed");
  }

  public boolean endOfStream() {
    return first == null && (delegate == null || delegate.endOfStream());
  }

  public AudioFormat getAudioFormat() {
    return delegate == null ? null : delegate.getAudioFormat();
  }

  public void cleanup() {
    if (delegate != null) delegate.cleanup();
    delegate = null;
    resource = null;
    first = null;
  }
}
