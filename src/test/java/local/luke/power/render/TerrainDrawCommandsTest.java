package local.luke.power.render;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class TerrainDrawCommandsTest {
  @Test void growsForMeshesAndRetainsAllFiveCommandWords() {
    var commands = TerrainDrawCommands.initial();
    assertTrue(commands.isDirect()); assertEquals(ByteOrder.nativeOrder(), commands.order());
    assertEquals(20 * 1024, commands.capacity() * Integer.BYTES);
    for (int draw = 0; draw < 40000; draw++) {
      commands = TerrainDrawCommands.room(commands);
      commands.put(draw * 6).put(1).put(0).put(draw * 4).put(draw);
    }
    assertEquals(40000 * 5, commands.position());
    assertTrue(commands.capacity() < 40000 * 10);
    commands.flip();
    for (int draw = 0; draw < 40000; draw++) {
      assertEquals(draw * 6, commands.get()); assertEquals(1, commands.get());
      assertEquals(0, commands.get()); assertEquals(draw * 4, commands.get());
      assertEquals(draw, commands.get());
    }
    commands.clear();
    for (int i = 0; i < 100; i++) {
      assertSame(commands, TerrainDrawCommands.room(commands));
      commands.put(6).put(1).put(0).put(0).put(i);
    }
  }
}
