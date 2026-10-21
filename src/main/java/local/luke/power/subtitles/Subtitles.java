package local.luke.power.subtitles;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import local.luke.power.input.*;
import local.luke.power.mixin.*;
import net.minecraft.class_564;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.client.sound.SoundManager;

/** Sound admission never reads terrain, changes playback or resolves a random sound twice. */
public final class Subtitles {
  private record Pending(long owner, SubtitleState.Cue cue) {}
  private static final int MAX_PENDING = 1024;
  private static final ArrayBlockingQueue<Pending> PENDING = new ArrayBlockingQueue<>(MAX_PENDING);
  private static final SubtitleState STATE = new SubtitleState();
  private static final AtomicLong DROPPED = new AtomicLong();
  private static volatile Object world;
  private static volatile long owner;
  private static volatile SubtitleState.Listener listener;
  private Subtitles() {}
  public static void initialize() {
    SubtitleConfig.current();
    AudioListenerPose.register(Subtitles::listener);
    SubtitleCatalog.captions();
  }
  public static boolean enabled() { return SubtitleConfig.current().enabled; }
  public static synchronized void clear() { owner++; PENDING.clear(); STATE.clear(); listener = null; }
  public static void tick(Minecraft mc) {
    if (!enabled()) return;
    context(mc.world);
    drain(now(), false);
  }
  public static void leave() { context(null); }
  private static synchronized void context(Object next) { if (world != next) { world = next; clear(); } }
  private static synchronized void listener(Object source, double x, double y, double z, float fx, float fz) {
    if (source != world || source == null || !enabled()) return;
    var pose = new SubtitleState.Listener(x, y, z, fx, fz); if (pose.valid()) listener = pose;
  }
  public static long dropped() { return DROPPED.get() + STATE.saturated(); }
  public static int pending() { return PENDING.size(); }
  public static void positional(SoundManager manager, String sound, double x, double y, double z, float volume) {
    admit(null, manager, sound, x, y, z, volume, false);
  }
  public static void positional(Object sourceWorld, SoundManager manager, String sound, double x, double y, double z, float volume) {
    admit(sourceWorld, manager, sound, x, y, z, volume, false);
  }
  public static void relative(SoundManager manager, String sound, float volume) {
    if (!"portal.travel".equals(sound) && !"portal.trigger".equals(sound)) return;
    admit(null, manager, sound, 0, 0, 0, volume, true);
  }
  private static void admit(Object sourceWorld, SoundManager manager, String sound, double x, double y, double z, float volume, boolean relative) {
    long generation = owner; Object current = world;
    if (!enabled() || current == null || sourceWorld != null && sourceWorld != current || manager == null || sound == null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
        || !Float.isFinite(volume) || volume <= 0) return;
    var caption = SubtitleCatalog.resolve(sound, SoundCaptionContext.claim()); if (caption == null) return;
    var pool = ((SoundManagerAccessor)manager).power$sounds(); if (pool == null) return;
    synchronized (pool) {
      var registrations = ((SoundPoolAccessor)pool).power$groups().get(sound);
      if (registrations == null || registrations.isEmpty()) return;
    }
    var cue = new SubtitleState.Cue(caption, now(), x, y, z, relative ? Double.POSITIVE_INFINITY : 16D * Math.max(1, volume), false, relative);
    if (!enabled() || generation != owner || current != world) return;
    var pending = new Pending(generation, cue);
    if (!PENDING.offer(pending)) { PENDING.poll(); if (!PENDING.offer(pending)) DROPPED.incrementAndGet(); DROPPED.incrementAndGet(); }
  }
  private static synchronized List<SubtitleState.Visible> drain(long now, boolean visible) {
    var settings = SubtitleConfig.current(); int count = 0;
    for (Pending event; count++ < MAX_PENDING && (event = PENDING.poll()) != null; )
      if (event.owner == owner) STATE.accept(event.cue, now, settings.displaySeconds / 3, listener);
    if (visible) return STATE.visible(now, settings.displaySeconds / 3, listener);
    STATE.expire(now, settings.displaySeconds / 3); return List.of();
  }
  public static void render(Minecraft mc) {
    if (!enabled()) return;
    if (world != mc.world) { tick(mc); }
    List<SubtitleState.Visible> visible = drain(now(), true);
    var settings = SubtitleConfig.current();
    if (mc.world == null || mc.player == null || mc.options.hideHud || settings.hideWithDebug && mc.options.debugHud
        || !settings.showInInventory && mc.currentScreen instanceof ContainerScreen) return;
    var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
    int width = size.method_1857(), height = size.method_1858();
    var layout = SubtitleLayout.at(settings, visible, width, height, mc.textRenderer::getWidth);
    if (!visible.isEmpty() && SubtitleLayout.defaultPosition(settings)) layout = layout.avoidActiveTweaks(settings,
        local.luke.power.status.ActiveTweaks.bounds(mc, width, height));
    SubtitleRenderer.draw(mc, layout);
  }
  private static long now() { return System.nanoTime() / 1_000_000; }
}
