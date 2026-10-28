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

  @Test void idleMusicNeverGetsTheNativePausedFlagThatWouldSkipItsFirstPreload() {
    SoundSystemConfig.setLogger(new SoundSystemLogger());
    for (String name : List.of("BgMusic", "PowerBetaMenu", "PowerBetaMusicPreview")) {
      Source source = nativeSource(name, true);
      assertTrue(source.stopped()); assertFalse(source.paused());
      assertFalse(MusicStream.canPause(source));
      if (MusicStream.canPause(source)) source.pause();
      assertTrue(source.stopped()); assertFalse(source.paused());
      Channel channel = new Channel(SoundSystemConfig.TYPE_STREAMING);
      // Native command order: a queued Play before Pause has already attached its channel.
      source.play(channel);
      assertTrue(MusicStream.canPause(source)); source.pause();
      assertSame(channel, source.channel);
      assertFalse(source.stopped()); assertTrue(source.paused());
      assertTrue(MusicStream.canPause(source), "an initialized paused stream keeps native resume behavior");
      source.play(channel);
      assertFalse(source.stopped()); assertFalse(source.paused());
    }
  }

  @Test void stoppedJukeboxesOtherModsAndNonStreamingSourcesKeepNativePauseBehavior() {
    SoundSystemConfig.setLogger(new SoundSystemLogger());
    for (Source source : List.of(nativeSource("streaming", true), nativeSource("another-mod", true), nativeSource("BgMusic", false))) {
      Channel channel = new Channel(SoundSystemConfig.TYPE_STREAMING);
      source.play(channel); source.stop();
      assertTrue(source.stopped()); assertFalse(source.paused());
      assertTrue(MusicStream.canPause(source)); source.pause();
      assertTrue(source.paused()); assertTrue(source.stopped()); assertSame(channel, source.channel);
    }
  }

  private static Source nativeSource(String name, boolean streamed) {
    return new Source(true, streamed, false, name, new FilenameURL("fixture.ogg"), null,
        0, 0, 0, SoundSystemConfig.ATTENUATION_NONE, 0, false);
  }
}
