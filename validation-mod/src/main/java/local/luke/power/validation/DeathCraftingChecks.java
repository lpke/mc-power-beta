package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.DeathChecks.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.death.*;
import local.luke.power.world.WorldInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

final class DeathCraftingChecks {
  static void run(Minecraft mc, String action) throws Exception {
    boolean saved = action.equals("saved");
    failures = 0; alive(mc); mc.setScreen(null); mc.field_2807 = mc.player;
    var properties = AccessRules.of(mc.world.method_262()); properties.allAllowed(true);
    ((WorldInventory)mc.world.method_262()).power$keepInventory(false);
    var settings = DeathConfig.copy(); var enabled = new DeathSettings(); enabled.enabled = true; enabled.announceLocation = false; DeathConfig.preview(enabled);
    Object compat = Class.forName("local.luke.power.compat.Config").getField("config").get(null);
    var allow = compat.getClass().getField("allowCraftingInventorySlots"); boolean old = (Boolean)allow.get(compat);
    try {
      allow.set(compat,true); clearAll(mc);
      if(action.equals("rollback")) { rollback(mc); return; }
      if(action.equals("retry")) {
        var death=latest(mc); check(death.cause.equals("Crafting rollback fixture"),"missing rollback fixture");
        var screen=DeathInventoryChecks.open(mc,death,320,240); DeathInventoryChecks.bulk(screen);
        check(DeathItems.encode(grid(mc,0)).equals(death.items.get(0)),"retry changed crafting data");
        String after=snapshot(mc); DeathInventoryChecks.bulk(screen); check(after.equals(snapshot(mc)),"retry duplicated grid");
        log("PASS restart allows crafting rollback recovery once through the GUI"); return;
      }
      if (saved) {
        var death = latest(mc); check(death.cause.equals("Crafting restart fixture") && death.slots.equals(List.of(41,42,43,44)), "crafting positions did not persist");
        check(DeathTracker.journal(mc).remaining(death.id).equals(List.of(0,2,3)), "crafting claim did not persist");
        DeathTracker.recover(mc, death);
        for (int i : new int[]{0,2,3}) check(DeathItems.encode(grid(mc,i)).equals(death.items.get(i)), "saved crafting data changed");
        check(grid(mc,1)==null && DeathTracker.journal(mc).state(death.id).equals("recovered"), "partial claim duplicated or not finished");
        log("PASS crafting positions, full native data and partial recovery claims survive restart"); return;
      }
      Files.createDirectories(Path.of("power-beta-data/reports/death-crafting"));
      test("normal deaths snapshot all four inputs, exclude recipe output and drop inputs once with the tweak on or off", () -> {
        for(boolean on : new boolean[]{false,true}) {
          allow.set(compat,on); alive(mc); clearAll(mc);
          for(int i=0;i<4;i++) grid(mc,i,tagged(35,3+i,i,"craft-drop-"+i));
          var encoded = new ArrayList<String>(); for(int i=0;i<4;i++) encoded.add(DeathItems.encode(grid(mc,i)));
          int drops = drops(mc); mc.player.health=1; mc.player.damage(null,10);
          var death=latest(mc); check(death.slots.equals(List.of(41,42,43,44)) && death.items.equals(encoded), "craft inputs missing or output included");
          check(drops(mc)==drops+4, "death did not drop four input stacks");
          for(int i=0;i<4;i++) check(grid(mc,i)==null,"death retained crafting input");
          mc.player.method_938(null); mc.player.markDead(); check(drops(mc)==drops+4,"repeat death or closure duplicated drops");
        }
      });
      test("GUI recovers original inputs individually and together with full counts, damage and NBT", () -> {
        alive(mc); clearAll(mc); allow.set(compat,true);
        for(int i=0;i<4;i++) grid(mc,i,tagged(35,3+i,i,"craft-recover-"+i));
        record(mc,"Crafting recovery fixture"); var death=latest(mc); clearAll(mc);
        var screen=DeathInventoryChecks.open(mc,death,320,240); DeathInventoryChecks.click(screen,42,0);
        check(DeathItems.encode(grid(mc,1)).equals(death.items.get(1)),"individual input recovery changed data");
        DeathInventoryChecks.bulk(screen);
        for(int i=0;i<4;i++) check(DeathItems.encode(grid(mc,i)).equals(death.items.get(i)),"bulk input recovery changed position or NBT");
        String after=snapshot(mc); DeathInventoryChecks.bulk(screen); check(after.equals(snapshot(mc)),"repeat recovery duplicated grid");
        for(int[] size:new int[][]{{320,240},{640,420}}) {
          screen=DeathInventoryChecks.open(mc,death,size[0],size[1]);
          SettingsSnapshot.render(mc,screen,size[0],size[1],Path.of("power-beta-data/reports/death-crafting/ui-"+size[0]+".png"));
        }
        mc.setScreen(null);
      });
      test("occupied inputs abort without writes or claims; locked GUI and overflow keep protection", () -> {
        clearAll(mc); grid(mc,0,tagged(35,70,5,"craft-overflow")); grid(mc,1,tagged(267,1,83,"craft-tool")); record(mc,"Crafting conflict fixture"); var death=latest(mc); clearAll(mc);
        grid(mc,1,new ItemStack(1,1,0)); String before=snapshot(mc); byte[] journal=Files.readAllBytes(path(mc));
        var screen=DeathInventoryChecks.open(mc,death,320,240); var output=ChatChecks.record(()->DeathInventoryChecks.bulk(screen));
        check(output.stream().anyMatch(t->t.contains("occupied")) && before.equals(snapshot(mc)) && Arrays.equals(journal,Files.readAllBytes(path(mc))),"conflict touched grid or journal");
        grid(mc,1,null); properties.allAllowed(false); before=snapshot(mc); DeathInventoryChecks.click(screen,41,0);
        check(before.equals(snapshot(mc)),"locked viewer recovered crafting input"); properties.allAllowed(true);
        DeathInventoryChecks.bulk(screen); check(grid(mc,0).count==64 && mc.player.inventory.main[0].count==6 && grid(mc,1).getDamage()==83,"craft overflow lost counts or tool damage");
        mc.setScreen(null);
      });
      properties.allAllowed(true); clearAll(mc);
      for(int i=0;i<4;i++) grid(mc,i,tagged(35,3+i,i,"craft-restart-"+i));
      record(mc,"Crafting restart fixture"); var death=latest(mc); clearAll(mc); DeathTracker.recover(mc,death,1);
      DeathConfig.save(enabled);
      log("DEATH CRAFTING FAILURES "+failures);
    } finally { mc.setScreen(null); allow.set(compat,old); clearAll(mc); if(saved || failures!=0) DeathConfig.preview(settings); }
  }
  private static void rollback(Minecraft mc) throws Exception {
    clearAll(mc); grid(mc,0,tagged(35,3,5,"craft-rollback")); record(mc,"Crafting rollback fixture"); var death=latest(mc); clearAll(mc);
    var original=mc.player.playerContainer;
    class FailingGrid extends net.minecraft.class_277 {
      boolean armed; int writes;
      FailingGrid() { super(mc.player.inventory); }
      @Override public void method_2088(net.minecraft.inventory.Inventory inventory) {
        super.method_2088(inventory);
        if(armed && ++writes==2) throw new IllegalStateException("Injected second crafting callback failure");
      }
    }
    var failing=new FailingGrid(); mc.player.playerContainer=failing; mc.player.container=failing;
    grid(mc,3,tagged(264,7,0,"craft-existing")); var existing=grid(mc,3); String before=snapshot(mc); var output=failing.method_2084(0).getStack(); failing.armed=true;
    try {
      try { DeathTracker.recover(mc,death); throw new AssertionError("craft callback failure not reached"); } catch(java.io.IOException expected) {}
      check(before.equals(snapshot(mc)) && grid(mc,3)==existing && failing.method_2084(0).getStack()==output,"callback rollback changed inventory, inputs or recipe output");
      check(DeathTracker.journal(mc).state(death.id).equals("available") && !DeathTracker.recoveryDenial(mc).isEmpty(),"callback failure did not rollback claim and shut down recovery");
      log("PASS partial crafting callback failure restores all stacks and output, rolls back claim and disables recovery until restart");
    } finally { mc.player.playerContainer=original; mc.player.container=original; }
  }
  static void clearAll(Minecraft mc) { clear(mc); for(int i=0;i<4;i++) grid(mc,i,null); }
  private static ItemStack grid(Minecraft mc,int i) { return mc.player.playerContainer.method_2084(i+1).getStack(); }
  private static void grid(Minecraft mc,int i,ItemStack item) { mc.player.playerContainer.method_2084(i+1).setStack(item); }
  private static String snapshot(Minecraft mc) throws Exception { return DeathItems.inventory(DeathItems.snapshot(mc.player.inventory,mc.player.playerContainer)); }
  private static int drops(Minecraft mc) { int count=0; for(Object e:mc.world.field_198) if(e instanceof net.minecraft.class_142 item && !item.dead) count++; return count; }
}
