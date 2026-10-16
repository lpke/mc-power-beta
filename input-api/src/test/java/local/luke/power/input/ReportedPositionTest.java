package local.luke.power.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReportedPositionTest {
  @Test void onlyTheCurrentOwnerCanUseTheReportedCameraPose() {
    Object player = new Object(), stranger = new Object();
    var reported = new ReportedPosition.Pose(-1.2, 65.5, 3, 45, -30);
    ReportedPosition.register(() -> player, () -> reported);
    assertSame(reported, ReportedPosition.current(player, 0, 0, 0, 0, 0));
    assertEquals(new ReportedPosition.Pose(1,2,3,4,5), ReportedPosition.current(stranger,1,2,3,4,5));
    ReportedPosition.register(() -> null, () -> reported);
    assertEquals(new ReportedPosition.Pose(1,2,3,4,5), ReportedPosition.current(player,1,2,3,4,5));
    ReportedPosition.register(() -> player, () -> new ReportedPosition.Pose(0,Double.NaN,0,0,0));
    assertEquals(new ReportedPosition.Pose(1,2,3,4,5), ReportedPosition.current(player,1,2,3,4,5));
    ReportedPosition.register(() -> null, () -> null);
  }
}
