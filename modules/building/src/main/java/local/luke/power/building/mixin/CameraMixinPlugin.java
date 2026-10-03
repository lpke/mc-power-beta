package local.luke.power.building.mixin;

import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;

public final class CameraMixinPlugin implements IMixinConfigPlugin {
  public void onLoad(String p) {}

  public String getRefMapperConfig() {
    return null;
  }

  public boolean shouldApplyMixin(String target, String mixin) {
    boolean arsenic = FabricLoader.getInstance().isModLoaded("station-renderer-arsenic");
    if (mixin.endsWith("CameraVanillaHandMixin")) return !arsenic;
    if (mixin.endsWith("CameraArsenicHandMixin")) return arsenic;
    return true;
  }

  public void acceptTargets(Set<String> mine, Set<String> others) {}

  public List<String> getMixins() {
    return null;
  }

  public void preApply(String name, ClassNode target, String mixin, IMixinInfo info) {}

  public void postApply(String name, ClassNode target, String mixin, IMixinInfo info) {}
}
