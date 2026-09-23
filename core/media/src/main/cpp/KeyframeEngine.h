#pragma once
#include <cstdint>

namespace vidopro {

class KeyframeEngine {
public:
    static float evaluateCubicBezier(float x1, float y1, float x2, float y2, float time);
    static float interpolate(
        float t,
        float startValue,
        float endValue,
        int interpolationType,
        float bezierX1 = 0.25f,
        float bezierY1 = 0.1f,
        float bezierX2 = 0.25f,
        float bezierY2 = 1.0f
    );
};

} // namespace vidopro
