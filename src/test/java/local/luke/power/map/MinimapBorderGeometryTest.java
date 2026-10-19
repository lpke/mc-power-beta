package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class MinimapBorderGeometryTest {
  private static final double EPSILON = 1e-4;
  private record Vertex(float x, float y, float u, float v) {}
  private record Quad(Vertex a, Vertex b, Vertex c, Vertex d) {
    double left() { return Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x)); }
    double right() { return Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x)); }
    double top() { return Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y)); }
    double bottom() { return Math.max(Math.max(a.y, b.y), Math.max(c.y, d.y)); }
    boolean covers(double x, double y) { return x >= left() && x < right() && y >= top() && y < bottom(); }
    List<Vertex> vertices() { return List.of(a, b, c, d); }
  }

  @Test void bundledAtlasIsTheExactDownloadedReferencePng() throws Exception {
    // Xaero Minimap26.6.0: assets/xaerobetterpvp/gui/minimap_frame.png, verified against the source JAR.
    try (var input = getClass().getResourceAsStream("/assets/powerbeta/map/minimap-frame.png")) {
      assertNotNull(input); byte[] bytes = input.readAllBytes();
      assertEquals("d634558d0cfc3ad9736125000f82f43c80bc7c2182df5b49ea6cf5c6a0a39744",
          HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
      var image = ImageIO.read(new ByteArrayInputStream(bytes));
      assertNotNull(image); assertEquals(256, image.getWidth()); assertEquals(256, image.getHeight());
    }
  }

  @Test void squareCornersAndStraightPiecesUseExactReferenceAtlasExtents() {
    var mesh = mesh(108, false); assertEquals(8, mesh.size());
    rectangle(mesh, -4, -4, 16, 16, 192, 97);
    rectangle(mesh, 96, -4, 16, 16, 192, 113);
    rectangle(mesh, -4, 96, 16, 16, 192, 129);
    rectangle(mesh, 96, 96, 16, 16, 192, 145);
    rectangle(mesh, 12, -4, 84, 16, 0, 0);
    rectangle(mesh, 12, 96, 84, 16, 0, 16);
    rectangle(mesh, -4, 12, 16, 84, 0, 97);
    rectangle(mesh, 96, 12, 16, 84, 16, 97);
  }

  @Test void largeSquareRepeatsAndCroppedLastPiecesMeetWithoutGapsOverlapsOrAtlasBleed() {
    var mesh = mesh(512, false); assertEquals(20, mesh.size());
    rectangle(mesh, 12, -4, 226, 16, 0, 0);
    rectangle(mesh, 238, -4, 226, 16, 0, 0);
    rectangle(mesh, 464, -4, 36, 16, 0, 0);
    rectangle(mesh, -4, 125, 16, 113, 32, 97);
    rectangle(mesh, -4, 238, 16, 113, 0, 97);
    rectangle(mesh, -4, 464, 16, 36, 0, 97);
    rectangle(mesh, 500, 464, 16, 36, 16, 97);
    for (int y = -4; y < 516; y++) for (int x = -4; x < 516; x++) {
      boolean interior = x >= 12 && x < 500 && y >= 12 && y < 500;
      int covers = 0;
      for (var quad : mesh) if (quad.covers(x + .5, y + .5)) covers++;
      assertEquals(interior ? 0 : 1, covers, "square seam at " + x + "," + y);
    }
  }

  @Test void circularFrameClosesAtEverySizeWithAnExactFourPixelThicknessAndOneAtlasRow() {
    for (int diameter : new int[]{24, 64, 108, 240, 480, 512}) {
      var mesh = mesh(diameter, true);
      assertTrue(mesh.size() >= 32); assertEquals(0, mesh.size() % 4);
      for (int i = 0; i < mesh.size(); i++) {
        var quad = mesh.get(i); var next = mesh.get((i + 1) % mesh.size());
        point(quad.b, next.a); point(quad.c, next.d);
        for (var inner : List.of(quad.a, quad.b)) {
          assertEquals(diameter / 2d, Math.hypot(inner.x - diameter / 2d, inner.y - diameter / 2d), EPSILON);
          assertEquals(214 / 256d, inner.v);
        }
        for (var outer : List.of(quad.c, quad.d)) {
          assertEquals(diameter / 2d + 4, Math.hypot(outer.x - diameter / 2d, outer.y - diameter / 2d), EPSILON);
          assertEquals(210 / 256d, outer.v);
        }
        for (var vertex : quad.vertices()) assertTrue(vertex.u >= 0 && vertex.u <= 226 / 256d);
      }
    }
  }

  @Test void circularShadeTransitionsRetainTheReferenceForwardReverseAndClosureSeams() {
    var mesh = mesh(108, true); assertEquals(48, mesh.size());
    // Pixel U values sampled from the reference renderer at its seam and shade transitions.
    assertEquals(0, mesh.get(0).a.u * 256d);
    assertEquals(7.592182159423828, mesh.get(0).b.u * 256d, EPSILON);
    assertEquals(33.290550231933594, mesh.get(13).b.u * 256d, EPSILON);
    assertEquals(138, mesh.get(14).a.u * 256d);
    assertEquals(145.59217834472656, mesh.get(14).b.u * 256d, EPSILON);
    assertEquals(161.10618591308594, mesh.get(25).b.u * 256d, EPSILON);
    assertEquals(221.51400756835938, mesh.get(26).a.u * 256d, EPSILON);
    assertEquals(138, mesh.get(37).b.u * 256d);
    assertEquals(69.5029296875, mesh.get(38).a.u * 256d, EPSILON);
    assertEquals(72.42474365234375, mesh.get(47).b.u * 256d, EPSILON);
  }

  @Test void unsupportedDiametersAreRejectedBeforeAllocatingGeometry() {
    for (int diameter : new int[]{Integer.MIN_VALUE, 0, 23, 513, Integer.MAX_VALUE})
      for (boolean circle : new boolean[]{false, true})
        assertThrows(IllegalArgumentException.class, () -> MinimapBorderGeometry.create(diameter, circle));
  }

  private static List<Quad> mesh(int diameter, boolean circle) {
    float[] values = MinimapBorderGeometry.create(diameter, circle);
    assertEquals(0, values.length % 16); var result = new ArrayList<Quad>();
    for (int i = 0; i < values.length; i += 16) {
      Vertex[] vertices = new Vertex[4];
      for (int v = 0; v < 4; v++) {
        int offset = i + v * 4;
        for (int p = offset; p < offset + 4; p++) assertTrue(Float.isFinite(values[p]));
        vertices[v] = new Vertex(values[offset], values[offset + 1], values[offset + 2], values[offset + 3]);
        assertTrue(vertices[v].u >= 0 && vertices[v].u <= 1);
        assertTrue(vertices[v].v >= 0 && vertices[v].v <= 1);
      }
      result.add(new Quad(vertices[0], vertices[1], vertices[2], vertices[3]));
    }
    return result;
  }
  private static void rectangle(List<Quad> mesh, int x, int y, int width, int height, int u, int v) {
    var matches = mesh.stream().filter(q -> q.left() == x && q.top() == y && q.right() == x + width && q.bottom() == y + height).toList();
    assertEquals(1, matches.size(), "missing rectangle at " + x + "," + y);
    var quad = matches.get(0);
    assertEquals(new Vertex(x, y + height, u / 256f, (v + height) / 256f), quad.a);
    assertEquals(new Vertex(x + width, y + height, (u + width) / 256f, (v + height) / 256f), quad.b);
    assertEquals(new Vertex(x + width, y, (u + width) / 256f, v / 256f), quad.c);
    assertEquals(new Vertex(x, y, u / 256f, v / 256f), quad.d);
  }
  private static void point(Vertex expected, Vertex actual) {
    assertEquals(expected.x, actual.x, EPSILON); assertEquals(expected.y, actual.y, EPSILON);
  }
}
