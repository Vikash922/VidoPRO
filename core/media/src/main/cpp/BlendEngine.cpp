#include "BlendEngine.h"
#include <algorithm>
#include <cmath>

namespace vidopro {

static inline uint8_t clamp8(int v) {
    return (v < 0) ? 0 : ((v > 255) ? 255 : (uint8_t)v);
}

void BlendEngine::blendSurfaces(
    uint32_t* basePixels,
    const uint32_t* overlayPixels,
    int width,
    int height,
    BlendMode mode,
    float opacity
) {
    if (!basePixels || !overlayPixels || width <= 0 || height <= 0 || opacity <= 0.001f) return;
    opacity = std::clamp(opacity, 0.0f, 1.0f);
    int totalPixels = width * height;

    for (int i = 0; i < totalPixels; ++i) {
        uint32_t oc = overlayPixels[i];
        uint8_t oa = (oc >> 24) & 0xFF;
        if (oa == 0) continue;

        float alphaFactor = (oa / 255.0f) * opacity;
        if (alphaFactor <= 0.001f) continue;

        uint32_t bc = basePixels[i];
        int br = bc & 0xFF;
        int bg = (bc >> 8) & 0xFF;
        int bb = (bc >> 16) & 0xFF;
        int or_ = oc & 0xFF;
        int og = (oc >> 8) & 0xFF;
        int ob = (oc >> 16) & 0xFF;

        int resR = or_, resG = og, resB = ob;

        switch (mode) {
            case BlendMode::NORMAL:
                resR = or_; resG = og; resB = ob;
                break;
            case BlendMode::MULTIPLY:
                resR = (br * or_) / 255;
                resG = (bg * og) / 255;
                resB = (bb * ob) / 255;
                break;
            case BlendMode::SCREEN:
                resR = 255 - ((255 - br) * (255 - or_)) / 255;
                resG = 255 - ((255 - bg) * (255 - og)) / 255;
                resB = 255 - ((255 - bb) * (255 - ob)) / 255;
                break;
            case BlendMode::OVERLAY:
                resR = (br < 128) ? (2 * br * or_) / 255 : (255 - 2 * (255 - br) * (255 - or_) / 255);
                resG = (bg < 128) ? (2 * bg * og) / 255 : (255 - 2 * (255 - bg) * (255 - og) / 255);
                resB = (bb < 128) ? (2 * bb * ob) / 255 : (255 - 2 * (255 - bb) * (255 - ob) / 255);
                break;
            case BlendMode::DARKEN:
                resR = std::min(br, or_);
                resG = std::min(bg, og);
                resB = std::min(bb, ob);
                break;
            case BlendMode::LIGHTEN:
                resR = std::max(br, or_);
                resG = std::max(bg, og);
                resB = std::max(bb, ob);
                break;
            case BlendMode::COLOR_DODGE:
                resR = (or_ == 255) ? 255 : std::min(255, (br << 8) / (255 - or_));
                resG = (og == 255) ? 255 : std::min(255, (bg << 8) / (255 - og));
                resB = (ob == 255) ? 255 : std::min(255, (bb << 8) / (255 - ob));
                break;
            case BlendMode::COLOR_BURN:
                resR = (or_ == 0) ? 0 : 255 - std::min(255, ((255 - br) << 8) / or_);
                resG = (og == 0) ? 0 : 255 - std::min(255, ((255 - bg) << 8) / og);
                resB = (ob == 0) ? 0 : 255 - std::min(255, ((255 - bb) << 8) / ob);
                break;
            case BlendMode::HARD_LIGHT:
                resR = (or_ < 128) ? (2 * or_ * br) / 255 : (255 - 2 * (255 - or_) * (255 - br) / 255);
                resG = (og < 128) ? (2 * og * bg) / 255 : (255 - 2 * (255 - og) * (255 - bg) / 255);
                resB = (ob < 128) ? (2 * ob * bb) / 255 : (255 - 2 * (255 - ob) * (255 - bb) / 255);
                break;
            case BlendMode::SOFT_LIGHT:
                resR = static_cast<int>((1.0f - 2.0f * (or_ / 255.0f)) * (br / 255.0f) * (br / 255.0f) * 255.0f + 2.0f * (or_ / 255.0f) * br);
                resG = static_cast<int>((1.0f - 2.0f * (og / 255.0f)) * (bg / 255.0f) * (bg / 255.0f) * 255.0f + 2.0f * (og / 255.0f) * bg);
                resB = static_cast<int>((1.0f - 2.0f * (ob / 255.0f)) * (bb / 255.0f) * (bb / 255.0f) * 255.0f + 2.0f * (ob / 255.0f) * bb);
                break;
            case BlendMode::DIFFERENCE:
                resR = std::abs(br - or_);
                resG = std::abs(bg - og);
                resB = std::abs(bb - ob);
                break;
            case BlendMode::EXCLUSION:
                resR = br + or_ - (2 * br * or_) / 255;
                resG = bg + og - (2 * bg * og) / 255;
                resB = bb + ob - (2 * bb * ob) / 255;
                break;
            case BlendMode::ADD:
                resR = std::min(255, br + or_);
                resG = std::min(255, bg + og);
                resB = std::min(255, bb + ob);
                break;
        }

        // Lerp based on alpha and opacity
        int finalR = static_cast<int>(br * (1.0f - alphaFactor) + resR * alphaFactor);
        int finalG = static_cast<int>(bg * (1.0f - alphaFactor) + resG * alphaFactor);
        int finalB = static_cast<int>(bb * (1.0f - alphaFactor) + resB * alphaFactor);

        basePixels[i] = (bc & 0xFF000000) |
                        (static_cast<uint32_t>(clamp8(finalB)) << 16) |
                        (static_cast<uint32_t>(clamp8(finalG)) << 8) |
                        static_cast<uint32_t>(clamp8(finalR));
    }
}

} // namespace vidopro
