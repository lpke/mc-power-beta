package local.luke.power.creative.inventory.mixin.client;

import java.util.List;
import local.luke.power.creative.inventory.CreativeGrid;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.client.gui.screen.container.PlayerScreen;
import net.minecraft.client.render.RenderHelper;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.container.Container;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.inventory.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.maths.MCMath;
import net.modificationstation.stationapi.api.client.item.CustomTooltipProvider;
import net.modificationstation.stationapi.api.network.packet.PacketHelper;
import net.modificationstation.stationapi.api.util.math.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.creative.inventory.api.CreativeTab;
import local.luke.power.creative.inventory.registry.TabRegistry;
import local.luke.power.creative.inventory.util.MHelper;
import local.luke.power.creative.inventory.util.SlotUpdatePacket;

@Mixin(PlayerScreen.class)
public abstract class PlayerScreenMixin extends ContainerScreen {
  @Unique private static final int CREATIVE_COLOR_FILLER = MHelper.getColor(198, 198, 198, 128);
  @Unique private static final ItemRenderer CREATIVE_ITEM_RENDERER = new ItemRenderer();

  @Unique
  private static final String CREATIVE_KEY_INVENTORY = "title.power_creative_inventory.selectGame.inventory";

  @Unique
  private static final String CREATIVE_KEY_CREATIVE = "title.power_creative_inventory.selectGame.creative";

  @Unique private static boolean power_lastSurvivalView;
  @Unique private static int power_lastTab;

  @Unique private List<ItemStack> creative_items;
  @Unique private boolean creative_normalGUI;
  @Unique private int creative_mouseDelta;
  @Unique private String creative_tabKey;
  @Unique private int creative_rowIndex;
  @Unique private int creative_maxIndex;
  @Unique private float creative_slider;
  @Unique private boolean creative_drag;

  @Unique private int creative_maxTabIndex;
  @Unique private int creative_pagesCount;
  @Unique private int creative_tabIndex;
  @Unique private int creative_tabPage;

  @Unique private ItemStack creative_creativeIcon;
  @Unique private ItemStack creative_survivalIcon;

  @Shadow private float mouseX;
  @Shadow private float mouseY;

  public PlayerScreenMixin(Container container) {
    super(container);
  }

  @Inject(method = "<init>(Lnet/minecraft/entity/living/player/PlayerEntity;)V", at = @At("TAIL"))
  private void creative_initPlayerInventory(PlayerEntity player, CallbackInfo info) {
    creative_creativeIcon = new ItemStack(Item.diamond);
    creative_survivalIcon = new ItemStack(Block.WORKBENCH);
    int remembered = Math.max(0, Math.min(power_lastTab, TabRegistry.getTabsCount() - 1));
    CreativeTab tab = TabRegistry.getTabByIndex(remembered);
    creative_normalGUI = power_lastSurvivalView;
    creative_tabKey = tab.getTranslationKey();
    creative_items = tab.getItems();
    creative_maxIndex = creative_getMaxItemIndex();
    creative_rowIndex = 0;
    creative_tabIndex = remembered % 7;
    creative_tabPage = remembered / 7;
    creative_updateMaxIndex();
    creative_pagesCount = (int) Math.ceil(TabRegistry.getTabsCount() / 7.0F);
  }

  @Inject(
      method = "renderContainerBackground(F)V",
      at =
          @At(
              value = "INVOKE",
              target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V",
              remap = false,
              shift = Shift.AFTER))
  private void creative_renderBackgroundEnd(float delta, CallbackInfo info) {
    if (!(creative_isInCreative() && creative_normalGUI)) {
      return;
    }

    int texture =
        this.minecraft.textureManager.getTextureId(
            "/assets/power_creative_inventory/textures/gui/creative_list.png");
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    this.minecraft.textureManager.bindTexture(texture);

    int posX = (this.width - this.containerWidth) / 2;
    int posY = (this.height - this.containerHeight) / 2;
    this.blit(posX + 173, posY + 138, 176, 32, 25, 24);

    GL11.glPushMatrix();
    GL11.glRotatef(120.0F, 1.0F, 0.0F, 0.0F);
    RenderHelper.enableLighting();
    GL11.glPopMatrix();
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    GL11.glEnable(GL12.GL_RESCALE_NORMAL);

    creative_renderItem(creative_creativeIcon, posX + 173 + 4, posY + 114 + 4);
    creative_renderItem(creative_survivalIcon, posX + 173 + 4, posY + 138 + 4);

    RenderHelper.disableLighting();
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

    creative_destroySlot(posX, posY);

    int tabX = (int) mouseX - posX - 173;
    int tabY = (int) mouseY - posY - 114;
    if (tabX >= 0 && tabX < 25 && tabY >= 0 && tabY < 24) {
      creative_renderString(creative_translate(CREATIVE_KEY_CREATIVE));
    }

    tabY = (int) mouseY - posY - 138;
    if (tabX >= 0 && tabX < 25 && tabY >= 0 && tabY < 24) {
      creative_renderString(creative_translate(CREATIVE_KEY_INVENTORY));
    }
  }

  @Inject(method = "renderContainerBackground", at = @At("HEAD"), cancellable = true)
  private void creative_renderBackgroundStart(float delta, CallbackInfo info) {
    if (!creative_isInCreative()) {
      return;
    }

    int posX = (this.width - this.containerWidth) / 2;
    int posY = (this.height - this.containerHeight) / 2;

    int texture =
        this.minecraft.textureManager.getTextureId(
            "/assets/power_creative_inventory/textures/gui/creative_list.png");
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    this.minecraft.textureManager.bindTexture(texture);

    if (creative_normalGUI) {
      this.blit(posX + 173, posY + 114, 176, 32, 25, 24); // Survival
    } else {
      PlayerInventory inventory = this.minecraft.player.inventory;

      for (int i = 0; i < creative_maxTabIndex; i++) {
        if (i != creative_tabIndex) {
          this.blit(posX + 4 + i * 24, CreativeGrid.tabTop(posY), 176, 0, 24, CreativeGrid.tabHeight(posY));
        }
      }

      this.blit(posX + 173, posY + 138, 176, 32, 25, 24);
      // Grow the catalogue upward; hotbar, side tabs and destroy slot keep their coordinates.
      this.blit(posX, posY - CreativeGrid.EXTRA_HEIGHT, 0, 0, this.containerWidth, 139);
      this.blit(posX, posY + 139 - CreativeGrid.EXTRA_HEIGHT, 0, 121, this.containerWidth, 18);
      for (int y = 157 - CreativeGrid.EXTRA_HEIGHT; y < 140; y++)
        this.blit(posX, posY + y, 0, 140, this.containerWidth, 1);
      this.blit(posX, posY + 140, 0, 140, this.containerWidth, this.containerHeight - 140);
      // Extend the scrollbar well without repeating its bottom border between rows.
      this.blit(posX + 154, posY + 138 - CreativeGrid.EXTRA_HEIGHT, 154, 100, 16, 18);
      this.blit(posX + 173, posY + 114, 176, 32, 25, 24);

      this.blit(posX + 150, posY - CreativeGrid.EXTRA_HEIGHT + 4, 208, 0, 9, 8);
      if (creative_tabPage == 0) {
        this.fill(posX + 150, posY - CreativeGrid.EXTRA_HEIGHT + 4, posX + 150 + 9, posY - CreativeGrid.EXTRA_HEIGHT + 4 + 8, CREATIVE_COLOR_FILLER);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      }
      this.blit(posX + 160, posY - CreativeGrid.EXTRA_HEIGHT + 4, 208, 8, 9, 8);
      if (creative_tabPage >= creative_pagesCount - 1) {
        this.fill(posX + 160, posY - CreativeGrid.EXTRA_HEIGHT + 4, posX + 160 + 9, posY - CreativeGrid.EXTRA_HEIGHT + 4 + 8, CREATIVE_COLOR_FILLER);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      }

      int sliderX = posX + 154;
      int sliderY = posY - CreativeGrid.EXTRA_HEIGHT + 14 + MCMath.floor(creative_slider * CreativeGrid.SCROLL_TRAVEL);
      if (creative_maxIndex > 0) this.blit(sliderX, sliderY, 240, 1, 14, 15);

      this.blit(posX + 4 + creative_tabIndex * 24, CreativeGrid.tabTop(posY), 176, 0, 24, CreativeGrid.tabHeight(posY));

      GL11.glPushMatrix();
      GL11.glRotatef(120.0F, 1.0F, 0.0F, 0.0F);
      RenderHelper.enableLighting();
      GL11.glPopMatrix();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(GL12.GL_RESCALE_NORMAL);

      creative_renderItem(creative_creativeIcon, posX + 173 + 4, posY + 114 + 4);
      creative_renderItem(creative_survivalIcon, posX + 173 + 4, posY + 138 + 4);

      for (int i = 0; i < creative_maxTabIndex; i++) {
        CreativeTab tab = creative_getTab(creative_tabPage, i);
        if (tab == null) continue;
        ItemStack icon = tab.getIcon();
        if (icon == null) continue;
        creative_renderItem(icon, posX + 8 + i * 24, CreativeGrid.tabTop(posY) + 4);
      }

      for (int i = 0; i < CreativeGrid.CAPACITY; i++) {
        int index = creative_rowIndex + i;
        if (index >= 0 && index < creative_items.size()) {
          ItemStack instance = creative_items.get(index);
          int x = posX + (i & 7) * 18 + 8;
          int y = posY - CreativeGrid.EXTRA_HEIGHT + (i / 8) * 18 + 14;
          creative_renderItem(instance, x, y);
        }
      }

      for (int i = 0; i < 9; i++) {
        ItemStack item = inventory.main[i];
        int x = posX + i * 18 + 8;
        int y = posY + 142;
        creative_renderItem(item, x, y);
      }

      RenderHelper.disableLighting();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      String translated = creative_translate(creative_tabKey);
      this.textManager.drawText(translated, posX + 8, posY - CreativeGrid.EXTRA_HEIGHT + 5, 0x373737);

      int slotX = MCMath.floor((mouseX - posX - 8) / 18);
      if (slotX >= 0) {
        int slotY = MCMath.floor((mouseY - posY + CreativeGrid.EXTRA_HEIGHT - 14) / 18);
        if (slotX < 8 && slotY >= 0 && slotY < CreativeGrid.ROWS) {
          int x = slotX * 18 + posX + 8;
          int y = slotY * 18 + posY - CreativeGrid.EXTRA_HEIGHT + 14;
          creative_renderSlotOverlay(x, y);

          int index = slotY * 8 + slotX + creative_rowIndex;
          ItemStack item = index < creative_items.size() ? creative_items.get(index) : null;
          RenderHelper.disableLighting();
          GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
          creative_renderName(item);
        }
        slotY = MCMath.floor((mouseY - posY - 142) / 18);
        if (slotX < 9 && slotY == 0) {
          int x = slotX * 18 + posX + 8;
          int y = posY + 142;
          creative_renderSlotOverlay(x, y);
          RenderHelper.disableLighting();
          GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
          creative_renderName(inventory.main[slotX]);
        }
      }

      int tabX = MCMath.floor((mouseX - posX - 4) / 24F);
      int tabY = (int) mouseY - CreativeGrid.tabTop(posY);
      if (tabX >= 0 && tabX < creative_maxTabIndex && tabY >= 0 && tabY < CreativeGrid.tabHeight(posY)) {
        CreativeTab tab = creative_getTab(creative_tabPage, tabX);
        if (tab != null) {
          translated = creative_translate(tab.getTranslationKey());
          creative_renderString(translated);
        }
      }

      tabX = (int) mouseX - posX - 173;
      tabY = (int) mouseY - posY - 114;
      if (tabX >= 0 && tabX < 25 && tabY >= 0 && tabY < 24) {
        creative_renderString(creative_translate(CREATIVE_KEY_CREATIVE));
      }

      tabY = (int) mouseY - posY - 138;
      if (tabX >= 0 && tabX < 25 && tabY >= 0 && tabY < 24) {
        creative_renderString(creative_translate(CREATIVE_KEY_INVENTORY));
      }

      creative_destroySlot(posX, posY);
      info.cancel();
    }
  }

  @Unique
  private void creative_destroySlot(int x, int y) {
    if (!local.luke.power.creative.config.Config.current().destroySlot) return;
    int left = x - CreativeGrid.DESTROY_WIDTH, top = y + CreativeGrid.DESTROY_TOP, bottom = y + containerHeight;
    // The extension shares the inventory's bottom border, including its two shadow pixels.
    fill(left + 1, top, x + 1, bottom, 0xFF000000);
    fill(left, top + 1, x + 4, bottom - 1, 0xFF000000);
    fill(left + 1, top + 1, x + 4, bottom - 1, 0xFFC6C6C6);
    fill(left + 2, top + 1, x + 3, top + 3, 0xFFFFFFFF);
    fill(left + 1, top + 2, left + 3, bottom - 3, 0xFFFFFFFF);
    fill(left + 2, bottom - 3, x + 4, bottom - 1, 0xFF555555);
    local.luke.power.creative.ui.Texture.draw(
        minecraft, "inventory", x - 18, y + 141, 18, 18, 172, 111, 18, 18, 256, 256);
    if (local.luke.power.creative.config.Config.current().shiftClearsInventory
        && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
        && mouseX >= x - 18
        && mouseX < x
        && mouseY >= y + 141
        && mouseY < y + 159) {
      creative_renderString("Clear inventory");
    }
  }

  @Unique
  private void creative_renderName(ItemStack item) {
    if (item == null) return;
    GL11.glDisable(GL11.GL_DEPTH_TEST);
    String name = creative_translate_2(item.getTranslationKey());
    if (item.getType() instanceof CustomTooltipProvider provider) {
      String[] tooltip = provider.getTooltip(item, name);
      creative_renderStrings(tooltip);
    } else {
      creative_renderString(name);
    }
    GL11.glEnable(GL11.GL_DEPTH_TEST);
  }

  @Unique
  private void creative_renderString(String string) {
    if (string == null || string.isEmpty()) return;

    GL11.glDisable(GL11.GL_DEPTH_TEST);

    int x = (int) mouseX + 12;
    int y = (int) mouseY - 12;
    int width = this.textManager.getTextWidth(string);
    this.fillGradient(x - 3, y - 3, x + width + 3, y + 8 + 3, -1073741824, -1073741824);
    this.textManager.drawTextWithShadow(string, x, y, -1);

    GL11.glEnable(GL11.GL_DEPTH_TEST);
  }

  @Unique
  private void creative_renderStrings(String[] strings) {
    GL11.glDisable(GL11.GL_DEPTH_TEST);

    int x = (int) mouseX + 12;
    int y = (int) mouseY - 12;
    int width = 0;

    for (String line : strings) {
      width = Math.max(width, this.textManager.getTextWidth(line));
    }

    this.fillGradient(
        x - 3,
        y - 3,
        x + width + 3,
        y + 8 + 3 + (strings.length - 1) * 12,
        -1073741824,
        -1073741824);

    for (int i = 0; i < strings.length; i++) {
      this.textManager.drawTextWithShadow(strings[i], x, y + i * 12, -1);
      width = Math.max(width, this.textManager.getTextWidth(strings[i]));
    }

    GL11.glEnable(GL11.GL_DEPTH_TEST);
  }

  @Inject(method = "renderForeground", at = @At("HEAD"), cancellable = true)
  private void creative_renderForeground(CallbackInfo info) {
    if (creative_isInCreative() && !creative_normalGUI) {
      info.cancel();
    }
  }

  @Unique
  private boolean creative_isInCreative() {
    return minecraft.player.creative_isCreative();
  }

  @Unique
  private void creative_renderItem(ItemStack instance, int x, int y) {
    if (instance == null) {
      return;
    }
    CREATIVE_ITEM_RENDERER.renderStackInGUI(
        this.textManager, this.minecraft.textureManager, instance, x, y);
    CREATIVE_ITEM_RENDERER.renderStackInGUIWithDamage(
        this.textManager, this.minecraft.textureManager, instance, x, y);
  }

  @Unique
  private void creative_renderSlotOverlay(int x, int y) {
    GL11.glDisable(GL11.GL_LIGHTING);
    GL11.glDisable(GL11.GL_DEPTH_TEST);
    this.fillGradient(x, y, x + 16, y + 16, -2130706433, -2130706433);
    GL11.glEnable(GL11.GL_LIGHTING);
    GL11.glEnable(GL11.GL_DEPTH_TEST);
  }

  @Inject(method = "render", at = @At("HEAD"), cancellable = true)
  public void creative_render(int mouseX, int mouseY, float delta, CallbackInfo info) {
    if (creative_isInCreative() && !creative_normalGUI) {
      creative_mouseScroll();

      this.renderBackground();
      int posX = (this.width - this.containerWidth) / 2;
      int posY = (this.height - this.containerHeight) / 2;
      this.renderContainerBackground(delta);

      GL11.glPushMatrix();
      GL11.glRotatef(120.0F, 1.0F, 0.0F, 0.0F);
      RenderHelper.enableLighting();
      GL11.glPopMatrix();

      GL11.glPushMatrix();
      GL11.glTranslatef((float) posX, (float) posY, 0.0F);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(GL12.GL_RESCALE_NORMAL);

      PlayerInventory inventory = this.minecraft.player.inventory;
      if (inventory.getCursorItem() != null) {
        GL11.glTranslatef(0.0F, 0.0F, 32.0F);
        CREATIVE_ITEM_RENDERER.renderStackInGUI(
            this.textManager,
            this.minecraft.textureManager,
            inventory.getCursorItem(),
            mouseX - posX - 8,
            mouseY - posY - 8);
        CREATIVE_ITEM_RENDERER.renderStackInGUIWithDamage(
            this.textManager,
            this.minecraft.textureManager,
            inventory.getCursorItem(),
            mouseX - posX - 8,
            mouseY - posY - 8);
      }

      GL11.glDisable(GL12.GL_RESCALE_NORMAL);
      RenderHelper.disableLighting();
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      this.renderForeground();

      GL11.glPopMatrix();
      GL11.glEnable(GL11.GL_LIGHTING);
      GL11.glEnable(GL11.GL_DEPTH_TEST);

      this.mouseX = (float) mouseX;
      this.mouseY = (float) mouseY;
      info.cancel();
    }
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int button) {
    if (local.luke.power.input.Bindings.matches(minecraft.options.inventoryKey, button - 100)) {
      minecraft.player.closeContainer();
      return;
    }
    if (creative_isInCreative()) {
      int posX = (this.width - this.containerWidth) / 2;
      int posY = (this.height - this.containerHeight) / 2;

      if (local.luke.power.creative.config.Config.current().destroySlot
          && mouseX >= posX - 18
          && mouseX < posX
          && mouseY >= posY + 141
          && mouseY < posY + 159) {
        local.luke.power.creative.InventoryActions.destroy(
            minecraft.player,
            Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
        creative_playSound();
        return;
      }
      int tabX = mouseX - posX - 173;
      int tabY = mouseY - posY - 114;
      if (tabX >= 0 && tabX < 25 && tabY >= 0 && tabY < 24) {
        creative_normalGUI = false;
        creative_drag = false;
        creative_playSound();
        return;
      }

      tabY = mouseY - posY - 138;
      if (tabX >= 0 && tabX < 25 && tabY >= 0 && tabY < 24) {
        creative_normalGUI = true;
        creative_drag = false;
        creative_playSound();
        return;
      }

      if (creative_normalGUI) {
        super.mouseClicked(mouseX, mouseY, button);
        return;
      }

      tabX = MCMath.floor((mouseX - posX - 4) / 24F);
      tabY = mouseY - CreativeGrid.tabTop(posY);
      if (tabX >= 0 && tabX < creative_maxTabIndex && tabY >= 0 && tabY < CreativeGrid.tabHeight(posY)) {
        CreativeTab tab = creative_getTab(creative_tabPage, tabX);
        if (tab == null) return;

        creative_tabIndex = tabX;
        creative_tabKey = tab.getTranslationKey();
        creative_items = tab.getItems();
        creative_maxIndex = creative_getMaxItemIndex();
        creative_rowIndex = 0;
        creative_slider = 0F;

        creative_playSound();

        return;
      }

      int buttonY = mouseY - posY + CreativeGrid.EXTRA_HEIGHT - 4;
      if (buttonY > 0 && buttonY < 8) {
        int buttonX = mouseX - posX - 150;
        if (creative_tabPage > 0 && buttonX >= 0 && buttonX < 9) {
          creative_tabIndex = 0;
          creative_tabPage--;
          creative_updateMaxIndex();

          creative_playSound();
          CreativeTab tab = creative_getTab(creative_tabPage, creative_tabIndex);
          creative_rowIndex = 0;
          creative_slider = 0F;
          if (tab == null) {
            return;
          }
          creative_tabKey = tab.getTranslationKey();
          creative_items = tab.getItems();
          creative_maxIndex = creative_getMaxItemIndex();

          return;
        }

        buttonX = mouseX - posX - 160;
        if ((creative_tabPage < (creative_pagesCount - 1)) && buttonX >= 0 && buttonX < 9) {
          creative_tabIndex = 0;
          creative_tabPage++;
          creative_updateMaxIndex();

          creative_playSound();
          CreativeTab tab = creative_getTab(creative_tabPage, creative_tabIndex);
          creative_rowIndex = 0;
          creative_slider = 0F;
          if (tab == null) {
            return;
          }
          creative_tabKey = tab.getTranslationKey();
          creative_items = tab.getItems();
          creative_maxIndex = creative_getMaxItemIndex();

          return;
        }
      }

      int sliderX = mouseX - posX - 154;
      int sliderY = mouseY - posY + CreativeGrid.EXTRA_HEIGHT - 14 - MCMath.floor(creative_slider * CreativeGrid.SCROLL_TRAVEL);
      if (creative_maxIndex > 0 && sliderX > 0 && sliderX < 14 && sliderY > 0 && sliderY < 15) {
        creative_mouseDelta = posY - CreativeGrid.EXTRA_HEIGHT + 14 + sliderY;
        creative_drag = true;
        return;
      }

      int slotX = MCMath.floor((mouseX - posX - 8) / 18F);
      int slotY = MCMath.floor((mouseY - posY + CreativeGrid.EXTRA_HEIGHT - 14) / 18F);

      PlayerInventory inventory = this.minecraft.player.inventory;
      if (slotY >= 0 && slotY < CreativeGrid.ROWS && slotX >= 0 && slotX < 8) {
        int index = slotY * 8 + slotX + creative_rowIndex;
        ItemStack cursor = inventory.getCursorItem();
        if (index < creative_items.size()) {
          ItemStack item = creative_items.get(index);
          boolean isSame = cursor != null && item != null && cursor.isDamageAndIDIdentical(item);
          if (item != null && button == 2) {
            cursor = item.copy();
            cursor.count = cursor.getMaxStackSize();
            inventory.setCursorItem(cursor);
            creative_updateInventory(inventory);
            return;
          } else if (item != null && (cursor == null || isSame)) {
            if (button == 0) {
              if (isSame) {
                if (cursor.count < cursor.getMaxStackSize()) cursor.count++;
              } else {
                inventory.setCursorItem(item.copy());
                creative_updateInventory(inventory);
              }
            }
            return;
          }
        }
        if (button == 1 && cursor != null && cursor.count > 1) {
          inventory.getCursorItem().count--;
        } else {
          inventory.setCursorItem(null);
          creative_updateInventory(inventory);
        }
        return;
      }

      slotY = MCMath.floor((mouseY - posY - 142) / 18F);
      if (slotY == 0 && slotX >= 0 && slotX < 9) {
        if (Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
          inventory.main[slotX] = null;
        }
        super.mouseClicked(mouseX, mouseY, button);
      } else if (CreativeGrid.outsideBody(mouseX, mouseY, posX, posY,
          containerWidth, containerHeight, local.luke.power.creative.config.Config.current().destroySlot)) {
        // Use the normal outside-slot click for whole-stack/single-item drops and server sync.
        super.mouseClicked(mouseX, mouseY, button);
      }
    } else {
      super.mouseClicked(mouseX, mouseY, button);
    }
  }

  @Override
  protected void mouseReleased(int mouseX, int mouseY, int button) {
    super.mouseReleased(mouseX, mouseY, button);
    if (button > -1) {
      creative_drag = false;
    }
  }

  @Unique
  private void creative_updateInventory(PlayerInventory inventory) {
    PacketHelper.send(new SlotUpdatePacket(-1, inventory.getCursorItem()));
  }

  @Unique
  private void creative_mouseScroll() {
    if (creative_maxIndex == 0) {
      creative_drag = false;
      creative_slider = 0;
      creative_rowIndex = 0;
      Mouse.getDWheel();
      return;
    }
    if (creative_drag) {
      int mousePos = (int) mouseY - creative_mouseDelta;
      creative_slider = MathHelper.clamp((float) mousePos / (float) CreativeGrid.SCROLL_TRAVEL, 0.0F, 1.0F);
      creative_rowIndex = (int) ((creative_slider * creative_maxIndex) / 8.0F) << 3;
      if (creative_rowIndex > creative_maxIndex) {
        creative_rowIndex = creative_maxIndex;
      }
      creative_slider = (float) creative_rowIndex / creative_maxIndex;
      return;
    }
    int wheel = Mouse.getDWheel();
    if (wheel > 0) {
      creative_rowIndex -= 8;
      if (creative_rowIndex < 0) {
        creative_rowIndex = 0;
      }
      creative_slider = (float) creative_rowIndex / creative_maxIndex;
    } else if (wheel < 0) {
      creative_rowIndex += 8;
      if (creative_rowIndex > creative_maxIndex) {
        creative_rowIndex = creative_maxIndex;
      }
      creative_slider = (float) creative_rowIndex / creative_maxIndex;
    }
  }

  @Unique
  private int creative_getMaxItemIndex() {
    return CreativeGrid.maxScroll(creative_items.size());
  }

  @Unique
  private void creative_playSound() {
    power_lastSurvivalView = creative_normalGUI;
    power_lastTab = creative_tabPage * 7 + creative_tabIndex;
    this.minecraft.soundHelper.playSound("random.click", 1.0F, 1.0F);
  }

  @Unique
  private void creative_updateMaxIndex() {
    creative_maxTabIndex = TabRegistry.getTabsCount() - creative_tabPage * 7;
    if (creative_maxTabIndex > 7) creative_maxTabIndex = 7;
  }

  @Unique
  private CreativeTab creative_getTab(int page, int index) {
    index = page * 7 + index;
    if (index < 0 || index >= TabRegistry.getTabsCount()) return null;
    return TabRegistry.getTabByIndex(index);
  }

  @Unique
  private String creative_translate(String key) {
    if (key == null) return "null";
    return TranslationStorage.getInstance().translate(key, key);
  }

  @Unique
  private String creative_translate_2(String key) {
    if (key == null) return "null";
    String translated = TranslationStorage.getInstance().method_995(key);
    return translated.isEmpty() ? key : translated;
  }
}
