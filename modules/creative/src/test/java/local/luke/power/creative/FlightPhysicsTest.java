package local.luke.power.creative;

import static org.junit.jupiter.api.Assertions.*;

import local.luke.power.creative.flight.FlightPhysics;
import local.luke.power.creative.flight.FlightPhysics.Motion;
import org.junit.jupiter.api.Test;

class FlightPhysicsTest {
  @Test void accelerationAndReleaseWorkForAllDirectionsAndRetainTerminalSpeed() {
    for(double acceleration:new double[]{0,1,3.5,5})
      for(int side=-1;side<=1;side++)for(int forward=-1;forward<=1;forward++)for(int up=-1;up<=1;up++) {
        Motion velocity=new Motion(0,0,0),move=velocity;
        for(int i=0;i<300;i++) {
          move=FlightPhysics.step(velocity,side,forward,up,71,.055,2,acceleration,5);
          velocity=move.drag();
        }
        Motion instant=FlightPhysics.step(new Motion(0,0,0),side,forward,up,71,.055,2,0,5);
        assertEquals(instant.x(),move.x(),1e-8);
        assertEquals(instant.y(),move.y(),1e-8);
        assertEquals(instant.z(),move.z(),1e-8);
        assertEquals(new Motion(0,0,0),FlightPhysics.step(velocity,0,0,0,71,.055,2,acceleration,0));
        Motion opposite=FlightPhysics.step(velocity,-side,-forward,-up,71,.055,2,0,5);
        assertEquals(-move.x(),opposite.x(),1e-8);
        assertEquals(-move.y(),opposite.y(),1e-8);
        assertEquals(-move.z(),opposite.z(),1e-8);
      }
  }
  @Test void sprintScalesEveryDirectionIncludingVerticalAndSpectatorSpeed() {
    for (double speed : new double[]{.05, .15})
      for (int side=-1;side<=1;side++) for(int forward=-1;forward<=1;forward++) for(int vertical=-1;vertical<=1;vertical++) {
        Motion zero=new Motion(0,0,0);
        Motion normal=FlightPhysics.step(zero,side,forward,vertical,30,speed,1,5);
        Motion fast=FlightPhysics.step(zero,side,forward,vertical,30,speed,3,5);
        assertEquals(normal.x()*3,fast.x(),1e-10);
        assertEquals(normal.y()*3,fast.y(),1e-10);
        assertEquals(normal.z()*3,fast.z(),1e-10);
      }
  }
  @Test
  void modernHorizontalAndVerticalTerminalSpeeds() {
    Motion v = new Motion(0, 0, 0), move = v;
    for (int i = 0; i < 300; i++) {
      move = FlightPhysics.step(v, 0, 1, 1, 0, .05, 1, 5);
      v = move.drag();
    }
    assertEquals(.05 * .98 / .09, move.z(), 1e-8);
    assertEquals(.15 / .4, move.y(), 1e-8);
  }

  @Test
  void sprintDoublesHorizontalSpeedAndDiagonalsAreNormalized() {
    Motion zero = new Motion(0, 0, 0);
    Motion forward = FlightPhysics.step(zero, 0, 1, 0, 0, .05, 1, 5);
    Motion diagonal = FlightPhysics.step(zero, 1, 1, 0, 0, .05, 1, 5);
    assertEquals(forward.z(), Math.hypot(diagonal.x(), diagonal.z()), 1e-10);
    assertEquals(forward.z() * 2, FlightPhysics.step(zero, 0, 1, 0, 0, .05, 2, 5).z(), 1e-10);
  }

  @Test
  void zeroGlideStopsOnTheFirstIdleTickAndLevelsAreLinear() {
    Motion v = new Motion(.8, .6, -.4);
    for (int i = 0; i <= 5; i++) {
      Motion idle = FlightPhysics.step(v, 0, 0, 0, 20, .05, 1, i);
      assertEquals(v.x() * i / 5, idle.x(), 1e-12);
      assertEquals(v.y() * i / 5, idle.y(), 1e-12);
      assertEquals(v.z() * i / 5, idle.z(), 1e-12);
    }
  }

  @Test
  void independentAxisReleaseStopsDriftWithOtherKeysHeld() {
    Motion v = new Motion(.8, .6, -.4);
    Motion vertical = FlightPhysics.step(v, 0, 0, 1, 0, .05, 1, 0);
    assertEquals(0, vertical.x());
    assertEquals(0, vertical.z());
    assertEquals(0, FlightPhysics.step(v, 1, 0, 0, 0, .05, 1, 0).y());
  }

  @Test
  void scrollUsesModernStepsAndBounds() {
    assertEquals(.055f, FlightPhysics.scroll(.05f, 120), 1e-6);
    assertEquals(0f, FlightPhysics.scroll(.005f, -120));
    assertEquals(.06f, FlightPhysics.scroll(.05f, 240), 1e-6);
    assertEquals(.005f, FlightPhysics.scroll(0f, 120), 1e-6);
    assertEquals(.2f, FlightPhysics.scroll(.2f, 120));
  }
}
