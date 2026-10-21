package local.luke.power.recipes.planner;

/** Immutable game-thread snapshot. Identity includes every native component except count. */
public record RecipeStack(String item,int damage,String identity,String components,int count,int limit,boolean usable) {
  public RecipeStack(String item,int damage,String identity,int count,int limit,boolean usable) { this(item,damage,identity,identity,count,limit,usable); }
  public RecipeStack {
    if(item==null||identity==null||components==null||count<1||count>1_000_000||limit<1)throw new IllegalArgumentException("Invalid recipe stack");
  }
  public RecipeStack count(int amount) { return amount==0?null:new RecipeStack(item,damage,identity,components,amount,limit,usable); }
}
