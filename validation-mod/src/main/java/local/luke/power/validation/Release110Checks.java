package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.chat.ChatBuffer;
import local.luke.power.config.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.world.WorldSpawning;
import local.luke.power.worldedit.WorldEditor;
import local.luke.power.worldedit.core.*;
import local.luke.power.validation.mixin.ChatInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.world.WorldProperties;
import net.minecraft.class_567;
import org.lwjgl.input.Keyboard;

final class Release110Checks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    var props = mc.world.method_262(); var cheats = (CheatWorld) props; var spawn = (WorldSpawning) props;
    boolean oldCheats = cheats.power$cheatsEnabled(), hostile = spawn.power$hostileSpawning(), passive = spawn.power$passiveSpawning();
    try {
      cheats.power$cheatsEnabled(true);
      test("chat history keeps arrows across completable commands and restores draft", () -> {
        Class<?> variables = Class.forName("local.luke.power.client_fixes.client.text.chat.ChatScreenVariables");
        @SuppressWarnings("unchecked") var history = (List<String>) variables.getField("CHAT_HISTORY").get(null);
        var saved = List.copyOf(history);
        try {
          history.clear(); history.add("//set stone"); history.add("//set rail");
          ChatScreen screen = new ChatScreen(); mc.setScreen(screen); ChatInput input = (ChatInput)screen;
          input.power$type('\0', Keyboard.KEY_UP); check(buffer().text().equals("//set rail"), "first history entry");
          input.power$type('\0', Keyboard.KEY_UP); check(buffer().text().equals("//set stone"), "suggestions stole history up");
          input.power$type('\0', Keyboard.KEY_DOWN); check(buffer().text().equals("//set rail"), "suggestions stole history down");
          input.power$type('\0', Keyboard.KEY_DOWN); check(buffer().text().isEmpty(), "draft not restored");
        } finally { history.clear(); history.addAll(saved); }
      });
      test("chat cursor moves without changing text and Tab preserves following arguments", () -> {
        ChatScreen screen = new ChatScreen(); mc.setScreen(screen); ChatInput input = (ChatInput)screen;
        for (char c : "//set st  tail".toCharArray()) input.power$type(c, 0);
        for (int i = 0; i < 6; i++) input.power$type('\0', Keyboard.KEY_LEFT);
        check(buffer().cursor()==8, "wrong caret: " + buffer().cursor() + " text=" + buffer().text());
        screen.tick(); screen.render(-1,-1,0);
        check(buffer().text().equals("//set st  tail"), "render changed text");
        input.power$type('\t', Keyboard.KEY_TAB);
        check(buffer().text().endsWith("  tail") && buffer().cursor() < buffer().text().length(), "completion overwrote suffix");
      });
      test("spawning defaults on, survives NBT and dimension copies", () -> {
        var fresh = (WorldSpawning)new WorldProperties(173L,"Spawning");
        check(fresh.power$hostileSpawning() && fresh.power$passiveSpawning(), "new world default");
        spawn.power$hostileSpawning(false); spawn.power$passiveSpawning(false);
        var copied = (WorldSpawning)new WorldProperties(new WorldProperties(props.asNbt()));
        check(!copied.power$hostileSpawning() && !copied.power$passiveSpawning(), "rules lost in save/copy");
      });
      test("spawning rows obey cheats and Apply/Cancel without previewing world mutations", () -> {
        spawn.power$hostileSpawning(true); spawn.power$passiveSpawning(true);
        var session = SettingsRegistry.open(mc);
        var h = find(session,"world.hostileSpawning"); var p = find(session,"world.passiveSpawning");
        check(h.label.contains("this world") && p.label.contains("this world"), "scope labels missing");
        check(SettingAccess.visible(session,h) && h.defaultValue.getAsBoolean(), "default/visibility");
        h.parse("false"); p.parse("false"); session.preview();
        check(spawn.power$hostileSpawning() && spawn.power$passiveSpawning(), "preview changed live world");
        session.discard(); check(spawn.power$hostileSpawning(), "Cancel changed rule");
        h.parse("false"); p.parse("false"); session.save(java.nio.file.Path.of("."));
        check(!spawn.power$hostileSpawning() && !spawn.power$passiveSpawning(), "Apply lost rules");
        var hidden = SettingsRegistry.open(mc); find(hidden,"world.cheats").parse("false");
        check(!SettingAccess.visible(hidden,find(hidden,h.id)), "hidden cheat row visible");
      });
      test("natural spawning stops without removing entities or blocking summon", () -> {
        spawn.power$hostileSpawning(false); spawn.power$passiveSpawning(false);
        int count = mc.world.field_198.size();
        for (int i=0;i<20;i++) check(class_567.method_1870(mc.world,true,true)==0,"natural spawn escaped gate");
        check(mc.world.field_198.size()==count,"existing entities removed");
        ChatChecks.submit(mc,"/summon pig ~ ~ ~");
        check(mc.world.field_198.size()>count,"summon blocked");
        for (boolean remote : new boolean[]{false,true}) {
          mc.world.isRemote = remote;
          for (boolean enabled : new boolean[]{false,true}) {
            cheats.power$cheatsEnabled(enabled);
            for (var method : class_567.class.getDeclaredMethods()) if (method.getName().contains("power$hostile") || method.getName().contains("power$passive")) {
              method.setAccessible(true);
              check((boolean)method.invoke(null,true,mc.world,true,true)==(remote || !enabled),"spawning gate remote/cheats");
            }
          }
        }
        mc.world.isRemote=false; cheats.power$cheatsEnabled(true);
      });
      test("WorldEdit rail states survive placement, undo and redo", () -> {
        var e=WorldEditor.editor(mc); e.select(new Region(new Pos(0,110,0),new Pos(2,110,0)));
        ChatChecks.submit(mc,"//set rail[shape=east_west]"); drain();
        for(int x=0;x<3;x++) check(mc.world.getBlockId(x,110,0)==66 && mc.world.method_1778(x,110,0)==1,"rail state lost");
        ChatChecks.submit(mc,"//undo");drain(); ChatChecks.submit(mc,"//redo");drain();
        check(mc.world.method_1778(1,110,0)==1,"redo lost rail shape");
        ChatChecks.submit(mc,"//set redstone_wall_torch[facing=east,lit=false]");drain();
        check(mc.world.getBlockId(1,110,0)==75 && mc.world.method_1778(1,110,0)==1,"torch state lost");
        int history=e.engine.undoSize(); ChatChecks.submit(mc,"//set rail[shape=invalid]");
        check(!e.engine.busy()&&history==e.engine.undoSize(),"invalid state changed world/history");
      });
      test("calculator works with cheats off and changes no blocks", () -> {
        cheats.power$cheatsEnabled(false);
        var messages=ChatChecks.submit(mc,"//calc 2 + 3 * 4");
        check(messages.stream().anyMatch(s->s.contains("14.0")),"calculation failed: "+messages);
      });
      test("facing setting defaults off and Cancel restores previews", () -> {
        var session=SettingsRegistry.open(mc);var facing=find(session,"power_compat:config.addFacingToDebugOverlay");
        check(!facing.defaultValue.getAsBoolean() && facing.page.equals("Interface") && facing.group.equals("Debug information"),"facing default/group");
        facing.parse("true");session.preview();session.discard();
        check(!find(SettingsRegistry.open(mc),facing.id).value.getAsBoolean(),"facing Cancel failed");
      });
    } finally {
      mc.world.isRemote=false;cheats.power$cheatsEnabled(oldCheats);spawn.power$hostileSpawning(hostile);spawn.power$passiveSpawning(passive);mc.setScreen(null);
    }
    log("RELEASE 1.1.0 FAILURES "+failures);
  }
  static ChatBuffer buffer() throws Exception { return (ChatBuffer)Class.forName("local.luke.power.client_fixes.client.text.chat.ChatScreenVariables").getField("editor").get(null); }
  static Setting find(ConfigSession session,String id) { return session.settings().stream().filter(s->s.id.equals(id)).findFirst().orElseThrow(); }
  static void drain() { for(int i=0;WorldEditor.current().engine.busy()&&i<100;i++)WorldEditor.current().engine.tick(1000);check(!WorldEditor.current().engine.busy(),"edit hung"); }
}
