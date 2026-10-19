package local.luke.power.sprint;

/** LocalPlayer.isHorizontalCollisionMinor in Java 1.21.1. */
public final class SprintCollision {
  private SprintCollision() {}
  public static boolean minor(float side,float forward,float yaw,double movedX,double movedZ) {
    double radians=yaw*((float)Math.PI/180),sin=Math.sin(radians),cos=Math.cos(radians);
    double wantedX=side*cos-forward*sin,wantedZ=forward*cos+side*sin;
    double wanted=wantedX*wantedX+wantedZ*wantedZ,actual=movedX*movedX+movedZ*movedZ;
    if(wanted<1e-5f || actual<1e-5f)return false;
    double cosine=(wantedX*movedX+wantedZ*movedZ)/Math.sqrt(wanted*actual);
    return Math.acos(Math.max(-1,Math.min(1,cosine)))<0.13962633907794952;
  }
}
