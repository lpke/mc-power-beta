package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class RadarIconPixelsTest {
  @Test void outlineAddsOnlyTheEightNeighborsAndKeepsTheHeadColor() {
    int side = 7, center = (3 * side + 3) * 4;
    var source = ByteBuffer.allocate(side * side * 4);
    var result = ByteBuffer.allocate(side * side * 4);
    source.put(center,(byte)0x12); source.put(center+1,(byte)0x34);
    source.put(center+2,(byte)0x56); source.put(center+3,(byte)255);
    RadarIconPixels.outline(source,result,side);
    int count = 0;
    for (int y=0;y<side;y++) for (int x=0;x<side;x++) {
      int i=(y*side+x)*4, alpha=result.get(i+3)&255;
      if (alpha>0) count++;
      if (Math.abs(x-3)>1 || Math.abs(y-3)>1) assertEquals(0,alpha);
      else assertEquals(255,alpha);
      if (i!=center) for (int channel=0;channel<3;channel++) assertEquals(0,result.get(i+channel));
    }
    assertEquals(9,count);
    assertEquals(0x12,result.get(center)); assertEquals(0x34,result.get(center+1));
    assertEquals(0x56,result.get(center+2));
  }

  @Test void edgesNeverWrapAndSemiTransparentReadbackReturnsStraightRgb() {
    int side=4;
    var source=ByteBuffer.allocate(side*side*4); var result=ByteBuffer.allocate(side*side*4);
    source.put(0,(byte)64); source.put(3,(byte)128);
    RadarIconPixels.outline(source,result,side);
    assertEquals(128,result.get(0)&255); assertEquals(128,result.get(3)&255);
    assertEquals(128,result.get((side+1)*4+3)&255);
    assertEquals(0,result.get((side-1)*4+3)&255);
    assertEquals(0,result.get((side*side-1)*4+3)&255);
  }
}
