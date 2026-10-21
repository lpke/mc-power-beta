package local.luke.power.validation;

/** Observe native ordering without changing the renderer. */
public final class Inventory330Capture {
  public static int frames, phase;
  public static boolean valid=true, tracking;
  public static void start(){phase=0;tracking=true;}
  public static void map(){if(tracking){valid&=phase==0;phase=1;}}
  public static void gui(){if(tracking){valid&=phase==1;frames++;tracking=false;}}
}
