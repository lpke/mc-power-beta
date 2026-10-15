package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.FutureTask;

class RenderedViewTest {
  private final Object world = new Object(), camera = new Object();

  @Test void invertsRotationTranslationAndNonUniformPortalScaling() {
    // x' = 2z-6, y' = 3y-12, z' = -4x+8, origin is (2,4,3).
    float[] view={0,0,-4,0, 0,3,0,0, 2,0,0,0, -6,-12,8,1};
    RenderedView.capture(world, camera, view, 100, 50, -100);
    assertTrue(RenderedView.valid(world, camera));
    assertEquals(102, RenderedView.x(), 1e-8);
    assertEquals(54, RenderedView.y(), 1e-8);
    assertEquals(-97, RenderedView.z(), 1e-8);
  }

  @Test void rejectsStaleOwnersThreadsAndInvalidMatrices() throws Exception {
    float[] view={1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
    RenderedView.capture(world, camera, view, 0, 0, 0);
    assertFalse(RenderedView.valid(new Object(), camera));
    assertFalse(RenderedView.valid(world, new Object()));
    var worker=new FutureTask<>(() -> RenderedView.valid(world, camera));
    new Thread(worker).start(); assertFalse(worker.get());
    view[0]=Float.NaN;
    RenderedView.capture(world, camera, view, 0, 0, 0);
    assertFalse(RenderedView.valid(world, camera));
    view[0]=0;
    RenderedView.capture(world, camera, view, 0, 0, 0);
    assertFalse(RenderedView.valid(world, camera));
    view[0]=1;
    RenderedView.capture(world, camera, view, 0, 0, 0);
    RenderedView.clear(); assertFalse(RenderedView.valid(world, camera));
  }
}
