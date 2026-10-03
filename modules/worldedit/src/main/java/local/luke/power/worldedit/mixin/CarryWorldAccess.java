package local.luke.power.worldedit.mixin;
import net.minecraft.world.World;
import net.minecraft.class_52;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(World.class)
public interface CarryWorldAccess {
  @Accessor("field_219") class_52 power$storage();
}
