package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.storage.PowerConfig;
import net.glasslauncher.mods.gcapi3.impl.GCCore;
import net.glasslauncher.mods.gcapi3.impl.object.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/** Native bucket exchanges, container clicks and saved stacks in a disposable world. */
final class BucketStackChecks {
  private static final String ID = "power_compat:config.STACK_SIZE_CONFIG.stackableEmptyBuckets";
  private static Minecraft client;

  static void run(Minecraft mc, String action) throws Exception {
    client = mc;
    if (action.equals("saved")) {
      check(Release110Checks.find(SettingsRegistry.open(mc), ID).value.getAsBoolean(), "setting lost on restart");
      stack(mc.player.inventory.main[8], 16, "saved-inventory");
      stack(((Inventory)mc.world.method_1777(2, 100, 2)).getStack(0), 16, "saved-chest");
      log("PASS bucket option and inventory/chest counts, damage and NBT survived restart"); return;
    }
    failures = 0;
    mc.setScreen(null); mc.player.closeScreen();
    ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);
    CreativeVehicleChecks.mode(mc, "SURVIVAL");
    ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(false);
    mc.player.method_1340(.5, 103, .5);
    mc.player.yaw = mc.player.prevYaw = 0; mc.player.pitch = mc.player.prevPitch = 90;
    for (int x=-2;x<=3;x++) for (int z=-2;z<=3;z++) {
      mc.world.method_200(x,100,z,1);
      for (int y=101;y<=106;y++) mc.world.method_200(x,y,z,0);
    }
    test("default off, Inventory / Stack sizes, no cheats; Cancel changes no stacks or saved data", () -> {
      enable(false); clear(); ItemStack held = tagged(8, "preview"); mc.player.inventory.main[0] = held;
      var session = SettingsRegistry.open(mc); var row = Release110Checks.find(session, ID);
      check(!row.defaultValue.getAsBoolean() && !row.restart && row.page.equals("Inventory")
          && row.group.equals("Stack sizes") && SettingAccess.visible(session,row), "wrong metadata/access");
      byte[] before=Files.readAllBytes(PowerConfig.path());
      row.parse("true"); session.preview(true); check(limit(325)==16,"preview not immediate");
      stack(held,8,"preview"); session.discard(); check(limit(325)==1,"Cancel did not restore limit");
      stack(held,8,"preview"); check(Arrays.equals(before,Files.readAllBytes(PowerConfig.path())),"Cancel saved data");
    });
    test("only empty buckets gain a live limit of 16", () -> {
      enable(true); check(limit(325)==16,"empty limit");
      for(int id:new int[]{326,327,335,267,257}) check(limit(id)==1,"changed item "+id);
      check(limit(1)==64 && limit(332)==16,"ordinary stack limits changed");
    });
    test("vanilla servers lock Beta limits and restore the local preference on exit", () -> {
      ConfigCategoryHandler root=GCCore.MOD_CONFIGS.get("power_compat:config").configCategoryHandler();
      ConfigCategoryHandler category=(ConfigCategoryHandler)root.values.values().stream()
          .filter(h->h.id.equals("STACK_SIZE_CONFIG")).findFirst().orElseThrow();
      ConfigEntryHandler<?> entry=(ConfigEntryHandler<?>)category.values.values().stream()
          .filter(h->h.id.equals("stackableEmptyBuckets")).findFirst().orElseThrow();
      byte[] before=Files.readAllBytes(PowerConfig.path());
      try {
        entry.vanillaServerBehavior(); check(limit(325)==1 && entry.multiplayerLoaded,"vanilla limit not enforced");
      } finally { entry.undoVanillaServerBehaviour(); }
      check(limit(325)==16 && !entry.multiplayerLoaded,"local preference not restored");
      check(Arrays.equals(before,Files.readAllBytes(PowerConfig.path())),"server override saved local settings");
    });
    test("native inventory pickup splits 20 buckets into 16 and 4", () -> {
      clear(); ItemStack pickup=new ItemStack(325,20,0); check(mc.player.inventory.method_671(pickup),"pickup failed");
      check(pickup.count==0 && mc.player.inventory.main[0].count==16 && mc.player.inventory.main[1].count==4,"pickup count/limit");
    });
    test("native slot clicks cap merges at 16 and split without loss", () -> {
      clear(); var container=mc.player.playerContainer;
      mc.player.inventory.main[0]=new ItemStack(325,10,0);
      mc.player.inventory.setCursorStack(new ItemStack(325,10,0));
      container.onSlotClick(36,0,false,mc.player);
      check(mc.player.inventory.main[0].count==16 && mc.player.inventory.getCursorStack().count==4,"merge overflow");
      mc.player.inventory.setCursorStack(null); container.onSlotClick(36,1,false,mc.player);
      check(mc.player.inventory.main[0].count==8 && mc.player.inventory.getCursorStack().count==8,"split lost items");
      container.onSlotClick(37,0,false,mc.player);
      check(mc.player.inventory.main[1].count==8 && mc.player.inventory.getCursorStack()==null,"placement lost items");
    });
    test("water and lava source pickup consume one and retain empty count/damage/NBT", () -> {
      for(int water:new int[]{9,11}) for(int count:new int[]{1,2,16}) {
        clear(); ItemStack held=tagged(count,"fluid"); mc.player.inventory.main[0]=held;
        ItemStack result=use(held,water,0);
        check(mc.world.getBlockId(0,101,0)==0,"source not collected");
        if(count==1) check(result.itemId==filledId(water) && result.count==1,"single did not become filled");
        else { check(result==held,"remaining stack replaced"); stack(held,count-1,"fluid"); check(total(filledId(water))==1,"filled bucket lost/duplicated"); }
      }
    });
    test("flowing water and dry block use leave the stack untouched", () -> {
      for(int block:new int[]{8,1,0}) {
        clear(); ItemStack held=tagged(16,"miss"); mc.player.inventory.main[0]=held;
        check(use(held,block,block==8?3:0)==held,"failed use replaced stack"); stack(held,16,"miss");
      }
    });
    test("full inventories drop exactly one filled bucket and retain 15 empties", () -> {
      for(int block:new int[]{9,11}) {
        clear(); fillInventory(); ItemStack held=tagged(16,"overflow"); mc.player.inventory.main[0]=held;
        int before=drops(filledId(block)); check(use(held,block,0)==held,"held stack replaced");
        stack(held,15,"overflow"); check(drops(filledId(block))==before+1,"overflow count");
        check(total(filledId(block))==0,"overflow inserted a duplicate");
      }
    });
    test("cow and optional squid milking consume one bucket", () -> {
      Object config=Class.forName("local.luke.power.compat.Config").getField("config").get(null);
      var squidSetting=config.getClass().getField("milkSquids"); Object previous=squidSetting.get(config);
      try {
        squidSetting.set(config,true);
        for(Entity animal:new Entity[]{new class_466(mc.world),new class_151(mc.world)}) for(int count:new int[]{1,2,16}) {
          clear(); ItemStack held=tagged(count,"milk"); mc.player.inventory.main[0]=held;
          check(animal.method_1323(mc.player),"milking failed");
          if(count==1) check(mc.player.inventory.main[0].itemId==335,"single milk bucket");
          else { stack(held,count-1,"milk"); check(mc.player.inventory.main[0]==held && total(335)==1,"milk exchange lost items"); }
        }
      } finally { squidSetting.set(config,previous); }
    });
    test("full-inventory milking drops exactly one milk bucket", () -> {
      clear(); fillInventory(); ItemStack held=tagged(16,"full-milk"); mc.player.inventory.main[0]=held;
      int before=drops(335); check(new class_466(mc.world).method_1323(mc.player),"milking failed");
      stack(held,15,"full-milk"); check(drops(335)==before+1 && total(335)==0,"milk overflow count");
    });
    test("placing filled water/lava and consuming milk return one empty bucket", () -> {
      for(int filled:new int[]{326,327,335}) {
        clear(); ItemStack result=use(new ItemStack(filled,1,0),0,0);
        check(result.itemId==325 && result.count==1,"empty result count/type");
      }
    });
    test("creative source pickup and milking preserve the empty stack without duplicate filled items", () -> {
      ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);
      try {
        CreativeVehicleChecks.mode(mc,"CREATIVE");
        for(int count:new int[]{1,16}) for(int block:new int[]{9,11}) {
          clear(); ItemStack held=tagged(count,"creative"); mc.player.inventory.main[0]=held;
          check(use(held,block,0)==held,"creative replaced held stack"); stack(held,count,"creative");
          check(total(filledId(block))==1,"creative output missing");
          use(held,block,0); check(total(filledId(block))==1,"creative output duplicated");
        }
        clear(); ItemStack held=tagged(16,"creative-milk"); mc.player.inventory.main[0]=held;
        new class_466(mc.world).method_1323(mc.player); new class_466(mc.world).method_1323(mc.player);
        stack(held,16,"creative-milk"); check(total(335)==1,"creative milk duplicated/lost");
      } finally { CreativeVehicleChecks.mode(mc,"SURVIVAL"); ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(false); }
    });
    test("disabling restores Beta limits without deleting, splitting or breaking saved stacks", () -> {
      clear(); ItemStack held=tagged(16,"disabled"); mc.player.inventory.main[0]=held;
      enable(false); check(limit(325)==1,"off limit"); stack(held,16,"disabled");
      check(use(held,9,0)==held,"off discarded stack"); stack(held,15,"disabled"); check(total(326)==1,"off exchange lost items");
      NbtCompound data=new NbtCompound(); held.writeNbt(data); stack(new ItemStack(data),15,"disabled");
      clear(); mc.player.inventory.main[0]=new ItemStack(325,1,0);
      check(use(mc.player.inventory.main[0],9,0).itemId==326,"Beta single use changed");
      ItemStack pickup=new ItemStack(325,2,0); clear(); mc.player.inventory.method_671(pickup);
      check(mc.player.inventory.main[0].count==1 && mc.player.inventory.main[1].count==1,"off pickup stacked");
    });
    test("Apply persists the option and prepared inventory/chest stacks", () -> {
      var session=SettingsRegistry.open(mc); Release110Checks.find(session,ID).parse("true"); session.save(Path.of(".").toAbsolutePath());
      check(limit(325)==16 && PowerConfig.section("power_compat:config").getAsJsonObject("STACK_SIZE_CONFIG")
          .get("stackableEmptyBuckets").getAsBoolean(),"Apply did not save");
      clear(); mc.player.inventory.main[8]=tagged(16,"saved-inventory");
      mc.world.method_200(2,100,2,54); Inventory chest=(Inventory)mc.world.method_1777(2,100,2);
      chest.setStack(0,tagged(16,"saved-chest")); chest.markDirty();
    });
    log("BUCKET STACK FAILURES "+failures);
  }

  private static void enable(boolean on) throws Exception {
    var session=SettingsRegistry.open(client); Release110Checks.find(session,ID).parse(Boolean.toString(on)); session.preview(true);
  }
  private static int limit(int id) { return new ItemStack(id,1,0).getMaxCount(); }
  private static int filledId(int block) { return block==9?326:327; }
  private static void clear() { Arrays.fill(client.player.inventory.main,null); client.player.inventory.selectedSlot=0; client.player.inventory.setCursorStack(null); }
  private static void fillInventory() { for(int i=0;i<36;i++) client.player.inventory.main[i]=new ItemStack(1,64,0); }
  private static NbtCompound tag(ItemStack item) throws Exception { return (NbtCompound)item.getClass().getMethod("getStationNbt").invoke(item); }
  private static ItemStack tagged(int count,String name) throws Exception { ItemStack item=new ItemStack(325,count,7); tag(item).putString("bucket-check",name); return item; }
  private static void stack(ItemStack item,int count,String name) throws Exception {
    check(item!=null && item.itemId==325 && item.count==count && item.getDamage()==7 && tag(item).getString("bucket-check").equals(name),"empty count/damage/NBT changed: "+name);
  }
  private static ItemStack use(ItemStack held,int block,int meta) {
    client.world.method_154(0,101,0,block,meta);
    client.player.prevX=client.player.x; client.player.prevY=client.player.y; client.player.prevZ=client.player.z;
    return held.method_698(client.world,client.player);
  }
  private static int total(int id) { return Arrays.stream(client.player.inventory.main).filter(Objects::nonNull).filter(i->i.itemId==id).mapToInt(i->i.count).sum(); }
  private static int drops(int id) { int n=0; for(Object e:client.world.field_198) if(e instanceof class_142 d && !d.dead && d.field_564.itemId==id) n+=d.field_564.count; return n; }
}
