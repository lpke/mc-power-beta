package local.luke.power.mixin;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ButtonWidget.class)
public interface ButtonAccessor { @Accessor("width") void power$width(int value); }
