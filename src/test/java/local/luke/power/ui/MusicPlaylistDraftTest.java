package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import local.luke.power.audio.MusicPlaylist;
import org.junit.jupiter.api.Test;

class MusicPlaylistDraftTest {
  private static final String A="music:calm1.ogg", B="music:calm2.ogg", MISSING="music:custom/missing";
  @Test void aNewPlaylistStartsEmptyAndImportAvailabilityDoesNotAdmitTracks() {
    var draft=new MusicPresetDraft(null,List.of(A,B));
    assertTrue(draft.tracks().isEmpty());
    draft.include(B,true);draft.include(A,true);draft.include(B,true);
    assertEquals(List.of(B,A),draft.snapshot("Selected").tracks());
  }
  @Test void editsAndReorderNeverMutateSavedSnapshotOrDiscardUnavailableEntries() {
    var saved=new MusicPlaylist("stable","Quiet",List.of(A,MISSING,B),Map.of("Alpha",40));
    var draft=new MusicPresetDraft(saved,List.of(A,B));
    draft.move(B,-2);draft.volume("Alpha",20);draft.excludeAll(List.of(A));
    assertEquals(List.of(B,MISSING),draft.tracks());
    assertEquals(List.of(A,MISSING,B),saved.tracks());assertEquals(40,saved.groupVolumes().get("Alpha"));
    draft.includeAll();assertEquals(List.of(B,MISSING,A),draft.tracks());
  }
  @Test void orderEditAcceptsOnlyTheSameUniqueMembership() {
    var draft=new MusicPresetDraft(new MusicPlaylist("stable","Quiet",List.of(A,B),Map.of()));
    assertThrows(IllegalArgumentException.class,()->draft.order(List.of(A)));
    assertThrows(IllegalArgumentException.class,()->draft.order(List.of(A,A)));
    assertEquals(List.of(A,B),draft.tracks());
    draft.order(List.of(B,A));assertTrue(draft.changed("Quiet"));
    assertEquals(List.of(B,A),draft.snapshot("Quiet").tracks());
  }
  @Test void copyingBetaProducesAnIndependentDraftWithItsOriginalTwelveTracks() {
    var beta = MusicPlaylist.beta();
    var draft = new MusicPresetDraft(beta, beta.tracks());
    assertTrue(draft.creating());
    assertTrue(draft.changed(draft.name()));
    var copy = draft.snapshot("My Beta");
    assertNotEquals(MusicPlaylist.BETA, copy.id());
    assertEquals(beta.tracks(), copy.tracks());
    draft.include(beta.tracks().get(0), false);
    assertEquals(12, beta.tracks().size());
    assertEquals(11, draft.tracks().size());
  }

  @Test void copiedNamesFitTheLimitAndAvoidCaseInsensitiveCollisions() {
    String original = "A".repeat(80);
    String first = MusicPresetDraft.copyName(original, List.of(original));
    assertEquals("A".repeat(75) + " copy", first);
    String second = MusicPresetDraft.copyName(original, List.of(original, first.toUpperCase(Locale.ROOT)));
    assertEquals("A".repeat(73) + " copy 2", second);
    assertEquals(80, second.length());
    assertDoesNotThrow(() -> new MusicPlaylist("copy", second, List.of(), Map.of()));
  }
}
