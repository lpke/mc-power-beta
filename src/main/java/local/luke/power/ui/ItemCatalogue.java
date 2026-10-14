package local.luke.power.ui;

import java.util.*;
import net.minecraft.Item;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.registry.BlockRegistry;
import net.modificationstation.stationapi.api.registry.ItemRegistry;
import net.modificationstation.stationapi.api.util.Identifier;

/** Built when an editor opens, never while rendering or ticking gameplay. */
public final class ItemCatalogue {
  public enum Domain { ITEM, FILTER, BLOCK, BLOCK_IDENTIFIER }
  public record Entry(String value, int id, int damage, String name, String technical, ItemStack icon) {
    public String tooltip() { return name + "\n" + technical; }
    boolean matches(String query) {
      return (name + " " + technical).toLowerCase(Locale.ROOT).contains(query);
    }
  }
  private ItemCatalogue() {}
  private static boolean blockDomain(Domain domain) {
    return domain == Domain.BLOCK || domain == Domain.BLOCK_IDENTIFIER;
  }
  private static boolean block(int id) { return id > 0 && id < Block.BLOCKS.length && Block.BLOCKS[id] != null; }
  private static boolean item(int id) { return id > 0 && id < Item.ITEMS.length && Item.ITEMS[id] != null; }

  public static List<Entry> entries(Domain domain) {
    Map<String,Entry> entries = new LinkedHashMap<>();
    int size = blockDomain(domain) ? Block.BLOCKS.length : Item.ITEMS.length;
    for(int id=1; id<size; id++) if(blockDomain(domain) ? block(id) : item(id)) {
      Entry entry=entry(domain,id,-1);entries.put(entry.value(),entry);
    }
    if(domain == Domain.FILTER) {
      // The creative catalogue already owns supported metadata variants, including modded items.
      try {
        Class<?> tabs=Class.forName("local.luke.power.creative.inventory.registry.TabRegistry");
        int count=(int)tabs.getMethod("getTabsCount").invoke(null);
        Map<Integer,Set<Integer>> variants=new TreeMap<>();
        for(int tab=0;tab<count;tab++) {
          Object value=tabs.getMethod("getTabByIndex",int.class).invoke(null,tab);
          for(Object stack:(List<?>)value.getClass().getMethod("getItems").invoke(value)) {
            ItemStack s=(ItemStack)stack;
            if(item(s.itemId)) variants.computeIfAbsent(s.itemId,k->new TreeSet<>()).add(s.getDamage());
          }
        }
        for(var variant:variants.entrySet()) if(variant.getValue().size()>1) {
          Entry any=entries.get(Integer.toString(variant.getKey()));
          entries.put(any.value(),new Entry(any.value(),any.id(),any.damage(),any.name()+" (all variants)",any.technical(),any.icon()));
          for(int damage:variant.getValue()) {
            Entry entry=entry(domain,variant.getKey(),damage);entries.put(entry.value(),entry);
          }
        }
      } catch(ReflectiveOperationException ignored) { /* Base registry and custom metadata remain available. */ }
    }
    return entries.values().stream().sorted(Comparator.comparingInt(Entry::id).thenComparingInt(Entry::damage)).toList();
  }

  public static Entry resolve(Domain domain,String text) {
    String value=text.strip();int damage=-1,id;
    if(domain == Domain.FILTER && value.matches("[0-9]+:[0-9]+")) {
      String[] pieces=value.split(":");id=Integer.parseInt(pieces[0]);damage=Integer.parseInt(pieces[1]);
      if(damage>32767)throw new IllegalArgumentException("Metadata must be 0 to 32767");
    } else if(value.matches("[0-9]+")) {
      try { id=Integer.parseInt(value); } catch(NumberFormatException e) { throw new IllegalArgumentException("Unknown ID: "+value); }
    } else {
      try {
        Identifier identifier=Identifier.of(value);
        if(blockDomain(domain)) {
          Block block=BlockRegistry.INSTANCE.get(identifier);
          if(block==null || !identifier.equals(BlockRegistry.INSTANCE.getId(block)))throw new IllegalArgumentException();
          id=block.id;
        } else {
          Item item=ItemRegistry.INSTANCE.get(identifier);
          if(item==null || !identifier.equals(ItemRegistry.INSTANCE.getId(item)))throw new IllegalArgumentException();
          id=item.id;
        }
      } catch(RuntimeException e) { throw new IllegalArgumentException("Unknown "+(blockDomain(domain)?"block":"item")+" ID: "+value); }
    }
    if(!(blockDomain(domain)?block(id):item(id)))throw new IllegalArgumentException("Unknown "+(blockDomain(domain)?"block":"item")+" ID: "+value);
    return entry(domain,id,damage);
  }

  private static Entry entry(Domain domain,int id,int damage) {
    boolean blocks=blockDomain(domain);
    Identifier identifier=blocks?BlockRegistry.INSTANCE.getId(Block.BLOCKS[id]):ItemRegistry.INSTANCE.getId(Item.ITEMS[id]);
    String registry=identifier==null?"":identifier.toString();
    String value=domain==Domain.BLOCK_IDENTIFIER?registry:Integer.toString(id)+(damage<0?"":":"+damage);
    ItemStack icon=item(id)?new ItemStack(id,1,Math.max(0,damage)):null;
    String name;
    try { name=blocks?Block.BLOCKS[id].method_1596():net.minecraft.class_629.method_2049(icon.getTranslationKey()+".name"); }
    catch(RuntimeException e) { name=""; }
    if(name==null || name.isBlank() || name.endsWith(".name"))name=registry.isBlank()?"ID "+id:registry.substring(registry.indexOf(':')+1).replace('_',' ');
    if(blocks)name=switch(id) {
      case 8 -> "Flowing water"; case 9 -> "Still water";case 10 -> "Flowing lava";case 11 -> "Still lava";
      case 43 -> "Double slab";case 62 -> "Lit furnace";case 68 -> "Wall sign";
      case 74 -> "Lit redstone ore";case 75 -> "Unlit redstone torch";case 94 -> "Powered repeater";
      default -> name;
    };
    String technical="ID: "+id+(damage<0?"":":"+damage)+(registry.isBlank()?"":"\n"+registry)
        +(domain==Domain.FILTER && damage<0?"\nMatches all variants.":"");
    return new Entry(value,id,damage,name,technical,icon);
  }
}
