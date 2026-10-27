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
    var saved=new MusicPlaylist("stable","Quiet",List.of(A,MISSING,B));
    var draft=new MusicPresetDraft(saved,List.of(A,B));
    draft.move(B,-2);draft.excludeAll(List.of(A));
    assertEquals(List.of(B,MISSING),draft.tracks());
    assertEquals(List.of(A,MISSING,B),saved.tracks());
    draft.includeAll();assertEquals(List.of(B,MISSING,A),draft.tracks());
  }
  @Test void orderEditAcceptsOnlyTheSameUniqueMembership() {
    var draft=new MusicPresetDraft(new MusicPlaylist("stable","Quiet",List.of(A,B)));
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
    assertDoesNotThrow(() -> new MusicPlaylist("copy", second, List.of()));
  }

  @Test void appendingAnotherPlaylistKeepsExistingOrderAndMissingIds() {
    var saved = new MusicPlaylist("target", "Quiet", List.of(B));
    var source = new MusicPlaylist("source", "Other", List.of(A, B, MISSING));
    var draft = new MusicPresetDraft(saved, List.of(A, B));
    draft.appendTracks(source.tracks());
    draft.appendTracks(source.tracks());
    assertEquals(List.of(B, A, MISSING), draft.tracks());
    assertEquals(List.of(B), saved.tracks());
    assertEquals(List.of(A, B, MISSING), source.tracks());
    assertEquals(List.of(B), draft.original().tracks());
  }

  @Test void invalidAppendDoesNotLeaveAPartlyChangedDraft() {
    var draft = new MusicPresetDraft(new MusicPlaylist("target", "Quiet", List.of(B)));
    assertThrows(IllegalArgumentException.class, () -> draft.appendTracks(List.of(A, "invalid")));
    assertEquals(List.of(B), draft.tracks());
    assertFalse(draft.changed("Quiet"));
  }

  @Test void favoritesKeepTheirReservedIdAndNameWhileEditsStayIsolated() {
    var saved = new MusicPlaylist(MusicPlaylist.FAVORITES, "Favorites", List.of(A, MISSING));
    var draft = new MusicPresetDraft(saved, List.of(A, B));
    assertTrue(draft.favorites());
    assertFalse(draft.creating());
    assertFalse(draft.changed("Another name"));
    draft.include(B, true);
    draft.include(A, false);
    var edited = draft.snapshot("Another name");
    assertEquals(MusicPlaylist.FAVORITES, edited.id());
    assertEquals("Favorites", edited.name());
    assertEquals(List.of(MISSING, B), edited.tracks());
    assertEquals(List.of(A, MISSING), saved.tracks());
  }

  @Test void protectedBetaViewKeepsItsIdentityAndRejectsEveryKindOfEdit() {
    var beta = MusicPlaylist.beta();
    var view = MusicPresetDraft.readOnly(beta);
    assertTrue(view.readOnly());
    assertFalse(view.creating());
    assertFalse(view.changed("Another name"));
    assertEquals(MusicPlaylist.BETA, view.id());
    assertEquals("Beta 1.7.3", view.snapshot("Another name").name());
    assertThrows(IllegalStateException.class, () -> view.appendTracks(List.of(MISSING)));
    assertThrows(IllegalStateException.class, () -> view.include(A, false));
    assertThrows(IllegalStateException.class, view::includeAll);
    assertThrows(IllegalStateException.class, () -> view.excludeAll(List.of(A)));
    assertThrows(IllegalStateException.class, () -> view.move(A, 1));
    var reversed = new java.util.ArrayList<>(beta.tracks());
    java.util.Collections.reverse(reversed);
    assertThrows(IllegalStateException.class, () -> view.order(reversed));
    assertEquals(beta.tracks(), view.tracks());
    assertEquals(beta.tracks(), view.original().tracks());
  }
}
