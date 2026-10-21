package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.AutoTools400State.*;
import local.luke.power.gameplay.*;
import local.luke.power.input.*;
import local.luke.power.inventory.*;
import local.luke.power.inventory.tools.*;
import local.luke.power.mining.*;
import local.luke.power.offhand.*;
import local.luke.power.world.WorldCompatibility;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Installed native automation hooks. Requires a muted disposable loaded world, never real user data. */
public final class AutoTools400Checks {
  private AutoTools400Checks() {}
  public static void run(Minecraft mc, String phase) throws Exception {
    failures = 0;
    check(phase.equals("native") || phase.equals("mining") || phase.equals("restock"), "expected native, mining or restock");
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.currentScreen == null && mc.player.container == mc.player.playerContainer, "close inventories before automation checks");
    check(mc.player.health > 0 && !mc.player.dead && mc.player.field_1595 == null, "use a living unmounted player");
    check(!Mouse.isButtonDown(0) && !DetachedCamera.isActive() && !InteractionState.carryingContainer,
        "release physical attack and leave freecam/container carrying before checks");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute this disposable clone before native use/mining");
    check(WorldCompatibility.ready(mc.world), "world load did not establish its durable format fence");
    check(!NativeInventoryPlan.busy() && !InventoryActions.busy() && !VeinMining.busy(), "another inventory/mining action is active");
    log("AUTOTOOLS400 phase=" + phase + " ready=" + WorldCompatibility.ready(mc.world)
        + " view=" + (mc.field_2807 == mc.player) + " focus=" + mc.field_2778 + " paused=" + mc.paused);
    try (var state = new AutoTools400State(mc)) {
      if (!phase.equals("restock")) mining(state);
      if (!phase.equals("mining")) restock(state);
    }
    log("AUTOTOOLS400 " + phase.toUpperCase(java.util.Locale.ROOT) + " failures=" + failures);
  }

  private static void mining(AutoTools400State s) throws Exception {
    var mc = s.mc; var inv = s.inventory;
    scenario(s, "native mining selects a harvest-capable hotbar tool over gold", () -> {
      // Power Beta's optional gold silk touch gives gold iron-tier harvesting. Obsidian
      // still requires diamond, so this remains a real harvestability test in every base.
      s.scene(49, 1); inv.main[0] = tagged(4, 7, 0, "held");
      inv.main[1] = tagged(285, 1, 3, "gold"); inv.main[3] = tagged(278, 1, 7, "diamond");
      check(!inv.main[1].method_714(Block.BLOCKS[49]) && inv.main[3].method_714(Block.BLOCKS[49]), "native harvest fixture is invalid");
      log("AUTOTOOLS400 native obsidian goldStrength=" + inv.main[1].method_708(Block.BLOCKS[49])
          + " diamondStrength=" + inv.main[3].method_708(Block.BLOCKS[49]));
      s.begin(); check(inv.selectedSlot == 3 && mc.player.method_514(Block.BLOCKS[49]), "non-harvesting tool won");
      check(s.breakBlock() && inv.main[3].getDamage() == 8 && inv.main[1].getDamage() == 3, "native successful harvest wore the wrong tool");
    });
    scenario(s, "native log mining selects an axe instead of a pickaxe", () -> {
      s.scene(17, 1); inv.main[0] = tagged(4, 7, 0, "held");
      inv.main[2] = tagged(278, 1, 2, "pick"); inv.main[7] = tagged(279, 1, 4, "axe");
      s.begin(); check(inv.selectedSlot == 7 && !NativeInventoryPlan.busy(), "native axe ranking failed");
    });
    scenario(s, "equal native tool speeds keep the selected slot and exact native data", () -> {
      s.scene(1, 1); inv.selectedSlot = 5;
      inv.main[1] = tagged(257, 1, 1, "other"); inv.main[5] = tagged(257, 1, 91, "selected");
      String before = s.snapshot(); s.begin();
      check(inv.selectedSlot == 5 && before.equals(s.snapshot()), "tie changed selection or item data");
    });
    scenario(s, "equal better tools use the first hotbar slot deterministically", () -> {
      s.scene(1, 1); inv.main[0] = tagged(4, 7, 0, "held");
      inv.main[2] = tagged(257, 1, 81, "first"); inv.main[7] = tagged(257, 1, 1, "second");
      s.begin(); check(inv.selectedSlot == 2, "equal candidate order changed");
    });
    scenario(s, "durability reserve skips a faster last-use candidate", () -> {
      s.scene(1, 1); inv.main[0] = tagged(4, 7, 0, "held");
      inv.main[1] = tagged(278, 1, 0, "last-use"); inv.main[1].setDamage(inv.main[1].getMaxDamage());
      inv.main[2] = tagged(257, 1, 4, "safe"); s.begin();
      check(inv.selectedSlot == 2 && inv.main[1].getDamage() == inv.main[1].getMaxDamage(), "reserve consumed the protected tool");
    });
    scenario(s, "zero reserve admits the last native tool use", () -> {
      s.scene(1, 1); var cfg = InventoryToolsConfig.copy(); cfg.toolDurabilityReserve = 0; InventoryToolsConfig.preview(cfg);
      inv.main[0] = tagged(4, 7, 0, "held"); inv.main[1] = tagged(278, 1, 0, "last-use");
      inv.main[1].setDamage(inv.main[1].getMaxDamage()); inv.main[2] = tagged(257, 1, 4, "safe");
      s.begin(); check(inv.selectedSlot == 1, "zero reserve still excluded last use");
    });
    scenario(s, "reserve never forces a slower replacement of the already selected tool", () -> {
      s.scene(1, 1); inv.main[0] = tagged(278, 1, 0, "selected");
      inv.main[0].setDamage(inv.main[0].getMaxDamage()); inv.main[1] = tagged(257, 1, 4, "slower");
      s.begin(); check(inv.selectedSlot == 0 && !NativeInventoryPlan.busy(), "reserve changed an existing held choice");
    });
    scenario(s, "hotbar-only selection never takes a storage tool", () -> {
      storage(s); var cfg = InventoryToolsConfig.copy(); cfg.toolsFromInventory = false; InventoryToolsConfig.preview(cfg);
      String before = s.snapshot(); s.begin();
      check(inv.selectedSlot == 0 && before.equals(s.snapshot()) && !NativeInventoryPlan.busy(), "hotbar-only setting moved storage");
    });
    scenario(s, "native storage selection waits for a durable full-data swap", () -> {
      storage(s); String held = bytes(inv.main[0]), tool = bytes(inv.main[9]), untouched = bytes(Offhand.get(inv));
      String before = s.snapshot();
      try (var slow = new SlowWriter()) {
        s.begin(); check(NativeInventoryPlan.ownsAction(mc, "tool_switch"), "native mining did not request its storage plan");
        NativeInventoryPlan.tick(mc); slow.checkBlocked();
        check(before.equals(s.snapshot()) && mc.world.getBlockId(s.x, s.y, s.z) == 1, "unflushed tool swap changed inventory or block");
        s.begin(); check(before.equals(s.snapshot()), "repeated held mining published an unflushed tool");
        slow.release(); s.finish();
      }
      check(inv.selectedSlot == 0 && tool.equals(bytes(inv.main[0])) && held.equals(bytes(inv.main[9]))
          && untouched.equals(bytes(Offhand.get(inv))), "storage swap changed count, NBT, offhand or selected slot");
      s.begin(); check(s.breakBlock() && inv.main[0].getDamage() == 38, "resumed native harvest did not use the swapped tool");
    });
    for (String reason : new String[]{"aim", "block", "release", "focus", "setting", "source"})
      scenario(s, "durable storage selection cancels on " + reason + " without reviving", () -> {
        storage(s);
        try (var slow = new SlowWriter()) {
          s.begin(); check(NativeInventoryPlan.busy(), "storage plan missing");
          switch (reason) {
            case "aim" -> s.aim(1);
            case "block" -> mc.world.method_200(s.x, s.y, s.z, 3);
            case "release" -> s.attack.set(false);
            case "focus" -> mc.field_2778 = false;
            case "setting" -> { var cfg = InventoryToolsConfig.copy(); cfg.toolSwitch = false; InventoryToolsConfig.preview(cfg); }
            case "source" -> inv.main[9] = tagged(278, 1, 41, "foreign-source");
          }
          String expected = s.snapshot(); NativeInventoryPlan.tick(mc); slow.checkBlocked();
          check(expected.equals(s.snapshot()), "canceled blocked plan wrote a slot");
          // Restoring the aim/press/focus before I/O completes must not revive the rejected plan.
          s.aim(0); s.attack.set(true); mc.field_2778 = true;
          slow.release(); s.finish(); check(expected.equals(s.snapshot()) && inv.selectedSlot == 0, "canceled plan revived after flush");
        }
      });
    scenario(s, "manual hand swapping supersedes a queued automatic storage plan", () -> {
      storage(s); String held = bytes(inv.main[0]), tool = bytes(inv.main[9]), off = bytes(Offhand.get(inv));
      try (var slow = new SlowWriter()) {
        s.begin(); check(NativeInventoryPlan.busy(), "storage plan missing");
        check(Offhand.swap(mc) && NativeInventoryPlan.ownsAction(mc, "swap_hands"), "manual hand swap did not supersede automation");
        slow.checkBlocked(); slow.release(); s.finish();
      }
      check(off.equals(bytes(inv.main[0])) && held.equals(bytes(Offhand.get(inv))) && tool.equals(bytes(inv.main[9])),
          "superseded automatic tool plan leaked into the manual swap");
    });
    for (String reason : new String[]{"Creative", "Spectator", "dead", "freecam", "menu", "focus"})
      scenario(s, "automatic tools and refill admission leave " + reason + " unchanged", () -> {
        storage(s);
        switch (reason) {
          case "Creative" -> CreativeVehicleChecks.mode(mc, "CREATIVE");
          case "Spectator" -> CreativeVehicleChecks.mode(mc, "SPECTATOR");
          case "dead" -> { mc.player.health = 0; mc.player.dead = true; }
          case "freecam" -> s.detached.set(true);
          case "menu" -> mc.currentScreen = new net.minecraft.class_525();
          case "focus" -> mc.field_2778 = false;
        }
        String before = s.snapshot();
        check(AutoToolSwitch.prepare(mc, s.x, s.y, s.z), "ineligible automation deferred a physical action");
        check(Restock.before(mc, inv.main[0]) == null, "ineligible native use requested a refill");
        check(before.equals(s.snapshot()) && inv.selectedSlot == 0 && !NativeInventoryPlan.busy(), "ineligible state changed inventory");
      });
    scenario(s, "automatic selection preserves Torch shovel ownership and charges only its worn diamond", () -> {
      s.scene(13, 1); var game = GameplayConfig.copy(); game.torchShovel = true; GameplayConfig.preview(game);
      inv.main[0] = tagged(50, 37, 0, "torch"); inv.main[1] = tagged(277, 1, 8, "fresh");
      inv.main[35] = tagged(277, 1, 120, "worn"); String torch = bytes(inv.main[0]);
      s.begin(); check(inv.selectedSlot == 0 && TorchShovel.slot(mc.player, Block.GRAVEL) == 35
          && !NativeInventoryPlan.busy(), "automatic tool selection displaced the Torch shovel");
      check(s.breakBlock() && inv.main[35].getDamage() == 121 && inv.main[1].getDamage() == 8
          && torch.equals(bytes(inv.main[0])), "Torch shovel native wear or held stack changed");
    });
    scenario(s, "a borrowed Torch shovel breaking never refills or swaps the held torch", () -> {
      s.scene(12, 1); var game = GameplayConfig.copy(); game.torchShovel = true; GameplayConfig.preview(game);
      inv.main[0] = tagged(50, 37, 0, "torch"); inv.main[9] = tagged(277, 1, 8, "shovel");
      inv.main[35] = tagged(277, 1, 0, "shovel"); inv.main[35].setDamage(inv.main[35].getMaxDamage());
      String torch = bytes(inv.main[0]), spare = bytes(inv.main[9]); s.begin();
      check(s.breakBlock() && inv.main[35] == null, "native borrowed shovel did not break");
      Restock.tick(mc); s.finish();
      check(torch.equals(bytes(inv.main[0])) && spare.equals(bytes(inv.main[9])) && inv.selectedSlot == 0
          && TorchShovel.slot(mc.player, Block.SAND) == 9, "borrowed-tool depletion refilled the wrong hand or consumed the spare");
    });
    scenario(s, "durable tool selection precedes vein capture and never steals an active vein tool", () -> {
      s.scene(16, 2); inv.main[0] = tagged(4, 7, 0, "held"); inv.main[9] = tagged(278, 1, 10, "vein-pick");
      var vein = new VeinSettings(); vein.enabled = true; vein.crouchingOnly = false;
      vein.maxBlocks = 2; vein.maxChecks = 64; vein.checksPerTick = 64; vein.blocksPerTick = 1; s.veins(vein);
      s.begin(); check(NativeInventoryPlan.busy(), "vein start skipped the durable selection"); s.finish(); s.begin();
      for (int i = 0; i < 64; i++) VeinMining.tick(mc);
      check(s.breakBlock() && VeinMining.busy(), "native first break did not retain a connected vein plan");
      inv.main[1] = tagged(285, 1, 0, "faster-after-capture");
      s.aim(1); mc.interactionManager.method_1707(s.x + 1, s.y, s.z, 1);
      check(inv.selectedSlot == 0 && !NativeInventoryPlan.busy(), "tool selection stole an active vein");
      for (int i = 0; i < 8 && VeinMining.busy(); i++) VeinMining.tick(mc);
      check(!VeinMining.busy() && mc.world.getBlockId(s.x + 1, s.y, s.z) == 0 && inv.main[0].getDamage() == 12
          && inv.main[1].getDamage() == 0, "vein continuation changed tools, wear or connected blocks");
    });
    scenario(s, "restocking a broken vein tool does not revive its canceled vein", () -> {
      s.scene(16, 2); inv.main[0] = tagged(278, 1, 0, "vein-break"); inv.main[0].setDamage(inv.main[0].getMaxDamage());
      inv.main[9] = tagged(278, 1, 10, "vein-break"); String spare = bytes(inv.main[9]);
      var vein = new VeinSettings(); vein.enabled = true; vein.crouchingOnly = false;
      vein.maxBlocks = 2; vein.maxChecks = 64; vein.checksPerTick = 64; s.veins(vein);
      s.begin(); for (int i = 0; i < 64; i++) VeinMining.tick(mc);
      check(s.breakBlock() && inv.main[0] == null && !VeinMining.busy(), "broken tool retained the vein job");
      Restock.tick(mc); s.finish(); VeinMining.tick(mc);
      check(spare.equals(bytes(inv.main[0])) && inv.main[9] == null && !VeinMining.busy()
          && mc.world.getBlockId(s.x + 1, s.y, s.z) == 16, "refill revived a vein against its original tool ownership");
    });
  }

  private static void restock(AutoTools400State s) throws Exception {
    var mc = s.mc; var inv = s.inventory;
    scenario(s, "native offhand food use refills only that hand after durable completion", () -> {
      inv.main[0] = tagged(35, 13, 5, "main-sentinel"); String main = bytes(inv.main[0]);
      Offhand.set(inv, tagged(260, 1, 0, "food")); inv.main[9] = tagged(260, 7, 0, "food");
      String refill = bytes(inv.main[9]); mc.player.health = 2;
      check(s.use(HandUse.Hand.OFF) && Offhand.get(inv) == null && mc.player.health > 2, "native offhand food was not consumed");
      try (var slow = new SlowWriter()) {
        Restock.tick(mc); check(NativeInventoryPlan.ownsAction(mc, "restock"), "offhand refill was not queued");
        NativeInventoryPlan.tick(mc); slow.checkBlocked();
        check(Offhand.get(inv) == null && refill.equals(bytes(inv.main[9])) && main.equals(bytes(inv.main[0])), "unflushed offhand refill touched a slot");
        slow.release(); s.finish();
      }
      check(refill.equals(bytes(Offhand.get(inv))) && inv.main[9] == null && main.equals(bytes(inv.main[0])) && inv.selectedSlot == 0,
          "offhand refill changed main-hand data or selected slot");
    });
    scenario(s, "main and offhand native consumptions retain independent pending refills", () -> {
      inv.main[0] = tagged(260, 1, 0, "both-food"); Offhand.set(inv, tagged(260, 1, 0, "both-food"));
      inv.main[9] = tagged(260, 7, 0, "both-food"); inv.main[10] = tagged(260, 8, 0, "both-food");
      String first = bytes(inv.main[9]), second = bytes(inv.main[10]); mc.player.health = 2;
      check(s.use(HandUse.Hand.MAIN) && s.use(HandUse.Hand.OFF), "one native consumption failed");
      check(inv.main[0] == null && Offhand.get(inv) == null, "one native hand was not depleted");
      Restock.tick(mc); s.finish(); Restock.tick(mc); s.finish();
      check(first.equals(bytes(inv.main[0])) && second.equals(bytes(Offhand.get(inv)))
          && inv.main[9] == null && inv.main[10] == null, "hand requests overwrote or shared a refill");
    });
    scenario(s, "restock skips an equal item ID with different native NBT", () -> {
      inv.main[0] = tagged(260, 1, 0, "wanted"); inv.main[9] = tagged(260, 7, 0, "foreign");
      inv.main[10] = tagged(260, 8, 0, "wanted"); String foreign = bytes(inv.main[9]), wanted = bytes(inv.main[10]);
      mc.player.health = 2; check(s.use(HandUse.Hand.MAIN), "native food failed"); Restock.tick(mc); s.finish();
      check(wanted.equals(bytes(inv.main[0])) && foreign.equals(bytes(inv.main[9])) && inv.main[10] == null, "full-data refill identity failed");
    });
    for (boolean otherHotbar : new boolean[]{false, true})
      scenario(s, "offhand native depletion observes other-hotbar source setting " + otherHotbar, () -> {
        var cfg = InventoryToolsConfig.copy(); cfg.restockOtherHotbar = otherHotbar; InventoryToolsConfig.preview(cfg);
        inv.main[0] = tagged(35, 13, 5, "main"); Offhand.set(inv, tagged(260, 1, 0, "food"));
        inv.main[1] = tagged(260, 7, 0, "food"); String source = bytes(inv.main[1]); mc.player.health = 2;
        check(s.use(HandUse.Hand.OFF), "native offhand food failed"); Restock.tick(mc); s.finish();
        check(otherHotbar ? source.equals(bytes(Offhand.get(inv))) && inv.main[1] == null
            : Offhand.get(inv) == null && source.equals(bytes(inv.main[1])), "other-hotbar source policy failed");
      });
    scenario(s, "disabling offhand after native depletion blocks its pending refill", () -> {
      Offhand.set(inv, tagged(260, 1, 0, "food")); inv.main[9] = tagged(260, 7, 0, "food"); mc.player.health = 2;
      check(s.use(HandUse.Hand.OFF), "native offhand food failed");
      var disabled = OffhandConfig.copy(); disabled.enabled = false; OffhandConfig.preview(disabled);
      Restock.tick(mc); s.finish(); check(Offhand.get(inv) == null && inv.main[9].count == 7, "disabled hand was refilled");
    });
    scenario(s, "disabled offhand does not block independent main-hand refills", () -> {
      Offhand.set(inv, tagged(35, 13, 5, "retained-offhand")); String off = bytes(Offhand.get(inv));
      var disabled = OffhandConfig.copy(); disabled.enabled = false; OffhandConfig.preview(disabled);
      inv.main[0] = tagged(260, 1, 0, "food"); inv.main[9] = tagged(260, 7, 0, "food"); mc.player.health = 2;
      check(s.use(HandUse.Hand.MAIN), "native main food failed"); Restock.tick(mc); s.finish();
      check(inv.main[0].count == 7 && inv.main[9] == null && off.equals(bytes(Offhand.get(inv))), "offhand setting interfered with the main hand");
    });
    for (boolean beforeEmpty : new boolean[]{false, true})
      scenario(s, "native partial placement observes before-empty refill setting " + beforeEmpty, () -> {
        s.scene(1, 1); var cfg = InventoryToolsConfig.copy(); cfg.restockBeforeEmpty = beforeEmpty; InventoryToolsConfig.preview(cfg);
        inv.main[0] = tagged(4, 2, 0, "place"); inv.main[9] = tagged(4, 63, 0, "place");
        s.place(HandUse.Hand.MAIN); check(inv.main[0].count == 1, "native placement consumed the wrong count");
        Restock.tick(mc); s.finish();
        check(beforeEmpty ? inv.main[0].count == 64 && inv.main[9] == null : inv.main[0].count == 1 && inv.main[9].count == 63,
            "partial refill ignored threshold/setting or lost counts");
      });
    for (boolean refillTools : new boolean[]{false, true})
      scenario(s, "native broken-tool restock observes its setting and chooses the worn replacement " + refillTools, () -> {
        s.scene(1, 1); var cfg = InventoryToolsConfig.copy(); cfg.toolSwitch = false; cfg.restockTools = refillTools; InventoryToolsConfig.preview(cfg);
        inv.main[0] = tagged(257, 1, 0, "pick"); inv.main[0].setDamage(inv.main[0].getMaxDamage());
        inv.main[9] = tagged(257, 1, 2, "pick"); inv.main[10] = tagged(257, 1, 81, "pick");
        String fresh = bytes(inv.main[9]), worn = bytes(inv.main[10]);
        check(s.breakBlock() && inv.main[0] == null, "native last tool use did not break its tool");
        Restock.tick(mc); s.finish();
        check(fresh.equals(bytes(inv.main[9])) && (refillTools ? worn.equals(bytes(inv.main[0])) && inv.main[10] == null
            : inv.main[0] == null && worn.equals(bytes(inv.main[10]))), "tool replacement setting or wear ranking failed");
      });
    for (HandUse.Hand hand : HandUse.Hand.values())
      scenario(s, "native milk bucket replacement remains an empty bucket in " + hand, () -> {
        s.scene(1, 1); s.lookAtTarget();
        inv.main[0] = tagged(35, 13, 5, "main"); Offhand.set(inv, tagged(35, 11, 6, "off"));
        Offhand.stack(mc.player, hand, tagged(335, 1, 0, "milk")); inv.main[9] = tagged(335, 1, 0, "milk");
        String source = bytes(inv.main[9]), other = bytes(Offhand.stack(mc.player, hand == HandUse.Hand.MAIN ? HandUse.Hand.OFF : HandUse.Hand.MAIN));
        check(s.use(hand), "native milk use failed"); Restock.tick(mc); s.finish();
        ItemStack result = Offhand.stack(mc.player, hand);
        check(result != null && result.itemId == 325 && result.count == 1 && source.equals(bytes(inv.main[9]))
            && other.equals(bytes(Offhand.stack(mc.player, hand == HandUse.Hand.MAIN ? HandUse.Hand.OFF : HandUse.Hand.MAIN))),
            "bucket replacement was mistaken for depletion or changed the other hand");
      });
    for (HandUse.Hand hand : HandUse.Hand.values())
      scenario(s, "native last bonemeal on fully grown wheat consumes only " + hand + " without a world effect", () -> {
        var cfg = InventoryToolsConfig.copy(); cfg.restock = false; InventoryToolsConfig.preview(cfg);
        s.scene(0, 0); mc.world.method_200(s.x, s.y - 1, s.z, 60); mc.world.method_154(s.x, s.y, s.z, 59, 7); s.aim(0);
        check(mc.world.getBlockId(s.x, s.y, s.z) == 59 && mc.world.method_1778(s.x, s.y, s.z) == 7, "fully grown wheat fixture is invalid");
        inv.main[0] = tagged(280, 13, 0, "main-stick"); Offhand.set(inv, tagged(351, 5, 15, "other-bonemeal"));
        ItemStack last = tagged(351, 1, 15, "last-bonemeal"); Offhand.stack(mc.player, hand, last);
        HandUse.Hand other = hand == HandUse.Hand.MAIN ? HandUse.Hand.OFF : HandUse.Hand.MAIN;
        String retained = bytes(Offhand.stack(mc.player, other)); OffhandUse.newPress(mc);
        // Actual Minecraft dispatch observes both hands and owns native zero-count cleanup.
        ((local.luke.power.validation.mixin.Client253Access)(Object)mc).power$use(1);
        check(last.count == 0 && Offhand.stack(mc.player, hand) == null, "native last dye was not consumed and normalized exactly once");
        check(retained.equals(bytes(Offhand.stack(mc.player, other))), "terminal main/offhand use consumed the other hand");
        check(mc.world.getBlockId(s.x, s.y, s.z) == 59 && mc.world.method_1778(s.x, s.y, s.z) == 7,
            "bonemeal fixture unexpectedly changed a fully grown crop");
        Restock.tick(mc); s.finish(); check(!OffhandUse.active() && !NativeInventoryPlan.busy(), "native hand scope or refill leaked after last dye");
      });
    scenario(s, "manual native dropping never requests automatic refill", () -> {
      inv.main[0] = tagged(260, 1, 0, "drop"); inv.main[9] = tagged(260, 7, 0, "drop");
      String source = bytes(inv.main[9]); mc.player.dropSelectedItem();
      check(inv.main[0] == null, "native manual drop did not empty the selected hand"); Restock.tick(mc); s.finish();
      check(inv.main[0] == null && source.equals(bytes(inv.main[9])) && !NativeInventoryPlan.busy(), "manual drop was refilled");
    });
    scenario(s, "queued offhand refill and automatic tool selection cannot publish competing plans", () -> {
      storage(s); Offhand.set(inv, tagged(260, 1, 0, "food")); inv.main[10] = inv.main[9];
      inv.main[9] = tagged(260, 7, 0, "food"); mc.player.health = 2;
      String held = bytes(inv.main[0]), tool = bytes(inv.main[10]), refill = bytes(inv.main[9]);
      check(s.use(HandUse.Hand.OFF), "native food failed");
      try (var slow = new SlowWriter()) {
        Restock.tick(mc); check(NativeInventoryPlan.ownsAction(mc, "restock"), "offhand refill missing");
        s.begin(); slow.checkBlocked();
        check(NativeInventoryPlan.ownsAction(mc, "restock") && held.equals(bytes(inv.main[0])) && tool.equals(bytes(inv.main[10])),
            "automatic tools replaced the refill lease");
        slow.release(); s.finish();
      }
      check(refill.equals(bytes(Offhand.get(inv))) && inv.main[9] == null && tool.equals(bytes(inv.main[10])), "refill lost its ownership");
      s.begin(); check(NativeInventoryPlan.ownsAction(mc, "tool_switch"), "tools did not resume after refill"); s.finish();
      check(tool.equals(bytes(inv.main[0])) && held.equals(bytes(inv.main[10])) && refill.equals(bytes(Offhand.get(inv))), "serial plans changed unrelated native data");
    });
  }

  private static void storage(AutoTools400State s) throws Exception {
    s.scene(1, 1); s.inventory.main[0] = tagged(4, 7, 0, "held");
    s.inventory.main[9] = tagged(278, 1, 37, "storage-tool");
    s.inventory.armor[0] = tagged(313, 1, 15, "armor"); Offhand.set(s.inventory, tagged(35, 13, 5, "offhand"));
  }
  private static void scenario(AutoTools400State s, String name, Check body) throws Exception {
    test("AUTOTOOLS400 " + name, () -> { s.reset(); try { body.run(); } finally { s.cancel(); } });
  }
}
