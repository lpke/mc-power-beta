package local.luke.power.ui;

import com.google.gson.*;
import java.util.*;
import local.luke.power.config.*;
import net.minecraft.client.gui.screen.Screen;

/** Routes storage formats to the same picker without changing setting IDs or saved data. */
public final class ItemEditors {
  private ItemEditors() {}
  private static String type(Setting setting){return Catalog.text(Catalog.metadata(setting.id),"valueType","");}
  public static boolean accepts(Setting setting){return Set.of("itemId","itemFilters","blockIdentifiers","veinRules","blockPalette").contains(type(setting));}
  public static String display(Setting setting) {
    return switch(type(setting)) {
      case "veinRules" -> setting.value.getAsJsonObject().size()+" block rules...";
      case "blockPalette" -> setting.value.getAsJsonObject().size()+" characters...";
      case "blockIdentifiers" -> setting.value.getAsJsonArray().size()+" blocks...";
      case "itemFilters" -> (setting.value.getAsString().isBlank()?0:setting.value.getAsString().split(",").length)+" items...";
      case "itemId" -> itemName(setting);
      default -> setting.display();
    };
  }
  private static String itemName(Setting setting) {
    try { return ItemCatalogue.resolve(ItemCatalogue.Domain.ITEM,setting.value.getAsString()).name(); }
    catch(IllegalArgumentException e) { return "ID "+setting.value.getAsString(); }
  }
  public static Screen screen(PowerOptionsScreen parent,Setting setting){
    String type=type(setting);
    if(type.equals("veinRules") || type.equals("blockPalette"))return new BlockRulesScreen(parent,setting,type.equals("veinRules"));
    ItemCatalogue.Domain domain=switch(type){
      case "itemId"->ItemCatalogue.Domain.ITEM;
      case "itemFilters"->ItemCatalogue.Domain.FILTER;
      case "blockIdentifiers"->ItemCatalogue.Domain.BLOCK_IDENTIFIER;
      default->throw new IllegalArgumentException("Unsupported item setting");
    };
    List<String> selected=new ArrayList<>();
    if(type.equals("itemId"))selected.add(setting.value.getAsString());
    else if(type.equals("itemFilters")){
      if(!setting.value.getAsString().isBlank())for(String item:setting.value.getAsString().split(","))selected.add(item.strip());
    }else setting.value.getAsJsonArray().forEach(item->selected.add(item.getAsString()));
    return new ItemPickerScreen(parent,setting.label,domain,selected,!type.equals("itemId"),values->{
      JsonElement next;
      if(type.equals("itemId"))next=new JsonPrimitive(Integer.parseInt(values.get(0)));
      else if(type.equals("itemFilters"))next=new JsonPrimitive(String.join(", ",values));
      else {JsonArray array=new JsonArray();values.forEach(array::add);next=array;}
      setting.validate(next);setting.value=next;parent.changed(setting);
    });
  }
}
