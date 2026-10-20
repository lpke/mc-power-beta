package local.luke.power.chat;

import java.util.ArrayDeque;
import net.minecraft.client.Minecraft;

/** Local notes reappear only in history; server echoes are never persisted or replayed to a server. */
public final class PlayerChat {
  private record Echo(String text, long expires) {}
  private static final ArrayDeque<Echo> echoes = new ArrayDeque<>();
  private static Object world;
  private static boolean ready, own, restoring;
  private static String reportedError = "";
  private PlayerChat() {}
  public static boolean own() { return own; }
  public static boolean restoring() { return restoring; }
  public static boolean echo(String text) {
    long now = System.nanoTime();
    while (!echoes.isEmpty() && echoes.peekFirst().expires < now) echoes.removeFirst();
    if (echoes.isEmpty()) return false;
    String plain = text.indexOf('\u00a7') < 0 ? text : text.replaceAll("\\u00a7.", "");
    for (var iterator = echoes.iterator(); iterator.hasNext();) if (iterator.next().text.equals(plain)) {
      iterator.remove(); return true;
    }
    return false;
  }
  private static void display(Minecraft mc, String text, boolean old) {
    boolean previous = own, previousRestore = restoring; own = true; restoring = old;
    try { mc.inGameHud.addChatMessage(text); }
    finally { own = previous; restoring = previousRestore; }
  }
  public static void tick(Minecraft mc) {
    if (!ready) {
      ChatNotes.history(); // Read once during startup, before gameplay input or HUD rendering can request notes.
      ready = true;
      ChatNotes.onSent(text -> {
        if (mc.world == null || mc.player == null || text.startsWith("/")) return;
        String line = "<" + mc.player.name + "> " + text;
        if (echoes.size() == 32) echoes.removeFirst();
        echoes.addLast(new Echo(line, System.nanoTime() + 10_000_000_000L));
        display(mc, line, false);
      });
    }
    ChatNotes.maintainLimit();
    if (mc.world != world) {
      world = mc.world; echoes.clear();
      if (mc.inGameHud instanceof ChatNotes.History history) history.power$removeSavedMessages();
      if (mc.world != null && mc.player != null)
        for (String text : ChatNotes.history()) if (!text.startsWith("/")) display(mc, "<You> " + text, true);
    }
    if (mc.world != null && !ChatNotes.error().isEmpty() && !ChatNotes.error().equals(reportedError)) {
      reportedError = ChatNotes.error(); local.luke.power.PowerBeta.LOG.error(reportedError);
      if (mc.inGameHud != null) mc.inGameHud.addChatMessage("\u00a7c" + reportedError);
    }
  }
}
