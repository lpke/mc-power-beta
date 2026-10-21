package local.luke.power.render;

/** Extend the pinned terrain shader without replacing its projection, textures or fog. */
public final class WaterShaderSource {
  private WaterShaderSource() {}
  public static String extend(String source) {
    if (source == null || !source.contains("vertexColor = Color;"))
      throw new IllegalStateException("Unsupported terrain shader: missing vertex color assignment");
    int versionEnd = source.indexOf('\n');
    if (versionEnd < 0 || !source.startsWith("#version"))
      throw new IllegalStateException("Unsupported terrain shader: missing GLSL version");
    return source.substring(0, versionEnd + 1)
        + """
          in vec4 PowerWaterColor;
          in float PowerLightMetaHigh;
          uniform float PowerWaterBrightness;
          uniform float PowerHeldStrength;
          uniform float PowerHeldEmission;
          uniform vec3 PowerHeldSourceRegion;
          uniform float PowerHeldCurve[16];
          """
        + source.substring(versionEnd + 1).replace("vertexColor = Color;",
            """
            uint powerMeta = (uint(PowerLightMetaHigh + 0.5) << 8u) | uint(PowerWaterColor.a * 255.0 + 0.5);
            bool powerWet = (powerMeta & 32768u) != 0u;
            vec3 powerWater = powerWet ? PowerWaterColor.rgb : Color.rgb;
            vec3 powerColor = mix(Color.rgb, powerWater, PowerWaterBrightness);
            if (PowerHeldStrength > 0.0) {
                vec3 powerDelta = Position - PowerHeldSourceRegion;
                float powerDistanceSquared = dot(powerDelta, powerDelta);
                if (powerDistanceSquared < 60.0625) {
                    float powerLevel = clamp(PowerHeldEmission * (1.0 - sqrt(powerDistanceSquared) / 7.75), 0.0, 15.0);
                    int powerLow = int(powerLevel);
                    float powerBrightness = mix(PowerHeldCurve[powerLow], PowerHeldCurve[min(15, powerLow + 1)], fract(powerLevel));
                    vec3 powerNeutral = powerWet
                        ? vec3(float((powerMeta >> 10u) & 31u), float((powerMeta >> 5u) & 31u), float(powerMeta & 31u)) / 31.0
                        : PowerWaterColor.rgb;
                    powerColor = max(powerColor, powerNeutral * (powerBrightness * PowerHeldStrength));
                }
            }
            vertexColor = vec4(powerColor, Color.a);
            """);
  }
}
