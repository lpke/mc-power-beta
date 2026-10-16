package local.luke.power.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightMotionTest {
  @Test void framesMatchNativeMovesAndPostMoveDragOnEveryAxis() {
    for (boolean vertical : new boolean[]{false, true})
      for (double speed : new double[]{0, 40, 100, 110, 400})
        for (double sprint : new double[]{1, 1.73, 4})
          for (double drift : new double[]{0, 1, 3, 5}) {
            double velocity = 0;
            for (int tick = 0; tick < 30; tick++) {
              double input = tick < 10 ? 1 : tick < 15 ? -1 : 0;
              // A turn keeps the other horizontal axis driven even with no input on this axis.
              boolean moving = input != 0 || !vertical && tick < 20;
              double factor = moving ? 1 : drift / 5;
              double before = velocity / 20 * factor + input * .05 * speed / 100 * sprint * (vertical ? 3 : .98);
              var frame = FlightMotion.frame(velocity, input, speed, sprint, drift, vertical, moving, .05);
              assertEquals(before, frame.distance(), 1e-10);
              velocity = before * (vertical ? .6 : .91) * 20;
              assertEquals(velocity, frame.velocity(), 1e-9);
            }
          }
  }
  @Test void unevenFramesHaveIdenticalDisplacementAndVelocity() {
    for (double input : new double[]{-1, 0, 1}) for (double drift : new double[]{0, 3, 5})
      for (boolean vertical : new boolean[]{false, true}) {
        var whole = FlightMotion.frame(8, input, 110, 2, drift, vertical, input != 0, .193);
        double distance = 0, velocity = 8;
        for (double dt : new double[]{.001, .1, .017, .05, .025}) {
          var part = FlightMotion.frame(velocity, input, 110, 2, drift, vertical, input != 0, dt);
          distance += part.distance(); velocity = part.velocity();
        }
        assertEquals(whole.distance(), distance, 1e-10);
        assertEquals(whole.velocity(), velocity, 1e-10);
      }
  }
  @Test void modernBaselineAndInstantStopUseTheSameUnits() {
    assertEquals(10.8888888889, FlightMotion.HORIZONTAL_SPEED, 1e-9);
    var horizontal = FlightMotion.frame(0, 1, 100, 1, 5, false, true, 100);
    assertEquals(FlightMotion.HORIZONTAL_SPEED * .91, horizontal.velocity(), 1e-9);
    var vertical = FlightMotion.frame(0, 1, 100, 1, 5, true, true, 100);
    assertEquals(4.5, vertical.velocity(), 1e-9);
    assertEquals(new FlightMotion.Step(0,0), FlightMotion.frame(10,0,110,2,0,false,false,.03));
    assertEquals(new FlightMotion.Step(10,0), FlightMotion.frame(10,1,110,2,3,false,true,0));
    assertEquals(new FlightMotion.Step(0,0), FlightMotion.frame(Double.NaN,1,110,2,3,false,true,.03));
  }
}
