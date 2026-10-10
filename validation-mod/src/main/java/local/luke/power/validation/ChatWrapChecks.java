package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.validation.mixin.ChatLinesAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.ChatHudLine;

final class ChatWrapChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    List<ChatHudLine> messages = ((ChatLinesAccess) mc.inGameHud).power$messages();
    var old = List.copyOf(messages);
    var world = (CheatWorld) mc.world.method_262();
    boolean cheats = world.power$cheatsEnabled();
    try {
      for (char code : "0123456789abcdefABCDEF".toCharArray()) test("chat wraps colour " + code, () -> {
        checkMessage(mc, messages, "§" + code + "Coloured chat continues over several lines. ".repeat(5));
        check(messages.size() >= 3, "message did not wrap repeatedly");
        for (var line : messages) check(line.text.startsWith("§" + code), line.text);
      });
      test("chat preserves colour changes, resets and boundary codes", () -> {
        checkMessage(mc, messages, "§e[WE] §c" + "Red ".repeat(40) + "§a" + "Green ".repeat(40)
            + "§r" + "White ".repeat(40));
        String full = "§c" + "W".repeat(40) + "§a" + "W".repeat(50);
        checkMessage(mc, messages, full);
        check(messages.get(messages.size() - 2).text.startsWith("§a"), "boundary colour lost");
      });
      test("plain chat and subsequent messages stay unchanged", () -> {
        checkMessage(mc, messages, "Plain chat ".repeat(30));
        for (var line : messages) check(!line.text.contains("§"), line.text);
        mc.inGameHud.addChatMessage("Short plain message");
        check(messages.get(0).text.equals("Short plain message"), "colour leaked between messages");
      });
      test("WorldEdit cheats warning retains red on every wrapped line", () -> {
        world.power$cheatsEnabled(false); messages.clear();
        var result = ChatChecks.submit(mc, "//set stone");
        check(result.size() == 1 && result.get(0).contains("Cheats are disabled"), result.toString());
        check(messages.size() >= 2, "warning did not wrap");
        for (int i = 0; i < messages.size() - 1; i++)
          check(messages.get(i).text.startsWith("§c"), messages.get(i).text);
        mc.setScreen(new net.minecraft.client.gui.screen.ChatScreen());
      });
    } finally {
      world.power$cheatsEnabled(cheats);
      if (failures != 0) { messages.clear(); messages.addAll(old); }
    }
    log("CHAT WRAP FAILURES " + failures);
  }

  private static void checkMessage(Minecraft mc, List<ChatHudLine> messages, String text) {
    messages.clear(); mc.inGameHud.addChatMessage(text);
    var ordered = new ArrayList<>(messages); Collections.reverse(ordered);
    StringBuilder visible = new StringBuilder(), colours = new StringBuilder();
    for (var line : ordered) {
      check(mc.textRenderer.getWidth(line.text) <= 320, "wrapped line exceeds chat width");
      append(line.text, visible, colours);
    }
    var expectedText = new StringBuilder(); var expectedColours = new StringBuilder();
    append(text, expectedText, expectedColours);
    check(visible.toString().equals(expectedText.toString()), "wrapping lost or duplicated text");
    check(colours.toString().equals(expectedColours.toString()), "wrapping changed character colours");
  }

  private static void append(String text, StringBuilder visible, StringBuilder colours) {
    char colour = 'f';
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '§' && i + 1 < text.length()) {
        char code = Character.toLowerCase(text.charAt(++i));
        colour = "0123456789abcdef".indexOf(code) < 0 ? 'f' : code;
      } else { visible.append(c); colours.append(colour); }
    }
  }
}
