package local.luke.power.chat;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatCompletionTest {
  @Test void fuzzyChoicesAreFullTokensRankedClosestToTheInput() {
    assertArrayEquals(new String[]{"plank", "planks", "wooden_planks"},
        ChatCompletion.ranked("plank", List.of("wooden_planks", "planks", "stone", "plank")));
    assertArrayEquals(new String[]{"diamond"},ChatCompletion.ranked("diamnod",List.of("dirt","diamond")));
    assertArrayEquals(new String[]{"Wooden_Planks"},ChatCompletion.ranked("PLANK",List.of("Wooden_Planks")));
    assertArrayEquals(new String[]{"wooden_planks"},ChatCompletion.ranked("wodden_planks",List.of("wooden_planks")));
    assertArrayEquals(new String[]{"minecraft:wooden_planks"},ChatCompletion.ranked("minecraft:plank",List.of("minecraft:wooden_planks","other:planks")));
    assertArrayEquals(new String[]{"/give"},ChatCompletion.ranked("/gve",List.of("/give","/time")));
  }
  @Test void matchingIgnoresCaseAndKeepsNativeSpellingAndOffsets() {
    assertArrayEquals(new String[] {"ative", "eper"},
        ChatCompletion.suffixes("CRE", List.of("creative", "Creeper", "stone", "creative")));
    assertTrue(ChatCompletion.startsWith("//help", "//HE"));
    assertTrue(ChatCompletion.startsWith("Minecraft:stone", "MINECRAFT:ST"));
    assertFalse(ChatCompletion.startsWith("help", "HELPER"));
    assertFalse(ChatCompletion.startsWith("stone", "tone"));
  }
}
