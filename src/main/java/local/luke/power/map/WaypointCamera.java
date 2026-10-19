package local.luke.power.map;

import java.nio.FloatBuffer;

/** Removes only the captured walking bob from a completed camera transform. */
public final class WaypointCamera {
  private final double[] before = new double[16], inverse = new double[16], correction = new double[16];
  private boolean valid;

  public void reset() { valid = false; }
  public void before(FloatBuffer matrix) {
    reset();
    for (int i = 0; i < 16; i++) before[i] = matrix.get(i);
  }

  public void after(FloatBuffer matrix) {
    valid = invert(matrix, inverse);
    if (!valid) return;
    for (int column = 0; column < 4; column++) for (int row = 0; row < 4; row++) {
      double value = 0;
      for (int k = 0; k < 4; k++) value += before[k * 4 + row] * inverse[column * 4 + k];
      correction[column * 4 + row] = value;
      if (!Double.isFinite(value)) valid = false;
    }
  }

  public void apply(FloatBuffer model, FloatBuffer output) {
    for (int column = 0; column < 4; column++) for (int row = 0; row < 4; row++) {
      double value = model.get(column * 4 + row);
      if (valid) {
        value = 0;
        for (int k = 0; k < 4; k++) value += correction[k * 4 + row] * model.get(column * 4 + k);
      }
      output.put(column * 4 + row, (float)value);
    }
  }

  private static boolean invert(FloatBuffer m, double[] result) {
    for (int k = 0; k < 16; k++) if (!Float.isFinite(m.get(k))) return false;
    if (Math.abs(m.get(3)) > 1e-6 || Math.abs(m.get(7)) > 1e-6
        || Math.abs(m.get(11)) > 1e-6 || Math.abs(m.get(15) - 1) > 1e-6) return false;
    double a=m.get(0), b=m.get(4), c=m.get(8), d=m.get(1), e=m.get(5), f=m.get(9),
        g=m.get(2), h=m.get(6), i=m.get(10);
    double det=a*(e*i-f*h)-b*(d*i-f*g)+c*(d*h-e*g);
    if (!Double.isFinite(det) || Math.abs(det)<1e-12) return false;
    result[0]=(e*i-f*h)/det; result[4]=(c*h-b*i)/det; result[8]=(b*f-c*e)/det;
    result[1]=(f*g-d*i)/det; result[5]=(a*i-c*g)/det; result[9]=(c*d-a*f)/det;
    result[2]=(d*h-e*g)/det; result[6]=(b*g-a*h)/det; result[10]=(a*e-b*d)/det;
    result[3]=result[7]=result[11]=0; result[15]=1;
    for (int row=0;row<3;row++) result[12+row]=-(result[row]*m.get(12)+result[4+row]*m.get(13)+result[8+row]*m.get(14));
    return true;
  }
}
