package local.luke.power.worldedit.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HandBlockTest {
  @Test void handKeepsVariantsAndSupportsExplicitMetadata() {
    var held = new AtomicReference<>(new BlockValue(35, 14));
    var parser = new BlockParser(id -> id <= 96, held::get);
    assertTrue(parser.block("hand").same(new BlockValue(35, 14)));
    assertTrue(parser.block("HAND").same(new BlockValue(35, 14)));
    assertTrue(parser.block("hand:3").same(new BlockValue(35, 3)));
    held.set(new BlockValue(44, 2));
    assertTrue(parser.block("hand").same(new BlockValue(44, 2)));
    assertTrue(parser.names().contains("hand"));
  }

  @Test void masksAndWeightedPatternsSnapshotTheHeldVariant() {
    var held = new AtomicReference<>(new BlockValue(35, 14));
    var parser = new BlockParser(id -> id <= 96, held::get);
    var mask = parser.mask("HAND");
    var inverse = parser.mask("!hand");
    var pattern = parser.pattern("50%hand,50%stone");
    held.set(new BlockValue(35, 0));
    assertTrue(mask.test(new BlockValue(35, 14)));
    assertFalse(mask.test(new BlockValue(35, 0)));
    assertFalse(inverse.test(new BlockValue(35, 14)));
    assertTrue(inverse.test(new BlockValue(35, 0)));
    boolean wool = false, stone = false;
    for (int x = 0; x < 1000; x++) {
      var value = pattern.apply(new Pos(x, 64, 0));
      assertTrue(value.same(new BlockValue(35, 14)) || value.same(new BlockValue(1, 0)));
      wool |= value.id == 35; stone |= value.id == 1;
    }
    assertTrue(wool && stone);
  }

  @Test void emptyHandsAndUnsupportedBlocksFailBeforeAnEdit() {
    var held = new AtomicReference<BlockValue>();
    var parser = new BlockParser(id -> id <= 96, held::get);
    assertEquals("Hold a block to use hand.", assertThrows(IllegalArgumentException.class, () -> parser.block("hand")).getMessage());
    for (var value : new BlockValue[] {BlockValue.AIR, new BlockValue(34, 0), new BlockValue(36, 0),
        new BlockValue(95, 0), new BlockValue(97, 0), new BlockValue(256, 0), new BlockValue(35, 16)}) {
      held.set(value);
      assertThrows(IllegalArgumentException.class, () -> parser.pattern("hand"));
    }
    held.set(new BlockValue(35, 14));
    assertThrows(IllegalArgumentException.class, () -> parser.block("hand:16"));
    assertThrows(IllegalArgumentException.class, () -> parser.block("hand:"));
  }
}
