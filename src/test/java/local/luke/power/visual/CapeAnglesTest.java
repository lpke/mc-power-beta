package local.luke.power.visual;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class CapeAnglesTest {
  @Test void normalMotionIsUnchangedAndHighSpeedCannotFlipTheCape() {
    for(float angle:new float[]{0,6,25,60,89,90}) assertEquals(angle,CapeAngles.pitch(angle));
    assertEquals(0,CapeAngles.pitch(-20));
    for(float angle:new float[]{91,180,360,10000,Float.MAX_VALUE}) assertEquals(90,CapeAngles.pitch(angle));
    for(float angle:new float[]{-30,-12,0,12,30}) assertEquals(angle,CapeAngles.sideways(angle));
    assertEquals(-30,CapeAngles.sideways(-10000));assertEquals(30,CapeAngles.sideways(10000));
    assertEquals(0,CapeAngles.pitch(Float.NaN));assertEquals(0,CapeAngles.sideways(Float.POSITIVE_INFINITY));
  }
  @Test void renderedLegsAndArmorStayAheadOfTheCapePlaneAcrossTheWholeStride() {
    for (float pivotY : new float[]{9,12}) for (float pivotZ : new float[]{0,4})
      for (float leg = -1.4f; leg <= 1.4f; leg += .025f) {
        double cape = Math.toRadians(CapeAngles.legClearance(leg,pivotY,pivotZ));
        for (double y : new double[]{-1,13}) for (double z : new double[]{-3,3}) {
          double renderedY = pivotY+y*Math.cos(leg)-z*Math.sin(leg);
          double renderedZ = pivotZ+y*Math.sin(leg)+z*Math.cos(leg);
          assertTrue(renderedY*Math.sin(cape)-(renderedZ-2)*Math.cos(cape)>=-1e-5,
              "leg crossed the cape plane");
        }
      }
  }
  @Test void existingProfilesGetTheDefaultAndAnOptOutSurvivesGson289() {
    var gson=new Gson();var defaults=gson.fromJson("{}",VisualSettings.class);
    assertTrue(defaults.limitCapeRotation);
    defaults.limitCapeRotation=false;
    assertFalse(gson.fromJson(gson.toJson(defaults),VisualSettings.class).limitCapeRotation);
  }
}
