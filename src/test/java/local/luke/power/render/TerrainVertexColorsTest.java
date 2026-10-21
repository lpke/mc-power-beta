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
  private int bits(ByteBuffer b, int p) { return (b.get(p + 27) & 255) << 8 | b.get(p + 31) & 255; }
  private float neutral(ByteBuffer b, int p, int channel) {
    int bits = bits(b, p);
    return (bits & 32768) == 0 ? (b.get(p + 28 + channel) & 255) / 255f : ((bits >> (10 - channel * 5)) & 31) / 31f;
  }
  private int water(ByteBuffer b, int p, int channel) { return b.get(p + ((bits(b, p) & 32768) == 0 ? 20 : 28) + channel) & 255; }

  @Test void preservesGeometryUvNativeAlphaRealNormalsAndInputCursorsAcrossByteOrders() {
    ByteBuffer nativeData = vertices().order(ByteOrder.LITTLE_ENDIAN), clear = vertices();
    for (int p = 0; p < 112; p += 28) for (int i = 20; i < 23; i++) clear.put(p + i, (byte)245);
    nativeData.position(7); clear.position(13);
    ByteBuffer out = ByteBuffer.allocate(256);
    assertTrue(TerrainVertexColors.pack(nativeData, clear, out));
    assertEquals(128, out.limit()); assertEquals(0, out.position());
    assertEquals(7, nativeData.position()); assertEquals(13, clear.position());
    for (int p = 0, q = 0; p < 112; p += 28, q += 32) {
      for (int i = 0; i < 27; i++) assertEquals(nativeData.get(p+i), out.get(q+i));
      for (int c = 0; c < 3; c++) { assertEquals(245, water(out,q,c)); assertEquals(0,neutral(out,q,c)); }
    }
  }

  @Test void nativePaddingCannotAccidentallyEnableSeparateWaterOrLight() {
    ByteBuffer in = vertices(), out = ByteBuffer.allocate(128);
    for (int p=0;p<112;p+=28) in.put(p+27,(byte)255);
    assertTrue(TerrainVertexColors.pack(in,null,out));
    for (int p=0,q=0;p<112;p+=28,q+=32) {
      assertEquals(0,bits(out,q));
      for(int c=0;c<3;c++) { assertEquals(in.get(p+20+c)&255,water(out,q,c)); assertEquals(0,neutral(out,q,c)); }
    }
  }

  @Test void dryVerticesKeepExactNeutralRgbAndWaterVerticesKeepExactWaterRgb() {
    ByteBuffer in=vertices(), clear=vertices(), lit=vertices(), out=ByteBuffer.allocate(128);
    for(int p=0;p<112;p+=28)for(int c=0;c<3;c++)lit.put(p+20+c,(byte)(251-c*79));
    for(int c=0;c<3;c++) clear.put(20+c,(byte)(200-c));
    TerrainVertexColors.pack(in,clear,out);
    assertTrue(TerrainVertexColors.neutral(out,lit));
    for(int p=0,q=0;p<112;p+=28,q+=32)for(int c=0;c<3;c++) {
      assertEquals(clear.get(p+20+c)&255,water(out,q,c));
      float expected=(lit.get(p+20+c)&255)/255f;
      assertEquals(expected,neutral(out,q,c),p==0? 4.097f/255 : 0);
    }
    TerrainVertexColors.clearNeutral(out);
    for(int p=0,q=0;p<112;p+=28,q+=32)for(int c=0;c<3;c++) {
      assertEquals(clear.get(p+20+c)&255,water(out,q,c)); assertEquals(0,neutral(out,q,c));
    }
  }

  @Test void everyRgb555ChannelHasBoundedErrorAndMonotonicEndpoints() {
    int previous=-1;
    for(int value=0;value<256;value++) {
      int quantized=TerrainVertexColors.quantize(value);
      assertTrue(quantized>=previous); previous=quantized;
      assertTrue(Math.abs(quantized/31.0-value/255.0)<=4.097/255.0);
    }
    assertEquals(0,TerrainVertexColors.quantize(0)); assertEquals(31,TerrainVertexColors.quantize(255));
  }

  @Test void endpointReuseDoesNotDestroySeparateWater() {
    ByteBuffer in=vertices(), clear=vertices(), out=ByteBuffer.allocate(128);
    for(int p=0;p<112;p+=28)for(int c=0;c<3;c++)clear.put(p+20+c,(byte)(220-c*13));
    TerrainVertexColors.pack(in,clear,out); TerrainVertexColors.reuseNative(out);
    for(int p=0,q=0;p<112;p+=28,q+=32)for(int c=0;c<3;c++) {
      assertEquals(clear.get(p+20+c)&255,water(out,q,c));
      assertEquals((in.get(p+20+c)&255)/255f,neutral(out,q,c),4.097f/255);
    }
    TerrainVertexColors.reuseWater(out);
    for(int p=0,q=0;p<112;p+=28,q+=32)for(int c=0;c<3;c++)
      assertEquals((clear.get(p+20+c)&255)/255f,neutral(out,q,c),4.097f/255);
  }

  @Test void changedGeometryUvNormalAlphaAndCountsLeavePriorEndpointsUntouched() {
    for(int offset:new int[]{0,12,16,23,24,26,80}) {
      ByteBuffer in=vertices(), wrong=vertices(); wrong.put(offset,(byte)255);
      assertFalse(TerrainVertexColors.compatible(in,wrong));
      ByteBuffer out=ByteBuffer.allocate(128); TerrainVertexColors.pack(in,null,out); TerrainVertexColors.reuseNative(out);
      byte[] before=out.array().clone();
      assertFalse(TerrainVertexColors.water(out,wrong)); assertArrayEquals(before,out.array());
      assertFalse(TerrainVertexColors.neutral(out,wrong)); assertArrayEquals(before,out.array());
    }
    ByteBuffer padding=vertices(); padding.put(27,(byte)255);
    assertTrue(TerrainVertexColors.compatible(vertices(),padding));
    assertFalse(TerrainVertexColors.compatible(vertices(),ByteBuffer.allocate(28)));
    assertThrows(IllegalArgumentException.class,()->TerrainVertexColors.outputSize(29));
    assertEquals(0,TerrainVertexColors.outputSize(0)); assertEquals(3200,TerrainVertexColors.outputSize(2800));
  }
}
