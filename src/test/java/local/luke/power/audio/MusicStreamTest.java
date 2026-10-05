package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import static paulscode.sound.CommandObject.*;

import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;
import paulscode.sound.*;

class MusicStreamTest {
  /** Capture the real SoundSystem API's queued commands without audio threads or hardware. */
  private static final class Commands extends SoundSystem {
    final List<CommandObject> commands = new ArrayList<>();

    Commands() { super(Library.class); }

    @Override protected void init(Class library) {
      commandThread = new CommandThread(this);
    }

    @Override public boolean CommandQueue(CommandObject command) {
      commands.add(command);
      return true;
    }
  }

  @Test void streamsStartOnceWithTheCorrectVolumeBeforeAnyAudioPlays() throws Exception {
    for (String name : List.of("calm1.ogg", "custom.wav", "position.pbseek")) {
      var system = new Commands();
      var url = URI.create("file:/music/" + name).toURL();
      MusicStream.start(system, "BgMusic", url, name, .3f, false);
      assertEquals(List.of(STOP, REMOVE_SOURCE, NEW_SOURCE, SET_VOLUME, PLAY),
          system.commands.stream().map(c -> c.Command).toList());
      var create = system.commands.get(2);
      assertArrayEquals(new boolean[] {true, true, false}, create.boolArgs);
      assertEquals(SoundSystemConfig.ATTENUATION_NONE, create.intArgs[0]);
      assertEquals(url, ((FilenameURL) create.objectArgs[0]).getURL());
      assertEquals(.3f, system.commands.get(3).floatArgs[0]);
      assertTrue(system.commands.stream().allMatch(c -> c.stringArgs[0].equals("BgMusic")));
    }
  }

  @Test void seekingWhilePausedDoesNotStartTheReplacementStreamUntilResumed() throws Exception {
    var system = new Commands();
    MusicStream.start(system, "PowerBetaMusicPreview", URI.create("file:/seek").toURL(),
        "position.pbseek", .6f, true);
    assertEquals(List.of(STOP, REMOVE_SOURCE, NEW_SOURCE, SET_VOLUME),
        system.commands.stream().map(c -> c.Command).toList());
    assertEquals(0, system.commands.get(3).floatArgs[0]);
    system.setVolume("PowerBetaMusicPreview", .6f);
    system.play("PowerBetaMusicPreview");
    assertEquals(1, system.commands.stream().filter(c -> c.Command == PLAY).count());
  }
}
