package local.luke.power.recipes;

/** Screen-owned state has the native screen's lifetime, without a static screen retention map. */
public interface RecipeBookHost {
  RecipeBookOverlay power$recipeBook();
}
