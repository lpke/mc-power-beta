package local.luke.power.recipes;

import java.io.IOException;
import java.util.*;
import local.luke.power.recipes.planner.*;
import local.luke.power.mixin.DeathSlotAccessor;
import net.minecraft.class_138;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/** Registry displays are stable while open; native ingredient availability is sampled on ticks. */
final class RecipeBookModel {
  final List<RecipeDefinition> recipes;
  private RecipeAvailability<RecipeDefinition> availability;
  private final Map<RecipeDefinition,List<RecipeIngredient>> layouts=new IdentityHashMap<>();
  private List<RecipeStack> preparedSnapshot;
  private RecipeMatcher.PreparedSupply preparedSupply;
  private RecipeContext sampledContext;
  private static final int CHECK_LIMIT=2048;
  private static final long CHECK_NANOS=1_500_000L;
  List<ItemStack> fuels=List.of();
  private boolean fuelsLoaded;
  private int ticks;
  RecipeBookModel(){recipes=NativeRecipes.load();}
  void tick(Minecraft mc,RecipeContext context) {
    if(mc.player==null)return;
    if(sampledContext!=context) {
      sampledContext=context;layouts.clear();preparedSnapshot=null;preparedSupply=null;ticks=0;
      for(var recipe:recipes)if(recipe.kind==context.kind) {
        var cells=recipe.layout(context.width,context.height);
        if(cells!=null)layouts.put(recipe,Collections.unmodifiableList(cells));
      }
      availability=new RecipeAvailability<>(recipes.stream().filter(layouts::containsKey).toList());
    }
    if(!fuelsLoaded&&context.kind==RecipeDefinition.Kind.FURNACE&&!context.inputs.isEmpty()) {
      fuelsLoaded=true;
      if(((DeathSlotAccessor)context.output).power$deathInventory() instanceof class_138 furnace)fuels=NativeRecipes.fuels(furnace);
    }
    if(ticks++%5==0)try {
      availability.sample(RecipeFills.supply(mc,context));
    }catch(IOException failure){availability.unavailable();return;}
    availability.refresh(CHECK_LIMIT,CHECK_NANOS,(recipe,supply)->{
      try {
        if(preparedSnapshot!=supply){preparedSnapshot=supply;preparedSupply=RecipeMatcher.prepare(supply);}
        return RecipeFills.craftable(context,recipe,layouts.get(recipe),preparedSupply);
      }catch(Exception unsupported){return false;}
    });
  }
  boolean available(RecipeDefinition recipe){return availability!=null&&availability.available(recipe);}
  boolean current(){return availability!=null&&availability.current();}
  List<List<RecipeDefinition>> groups(RecipeContext context,int category,String query,boolean filter) {
    Map<String,List<RecipeDefinition>> groups=new LinkedHashMap<>();
    Map<String,Integer> scores=new HashMap<>();
    for(var recipe:recipes) {
      if(recipe.kind!=context.kind||recipe.layout(context.width,context.height)==null||category!=0&&recipe.category!=category||filter&&!available(recipe))continue;
      int score=local.luke.power.ui.FuzzySearch.score(query,recipe.name,recipe.search);
      if(score<local.luke.power.ui.FuzzySearch.NONE){
        groups.computeIfAbsent(recipe.group,key->new ArrayList<>()).add(recipe);
        scores.merge(recipe.group,score,Math::min);
      }
    }
    return groups.entrySet().stream().sorted(Comparator.comparingInt(entry->scores.get(entry.getKey())))
        .map(Map.Entry::getValue).toList();
  }
}
