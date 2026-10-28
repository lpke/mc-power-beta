package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import static paulscode.sound.CommandObject.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import net.minecraft.class_267;
import org.junit.jupiter.api.*;
import paulscode.sound.*;

class MusicTransportTest {
  /** Observe source commands without creating an audio device or sound thread. */
  private static final class Commands extends SoundSystem {
    final List<CommandObject> commands = new ArrayList<>();
    final Set<String> playing = new HashSet<>();
    Commands() { super(Library.class); }
    @Override protected void init(Class library) { commandThread = new CommandThread(this); }
    @Override public boolean playing(String source) { return playing.contains(source); }
    @Override public boolean CommandQueue(CommandObject command) {
      commands.add(command);
      if (command.Command == PLAY) playing.add(command.stringArgs[0]);
      if (command.Command == PAUSE || command.Command == STOP) playing.remove(command.stringArgs[0]);
      return true;
    }
  }

  private record Saved(Object owner, Field field, Object value) {}
  private final List<Saved> saved = new ArrayList<>();
  private AudioSettings settings;
  private MusicProgress background, preview;

  private Field field(Class<?> type, String name) throws Exception {
    Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
  }
  private void retain(Class<?> type, Object owner, String... names) throws Exception {
    for (String name : names) {
      Field field = field(type, name); saved.add(new Saved(owner, field, field.get(owner)));
    }
  }
  private void set(Class<?> type, String name, Object value) throws Exception { field(type, name).set(null, value); }

  @BeforeEach void isolatePlayback() throws Exception {
    retain(AudioController.class, null, "client", "paused", "currentMusic", "menuMusic", "historyIndex", "rotationConfig");
    retain(MusicPreview.class, null, "track", "entry", "paused", "playing", "started");
    retain(AudioConfig.class, null, "current");
    background = (MusicProgress) field(AudioController.class, "progress").get(null);
    preview = (MusicProgress) field(MusicPreview.class, "progress").get(null);
    retain(MusicProgress.class, background, "seconds", "last", "advancing");
    retain(MusicProgress.class, preview, "seconds", "last", "advancing");
    settings = new AudioSettings();
    set(AudioConfig.class, "current", settings);
    set(AudioController.class, "client", null);
    set(AudioController.class, "rotationConfig", settings);
    set(AudioController.class, "paused", false);
    set(AudioController.class, "currentMusic", "music:calm1.ogg");
    set(AudioController.class, "menuMusic", "");
    set(AudioController.class, "historyIndex", 5);
    set(MusicPreview.class, "track", "");
    set(MusicPreview.class, "paused", false);
    background.reset(42, 0); preview.reset(13, 0);
  }

  @AfterEach void restorePlayback() throws Exception {
    MusicSeeking.cancel();
    for (int i = saved.size() - 1; i >= 0; i--) {
      Saved value = saved.get(i); value.field.set(value.owner, value.value);
    }
  }

  @Test void previousUsesTheVisibleSongAndStrictTenSecondBoundary() throws Exception {
    assertTrue(AudioController.previousRestartsCurrent());
    set(MusicPreview.class, "track", "music:calm2.ogg");
    preview.reset(10, 0);
    assertFalse(AudioController.previousRestartsCurrent(), "hidden background time must not restart an audition");
    preview.reset(10.001, 0);
    assertTrue(AudioController.previousRestartsCurrent());
    set(MusicPreview.class, "paused", true);
    assertTrue(AudioController.previousRestartsCurrent(), "paused audition keeps its position");
    set(MusicPreview.class, "track", "");
    set(AudioController.class, "currentMusic", "");
    assertFalse(AudioController.previousRestartsCurrent(), "a gap must replay history even with retained clock data");
  }

  @Test void nativeMutePausesWithoutReplacingTheSourceOrLosingSongPosition() throws Exception {
    Commands system = new Commands();
    system.playing.add("BgMusic");
    AudioController.muteMusic(system);
    assertTrue(AudioController.backgroundPaused());
    assertEquals("music:calm1.ogg", AudioController.currentTrackId());
    assertEquals(42, AudioController.position());
    assertEquals(5, field(AudioController.class, "historyIndex").get(null));
    assertEquals(List.of(SET_VOLUME, PAUSE, SET_VOLUME, PAUSE),
        system.commands.stream().map(c -> c.Command).toList());
    assertTrue(system.commands.stream().filter(c -> c.Command == SET_VOLUME).allMatch(c -> c.floatArgs[0] == 0));
    // A Play already queued on the native thread can arrive after the first pause.
    system.playing.add("BgMusic"); system.commands.clear();
    AudioController.muteMusic(system);
    assertEquals(List.of(SET_VOLUME, PAUSE), system.commands.stream().map(c -> c.Command).toList());
    assertEquals(42, background.seconds());
  }

  @Test void mutePausesBothOwnersAndUnmuteRequiresAnExplicitAuditionResume() throws Exception {
    Commands system = new Commands();
    set(MusicPreview.class, "track", "music:calm2.ogg");
    system.playing.addAll(List.of("BgMusic", MusicPreview.SOURCE, "streaming"));
    settings.master = 0;
    AudioController.muteMusic(system);
    assertTrue(AudioController.backgroundPaused()); assertTrue(MusicPreview.paused());
    assertEquals(42, background.seconds()); assertEquals(13, preview.seconds());
    assertEquals("Music muted", AudioController.status());
    assertFalse(AudioController.musicPlaying());
    assertTrue(system.playing("streaming"), "Music mute must not pause a jukebox");
    system.commands.clear();
    MusicPreview.resume(system);
    assertTrue(system.commands.isEmpty(), "resume while muted must not queue Play");
    settings.master = 100;
    assertTrue(MusicPreview.paused()); assertTrue(AudioController.backgroundPaused());
    assertTrue(AudioController.status().startsWith("Paused:"));
    MusicPreview.resume(system);
    assertFalse(MusicPreview.paused()); assertTrue(AudioController.backgroundPaused());
    assertEquals(List.of(SET_VOLUME, PLAY), system.commands.stream().map(c -> c.Command).toList());
    assertTrue(system.commands.stream().allMatch(c -> c.stringArgs[0].equals(MusicPreview.SOURCE)));
  }

  @Test void restartingAnAuditionLeavesTheBackgroundAndHistoryUntouched() throws Exception {
    Commands system = new Commands();
    set(MusicPreview.class, "track", "music:calm2.ogg");
    class_267 song = new class_267("calm2.ogg", URI.create("file:/music/calm2.ogg").toURL());
    set(MusicPreview.class, "entry", song);
    MusicPreview.restart(system);
    assertEquals(0, preview.seconds()); assertEquals(42, background.seconds());
    assertEquals(5, field(AudioController.class, "historyIndex").get(null));
    assertEquals(List.of(STOP, REMOVE_SOURCE, NEW_SOURCE, SET_VOLUME, PLAY),
        system.commands.stream().map(c -> c.Command).toList());
    assertTrue(system.commands.stream().allMatch(c -> c.stringArgs[0].equals(MusicPreview.SOURCE)));
    system.commands.clear(); settings.master = 0;
    MusicPreview.restart(system);
    assertTrue(MusicPreview.paused());
    assertEquals(List.of(STOP, REMOVE_SOURCE, NEW_SOURCE, SET_VOLUME),
        system.commands.stream().map(c -> c.Command).toList());
  }
}
