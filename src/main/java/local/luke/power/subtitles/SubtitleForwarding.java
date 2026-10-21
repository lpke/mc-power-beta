package local.luke.power.subtitles;

import java.util.ArrayDeque;

/** Only the exact synchronous WorldRenderer -> SoundManager invocation consumes a token. */
public final class SubtitleForwarding {
  private static final ThreadLocal<ArrayDeque<Token>> TOKENS = ThreadLocal.withInitial(ArrayDeque::new);
  private SubtitleForwarding() {}
  public static Token push(String id, float x, float y, float z, float volume, float pitch) {
    Token token = new Token(id, x, y, z, volume, pitch); TOKENS.get().push(token); return token;
  }
  public static boolean consume(String id, float x, float y, float z, float volume, float pitch) {
    Token token = TOKENS.get().peek();
    if (token == null || token.consumed || !token.id.equals(id) || token.x != x || token.y != y || token.z != z || token.volume != volume || token.pitch != pitch) return false;
    token.consumed = true; return true;
  }
  public static final class Token implements AutoCloseable {
    private final String id; private final float x, y, z, volume, pitch;
    private boolean consumed, closed;
    private Token(String id, float x, float y, float z, float volume, float pitch) { this.id = id; this.x = x; this.y = y; this.z = z; this.volume = volume; this.pitch = pitch; }
    public void close() {
      if (closed) return; closed = true; var stack = TOKENS.get();
      if (stack.poll() != this) { stack.clear(); throw new IllegalStateException("Subtitle forwarding scopes closed out of order"); }
      if (stack.isEmpty()) TOKENS.remove();
    }
  }
}
