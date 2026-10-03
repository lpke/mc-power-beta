package local.luke.power.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.io.File;
import java.util.*;
import net.minecraft.class_266;
import net.minecraft.class_267;
import org.spongepowered.asm.mixin.*;
/** Resource downloads can finish after menus open. Serialize pool access and reject duplicate loads. */
@Mixin(class_266.class)
public class SoundPoolMixin {
  @Unique private final Map<String,class_267> power$loaded=new HashMap<>();
  @WrapMethod(method="method_959")
  private class_267 power$load(String name,File file,Operation<class_267> original){synchronized(this){String key=name+"\n"+file.toPath().toAbsolutePath().normalize();class_267 prior=power$loaded.get(key);if(prior!=null)return prior;class_267 loaded=original.call(name,file);if(loaded!=null)power$loaded.put(key,loaded);return loaded;}}
  @WrapMethod(method="method_958") private class_267 power$named(String name,Operation<class_267> original){synchronized(this){return original.call(name);}}
  @WrapMethod(method="method_957") private class_267 power$random(Operation<class_267> original){synchronized(this){return original.call();}}
}
