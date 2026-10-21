package local.luke.power.offhand;

import static local.luke.power.offhand.HeldItemTransforms.Kind.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class HeldItemTransformsTest {
  private static final double EPSILON = 1e-6;

  @Test void nativeFlatBakeIsRemovedBeforeCenteringBothFacesAndEveryCorner() {
    for (var kind : new HeldItemTransforms.Kind[]{GENERATED, HANDHELD, BOW, ROD}) {
      var pose = new Matrix(); HeldItemTransforms.normalizeNative(pose, kind);
      // Native class_556 / StationAPI ArsenicOverlayRenderer's complete sprite bake.
      pose.translate(0, -.3f, 0); pose.scale(1.5f, 1.5f, 1.5f);
      pose.rotate(50, 0, 1, 0); pose.rotate(335, 0, 0, 1); pose.translate(-.9375f, -.0625f, 0);
      for (float x : new float[]{0, 1}) for (float y : new float[]{0, 1}) for (float z : new float[]{0, -1 / 16f})
        assertArrayEquals(kind == GENERATED ? new double[]{x - .5, y - .5, z + 1 / 32.0}
            : new double[]{.5 - x, y - .5, -z - 1 / 32.0}, pose.point(x, y, z), EPSILON);
    }
    var block = new Matrix(); HeldItemTransforms.normalizeNative(block, BLOCK);
    assertArrayEquals(new double[]{.5, -.5, .5}, block.point(.5, -.5, .5), EPSILON);
  }

  @Test void handheldTexturePixelsMatchModernModelCoordinatesInBothHandsAndViews() {
    for (var kind : new HeldItemTransforms.Kind[]{HANDHELD, BOW})
      for (boolean firstPerson : new boolean[]{true, false}) for (int side : new int[]{-1, 1}) {
        var actual = new Matrix(); HeldItemTransforms.display(actual, kind, firstPerson, side);
        HeldItemTransforms.normalizeNative(actual, kind);
        // Beta's actual baked quad: model X = 1-U, model Y = 1-V.
        actual.translate(0, -.3f, 0); actual.scale(1.5f, 1.5f, 1.5f);
        actual.rotate(50, 0, 1, 0); actual.rotate(335, 0, 0, 1); actual.translate(-.9375f, -.0625f, 0);

        // Independent reference: official display JSON applied to centered modern
        // generated geometry. Its texture U increases with model X on both faces.
        var modern = new Matrix();
        if (firstPerson) {
          modern.translate(side * 1.13f / 16, 3.2f / 16, 1.13f / 16);
          modern.rotate(-90, 0, 1, 0); modern.rotate(25, 0, 0, 1); modern.scale(.68f, .68f, .68f);
        } else if (kind == HANDHELD) {
          modern.translate(0, 4f / 16, .5f / 16);
          modern.rotate(-90, 0, 1, 0); modern.rotate(55, 0, 0, 1); modern.scale(.85f, .85f, .85f);
        } else {
          modern.translate(-side / 16f, -2f / 16, 2.5f / 16);
          modern.rotate(-80, 1, 0, 0); modern.rotate(side < 0 ? 280 : 260, 0, 1, 0);
          modern.rotate(-40, 0, 0, 1); modern.scale(.9f, .9f, .9f);
        }
        // Include diagonal handle/tip pixels and the opposite corners. A centered
        // bounding-box test alone cannot detect the native sprite's reversed U.
        for (double u : new double[]{1.5 / 16, 14.5 / 16}) for (double v : new double[]{1.5 / 16, 14.5 / 16})
          for (double z : new double[]{0, -1 / 16.0})
            assertArrayEquals(modern.point(u - .5, .5 - v, -z - 1 / 32.0),
                actual.point(1 - u, 1 - v, z), EPSILON, kind + " texel orientation");
      }
  }

  @Test void fishingAttachmentUsesTheSameRenderedTipThroughEquipAndIndependentSwing() {
    for (int side : new int[]{-1, 1}) for (float swing : new float[]{0, .15f, .5f, .9f})
      for (float equip : new float[]{0, .3f, 1}) {
        var pose = new Matrix(); HeldItemTransforms.firstPersonHand(pose, side, swing, equip);
        HeldItemTransforms.display(pose, ROD, true, side); HeldItemTransforms.normalizeNative(pose, ROD);
        pose.translate(0, -.3f, 0); pose.scale(1.5f, 1.5f, 1.5f);
        pose.rotate(50, 0, 1, 0); pose.rotate(335, 0, 0, 1); pose.translate(-.9375f, -.0625f, 0);
        var tip = HeldItemTransforms.firstPersonRodTip(side, swing, equip);
        assertArrayEquals(pose.point(1 - 14.5 / 16, 1 - 1.5 / 16, 0), new double[]{tip.x(), tip.y(), tip.z()}, EPSILON);
      }
    var idle = HeldItemTransforms.firstPersonRodTip(-1, 0, 1);
    assertTrue(idle.y() > -.1 && idle.z() < -.7, "modern rod tip extends upward and away from its hand");
  }

  @Test void idleFirstPersonHandHasNoExtraYawAndEquipOnlyLowersItsOwnHand() {
    for (int side : new int[]{-1, 1}) {
      var raised = new Matrix(); HeldItemTransforms.firstPersonHand(raised, side, 0, 1);
      assertArrayEquals(new double[]{side * .56, -.52, -.72}, raised.point(0, 0, 0), EPSILON);
      assertArrayEquals(new double[]{side * .56 + 1, -.52, -.72}, raised.point(1, 0, 0), EPSILON);
      var lowered = new Matrix(); HeldItemTransforms.firstPersonHand(lowered, side, 0, 0);
      assertArrayEquals(new double[]{side * .56, -1.12, -.72}, lowered.point(0, 0, 0), EPSILON);
    }
  }

  @Test void generatedToolsAndRodsUseTheirDistinctOfficialLeftHandOrientations() {
    double sin = Math.sin(Math.toRadians(25)) * .68, cos = Math.cos(Math.toRadians(25)) * .68;
    for (var kind : new HeldItemTransforms.Kind[]{GENERATED, HANDHELD, BOW, ROD}) {
      var pose = new Matrix(); HeldItemTransforms.display(pose, kind, true, -1);
      double[] origin = kind == ROD ? new double[]{0, .1, .05} : new double[]{-1.13 / 16, .2, 1.13 / 16};
      assertArrayEquals(origin, pose.point(0, 0, 0), EPSILON);
      double y = kind == GENERATED ? -sin : sin;
      double z = kind == HANDHELD || kind == BOW ? cos : -cos;
      assertArrayEquals(new double[]{origin[0], origin[1] + y, origin[2] + z}, pose.point(1, 0, 0), EPSILON);
    }
    var block = new Matrix(); HeldItemTransforms.display(block, BLOCK, true, -1);
    double diagonal = Math.sqrt(.08);
    assertArrayEquals(new double[]{-diagonal, 0, -diagonal}, block.point(1, 0, 0), EPSILON);
  }

  @Test void thirdPersonHandFrameAndItemOriginsMatchModernAdultLeftHandValues() {
    var hand = new Matrix(); HeldItemTransforms.thirdPersonHand(hand, -1);
    assertArrayEquals(new double[]{1 / 16.0, 10 / 16.0, -2 / 16.0}, hand.point(0, 0, 0), EPSILON);
    var kinds = new HeldItemTransforms.Kind[]{GENERATED, HANDHELD, BOW, ROD, BLOCK};
    double[][] origins = {{0, 3 / 16.0, 1 / 16.0}, {0, 4 / 16.0, .5 / 16}, {1 / 16.0, -2 / 16.0, 2.5 / 16},
        {0, 4 / 16.0, 2.5 / 16}, {0, 2.5 / 16, 0}};
    double[] scales = {.55, .85, .9, .85, .375};
    for (int i = 0; i < kinds.length; i++) {
      var pose = new Matrix(); HeldItemTransforms.display(pose, kinds[i], false, -1);
      assertArrayEquals(origins[i], pose.point(0, 0, 0), EPSILON);
      assertEquals(Math.pow(scales[i], 3), pose.determinant(), EPSILON,
          "left-hand displays preserve face winding and native texture orientation");
    }
  }

  /** CPU matrix evaluation tests renderer coordinates without creating an OpenGL context. */
  private static final class Matrix implements HeldItemTransforms.Pose {
    double[] matrix = identity();
    private static double[] identity() { return new double[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}; }
    private void multiply(double[] next) {
      double[] result = new double[16];
      for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++) for (int k = 0; k < 4; k++)
        result[row * 4 + col] += matrix[row * 4 + k] * next[k * 4 + col];
      matrix = result;
    }
    public void translate(float x, float y, float z) {
      double[] next = identity(); next[3] = x; next[7] = y; next[11] = z; multiply(next);
    }
    public void scale(float x, float y, float z) {
      double[] next = identity(); next[0] = x; next[5] = y; next[10] = z; multiply(next);
    }
    public void rotate(float angle, float x, float y, float z) {
      double c = Math.cos(Math.toRadians(angle)), s = Math.sin(Math.toRadians(angle)), t = 1 - c;
      multiply(new double[]{t*x*x+c, t*x*y-s*z, t*x*z+s*y, 0,
          t*x*y+s*z, t*y*y+c, t*y*z-s*x, 0,
          t*x*z-s*y, t*y*z+s*x, t*z*z+c, 0, 0, 0, 0, 1});
    }
    double[] point(double x, double y, double z) {
      return new double[]{matrix[0]*x+matrix[1]*y+matrix[2]*z+matrix[3],
          matrix[4]*x+matrix[5]*y+matrix[6]*z+matrix[7], matrix[8]*x+matrix[9]*y+matrix[10]*z+matrix[11]};
    }
    double determinant() {
      return matrix[0]*(matrix[5]*matrix[10]-matrix[6]*matrix[9])
          -matrix[1]*(matrix[4]*matrix[10]-matrix[6]*matrix[8])+matrix[2]*(matrix[4]*matrix[9]-matrix[5]*matrix[8]);
    }
  }
}
