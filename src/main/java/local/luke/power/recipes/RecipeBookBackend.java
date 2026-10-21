package local.luke.power.recipes;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;

public final class RecipeBookBackend implements Backend {
  public static void register(ConfigSession session) {
    RecipeBookSettings value=RecipeBookConfig.current();
    session.add(new RecipeBookBackend(),List.of(
        entry("enabled","Recipe book","Browse registered crafting and smelting recipes. Beta recipes are available immediately. Local recipe filling preserves native item data.",value.enabled,false),
        entry("inventory","Show in inventories","Show the recipe-book button in the player inventory, crafting table and furnace. Turn this off for a pause-menu-only viewer.",value.inventory,true),
        entry("pauseMenu","Show in pause menu","Open the same recipe book as a read-only viewer from the pause menu.",value.pauseMenu,false),
        new Setting("recipeBook.buttonStyle","recipeBook","Gameplay","Recipe book","Button style",
            "Modern uses the recipe-book icon. Subtle places a small R beside the inventory sorting button.",Setting.Kind.CHOICE,
            new JsonPrimitive(value.buttonStyle),new JsonPrimitive(0),0,1,1,List.of("Modern","Subtle"),false)));
  }
  private static Setting entry(String key,String label,String help,boolean value,boolean defaults) {
    return new Setting("recipeBook."+key,"recipeBook","Gameplay","Recipe book",label,help,Setting.Kind.BOOLEAN,
        new JsonPrimitive(value),new JsonPrimitive(defaults),0,1,1,List.of(),false);
  }
  public String id(){return "recipeBook";}
  public List<Path> files(){return List.of(PowerConfig.path());}
  private RecipeBookSettings draft(Map<String,JsonElement> values)throws ReflectiveOperationException {
    var next=RecipeBookConfig.copy();
    for(var entry:values.entrySet()) {
      String key=entry.getKey().substring("recipeBook.".length());
      if(key.equals("buttonStyle")) {
        if(!entry.getValue().isJsonPrimitive()||!entry.getValue().getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Choose Modern or Subtle");
        int style;
        try{style=entry.getValue().getAsBigDecimal().intValueExact();}catch(ArithmeticException invalid){throw new IllegalArgumentException("Choose Modern or Subtle");}
        if(style<0||style>1)throw new IllegalArgumentException("Choose Modern or Subtle");
        next.buttonStyle=style;continue;
      }
      if(!Set.of("enabled","inventory","pauseMenu").contains(key)||!entry.getValue().isJsonPrimitive()
          ||!entry.getValue().getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("Choose On or Off");
      RecipeBookSettings.class.getField(key).setBoolean(next,entry.getValue().getAsBoolean());
    }
    return next;
  }
  public void validate(Map<String,JsonElement> values)throws Exception{draft(values);}
  public void apply(Map<String,JsonElement> values)throws Exception{RecipeBookConfig.save(draft(values));}
}
