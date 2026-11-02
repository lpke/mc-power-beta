package local.luke.power.controls.tweaks.rawinput;

import java.awt.*;

public class RawMouseHelper extends net.minecraft.client.Mouse {

    public RawMouseHelper(Component parent) {
        super(parent);
    }

    @Override
    public void poll() {
        if (net.minecraft.client.Minecraft.INSTANCE.currentScreen != null) {
            // GUI looking follows the visible cursor; raw movement is ignored in menus.
            super.poll();
            RawInputHandler.motion.clear();
            return;
        }
        long sample = RawInputHandler.motion.drain();
        this.deltaX = RawMouseMotion.x(sample);
        this.deltaY = -RawMouseMotion.y(sample);
    }

    @Override
    public void lockCursor() {
        super.lockCursor();
        RawInputHandler.motion.discard();
    }
}
