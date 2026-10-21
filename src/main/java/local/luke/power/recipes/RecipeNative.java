package local.luke.power.recipes;

import java.io.IOException;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.recipes.planner.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.modificationstation.stationapi.api.registry.ItemRegistry;

/** Native item data is read and copied only on the game thread. */
public final class RecipeNative {
  private RecipeNative() {}
  public static RecipeStack stack(ItemStack stack)throws IOException {
    if(stack==null)return null;
    ItemStack identity=DeathItems.decode(DeathItems.encode(stack));identity.count=1;
    String bytes=DeathItems.encode(identity);identity.setDamage(0);
    var id=ItemRegistry.INSTANCE.getId(stack.getItem());
    return new RecipeStack(id==null?"numeric:"+stack.itemId:id.toString(),stack.getDamage(),bytes,DeathItems.encode(identity),
        stack.count,Math.min(64,stack.getMaxCount()),!stack.isDamaged()&&!special(stack.writeNbt(new NbtCompound()),0));
  }
  public static ItemStack nativeStack(RecipeStack stack)throws IOException {
    if(stack==null)return null;ItemStack result=DeathItems.decode(stack.identity());result.count=stack.count();return result;
  }
  public static RecipeIngredient ingredient(ItemStack template,boolean exactComponents)throws IOException {
    ItemStack visible=template.clone();visible.count=1;
    if(visible.getDamage()<0)visible.setDamage(0);
    RecipeStack stack=stack(visible);
    return new RecipeIngredient(List.of(new RecipeIngredient.Option(stack.item(),template.getDamage(),exactComponents?stack.components():null)));
  }
  private static boolean special(NbtCompound tag,int depth) {
    if(depth>16)return true;
    for(Object object:tag.values()) {
      NbtElement element=(NbtElement)object;String key=element.getKey().toLowerCase(Locale.ROOT);
      if(Set.of("ench","enchantments","storedenchantments","customname","custom_name").contains(key))return true;
      if(key.equals("display")&&element instanceof NbtCompound display&&display.contains("Name"))return true;
      if(element instanceof NbtCompound compound&&special(compound,depth+1))return true;
    }
    return false;
  }
}
