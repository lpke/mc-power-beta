package local.luke.power.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import org.junit.jupiter.api.Test;

class BtaParityTest {
  @Test
  void matchesOfficialReleaseAcrossRandomStatesAndInputs() {
    BoatMotion port = new BoatMotion();
    BtaBoatReference bta = new BtaBoatReference();
    Random random = new Random(801);
    for (int i = 0; i < 100_000; i++) {
      bta.xd = random.nextDouble() * 4 - 2;
      bta.zd = random.nextDouble() * 4 - 2;
      bta.yRot = random.nextFloat() * 7200 - 3600;
      bta.forward = random.nextInt(3) - 1;
      bta.strafe = random.nextInt(3) - 1;
      boolean occupied = random.nextBoolean();
      bta.passenger = occupied ? bta.new Rider() : null;
      port.step(bta.xd, bta.zd, bta.yRot, bta.forward, bta.strafe, occupied);
      bta.boatMovement();
      assertEquals(bta.xd, port.x, 1e-12, "X at state " + i);
      assertEquals(bta.zd, port.z, 1e-12, "Z at state " + i);
      assertEquals(bta.yRot, port.yaw, "yaw at state " + i);
    }
  }
}
