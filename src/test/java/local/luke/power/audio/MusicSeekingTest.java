package local.luke.power.audio;

import java.net.URI;
import java.security.MessageDigest;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sound.sampled.AudioFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import paulscode.sound.*;
import paulscode.sound.codecs.CodecJOrbis;

class MusicSeekingTest {
  private static final String BACKGROUND = "BgMusic", PREVIEW = "PowerBetaMusicPreview";

  private static final class TrackedCodec implements ICodec {
    final AtomicInteger cleanups = new AtomicInteger();
    final CountDownLatch cleaned = new CountDownLatch(1);
    volatile String cleanupThread;
    public void reverseByteOrder(boolean reverse) {}
    public boolean initialize(java.net.URL url) { return true; }
    public boolean initialized() { return cleanups.get() == 0; }
    public SoundBuffer read() { return null; }
    public SoundBuffer readAll() { return null; }
    public boolean endOfStream() { return true; }
    public AudioFormat getAudioFormat() { return new AudioFormat(44100, 16, 2, true, false); }
    public void cleanup() { cleanupThread = Thread.currentThread().getName(); cleanups.incrementAndGet(); cleaned.countDown(); }
    void awaitCleanup() throws Exception {
      assertTrue(cleaned.await(3, TimeUnit.SECONDS), "unclaimed decoder was not released");
      assertEquals(1, cleanups.get(), "decoder cleanup must have exactly one owner");
    }
  }

  private static final class Commands extends SoundSystem {
    Commands() { super(Library.class); }
    @Override protected void init(Class library) { commandThread = new CommandThread(this); }
    @Override public boolean CommandQueue(CommandObject command) { return true; }
  }

  private static MusicSeeking.Prepared prepared(String source, TrackedCodec codec) {
    return new MusicSeeking.Prepared("music:owned.ogg", source, 45, codec,
        new SoundBuffer(new byte[] {1, 2, 3, 4}, codec.getAudioFormat()));
  }

  @Test void retainedPausedBackgroundSurvivesPreviewSeekRestartAndStop() throws Exception {
    var background = new TrackedCodec(); var audition = new TrackedCodec();
    var backgroundUrl = MusicSeeking.publish(prepared(BACKGROUND, background));
    var system = new Commands(); var resumed = new SeekCodec();
    try {
      MusicStream.start(system, BACKGROUND, backgroundUrl, "position.pbseek", .2f, true);
      MusicSeeking.cancelPending();
      var previewUrl = MusicSeeking.publish(prepared(PREVIEW, audition));
      MusicStream.start(system, PREVIEW, previewUrl, "position.pbseek", .2f, true);
      MusicSeeking.cancelPending();
      MusicStream.start(system, PREVIEW, URI.create("file:/music/calm2.ogg").toURL(), "calm2.ogg", .2f, false);
      MusicStream.stop(system, PREVIEW);
      audition.awaitCleanup();
      assertEquals("Power Beta music seek", audition.cleanupThread);
      assertEquals(0, background.cleanups.get(), "audition actions discarded the retained background handoff");
      assertTrue(resumed.initialize(backgroundUrl), "paused background handoff could not be claimed on manual resume");
      assertArrayEquals(new byte[] {1, 2, 3, 4}, resumed.read().audioData);
      MusicStream.stop(system, BACKGROUND);
      assertEquals(0, background.cleanups.get(), "claimed decoder belongs to native source cleanup");
      resumed.cleanup(); resumed.cleanup(); background.awaitCleanup();
    } finally { resumed.cleanup(); MusicSeeking.cancel(); }
  }

  @Test void sameSourceReplacementRetiresOnlyObsoleteTokensAndPreservesItsIncomingUrl() throws Exception {
    var old = new TrackedCodec(); var current = new TrackedCodec(); var audition = new TrackedCodec();
    var oldUrl = MusicSeeking.publish(prepared(BACKGROUND, old));
    var currentUrl = MusicSeeking.publish(prepared(BACKGROUND, current));
    var previewUrl = MusicSeeking.publish(prepared(PREVIEW, audition));
    var claimedPreview = new SeekCodec(); var retired = new SeekCodec();
    try {
      old.awaitCleanup(); assertFalse(retired.initialize(oldUrl));
      var system = new Commands();
      MusicStream.start(system, BACKGROUND, currentUrl, "position.pbseek", .2f, true);
      assertEquals(0, current.cleanups.get(), "replacement discarded its newly published incoming URL");
      MusicStream.start(system, BACKGROUND, URI.create("file:/music/calm1.ogg").toURL(), "calm1.ogg", .2f, false);
      current.awaitCleanup(); assertFalse(retired.initialize(currentUrl));
      assertTrue(claimedPreview.initialize(previewUrl), "replacing background discarded a different source's handoff");
      MusicStream.start(system, PREVIEW, URI.create("file:/music/calm2.ogg").toURL(), "calm2.ogg", .2f, false);
      assertEquals(0, audition.cleanups.get(), "replacement must leave a claimed delegate to native source cleanup");
      MusicSeeking.cancel(); assertEquals(0, audition.cleanups.get(), "global teardown cannot clean a claimed delegate");
      claimedPreview.cleanup(); audition.awaitCleanup();
    } finally { claimedPreview.cleanup(); retired.cleanup(); MusicSeeking.cancel(); }
  }

  @Test void requestingAnotherSeekPreservesPublishedStreamsAndSourceStopKeepsOtherPendingWork() throws Exception {
    SoundSystemConfig.setLogger(new SoundSystemLogger()); SoundSystemConfig.setCodec("ogg", CodecJOrbis.class);
    var background = new TrackedCodec();
    var backgroundUrl = MusicSeeking.publish(prepared(BACKGROUND, background));
    var claimed = new SeekCodec();
    try {
      var url = getClass().getResource("/assets/powerbeta/music/calm1.ogg");
      MusicSeeking.request("music:calm1.ogg", PREVIEW, url, "calm1.ogg", 45);
      assertTrue(claimed.initialize(backgroundUrl), "a preview seek invalidated an accepted paused background seek");
      MusicSeeking.discardSource(BACKGROUND);
      assertTrue(MusicSeeking.busy(), "stopping background cancelled the audition's worker");
      assertEquals(45, MusicSeeking.position("music:calm1.ogg", PREVIEW, 3));
      MusicSeeking.cancelPending(); assertFalse(MusicSeeking.busy());
      assertEquals(0, background.cleanups.get());
    } finally { claimed.cleanup(); MusicSeeking.cancel(); }
    background.awaitCleanup();
  }

  @Test void cancellingAnAlreadyPreparedRequestCleansItsDecoderOffTheCallingThread() throws Exception {
    var codec = new TrackedCodec();
    var pending = MusicSeeking.class.getDeclaredField("pending"); pending.setAccessible(true);
    try {
      pending.set(null, CompletableFuture.completedFuture(prepared(BACKGROUND, codec)));
      MusicSeeking.cancelPending(); codec.awaitCleanup();
      assertEquals("Power Beta music seek", codec.cleanupThread);
    } finally { MusicSeeking.cancel(); }
  }

  @Test void concurrentClaimAndSourceDisposalTransferCleanupOwnershipExactlyOnce() throws Exception {
    var executor = Executors.newFixedThreadPool(2);
    try {
      for (int attempt = 0; attempt < 32; attempt++) {
        var codec = new TrackedCodec(); var wrapper = new SeekCodec();
        var url = MusicSeeking.publish(prepared(BACKGROUND, codec)); var start = new CountDownLatch(1);
        var claim = executor.submit(() -> { start.await(); return wrapper.initialize(url); });
        var discard = executor.submit(() -> { start.await(); MusicSeeking.discardSource(BACKGROUND); return null; });
        start.countDown(); boolean owned = claim.get(3, TimeUnit.SECONDS); discard.get(3, TimeUnit.SECONDS);
        if (owned) { assertEquals(0, codec.cleanups.get()); wrapper.cleanup(); }
        codec.awaitCleanup(); wrapper.cleanup();
        assertEquals(1, codec.cleanups.get());
      }
    } finally { executor.shutdownNow(); MusicSeeking.cancel(); }
  }

  @Test void pendingSeekKeepsTheRequestedPositionUntilCommittedOrCancelled() {
    SoundSystemConfig.setLogger(new SoundSystemLogger());
    SoundSystemConfig.setCodec("ogg",CodecJOrbis.class);
    var url=getClass().getResource("/assets/powerbeta/music/calm1.ogg");
    try {
      MusicSeeking.request("music:calm1.ogg","BgMusic",url,"calm1.ogg",120);
      assertEquals(120,MusicSeeking.position("music:calm1.ogg","BgMusic",3));
      assertEquals(3,MusicSeeking.position("music:calm1.ogg","PowerBetaMusicPreview",3));
      assertEquals(3,MusicSeeking.position("music:other.ogg","BgMusic",3));
      MusicSeeking.request("music:calm1.ogg","BgMusic",url,"calm1.ogg",45);
      assertEquals(45,MusicSeeking.position("music:calm1.ogg","BgMusic",3));
      MusicSeeking.cancel();
      assertEquals(3,MusicSeeking.position("music:calm1.ogg","BgMusic",3));
    } finally { MusicSeeking.cancel(); }
  }

  @Test void repeatedInitializationPreservesTheExactRemainingPcm() throws Exception {
    SoundSystemConfig.setLogger(new SoundSystemLogger());
    SoundSystemConfig.setCodec("ogg",CodecJOrbis.class);
    var url=getClass().getResource("/assets/powerbeta/music/calm1.ogg");
    assertNotNull(url);
    assertTrue(MusicSeeking.oggDuration(url)>250);
    double position=123.45;
    MusicSeeking.request("music:calm1.ogg","BgMusic",url,"calm1.ogg",position);
    MusicSeeking.Prepared prepared=null;
    long deadline=System.nanoTime()+20_000_000_000L;
    while(prepared==null&&System.nanoTime()<deadline){prepared=MusicSeeking.take();if(prepared==null)Thread.sleep(10);}
    assertNotNull(prepared);
    assertEquals(position,prepared.seconds(),.001);
    var handoff=MusicSeeking.publish(prepared);
    var seek=new SeekCodec();
    try {
      assertTrue(seek.initialize(handoff));assertTrue(seek.initialize(handoff));
      var actual=MessageDigest.getInstance("SHA-256");long count=0;
      while(!seek.endOfStream()){var b=seek.read();if(b==null)break;actual.update(b.audioData);count+=b.audioData.length;}
      var original=new CodecJOrbis();
      var expected=MessageDigest.getInstance("SHA-256");long expectedCount=0;
      try {
        assertTrue(original.initialize(url));var f=original.getAudioFormat();
        long skip=(long)(position*f.getFrameRate())*f.getFrameSize();
        while(!original.endOfStream()){
          var b=original.read();if(b==null)break;
          int offset=(int)Math.min(skip,b.audioData.length);skip-=offset;
          expected.update(b.audioData,offset,b.audioData.length-offset);expectedCount+=b.audioData.length-offset;
        }
      }finally{original.cleanup();}
      assertEquals(expectedCount,count);assertArrayEquals(expected.digest(),actual.digest());
    }finally{seek.cleanup();MusicSeeking.cancel();}
    assertFalse(new SeekCodec().initialize(handoff),"handoff reused by a second decoder");
  }
}
