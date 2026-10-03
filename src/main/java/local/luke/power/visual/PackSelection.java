package local.luke.power.visual;

import net.minecraft.class_285;
import net.minecraft.client.Minecraft;

public final class PackSelection {
  private static boolean preview;
  public static boolean previewing() { return preview; }
  public static void select(Minecraft mc, class_285 pack) {
    preview = true;
    try {
      if (mc.field_2768.method_999(pack)) {
        TextureOverrides.clear();
        mc.textureManager.method_1096();
      }
    } finally { preview = false; }
  }
}
