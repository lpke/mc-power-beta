package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import local.luke.power.config.Catalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Synthetic paired motion; real helper/grab hooks, synthetic accepted Linux focus notifications. */
final class RawOwnership473Checks {
    static void run(Minecraft mc) throws Exception {
        check(mc.world != null && mc.player != null && Display.isActive() && !Display.isFullscreen(),
                "focus a windowed disposable world for raw-ownership-473");
        check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
        Class<?> handler = Class.forName("local.luke.power.controls.tweaks.rawinput.RawInputHandler");
        Class<?> helper = Class.forName("local.luke.power.controls.tweaks.rawinput.RawMouseHelper");
        Class<?> accumulator = Class.forName("local.luke.power.controls.tweaks.rawinput.RawMouseMotion");
        Object motion = handler.getField("motion").get(null);
        Method add = accumulator.getMethod("add", int.class, int.class), clear = accumulator.getMethod("clear");
        Method capture = accumulator.getMethod("capture"), refresh = handler.getMethod("refreshCapture");
        Field enabled = handler.getField("rawInputEnabled"), running = handler.getField("runInputThread");
        Thread worker = (Thread)handler.getField("inputThread").get(null);
        boolean wasEnabled = enabled.getBoolean(null), wasRunning = running.getBoolean(null), grabbed = Mouse.isGrabbed();
        Screen screen = mc.currentScreen;
        var originalHelper = mc.field_2767;
        float yaw = mc.player.yaw, previousYaw = mc.player.prevYaw, pitch = mc.player.pitch, previousPitch = mc.player.prevPitch;
        Field implementation = field(Display.class, "display_impl"); Object backend = implementation.get(null);
        check(backend.getClass().getName().equals("org.lwjgl.opengl.LinuxDisplay"), "Linux focus adapter fixture requires LWJGL LinuxDisplay");
        Field focused = field(backend.getClass(), "focused"); boolean wasFocused = focused.getBoolean(backend);
        Method focus = backend.getClass().getDeclaredMethod("setFocused", boolean.class, int.class); focus.setAccessible(true);
        var reports = new ArrayList<String>();
        try {
            running.setBoolean(null, false);
            if (worker != null) {
                worker.interrupt(); worker.join(2000);
                check(!worker.isAlive(), "physical-device worker did not stop before synthetic sampling");
            }
            var raw = (net.minecraft.class_596)helper.getConstructor(java.awt.Component.class).newInstance(mc.canvas);
            mc.field_2767 = raw;
            enabled.setBoolean(null, true); mc.currentScreen = null; Mouse.setGrabbed(true);
            check((boolean)refresh.invoke(null), "focused grabbed player did not own raw capture");
            clear.invoke(motion); add.invoke(motion, 7, -5); raw.method_1972();
            check(raw.field_2586 == 7 && raw.field_2587 == 5, "owned raw helper changed paired counts");
            reports.add("focused grabbed synthetic counts passed unchanged");

            Method focusChanged = handler.getMethod("focusChanged", boolean.class);
            Object sameFocusSession = capture.invoke(motion);
            add.invoke(motion, 6, -8);
            focusChanged.invoke(null, true); focusChanged.invoke(null, true);
            check(capture.invoke(motion) == sameFocusSession, "repeated identical focus state retired an owned session");
            raw.method_1972();
            check(raw.field_2586 == 6 && raw.field_2587 == 8, "repeated identical focus state discarded pending movement");
            reports.add("repeated identical focus assignments preserve the session and pending counts");

            Object stale = capture.invoke(motion);
            Mouse.setGrabbed(false);
            samples(stale, 1000, -1000); add.invoke(motion, 2000, -2000); raw.method_1972();
            stationary(mc, raw, "released native cursor");
            Mouse.setGrabbed(true); raw.method_1972();
            stationary(mc, raw, "first regrab frame");
            add.invoke(motion, 3, -2); raw.method_1972();
            check(raw.field_2586 == 3 && raw.field_2587 == 2, "fresh regrab counts were delayed or discarded");
            reports.add("release/regrab rejects old counts and accepts fresh counts");

            stale = capture.invoke(motion);
            Mouse.setGrabbed(false); Mouse.setGrabbed(true);
            samples(stale, 3000, -3000); raw.method_1972();
            stationary(mc, raw, "release/regrab in one frame");
            reports.add("same-frame grab pair retires its old session");

            stale = capture.invoke(motion);
            focus.invoke(backend, false, 0);
            check(!Display.isActive(), "synthetic accepted FocusOut did not reach native active state");
            samples(stale, 4000, -4000); add.invoke(motion, 5000, -5000); raw.method_1972();
            stationary(mc, raw, "unfocused display");
            focus.invoke(backend, true, 0); raw.method_1972();
            stationary(mc, raw, "first refocus frame");
            stale = capture.invoke(motion);
            focus.invoke(backend, false, 0); focus.invoke(backend, true, 0);
            samples(stale, 6000, -6000); raw.method_1972();
            stationary(mc, raw, "focus out/in in one native batch");
            add.invoke(motion, 2, -4); raw.method_1972();
            check(raw.field_2586 == 2 && raw.field_2587 == 4, "fresh refocus counts were not delivered");
            reports.add("synthetic accepted focus edges retire counts even without an intervening frame");

            Field windowMode = field(backend.getClass(), "current_window_mode");
            int originalMode = windowMode.getInt(null);
            try {
                enabled.setBoolean(null, false);
                focus.invoke(backend, false, 0); // Accepted while raw input is disabled.
                windowMode.setInt(null, 1); // Only simulate isActive's legacy-fullscreen predicate.
                check(Display.isActive(), "legacy-fullscreen native active predicate was not simulated");
                enabled.setBoolean(null, true);
                check(!(boolean)refresh.invoke(null), "legacy fullscreen captured an unfocused display after re-enable");
                add.invoke(motion, 6500, -6500); raw.method_1972();
                stationary(mc, raw, "legacy fullscreen reporting active after accepted FocusOut");
            } finally {
                windowMode.setInt(null, originalMode); enabled.setBoolean(null, true);
                focus.invoke(backend, true, 0); refresh.invoke(null);
            }
            reports.add("synthetic legacy-fullscreen active predicate cannot override accepted focus loss, including disable/re-enable");

            var entered = new CountDownLatch(1); var finish = new CountDownLatch(1);
            var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
            Thread late = new Thread(() -> {
                try {
                    Object old = capture.invoke(motion); entered.countDown();
                    if (!finish.await(2, TimeUnit.SECONDS)) throw new AssertionError("late producer timed out");
                    samples(old, 7000, -7000);
                } catch (Throwable error) { failure.set(error); }
            }, "power-validation-synthetic-raw-poll");
            late.start();
            try {
                check(entered.await(2, TimeUnit.SECONDS), "synthetic poll did not enter");
                Mouse.setGrabbed(false); Mouse.setGrabbed(true);
            } finally { finish.countDown(); late.join(2000); }
            check(!late.isAlive() && failure.get() == null, "synthetic producer failed: " + failure.get());
            raw.method_1972(); stationary(mc, raw, "late worker poll after a new grab session");
            reports.add("latched synthetic late poll cannot enter a new session");

            Mouse.setGrabbed(false); mc.currentScreen = new Screen() {};
            Field dx = field(Mouse.class, "dx"), dy = field(Mouse.class, "dy"), queue = field(Mouse.class, "readBuffer");
            int oldDx = dx.getInt(null), oldDy = dy.getInt(null); Object oldQueue = queue.get(null);
            var nativeQueue = ByteBuffer.allocate(22);
            nativeQueue.put((byte)0).put((byte)1).putInt(17).putInt(23).putInt(0).putLong(123456).flip();
            try {
                var nativeHelper = new net.minecraft.class_596(mc.canvas);
                dx.setInt(null, 13); dy.setInt(null, -9); nativeHelper.method_1972();
                dx.setInt(null, 13); dy.setInt(null, -9); queue.set(null, nativeQueue);
                add.invoke(motion, 8000, -8000); raw.method_1972();
                check(raw.field_2586 == nativeHelper.field_2586 && raw.field_2587 == nativeHelper.field_2587,
                        "GUI raw helper diverged from native polling");
                check(nativeQueue.position() == 0 && nativeQueue.remaining() == 22,
                        "GUI raw helper consumed the queued native click");
            } finally { dx.setInt(null, oldDx); dy.setInt(null, oldDy); queue.set(null, oldQueue); }
            reports.add("GUI uses native polling and leaves its synthetic native click queued");

            mc.currentScreen = null; Mouse.setGrabbed(true); refresh.invoke(null);
            oldDx = dx.getInt(null); oldDy = dy.getInt(null); oldQueue = queue.get(null);
            Method replace = handler.getDeclaredMethod("replaceRawHelper"); replace.setAccessible(true);
            try {
                dx.setInt(null, 9000); dy.setInt(null, -9000); nativeQueue.position(0); queue.set(null, nativeQueue);
                replace.invoke(null);
                check(!helper.isInstance(mc.field_2767), "fallback did not install native helper");
                mc.field_2767.method_1972();
                stationary(mc, mc.field_2767, "raw-to-native fallback with accumulated native counters");
                check(nativeQueue.position() == 0 && nativeQueue.remaining() == 22,
                        "raw-to-native fallback consumed a queued click");
                dx.setInt(null, 11); dy.setInt(null, -6);
                replace.invoke(null); // Already-native input must retain deliberate movement.
                mc.field_2767.method_1972();
                check(mc.field_2767.field_2586 == 11 && mc.field_2767.field_2587 == -6,
                        "already-native fallback discarded fresh native movement");
                check(nativeQueue.position() == 0, "native fallback polling consumed a queued click");
            } finally { dx.setInt(null, oldDx); dy.setInt(null, oldDy); queue.set(null, oldQueue); }
            reports.add("raw-to-native fallback discards old relative counters, preserves queued clicks and fresh native movement");
        } finally {
            handler.getMethod("suspendCapture").invoke(null);
            if (focused.getBoolean(backend) != wasFocused) focus.invoke(backend, wasFocused, 0);
            enabled.setBoolean(null, wasEnabled); mc.currentScreen = screen; mc.field_2767 = originalHelper;
            Mouse.setGrabbed(grabbed); clear.invoke(motion); refresh.invoke(null);
            mc.player.yaw = yaw; mc.player.prevYaw = previousYaw; mc.player.pitch = pitch; mc.player.prevPitch = previousPitch;
            if (wasRunning) handler.getMethod("startInputThread").invoke(null);
        }
        Path report = Path.of("power-beta-data/reports/release-473/raw-ownership"); Files.createDirectories(report);
        Files.writeString(report.resolve("synthetic.json"), Catalog.JSON.toJson(Map.of("source", "synthetic paired counts; no hardware motion injected",
                "focus", "synthetic LinuxDisplay accepted focus notifications; not a window-manager latency test", "checks", reports)));
        log("RAW OWNERSHIP473 synthetic native-helper/grab/focus/session/GUI checks PASS");
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field result = owner.getDeclaredField(name); result.setAccessible(true); return result;
    }
    private static void samples(Object samples, int x, int y) throws Exception {
        samples.getClass().getMethod("add", int.class, int.class).invoke(samples, x, y);
    }
    private static void stationary(Minecraft mc, net.minecraft.class_596 mouse, String state) {
        check(mouse.field_2586 == 0 && mouse.field_2587 == 0, state + " emitted stale raw counts");
        float yaw = mc.player.yaw, pitch = mc.player.pitch;
        mc.player.method_1362(mouse.field_2586, mouse.field_2587);
        check(mc.player.yaw == yaw && mc.player.pitch == pitch, state + " turned the native camera");
    }
}
