package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class MusicQueueAdmissionTest {
  private static final String URL = "file:/music/requested.wav";
  private static final List<String> QUEUE = List.of("music:requested", "music:requested");

  @Test void oldPlayingSourceCannotAcknowledgeEvenTheSameRequestedSong() {
    var admission = new MusicQueueAdmission();
    Object old = new Object(), replacement = new Object();
    admission.begin(QUEUE, 4, old, URL);
    assertEquals(MusicQueueAdmission.Result.WAITING, admission.observe(QUEUE, 4, old, URL, true, 10));
    assertEquals(MusicQueueAdmission.Result.WAITING, admission.observe(QUEUE, 4, replacement, "file:/other.wav", true, 20));
    assertEquals(MusicQueueAdmission.Result.WAITING, admission.observe(QUEUE, 4, replacement, URL, false, 30));
    assertEquals(MusicQueueAdmission.Result.PLAYING, admission.observe(QUEUE, 4, replacement, URL, true, 40));
    assertEquals(MusicQueueAdmission.Result.NONE, admission.observe(QUEUE, 4, replacement, URL, true, 50));
  }

  @Test void failedStartupKeepsItsRequestBlockedUntilAnEditOrNewScan() {
    var admission = new MusicQueueAdmission();
    admission.begin(QUEUE, 4, null, URL);
    assertEquals(MusicQueueAdmission.Result.WAITING, admission.observe(QUEUE, 4, null, "", false, 4_999_999_999L));
    assertEquals(MusicQueueAdmission.Result.FAILED, admission.observe(QUEUE, 4, null, "", false, 5_000_000_000L));
    assertTrue(admission.blocked(QUEUE, 4));
    admission.cancel();
    assertTrue(admission.blocked(QUEUE, 4), "manual transport must not restart a failed queued decoder");
    assertFalse(admission.blocked(QUEUE, 5));
    admission.reject(QUEUE, 5);
    assertFalse(admission.blocked(List.copyOf(new ArrayList<>(QUEUE)), 5), "same-valued edit is a new request snapshot");
  }

  @Test void anEarlierUnpublishedSameSongCannotClaimTheNewAttempt() {
    var admission = new MusicQueueAdmission();
    Object earlier = new Object(), replacement = new Object();
    String earlierRequest = new String(URL);
    admission.begin(QUEUE, 4, null, URL);
    assertEquals(URL, earlierRequest);
    assertEquals(MusicQueueAdmission.Result.WAITING,
        admission.observe(QUEUE, 4, earlier, earlierRequest, true, 10));
    assertEquals(MusicQueueAdmission.Result.PLAYING,
        admission.observe(QUEUE, 4, replacement, URL, true, 20));
  }

  @Test void editedRowsAndRescannedUrlsCannotBeConsumedByAnEarlierPlayback() {
    for (boolean scan : List.of(false, true)) {
      var admission = new MusicQueueAdmission();
      admission.begin(QUEUE, 4, null, URL);
      List<String> edited = scan ? QUEUE : List.copyOf(new ArrayList<>(QUEUE));
      assertEquals(MusicQueueAdmission.Result.NONE,
          admission.observe(edited, scan ? 5 : 4, new Object(), URL, true, 10));
      assertFalse(admission.blocked(edited, scan ? 5 : 4));
    }
  }

  @Test void cancellationDoesNotLetALateDecoderConsumeTheQueue() {
    var admission = new MusicQueueAdmission();
    admission.begin(QUEUE, 4, null, URL);
    admission.cancel();
    assertEquals(MusicQueueAdmission.Result.NONE, admission.observe(QUEUE, 4, new Object(), URL, true, 1));
  }

  @Test void pendingOwnershipSurvivesPauseUntilTheResumeClockStarts() {
    var admission = new MusicQueueAdmission();
    admission.begin(QUEUE, 4, null, URL);
    // Paused controller ticks do not observe startup timeouts. Resume resets its clock.
    assertTrue(admission.pending(QUEUE, 4));
    assertTrue(admission.blocked(QUEUE, 4));
    assertEquals(MusicQueueAdmission.Result.PLAYING, admission.observe(QUEUE, 4, new Object(), URL, true, 0));
  }
}
