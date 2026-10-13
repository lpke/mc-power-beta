package local.luke.power.visual;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class CapeAnglesTest {
  @Test void normalMotionIsUnchangedAndHighSpeedCannotFlipTheCape() {
    for(float angle:new float[]{-20,0,6,25,60,89,90}) assertEquals(angle,CapeAngles.pitch(angle));
    for(float angle:new float[]{91,180,360,10000,Float.MAX_VALUE}) assertEquals(90,CapeAngles.pitch(angle));
    for(float angle:new float[]{-30,-12,0,12,30}) assertEquals(angle,CapeAngles.sideways(angle));
    assertEquals(-30,CapeAngles.sideways(-10000));assertEquals(30,CapeAngles.sideways(10000));
    assertEquals(0,CapeAngles.pitch(Float.NaN));assertEquals(0,CapeAngles.sideways(Float.POSITIVE_INFINITY));
  }
  @Test void existingProfilesGetTheDefaultAndAnOptOutSurvivesGson289() {
    var gson=new Gson();var defaults=gson.fromJson("{}",VisualSettings.class);
    assertTrue(defaults.limitCapeRotation);
    defaults.limitCapeRotation=false;
    assertFalse(gson.fromJson(gson.toJson(defaults),VisualSettings.class).limitCapeRotation);
  }
}
