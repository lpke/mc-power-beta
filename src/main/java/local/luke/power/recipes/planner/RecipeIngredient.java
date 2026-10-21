package local.luke.power.recipes.planner;

import java.util.*;

public record RecipeIngredient(List<Option> options) {
  public RecipeIngredient { options=List.copyOf(options);if(options.isEmpty()||options.size()>8192)throw new IllegalArgumentException("Invalid recipe ingredient"); }
  public record Option(String item,int damage,String components) {
    public Option { Objects.requireNonNull(item); }
    public boolean accepts(RecipeStack stack) {
      return item.equals(stack.item())&&(damage<0||damage==stack.damage())&&(components==null||components.equals(stack.components()));
    }
  }
  public boolean accepts(RecipeStack stack) { return stack!=null&&stack.usable()&&options.stream().anyMatch(option->option.accepts(stack)); }
}
