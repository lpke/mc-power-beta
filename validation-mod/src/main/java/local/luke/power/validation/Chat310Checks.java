package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.chat.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.validation.mixin.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.input.Keyboard;

/** Sends through the real chat editor, then verifies local/server categories and persisted notes. */
final class Chat310Checks {
  @SuppressWarnings("unchecked") static void run(Minecraft mc) throws Exception {
    failures=0;check(mc.world!=null&&mc.player!=null,"open disposable world first");
    PowerConfig.flushPending();byte[] original=Files.readAllBytes(PowerConfig.path());
    var lines=((ChatLinesAccess)mc.inGameHud).power$messages();var savedLines=List.copyOf(lines);var previous=mc.currentScreen;
    var own=ChatNotes.class.getDeclaredField("ownLimit");own.setAccessible(true);var oldOwn=own.get(null);
    var other=ChatNotes.class.getDeclaredField("otherLimit");other.setAccessible(true);var oldOther=other.get(null);
    var notes=ChatNotes.class.getDeclaredField("messages");notes.setAccessible(true);var savedNotes=List.copyOf((List<String>)notes.get(null));
    Object retention=Editor230Checks.get(mc.inGameHud,"power$retention");
    Map<Object,Object> tags=(Map<Object,Object>)field(retention,"tags");var savedTags=new IdentityHashMap<>(tags);
    var world=PlayerChat.class.getDeclaredField("world");world.setAccessible(true);var oldWorld=world.get(null);
    try {
      ChatNotes.limits(()->3,()->2);lines.clear();
      test("native Enter persists local text once while other chat uses its own limit",()->{
        for(String note:List.of("PB310 note alpha","PB310 note beta")) {
          var screen=new ChatScreen();mc.setScreen(screen);
          Class.forName("local.luke.power.worldedit.chat.ChatAccess").getMethod("setText",String.class).invoke(null,note);
          ((ChatInput)screen).power$type('\0',Keyboard.KEY_RETURN);
          check(lines.stream().filter(line->line.text.contains(note)).count()==1,"native echo duplicated own note");
        }
        for(int i=0;i<12;i++)mc.inGameHud.addChatMessage("PB310 server row "+i);
        check(lines.stream().filter(line->line.text.contains("PB310 note")).count()==2,"ordinary limit evicted own notes");
        check(lines.stream().filter(line->line.text.contains("PB310 server row")).count()==3,"ordinary chat limit not applied independently");
        PowerConfig.flushPending();var persisted=PowerConfig.section("chatNotes").getAsJsonArray("messages");
        check(persisted.size()==2 && persisted.toString().contains("PB310 note alpha") && persisted.toString().contains("PB310 note beta"),"wrong notes saved");
        check(!persisted.toString().contains("server row"),"server transcript reached persistent notes");
        mc.setScreen(new ChatScreen());
        List<String> recall=(List<String>)Class.forName("local.luke.power.client_fixes.client.text.chat.ChatScreenVariables").getField("CHAT_HISTORY").get(null);
        check(recall.equals(List.of("PB310 note alpha","PB310 note beta")),"reopening chat lost saved recall");
      });
      test("world transitions restore saved scrollback without duplicating own messages",()->{
        for(int pass=0;pass<2;pass++) {
          world.set(null,null);PlayerChat.tick(mc);
          check(lines.stream().filter(line->line.text.contains("PB310 note alpha")).count()==1,"world restore duplicated own note");
          check(lines.stream().filter(line->line.text.contains("PB310 note beta")).count()==1,"world restore duplicated own note");
        }
      });
    } finally {
      own.set(null,oldOwn);other.set(null,oldOther);world.set(null,oldWorld);
      var stored=(List<String>)notes.get(null);stored.clear();stored.addAll(savedNotes);
      lines.clear();lines.addAll(savedLines);tags.clear();tags.putAll(savedTags);
      PowerConfig.flushPending();PowerConfig.write(com.google.gson.JsonParser.parseString(new String(original,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
      mc.setScreen(previous);
    }
    log("CHAT310 COMPLETE failures="+failures);
  }
}
