package local.luke.power.render;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class FrameSwapScopeTest {
  @Test void onlyCappedNativeUpdateSwapsWaitForFrameEnd(){
    var scope=new FrameSwapScope();scope.begin(true);assertFalse(scope.defer());
    scope.enterUpdate();assertTrue(scope.defer());scope.exitUpdate();assertFalse(scope.defer());
    assertTrue(scope.finish());assertFalse(scope.finish());assertFalse(scope.defer());
    scope.begin(false);scope.enterUpdate();assertFalse(scope.defer());scope.exitUpdate();assertFalse(scope.finish());
  }
  @Test void nestedRequestsCoalesceAndClosingAnInnerCallbackKeepsTheOuterScope(){
    var scope=new FrameSwapScope();scope.begin(true);scope.enterUpdate();scope.enterUpdate();assertTrue(scope.defer());
    scope.exitUpdate();assertTrue(scope.defer());scope.exitUpdate();assertFalse(scope.defer());assertTrue(scope.finish());
  }
  @Test void unrelatedThreadsCannotDeferOrConsumeTheMainThreadSwap()throws Exception{
    var scope=new FrameSwapScope();scope.begin(true);scope.enterUpdate();assertTrue(scope.defer());
    var changed=new AtomicBoolean();var other=new Thread(()->{scope.enterUpdate();changed.set(scope.defer()||scope.finish());scope.exitUpdate();scope.cancel();});
    other.start();other.join();assertFalse(changed.get());scope.exitUpdate();assertTrue(scope.finish());
  }
  @Test void exceptionsAndLaterFramesCannotReplayStalePendingSwaps(){
    var scope=new FrameSwapScope();scope.begin(true);scope.enterUpdate();assertTrue(scope.defer());scope.cancel();scope.exitUpdate();assertFalse(scope.finish());
    scope.begin(true);scope.enterUpdate();assertTrue(scope.defer());scope.begin(true);assertFalse(scope.defer());assertFalse(scope.finish());
  }
}
