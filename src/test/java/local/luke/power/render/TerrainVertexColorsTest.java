package local.luke.power.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerrainVertexColorsTest {
  private ByteBuffer vertices() {
    ByteBuffer b = ByteBuffer.allocate(112);
    for (int i = 0; i < b.limit(); i++) b.put(i, (byte)i);
    return b;
  }
  @Test void packsOnlyExtraColorsAndPreservesBothInputCursorsAndGeometry() {
    ByteBuffer nativeData = vertices().order(ByteOrder.LITTLE_ENDIAN), clear = vertices();
    for (int p = 0; p < 112; p += 28) for (int i = 20; i < 23; i++) clear.put(p + i, (byte)245);
    nativeData.position(7); clear.position(13);
    ByteBuffer out = ByteBuffer.allocate(256);
    TerrainVertexColors.pack(nativeData, clear, out);
    assertEquals(128, out.limit()); assertEquals(0, out.position());
    assertEquals(7, nativeData.position()); assertEquals(13, clear.position());
    for (int p = 0, q = 0; p < 112; p += 28, q += 32) {
      for (int i = 0; i < 28; i++) assertEquals(nativeData.get(p+i), out.get(q+i));
      for (int i = 0; i < 4; i++) assertEquals(clear.get(p+20+i), out.get(q+28+i));
    }
  }
  @Test void unchangedMeshUsesItsNativeColorAtEveryPercentage() {
    ByteBuffer in = vertices(), out = ByteBuffer.allocate(128);
    TerrainVertexColors.pack(in, null, out);
    for (int p=0;p<128;p+=32) for(int i=0;i<4;i++) assertEquals(out.get(p+20+i),out.get(p+28+i));
  }
  @Test void changedGeometryUvNormalAlphaAndCountsFallBackToNativeColors() {
    for (int offset : new int[]{0,12,16,23,24,80}) {
      ByteBuffer a=vertices(), b=vertices(); b.put(offset,(byte)255);
      assertFalse(TerrainVertexColors.compatible(a,b));
      ByteBuffer output=ByteBuffer.allocate(128);
      assertFalse(TerrainVertexColors.pack(a,b,output));
      for(int p=0;p<128;p+=32)for(int c=0;c<4;c++)assertEquals(output.get(p+20+c),output.get(p+28+c));
    }
    assertFalse(TerrainVertexColors.compatible(vertices(),ByteBuffer.allocate(28)));
    assertThrows(IllegalArgumentException.class,()->TerrainVertexColors.outputSize(29));
    assertEquals(0,TerrainVertexColors.outputSize(0));
    assertEquals(3200,TerrainVertexColors.outputSize(2800));
  }
}
