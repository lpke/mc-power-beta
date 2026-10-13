package local.luke.power.controls.tweaks.rawinput;

import java.awt.*;

public class RawMouseHelper extends net.minecraft.client.Mouse {

    public RawMouseHelper(Component parent) {
        super(parent);
    }

    @Override
    public void poll() {
        if (net.minecraft.client.Minecraft.INSTANCE.currentScreen != null) {
            // GUI looking follows the visible cursor; raw device polling pauses in menus.
            super.poll();
            RawInputHandler.dx = 0;
            RawInputHandler.dy = 0;
            return;
        }
        this.deltaX = RawInputHandler.dx;
        RawInputHandler.dx = 0;
        this.deltaY = -RawInputHandler.dy;
        RawInputHandler.dy = 0;
    }

    @Override
    public void lockCursor() {
        super.lockCursor();
        RawInputHandler.dx = 0;
        RawInputHandler.dy = 0;
    }
}
