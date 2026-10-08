package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import local.luke.power.permissions.CheatWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.class_27;
import net.minecraft.util.math.Vec3d;

final class Release110InputChecks {
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("state")) {
      var b = Release110Checks.buffer();
      log("CHAT text="+b.text()+" cursor="+b.cursor()+" selection="+b.selection());
      var sprint = Class.forName("local.luke.power.creative.FlightController").getDeclaredField("sprinting");sprint.setAccessible(true);
      Object camera = Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
      log("FLIGHT sprint="+sprint.getBoolean(null)+" velocity="+Math.hypot(mc.player.velocityX,mc.player.velocityZ)
          +" camera="+camera.getClass().getMethod("isActive").invoke(camera)+" cameraSpeed="+camera.getClass().getMethod("movementSpeed").invoke(camera));
      return;
    }
    if (action.equals("flight") || action.equals("freecam")) {
      mc.setScreen(null);((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);CreativeVehicleChecks.mode(mc,"CREATIVE");
      mc.player.method_1340(mc.player.x,110,mc.player.z);mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,true);
      mc.player.field_1623=false;mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;
      Object camera=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
      camera.getClass().getMethod("setActive",boolean.class).invoke(camera,action.equals("freecam"));
      return;
    }
    if (action.equals("pick")) {
      failures=0;
      var inventory=mc.player.inventory.main.clone();int slot=mc.player.inventory.selectedSlot;var target=mc.field_2823;
      boolean cheats=((CheatWorld)mc.world.method_262()).power$cheatsEnabled();
      try {
        ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);
        for(String mode:new String[]{"CREATIVE","SURVIVAL"}) test("powered redstone pick returns dust in "+mode,()->{
          CreativeVehicleChecks.mode(mc,mode);java.util.Arrays.fill(mc.player.inventory.main,null);mc.player.inventory.selectedSlot=0;
          if(mode.equals("SURVIVAL"))mc.player.inventory.main[15]=new ItemStack(331,17,0);
          mc.world.method_201(0,112,0,1,0);mc.world.method_201(0,113,0,55,15);
          mc.field_2823=new class_27(0,113,0,1,Vec3d.createCached(.5,113,.5));
          var pick=Minecraft.class.getDeclaredMethod("method_2103");pick.setAccessible(true);pick.invoke(mc);
          ItemStack held=mc.player.inventory.main[mc.player.inventory.selectedSlot];
          check(held!=null&&held.itemId==331&&held.getDamage()==0,"picked wrong item");
          check(held.count==(mode.equals("SURVIVAL")?17:1),"wrong count");
        });
      } finally {System.arraycopy(inventory,0,mc.player.inventory.main,0,inventory.length);mc.player.inventory.selectedSlot=slot;mc.field_2823=target;((CheatWorld)mc.world.method_262()).power$cheatsEnabled(cheats);}
      log("REDSTONE PICK FAILURES "+failures);
    }
  }
}
