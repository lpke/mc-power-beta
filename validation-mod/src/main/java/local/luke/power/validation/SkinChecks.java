package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.texture.SkinImageProcessor;
import net.minecraft.entity.player.PlayerEntity;

/** Network/profile and actual GPU texture checks, only in disposable worlds. */
final class SkinChecks {
  static void run(Minecraft mc, String action) throws Exception {
    Class<?> type = Class.forName("com.matthewperiut.retroauth.skin.SkinService");
    Object service = type.getMethod("getInstance").invoke(null);
    if (action.equals("check")) {
      check(mc.player.skinUrl != null && mc.player.skinUrl.startsWith("http"), "missing remote skin URL");
      var image = mc.textureManager.downloadImage(mc.player.skinUrl, new SkinImageProcessor());
      try {
        check(image != null && image.image != null, "skin PNG not downloaded/decoded");
        int texture = mc.textureManager.method_1093(mc.player.skinUrl, null);
        check(texture >= 0 && image.textureId == texture, "skin not uploaded to GPU");
        check(texture != mc.textureManager.getTextureId(mc.player.method_1314()), "renderer still uses default skin");
        log("PASS custom skin decoded " + image.image.getWidth() + "x" + image.image.getHeight() + " and uploaded as texture " + texture);
      } finally { mc.textureManager.releaseImage(mc.player.skinUrl); }
      return;
    }
    String name = mc.player.name;
    try {
      if (action.equals("default")) {
        var field = type.getDeclaredField("profiles"); field.setAccessible(true);
        @SuppressWarnings("unchecked") Map<String, Object> profiles = (Map<String, Object>)field.get(service);
        Class<?> profile = Class.forName("com.matthewperiut.retroauth.profile.PlayerProfile");
        profiles.put("FixtureDefault", profile.getConstructor(UUID.class, String.class, String.class, String.class)
            .newInstance(UUID.randomUUID(), "/assets/retroauth/mob/steve.png", null, "default"));
        mc.player.name = "FixtureDefault";
      } else {
        check(action.matches("[A-Za-z0-9_]{1,16}"), "invalid profile name");
        type.getMethod("init", String.class).invoke(service, action);
        mc.player.name = action;
      }
      type.getMethod("init", PlayerEntity.class).invoke(service, mc.player);
      if (action.equals("default")) check(mc.player.skinUrl == null, "local fallback sent to HTTP downloader");
      else check(mc.player.skinUrl != null && mc.player.skinUrl.startsWith("http"), "provider fallback failed");
      log("PASS skin profile " + (action.equals("default") ? "default without download" : "remote skin selected"));
    } finally { mc.player.name = name; }
  }
}
