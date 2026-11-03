package local.luke.power.visual;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class CapeAnglesTest {
  @Test void normalMotionIsUnchangedAndHighSpeedCannotFlipTheCape() {
    for(float angle:new float[]{0,6,25,60,89,90}) assertEquals(angle,CapeAngles.pitch(angle));
    assertEquals(0,CapeAngles.pitch(-20));
    for(float angle:new float[]{91,180,360,10000,Float.MAX_VALUE}) assertEquals(90,CapeAngles.pitch(angle));
    for(float angle:new float[]{-10000,-30,-12,0,12,30,10000}) assertEquals(0,CapeAngles.sideways(angle));
    assertEquals(0,CapeAngles.pitch(Float.NaN));assertEquals(0,CapeAngles.sideways(Float.POSITIVE_INFINITY));
  }
  @Test void oneStableGaitAngleClearsTheFiniteCapeAndArmorThroughoutWalkingAndSprinting() {
    for (boolean crouched : new boolean[]{false,true}) {
      float pivotY=crouched?9:12,pivotZ=crouched?4:0;
      for (int step=0;step<=100;step++) {
        float amplitude=step/100f;
        float cape=CapeAngles.strideClearance(amplitude,pivotY,pivotZ);
        for (int phase=0;phase<256;phase++) {
          float leg=(float)Math.cos(phase*Math.PI/128)*1.4f*amplitude;
          assertFalse(intersects(cape,leg,pivotY,pivotZ),
              "finite cape hit armor, amplitude="+amplitude+" leg="+leg+" crouched="+crouched);
        }
      }
    }
  }
  @Test void walkingStaysNearTheBackAndGaitTransitionsRemainContinuous() {
    assertTrue(CapeAngles.strideClearance(.35f,12,0)<13,"walking cape pulled too far from the back");
    assertTrue(CapeAngles.strideClearance(1,12,0)<46,"sprinting clearance included boots below the hem");
    float previous=CapeAngles.strideClearance(0,12,0);
    for(int step=1;step<=1000;step++) {
      float current=CapeAngles.strideClearance(step/1000f,12,0);
      assertTrue(current>=previous-.0001f,"faster motion pulled the cape inward");
      assertTrue(current-previous<.2f,"gait amplitude introduced an angle step");previous=current;
    }
  }
  private static boolean intersects(float cape,float leg,float pivotY,float pivotZ) {
    double angle=Math.toRadians(cape),sin=Math.sin(leg),cos=Math.cos(leg);
    double y=-pivotY*cos+(2-pivotZ)*sin,z=pivotY*sin+(2-pivotZ)*cos;
    double dy=Math.cos(angle)*cos+Math.sin(angle)*sin;
    double dz=-Math.cos(angle)*sin+Math.sin(angle)*cos;
    double enter=0,exit=16;
    for(double[] slab:new double[][]{{y,dy,-.9999,12.9999},{z,dz,-2.9999,2.9999}}) {
      if(Math.abs(slab[1])<1e-10) { if(slab[0]<=slab[2]||slab[0]>=slab[3])return false; }
      else {
        double a=(slab[2]-slab[0])/slab[1],b=(slab[3]-slab[0])/slab[1];
        enter=Math.max(enter,Math.min(a,b));exit=Math.min(exit,Math.max(a,b));
      }
    }
    return enter<exit;
  }
  @Test void existingProfilesGetTheDefaultAndAnOptOutSurvivesGson289() {
    var gson=new Gson();var defaults=gson.fromJson("{}",VisualSettings.class);
    assertTrue(defaults.limitCapeRotation);
    defaults.limitCapeRotation=false;
    assertFalse(gson.fromJson(gson.toJson(defaults),VisualSettings.class).limitCapeRotation);
  }
}
