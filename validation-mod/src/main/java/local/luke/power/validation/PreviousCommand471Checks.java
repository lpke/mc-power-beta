package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.chat.*;
import local.luke.power.input.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Real chat submission followed by a physical repeat binding, without reopening chat. */
final class PreviousCommand471Checks {
  private static final Path REPORT = Path.of("power-beta-previous-command-471.json");
  private static JsonObject document;
  private static Map<String,Integer> modifiers;
  private static final Map<KeyBinding,Integer> codes = new IdentityHashMap<>();
  private static List<String> notes, history, expectedNotes, expectedHistory;
  private static Object previousWorld;
  private static String previousCommand;
  private static Screen previousScreen;
  private static int seedBefore, emptyBefore;
  private PreviousCommand471Checks() {}

  @SuppressWarnings("unchecked")
  private static List<String> history() throws Exception {
    return (List<String>) Class.forName("local.luke.power.client_fixes.client.text.chat.ChatScreenVariables")
        .getField("CHAT_HISTORY").get(null);
  }
  private static java.lang.reflect.Field recall(String name) throws Exception {
    var field = PreviousCommand.class.getDeclaredField(name); field.setAccessible(true); return field;
  }
  private static long count(String text) {
    return ChatTranscript.messages().stream().filter(message -> message.plain.equals(text)).count();
  }
  private static String seed(Minecraft mc) { return "Seed | " + mc.world.getSeed(); }
  private static void baseline() throws Exception {
    expectedNotes = ChatNotes.history(); expectedHistory = List.copyOf(history());
  }

  @SuppressWarnings("unchecked")
  static void run(Minecraft mc, String action) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "use a disposable singleplayer world");
    if (action.equals("setup")) {
      check(document == null && Display.isActive(), "finish the earlier check and focus the disposable client");
      document = PowerConfig.document(); modifiers = Bindings.modifiers();
      notes = ChatNotes.history(); history = List.copyOf(history());
      previousWorld = recall("world").get(null); previousCommand = (String)recall("command").get(null);
      previousScreen = mc.currentScreen;
      for (KeyBinding binding : mc.options.allKeys) if (binding.code == Keyboard.KEY_F9
          || binding == GameplayKeys.PREVIOUS_COMMAND || binding == GameplayKeys.COMMANDS || binding == mc.options.chatKey) {
        codes.put(binding, binding.code); binding.code = 0;
      }
      GameplayKeys.PREVIOUS_COMMAND.code = GameplayKeys.COMMANDS.code = mc.options.chatKey.code = Keyboard.KEY_F9;
      var changed = new HashMap<>(modifiers);
      changed.remove(GameplayKeys.PREVIOUS_COMMAND.translationKey); changed.remove(GameplayKeys.COMMANDS.translationKey);
      changed.remove(mc.options.chatKey.translationKey);
      Bindings.configure(changed); Bindings.register(mc.options.allKeys);
      PreviousCommand.clear(); mc.setScreen(null); baseline();
      seedBefore = (int)count(seed(mc)); emptyBefore = (int)count("No previous command in this world.");
      log("PREVIOUS471 ready for physical F9, shared with Chat and Open commands to verify ownership");
      return;
    }
    check(document != null, "run previous-command-471 setup first");
    if (action.equals("finish")) {
      try {
        codes.forEach((binding,code) -> binding.code = code); Bindings.configure(modifiers); Bindings.register(mc.options.allKeys);
        PreviousCommand.clear(); recall("world").set(null, previousWorld); recall("command").set(null, previousCommand);
        var stored = ChatNotes.class.getDeclaredField("messages"); stored.setAccessible(true);
        var values = (List<String>)stored.get(null); values.clear(); values.addAll(notes);
        history().clear(); history().addAll(history);
        PowerConfig.flushPending(); PowerConfig.write(document);
        mc.setScreen(previousScreen);
      } finally { document = null; codes.clear(); }
      return;
    }
    if (action.equals("seed")) {
      var response = ChatChecks.submit(mc, "/seed");
      check(response.stream().anyMatch(line -> line.contains("Seed")), "native seed command did not run"); baseline();
    } else if (action.equals("chat")) {
      ChatChecks.submit(mc, "PB471 ordinary chat after a command"); baseline();
    } else if (action.equals("menu")) mc.setScreen(new net.minecraft.class_525());
    else if (action.equals("ordinary-chat")) {
      GameplayKeys.PREVIOUS_COMMAND.code = GameplayKeys.COMMANDS.code = 0;
      Bindings.register(mc.options.allKeys);
    }
    else if (action.equals("close")) mc.setScreen(null);
    else if (action.equals("clear")) PreviousCommand.clear();
    else check(action.equals("state"), "unknown previous-command-471 action");
    JsonObject state = new JsonObject();
    state.addProperty("seedMessages", count(seed(mc)) - seedBefore);
    state.addProperty("emptyMessages", count("No previous command in this world.") - emptyBefore);
    state.addProperty("screen", mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getSimpleName());
    state.addProperty("notesUnchanged", expectedNotes.equals(ChatNotes.history()));
    state.addProperty("historyUnchanged", expectedHistory.equals(history()));
    Files.writeString(REPORT, new Gson().toJson(state));
  }
}
