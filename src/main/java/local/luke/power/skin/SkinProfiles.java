package local.luke.power.skin;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Reject incomplete provider responses so authentication's next provider can try. */
public final class SkinProfiles {
  private SkinProfiles() {}

  public static boolean hasTextureData(String encoded) {
    if (encoded == null || encoded.length() > 131072) return false;
    try {
      var data = JsonParser.parseString(new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8));
      return data.isJsonObject() && data.getAsJsonObject().has("textures")
          && data.getAsJsonObject().get("textures").isJsonObject();
    } catch (RuntimeException invalid) { return false; }
  }

  /** Bundled fallback textures belong to the renderer, not the HTTP downloader. */
  public static String downloadUrl(String url) {
    if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) return null;
    return url;
  }
}
