package local.luke.power.recipes;

import java.io.IOException;
import java.util.*;
import local.luke.power.mixin.DeathSlotAccessor;
import net.minecraft.class_138;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/** Registry displays are stable while open; native ingredient availability is sampled on ticks. */
final class RecipeBookModel {
  final List<RecipeDefinition> recipes;
  private final RecipeAvailability<RecipeDefinition> availability;
  List<ItemStack> fuels=List.of();
  private boolean fuelsLoaded;
  private int ticks;
  RecipeBookModel(){recipes=NativeRecipes.load();availability=new RecipeAvailability<>(recipes);}
  void tick(Minecraft mc,RecipeContext context) {
    if(mc.player==null)return;
    if(!fuelsLoaded&&context.kind==RecipeDefinition.Kind.FURNACE&&!context.inputs.isEmpty()) {
      fuelsLoaded=true;
      if(((DeathSlotAccessor)context.output).power$deathInventory() instanceof class_138 furnace)fuels=NativeRecipes.fuels(furnace);
    }
    if(ticks++%5==0)try {
      availability.sample(RecipeFills.supply(mc,context));
    }catch(IOException failure){availability.unavailable();return;}
    availability.refresh(24,(recipe,supply)->{
      try{return RecipeFills.craftable(context,recipe,supply);}catch(Exception unsupported){return false;}
    });
  }
  boolean available(RecipeDefinition recipe){return availability.available(recipe);}
  boolean current(){return availability.current();}
  List<List<RecipeDefinition>> groups(RecipeContext context,int category,String query,boolean filter) {
    Map<String,List<RecipeDefinition>> groups=new LinkedHashMap<>();String[] words=query.toLowerCase(Locale.ROOT).strip().split("\\s+");
    for(var recipe:recipes) {
      if(recipe.kind!=context.kind||recipe.layout(context.width,context.height)==null||category!=0&&recipe.category!=category||filter&&!available(recipe))continue;
      boolean matches=true;for(String word:words)if(!recipe.search.contains(word)){matches=false;break;}
      if(matches)groups.computeIfAbsent(recipe.group,key->new ArrayList<>()).add(recipe);
    }
    return List.copyOf(groups.values());
  }
}
