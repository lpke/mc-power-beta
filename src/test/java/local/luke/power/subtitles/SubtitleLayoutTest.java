package local.luke.power.subtitles;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import local.luke.power.status.StatusLayout;
import local.luke.power.status.StatusSettings;
import org.junit.jupiter.api.Test;

class SubtitleLayoutTest {
  private int measure(String text) { return text.length() * 6; }
  @Test void defaultPanelMatchesModernRightEdgeBottomGapAndFirstRowCenter() {
    var layout = SubtitleLayout.preview(new SubtitleSettings(), 320, 240, this::measure);
    assertEquals(319, layout.x() + layout.width()); assertEquals(210, layout.y() + layout.height());
    assertEquals(201, layout.y() + layout.lines().get(0).textY());
    assertEquals(191, layout.y() + layout.lines().get(1).textY());
    assertEquals(0xcc000000, layout.background());
    assertEquals('<', layout.lines().get(0).arrow()); assertEquals('>', layout.lines().get(2).arrow());
  }
  @Test void nativeIntegerHalfWidthsAndOddCaptionWidthsUseSharedGeometry() {
    var settings = new SubtitleSettings();
    var layout = SubtitleLayout.at(settings, List.of(new SubtitleState.Visible("odd", "odd", '<', 255)), 320, 240, String::length);
    assertEquals(3, layout.halfWidth()); assertEquals(8, layout.width()); assertEquals(311, layout.x());
    assertEquals(3, layout.lines().get(0).textX());
  }
  @Test void everyAnchorAndScaleKeepsPreviewBoundsInsideNarrowScreenWithoutChangingArrows() {
    for (int anchor = 0; anchor < 9; anchor++) for (double scale : new double[]{.5, 1, 3}) {
      var settings = new SubtitleSettings(); settings.position = anchor; settings.textSize = scale;
      settings.offsetX = anchor % 2 == 0 ? -4096 : 4096; settings.offsetY = anchor % 2 == 0 ? 4096 : -4096;
      var layout = SubtitleLayout.preview(settings, 160, 90, this::measure);
      assertTrue(layout.x() >= 0); assertTrue(layout.y() >= 0);
      assertTrue(layout.x() + layout.width() <= 160); assertTrue(layout.y() + layout.height() <= 90);
      assertEquals('<', layout.lines().get(0).arrow());
    }
  }
  @Test void previewsNeverNeedLiveSoundStateOrChangeTheirSettingsDraft() {
    var settings = new SubtitleSettings(); settings.offsetX = -77;
    var first = SubtitleLayout.preview(settings, 320, 240, this::measure);
    assertEquals(first, SubtitleLayout.preview(settings, 320, 240, this::measure)); assertEquals(-77, settings.offsetX);
  }
  @Test void defaultCaptionsClearTheActualThreeRowTweakBlockWithoutMovingSettingsOrText() {
    var settings = new SubtitleSettings();
    var original = SubtitleLayout.preview(settings, 320, 240, this::measure);
    var tweaks = StatusLayout.at(320, 240, 100, 3, new StatusSettings()).bounds(100, 3);
    var moved = original.avoidActiveTweaks(settings, tweaks);
    assertEquals(tweaks.y() - 2, moved.y() + moved.height());
    assertEquals(original.x(), moved.x()); assertEquals(original.width(), moved.width());
    assertSame(original.lines(), moved.lines()); assertEquals(original.scale(), moved.scale());
    assertEquals(8, settings.position); assertEquals(-1, settings.offsetX); assertEquals(-30, settings.offsetY);
    assertEquals(original.background(), moved.background()); assertEquals(original.halfWidth(), moved.halfWidth());
  }
  @Test void absentHiddenShortOrHorizontallySeparateTweaksDoNotDisplaceCaptions() {
    var settings = new SubtitleSettings();
    var original = SubtitleLayout.preview(settings, 320, 240, this::measure);
    assertSame(original, original.avoidActiveTweaks(settings, null));
    assertSame(original, original.avoidActiveTweaks(settings, new StatusLayout.Bounds(220, 120, 100, 0)));
    assertSame(original, original.avoidActiveTweaks(settings,
        StatusLayout.at(320, 240, 100, 2, new StatusSettings()).bounds(100, 2)));
    assertSame(original, original.avoidActiveTweaks(settings, new StatusLayout.Bounds(2, 180, 100, 30)));
  }
  @Test void customizedAnchorOrEitherOffsetKeepsExactPlacementEvenWhenOverlapping() {
    for (int changed = 0; changed < 3; changed++) {
      var settings = new SubtitleSettings();
      if (changed == 0) settings.position = 5;
      if (changed == 1) settings.offsetX = -2;
      if (changed == 2) settings.offsetY = -31;
      var layout = SubtitleLayout.preview(settings, 320, 240, this::measure);
      assertSame(layout, layout.avoidActiveTweaks(settings, new StatusLayout.Bounds(0, 0, 320, 240)));
    }
  }
  @Test void liveAndPreviewUseTheSameScaledAvoidanceAndRemainInsideTheScreen() {
    var settings = new SubtitleSettings(); settings.textSize = 2;
    var cues = List.of(new SubtitleState.Visible("preview.steps", "Footsteps", '<', 255),
        new SubtitleState.Visible("preview.water", "Water flows", '\0', 210),
        new SubtitleState.Visible("preview.zombie", "Zombie groans", '>', 160));
    var tweaks = StatusLayout.at(320, 240, 100, 12, new StatusSettings()).bounds(100, 12);
    var live = SubtitleLayout.at(settings, cues, 320, 240, this::measure).avoidActiveTweaks(settings, tweaks);
    var preview = SubtitleLayout.preview(settings, 320, 240, this::measure).avoidActiveTweaks(settings, tweaks);
    assertEquals(live, preview); assertEquals(tweaks.y() - 2, live.y() + live.height());
    var crowded = live.avoidActiveTweaks(settings, new StatusLayout.Bounds(0, 5, 320, 235));
    assertEquals(0, crowded.y()); assertTrue(crowded.y() + crowded.height() <= 240);
  }
}
