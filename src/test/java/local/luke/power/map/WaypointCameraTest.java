package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;

class WaypointCameraTest {
  private static FloatBuffer matrix(double... values) {
    var result = FloatBuffer.allocate(16);
    for (int i=0;i<16;i++) result.put(i,(float)values[i]);
    return result;
  }
  private static FloatBuffer identity() { return matrix(1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1); }
  private static FloatBuffer translation(double x,double y,double z) {
    var result=identity(); result.put(12,(float)x);result.put(13,(float)y);result.put(14,(float)z);return result;
  }
  private static FloatBuffer rotation(double radians) {
    double c=Math.cos(radians),s=Math.sin(radians);return matrix(c,s,0,0, -s,c,0,0, 0,0,1,0, 0,0,0,1);
  }
  private static FloatBuffer multiply(FloatBuffer a,FloatBuffer b) {
    var result=FloatBuffer.allocate(16);
    for(int c=0;c<4;c++)for(int r=0;r<4;r++) {
      double value=0;for(int k=0;k<4;k++)value+=a.get(k*4+r)*b.get(c*4+k);result.put(c*4+r,(float)value);
    }
    return result;
  }
  @Test void removesOnlyWalkingBobWhileKeepingStereoHurtPortalAndCameraTransforms() {
    var prefix=multiply(translation(.1,.02,0),rotation(.2));
    var portal=matrix(.8,0,0,0, 0,1.2,0,0, 0,0,1,0, 0,0,0,1);
    var camera=multiply(rotation(.7),translation(0,-1.62,-4));
    var expected=multiply(prefix,multiply(portal,camera));
    var actual=FloatBuffer.allocate(16); var state=new WaypointCamera();
    for(int phase=0;phase<64;phase++) {
      var bob=multiply(translation(Math.sin(phase)*.08,Math.cos(phase)*.07,0),rotation(Math.sin(phase)*.12));
      state.before(prefix);state.after(multiply(prefix,bob));
      state.apply(multiply(multiply(prefix,bob),multiply(portal,camera)),actual);
      for(int i=0;i<16;i++)assertEquals(expected.get(i),actual.get(i),2e-6,"phase="+phase+" component="+i);
    }
  }
  @Test void canceledBobAndEachNewEyeOrDetachedFrameKeepTheOriginalMatrix() {
    var state=new WaypointCamera();var output=FloatBuffer.allocate(16);var model=multiply(rotation(.6),translation(2,3,4));
    state.before(model);state.after(model);state.apply(model,output);
    for(int i=0;i<16;i++)assertEquals(model.get(i),output.get(i),1e-6);
    state.before(identity());state.after(translation(4,5,6));state.reset();state.apply(model,output);
    for(int i=0;i<16;i++)assertEquals(model.get(i),output.get(i));
    var singular=identity();singular.put(0,0);state.before(identity());state.after(singular);state.apply(model,output);
    for(int i=0;i<16;i++)assertEquals(model.get(i),output.get(i));
  }
  @Test void distantWaypointStaysVisibleAtCenterBeyondTerrainFarPlane() {
    var projection=matrix(1.4,0,0,0, 0,1.8,0,0, 0,0,-1.0008,-1, 0,0,-.10004,0);
    assertNotNull(WaypointProjection.project(0,0,-900,identity(),projection));
    assertNotNull(WaypointProjection.project(100,0,-900,identity(),projection));
    assertNull(WaypointProjection.project(0,0,900,identity(),projection));
    assertNull(WaypointProjection.project(0,0,-.01,identity(),projection));
  }
}
