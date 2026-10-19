package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.*;
import local.luke.power.chat.ChatSelection;
import local.luke.power.validation.mixin.*;
import net.minecraft.class_27;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Native chat rendering/input and the Creative mouse-press dispatcher, without OS events. */
final class Release300ChatBreakChecks {
  static void run(Minecraft mc, String action) throws Exception {
    check(Set.of("", "all", "chat", "break", "physical-setup", "physical-state", "physical-cleanup",
        "chat-physical-setup", "chat-physical-state", "chat-physical-cleanup").contains(action),
        "Use release-300-chat-break [all|chat|break|physical-setup|physical-state|physical-cleanup|chat-physical-setup|chat-physical-state|chat-physical-cleanup]");
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive(),
        "focus a disposable singleplayer world first");
    if (action.startsWith("physical-")) { CreativePressChecks.run(mc, action); return; }
    if (action.startsWith("chat-physical-")) { ChatCopyChecks.run(mc, action); return; }
    failures = 0;
    if (action.isBlank() || action.equals("all") || action.equals("chat")) chat(mc);
    if (action.isBlank() || action.equals("all") || action.equals("break")) breaking(mc);
    log("RELEASE300 CHAT BREAK failures=" + failures);
  }

  private static void chat(Minecraft mc) throws Exception {
    var messages = ((ChatLinesAccess)mc.inGameHud).power$messages();
    var savedMessages = List.copyOf(messages); var previous = mc.currentScreen;
    var rules = AccessRules.copy(mc);
    var keysField = Keyboard.class.getDeclaredField("keyDownBuffer"); keysField.setAccessible(true);
    var keys = (ByteBuffer)keysField.get(null);
    int[] modifiers = {Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL, Keyboard.KEY_LSHIFT,
        Keyboard.KEY_RSHIFT, Keyboard.KEY_LMENU, Keyboard.KEY_RMENU};
    byte[] savedModifiers = new byte[modifiers.length];
    for (int i = 0; i < modifiers.length; i++) { savedModifiers[i] = keys.get(modifiers[i]); keys.put(modifiers[i], (byte)0); }
    var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard(); var previousClipboard = clipboard.getContents(null);
    try {
      AccessRules.of(mc.world.method_262()).allAllowed(true);
      test("native wrapped chat selection copies color-free text through Ctrl+C", () -> {
        messages.clear(); mc.inGameHud.addChatMessage("\u00a7cSelectable wrapped chat history. ".repeat(20));
        ChatScreen screen = new ChatScreen(); mc.setScreen(screen);
        mc.inGameHud.render(0, true, -1, -1);
        int bottom = screen.height - 48;
        ((ScreenInput)screen).power$click(2, bottom - 8, 0);
        ChatSelection.history.move(screen, 9999, bottom + 1, true);
        String expected = plain(messages.get(1).text) + "\n" + plain(messages.get(0).text);
        check(ChatSelection.history.text().equals(expected), ChatSelection.history.text());
        for (int key : modifiers) {
          keys.put(key, (byte)1); ((ChatInput)screen).power$type('\0', key);
          check(ChatSelection.history.text().equals(expected), "modifier cleared history selection: " + key);
          check(((ChatInput)screen).power$text().isEmpty(), "modifier changed chat input: " + key);
          keys.put(key, (byte)0);
        }
        for (int key : new int[] {Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL}) {
          clipboard.setContents(new java.awt.datatransfer.StringSelection("Waiting for native Ctrl+C"), null);
          keys.put(key, (byte)1); ((ChatInput)screen).power$type('\0', key);
          ((ChatInput)screen).power$type('\0', Keyboard.KEY_C); keys.put(key, (byte)0);
          check(expected.equals(clipboard.getData(DataFlavor.stringFlavor)), "clipboard differs from visible selection for Ctrl " + key);
          check(ChatSelection.history.text().equals(expected), "Ctrl+C cleared history selection");
          check(((ChatInput)screen).power$text().isEmpty(), "copy changed chat input");
        }
      });
      test("pure modifiers preserve normal input selection and chosen completion", () -> {
        ChatScreen screen = new ChatScreen(); mc.setScreen(screen);
        Class.forName("local.luke.power.worldedit.chat.ChatAccess").getMethod("setText", String.class).invoke(null, "//set st");
        ((ChatInput)screen).power$type('\0', Keyboard.KEY_F8);
        ((ChatInput)screen).power$type('\0', Keyboard.KEY_UP);
        int chosen = field(screen, "chosen").getInt(screen);
        var editor = (local.luke.power.chat.ChatBuffer)Class.forName("local.luke.power.client_fixes.client.text.chat.ChatScreenVariables")
            .getField("editor").get(null);
        for (int key : modifiers) {
          keys.put(key, (byte)1); ((ChatInput)screen).power$type('\0', key); keys.put(key, (byte)0);
          check(field(screen, "chosen").getInt(screen) == chosen, "modifier changed chosen completion: " + key);
          check(editor.text().equals("//set st"), "modifier changed input: " + key);
        }
        editor.position(2, true); int start = editor.start(), end = editor.end(), cursor = editor.cursor();
        for (int key : modifiers) {
          keys.put(key, (byte)1); ((ChatInput)screen).power$type('\0', key); keys.put(key, (byte)0);
          check(editor.cursor() == cursor && editor.start() == start && editor.end() == end,
              "modifier changed input selection: " + key);
        }
      });
      test("scrolled native chat selects the rendered message and input selection stays independent", () -> {
        messages.clear(); for (int i = 0; i < 25; i++) mc.inGameHud.addChatMessage("History row " + i);
        var offset = field(mc.inGameHud, "chatOffset"); offset.setInt(mc.inGameHud, 3);
        ChatScreen screen = new ChatScreen(); mc.setScreen(screen); mc.inGameHud.render(0, true, -1, -1);
        int bottom = screen.height - 48;
        ((ScreenInput)screen).power$click(2, bottom + 1, 0);
        ChatSelection.history.move(screen, 9999, bottom + 1, true);
        check(ChatSelection.history.text().equals(messages.get(3).text), "selected an unscrolled history row");
        ((ScreenInput)screen).power$click(20, screen.height - 10, 0);
        check(!ChatSelection.history.selected(), "input click retained history selection");
        ((ChatInput)screen).power$type('x', Keyboard.KEY_X);
        check(((ChatInput)screen).power$text().equals("x"), "history selection interfered with input typing");
        ((ScreenInput)screen).power$click(2, bottom + 1, 0);
        ChatSelection.history.move(screen, 9999, bottom + 1, false);
        ChatSelection.history.context(screen, false);
        check(!ChatSelection.history.selected() && !ChatSelection.history.dragging(), "focus loss retained selection");
        mc.setScreen(null); check(!ChatSelection.history.selected(), "closed chat retained selection");
      });
    } finally {
      for (int i = 0; i < modifiers.length; i++) keys.put(modifiers[i], savedModifiers[i]);
      if (previousClipboard != null) clipboard.setContents(previousClipboard, null);
      messages.clear(); messages.addAll(savedMessages); mc.setScreen(previous);
      AccessRules.restore(mc, rules);
      field(mc.inGameHud, "chatOffset").setInt(mc.inGameHud, 0);
    }
  }

  private static void breaking(Minecraft mc) throws Exception {
    var rules = AccessRules.copy(mc); var previous = mc.currentScreen; var target = mc.field_2823;
    boolean focus = mc.field_2778; var view = mc.field_2807;
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    var inventory = mc.player.inventory.main.clone(); int slot = mc.player.inventory.selectedSlot;
    Field cooldown = field(mc, "field_2787"); int oldCooldown = cooldown.getInt(mc);
    int x = (int)Math.floor(mc.player.x) + 2, y = Math.min(124, (int)Math.floor(mc.player.y) + 3), z = (int)Math.floor(mc.player.z);
    for (int i = 0; i < 3; i++) check(mc.world.getBlockId(x + i, y, z) == 0, "fixture needs three air blocks above the player");
    try {
      AccessRules.of(mc.world.method_262()).allAllowed(true); mc.setScreen(null); mc.field_2778 = true; mc.field_2807 = mc.player;
      CreativeVehicleChecks.mode(mc, "CREATIVE"); mc.player.inventory.main[slot] = null;
      Method press = Arrays.stream(Minecraft.class.getDeclaredMethods())
          .filter(m -> m.getName().contains("power$creativePress"))
          .findFirst().orElseThrow(() -> new NoSuchMethodException("Creative press handler missing from " + Minecraft.class.getName()
              + "; launcher class=" + mc.getClass().getName()));
      press.setAccessible(true);
      Operation<Void> nativeAttack = args -> { ((Client253Access)mc).power$use((int)args[1]); return null; };
      test("rapid Creative presses break once each while held repeats retain their delay", () -> {
        cooldown.setInt(mc, 0);
        for (int i = 0; i < 3; i++) mc.world.method_154(x + i, y, z, 1, 0);
        hit(mc, x, y, z); press.invoke(mc, mc, 0, nativeAttack);
        check(mc.world.getBlockId(x, y, z) == 0 && cooldown.getInt(mc) == 5, "first press did not break exactly once");
        hit(mc, x + 1, y, z); ((Client253Access)mc).power$use(0);
        check(mc.world.getBlockId(x + 1, y, z) == 1, "held repeat bypassed Creative delay");
        press.invoke(mc, mc, 0, nativeAttack);
        check(mc.world.getBlockId(x + 1, y, z) == 0 && mc.world.getBlockId(x + 2, y, z) == 1,
            "rapid second press skipped or broke a second target");
        hit(mc, x + 2, y, z); cooldown.setInt(mc, 10); press.invoke(mc, mc, 0, nativeAttack);
        check(mc.world.getBlockId(x + 2, y, z) == 1 && cooldown.getInt(mc) == 10, "press erased an unrelated native cooldown");
        cooldown.setInt(mc, 5); mc.field_2823 = null; press.invoke(mc, mc, 0, nativeAttack);
        check(cooldown.getInt(mc) == 5, "unsupported press erased Creative delay");
        hit(mc, x + 2, y, z); CreativeVehicleChecks.mode(mc, "SURVIVAL");
        press.invoke(mc, mc, 0, nativeAttack);
        check(mc.world.getBlockId(x + 2, y, z) == 1 && cooldown.getInt(mc) == 5, "Survival bypassed an owned Creative delay");
        CreativeVehicleChecks.mode(mc, "CREATIVE"); mc.world.isRemote = true;
        try {
          press.invoke(mc, mc, 0, nativeAttack);
          check(mc.world.getBlockId(x + 2, y, z) == 1 && cooldown.getInt(mc) == 5, "remote press bypassed the client delay");
        } finally { mc.world.isRemote = false; }
      });
    } finally {
      for (int i = 0; i < 3; i++) mc.world.method_154(x + i, y, z, 0, 0);
      System.arraycopy(inventory, 0, mc.player.inventory.main, 0, inventory.length); mc.player.inventory.selectedSlot = slot;
      CreativeVehicleChecks.mode(mc, mode); AccessRules.restore(mc, rules); mc.field_2823 = target;
      mc.field_2778 = focus; mc.field_2807 = view; cooldown.setInt(mc, oldCooldown); mc.setScreen(previous);
    }
  }

  private static void hit(Minecraft mc, int x, int y, int z) {
    mc.field_2823 = new class_27(x, y, z, 1, Vec3d.createCached(x + .5, y + 1, z + .5));
  }
  private static String plain(String text) { return text.replaceAll("\u00a7.", ""); }
  private static Field field(Object owner, String suffix) throws Exception {
    for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass())
      for (Field f : type.getDeclaredFields()) if (f.getName().equals(suffix) || f.getName().endsWith("$" + suffix)) {
        f.setAccessible(true); return f;
      }
    throw new NoSuchFieldException(suffix);
  }
}
