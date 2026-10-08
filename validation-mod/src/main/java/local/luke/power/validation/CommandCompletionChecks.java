package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.validation.mixin.ChatInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.input.Keyboard;

final class CommandCompletionChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    var rules = (CheatWorld) mc.world.method_262(); boolean cheats = rules.power$cheatsEnabled();
    try {
      rules.power$cheatsEnabled(true);
      for (String seed : new String[] {"//set st", "//replace st", "/gamemode "})
        for (int key : new int[] {Keyboard.KEY_UP, Keyboard.KEY_DOWN}) test("arrow selection and immediate Tab: " + seed + "/" + key, () -> {
          ChatScreen screen = new ChatScreen(); mc.setScreen(screen);
          seed(screen, seed);
          var input = (ChatInput) screen; input.power$type('\0', key);
          String[] choices = (String[]) field(screen, "suggestions");
          check(choices.length > 1, "no selection list " + java.util.Arrays.toString(choices));
          int chosen = (int) field(screen, "chosen");
          String expected = seed + choices[chosen];
          input.power$type('\t', Keyboard.KEY_TAB);
          check(text(screen).equals(expected) && input.power$text().equals(expected), "Tab did not update both inputs: " + text(screen) + "/" + input.power$text());
          screen.render(-1, -1, 0); screen.render(-1, -1, 0);
          check(text(screen).equals(expected), "render changed input");
          input.power$type(' ', Keyboard.KEY_SPACE);
          check(text(screen).equals(expected + " "), "typing after completion duplicated or lost text: " + text(screen));
          input.power$type('h', Keyboard.KEY_H);
          check(text(screen).equals(expected + " h"), "next character lost");
        });
      test("typing refreshes the list immediately; single match Tab works", () -> {
        ChatScreen s = new ChatScreen(); mc.setScreen(s); seed(s, "//h");
        ((ChatInput)s).power$type('e', Keyboard.KEY_E);
        check(text(s).equals("//he"), "typing swallowed");
        String[] choices = (String[]) field(s, "suggestions");
        check(java.util.Arrays.asList(choices).contains("lp"), "suggestions one keystroke behind: " + java.util.Arrays.toString(choices));
        ((ChatInput)s).power$type('\t', Keyboard.KEY_TAB);
        check(text(s).equals("//help"), "single match did not complete");
      });
    } finally { rules.power$cheatsEnabled(cheats); mc.setScreen(null); }
    log("COMMAND COMPLETION FAILURES " + failures);
  }
  private static void seed(ChatScreen s, String text) throws Exception {
    ((ChatInput)s).power$text(text);
    Class.forName("local.luke.power.worldedit.chat.ChatAccess").getMethod("setText", String.class).invoke(null, text);
  }
  private static String text(ChatScreen s) throws Exception {
    return (String) Class.forName("local.luke.power.worldedit.chat.ChatAccess").getMethod("text", String.class).invoke(null, ((ChatInput)s).power$text());
  }
  private static Object field(Object o, String name) throws Exception {
    for (var f : o.getClass().getDeclaredFields()) if (f.getName().equals(name) || f.getName().endsWith("$" + name)) {
      f.setAccessible(true); return f.get(o);
    }
    throw new NoSuchFieldException(name);
  }
}
