package local.luke.power.camera.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CameraPoseTest {
    @Test void reconstructsWalkingViewsAcrossPitchYawAndRoll() {
        for (double pitch : new double[] {-89, -30, 0, 47, 89})
            for (double yaw : new double[] {-360, -179, -90, 0, 90, 179, 540})
                for (double roll : new double[] {-4, 0, 6}) roundTrip(pitch, yaw, roll, 12.3, 70.4, -23.8);
    }

    @Test void verticalViewsRemainFiniteAndKeepTheSameTransform() {
        for (double pitch : new double[] {-90, 90})
            for (double yaw : new double[] {-179, -90, 0, 90, 179}) roundTrip(pitch, yaw, 3, 5, 101, 7);
    }

    @Test void retainsSmallOffsetsAtLargeWorldCoordinates() {
        roundTrip(12, 250, -2, 29_999_999.9, 127.01, -29_999_999.7);
    }

    private static void roundTrip(double pitch, double yaw, double roll, double x, double y, double z) {
        float[] view = view(pitch, yaw, roll, .14, -.23, .1);
        var pose = CameraPose.fromView(view, x, y, z, (float)yaw);
        float[] rebuilt = view(pose.pitch, pose.yaw, pose.roll, x - pose.x, y - pose.y, z - pose.z);
        for (int i = 0; i < 16; i++) assertEquals(view[i], rebuilt[i], 2e-6, "matrix component " + i);
    }

    // OpenGL postmultiplies roll, pitch and yaw, then the relative translation.
    private static float[] view(double pitch, double yaw, double roll, double x, double y, double z) {
        double a=Math.toRadians(pitch), b=Math.toRadians(yaw), c=Math.toRadians(-roll);
        double ca=Math.cos(a),sa=Math.sin(a),cb=Math.cos(b),sb=Math.sin(b),cc=Math.cos(c),sc=Math.sin(c);
        double[][] r={
            {cc*cb-sc*sa*sb,-sc*ca,cc*sb+sc*sa*cb},
            {sc*cb+cc*sa*sb,cc*ca,sc*sb-cc*sa*cb},
            {-ca*sb,sa,ca*cb}};
        float[] m=new float[16]; m[15]=1;
        for(int row=0;row<3;row++) {
            for(int col=0;col<3;col++) m[col*4+row]=(float)r[row][col];
            m[12+row]=(float)(r[row][0]*x+r[row][1]*y+r[row][2]*z);
        }
        return m;
    }
}
