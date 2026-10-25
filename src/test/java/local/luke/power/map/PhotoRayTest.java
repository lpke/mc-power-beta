package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PhotoRayTest {
  @Test void selectsTheFirstVisibleSurfaceIncludingAnExposedCaveFloor() {
    assertEquals(new PhotoRay.Hit(4,63,2),PhotoRay.trace(4.5,127.9,2.5,4.5,.1,2.5,256,
        (x,y,z)->y==63?1:0));
    assertEquals(new PhotoRay.Hit(12,20,1),PhotoRay.trace(.5,20.5,1.5,25.5,20.5,1.5,256,
        (x,y,z)->x==12?1:0));
  }
  @Test void negativeCoordinatesAndCornerCrossingsDoNotSkipTheHit() {
    assertEquals(new PhotoRay.Hit(-3,2,-3),PhotoRay.trace(-.5,4.5,-.5,-5.5,-.5,-5.5,256,
        (x,y,z)->x==-3&&y==2&&z==-3?1:0));
  }
  @Test void unloadedTerrainStopsTheRayWithoutReadingBeyondIt() {
    AtomicInteger reads=new AtomicInteger();
    assertNull(PhotoRay.trace(.5,20.5,.5,64.5,20.5,.5,256,(x,y,z)->{
      reads.incrementAndGet();assertTrue(x<=16);return x==16?-1:0;
    }));
    assertEquals(17,reads.get());
  }
  @Test void malformedAndLongRaysRemainBoundedAndCannotSelectPastTheirEndpoint() {
    AtomicInteger reads=new AtomicInteger();
    assertNull(PhotoRay.trace(Double.NaN,0,0,1,1,1,8192,(x,y,z)->{reads.incrementAndGet();return 1;}));
    assertEquals(0,reads.get());
    assertNull(PhotoRay.trace(.5,.5,.5,30000,.5,.5,32,(x,y,z)->{reads.incrementAndGet();return 0;}));
    assertEquals(32,reads.get());
    assertNull(PhotoRay.trace(.5,.5,.5,.9,.5,.5,32,(x,y,z)->x==1?1:0));
  }
}
