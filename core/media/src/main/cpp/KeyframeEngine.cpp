#include "KeyframeEngine.h"
#include <cmath>
#include <algorithm>

namespace vidopro {

static inline float sampleCurveX(float p1x, float p2x, float t) {
    // ((1-3*p2x+3*p1x)*t + (3*p2x-6*p1x))*t + 3*p1x)*t
    float cx = 3.0f * p1x;
    float bx = 3.0f * (p2x - p1x) - cx;
    float ax = 1.0f - cx - bx;
    return ((ax * t + bx) * t + cx) * t;
}

static inline float sampleCurveY(float p1y, float p2y, float t) {
    float cy = 3.0f * p1y;
    float by = 3.0f * (p2y - p1y) - cy;
    float ay = 1.0f - cy - by;
    return ((ay * t + by) * t + cy) * t;
}

static inline float sampleCurveDerivativeX(float p1x, float p2x, float t) {
    float cx = 3.0f * p1x;
    float bx = 3.0f * (p2x - p1x) - cx;
    float ax = 1.0f - cx - bx;
    return (3.0f * ax * t + 2.0f * bx) * t + cx;
}

float KeyframeEngine::evaluateCubicBezier(float x1, float y1, float x2, float y2, float time) {
    if (time <= 0.0f) return 0.0f;
    if (time >= 1.0f) return 1.0f;

    // Newton-Raphson method
    float t = time;
    for (int i = 0; i < 8; ++i) {
        float x = sampleCurveX(x1, x2, t) - time;
        if (std::abs(x) < 0.001f) break;
        float d2 = sampleCurveDerivativeX(x1, x2, t);
        if (std::abs(d2) < 0.000001f) break;
        t -= x / d2;
    }

    t = std::clamp(t, 0.0f, 1.0f);
    return sampleCurveY(y1, y2, t);
}

float KeyframeEngine::interpolate(
    float t,
    float startValue,
    float endValue,
    int interpolationType,
    float bezierX1,
    float bezierY1,
    float bezierX2,
    float bezierY2
) {
    t = std::clamp(t, 0.0f, 1.0f);
    float progress = t;

    switch (interpolationType) {
        case 0: // LINEAR
            progress = t;
            break;
        case 1: // BEZIER / EASE_IN_OUT
            progress = evaluateCubicBezier(bezierX1, bezierY1, bezierX2, bezierY2, t);
            break;
        case 2: // EASE_IN
            progress = evaluateCubicBezier(0.42f, 0.0f, 1.0f, 1.0f, t);
            break;
        case 3: // EASE_OUT
            progress = evaluateCubicBezier(0.0f, 0.0f, 0.58f, 1.0f, t);
            break;
        case 4: // HOLD
            progress = (t >= 1.0f) ? 1.0f : 0.0f;
            break;
        default:
            progress = t;
            break;
    }

    return startValue + (endValue - startValue) * progress;
}

} // namespace vidopro
