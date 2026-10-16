package local.luke.power.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightMotionTest {
  @Test void accelerationAndDecelerationMatchTicksAcrossAxesAndFrameIntervals() {
    for (double acceleration : new double[]{0, 1, 3.5, 5})
      for (double deceleration : new double[]{0, 1, 3, 5})
        for (int side = -1; side <= 1; side++) for (int forward = -1; forward <= 1; forward++)
          for (int up = -1; up <= 1; up++) for (double sprint : new double[]{1, 2}) {
            double[] state = {0, 0, 0};
            for (int t = 0; t < 90; t++) {
              int phase = t < 30 ? 1 : t < 60 ? -1 : 0;
              double s = side * phase, f = forward * phase; int v = up * phase;
              var tick = FlightMotion.tick(state[0]/20, state[1]/20, state[2]/20,
                  s, f, v, 37, .055, sprint, acceleration, deceleration);
              var direction = FlightMotion.tick(0,0,0,s,f,v,37,1,1,5,5);
              double[] input = {direction.x()/.98, v, direction.z()/.98};
              double[] moved = {tick.x(),tick.y(),tick.z()};
              for(int axis=0;axis<3;axis++) {
                boolean moving = axis==1 ? v!=0 : s!=0||f!=0;
                double velocity=state[axis], distance=0;
                for(double dt:new double[]{.001,.017,.025,.007}) {
                  var step=FlightMotion.frame(velocity,input[axis],110,sprint,acceleration,
                      deceleration,axis==1,moving,dt);
                  velocity=step.velocity();distance+=step.distance();
                }
                // Native ticks snap tiny velocities; frame integration deliberately stays continuous.
                if(Math.abs(moved[axis])>=.003 || Math.abs(distance)<1e-12)
                  assertEquals(moved[axis],distance,1e-9);
                state[axis]=velocity;
              }
            }
          }
  }

  @Test void accelerationPreservesTerminalSpeedAndLowerLevelsRespondFasterInBothDirections() {
    for (boolean vertical : new boolean[]{false,true}) for(int direction:new int[]{-1,1}) {
      double target=FlightMotion.frame(0,direction,110,2,5,3,vertical,true,100).velocity();
      double previous=Double.POSITIVE_INFINITY;
      for(double level:new double[]{0,1,3.5,5}) {
        var early=FlightMotion.frame(0,direction,110,2,level,3,vertical,true,.05);
        assertTrue(Math.abs(early.velocity())<=previous+1e-10);
        previous=Math.abs(early.velocity());
        assertEquals(target,FlightMotion.frame(0,direction,110,2,level,3,vertical,true,100).velocity(),1e-9);
        assertEquals(-target,FlightMotion.frame(target,-direction,110,2,0,3,vertical,true,.05).velocity(),1e-9);
        assertEquals(0,FlightMotion.frame(target,0,110,2,level,0,vertical,false,.05).velocity());
      }
    }
  }

  @Test void freecamDefaultRestoresEarlierHorizontalPickupWithoutChangingTiming() {
    // Earlier freecam's response rate was 10/s. Compare displacement at 50, 100 and 250 ms.
    for(double seconds:new double[]{.05,.1,.25}) {
      double oldFraction=1-(1-Math.exp(-10*seconds))/(10*seconds);
      var step=FlightMotion.frame(0,1,110,1,3.5,3,false,true,seconds);
      double fraction=step.distance()/(FlightMotion.HORIZONTAL_SPEED*1.1*seconds);
      assertEquals(oldFraction,fraction,.15);
    }
  }

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
