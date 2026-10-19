package local.luke.power.chat;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatCompletionTest {
  @Test void matchingIgnoresCaseAndKeepsNativeSpellingAndOffsets() {
    assertArrayEquals(new String[] {"ative", "eper"},
        ChatCompletion.suffixes("CRE", List.of("creative", "Creeper", "stone", "creative")));
    assertTrue(ChatCompletion.startsWith("//help", "//HE"));
    assertTrue(ChatCompletion.startsWith("Minecraft:stone", "MINECRAFT:ST"));
    assertFalse(ChatCompletion.startsWith("help", "HELPER"));
    assertFalse(ChatCompletion.startsWith("stone", "tone"));
  }
}
