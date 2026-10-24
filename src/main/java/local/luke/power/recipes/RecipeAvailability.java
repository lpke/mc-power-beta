package local.luke.power.recipes;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.LongSupplier;
import local.luke.power.recipes.planner.RecipeStack;

/** Refresh a bounded batch without exposing a half-empty recipe list to the GUI. */
final class RecipeAvailability<T> {
  private final List<T> recipes;
  private Map<T,Boolean> visible=Map.of(),pending;
  private List<RecipeStack> latest,sampled,published;
  private int cursor;
  RecipeAvailability(List<T> recipes){this.recipes=List.copyOf(recipes);}
  void sample(List<RecipeStack> supply){latest=Collections.unmodifiableList(new ArrayList<>(supply));}
  void refresh(int budget,BiPredicate<T,List<RecipeStack>> craftable) {
    refresh(budget,Long.MAX_VALUE,System::nanoTime,craftable);
  }
  void refresh(int budget,long nanos,BiPredicate<T,List<RecipeStack>> craftable) {
    refresh(budget,nanos,System::nanoTime,craftable);
  }
  void refresh(int budget,long nanos,LongSupplier clock,BiPredicate<T,List<RecipeStack>> craftable) {
    if(budget<=0||nanos<=0)return;
    if(pending==null) {
      if(latest==null||latest.equals(published))return;
      sampled=latest;pending=new HashMap<>();cursor=0;
    }
    long start=clock.getAsLong();
    while(cursor<recipes.size()&&budget-->0) {
      T recipe=recipes.get(cursor++);pending.put(recipe,craftable.test(recipe,sampled));
      // Native recipe callbacks cannot be preempted. Never start another after the time slice.
      if(clock.getAsLong()-start>=nanos)break;
    }
    if(cursor==recipes.size()){visible=Map.copyOf(pending);published=sampled;pending=null;}
  }
  boolean available(T recipe){return visible.getOrDefault(recipe,false);}
  boolean current(){return latest!=null&&latest.equals(published);}
  void unavailable(){latest=sampled=published=null;pending=null;visible=Map.of();}
}
