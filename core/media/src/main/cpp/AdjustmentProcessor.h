#pragma once
#include <cstdint>
#include <vector>

namespace vidopro {

struct AdjustmentParams {
    // Basic
    float brightness = 0.0f;       // -100 to 100
    float contrast = 1.0f;         // 0.2 to 3.0
    float exposure = 0.0f;         // -100 to 100
    float highlights = 0.0f;       // -100 to 100
    float shadows = 0.0f;          // -100 to 100
    float whites = 0.0f;           // -100 to 100
    float blacks = 0.0f;           // -100 to 100
    float saturation = 1.0f;       // 0.0 to 2.0
    float vibrance = 0.0f;         // -100 to 100
    float temperature = 0.0f;      // -100 to 100
    float tint = 0.0f;             // -100 to 100

    // Color & HSL
    float hue = 0.0f;              // -180 to 180
    float redBalance = 0.0f;       // -100 to 100
    float greenBalance = 0.0f;     // -100 to 100
    float blueBalance = 0.0f;      // -100 to 100

    // Tone & Light
    float gamma = 1.0f;            // 0.5 to 2.0
    float midtones = 0.0f;         // -100 to 100

    // Effects & Detail
    float vignette = 0.0f;         // 0 to 100
    float grain = 0.0f;            // 0 to 100
    float sharpen = 0.0f;          // 0 to 100
    float blur = 0.0f;             // 0 to 25
    float fade = 0.0f;             // 0 to 100
    float opacity = 1.0f;          // 0.0 to 1.0
};

class AdjustmentProcessor {
public:
    static void processPixels(uint32_t* pixels, int width, int height, int stride, const AdjustmentParams& params);
    static void applyFastBlur(uint32_t* pixels, int width, int height, int radius);
    static void applySharpen(uint32_t* pixels, int width, int height, float amount);
    static void applyVignette(uint32_t* pixels, int width, int height, float amount);
    static void applyGrain(uint32_t* pixels, int width, int height, float amount, uint64_t seed);
    static void applyLut3D(uint32_t* pixels, int width, int height, const uint8_t* lutData, int lutSize, float intensity);
};

} // namespace vidopro
