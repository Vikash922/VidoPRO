#include "AdjustmentProcessor.h"
#include <cmath>
#include <algorithm>
#include <thread>
#include <vector>
#include <cstring>

namespace vidopro {

static inline uint8_t clamp8(int v) {
    return (v < 0) ? 0 : ((v > 255) ? 255 : (uint8_t)v);
}

static inline uint8_t clamp8f(float v) {
    if (v <= 0.0f) return 0;
    if (v >= 255.0f) return 255;
    return static_cast<uint8_t>(v + 0.5f);
}

// Convert RGB (0..255) to HSL (H: 0..360, S: 0..1, L: 0..1)
static void rgbToHsl(float r, float g, float b, float& h, float& s, float& l) {
    r /= 255.0f; g /= 255.0f; b /= 255.0f;
    float maxVal = std::max({r, g, b});
    float minVal = std::min({r, g, b});
    float delta = maxVal - minVal;

    l = (maxVal + minVal) * 0.5f;

    if (delta <= 0.0001f) {
        h = 0.0f;
        s = 0.0f;
        return;
    }

    s = (l > 0.5f) ? (delta / (2.0f - maxVal - minVal)) : (delta / (maxVal + minVal));

    if (maxVal == r) {
        h = 60.0f * (fmodf(((g - b) / delta), 6.0f));
    } else if (maxVal == g) {
        h = 60.0f * (((b - r) / delta) + 2.0f);
    } else {
        h = 60.0f * (((r - g) / delta) + 4.0f);
    }
    if (h < 0.0f) h += 360.0f;
}

static inline float hueToRgb(float p, float q, float t) {
    if (t < 0.0f) t += 1.0f;
    if (t > 1.0f) t -= 1.0f;
    if (t < 1.0f / 6.0f) return p + (q - p) * 6.0f * t;
    if (t < 1.0f / 2.0f) return q;
    if (t < 2.0f / 3.0f) return p + (q - p) * (2.0f / 3.0f - t) * 6.0f;
    return p;
}

// Convert HSL back to RGB (0..255)
static void hslToRgb(float h, float s, float l, float& r, float& g, float& b) {
    if (s <= 0.0001f) {
        r = g = b = l * 255.0f;
        return;
    }

    float q = (l < 0.5f) ? (l * (1.0f + s)) : (l + s - l * s);
    float p = 2.0f * l - q;
    float hk = h / 360.0f;

    r = hueToRgb(p, q, hk + 1.0f / 3.0f) * 255.0f;
    g = hueToRgb(p, q, hk) * 255.0f;
    b = hueToRgb(p, q, hk - 1.0f / 3.0f) * 255.0f;
}

void AdjustmentProcessor::processPixels(
    uint32_t* pixels,
    int width,
    int height,
    int stride,
    const AdjustmentParams& params
) {
    if (!pixels || width <= 0 || height <= 0) return;

    // Pre-calculate adjustment constants
    const float exposureGain = powf(2.0f, params.exposure / 50.0f);
    const float contrast = std::clamp(params.contrast, 0.2f, 3.0f);
    const float brightness = (params.brightness / 100.0f) * 128.0f;
    const float sat = std::clamp(params.saturation, 0.0f, 3.0f);
    const float tempR = (params.temperature > 0) ? (params.temperature * 0.35f) : (params.temperature * 0.15f);
    const float tempB = (params.temperature < 0) ? (-params.temperature * 0.40f) : (-params.temperature * 0.20f);
    const float tintG = (params.tint < 0) ? (-params.tint * 0.35f) : (-params.tint * 0.10f);
    const float tintM = (params.tint > 0) ? (params.tint * 0.25f) : 0.0f;
    const float redBal = params.redBalance * 0.35f;
    const float greenBal = params.greenBalance * 0.35f;
    const float blueBal = params.blueBalance * 0.35f;
    const float highlightsGain = 1.0f + (params.highlights / 100.0f) * 0.30f;
    const float shadowsOffset = (params.shadows / 100.0f) * 25.0f;
    const float whitesGain = 1.0f + (params.whites / 100.0f) * 0.35f;
    const float blacksOffset = (params.blacks / 100.0f) * 35.0f;
    const float invGamma = 1.0f / std::clamp(params.gamma, 0.5f, 2.5f);
    const float opacity = std::clamp(params.opacity, 0.0f, 1.0f);
    const bool hasHueShift = (fabsf(params.hue) > 0.01f);
    const bool hasVignette = (params.vignette > 0.01f);
    const float vigAmount = params.vignette / 100.0f;
    const float centerX = width * 0.5f;
    const float centerY = height * 0.5f;
    const float maxRadiusSq = centerX * centerX + centerY * centerY;

    // Multi-threaded execution across CPU cores
    unsigned int numThreads = std::min(std::max(1u, std::thread::hardware_concurrency()), 8u);
    std::vector<std::thread> workers;
    workers.reserve(numThreads);

    int rowsPerThread = height / numThreads;

    for (unsigned int t = 0; t < numThreads; ++t) {
        int startY = t * rowsPerThread;
        int endY = (t == numThreads - 1) ? height : (startY + rowsPerThread);

        workers.emplace_back([=, &params]() {
            for (int y = startY; y < endY; ++y) {
                uint32_t* row = pixels + y * stride;
                float dy = y - centerY;
                float dySq = dy * dy;

                for (int x = 0; x < width; ++x) {
                    uint32_t c = row[x];
                    uint8_t a = (c >> 24) & 0xFF;
                    uint8_t b = (c >> 16) & 0xFF;
                    uint8_t g = (c >> 8) & 0xFF;
                    uint8_t r = c & 0xFF;

                    if (a == 0 && opacity == 1.0f) continue;

                    // 1. Exposure and Contrast
                    float rf = (static_cast<float>(r) * exposureGain - 128.0f) * contrast + 128.0f + brightness;
                    float gf = (static_cast<float>(g) * exposureGain - 128.0f) * contrast + 128.0f + brightness;
                    float bf = (static_cast<float>(b) * exposureGain - 128.0f) * contrast + 128.0f + brightness;

                    // 2. Whites & Blacks
                    rf = rf * whitesGain + blacksOffset;
                    gf = gf * whitesGain + blacksOffset;
                    bf = bf * whitesGain + blacksOffset;

                    // 3. Highlights & Shadows
                    float lum = 0.299f * rf + 0.587f * gf + 0.114f * bf;
                    if (lum > 128.0f) {
                        float hlWeight = (lum - 128.0f) / 128.0f;
                        rf *= (1.0f + (highlightsGain - 1.0f) * hlWeight);
                        gf *= (1.0f + (highlightsGain - 1.0f) * hlWeight);
                        bf *= (1.0f + (highlightsGain - 1.0f) * hlWeight);
                    } else {
                        float shWeight = (128.0f - lum) / 128.0f;
                        rf += shadowsOffset * shWeight;
                        gf += shadowsOffset * shWeight;
                        bf += shadowsOffset * shWeight;
                    }

                    // 4. Temperature, Tint & Color Balance
                    rf += tempR + tintM + redBal;
                    gf += tintG + greenBal;
                    bf += tempB + tintM + blueBal;

                    // 5. Gamma
                    if (invGamma != 1.0f) {
                        rf = powf(std::max(0.0f, rf / 255.0f), invGamma) * 255.0f;
                        gf = powf(std::max(0.0f, gf / 255.0f), invGamma) * 255.0f;
                        bf = powf(std::max(0.0f, bf / 255.0f), invGamma) * 255.0f;
                    }

                    // 6. Saturation & Vibrance
                    lum = 0.299f * rf + 0.587f * gf + 0.114f * bf;
                    float satFactor = sat;
                    if (params.vibrance != 0.0f) {
                        float maxC = std::max({rf, gf, bf});
                        float minC = std::min({rf, gf, bf});
                        float currentSat = (maxC - minC) / (maxC + 0.001f);
                        satFactor += (1.0f - currentSat) * (params.vibrance / 100.0f);
                    }
                    rf = lum + (rf - lum) * satFactor;
                    gf = lum + (gf - lum) * satFactor;
                    bf = lum + (bf - lum) * satFactor;

                    // 7. Hue shift
                    if (hasHueShift) {
                        float h, s, l;
                        rgbToHsl(rf, gf, bf, h, s, l);
                        h = fmodf(h + params.hue + 360.0f, 360.0f);
                        hslToRgb(h, s, l, rf, gf, bf);
                    }

                    // 8. Vignette
                    if (hasVignette) {
                        float dx = x - centerX;
                        float distSq = dx * dx + dySq;
                        float vigFactor = 1.0f - vigAmount * (distSq / maxRadiusSq);
                        vigFactor = std::clamp(vigFactor, 0.0f, 1.0f);
                        rf *= vigFactor;
                        gf *= vigFactor;
                        bf *= vigFactor;
                    }

                    // 9. Opacity
                    uint8_t outA = static_cast<uint8_t>(a * opacity);

                    row[x] = (static_cast<uint32_t>(outA) << 24) |
                             (static_cast<uint32_t>(clamp8f(bf)) << 16) |
                             (static_cast<uint32_t>(clamp8f(gf)) << 8) |
                             static_cast<uint32_t>(clamp8f(rf));
                }
            }
        });
    }

    for (auto& w : workers) {
        if (w.joinable()) w.join();
    }
}

void AdjustmentProcessor::applyFastBlur(uint32_t* pixels, int width, int height, int radius) {
    if (!pixels || width <= 0 || height <= 0 || radius <= 0) return;
    radius = std::clamp(radius, 1, 30);

    std::vector<uint32_t> temp(width * height);
    int wm = width - 1;
    int hm = height - 1;
    int div = radius + radius + 1;

    // Horizontal pass
    for (int y = 0; y < height; ++y) {
        int rsum = 0, gsum = 0, bsum = 0, asum = 0;
        int yi = y * width;

        for (int i = -radius; i <= radius; ++i) {
            uint32_t p = pixels[yi + std::clamp(i, 0, wm)];
            asum += (p >> 24) & 0xFF;
            bsum += (p >> 16) & 0xFF;
            gsum += (p >> 8) & 0xFF;
            rsum += p & 0xFF;
        }

        for (int x = 0; x < width; ++x) {
            temp[yi + x] = ((asum / div) << 24) |
                           ((bsum / div) << 16) |
                           ((gsum / div) << 8) |
                           (rsum / div);

            int p1 = yi + std::clamp(x - radius, 0, wm);
            int p2 = yi + std::clamp(x + radius + 1, 0, wm);
            uint32_t c1 = pixels[p1];
            uint32_t c2 = pixels[p2];

            asum += ((c2 >> 24) & 0xFF) - ((c1 >> 24) & 0xFF);
            bsum += ((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF);
            gsum += ((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF);
            rsum += (c2 & 0xFF) - (c1 & 0xFF);
        }
    }

    // Vertical pass
    for (int x = 0; x < width; ++x) {
        int rsum = 0, gsum = 0, bsum = 0, asum = 0;

        for (int i = -radius; i <= radius; ++i) {
            uint32_t p = temp[std::clamp(i, 0, hm) * width + x];
            asum += (p >> 24) & 0xFF;
            bsum += (p >> 16) & 0xFF;
            gsum += (p >> 8) & 0xFF;
            rsum += p & 0xFF;
        }

        for (int y = 0; y < height; ++y) {
            int yi = y * width + x;
            pixels[yi] = ((asum / div) << 24) |
                         ((bsum / div) << 16) |
                         ((gsum / div) << 8) |
                         (rsum / div);

            int p1 = std::clamp(y - radius, 0, hm) * width + x;
            int p2 = std::clamp(y + radius + 1, 0, hm) * width + x;
            uint32_t c1 = temp[p1];
            uint32_t c2 = temp[p2];

            asum += ((c2 >> 24) & 0xFF) - ((c1 >> 24) & 0xFF);
            bsum += ((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF);
            gsum += ((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF);
            rsum += (c2 & 0xFF) - (c1 & 0xFF);
        }
    }
}

void AdjustmentProcessor::applySharpen(uint32_t* pixels, int width, int height, float amount) {
    if (!pixels || width <= 2 || height <= 2 || amount <= 0.01f) return;
    float w = (amount / 100.0f) * 1.5f;

    std::vector<uint32_t> copy(pixels, pixels + (width * height));

    for (int y = 1; y < height - 1; ++y) {
        int yi = y * width;
        for (int x = 1; x < width - 1; ++x) {
            uint32_t c = copy[yi + x];
            uint32_t up = copy[yi - width + x];
            uint32_t dn = copy[yi + width + x];
            uint32_t lf = copy[yi + x - 1];
            uint32_t rt = copy[yi + x + 1];

            float r = (c & 0xFF) * (1.0f + 4.0f * w) - ((up & 0xFF) + (dn & 0xFF) + (lf & 0xFF) + (rt & 0xFF)) * w;
            float g = ((c >> 8) & 0xFF) * (1.0f + 4.0f * w) - (((up >> 8) & 0xFF) + ((dn >> 8) & 0xFF) + ((lf >> 8) & 0xFF) + ((rt >> 8) & 0xFF)) * w;
            float b = ((c >> 16) & 0xFF) * (1.0f + 4.0f * w) - (((up >> 16) & 0xFF) + ((dn >> 16) & 0xFF) + ((lf >> 16) & 0xFF) + ((rt >> 16) & 0xFF)) * w;

            pixels[yi + x] = (c & 0xFF000000) |
                             (static_cast<uint32_t>(clamp8f(b)) << 16) |
                             (static_cast<uint32_t>(clamp8f(g)) << 8) |
                             static_cast<uint32_t>(clamp8f(r));
        }
    }
}

void AdjustmentProcessor::applyGrain(uint32_t* pixels, int width, int height, float amount, uint64_t seed) {
    if (!pixels || width <= 0 || height <= 0 || amount <= 0.01f) return;
    float intensity = (amount / 100.0f) * 45.0f;
    uint32_t state = static_cast<uint32_t>(seed ? seed : 123456789);

    int totalPixels = width * height;
    for (int i = 0; i < totalPixels; ++i) {
        // Fast Xorshift32 PRNG
        state ^= state << 13;
        state ^= state >> 17;
        state ^= state << 5;

        float noise = ((static_cast<float>(state & 0xFFFF) / 32768.0f) - 1.0f) * intensity;

        uint32_t c = pixels[i];
        int r = (c & 0xFF) + static_cast<int>(noise);
        int g = ((c >> 8) & 0xFF) + static_cast<int>(noise);
        int b = ((c >> 16) & 0xFF) + static_cast<int>(noise);

        pixels[i] = (c & 0xFF000000) |
                    (static_cast<uint32_t>(clamp8(b)) << 16) |
                    (static_cast<uint32_t>(clamp8(g)) << 8) |
                    static_cast<uint32_t>(clamp8(r));
    }
}

void AdjustmentProcessor::applyLut3D(
    uint32_t* pixels,
    int width,
    int height,
    const uint8_t* lutData,
    int lutSize,
    float intensity
) {
    if (!pixels || !lutData || lutSize < 2 || intensity <= 0.01f) return;
    intensity = std::clamp(intensity, 0.0f, 1.0f);
    int totalPixels = width * height;
    float maxIdx = static_cast<float>(lutSize - 1);

    for (int i = 0; i < totalPixels; ++i) {
        uint32_t c = pixels[i];
        float r = (c & 0xFF) / 255.0f;
        float g = ((c >> 8) & 0xFF) / 255.0f;
        float b = ((c >> 16) & 0xFF) / 255.0f;

        // Trilinear cube sampling
        int ri = std::clamp(static_cast<int>(r * maxIdx), 0, lutSize - 1);
        int gi = std::clamp(static_cast<int>(g * maxIdx), 0, lutSize - 1);
        int bi = std::clamp(static_cast<int>(b * maxIdx), 0, lutSize - 1);

        int lutIdx = (bi * lutSize * lutSize + gi * lutSize + ri) * 3;
        float lutR = lutData[lutIdx];
        float lutG = lutData[lutIdx + 1];
        float lutB = lutData[lutIdx + 2];

        float finalR = (c & 0xFF) * (1.0f - intensity) + lutR * intensity;
        float finalG = ((c >> 8) & 0xFF) * (1.0f - intensity) + lutG * intensity;
        float finalB = ((c >> 16) & 0xFF) * (1.0f - intensity) + lutB * intensity;

        pixels[i] = (c & 0xFF000000) |
                    (static_cast<uint32_t>(clamp8f(finalB)) << 16) |
                    (static_cast<uint32_t>(clamp8f(finalG)) << 8) |
                    static_cast<uint32_t>(clamp8f(finalR));
    }
}

} // namespace vidopro
