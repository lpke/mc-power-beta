package local.luke.power.controls.tweaks.photomode;

import local.luke.power.controls.util.ModOptions;
import local.luke.power.controls.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.ScreenScaler;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Display;

@SuppressWarnings("unchecked")
public class PhotoModeScreen extends Screen {
    private ButtonWidget exitButton;
    private SliderWidget tiltSlider;
    private SliderWidget timeSlider;

    private ButtonWidget centerButton;
    private ButtonWidget rotateLeftButton;
    private ButtonWidget rotateRightButton;
    private ButtonWidget screenshotButton;
    private ButtonWidget renderPlayerButton;

    private long originalTOD;
    private local.luke.power.world.WorldCycles originalCycles;
    private long desiredTOD = -1L;
    private long desiredDay = -1L;

    boolean shouldScreenshot = false;
    public boolean renderPlayer = true;

    public float rotation = 0.0f;
    public float rotationGoal = 0.0f;
    public float zoom = 1.25f;
    public float zoomGoal = 1.25f;
    public float tilt = 30.0f;
    public float tiltGoal = 30.0f;
    public float cameraX = 0.0f;
    public float cameraY = 0.0f;

    public Screen previousScreen;
    private long lastTickTime = System.nanoTime();
    private int dragButton = -1;

    public PhotoModeScreen(Screen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public void init() {
        super.init();

        int id = 0;
        this.exitButton = new ButtonWidget(id++, 0, 0, 20, 20, "X");
        this.tiltSlider = new SliderWidgetWithoutSaving(id++, this.width - 150, 0, "Tilt: Default", 0.33333334f);
        this.timeSlider = new SliderWidgetWithoutSaving(id++, this.width - 150, 20, "Time of Day: Current", 0.0f);
        int rotationY = this.height - (this.width < 360 ? 44 : 20);
        this.centerButton = new ButtonWidget(id++, 0, this.height - 20, 98, 20, "Center");
        this.rotateLeftButton = new ButtonWidget(id++, this.width / 2 - 49 - 2 - 20, rotationY, 20, 20, "<");
        this.rotateRightButton = new ButtonWidget(id++, this.width / 2 + 49 + 2, rotationY, 20, 20, ">");
        this.screenshotButton = new ButtonWidget(id++, this.width / 2 - 49, rotationY, 98, 20, "Take Screenshot");
        this.renderPlayerButton = new ButtonWidget(id++, this.width - 104, this.height - 20, 105, 20, "Render Player: On");

        this.buttons.add(this.exitButton);
        this.buttons.add(this.tiltSlider);
        this.buttons.add(this.centerButton);
        this.buttons.add(this.rotateLeftButton);
        this.buttons.add(this.rotateRightButton);
        this.buttons.add(this.screenshotButton);
        this.buttons.add(this.renderPlayerButton);

        if (!this.minecraft.world.isRemote) {
            this.buttons.add(this.timeSlider);
        }

        if (originalCycles == null) {
            originalCycles = (local.luke.power.world.WorldCycles)this.minecraft.world.getProperties();
            this.originalTOD = originalCycles.power$daylightTime();
        }
        if (this.desiredTOD == -1L) {
            this.desiredTOD = this.originalTOD % 24000L;
        } else {
            this.timeSlider.value = (float) this.desiredTOD / 24000.0f;
        }
        if (this.desiredDay == -1L) {
            this.desiredDay = this.originalTOD / 24000L;
        }
        this.tiltSlider.value = this.tiltGoal / 90.0f;
        this.dragButton = -1;
        this.lastTickTime = System.nanoTime();
        this.updateButtonsText();
        ModOptions.photoModeFogMultiplier = 100F;
    }

    @Override
    public void removed() {
        if (originalCycles != null) originalCycles.power$daylightTime(this.originalTOD);
        ModOptions.photoModeFogMultiplier = 1.0F;
    }

    // Input
    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (button == this.rotateLeftButton) {
            this.rotateLeft();
        } else if (button == this.rotateRightButton) {
            this.rotateRight();
        } else if (button == this.screenshotButton) {
            this.shouldScreenshot = true;
        } else if (button == this.exitButton) {
            this.minecraft.setScreen(this.previousScreen);
        } else if (button == this.renderPlayerButton) {
            this.renderPlayer = !this.renderPlayer;
        } else if (button == this.centerButton) {
            this.center();
        }
        this.updateButtonsText();
    }

    private void rotateLeft() {
        float target = this.rotationGoal + 0.05f;
        this.rotationGoal = (float) Math.ceil(target * 2f) / 2f;
        
        if (this.rotationGoal > 200) {
            this.rotationGoal = 200;
        }
    }

    private void rotateRight() {
        float target = this.rotationGoal - 0.05f;
        this.rotationGoal = (float) Math.floor(target * 2f) / 2f;
        
        if (this.rotationGoal < -200) {
            this.rotationGoal = -200;
        }
    }
    
    private void center() {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            this.rotation = 0;
            this.rotationGoal = 0;
            this.zoom = 1.25f;
            this.zoomGoal = 1.25f;
            this.tilt = 30.0f;
            this.tiltGoal = 30.0f;
            this.tiltSlider.value = 1.0f / 3.0f;
        }
        
        this.cameraX = 0;
        this.cameraY = 0;
    }

    @Override
    protected void keyPressed(char character, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.minecraft.setScreen(null);
            this.minecraft.lockMouse();
        }

        // Controls via keyboard
        if (Keyboard.getEventKeyState()) {
            switch (keyCode) {
                case Keyboard.KEY_Q -> this.rotateLeft();
                case Keyboard.KEY_E -> this.rotateRight();
                case Keyboard.KEY_SPACE -> this.center();
            }
        }
    }
    
    private boolean shiftHeld() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onMouseEvent() {
        if (!Display.isActive()) {
            this.dragButton = -1;
            return;
        }
        int mouseX = Mouse.getEventX() * width / minecraft.displayWidth;
        moveSlider(tiltSlider, mouseX);
        moveSlider(timeSlider, mouseX);
        updateSliders();
        int dx = Mouse.getEventDX(), dy = Mouse.getEventDY();
        if (this.dragButton == 0) {
            float speed = (float) (0.0005f / Math.pow(2, this.zoom));
            if (shiftHeld()) speed *= 2;
            this.cameraX += dx * speed;
            this.cameraY += dy * speed * ((float) width / height);
        } else if (this.dragButton >= 0) {
            this.tiltSlider.value = Util.clamp(this.tiltSlider.value - dy * 0.0020f, 0.0f, 1.0f);
            this.tiltGoal = (int) (this.tiltSlider.value * 90.0f);
            this.rotationGoal += dx * 0.0020f;
        }
        int button = Mouse.getEventButton();
        if (button >= 0 && button <= 2) {
            if (!Mouse.getEventButtonState()) {
                if (this.dragButton == button) this.dragButton = -1;
            } else if (this.dragButton < 0) {
                int x = Mouse.getEventX() * width / minecraft.displayWidth;
                int y = height - Mouse.getEventY() * height / minecraft.displayHeight - 1;
                boolean overButton = buttons.stream().anyMatch(value ->
                    value instanceof ButtonWidget widget && isMouseInBounds(x, y, widget));
                if (!overButton) this.dragButton = button;
            }
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) this.zoomGoal = Util.clamp(this.zoomGoal + Math.signum(wheel) * 0.25f, 0.01f, 12.0f);
        super.onMouseEvent();
        updateSliders();
    }

    public void handleInput(int mouseX, int mouseY, float delta) {
        // Dispatch newly polled events between ticks too. A whole quick drag can be
        // queued here, so its motion must be applied before its release is handled.
        while (minecraft.currentScreen == this && Mouse.next()) onMouseEvent();
        Mouse.getDX(); Mouse.getDY(); Mouse.getDWheel();
        if (minecraft.currentScreen != this || !Display.isActive()) {
            this.dragButton = -1;
            this.tiltSlider.dragging = this.timeSlider.dragging = false;
            return;
        }

        // Movement via keyboard
        boolean shift = shiftHeld();
        float deltaTime = Math.min(1.0f, (System.nanoTime() - lastTickTime) / 100000000F);
        float cameraVelocity = (float) (0.075f / Math.pow(2, this.zoom)) * deltaTime;
        if (shift) cameraVelocity *= 2;

        if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
            this.cameraY -= cameraVelocity * ((float) width / height);
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
            this.cameraY += cameraVelocity * ((float) width / height);
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
            this.cameraX += cameraVelocity;
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
            this.cameraX -= cameraVelocity;
        }

        updateButtonsText();
    }
    
    private boolean isMouseInBounds(int mouseX, int mouseY, ButtonWidget button) {
        return button.visible && button.active && mouseX >= button.x && mouseY >= button.y && mouseX < button.x + button.width && mouseY < button.y + button.height;
    }

    // UI
    private void updateButtonsText() {
        this.timeSlider.text = this.timeSlider.value == 0.0f ? "Time of Day: Default" : "Time of Day: " + (long) (this.timeSlider.value * 24000.0f);
        this.tiltSlider.text = (int) (this.tiltSlider.value * 90.0f) == 30 ? "Tilt: Default" : "Tilt: " + (Math.floor(this.tiltSlider.value * 90.0f)) + " degrees";
        this.renderPlayerButton.text = this.renderPlayer ? "Render Player: On" : "Render Player: Off";
        this.centerButton.text = shiftHeld() ? "Full Reset" : "Center";
    }

    // Rendering
    @Override
    public void render(int mouseX, int mouseY, float delta) {
        this.handleInput(mouseX, mouseY, delta);
        if (minecraft.currentScreen != this) return;
        ScreenScaler scaledResolution = new ScreenScaler(this.minecraft.options, this.minecraft.displayWidth, this.minecraft.displayHeight);

        GL11.glViewport(0, 0, this.minecraft.displayWidth, this.minecraft.displayHeight);
        GL11.glMatrixMode(5889);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(5888);
        GL11.glLoadIdentity();
        GL11.glClear(256);
        GL11.glMatrixMode(5889);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), 0.0, 1000.0, 3000.0);
        GL11.glMatrixMode(5888);
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0f, 0.0f, -2000.0f);

        if (this.zoom != this.zoomGoal) {
            this.zoom += (this.zoomGoal - this.zoom) * 0.02f + (this.zoomGoal - this.zoom) * 0.02f * delta;
            if (Math.abs(this.zoom - this.zoomGoal) < 5.0E-4f) {
                this.zoom = this.zoomGoal;
            }
        }
        if (this.rotation != this.rotationGoal) {
            this.rotation += (this.rotationGoal - this.rotation) * 0.02f + (this.rotationGoal - this.rotation) * 0.02f * delta;
            if (Math.abs(this.rotation - this.rotationGoal) < 5.0E-4f) {
                this.rotation = this.rotationGoal;
            }
        }

        //Makes rotation and rotationGoal wrap around back to 0 smoothly, so the 200 limit is never hit
        if (Math.abs(this.rotation) >= 4.0f) {
            this.rotation = this.rotation % 4;
            this.rotationGoal = this.rotationGoal % 4;
        }
        if (this.tilt != this.tiltGoal) {
            this.tilt += (this.tiltGoal - this.tilt) * 0.02f + (this.tiltGoal - this.tilt) * 0.02f * delta;
            if (Math.abs(this.tilt - this.tiltGoal) < 0.01f) {
                this.tilt = this.tiltGoal;
            }
        }
        if (!this.shouldScreenshot) {
            super.render(mouseX, mouseY, delta);
        } else {
            Screenshot.take(Minecraft.getRunDirectory(), this.minecraft.displayWidth, this.minecraft.displayHeight);
            this.shouldScreenshot = false;
        }
        updateSliders();
        this.lastTickTime = System.nanoTime();
    }

    private void moveSlider(SliderWidget slider, int x) {
        if (slider.dragging) slider.value = Util.clamp((float)(x - slider.x - 4) / (slider.width - 8), 0.0f, 1.0f);
    }

    private void updateSliders() {
        if (this.timeSlider.dragging && !this.minecraft.world.isRemote) {
            this.desiredTOD = this.timeSlider.value == 0.0f ? this.originalTOD % 24000L
                : (long)(this.timeSlider.value * 24000.0f);
            var cycles = (local.luke.power.world.WorldCycles)this.minecraft.world.getProperties();
            long target = this.desiredDay * 24000L + this.desiredTOD;
            if (cycles.power$daylightTime() != target) {
                cycles.power$daylightTime(target);
                this.minecraft.world.updateSkyBrightness();
                this.minecraft.worldRenderer.notifyAmbientDarknessChanged();
            }
            this.updateButtonsText();
        }
        if (this.tiltSlider.dragging) {
            this.tiltGoal = (int)(this.tiltSlider.value * 90.0f);
            this.updateButtonsText();
        }
    }
}
