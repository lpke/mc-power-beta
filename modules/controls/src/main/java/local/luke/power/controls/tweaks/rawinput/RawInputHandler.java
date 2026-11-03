package local.luke.power.controls.tweaks.rawinput;

import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.util.Util;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RawInputHandler {
    private static final Logger logger = ControlFeatures.LOGGER;

    public static Controller[] controllers;

    public static final RawMouseMotion motion = new RawMouseMotion();
    private static final RawInputOwnership OWNERSHIP = new RawInputOwnership(motion);
    private record Devices(List<Controller> controllers, List<Mouse> mice) {
        Devices { controllers = List.copyOf(controllers); mice = List.copyOf(mice); }
    }
    private static final RawInputDiscovery<Devices> DISCOVERY = new RawInputDiscovery<>(
            RawInputHandler::discoverMice, devices -> !devices.mice().isEmpty());
    private static boolean notifyOnActivation;
    // Linux legacy fullscreen reports Display.isActive even after accepted FocusOut.
    // Other display backends retain their native isActive predicate.
    private static boolean nativeFocusAllowed = true;

    private static int worldJoinTimer;

    private static boolean shouldGetMouse = false;
    public static boolean rawInputEnabled = false;

    public static volatile Thread inputThread;
    public static volatile boolean runInputThread = false;
    private static final RawInputWorker<Mouse> WORKER = new RawInputWorker<>(
            () -> runInputThread, RawInputHandler::pollMice, thread -> inputThread = thread);

    public static List<Mouse> mice() { return WORKER.devices(); }

    private static void pollMice(List<Mouse> devices) {
        var destination = OWNERSHIP.destination();
        boolean capture = OWNERSHIP.ready(destination, devices);
        for (Mouse mouse : devices) {
            if (!runInputThread) break;
            mouse.poll();
            if (capture && runInputThread) destination.add((int) mouse.getX().getPollData(), (int) mouse.getY().getPollData());
        }
        OWNERSHIP.drained(destination, devices);
    }

    /** Called on the game thread, never by the device worker. */
    public static boolean refreshCapture() {
        if (!rawInputEnabled) {
            OWNERSHIP.suspend();
            return false;
        }
        Minecraft mc = Minecraft.INSTANCE;
        boolean owns = nativeFocusAllowed && mc != null && mc.mouse instanceof RawMouseHelper && mc.currentScreen == null && mc.world != null && mc.player != null
                && mc.player.world == mc.world && org.lwjgl.opengl.Display.isCreated()
                && org.lwjgl.opengl.Display.isActive() && org.lwjgl.input.Mouse.isCreated()
                && org.lwjgl.input.Mouse.isGrabbed();
        return OWNERSHIP.update(owns, mc == null ? null : mc.world, mc == null ? null : mc.player);
    }

    public static void suspendCapture() { OWNERSHIP.suspend(); }

    /** Retire the old session before a grab transition can clear/rebase native events. */
    public static void beforeGrab(boolean grabbed) {
        if (rawInputEnabled && grabbed != org.lwjgl.input.Mouse.isGrabbed()) OWNERSHIP.suspend();
    }

    /** Focus notifications can contain an entire out/in pair before the next rendered frame. */
    public static void focusChanged(boolean focused) {
        if (nativeFocusAllowed == focused) return;
        nativeFocusAllowed = focused;
        if (!rawInputEnabled) return;
        OWNERSHIP.suspend();
        refreshCapture();
    }

    public static void init() {
        getMouse("Initialize Raw Input");
    }

    public static void startInputThread() {
        if (!rawInputEnabled || WORKER.devices().isEmpty()) return;
        runInputThread = true;
        WORKER.start();
    }

    public static void getMouse(String reason) {
        if (!rawInputEnabled) return;
        logger.debug("Mouse discovery requested: {}", reason);
        DISCOVERY.enable();
        update();
    }

    public static void rescanMouse() {
        if (!rawInputEnabled) return;
        DISCOVERY.refresh();
        update();
    }

    private static Devices discoverMice() {
        ControllerEnvironment controllerEnvironment = ControllerEnvironment.getDefaultEnvironment();
        var controllers = List.of(controllerEnvironment.getControllers());
        var discovered = new ArrayList<Mouse>();
        for (Controller controller : controllers) {
            if (controller instanceof Mouse mouseController) discovered.add(mouseController);
        }
        return new Devices(controllers, discovered);
    }

    /** Frame-owned acceptance; native helpers stay usable while first discovery is pending. */
    public static void update() {
        if (!rawInputEnabled) return;
        var result = DISCOVERY.poll();
        if (result != null) {
            if (result.failure() != null) {
                logger.warn("Could not discover raw mouse devices; retaining native input", result.failure());
                disableRawInput(false, false);
                Util.notify("Could not initialize raw input; using normal mouse input", true);
            } else if (result.devices().mice().isEmpty()) {
                disableRawInput(false, false);
                Util.notify("No mouse controllers found, using normal mouse input", true);
            } else {
                var devices = result.devices();
                controllers = devices.controllers().toArray(Controller[]::new);
                suspendCapture();
                WORKER.devices(devices.mice());
                if (!(Minecraft.INSTANCE.mouse instanceof RawMouseHelper)) {
                    Minecraft.INSTANCE.mouse = new RawMouseHelper(Minecraft.INSTANCE.canvas);
                    if (notifyOnActivation) Util.notify("Raw Input Toggled ON", true);
                }
                notifyOnActivation = false;
                logger.debug("Found {} mouse controllers", devices.mice().size());
                refreshCapture();
                startInputThread();
            }
        }
        refreshCapture();
    }

    public static void toggleRawInput() {
        PlayerEntity player = Minecraft.INSTANCE.player;
        float saveYaw = player.yaw;
        float savePitch = player.pitch;

        if (rawInputEnabled) {
            disableRawInput(true, true);
        } else {
            enableRawInput(true, true);
        }

        player.yaw = saveYaw;
        player.pitch = savePitch;
    }

    public static void enableRawInput(boolean lock, boolean notifyInChat) {
        rawInputEnabled = true;
        notifyOnActivation = notifyInChat;
        if (lock) Minecraft.INSTANCE.mouse.lockCursor();
        getMouse("Enabled Raw Input");
    }

    public static void disableRawInput(boolean lock, boolean notifyInChat) {
        rawInputEnabled = false;
        DISCOVERY.disable();
        notifyOnActivation = false;
        suspendCapture();
        replaceRawHelper();
        if (lock) {
            Minecraft.INSTANCE.mouse.lockCursor();
        }
        runInputThread = false;
        WORKER.wake();
        Util.notify("Raw Input Toggled OFF", notifyInChat);
    }

    private static void replaceRawHelper() {
        if (!(Minecraft.INSTANCE.mouse instanceof RawMouseHelper)) return;
        // Raw gameplay left these native relative counters unread. Do not replay
        // them after fallback, or consume any queued GUI/button events.
        if (org.lwjgl.input.Mouse.isCreated()) {
            org.lwjgl.input.Mouse.getDX();
            org.lwjgl.input.Mouse.getDY();
        }
        Minecraft.INSTANCE.mouse = new net.minecraft.client.Mouse(Minecraft.INSTANCE.canvas);
    }

    public static void tick() {
        if (!rawInputEnabled) return;
        update();
        if (worldJoinTimer >= 0) {
            worldJoinTimer--;
        }
        if (shouldGetMouse) {
            getMouse("Post Join/Leave Timer");
            shouldGetMouse = false;
        }
    }

    public static void onJoinWorld() {
        suspendCapture();
        refreshCapture();
//        ControlFeatures.logger.info(String.format("Player Joined World. Getting Mouse"));
        worldJoinTimer = 3;
        shouldGetMouse = rawInputEnabled;

    }

    public static void onLeaveWorld() {
        suspendCapture();
        shouldGetMouse = false;
    }
}


