package local.luke.power.recipes;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import local.luke.power.PowerBeta;
import local.luke.power.death.DeathItems;
import local.luke.power.recipes.mixin.*;
import local.luke.power.recipes.planner.*;
import local.luke.power.ui.ItemCatalogue;
import net.minecraft.*;
import net.minecraft.item.ItemStack;
import net.minecraft.client.resource.language.TranslationStorage;

/** Registry adapters. Unsupported custom recipes need an explicit provider, never guessed inputs. */
public final class NativeRecipes {
  @FunctionalInterface public interface Provider { RecipeDefinition read(class_160 nativeRecipe)throws Exception; }
  private static final Map<Class<?>,Provider> PROVIDERS=new LinkedHashMap<>();
  private static Method tagTest;
  private static final Map<Integer,Integer> VANILLA_CATEGORIES=categories();
  private NativeRecipes() {}
  public static void register(Class<?> type,Provider provider){PROVIDERS.put(Objects.requireNonNull(type),Objects.requireNonNull(provider));}
  public static List<ItemStack> fuels(class_138 furnace) {
    List<ItemStack> result=new ArrayList<>();var nativeFuel=(RecipeFurnaceAccessor)furnace;
    for(ItemStack item:catalogue()) {
      ItemStack icon=item.clone();icon.count=1;
      if(nativeFuel.power$recipeFuelTime(icon)>0)result.add(icon);
      if(result.size()>=128)break;
    }
    return List.copyOf(result);
  }
  public static List<RecipeDefinition> load() {
    List<RecipeDefinition> result=new ArrayList<>();List<ItemStack> catalogue=catalogue();
    int index=0;
    for(Object object:List.copyOf(class_148.method_538().method_541())) {
      if(++index>16384)break;
      if(!(object instanceof class_160 recipe))continue;
      try {
        RecipeDefinition definition=read(recipe,catalogue);
        if(definition!=null)result.add(definition);
      } catch(Exception failure){PowerBeta.LOG.warn("Recipe book skipped unsafe recipe adapter: {}",object.getClass().getName(),failure);}
    }
    for(Object object:class_44.method_144().method_147().entrySet()) {
      if(result.size()>=16384)break;
      var entry=(Map.Entry<?,?>)object;if(!(entry.getValue() instanceof ItemStack output))continue;
      try {
        // Numeric native smelting keys match the item ID regardless of metadata.
        Part input=entry.getKey() instanceof Number number?part(new ItemStack(number.intValue(),1,-1),false)
            :entry.getKey() instanceof ItemStack stack?part(stack,true):tag(entry.getKey(),catalogue);
        if(input!=null)result.add(definition(RecipeDefinition.Kind.FURNACE,1,1,true,List.of(input),output,null));
      } catch(Exception failure){PowerBeta.LOG.warn("Recipe book skipped unsafe smelting recipe",failure);}
    }
    // Native order breaks ties deterministically; duplicate displays do not create duplicate book cells.
    LinkedHashMap<String,RecipeDefinition> distinct=new LinkedHashMap<>();for(var recipe:result)distinct.putIfAbsent(recipe.id,recipe);
    return List.copyOf(distinct.values());
  }
  private static RecipeDefinition read(class_160 recipe,List<ItemStack> catalogue)throws Exception {
    for(var entry:PROVIDERS.entrySet())if(entry.getKey().isInstance(recipe))return entry.getValue().read(recipe);
    List<Part> parts=new ArrayList<>();int width=1,height=1;boolean shaped=false;
    if(recipe.getClass()==class_193.class&&recipe instanceof ShapedRecipeAccessor access) {
      width=access.power$recipeWidth();height=access.power$recipeHeight();shaped=true;
      for(ItemStack item:access.power$recipeInputs())parts.add(part(item,false));
    } else if(recipe.getClass()==class_453.class&&recipe instanceof ShapelessRecipeAccessor access) {
      for(ItemStack item:access.power$recipeInputs())parts.add(part(item,false));
    } else {
      String name=recipe.getClass().getName();
      if(name.equals("net.modificationstation.stationapi.impl.recipe.StationShapedRecipe")) {
        width=recipe.getClass().getField("width").getInt(recipe);height=recipe.getClass().getField("height").getInt(recipe);shaped=true;
        for(Object either:(Object[])recipe.getClass().getMethod("getGrid").invoke(recipe))parts.add(either(either,catalogue));
      } else if(name.equals("net.modificationstation.stationapi.impl.recipe.StationShapelessRecipe")) {
        for(Object either:(Object[])recipe.getClass().getMethod("getIngredients").invoke(recipe))parts.add(either(either,catalogue));
      } else return null;
    }
    if(width<1||height<1||width>3||height>3||parts.size()>9||shaped&&parts.size()!=width*height)return null;
    return definition(RecipeDefinition.Kind.CRAFTING,width,height,shaped,parts,recipe.method_2073(),recipe);
  }
  private record Part(RecipeIngredient ingredient,List<ItemStack> icons) {}
  private static Part part(ItemStack template,boolean exact)throws IOException {
    if(template==null)return null;
    ItemStack icon=template.clone();icon.count=1;if(icon.getDamage()<0)icon.setDamage(0);
    return new Part(RecipeNative.ingredient(template,exact),List.of(icon));
  }
  private static Part either(Object either,List<ItemStack> catalogue)throws Exception {
    if(either==null)return null;
    Class<?> type=Class.forName("com.mojang.datafixers.util.Either");
    Optional<?> right=(Optional<?>)type.getMethod("right").invoke(either);
    if(right.isPresent())return part((ItemStack)right.get(),true);
    Optional<?> left=(Optional<?>)type.getMethod("left").invoke(either);
    Part resolved=left.isEmpty()?null:tag(left.get(),catalogue);
    if(resolved==null)throw new IOException("Recipe ingredient tag has no supported items");
    return resolved;
  }
  private static Part tag(Object tag,List<ItemStack> catalogue)throws Exception {
    if(tag==null)return null;
    if(tagTest==null)for(Method method:ItemStack.class.getMethods())if(method.getName().equals("isIn")&&method.getParameterCount()==1){tagTest=method;break;}
    if(tagTest==null)return null;
    List<RecipeIngredient.Option> options=new ArrayList<>();List<ItemStack> icons=new ArrayList<>();Set<String> items=new HashSet<>();
    for(ItemStack item:catalogue)if((boolean)tagTest.invoke(item,tag)) {
      RecipeStack stack=RecipeNative.stack(item);
      if(items.add(stack.item()))options.add(new RecipeIngredient.Option(stack.item(),-1,null));
      if(icons.size()<128)icons.add(item.clone());
    }
    return options.isEmpty()?null:new Part(new RecipeIngredient(options),List.copyOf(icons));
  }
  private static RecipeDefinition definition(RecipeDefinition.Kind kind,int width,int height,boolean shaped,List<Part> parts,
      ItemStack output,class_160 recipe)throws IOException,NoSuchAlgorithmException {
    if(output==null)return null;
    ItemStack saved=DeathItems.decode(DeathItems.encode(output));
    List<RecipeIngredient> ingredients=new ArrayList<>();List<List<ItemStack>> icons=new ArrayList<>();
    for(Part part:parts){ingredients.add(part==null?null:part.ingredient);icons.add(part==null?List.of():part.icons);}
    String data=kind+"|"+width+"x"+height+"|"+shaped+"|"+ingredients+"|"+DeathItems.encode(saved);
    String id=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8)));
    String name=TranslationStorage.getInstance().get(saved.getTranslationKey()+".name");
    if(name==null||name.endsWith(".name"))name=saved.getTranslationKey();
    int category=category(saved,kind);RecipeStack identity=RecipeNative.stack(saved);
    return new RecipeDefinition(id,kind,width,height,shaped,ingredients,icons,saved,recipe,name,category,
        saved.itemId==35&&parts.stream().filter(Objects::nonNull).flatMap(part->part.icons.stream()).anyMatch(item->item.itemId==351)
            ?kind+"|"+category+"|minecraft:wool":kind+"|"+category+"|"+identity.identity());
  }
  private static int category(ItemStack item,RecipeDefinition.Kind kind) {
    if(kind==RecipeDefinition.Kind.FURNACE)return item.getItem() instanceof class_554?1:item.itemId<256?2:3;
    Integer category=VANILLA_CATEGORIES.get(item.itemId);if(category!=null)return category;
    if(Set.of(23,25,27,28,29,33,55,69,70,72,75,76,77,93,331,356).contains(item.itemId))return 2;
    if(item.itemId<256)return 1;
    if(item.isDamageable()||item.getItem() instanceof class_500||item.getItem() instanceof class_367||item.getItem() instanceof class_607)return 3;
    return 4;
  }
  private static Map<Integer,Integer> categories() {
    try(var input=NativeRecipes.class.getResourceAsStream("/assets/powerbeta/vanilla-recipe-categories.json")) {
      if(input==null)return Map.of();
      var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("categories");
      Map<Integer,Integer> result=new HashMap<>();for(var entry:json.entrySet())result.put(Integer.parseInt(entry.getKey()),entry.getValue().getAsInt());return Map.copyOf(result);
    }catch(Exception failure){throw new IllegalStateException("Recipe category reference could not be read",failure);}
  }
  private static List<ItemStack> catalogue() {
    LinkedHashMap<String,ItemStack> items=new LinkedHashMap<>();
    for(var entry:ItemCatalogue.entries(ItemCatalogue.Domain.FILTER))if(entry.icon()!=null)
      items.put(entry.id()+":"+entry.icon().getDamage(),entry.icon());
    return List.copyOf(items.values());
  }
}
