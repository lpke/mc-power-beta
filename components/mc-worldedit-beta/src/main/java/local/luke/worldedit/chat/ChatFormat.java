package local.luke.worldedit.chat;

/** Colour only editor output. Player chat and third-party messages pass through untouched. */
public final class ChatFormat {
  private ChatFormat() {}
  public static String info(String text) {
    String colour = text.startsWith("/") || text.startsWith("WorldEdit Beta help") ? "\u00a7b" : text.startsWith("Removed ") || text.startsWith("Changed ") || text.startsWith("Undid ") || text.startsWith("Redid ") ? "\u00a7a" : "\u00a77";
    return "\u00a76[WE] " + colour + text + "\u00a7r";
  }
  public static String error(String text) { return "\u00a76[WE] \u00a7c" + text + "\u00a7r"; }
}
