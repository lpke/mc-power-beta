package local.luke.power.recipes;

import java.io.IOException;
import java.util.*;
import local.luke.power.recipes.planner.RecipeStack;
import local.luke.power.mixin.DeathSlotAccessor;
import net.minecraft.class_138;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/** Registry displays are stable while open; native ingredient availability is sampled on ticks. */
final class RecipeBookModel {
  final List<RecipeDefinition> recipes;
  final Map<String,Boolean> available=new HashMap<>();
  List<ItemStack> fuels=List.of();
  private boolean fuelsLoaded;
  private List<RecipeStack> supply=List.of();
  private int cursor,ticks;
  RecipeBookModel(){recipes=NativeRecipes.load();}
  void tick(Minecraft mc,RecipeContext context) {
    if(mc.player==null)return;
    if(!fuelsLoaded&&context.kind==RecipeDefinition.Kind.FURNACE&&!context.inputs.isEmpty()) {
      fuelsLoaded=true;
      if(((DeathSlotAccessor)context.output).power$deathInventory() instanceof class_138 furnace)fuels=NativeRecipes.fuels(furnace);
    }
    if(ticks++%5==0)try {
      List<RecipeStack> next=RecipeFills.supply(mc,context);
      if(!next.equals(supply)){supply=Collections.unmodifiableList(next);available.clear();cursor=0;}
    }catch(IOException failure){available.clear();supply=List.of();cursor=0;return;}
    int budget=24;
    while(cursor<recipes.size()&&budget-->0){var recipe=recipes.get(cursor++);
      try{available.put(recipe.id,RecipeFills.craftable(context,recipe,supply));}catch(Exception unsupported){available.put(recipe.id,false);}
    }
  }
  boolean available(RecipeDefinition recipe){return available.getOrDefault(recipe.id,false);}
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
