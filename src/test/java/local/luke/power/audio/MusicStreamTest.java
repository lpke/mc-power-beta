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
      assertSame(url, ((FilenameURL) create.objectArgs[0]).getURL(), "native commands must retain the admission token");
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

  @Test void commandThreadPublishesIdentityAndUrlWithoutExposingTheMutableSourceMap() throws Exception {
    SoundSystemConfig.setLogger(new SoundSystemLogger());
    var url = URI.create("file:/music/duplicate.wav").toURL();
    Source first = new Source(true, true, false, "BgMusic", new FilenameURL(url, "duplicate.wav"), null,
        0, 0, 0, SoundSystemConfig.ATTENUATION_NONE, 0, false);
    Source next = new Source(true, true, false, "BgMusic", new FilenameURL(url, "duplicate.wav"), null,
        0, 0, 0, SoundSystemConfig.ATTENUATION_NONE, 0, false);
    try {
      MusicStream.played(first);
      assertSame(first, MusicStream.background().source());
      assertEquals(url.toExternalForm(), MusicStream.background().url());
      assertSame(url, MusicStream.background().request());
      MusicStream.played(next);
      MusicStream.removing(first);
      assertSame(next, MusicStream.background().source());
      MusicStream.removing(next);
      assertNull(MusicStream.background());
    } finally { MusicStream.removing(first); MusicStream.removing(next); }
  }

  @Test void attemptUrlsKeepFileJarAndCustomHandlerSemanticsWithoutOpeningThem() throws Exception {
    for (String address : List.of("file:/music/space%20and%23.wav", "jar:file:/game/music.jar!/music/calm.ogg")) {
      var original = new java.net.URL(address);
      var request = MusicStream.requestUrl(original);
      assertNotSame(original, request);
      assertEquals(original.toExternalForm(), request.toExternalForm());
    }
    var opens = new java.util.concurrent.atomic.AtomicInteger();
    var handler = new java.net.URLStreamHandler() {
      @Override protected java.net.URLConnection openConnection(java.net.URL url) {
        opens.incrementAndGet();
        return new java.net.URLConnection(url) { @Override public void connect() {} };
      }
    };
    var original = new java.net.URL(null, "musicfixture:/stream?id=4#part", handler);
    var request = MusicStream.requestUrl(original);
    assertNotSame(original, request);
    assertEquals(original.toExternalForm(), request.toExternalForm());
    assertEquals(0, opens.get(), "copying a queued URL must not touch its source");
    assertEquals(request, request.openConnection().getURL());
    assertEquals(1, opens.get(), "custom protocol handler must survive the copy");
  }

  private static Source nativeSource(String name, boolean streamed) {
    return new Source(true, streamed, false, name, new FilenameURL("fixture.ogg"), null,
        0, 0, 0, SoundSystemConfig.ATTENUATION_NONE, 0, false);
  }
}
