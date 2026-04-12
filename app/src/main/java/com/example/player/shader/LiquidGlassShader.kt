package com.example.player.shader

// AGSL (Android Graphics Shading Language) shader for liquid glass refraction effect.
// Requires Android 13+ (API 33). Applied via RuntimeShader + ShaderBrush in Compose.
const val LIQUID_GLASS_AGSL = """
    uniform shader content;
    uniform float2 size;

    // Barrel/lens distortion + chromatic aberration — simulates light through curved glass
    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / size;
        float2 center = float2(0.5, 0.5);
        float2 offset = uv - center;
        float dist = length(offset);

        // Barrel distortion — stronger near edges
        float barrel = 1.0 + dist * dist * 0.06;
        float2 distortedUV = center + offset * barrel;

        // Chromatic aberration — slight RGB split at edges
        float aberration = dist * 0.004;
        float2 uvR = center + offset * (barrel + aberration);
        float2 uvB = center + offset * (barrel - aberration);

        distortedUV = clamp(distortedUV, float2(0.001), float2(0.999));
        uvR = clamp(uvR, float2(0.001), float2(0.999));
        uvB = clamp(uvB, float2(0.001), float2(0.999));

        half4 colorG = content.eval(distortedUV * size);
        half4 colorR = content.eval(uvR * size);
        half4 colorB = content.eval(uvB * size);

        return half4(colorR.r, colorG.g, colorB.b, colorG.a);
    }
"""
