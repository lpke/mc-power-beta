package local.luke.power.transport;

// Test oracle extracted from the official BTA 8.0.1 EntityBoat class.
// Only dependencies are replaced. These two method bodies are unmodified.
final class BtaBoatReference {
  double xd, yd, zd;
  float yRot;
  float pendingYRot;
  double pendingXDChange, pendingZDChange;
  boolean onGround;
  Rider passenger;
  float forward, strafe;

  final class Rider {
    void handleSpecialVehicleControl() {
      controlBoat(forward, strafe);
    }

    void sendSpecialVehiclePacket() {}
  }

  public void boatMovement() {
    double boatRad = Math.toRadians(this.yRot + 90.0f);
    double vectorRad = Math.atan2(this.xd, this.zd);
    if (this.passenger != null) {
      boolean isBackwards = Math.cos(-boatRad - vectorRad) < 0.0;
      double ang = vectorRad + boatRad + (isBackwards ? Math.PI : 0.0);
      double _xd = this.xd;
      this.xd = _xd * Math.cos(ang) - this.zd * Math.sin(ang);
      this.zd = _xd * Math.sin(ang) + this.zd * Math.cos(ang);
    }
    double currentBoatSpeed = Math.hypot(this.xd, this.zd);
    if (this.passenger != null) {
      this.passenger.handleSpecialVehicleControl();
    }
    this.yRot += this.pendingYRot;
    this.pendingYRot = 0.0f;
    double xdOff = this.pendingXDChange;
    double zdOff = this.pendingZDChange;
    this.pendingXDChange = 0.0;
    this.pendingZDChange = 0.0;
    double offsetMagnitude = Math.hypot(xdOff, zdOff);
    double vecAngle =
        Math.acos(
            (this.xd * xdOff + this.zd * zdOff)
                / ((currentBoatSpeed + 1.0E-11) * (offsetMagnitude + 1.0E-11)));
    double multiplier = 1.0 + vecAngle / Math.PI;
    this.xd += (xdOff *= multiplier);
    this.zd += (zdOff *= multiplier);
    if (this.passenger != null) {
      this.passenger.sendSpecialVehiclePacket();
    }
    if ((currentBoatSpeed = Math.hypot(this.xd, this.zd)) > 0.8) {
      this.xd = this.xd / currentBoatSpeed * 0.8;
      this.zd = this.zd / currentBoatSpeed * 0.8;
    }
    if (this.onGround) {
      this.xd *= 0.5;
      this.yd *= 0.5;
      this.zd *= 0.5;
    }
  }

  public void controlBoat(float forward, float strafe) {
    double currentBoatSpeed = Math.hypot(this.xd, this.zd);
    this.pendingYRot =
        (float)
            ((double) strafe * (3.0 + 2.0 * Math.max((0.8 - currentBoatSpeed * 1.5) / 0.8, 0.0)));
    float velOff = (float) ((double) forward * 0.0065);
    this.pendingXDChange = Math.cos(Math.toRadians(this.yRot)) * (double) velOff;
    this.pendingZDChange = Math.sin(Math.toRadians(this.yRot)) * (double) velOff;
  }
}
