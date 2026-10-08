package local.luke.power.capture;

/** Capture bounds follow the actual renderer grid, including custom chunk distances. */
final class IsometricLayout {
    static final int WORLD_HEIGHT = 128;
    private IsometricLayout() {}

    static int blockSpan(int chunks) {
        if (chunks < 1) throw new IllegalArgumentException("Renderer grid must not be empty");
        return Math.min(chunks, 26) * 16;
    }

    static double chunkOffset(double position) {
        return position - (Math.floor(position / 16) * 16 + 8);
    }
}
