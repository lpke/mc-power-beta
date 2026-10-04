package local.luke.power.chat;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Shared help formatting; the client supplies its live chat-scrolling preference. */
public final class HelpOutput {
  private static BooleanSupplier scrolling = () -> false;

  private HelpOutput() {}

  public static void scrolling(BooleanSupplier value) {
    scrolling = value;
  }

  public static boolean scrolling() {
    return scrolling.getAsBoolean();
  }

  public static String line(String text) {
    return "§7"
        + text.replaceFirst("^(Usage(?: [0-9]+)?|Info): *", "")
            .replaceAll("(/{1,2}[a-z][a-z0-9!_-]*)", "§b$1§7")
            .replace(" | ", " §8| §7")
        + "§r";
  }

  public static void print(
      Consumer<String> out,
      String title,
      List<String> lines,
      int requestedPage,
      int pageSize,
      boolean all) {
    int pages = Math.max(1, (lines.size() + pageSize - 1) / pageSize);
    if (requestedPage < 1 || requestedPage > pages)
      throw new IllegalArgumentException("Choose a help page from 1 to " + pages + ".");
    out.accept("§6" + title + (all ? "" : " §8| §7" + requestedPage + "/" + pages) + "§r");
    int from = all ? 0 : (requestedPage - 1) * pageSize;
    int to = all ? lines.size() : Math.min(lines.size(), from + pageSize);
    for (int i = from; i < to; i++) out.accept(line(lines.get(i)));
  }
}
