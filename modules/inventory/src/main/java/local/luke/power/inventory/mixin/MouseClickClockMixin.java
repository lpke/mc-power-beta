package local.luke.power.inventory.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.inventory.MouseClickClock;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Screen.class)
public class MouseClickClockMixin {
    @WrapMethod(method = "onMouseEvent")
    private void powerInventory$eventTime(Operation<Void> original) {
        long previous = MouseClickClock.event(Mouse.getEventNanoseconds());
        try { original.call(); }
        finally { MouseClickClock.event(previous); }
    }
}
