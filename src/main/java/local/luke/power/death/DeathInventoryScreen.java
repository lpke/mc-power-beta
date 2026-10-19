package local.luke.power.death;

import java.io.IOException;
import java.util.*;
import local.luke.power.commands.CommandContext;
import local.luke.power.input.Bindings;
import local.luke.power.ui.UiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Saved items are display copies. Only explicit recovery uses the journal transaction. */
public final class DeathInventoryScreen extends UiScreen {
  private final DeathRecord record;
  private final DeathJournal journal;
  private final Object world, player;
  private final ItemStack[] items;
  private final String date;
  private final ItemRenderer renderer = new ItemRenderer();
  private Set<Integer> remaining;

  public DeathInventoryScreen(Minecraft mc, DeathRecord record) throws IOException {
    this.record = record; journal = DeathTracker.journal(mc); world = mc.world; player = mc.player;
    date = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")
        .withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(record.date));
    items = new ItemStack[record.items.size()];
    for (int i = 0; i < items.length; i++) items[i] = DeathItems.decode(record.items.get(i));
    remaining = new HashSet<>(journal.remaining(record.id));
  }

  private int left() { return (width - 176) / 2; }
  private int top() { return (height - 210) / 2 + 22; }
  private boolean owned() {
    return minecraft.world == world && minecraft.player == player && DeathTracker.available(minecraft)
        && record.player.equals(minecraft.player.name) && record.world.equals(CommandContext.world(minecraft));
  }
  private boolean canRecover() {
    return owned() && Set.of("available", "partial").contains(journal.state(record.id))
        && DeathTracker.recoveryDenial(minecraft).isEmpty();
  }
  private int displayedSlot(int index) {
    return record.slots.get(index);
  }
  private int slotX(int slot) { return slot < 36 ? 8 + slot % 9 * 18 : slot < 40 ? 8 : slot == 40 ? 144 : 88 + (slot - 41) % 2 * 18; }
  private int slotY(int slot) { return slot < 9 ? 142 : slot < 36 ? 84 + (slot / 9 - 1) * 18 : slot < 40 ? 8 + (39 - slot) * 18 : slot == 40 ? 36 : 26 + (slot - 41) / 2 * 18; }
  private int hit(int mx, int my) {
    for (int i = 0; i < items.length; i++) {
      int slot = displayedSlot(i);
      if (slot >= 0 && inside(mx, my, left() + slotX(slot) - 1, top() + slotY(slot) - 1, 18, 18)) return i;
    }
    return -1;
  }

  @Override public void tick() { if (!owned()) minecraft.setScreen(null); }
  @Override public boolean shouldPause() { return true; }
  @Override protected void keyPressed(char character, int key) {
    if (key == Keyboard.KEY_ESCAPE || Bindings.matches(minecraft.options.inventoryKey, key)) minecraft.setScreen(null);
  }
  @Override protected void mouseClicked(int mx, int my, int button) {
    if (Bindings.matches(minecraft.options.inventoryKey, button - 100)) { minecraft.setScreen(null); return; }
    if (button != 0) return;
    int x = left(), y = top();
    if (inside(mx, my, x, y + 170, 80, 18)) { click(); minecraft.setScreen(null); return; }
    if (!canRecover()) return;
    int index = hit(mx, my);
    boolean all = inside(mx, my, x + 84, y + 170, 92, 18);
    if (!all && (index < 0 || !remaining.contains(index))) return;
    click();
    try {
      if (all) DeathTracker.recover(minecraft, record); else DeathTracker.recover(minecraft, record, index);
      remaining = new HashSet<>(journal.remaining(record.id));
      minecraft.inGameHud.addChatMessage("§aRecovered items. Dropped items remain in the world.§r");
    } catch (IOException | IllegalArgumentException e) { minecraft.inGameHud.addChatMessage("§c" + e.getMessage() + "§r"); }
  }

  @Override public void render(int mx, int my, float delta) {
    renderBackground();
    int x = left(), y = top(), hovered = hit(mx, my);
    boolean recover = canRecover();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
    try {
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glColor4f(1, 1, 1, 1);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId("/gui/inventory.png"));
      drawTexture(x, y, 0, 0, 176, 166);
      // Cover the model panel, its black border and the recipe output/arrow.
      fill(x + 26, y + 7, x + 169, y + 79, 0xffc6c6c6);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId("/gui/inventory.png"));
      drawTexture(x + 87, y + 25, 87, 25, 36, 36);
      // Cursor items have no lasting inventory position, so show them separately.
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId("/gui/inventory.png"));
      drawTexture(x + 143, y + 35, 7, 83, 18, 18);
      drawCenteredTextWithShadow(textRenderer, "Death " + record.id, width / 2, y - 25, 0xffffff);
      drawCenteredTextWithShadow(textRenderer, date, width / 2, y - 13, 0xaaaaaa);
      GL11.glPushMatrix(); GL11.glRotatef(120, 1, 0, 0); net.minecraft.class_583.method_1930(); GL11.glPopMatrix();
      GL11.glEnable(32826);
      for (int i = 0; i < items.length; i++) {
        int slot = displayedSlot(i); if (slot < 0) continue;
        int sx = x + slotX(slot), sy = y + slotY(slot);
        renderer.method_1487(minecraft.textRenderer, minecraft.textureManager, items[i], sx, sy);
        renderer.method_1488(minecraft.textRenderer, minecraft.textureManager, items[i], sx, sy);
      }
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      for (int i = 0; i < items.length; i++) {
        int slot = displayedSlot(i); if (slot < 0) continue;
        int sx = x + slotX(slot), sy = y + slotY(slot);
        if (!remaining.contains(i)) fill(sx, sy, sx + 16, sy + 16, 0xbb777777);
        else if (hovered == i) fill(sx, sy, sx + 16, sy + 16, 0x55ffffff);
      }
      button("Close", x, y + 170, 80, 18, mx, my, true);
      button("Recover all", x + 84, y + 170, 92, 18, mx, my, recover);
      if (hovered >= 0) {
        ItemStack item = items[hovered];
        String name = TranslationStorage.getInstance().get(item.getTranslationKey() + ".name");
        String status = !remaining.contains(hovered) ? "Already recovered" : recover ? "Click to recover this stack" : "Locked";
        tooltip(item.count + "x " + name + "\n" + status, mx, my);
      } else if (inside(mx, my, x, y + 170, 80, 18)) tooltip("Close", mx, my);
      else if (inside(mx, my, x + 84, y + 170, 92, 18)) {
        String denial = DeathTracker.recoveryDenial(minecraft);
        tooltip(recover ? "Recover all remaining items" : denial.isEmpty() ? "Already recovered or interrupted" : denial, mx, my);
      }
    } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
  }
}
