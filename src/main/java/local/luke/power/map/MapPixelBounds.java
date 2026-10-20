package local.luke.power.map;

/** A GUI map viewport in framebuffer pixels, with OpenGL's bottom-left origin. */
public record MapPixelBounds(int x, int y, int width, int height) {
  public static MapPixelBounds of(MapProjection view, int guiWidth, int guiHeight,
      int pixelWidth, int pixelHeight) {
    int x = Math.max(0, (int) Math.ceil(view.left * pixelWidth / guiWidth));
    int y = Math.max(0, (int) Math.ceil((guiHeight - view.top - view.height) * pixelHeight / guiHeight));
    int right = Math.min(pixelWidth, (int) Math.floor((view.left + view.width) * pixelWidth / guiWidth));
    int top = Math.min(pixelHeight, (int) Math.floor((guiHeight - view.top) * pixelHeight / guiHeight));
    return new MapPixelBounds(x, y, Math.max(0, right - x), Math.max(0, top - y));
  }
}
