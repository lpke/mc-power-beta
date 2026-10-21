package local.luke.power.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeldLightMathTest {
  @Test void boundedPreparationTouchesAtMost27AndInfluenceAtMost8Sections() {
    for(double radius:new double[]{HeldLightMath.RADIUS,HeldLightMath.PREPARE_RADIUS}) {
      int maximum=radius==HeldLightMath.RADIUS?2:3;
      for(int i=-6400;i<=6400;i++) {
        double point=i/100.0;
        int low=(int)Math.floor((point-radius)/16), high=(int)Math.floor((point+radius)/16);
        assertTrue(high-low+1<=maximum);
      }
    }
  }

  @Test void sectionDistanceWorksAcrossZeroNegativeAndFarRegionBoundaries() {
    for(int offset:new int[]{-32000000,-1024,-16,0,16,1024,32000000}) {
      assertEquals(0,HeldLightMath.sectionDistanceSquared(offset+8,8,offset+8,offset,0,offset));
      assertEquals(9,HeldLightMath.sectionDistanceSquared(offset-3,8,offset+8,offset,0,offset));
      assertEquals(25,HeldLightMath.sectionDistanceSquared(offset-3,8,offset+20,offset,0,offset));
    }
  }

  @Test void radialCurveUsesNativeEndpointsAndNeverExceedsEmission() {
    float[] curve=new float[16];
    for(int i=0;i<16;i++) { float darkness=1-i/15f; curve[i]=(1-darkness)/(darkness*3+1)*.95f+.05f; }
    for(int emission=1;emission<=15;emission++) {
      assertEquals(curve[emission],HeldLightMath.brightness(curve,emission,0));
      float previous=curve[emission];
      for(int step=1;step<=1000;step++) {
        double distance=HeldLightMath.RADIUS*step/1000;
        float actual=HeldLightMath.brightness(curve,emission,distance*distance);
        assertTrue(actual>=0&&actual<=previous+1e-6); previous=actual;
      }
      assertEquals(0,HeldLightMath.brightness(curve,emission,HeldLightMath.RADIUS*HeldLightMath.RADIUS));
    }
    assertEquals(0,HeldLightMath.brightness(curve,0,0));
    assertEquals(0,HeldLightMath.brightness(curve,16,0));
    assertEquals(0,HeldLightMath.brightness(curve,15,Double.NaN));
    assertEquals(0,HeldLightMath.brightness(curve,15,-1));
  }

  @Test void sourceValidationRejectsInvalidCoordinatesWithoutTerrainQueries() {
    assertTrue(HeldLightMath.finiteSource(32000000,64,-32000000));
    assertFalse(HeldLightMath.finiteSource(32000001,64,0));
    assertFalse(HeldLightMath.finiteSource(Double.NaN,64,0));
    assertFalse(HeldLightMath.finiteSource(0,Double.POSITIVE_INFINITY,0));
    assertFalse(HeldLightMath.finiteSource(0,-100,0));
  }
}
