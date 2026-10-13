package local.luke.power.skin;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkinProfilesTest {
  private static String encoded(String json) {
    return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
  }
  @Test void rejectsIncompleteAndMalformedProfiles() {
    assertFalse(SkinProfiles.hasTextureData(null));
    assertFalse(SkinProfiles.hasTextureData("not base64!"));
    for (String json : new String[] {"null", "[]", "{}", "{\"textures\":null}", "{\"textures\":[]}", "{\"error\":\"rate limited\"}"})
      assertFalse(SkinProfiles.hasTextureData(encoded(json)), json);
    assertFalse(SkinProfiles.hasTextureData("a".repeat(131073)));
  }
  @Test void acceptsBothCustomAndGenuineDefaultSkinProfiles() {
    assertTrue(SkinProfiles.hasTextureData(encoded("{\"textures\":{}}")));
    assertTrue(SkinProfiles.hasTextureData(encoded("{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/test\"}}}")));
  }
  @Test void localFallbacksNeverReachHttpDownloader() {
    assertNull(SkinProfiles.downloadUrl(null));
    assertNull(SkinProfiles.downloadUrl("/assets/retroauth/mob/steve.png"));
    assertNull(SkinProfiles.downloadUrl("file:///tmp/skin.png"));
    for (String url : new String[] {"http://textures.minecraft.net/texture/test", "https://textures.minecraft.net/texture/test"})
      assertEquals(url, SkinProfiles.downloadUrl(url));
  }
}
