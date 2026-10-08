package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.Path;
import local.luke.power.config.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.validation.mixin.ChatInput;
import local.luke.power.worldedit.carry.ContainerCarry;
import local.luke.power.worldedit.core.*;
import local.luke.power.worldedit.MinecraftWorld;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/** Repeatable, anonymous feature scenes. Only install in a disposable instance. */
final class GalleryChecks {
  private static int x, y, z;
  private static void setting(Minecraft mc, String id, String value) throws Exception {
    var session = SettingsRegistry.open(mc); find(session, id).parse(value);
    session.save(Path.of(".").toAbsolutePath());
  }
  private static void block(Minecraft mc, int dx, int dy, int dz, int id, int data) {
    new MinecraftWorld(mc.world).set(new Pos(x + dx, y + dy, z + dz), new BlockValue(id, data));
  }
  private static void camera(Minecraft mc, double dx, double dy, double dz, float yaw, float pitch) {
    mc.setScreen(null);
    try { mc.player.getClass().getMethod("creative_setFlying", boolean.class).invoke(mc.player,true); }
    catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    mc.player.field_1623 = false; mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
    mc.player.method_1340(x + dx, y + dy, z + dz);
    mc.player.yaw = mc.player.prevYaw = yaw; mc.player.pitch = mc.player.prevPitch = pitch;
  }
  static void run(Minecraft mc, String action) throws Exception {
    switch (action) {
      case "setup" -> {
        ((CheatWorld) mc.world.method_262()).power$cheatsEnabled(true);
        ChatChecks.submit(mc, "/gamemode creative");
        ChatChecks.submit(mc, "/time set noon");
        ChatChecks.submit(mc, "/weather clear");
        setting(mc, "native.guiScale", "2");
        x = (int) Math.floor(mc.player.x); y = (int) Math.floor(mc.player.boundingBox.minY); z = (int) Math.floor(mc.player.z);
        for (int dx = -12; dx <= 12; dx++) for (int dz = -12; dz <= 12; dz++) {
          block(mc, dx, -1, dz, 2, 0);
          for (int dy = 0; dy < 10; dy++) block(mc, dx, dy, dz, 0, 0);
        }
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
          block(mc, dx, -1, dz, 4, 0);
          for (int dy = 0; dy <= 4; dy++) {
            boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 4;
            if (dy == 4) block(mc, dx, dy, dz, 5, 0);
            else if (edge) block(mc, dx, dy, dz, Math.abs(dx) == 4 && Math.abs(dz) == 4 ? 17 : dy > 0 && dy < 3 ? 20 : 5, 0);
          }
        }
        for (int dy = 0; dy < 3; dy++) for (int dx = -1; dx <= 1; dx++) block(mc, dx, dy, -4, 0, 0);
        block(mc, -3, 0, -2, 58, 0); block(mc, -3, 0, 0, 61, 0);
        block(mc, 2, 0, -2, 54, 0);
        Inventory chest = (Inventory) mc.world.method_1777(x + 2, y, z - 2);
        for (int i = 0; i < 9; i++) chest.setStack(i, new ItemStack(new int[]{4,5,20,17,50,264,265,331,35}[i], i == 5 ? 8 : 64, 0));
        mc.player.inventory.main[0] = new ItemStack(5,64,0);
        mc.player.inventory.main[1] = new ItemStack(4,64,0);
        mc.player.inventory.main[2] = new ItemStack(20,64,0);
        mc.player.inventory.main[3] = new ItemStack(50,64,0);
        mc.player.inventory.main[4] = new ItemStack(331,64,0);
        camera(mc, 9, 4, -10, 42, 14);
        log("GALLERY ORIGIN " + x + " " + y + " " + z);
      }
      case "worldedit" -> {
        ChatChecks.submit(mc, "//pos1 " + (x-4) + "," + (y-1) + "," + (z-4));
        ChatChecks.submit(mc, "//pos2 " + (x+4) + "," + (y+4) + "," + (z+4));
        camera(mc, 9, 4, -10, 42, 14);
      }
      case "chat" -> {
        mc.setScreen(new ChatScreen());
        ((ChatInput) mc.currentScreen).power$text("//set st");
        Class.forName("local.luke.power.worldedit.chat.ChatAccess").getMethod("setText",String.class).invoke(null,"//set st");
        ((ChatInput) mc.currentScreen).power$type('\0',org.lwjgl.input.Keyboard.KEY_DOWN);
      }
      case "sign" -> {
        block(mc, 0, 0, -7, 63, 0);
        var sign = (SignBlockEntity) mc.world.method_1777(x, y, z-7);
        mc.player.method_489(sign);
        var input = (local.luke.power.validation.mixin.ScreenInput) mc.currentScreen;
        for (char c : "Workshop".toCharArray()) input.power$key(c,0);
        input.power$key('\r',org.lwjgl.input.Keyboard.KEY_RETURN);
        for (char c : "Blocks & tools".toCharArray()) input.power$key(c,0);
      }
      case "redstone" -> {
        setting(mc, "visual.redstonePowerLevels", "true");
        for (int dx=-8; dx<=8; dx++) for (int dz=-11; dz<=-8; dz++) {
          block(mc,dx,-1,dz,35,0); block(mc,dx,0,dz,0,0);
        }
        for (int dx=-7; dx<=7; dx++) block(mc,dx,0,-10,55,0);
        mc.world.method_200(x-8,y,z-10,0);
        mc.world.method_200(x-8,y,z-10,76);
        camera(mc, -5, 3, -9.5, -90, 60);
      }
      case "preview", "carry" -> {
        setting(mc, "visual.containerPreview", "true");
        setting(mc, "visual.containerCarry", "true");
        block(mc, 7, 0, -7, 54, 0);
        Inventory chest = (Inventory) mc.world.method_1777(x+7,y,z-7);
        for (int i=0;i<9;i++) chest.setStack(i,new ItemStack(new int[]{4,5,20,17,50,264,265,331,35}[i],i==5?8:64,0));
        camera(mc, 7.5, 2.2, -9.5, 0, 34);
        mc.player.inventory.selectedSlot=8; mc.player.inventory.main[8]=null;
        if (action.equals("carry")) {
          mc.player.field_161.field_2536=true;
          mc.field_2823=new net.minecraft.class_27(x+7,y,z-7,1,Vec3d.createCached(x+7.5,y+.5,z-6.5));
          check(ContainerCarry.click(mc,1),"could not pick up gallery chest");
          mc.player.field_161.field_2536=false;
          camera(mc, 7.5, 2.2, -9.5, 35, 5);
        }
      }
      case "light" -> {
        mc.world.method_200(x+5,y,z-5,50);
        setting(mc,"lightOverlay.enabled","true");
        camera(mc,9,4,-10,42,24);
      }
      case "view" -> camera(mc, 9, 4, -10, 42, 14);
      default -> throw new IllegalArgumentException("Unknown gallery scene: " + action);
    }
  }
}
