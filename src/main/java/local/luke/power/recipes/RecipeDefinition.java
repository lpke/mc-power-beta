package local.luke.power.recipes;

import java.util.*;
import local.luke.power.recipes.planner.RecipeIngredient;
import net.minecraft.class_160;
import net.minecraft.item.ItemStack;

/** Registry-backed recipe. Native identity is retained for final callback validation. */
public final class RecipeDefinition {
  public enum Kind { CRAFTING, FURNACE }
  public final String id,name,search,group;
  public final Kind kind;
  public final int width,height,category;
  public final boolean shaped;
  public final List<RecipeIngredient> ingredients;
  public final List<List<ItemStack>> icons;
  public final ItemStack output;
  public final class_160 nativeRecipe;
  public RecipeDefinition(String id,Kind kind,int width,int height,boolean shaped,List<RecipeIngredient> ingredients,
      List<List<ItemStack>> icons,ItemStack output,class_160 nativeRecipe,String name,int category,String group) {
    if(id==null||width<1||height<1||width>3||height>3||ingredients.size()>9||ingredients.isEmpty()||ingredients.stream().allMatch(Objects::isNull)
        ||icons.size()!=ingredients.size()||output==null)throw new IllegalArgumentException("Unsupported recipe display");
    this.id=id;this.kind=kind;this.width=width;this.height=height;this.shaped=shaped;
    this.ingredients=Collections.unmodifiableList(new ArrayList<>(ingredients));this.icons=List.copyOf(icons);this.output=output;
    this.nativeRecipe=nativeRecipe;this.name=name;this.category=category;this.group=group;
    this.search=(name+" "+id+" "+output.getTranslationKey()+" "+net.modificationstation.stationapi.api.registry.ItemRegistry.INSTANCE.getId(output.getItem())).toLowerCase(Locale.ROOT);
  }
  public List<RecipeIngredient> layout(int gridWidth,int gridHeight) {
    if(width>gridWidth||height>gridHeight||ingredients.size()>gridWidth*gridHeight)return null;
    List<RecipeIngredient> result=new ArrayList<>(Collections.nCopies(gridWidth*gridHeight,null));
    if(!shaped){for(int i=0;i<ingredients.size();i++)result.set(i,ingredients.get(i));return result;}
    int x=width<gridWidth/2f?(int)Math.floor(gridWidth/2f-width/2f):0;
    int y=height<gridHeight/2f?(int)Math.floor(gridHeight/2f-height/2f):0;
    for(int row=0;row<height;row++)for(int column=0;column<width;column++)result.set(x+column+(y+row)*gridWidth,ingredients.get(column+row*width));
    return result;
  }
}
