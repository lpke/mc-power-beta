package local.luke.power.creative.inventory.registry;

import com.google.gson.JsonObject;
import java.util.*;
import local.luke.power.creative.CreativeTabsData;
import local.luke.power.creative.inventory.CreativeInventory;
import local.luke.power.creative.inventory.api.CreativeTab;
import local.luke.power.creative.inventory.api.SimpleTab;
import local.luke.power.creative.inventory.listeners.VanillaTabListener;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.registry.ItemRegistry;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.api.util.Namespace;

public final class TabRegistry {
  private static final Map<Namespace, List<CreativeTab>> PRE_ORDERED_TABS = new HashMap<>();
  private static final List<CreativeTab> NATIVE_TABS = new ArrayList<>(), DEFAULT_TABS = new ArrayList<>();
  private static final List<CreativeTab> ORDERED_TABS = new ArrayList<>();
  private static CreativeTab search;
  private static int revision;
  private static String problem = "";
  private TabRegistry() {}
  public static void register(Identifier id, CreativeTab tab) {
    PRE_ORDERED_TABS.computeIfAbsent(id.namespace, k -> new ArrayList<>()).add(tab);
  }
  public static void sort() {
    NATIVE_TABS.clear(); DEFAULT_TABS.clear();
    PRE_ORDERED_TABS.keySet().stream().sorted((a, b) -> {
      if (a == b) return 0;
      if (a == CreativeInventory.NAMESPACE) return -1;
      if (b == CreativeInventory.NAMESPACE) return 1;
      return a.compareTo(b);
    }).forEach(id -> NATIVE_TABS.addAll(PRE_ORDERED_TABS.get(id)));
    // The original seven groups remain available to append in the layout editor.
    CreativeTab tools = new SimpleTab(CreativeInventory.id("tools_and_weapons"), VanillaTabListener.tabTools.getIcon()) {
      @Override public String getTranslationKey() { return "tab.power_creative_inventory.tools_and_weapons.name"; }
    };
    tools.getItems().addAll(VanillaTabListener.tabTools.getItems());
    tools.getItems().addAll(VanillaTabListener.tabWeapons.getItems());
    for (CreativeTab tab : NATIVE_TABS) {
      if (tab == VanillaTabListener.tabTools) DEFAULT_TABS.add(tools);
      else if (tab != VanillaTabListener.tabWeapons) DEFAULT_TABS.add(tab);
    }
    search = new SimpleTab(CreativeInventory.id("search"), new ItemStack(Item.compass));
    Map<String, ItemStack> all = new LinkedHashMap<>();
    for (CreativeTab tab : NATIVE_TABS) for (ItemStack stack : tab.getItems())
      if (stack != null) all.putIfAbsent(stack.itemId + ":" + stack.getDamage(), stack);
    for (int id = 1; id < Item.byId.length; id++) if (Item.byId[id] != null)
      all.putIfAbsent(id + ":0", new ItemStack(id, 1, 0));
    all.values().stream().sorted(Comparator.comparingInt((ItemStack stack) -> stack.itemId)
        .thenComparingInt(ItemStack::getDamage)).forEach(search::addItem);
    reload(PowerConfig.section("creativeTabs"));
  }
  public static void reload(JsonObject source) {
    List<CreativeTab> next = new ArrayList<>();
    try {
      CreativeTabsData data = CreativeTabsData.read(source);
      if (data.customized()) for (int i = 0; i < data.size(); i++) {
        JsonObject value = data.tab(i);
        String title = value.get("title").getAsString();
        CreativeTab tab = new SimpleTab(CreativeInventory.id("custom_" + i), resolve(value.get("icon").getAsString())) {
          @Override public String getTranslationKey() { return title; }
          @Override public boolean literalTitle() { return true; }
        };
        for (String item : CreativeTabsData.slots(value)) tab.getItems().add(item.isEmpty() ? null : resolve(item));
        next.add(tab);
      } else next.addAll(DEFAULT_TABS);
      problem = "";
    } catch (RuntimeException invalid) {
      problem = invalid.getMessage(); next.addAll(DEFAULT_TABS);
      CreativeInventory.LOGGER.error("Creative layout could not be loaded; original settings preserved", invalid);
    }
    ORDERED_TABS.clear(); ORDERED_TABS.addAll(next); revision++;
  }
  private static ItemStack resolve(String text) {
    try {
      if (text.matches("[0-9]+(?::[0-9]+)?")) {
        String[] pieces = text.split(":"); int id = Integer.parseInt(pieces[0]);
        if (id > 0 && id < Item.byId.length && Item.byId[id] != null)
          return new ItemStack(id, 1, pieces.length == 2 ? Integer.parseInt(pieces[1]) : 0);
      } else {
        Item item = ItemRegistry.INSTANCE.get(Identifier.of(text));
        if (item != null) return new ItemStack(item);
      }
    } catch (RuntimeException ignored) { /* Keep unavailable IDs in settings, with empty display cells. */ }
    return null;
  }
  public static int revision() { return revision; }
  public static String problem() { return problem; }
  public static CreativeTab getTabByIndex(int index) { return index == ORDERED_TABS.size() ? search : ORDERED_TABS.get(index); }
  /** Regular tabs only. Search is pinned separately at index six. */
  public static int getTabsCount() { return ORDERED_TABS.size(); }
  public static CreativeTab search() { return search; }
  public static JsonObject defaultLayout() { return snapshot(DEFAULT_TABS); }
  public static JsonObject nativeLayout() { return snapshot(NATIVE_TABS); }
  private static JsonObject snapshot(List<CreativeTab> tabs) {
    CreativeTabsData data = CreativeTabsData.read(CreativeTabsData.defaults()); data.customized(true);
    for (CreativeTab tab : tabs) {
      String key = tab.getTranslationKey();
      String title = tab.literalTitle() ? key : TranslationStorage.getInstance().translate(key, key);
      if (tab == VanillaTabListener.tabTools) title = "Tools";
      List<String> items = new ArrayList<>();
      for (ItemStack stack : tab.getItems()) {
        items.add(stack == null ? "" : stack.itemId + ":" + stack.getDamage());
      }
      ItemStack icon = tab.getIcon();
      data.append(CreativeTabsData.tab(tab.getID().toString(), title,
          icon == null ? "1" : icon.itemId + ":" + icon.getDamage(), items));
    }
    return data.json();
  }
}
