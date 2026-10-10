package local.luke.power.chat;

/** Beta treats every complete section-sign pair as a colour, including reset codes. */
public final class ChatColours {
  private ChatColours() {}

  public static String continuation(String text, int end) {
    String colour = "";
    for (int i = 0; i + 1 < end; i++) {
      if (text.charAt(i) == '\u00a7') {
        colour = text.substring(i, i + 2);
        i++;
      }
    }
    return colour;
  }
}
