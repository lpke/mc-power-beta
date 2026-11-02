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

@SuppressWarnings({"StringConcatenationArgumentToLogCall", "BusyWait"})
public class RawInputHandler {
    private static final Logger logger = ControlFeatures.LOGGER;

    public static Controller[] controllers;

    public static final RawMouseMotion motion = new RawMouseMotion();

    private static int worldJoinTimer;

    private static boolean shouldGetMouse = false;
    public static boolean rawInputEnabled = false;

    public static volatile Thread inputThread;
    public static volatile boolean runInputThread = false;
    private static final RawInputWorker<Mouse> WORKER = new RawInputWorker<>(
            () -> runInputThread, RawInputHandler::pollMice, thread -> inputThread = thread);

    public static List<Mouse> mice() { return WORKER.devices(); }

    private static void pollMice(List<Mouse> devices) {
        boolean capture = Minecraft.INSTANCE.currentScreen == null;
        for (Mouse mouse : devices) {
            if (!runInputThread) break;
            var samples = motion.capture();
            mouse.poll();
            if (capture && runInputThread) samples.add((int) mouse.getX().getPollData(), (int) mouse.getY().getPollData());
        }
    }

    public static void init() {
        startInputThread();
    }

    public static void startInputThread() {
        if (!rawInputEnabled) return;
        runInputThread = true;
        WORKER.start();
    }

    public static void getMouse(String reason) {
        if (!rawInputEnabled) return;
        // Log a debug message
        logger.debug(String.format("getMouse called. Reason: %s. Should get mouse: %s", reason, shouldGetMouse));

        // Setup the controller environment
        ControllerEnvironment controllerEnvironment = ControllerEnvironment.getDefaultEnvironment();
        controllers = controllerEnvironment.getControllers();

        var discovered = new ArrayList<Mouse>();

        // Go thru controllers and find mice
        for (Controller controller : controllers) {
            logger.info("Found Controller " + controller.getName() + " of type " + controller.getType());
            if (controller instanceof Mouse mouseController) {
                discovered.add(mouseController);
                //logger.info("Adding Controller " + controller.getName() + " of type " + controller.getType());
            }
        }

        WORKER.devices(discovered);
        logger.info("Found " + discovered.size() + " mouse controllers");

        // If none are found, fall back to Vanilla Mouse Helper
        if (discovered.isEmpty()) {
            Util.notify("No mouse controllers found, switching back to Vanilla Mouse Helper", true);
            disableRawInput(false, true);
        }
    }

    public static void toggleRawInput() {
        PlayerEntity player = Minecraft.INSTANCE.player;
        float saveYaw = player.yaw;
        float savePitch = player.pitch;

        if (Minecraft.INSTANCE.mouse instanceof RawMouseHelper) {
            disableRawInput(true, true);
        } else {
            enableRawInput(true, true);
        }

        player.yaw = saveYaw;
        player.pitch = savePitch;
    }

    public static void enableRawInput(boolean lock, boolean notifyInChat) {
        Util.notify("Raw Input Toggled ON", notifyInChat);
        Minecraft.INSTANCE.mouse = new RawMouseHelper(Minecraft.INSTANCE.canvas);
        if (lock) {
            Minecraft.INSTANCE.mouse.lockCursor();
        }
        rawInputEnabled = true;
        getMouse("Enabled Raw Input");
        startInputThread();
    }

    public static void disableRawInput(boolean lock, boolean notifyInChat) {
        Minecraft.INSTANCE.mouse = new net.minecraft.client.Mouse(Minecraft.INSTANCE.canvas);
        if (lock) {
            Minecraft.INSTANCE.mouse.lockCursor();
        }
        rawInputEnabled = false;
        runInputThread = false;
        WORKER.wake();
        motion.discard();
        Util.notify("Raw Input Toggled OFF", notifyInChat);
    }

    public static void tick() {
        if (!rawInputEnabled) return;
        if (worldJoinTimer >= 0) {
            worldJoinTimer--;
        }
        if (shouldGetMouse) {
            getMouse("Post Join/Leave Timer");
            shouldGetMouse = false;
        }
    }

    public static void onJoinWorld() {
//        ControlFeatures.logger.info(String.format("Player Joined World. Getting Mouse"));
        worldJoinTimer = 3;
        shouldGetMouse = rawInputEnabled;

    }

    public static void onLeaveWorld() {
        shouldGetMouse = false;
    }
}


