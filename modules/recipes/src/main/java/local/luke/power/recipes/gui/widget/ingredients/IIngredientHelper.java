package local.luke.power.recipes.gui.widget.ingredients;

import local.luke.power.recipes.recipe.Focus;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface IIngredientHelper<T> {
    Collection<T> expandSubtypes(Collection<T> contained);

    T getMatch(Iterable<T> contained, @Nonnull Focus toMatch);
}
