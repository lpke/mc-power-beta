package local.luke.power.bow;

import local.luke.power.input.CameraView;
import local.luke.power.input.DetachedCamera;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Tessellator;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/** Official 26.3 critical-hit sprite and particle motion, isolated from terrain/entity callbacks. */
public final class BowCriticalParticle extends Particle {
  private static final String TEXTURE = "/assets/powerbeta/textures/bow/crit.png";

  public static void trail(World world, double x, double y, double z, double dx, double dy, double dz) {
    if (!ModernBow.enabled()) return;
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    if (mc.world != world || mc.field_2808 == null) return;
    for (int i = 0; i < 4; i++) mc.field_2808.method_325(new BowCriticalParticle(world,
        x + dx * i / 4, y + dy * i / 4, z + dz * i / 4, -dx, -dy + .2, -dz));
  }

  public BowCriticalParticle(World world, double x, double y, double z, double dx, double dy, double dz) {
    super(world, x, y, z, 0, 0, 0);
    field_1631 = 0;
    method_1340(x, y, z);
    prevX = x; prevY = y; prevZ = z;
    double vx = (random.nextFloat() * 2 - 1) * .4f,
        vy = (random.nextFloat() * 2 - 1) * .4f,
        vz = (random.nextFloat() * 2 - 1) * .4f;
    double speed = (random.nextFloat() + random.nextFloat() + 1) * .15f,
        length = Math.sqrt(vx * vx + vy * vy + vz * vz);
    velocityX = (length == 0 ? 0 : vx / length * speed * .4f) * .1f + dx * .4;
    velocityY = ((length == 0 ? 0 : vy / length * speed * .4f) + .1f) * .1f + dy * .4;
    velocityZ = (length == 0 ? 0 : vz / length * speed * .4f) * .1f + dz * .4;
    red = green = blue = random.nextFloat() * .3f + .6f;
    scale = (random.nextFloat() * .5f + .5f) * 2 * .75f;
    maxAge = Math.max((int)(6 / (random.nextFloat() * .8 + .6)), 1);
    tick();
  }

  @Override public void tick() {
    if (!ModernBow.enabled()) { markDead(); return; }
    prevX = x; prevY = y; prevZ = z;
    if (field_2638++ >= maxAge) { markDead(); return; }
    velocityY -= .04 * .5f;
    method_1340(x + velocityX, y + velocityY, z + velocityZ);
    velocityX *= .7f; velocityY *= .7f; velocityZ *= .7f;
    green *= .96f; blue *= .9f;
  }

  /** Native layer three lets this sprite own its texture and complete draw batch. */
  @Override public int method_2003() { return 3; }

  @Override public void method_2002(Tessellator t, float delta, float a, float b, float c, float d, float e) {
    if (!ModernBow.enabled()) return;
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    if (mc.field_2807 == null || mc.world != world) return;
    float yaw = CameraView.yaw(mc.field_2807.prevYaw + (mc.field_2807.yaw - mc.field_2807.prevYaw) * delta);
    float pitch = CameraView.pitch(mc.field_2807.prevPitch + (mc.field_2807.pitch - mc.field_2807.prevPitch) * delta);
    if (DetachedCamera.isActive()) { yaw = (float)DetachedCamera.yaw(); pitch = (float)DetachedCamera.pitch(); }
    else if (mc.options.thirdPerson && CameraView.front()) { yaw += 180; pitch = -pitch; }
    float cos = (float)Math.cos(Math.toRadians(yaw)), sin = (float)Math.sin(Math.toRadians(yaw)),
        vertical = (float)Math.cos(Math.toRadians(pitch)),
        xTilt = -sin * (float)Math.sin(Math.toRadians(pitch)),
        zTilt = cos * (float)Math.sin(Math.toRadians(pitch));
    float size = .1f * scale * Math.min(1, (field_2638 + delta) / maxAge * 32);
    double px = prevX + (x - prevX) * delta - field_2645,
        py = prevY + (y - prevY) * delta - field_2646,
        pz = prevZ + (z - prevZ) * delta - field_2647;
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_COLOR_BUFFER_BIT);
    try {
      mc.textureManager.bindTexture(mc.textureManager.getTextureId(TEXTURE));
      GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_ALPHA_TEST);
      GL11.glAlphaFunc(GL11.GL_GREATER, .1f); GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_BLEND);
      float light = method_1394(delta);
      t.startQuads(); t.color(red * light, green * light, blue * light);
      t.vertex(px - cos * size - xTilt * size, py - vertical * size, pz - sin * size - zTilt * size, 1, 1);
      t.vertex(px - cos * size + xTilt * size, py + vertical * size, pz - sin * size + zTilt * size, 1, 0);
      t.vertex(px + cos * size + xTilt * size, py + vertical * size, pz + sin * size + zTilt * size, 0, 0);
      t.vertex(px + cos * size - xTilt * size, py - vertical * size, pz + sin * size - zTilt * size, 0, 1);
      t.draw();
    } finally { GL11.glPopAttrib(); }
  }
}
