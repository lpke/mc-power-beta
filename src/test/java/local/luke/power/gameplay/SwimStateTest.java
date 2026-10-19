package local.luke.power.gameplay;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SwimStateTest {
  @Test void modernWaterSettlesOnlyVelocityStrictlyBelowThreeThousandths() {
    assertEquals(0,SwimState.settled(.002999999));
    assertEquals(0,SwimState.settled(-.002999999));
    assertEquals(.003,SwimState.settled(.003));
    assertEquals(-.003,SwimState.settled(-.003));
    assertEquals(.0045,SwimState.settled(.0045));
  }

  @Test void modernDiveAndSurfaceSteeringMatchesOfficialPlayerTravel() {
    assertEquals(-.085,SwimState.steer(0,90,false,true),1e-12);
    assertEquals(.06,SwimState.steer(0,-90,false,true),1e-12);
    assertEquals(.2,SwimState.steer(.2,-90,false,false),1e-12);
    assertEquals(.248,SwimState.steer(.2,-90,true,false),1e-12);
    assertEquals(.188,SwimState.steer(.2,0,false,false),1e-12);
    assertEquals(-.15160407640085653,SwimState.steer(-.1,45,false,true),1e-12);
  }

  @Test void pitchAndVerticalDragConvergeToTheIndependentModernReference() {
    for(float pitch:new float[]{-90,-45,0,30,90}) {
      double actual=0,reference=0;
      double lookY=-Math.sin(pitch*Math.PI/180);
      for(int tick=0;tick<80;tick++) {
        actual=SwimState.steer(actual,pitch,false,true)*(double).8f;
        reference=(reference+(lookY-reference)*(lookY<-.2?.085:.06))*(double).8f;
        assertEquals(reference,actual,1e-12);
      }
      double expected=lookY*(lookY<-.2?.085:.06)*.8/(1-.8*(1-(lookY<-.2?.085:.06)));
      assertEquals(expected,actual,1e-7);
    }
  }

  @Test void compactAnimationInterpolatesTwelveTicksAndClearsWithoutResidualPose() {
    var state=new SwimState();state.compact=true;
    state.animate();assertEquals(.045,state.amount(.5f),1e-7);
    for(int tick=1;tick<12;tick++)state.animate();
    assertEquals(1,state.amount);state.compact=false;
    for(int tick=0;tick<12;tick++)state.animate();
    assertEquals(0,state.amount);assertEquals(0,state.amount(1));
  }

  @Test void cameraUsesModernHalfResponseInBothDirectionsAndInterpolatesFrames() {
    var state=new SwimState();
    state.cameraTick(.4f);
    assertEquals(1.62,state.cameraEye(0),1e-6);
    assertEquals(1.315,state.cameraEye(.5f),1e-6);
    assertEquals(1.01,state.cameraEye(1),1e-6);
    state.cameraTick(.4f);assertEquals(.705,state.cameraEye(1),1e-6);
    state.cameraTick(.4f);assertEquals(.5525,state.cameraEye(1),1e-6);
    state.cameraTick(1.62f);assertEquals(1.08625,state.cameraEye(1),1e-6);
    state.resetCamera(.4f);assertEquals(.4,state.cameraEye(.5f),1e-6);
  }

  @Test void strokeMatchesTheModernThreePhasesAndOpposingLegCycle() {
    assertEquals(0,SwimAnimation.armPitch(0));
    assertEquals(0,SwimAnimation.armPitch(14));
    assertEquals(Math.PI/2,SwimAnimation.armPitch(22),1e-6);
    assertEquals(Math.PI/4,SwimAnimation.armPitch(24),1e-6);
    assertEquals(Math.PI,SwimAnimation.leftArmRoll(0),1e-6);
    assertEquals(5.012389,SwimAnimation.leftArmRoll(14),1e-6);
    assertEquals(Math.PI,SwimAnimation.leftArmRoll(22),1e-6);
    for(float phase:new float[]{0,4,13.9f,14,20,22,25.9f}) {
      assertEquals(SwimAnimation.armPitch(phase),SwimAnimation.armPitch(phase+26),1e-6);
      assertEquals(SwimAnimation.leftArmRoll(phase),SwimAnimation.leftArmRoll(phase+26),1e-6);
    }
    assertEquals(Math.PI,SwimAnimation.angle(.5f,(float)Math.PI-.1f,-(float)Math.PI+.1f),1e-6);
  }
}
