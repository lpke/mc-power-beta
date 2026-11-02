package local.luke.power.render;

import local.luke.power.controls.util.ModOptions;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

/** Capped frames swap after the existing limiter, retaining native polling, resize work and callbacks. */
public final class FramePresentation {
  private static final FrameSwapScope swaps=new FrameSwapScope();
  private FramePresentation() {}
  public static void beginFrame(){swaps.begin(ModOptions.isFramerateLimited());}
  public static void beginUpdate(){swaps.enterUpdate();}
  public static void endUpdate(){swaps.exitUpdate();}
  public static boolean deferSwap(){return swaps.defer();}
  public static void abortFrame(){swaps.cancel();}
  public static void finishFrame(){
    if(!swaps.finish())return;
    // Finish clears the scope before this call, so the deferred swap cannot defer itself.
    try{Display.swapBuffers();}catch(LWJGLException failure){throw new RuntimeException(failure);}
  }
}
