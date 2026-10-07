package local.luke.power.validation;
import java.util.*;
import local.luke.power.validation.mixin.ChatInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.input.Keyboard;

/** Exercises the full screen -> player -> command dispatch path with installed mixins. */
public final class ChatChecks {
  private static final List<String> messages = new ArrayList<>();
  private static boolean recording;
  static List<String> record(Validation.Check action) throws Exception {
    messages.clear(); recording = true;
    try { action.run(); return List.copyOf(messages); }
    finally { recording = false; }
  }
  public static void capture(String text) {
    if (recording) messages.add(text);
    else try { Validation.log("GAME CHAT " + text); } catch (Exception ignored) {}
  }
  public static List<String> submit(Minecraft mc, String input) throws Exception {
    ChatScreen screen = new ChatScreen();
    mc.setScreen(screen);
    // The installed text widget owns chat history and input. Do not bypass Enter.
    Class.forName("local.luke.power.worldedit.chat.ChatAccess").getMethod("setText", String.class).invoke(null, input);
    ((ChatInput)screen).power$text(input);
    messages.clear(); recording = true;
    try { ((ChatInput)screen).power$type('\n', Keyboard.KEY_RETURN); }
    finally { recording = false; }
    List<String> result = List.copyOf(messages);
    for (String message : result) Validation.log("CHAT " + message);
    return result;
  }
  public static void run(Minecraft mc) throws Exception {
    var session = local.luke.power.config.SettingsRegistry.open(mc);
    var enabled = Validation.find(session, "worldedit.enabled");
    var before = enabled.value.deepCopy();
    enabled.value = new com.google.gson.JsonPrimitive(true);
    session.save(java.nio.file.Path.of(".").toAbsolutePath());
    try {
      submit(mc, "/gamemode creative");
      var help = submit(mc, "//help");
      Validation.test("chat help executes once", () -> Validation.check(help.stream().filter(s -> s.contains("World editing")).count() == 1, help.toString()));
      submit(mc, "/remove items 5");
      var item = new net.minecraft.class_142(mc.world, mc.player.x + 1,
          mc.player.boundingBox.minY, mc.player.z, new net.minecraft.item.ItemStack(1, 1, 0));
      mc.world.method_210(item);
      var remove = submit(mc, "/remove items 5");
      Validation.test("chat removal has one real side effect", () -> Validation.check(item.dead && remove.stream().anyMatch(m -> m.contains("Removed 1 entities")), remove.toString()));
      Validation.test("chat entity removal executes once", () -> Validation.check(remove.stream().filter(s -> s.contains("Removed ")).count() == 1, remove.toString()));
      var upper = submit(mc, "//HELP 6");
      Validation.test("uppercase command executes once", () -> Validation.check(upper.stream().filter(s -> s.contains("Help 6/6")).count() == 1, upper.toString()));
      var invalid = submit(mc, "//set");
      Validation.test("command error is coloured", () -> Validation.check(invalid.size() == 1 && invalid.get(0).contains("\u00a7c"), invalid.toString()));
      // Native history must still contain the latest submission.
      mc.setScreen(new ChatScreen());
      ((ChatInput)mc.currentScreen).power$type('\0', Keyboard.KEY_UP);
      String history = (String)Class.forName("local.luke.power.worldedit.chat.ChatAccess").getMethod("text", String.class).invoke(null, "");
      Validation.test("chat history retains command", () -> Validation.check(history.equals("//set"), history));
      Class.forName("local.luke.power.worldedit.chat.ChatAccess").getMethod("setText", String.class).invoke(null, "//he");
      ((ChatInput)mc.currentScreen).power$text("//he");
      ((ChatInput)mc.currentScreen).power$type('\t', Keyboard.KEY_TAB);
      String completion = (String)Class.forName("local.luke.power.worldedit.chat.ChatAccess").getMethod("text", String.class).invoke(null, "");
      Validation.test("double slash completion preserved", () -> Validation.check(completion.equals("//help"), completion));
    } finally { enabled.value = before; session.save(java.nio.file.Path.of(".").toAbsolutePath()); mc.setScreen(null); }
  }
}
