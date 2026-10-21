package local.luke.power.recipes;

import local.luke.power.config.Catalog;
import local.luke.power.storage.PowerConfig;

public final class RecipeBookConfig {
  private static RecipeBookSettings current=PowerConfig.read("recipeBook",RecipeBookSettings.class);
  private RecipeBookConfig() {}
  public static RecipeBookSettings current(){return current;}
  public static RecipeBookSettings copy(){return Catalog.JSON.fromJson(Catalog.JSON.toJson(current),RecipeBookSettings.class);}
  public static void preview(RecipeBookSettings next){current=Catalog.JSON.fromJson(Catalog.JSON.toJson(next),RecipeBookSettings.class);}
  public static void save(RecipeBookSettings next)throws java.io.IOException{PowerConfig.save("recipeBook",next);preview(next);}
  public static void state(boolean furnace,boolean open,boolean filter) {
    var next=copy();if(furnace){next.furnaceOpen=open;next.furnaceFilter=filter;}else{next.craftingOpen=open;next.craftingFilter=filter;}
    PowerConfig.saveDeferred("recipeBook",next);preview(next);
  }
}
