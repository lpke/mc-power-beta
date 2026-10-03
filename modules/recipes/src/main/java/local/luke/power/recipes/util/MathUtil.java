package local.luke.power.recipes.util;

public class MathUtil {
    private MathUtil() {

    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    public static int divideCeil(int numerator, int denominator) {
        return (int) Math.ceil((float) numerator / denominator);
    }

}
