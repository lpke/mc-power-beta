package local.luke.power.worldedit.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

class EditorTest {
  static class Memory implements WorldAccess {
    Map<Pos, BlockValue> data = new HashMap<>();
    Pos unloaded, fail;
    boolean once;

    public boolean loaded(Pos p) {
      return !p.equals(unloaded);
    }

    public BlockValue get(Pos p) {
      return data.getOrDefault(p, BlockValue.AIR);
    }

    public void set(Pos p, BlockValue b) {
      data.put(p, b);
      if (p.equals(fail) && !once) {
        once = true;
        throw new IllegalStateException("simulated write failure");
      }
    }
  }

  Memory w;
  Editor e;
  Commands commands;
  List<String> messages;
  Pos feet = new Pos(0, 64, 0);

  @BeforeEach
  void setup() {
    w = new Memory();
    messages = new ArrayList<>();
    e = new Editor(w, new BlockParser(id -> id >= 1 && id <= 96), messages::add);
    commands =
        new Commands(
            e,
            new Commands.Player() {
              public Pos feet() {
                return feet;
              }

              public Pos target() {
                return new Pos(1, 64, 1);
              }

              public Pos facing() {
                return new Pos(0, 0, 1);
              }

              public void wand() {}

              public void toggleWand() {}

              public void drawSelection() {}
            });
    e.select(Region.between(new Pos(0, 64, 0), new Pos(2, 66, 2)));
  }

  void run(String c) {
    commands.run(c);
    drain();
  }

  void drain() {
    int ticks = 0;
    while (e.engine.busy()) {
      e.engine.tick(7);
      assertTrue(++ticks < 100000);
    }
  }

  @Test
  void fillReplaceUndoRedoMetadataAndContainers() {
    Pos p = new Pos(1, 65, 1);
    BlockValue chest = new BlockValue(54, 2, new byte[] {1, 2, 3});
    w.data.put(p, chest);
    run("//set 44:2");
    assertEquals(27, w.data.values().stream().filter(b -> b.id == 44 && b.meta == 2).count());
    run("//undo");
    assertTrue(w.get(p).same(chest));
    run("//redo");
    assertEquals(44, w.get(p).id);
    run("//replace 44:2 43:2");
    assertEquals(43, w.get(p).id);
    run("//undo 2");
    assertTrue(w.get(p).same(chest));
    run("//redo 2");
    assertEquals(43, w.get(p).id);
  }

  @Test
  void preflightLimitAndUnloadedDoNotPartiallyWrite() {
    e.engine.configure(5, 20);
    run("//set stone");
    assertTrue(w.data.isEmpty());
    assertTrue(messages.stream().anyMatch(s -> s.contains("No blocks changed")));
    e.engine.configure(100, 20);
    w.unloaded = new Pos(2, 66, 2);
    run("//set stone");
    assertTrue(w.data.isEmpty());
  }

  @Test
  void cancelRestoresWrittenBlocks() {
    commands.run("//set stone");
    e.engine.tick(30);
    assertFalse(w.data.isEmpty());
    e.engine.cancel();
    drain();
    assertTrue(w.data.values().stream().allMatch(b -> b.id == 0));
    assertEquals(0, e.engine.undoSize());
  }

  @Test
  void writeFailureRestoresIncludingFailingWrite() {
    w.fail = new Pos(1, 64, 0);
    run("//set stone");
    assertTrue(w.data.values().stream().allMatch(b -> b.id == 0));
    assertEquals(0, e.engine.undoSize());
  }

  @Test
  void wallsFacesCenterAndHollow() {
    run("//walls stone");
    assertEquals(24, w.data.values().stream().filter(b -> b.id == 1).count());
    run("//set air");
    run("//faces stone");
    assertEquals(26, w.data.values().stream().filter(b -> b.id == 1).count());
    run("//center gold_block");
    assertEquals(41, w.get(new Pos(1, 65, 1)).id);
    run("//hollow");
    assertEquals(0, w.get(new Pos(1, 65, 1)).id);
  }

  @Test
  void clipboardOriginRotationSkipAirAndOverlap() {
    run("//set air");
    w.data.put(new Pos(1, 64, 0), new BlockValue(53, 0));
    run("//copy");
    feet = new Pos(10, 64, 0);
    run("//rotate 90");
    run("//paste -a");
    assertEquals(53, w.get(new Pos(10, 64, 1)).id);
    assertEquals(2, w.get(new Pos(10, 64, 1)).meta);
    e.select(Region.between(new Pos(0, 64, 0), new Pos(2, 64, 0)));
    run("//move 1 east");
    assertEquals(53, w.get(new Pos(2, 64, 0)).id);
    assertEquals(0, w.get(new Pos(1, 64, 0)).id);
    run("//undo");
    assertEquals(53, w.get(new Pos(1, 64, 0)).id);
  }

  @Test
  void relativeCoordinatesAndSelectionTransforms() {
    run("//pos1 ~-1,~0,~2");
    assertEquals(new Pos(-1, 64, 2), e.pos1);
    run("//pos2 1 66 4");
    run("//expand 2 1 up");
    assertEquals(63, e.region().min().y());
    assertEquals(68, e.region().max().y());
    run("//contract 2 1 down");
    assertEquals(64, e.region().min().y());
    assertEquals(66, e.region().max().y());
    run("//shift 2 east");
    assertEquals(1, e.region().min().x());
    run("//expand vert");
    assertEquals(128, e.region().sizeY());
  }

  @Test
  void fillVersusRecursiveUndercut() {
    feet = new Pos(0, 65, 0);
    for (int x = -2; x <= 2; x++)
      for (int z = -2; z <= 2; z++)
        for (int y = 63; y <= 65; y++) w.data.put(new Pos(x, y, z), new BlockValue(1, 0));
    w.data.put(feet, BlockValue.AIR);
    w.data.put(new Pos(0, 64, 0), BlockValue.AIR);
    w.data.put(new Pos(1, 64, 0), BlockValue.AIR);
    run("//fill glass 2 2");
    assertEquals(20, w.get(new Pos(0, 64, 0)).id);
    assertEquals(0, w.get(new Pos(1, 64, 0)).id);
    run("//undo");
    run("//fillr glass 2 2");
    assertEquals(20, w.get(new Pos(1, 64, 0)).id);
    assertEquals(1, w.get(new Pos(0, 63, 0)).id);
  }

  @Test
  void shapesAndMasks() {
    run("//gmask air");
    run("//sphere stone 2");
    assertEquals(1, w.get(feet).id);
    assertEquals(0, w.get(feet.add(2, 2, 2)).id);
    run("//gmask");
    run("//undo");
    run("//hcyl glass 2 3");
    assertEquals(0, w.get(feet).id);
    assertEquals(20, w.get(feet.add(2, 2, 0)).id);
  }

  @Test
  void namedMetadataAliasesAreExactMasks() {
    assertEquals(14, e.blocks.block("red_wool").meta);
    assertEquals(2, e.blocks.block("oak_slab").meta);
    assertTrue(e.blocks.mask("red_wool").test(new BlockValue(35, 14)));
    assertFalse(e.blocks.mask("red_wool").test(new BlockValue(35, 0)));
    assertTrue(e.blocks.mask("wool").test(new BlockValue(35, 14)));
  }

  @Test
  void invalidFlagsCannotQueueEdits() {
    assertThrows(IllegalArgumentException.class, () -> commands.run("//set -z stone"));
    assertFalse(e.engine.busy());
    assertThrows(IllegalArgumentException.class, () -> commands.run("//paste -e"));
    assertFalse(e.engine.busy());
  }

  @Test
  void invalidBlocksBoundsAndArguments() {
    for (String s : List.of("observer", "36", "95", "256", "1:16", "-1", "stone[]"))
      assertThrows(IllegalArgumentException.class, () -> e.blocks.block(s));
    assertThrows(IllegalArgumentException.class, () -> commands.run("//pos1 0,128,0"));
    assertThrows(IllegalArgumentException.class, () -> commands.run("//sphere stone NaN"));
    assertThrows(IllegalArgumentException.class, () -> commands.run("//fill glass 2 -1"));
  }

  @Test
  void weightedPatternsAndMetadataMasks() {
    var pattern = e.blocks.pattern("75%stone,25%44:2");
    int a = 0, b = 0;
    for (int x = 0; x < 10000; x++) {
      BlockValue v = pattern.apply(new Pos(x, 64, 0));
      if (v.id == 1) a++;
      else if (v.id == 44 && v.meta == 2) b++;
    }
    assertTrue(a > 6500 && a < 8500);
    assertEquals(10000, a + b);
    assertTrue(e.blocks.mask("44").test(new BlockValue(44, 3)));
    assertFalse(e.blocks.mask("44:2").test(new BlockValue(44, 3)));
    assertTrue(e.blocks.mask("!air").test(new BlockValue(44, 3)));
  }

  @Test
  void rotationRoundTripsAllMetadata() {
    for (int id :
        List.of(
            23, 26, 29, 33, 53, 54, 61, 62, 63, 64, 65, 66, 67, 68, 69, 71, 77, 86, 91, 93, 94, 96))
      for (int m = 0; m < 16; m++) {
        BlockValue start = new BlockValue(id, m), b = start;
        for (int n = 0; n < 4; n++) b = BlockRotation.rotate(b);
        assertEquals(start.meta, b.meta, "id=" + id + " meta=" + m);
      }
  }

  @Test
  void historyMemoryAndStepBounds() {
    e.engine.configure(100, 2);
    run("//set stone");
    run("//set dirt");
    run("//set glass");
    assertEquals(2, e.engine.undoSize());
    assertThrows(IllegalArgumentException.class, () -> commands.run("//undo 3"));
    run("//undo 2");
    assertEquals(1, w.get(feet).id);
    run("//set wool:3");
    assertThrows(IllegalArgumentException.class, () -> commands.run("//redo"));
  }

  @Test
  void volumeAndNegativeChunkCoordinates() {
    Region huge =
        Region.between(new Pos(-31999999, 0, -31999999), new Pos(31999999, 127, 31999999));
    assertTrue(huge.volume() > Integer.MAX_VALUE);
    feet = new Pos(-1, 64, -17);
    run("//chunk");
    assertEquals(new Pos(-16, 0, -32), e.region().min());
  }
  @Test
  void contractDirectionsAndFaceAliases() {
    for (String direction : List.of("down", "top", "up", "bottom")) {
      e.select(new Region(new Pos(0, 64, 0), new Pos(4, 70, 4)));
      run("//contract 1 " + direction);
      boolean top = direction.equals("down") || direction.equals("top");
      assertEquals(top ? 64 : 65, e.region().min().y());
      assertEquals(top ? 69 : 70, e.region().max().y());
    }
    e.select(new Region(new Pos(0, 64, 0), new Pos(4, 70, 4)));
    run("//contract 1 east"); assertEquals(1, e.region().min().x()); assertEquals(4, e.region().max().x());
    Region before = e.region();
    assertThrows(IllegalArgumentException.class, () -> run("//contract 100 down"));
    assertEquals(before, e.region());
  }
}
