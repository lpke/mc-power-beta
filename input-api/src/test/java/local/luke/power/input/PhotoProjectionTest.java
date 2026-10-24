package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PhotoProjectionTest {
  @Test void sameAspectRetainsFramingAcrossResolutionAndGuiScale() {
    assertEquals(PhotoProjection.width(1920, 1080), PhotoProjection.width(3840, 2160));
    assertEquals(PhotoProjection.width(2560, 1080), PhotoProjection.width(5120, 2160));
    assertEquals(1080, PhotoProjection.height());
  }
  @Test void equalFractionPointerDragsPanTheSameDistance() {
    assertEquals(100 * PhotoProjection.pointerScale(1080), 200 * PhotoProjection.pointerScale(2160));
    assertEquals(1080 * 5120d / 2160, PhotoProjection.width(5120, 2160));
    assertTrue(Double.isFinite(PhotoProjection.width(0, 0)));
  }

  @Test void retainsOriginalZoomScaleAndFullZoomOutRange() {
    for (double zoom : new double[]{0.01, 1.25, 2.25, 12})
      assertEquals(Math.pow(2, zoom), PhotoProjection.zoomFactor(zoom));
    assertEquals(1072.539895, PhotoProjection.height() / PhotoProjection.zoomFactor(0.01), 1e-5);
    assertEquals(454.084064, PhotoProjection.height() / PhotoProjection.zoomFactor(1.25), 1e-5);
    assertEquals(2, PhotoProjection.zoomFactor(2.25) / PhotoProjection.zoomFactor(1.25), 1e-10);
  }

  @Test void openingFitUsesMostOfTheViewportWithoutCroppingEitherAxis() {
    for (int[] size : new int[][]{{1920,1080}, {1080,1920}, {5120,2160}, {320,240}}) {
      for (int[] spans : new int[][]{{144,144}, {272,272}, {528,336}}) {
        for (double tilt : new double[]{0,30,60,90}) for (double yaw : new double[]{0,45,90,135,270}) {
          double zoom = gridZoom(size[0], size[1], spans[0], spans[1], tilt, yaw);
          double viewWidth = PhotoProjection.width(size[0], size[1]) / PhotoProjection.zoomFactor(zoom);
          double viewHeight = PhotoProjection.height() / PhotoProjection.zoomFactor(zoom);
          double horizontal = 0, vertical = 0;
          // Project all eight corners independently of the fit's extent formula.
          for (int sx : new int[]{-1,1}) for (int sy : new int[]{-1,1}) for (int sz : new int[]{-1,1}) {
            double x = sx * spans[0] / 2d, y = sy * 64d, z = sz * spans[1] / 2d;
            double rotatedX = x * Math.cos(Math.toRadians(yaw)) + z * Math.sin(Math.toRadians(yaw));
            double rotatedZ = z * Math.cos(Math.toRadians(yaw)) - x * Math.sin(Math.toRadians(yaw));
            double rotatedY = y * Math.cos(Math.toRadians(tilt)) - rotatedZ * Math.sin(Math.toRadians(tilt));
            horizontal = Math.max(horizontal, Math.abs(rotatedX) * 2 / viewWidth);
            vertical = Math.max(vertical, Math.abs(rotatedY) * 2 / viewHeight);
          }
          if (zoom > 0.01) {
            assertTrue(horizontal <= 0.840001, "horizontal cropping");
            assertTrue(vertical <= 0.840001, "vertical cropping");
            assertEquals(0.84, Math.max(horizontal, vertical), 1e-10);
          } else {
            assertTrue(Math.max(horizontal, vertical) >= 0.84, "zoomed past the manual minimum");
          }
        }
      }
    }
  }

  @Test void openingZoomAdaptsToTerrainAndAspectRatherThanPixelDensity() {
    double smaller = gridZoom(1920,1080,144,144,30,45);
    double larger = gridZoom(1920,1080,272,272,30,45);
    assertTrue(smaller > larger);
    assertTrue(larger > 1.25);
    assertEquals(larger, gridZoom(3840,2160,272,272,30,45));
    assertTrue(gridZoom(1080,1920,272,272,30,45) < larger);
    assertNotEquals(larger, gridZoom(1920,1080,272,272,90,45));
    assertNotEquals(larger, gridZoom(1920,1080,272,272,30,0));
  }

  @Test void openingZoomRemainsFiniteAndWithinTheManualRange() {
    for (int[] size : new int[][]{{0,0}, {-1,-1}, {1,Integer.MAX_VALUE}, {Integer.MAX_VALUE,1}}) {
      for (int span : new int[]{-1,0,16,Integer.MAX_VALUE}) {
        double zoom = gridZoom(size[0],size[1],span,span,30,45);
        assertTrue(Double.isFinite(zoom) && zoom >= 0.01 && zoom <= 12);
      }
    }
    assertEquals(1.25, gridZoom(1920,1080,272,272,Double.NaN,45));
    assertEquals(1.25, gridZoom(1920,1080,272,272,30,Double.POSITIVE_INFINITY));
  }

  @Test void measuredSectionsFitAndCenterAtAnyCameraHeight() {
    for (double eyeY : new double[]{-128, 64, 512, 10000}) {
      var frame = new PhotoProjection.TerrainFrame(30,45);
      double[][] boxes = {{-96,-eyeY,-96,-80,16-eyeY,-80}, {80,64-eyeY,80,96,80-eyeY,96},
          {-16,64-eyeY,-16,16,96-eyeY,16}};
      for (double[] box : boxes) frame.include(box[0],box[1],box[2],box[3],box[4],box[5]);
      var view = frame.initialView(1920,1080);
      double minX=Double.POSITIVE_INFINITY,minY=Double.POSITIVE_INFINITY;
      double maxX=Double.NEGATIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY;
      for (double[] box : boxes) for (int ix : new int[]{0,3})
          for (int iy : new int[]{1,4}) for (int iz : new int[]{2,5}) {
        double x=box[ix]*Math.cos(Math.PI/4)+box[iz]*Math.sin(Math.PI/4);
        double z=box[iz]*Math.cos(Math.PI/4)-box[ix]*Math.sin(Math.PI/4);
        double y=box[iy]*Math.cos(Math.PI/6)-z*Math.sin(Math.PI/6);
        x=(x+view.cameraX()*1920)*PhotoProjection.zoomFactor(view.zoom())/960;
        y=(y+view.cameraY()*1080)*PhotoProjection.zoomFactor(view.zoom())/540;
        minX=Math.min(minX,x);maxX=Math.max(maxX,x);
        minY=Math.min(minY,y);maxY=Math.max(maxY,y);
      }
      assertEquals(0,minX+maxX,1e-10);
      assertEquals(0,minY+maxY,1e-10);
      assertEquals(.84,Math.max(maxX,maxY),1e-10);
      assertTrue(view.zoom()>gridZoom(1920,1080,192,192,30,45));
    }
  }

  @Test void emptyOrDegenerateMetadataHasABoundedImmediateFallback() {
    assertEquals(new PhotoProjection.InitialView(1.25,0,0),
        new PhotoProjection.TerrainFrame(30,45).initialView(1920,1080));
    var frame = new PhotoProjection.TerrainFrame(0,0);
    frame.include(0,0,0,0,0,0);
    assertTrue(Double.isFinite(frame.initialView(0,0).zoom()));
  }

  private static double gridZoom(int width,int height,int spanX,int spanZ,double tilt,double yaw) {
    var frame = new PhotoProjection.TerrainFrame(tilt,yaw);
    double x=Math.max(16,spanX)/2d,z=Math.max(16,spanZ)/2d;
    frame.include(-x,-64,-z,x,64,z);
    return frame.initialView(width,height).zoom();
  }

}
