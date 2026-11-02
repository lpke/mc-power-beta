package local.luke.power.render;

/** One render loop may defer only swaps requested inside its native display-update callbacks. */
final class FrameSwapScope {
  private Thread owner;
  private boolean enabled,pending;
  private int depth;
  void begin(boolean limited){owner=Thread.currentThread();enabled=limited;pending=false;depth=0;}
  void enterUpdate(){if(enabled&&owner==Thread.currentThread())depth++;}
  void exitUpdate(){if(owner==Thread.currentThread()&&depth>0)depth--;}
  boolean defer(){
    if(!enabled||owner!=Thread.currentThread()||depth==0)return false;
    pending=true;return true;
  }
  boolean finish(){
    if(owner!=Thread.currentThread())return false;
    boolean swap=pending;clear();return swap;
  }
  void cancel(){if(owner==null||owner==Thread.currentThread())clear();}
  private void clear(){owner=null;enabled=pending=false;depth=0;}
}
