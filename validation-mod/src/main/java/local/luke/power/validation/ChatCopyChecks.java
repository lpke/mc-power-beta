package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.lang.reflect.Field;
import java.util.List;
import local.luke.power.chat.ChatSelection;
import local.luke.power.validation.mixin.ChatInput;
import local.luke.power.validation.mixin.ChatLinesAccess;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;

/** Leaves a real native history selection ready for an OS Ctrl+C press/release sequence. */
final class ChatCopyChecks {
  private static final String EXPECTED = "Copy this selected chat history.";
  private static Session session;

  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("chat-physical-setup")) setup(mc);
    else if (action.equals("chat-physical-state")) state(mc);
    else if (action.equals("chat-physical-cleanup")) cleanup(mc);
    else throw new IllegalArgumentException("Use chat-physical-setup, chat-physical-state or chat-physical-cleanup");
  }

  private static void setup(Minecraft mc) throws Exception {
    check(session == null, "clean up the previous physical chat fixture first");
    var messages = ((ChatLinesAccess)mc.inGameHud).power$messages();
    var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    ChatScreen screen = new ChatScreen();
    session = new Session(List.copyOf(messages), mc.currentScreen, screen, clipboard.getContents(null), offset(mc).getInt(mc.inGameHud));
    try {
      clipboard.setContents(new StringSelection("Waiting for a real Ctrl+C key sequence"), null);
      messages.clear(); mc.inGameHud.addChatMessage("\u00a7c" + EXPECTED);
      mc.setScreen(screen); mc.inGameHud.render(0, true, -1, -1);
      int y = screen.height - 47, end = 2 + mc.textRenderer.getWidth(EXPECTED);
      ((ScreenInput)screen).power$click(2, y, 0);
      ChatSelection.history.move(screen, end, y, true);
      check(ChatSelection.history.text().equals(EXPECTED), "native history setup did not select the expected text");
      log("CHAT PHYSICAL ARMED selected=" + EXPECTED + "; send real left/right Ctrl+C key press/releases, then chat-physical-state"
          + "; optional real selection drag GUI=(2," + y + ")->(" + end + "," + y + ")"
          + " GUI size=" + screen.width + "x" + screen.height + " display=" + mc.displayWidth + "x" + mc.displayHeight
          + "; finish with chat-physical-cleanup");
    } catch (Exception | Error error) { cleanup(mc); throw error; }
  }

  private static void state(Minecraft mc) throws Exception {
    check(session != null && mc.currentScreen == session.chat, "physical chat fixture is not open");
    failures = 0;
    test("real modifier and C events copy native history without touching input", () -> {
      check(EXPECTED.equals(Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor)),
          "real Ctrl+C did not copy selected history");
      check(ChatSelection.history.text().equals(EXPECTED), "real modifier sequence cleared history selection");
      check(((ChatInput)session.chat).power$text().isEmpty(), "real Ctrl+C changed chat input");
    });
    log("CHAT PHYSICAL failures=" + failures);
  }

  private static void cleanup(Minecraft mc) throws Exception {
    if (session == null) return;
    Session saved = session;
    var messages = ((ChatLinesAccess)mc.inGameHud).power$messages();
    messages.clear(); messages.addAll(saved.messages);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
        saved.clipboard == null ? new StringSelection("") : saved.clipboard, null);
    mc.setScreen(saved.previous); offset(mc).setInt(mc.inGameHud, saved.offset);
    ChatSelection.history.context(null, false);
    session = null; log("CHAT PHYSICAL CLEANED");
  }

  private static Field offset(Minecraft mc) throws Exception {
    for (Class<?> type = mc.inGameHud.getClass(); type != null; type = type.getSuperclass())
      for (Field field : type.getDeclaredFields())
        if (field.getName().equals("chatOffset") || field.getName().endsWith("$chatOffset")) {
          field.setAccessible(true); return field;
        }
    throw new NoSuchFieldException("chatOffset");
  }

  private record Session(List<ChatHudLine> messages, Screen previous, ChatScreen chat, Transferable clipboard, int offset) {}
}
