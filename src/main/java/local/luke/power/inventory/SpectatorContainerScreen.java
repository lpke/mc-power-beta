package local.luke.power.inventory;

import java.util.*;
import local.luke.power.input.*;
import net.minecraft.class_137;
import net.minecraft.class_138;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Native presentation over display copies, without opening a world-owned container. */
public final class SpectatorContainerScreen extends ContainerScreen {
  private final View view;
  private final Object world, player;
  private SpectatorContainerScreen(Minecraft mc, List<Inventory> sources) {
    super(new View(mc, sources));
    view = (View)container; world = mc.world; player = mc.player;
    backgroundHeight = view.rows > 0 ? 114 + view.rows * 18 : 166;
  }
  public static void open(Minecraft mc) {
    if (mc.world == null || mc.world.isRemote || mc.player == null || mc.currentScreen != null
        || !(mc.player instanceof PlayerMode mode) || !mode.power$spectator() || DetachedCamera.isActive()) return;
    var sources = ContainerPreview.sources(mc, 1);
    int size = sources.stream().mapToInt(Inventory::size).sum();
    if (size < 1 || size > 54 || sources.stream().anyMatch(source -> source.size() < 1)) return;
    mc.setScreen(new SpectatorContainerScreen(mc, sources));
  }
  // ContainerScreen.init would install the display handler on the real player.
  @Override public void init() {}
  @Override public void removed() {}
  @Override public void tick() {
    if (minecraft.world != world || minecraft.player != player
        || !(minecraft.player instanceof PlayerMode mode) || !mode.power$spectator()) minecraft.setScreen(null);
  }
  @Override protected void mouseClicked(int x, int y, int button) {
    if (Bindings.matches(minecraft.options.inventoryKey, button - 100)) minecraft.setScreen(null);
  }
  @Override protected void mouseReleased(int x, int y, int button) {}
  @Override protected void keyPressed(char character, int key) {
    if (key == Keyboard.KEY_ESCAPE || Bindings.matches(minecraft.options.inventoryKey, key)) minecraft.setScreen(null);
  }
  @Override protected void drawForeground() {
    textRenderer.draw(view.contents.getName(), view.rows > 0 ? 8 : 60, 6, 0x404040);
    textRenderer.draw("Inventory", 8, backgroundHeight - 94, 0x404040);
  }
  @Override protected void drawBackground(float delta) {
    int x = (width - backgroundWidth) / 2, y = (height - backgroundHeight) / 2;
    GL11.glColor4f(1,1,1,1);
    minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId(view.texture));
    if (view.rows > 0) {
      int upper = 17 + view.rows * 18;
      drawTexture(x,y,0,0,176,upper);
      drawTexture(x,y+upper,0,126,176,96);
    } else drawTexture(x,y,0,0,176,166);
    if (view.offhand) {
      minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId(InventorySideSlots.FRAME_TEXTURE));
      InventorySideSlots.frame((px,py,w,h,u,v,sw,sh) -> GuiSprite.draw(px,py,w,h,u,v,sw,sh,
          InventorySideSlots.FRAME_TEXTURE_WIDTH,InventorySideSlots.FRAME_TEXTURE_HEIGHT),
          x,y,backgroundHeight-(view.rows>0?1:0),view.hotbarY,false);
      minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId(InventorySideSlots.SLOT_TEXTURE));
      GuiSprite.draw(x+InventorySideSlots.SLOT_X,y+view.hotbarY-1,18,18,0,0,18,18,18,18);
    }
  }

  private static final class DisplayInventory implements Inventory {
    private final List<Inventory> sources;
    private final int size;
    DisplayInventory(List<Inventory> sources) { this.sources = List.copyOf(sources); size = sources.stream().mapToInt(Inventory::size).sum(); }
    public int size() { return size; }
    public ItemStack getStack(int index) {
      for (Inventory source : sources) {
        if (index < source.size()) { var stack = source.getStack(index); return stack == null ? null : stack.clone(); }
        index -= source.size();
      }
      return null;
    }
    public ItemStack removeStack(int index,int count) { return null; }
    public void setStack(int index,ItemStack stack) {}
    public String getName() { return sources.get(0).getName(); }
    public int getMaxCountPerStack() { return 64; }
    public void markDirty() {}
    public boolean canPlayerUse(PlayerEntity player) { return true; }
  }
  private static final class View extends Container {
    final DisplayInventory contents;
    final int rows;
    final String texture;
    final boolean offhand;
    final int hotbarY;
    View(Minecraft mc,List<Inventory> sources) {
      contents = new DisplayInventory(sources);
      boolean furnace = sources.size() == 1 && sources.get(0) instanceof class_138;
      boolean dispenser = sources.size() == 1 && sources.get(0) instanceof class_137;
      rows = furnace || dispenser ? 0 : (contents.size() + 8) / 9;
      texture = furnace ? "/gui/furnace.png" : dispenser ? "/gui/trap.png" : "/gui/container.png";
      for (int i=0;i<contents.size();i++) {
        int x = furnace ? i == 2 ? 116 : 56 : dispenser ? 62+i%3*18 : 8+i%9*18;
        int y = furnace ? i == 0 ? 17 : i == 1 ? 53 : 35 : dispenser ? 17+i/3*18 : 18+i/9*18;
        method_2079(new Slot(contents,i,x,y));
      }
      var inventory = new DisplayInventory(List.of(mc.player.inventory));
      int start = rows > 0 ? 31 + rows*18 : 84;
      hotbarY = start+58;
      for (int i=9;i<36;i++) method_2079(new Slot(inventory,i,8+i%9*18,start+(i/9-1)*18));
      for (int i=0;i<9;i++) method_2079(new Slot(inventory,i,8+i*18,start+58));
      offhand = local.luke.power.offhand.Offhand.visible(mc.player);
      if(offhand) method_2079(new Slot(new DisplayInventory(List.of(new local.luke.power.offhand.OffhandSlotInventory(mc.player.inventory))),
          0,InventorySideSlots.OFFHAND_X,hotbarY));
    }
    @Override public ItemStack onSlotClick(int slot,int button,boolean shift,PlayerEntity player) { return null; }
    @Override public ItemStack method_2086(int slot) { return null; }
    @Override public void method_2087(PlayerEntity player) {}
    @Override public boolean method_2094(PlayerEntity player) { return true; }
  }
}
