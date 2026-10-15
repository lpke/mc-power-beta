package local.luke.power.render;

/** Extend the pinned terrain shader without replacing its projection, textures or fog. */
public final class WaterShaderSource {
  private WaterShaderSource() {}
  public static String extend(String source) {
    if (source == null || !source.contains("vertexColor = Color;"))
      throw new IllegalStateException("Unsupported terrain shader: missing vertex colour assignment");
    int versionEnd = source.indexOf('\n');
    if (versionEnd < 0 || !source.startsWith("#version"))
      throw new IllegalStateException("Unsupported terrain shader: missing GLSL version");
    return source.substring(0, versionEnd + 1)
        + "in vec4 PowerWaterColor;\nuniform float PowerWaterBrightness;\n"
        + source.substring(versionEnd + 1).replace("vertexColor = Color;",
            "vertexColor = mix(Color, PowerWaterColor, PowerWaterBrightness);");
  }
}
