package local.luke.power.chat;

import java.util.*;

/** Original messages remain immutable while their displayed rows reflow. */
public final class ChatTranscript {
  public interface Source { List<Message> messages(); }
  private static Source source = List::of;
  private ChatTranscript() {}
  public static void source(Source value) { source = Objects.requireNonNull(value); ChatSelection.history.clear(); }
  public static List<Message> messages() { return source.messages(); }
  public static final class Message {
    public final String raw, plain;
    private final int[] offsets;
    private final String[] colors;
    public Message(String raw) {
      this.raw = Objects.requireNonNull(raw);
      StringBuilder text = new StringBuilder();
      int[] positions = new int[raw.length() + 1]; String[] styles = new String[raw.length() + 1];
      String color = "";
      for (int i = 0; i < raw.length(); i++) {
        if (raw.charAt(i) == '\u00a7' && i + 1 < raw.length()) { color = raw.substring(i, i + 2); i++; continue; }
        positions[text.length()] = i; styles[text.length()] = color; text.append(raw.charAt(i));
      }
      plain = text.toString(); positions[plain.length()] = raw.length(); styles[plain.length()] = color;
      offsets = Arrays.copyOf(positions, plain.length() + 1); colors = Arrays.copyOf(styles, plain.length() + 1);
    }
    public String slice(int start, int end) { return colors[start] + raw.substring(offsets[start], offsets[end]); }
  }
}
