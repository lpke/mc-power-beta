package local.luke.power.controls.tweaks.rawinput;

import java.awt.*;

public class RawMouseHelper extends net.minecraft.client.Mouse {

    public RawMouseHelper(Component parent) {
        super(parent);
    }

    @Override
    public void poll() {
        boolean ownsPointer = RawInputHandler.refreshCapture();
        if (net.minecraft.client.Minecraft.INSTANCE.currentScreen != null) {
            // GUI looking follows the visible cursor; raw movement is ignored in menus.
            super.poll();
            return;
        }
        if (!ownsPointer) {
            this.deltaX = this.deltaY = 0;
            return;
        }
        long sample = RawInputHandler.motion.drain();
        this.deltaX = RawMouseMotion.x(sample);
        this.deltaY = -RawMouseMotion.y(sample);
    }

    @Override
    public void lockCursor() {
        RawInputHandler.suspendCapture();
        super.lockCursor();
        RawInputHandler.refreshCapture();
    }
}
