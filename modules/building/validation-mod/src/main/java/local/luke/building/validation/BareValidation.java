package local.luke.building.validation;

import static local.luke.building.validation.ValidationRun.*;

import java.nio.file.*;
import local.luke.building.validation.mixin.ChatInvoker;
import local.luke.power.fastplace.*;
import local.luke.power.building.camera.*;
import local.luke.power.building.config.Config;
import local.luke.power.worldedit.WorldEditor;
import net.minecraft.SingleplayerInteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;

/** Smoke tests without StationAPI, ClientCommands, MojangFix, or CreativeInventory. */
public final class BareValidation {
  private static int stage;

  public static void tick(Minecraft mc) {
    try {
      if (stage == 0) {
        stage = 1;
        Files.deleteIfExists(Path.of("building-validation.log"));
        mc.options.difficulty = 0;
        mc.interactionManager = new SingleplayerInteractionManager(mc);
        String name = "Bare Laboratory " + System.currentTimeMillis();
        mc.method_2120(name, name, 17320261003L);
        mc.setScreen(null);
        return;
      }
      if (stage == 1 && mc.world != null && mc.player != null) {
        stage = 2;
        mc.field_2778 = true;
        mc.paused = false;
        for (int x = -5; x <= 5; x++)
          for (int z = -5; z <= 5; z++) {
            set(mc.world, x, 100, z, 1, 0);
            for (int y = 101; y < 109; y++) set(mc.world, x, y, z, 0, 0);
          }
        aim(mc, .5, 103, 3.5, .5, 101, .5);
        var config = Config.current().copy();
        config.freeLookPerspective = Perspective.FIRST_PERSON;
        config.freeLookToggle = false;
        config.freeLook = true;
        config.placement.setEnabled(true);
        config.placement.slabMode = SlabMode.DOUBLE;
        Config.apply(config);
        FastPlace.release();
        stack(mc, 44, 0);
        check(
            mc.interactionManager.method_1713(
                mc.player, mc.world, mc.player.inventory.getSelectedItem(), 0, 100, 0, 1),
            "bare pair placement");
        check(
            mc.world.getBlockId(0, 101, 0) == 43
                && mc.player.inventory.getSelectedItem().count == 62,
            "bare pair cost");
        log("PASS bare double slabs and inventory");
        var chat = new ChatScreen();
        mc.setScreen(chat);
        var access = (ChatInvoker) chat;
        access.revision$text("//pos");
        access.revision$key('\t', 15);
        check(access.revision$text().equals("//pos1"), "bare Tab");
        access.revision$key('\r', 28);
        check(WorldEditor.current().pos1 != null, "bare execution");
        mc.setScreen(new ChatScreen());
        access = (ChatInvoker) mc.currentScreen;
        access.revision$key('\0', 200);
        check(access.revision$text().equals("//pos1"), "bare history");
        mc.setScreen(null);
        log("PASS bare chat execution completion and history");
        FreeLook.KEY.code = 41;
        mc.options.thirdPerson = false;
        FreeLook.begin(mc);
        check(!mc.options.thirdPerson, "bare first person");
        float yaw = mc.player.yaw;
        FreeLook.turn(mc.player, 100, 100);
        check(mc.player.yaw == yaw && FreeLook.yaw(yaw) != yaw, "bare camera separation");
        FreeLook.reset(mc);
        log("PASS bare free look and key registration");
        log("BARE READY");
      }
      Path command = Path.of("building-validation.command");
      if (stage == 2 && Files.exists(command)) {
        String s = Files.readString(command).trim();
        Files.delete(command);
        if (s.equals("revision-state")) RevisionValidation.state(mc);
        if (s.equals("quit")) {
          mc.world.method_195(true, null);
          mc.scheduleStop();
        }
      }
    } catch (Throwable e) {
      stage = 3;
      log("FAIL bare " + e);
      e.printStackTrace();
    }
  }
}
